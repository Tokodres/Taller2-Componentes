package com.example.taller2_componentes.Controlador

import com.example.taller2_componentes.modelo.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class JuegoController {
    private var salaActual: Sala? = null
    private val emojisDisponibles = listOf("😀", "😂", "🥰", "😎", "🤔", "😴", "🥳", "😡", "👻", "🤖")
    private val database = FirebaseDatabase.getInstance()
    private val salasRef = database.getReference("salas")

    // Flujos para estado compartido
    private val _mensajesChat = MutableStateFlow<List<MensajeChat>>(emptyList())
    val mensajesChat: StateFlow<List<MensajeChat>> = _mensajesChat.asStateFlow()

    private val _tiempoRestante = MutableStateFlow(30L)
    val tiempoRestante: StateFlow<Long> = _tiempoRestante.asStateFlow()

    // ===== MÉTODOS DE GESTIÓN DE SALA =====

    fun crearSalaComoAnfitrion(nombreAnfitrion: String): String {
        val codigoSala = generarCodigoSala()
        val anfitrion = Jugador(
            id = UUID.randomUUID().toString(),
            nombre = nombreAnfitrion,
            esAnfitrion = true
        )

        val nuevaSala = Sala(
            id = codigoSala,
            jugadores = mutableListOf(anfitrion),
            enCurso = false,
            codigo = codigoSala
        )

        salaActual = nuevaSala

        // Guardar en Firebase
        salasRef.child(codigoSala).setValue(nuevaSala)

        // Escuchar cambios en la sala
        escucharCambiosEnSala(codigoSala)

        agregarMensajeChat("Sistema", "🎮 Sala creada con código: $codigoSala")
        agregarMensajeChat("Sistema", "👑 $nombreAnfitrion es el anfitrión")

        return codigoSala
    }

    // CORREGIDO: Función con callback para manejar resultado asíncrono
    fun unirseASala(codigoSala: String, nombreJugador: String, onResult: (Boolean) -> Unit) {
        println("🔄 Intentando unirse a sala: $codigoSala")

        // Verificar si la sala existe en Firebase
        salasRef.child(codigoSala).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val sala = snapshot.getValue(Sala::class.java)
                if (sala != null && !sala.enCurso) {
                    println("✅ Sala encontrada: ${sala.codigo}")

                    val nuevoJugador = Jugador(
                        id = UUID.randomUUID().toString(),
                        nombre = nombreJugador,
                        esAnfitrion = false
                    )

                    sala.jugadores.add(nuevoJugador)
                    salaActual = sala

                    // Actualizar en Firebase
                    salasRef.child(codigoSala).setValue(sala).addOnSuccessListener {
                        println("✅ Jugador agregado: $nombreJugador")

                        // Escuchar cambios en la sala
                        escucharCambiosEnSala(codigoSala)
                        agregarMensajeChat("Sistema", "🎉 $nombreJugador se unió a la sala")
                        onResult(true)
                    }.addOnFailureListener { error ->
                        println("❌ Error al actualizar sala: ${error.message}")
                        onResult(false)
                    }
                } else {
                    println("❌ Sala no disponible o en curso")
                    onResult(false)
                }
            } else {
                println("❌ Sala no encontrada: $codigoSala")
                onResult(false)
            }
        }.addOnFailureListener { error ->
            println("❌ Error de Firebase: ${error.message}")
            onResult(false)
        }
    }

    private fun escucharCambiosEnSala(codigoSala: String) {
        salasRef.child(codigoSala).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sala = snapshot.getValue(Sala::class.java)
                if (sala != null) {
                    salaActual = sala
                    println("🔄 Sala actualizada: ${sala.jugadores.size} jugadores")
                    sala.jugadores.forEach { jugador ->
                        println("   👤 ${jugador.nombre} (Anfitrión: ${jugador.esAnfitrion})")
                    }
                } else {
                    println("❌ No se pudo obtener la sala")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ Error escuchando sala: ${error.message}")
            }
        })
    }

    // ===== MÉTODOS DE OBTENCIÓN DE DATOS =====

    fun obtenerSala(): Sala? {
        return salaActual
    }

    fun obtenerJugadores(): List<Jugador> {
        return salaActual?.jugadores ?: emptyList()
    }

    fun obtenerCodigoSala(): String {
        return salaActual?.codigo ?: ""
    }

    fun obtenerJugadorActual(): Jugador? {
        return salaActual?.jugadores?.firstOrNull()
    }

    fun esAnfitrion(jugadorId: String): Boolean {
        return salaActual?.jugadores?.find { it.id == jugadorId }?.esAnfitrion ?: false
    }

    fun obtenerJugadorEnTurno(): Jugador? {
        return salaActual?.rondaActual?.jugadorEnTurno
    }

    fun obtenerTiempoRestante(): Long {
        return salaActual?.rondaActual?.tiempoRestante ?: 30L
    }

    fun obtenerMensajesChat(): List<MensajeChat> {
        return _mensajesChat.value
    }

    fun obtenerEmojisVisibles(jugadorIdActual: String): List<Pair<String, String>> {
        val sala = salaActual ?: return emptyList()
        return sala.jugadores
            .filter { it.id != jugadorIdActual && it.emojiAsignado != null }
            .map { it.nombre to (it.emojiAsignado?.codigo ?: "") }
    }

    // ===== MÉTODOS DE JUEGO =====

    fun iniciarJuego(): Boolean {
        val sala = salaActual ?: return false

        if (sala.jugadores.size < 2) {
            println("❌ No hay suficientes jugadores: ${sala.jugadores.size}")
            return false
        }

        sala.enCurso = true
        sala.ganador = null
        asignarEmojis()
        iniciarNuevaRonda()

        // Sincronizar con Firebase
        salasRef.child(sala.codigo).setValue(sala)

        agregarMensajeChat("Sistema", "🚀 ¡El juego ha comenzado!")
        println("✅ Juego iniciado con ${sala.jugadores.size} jugadores")

        return true
    }

    fun procesarAdivinanza(jugadorId: String, emojiAdivinado: String): Boolean {
        val sala = salaActual ?: return false
        val jugador = sala.jugadores.find { it.id == jugadorId } ?: return false

        val adivinoCorrecto = jugador.emojiAsignado?.codigo == emojiAdivinado

        if (adivinoCorrecto) {
            agregarMensajeChat("Sistema", "✅ ${jugador.nombre} adivinó correctamente!")
        } else {
            jugador.sigueEnJuego = false
            agregarMensajeChat("Sistema", "❌ ${jugador.nombre} falló y fue eliminado")
        }

        // Sincronizar con Firebase
        salasRef.child(sala.codigo).setValue(sala)

        return adivinoCorrecto
    }

    fun actualizarTemporizador() {
        val sala = salaActual ?: return
        val ronda = sala.rondaActual ?: return

        if (ronda.tiempoRestante > 0) {
            ronda.tiempoRestante--
        } else {
            // Tiempo agotado, eliminar jugador
            ronda.jugadorEnTurno?.sigueEnJuego = false
            agregarMensajeChat("Sistema", "⏰ Tiempo agotado para ${ronda.jugadorEnTurno?.nombre}")
            iniciarNuevaRonda()
        }
    }

    // ===== MÉTODOS DE CHAT =====

    fun enviarMensajeChat(nombreJugador: String, mensaje: String) {
        agregarMensajeChat(nombreJugador, mensaje)
    }

    private fun agregarMensajeChat(nombreJugador: String, mensaje: String) {
        val nuevoMensaje = MensajeChat(
            jugadorId = "sistema",
            nombreJugador = nombreJugador,
            mensaje = mensaje
        )
        _mensajesChat.value = _mensajesChat.value + nuevoMensaje
    }

    // ===== MÉTODOS AUXILIARES =====

    private fun generarCodigoSala(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { caracteres.random() }.joinToString("")
    }

    private fun asignarEmojis() {
        val sala = salaActual ?: return
        val emojisMezclados = emojisDisponibles.shuffled()

        sala.jugadores.forEachIndexed { index, jugador ->
            if (jugador.sigueEnJuego) {
                jugador.emojiAsignado = Emoji(emojisMezclados[index % emojisMezclados.size])
            }
        }
    }

    private fun iniciarNuevaRonda() {
        val sala = salaActual ?: return
        val jugadoresActivos = sala.jugadores.filter { it.sigueEnJuego }
        if (jugadoresActivos.isEmpty()) return

        sala.rondaActual = Ronda(
            numero = (sala.rondaActual?.numero ?: 0) + 1,
            jugadorEnTurno = jugadoresActivos.first(),
            tiempoRestante = 30
        )
    }

    fun reiniciarJuego() {
        salaActual?.jugadores?.forEach { jugador ->
            jugador.sigueEnJuego = true
            jugador.emojiAsignado = null
        }
        salaActual?.enCurso = false
        salaActual?.rondaActual = null
        salaActual?.ganador = null
        _mensajesChat.value = emptyList()
    }

    fun salirDeSala() {
        salaActual = null
    }
}