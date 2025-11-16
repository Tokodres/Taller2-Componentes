package com.example.taller2_componentes.Controlador

import com.example.taller2_componentes.modelo.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class JuegoController {
    private var salaActual: Sala? = null
    private val emojisDisponibles = listOf("😀", "😂", "🥰", "😎", "🤔", "😴", "🥳", "😡", "👻", "🤖")

    // Mapa de salas activas (código -> Sala)
    private val salasActivas = mutableMapOf<String, Sala>()

    // Flujos para estado compartido
    private val _mensajesChat = MutableStateFlow<List<MensajeChat>>(emptyList())
    val mensajesChat: StateFlow<List<MensajeChat>> = _mensajesChat.asStateFlow()

    private val _tiempoRestante = MutableStateFlow(30L)
    val tiempoRestante: StateFlow<Long> = _tiempoRestante.asStateFlow()

    private var temporizadorActivo = false

    // NUEVO: Crear sala como anfitrión
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
        salasActivas[codigoSala] = nuevaSala

        // Mensaje de sistema
        agregarMensajeChat("Sistema", "🎮 Sala creada con código: $codigoSala")
        agregarMensajeChat("Sistema", "👑 $nombreAnfitrion es el anfitrión")

        return codigoSala
    }

    // NUEVO: Unirse a sala existente
    fun unirseASala(codigoSala: String, nombreJugador: String): Boolean {
        val sala = salasActivas[codigoSala]

        if (sala == null) {
            return false // Sala no existe
        }

        if (sala.enCurso) {
            return false // El juego ya empezó
        }

        // Verificar si el nombre ya existe en la sala
        if (sala.jugadores.any { it.nombre == nombreJugador }) {
            return false // Nombre duplicado
        }

        val nuevoJugador = Jugador(
            id = UUID.randomUUID().toString(),
            nombre = nombreJugador,
            esAnfitrion = false
        )

        sala.jugadores.add(nuevoJugador)
        salaActual = sala

        // Mensaje de sistema
        agregarMensajeChat("Sistema", "🎉 $nombreJugador se unió a la sala")

        return true
    }

    // NUEVO: Generar código de sala único
    private fun generarCodigoSala(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { caracteres.random() }.joinToString("")
    }

    // NUEVO: Obtener código de sala actual
    fun obtenerCodigoSala(): String {
        return salaActual?.codigo ?: ""
    }

    // NUEVO: Verificar si el usuario actual es anfitrión
    fun esAnfitrion(jugadorId: String): Boolean {
        return salaActual?.jugadores?.find { it.id == jugadorId }?.esAnfitrion ?: false
    }

    // NUEVO: Obtener jugador actual
    fun obtenerJugadorActual(): Jugador? {
        // En una implementación real, esto vendría de la autenticación
        // Por ahora, devolvemos el primer jugador como ejemplo
        return salaActual?.jugadores?.firstOrNull()
    }

    // NUEVO: Iniciar juego (solo anfitrión)
    fun iniciarJuego(): Boolean {
        val sala = salaActual ?: return false

        // Verificar que hay al menos 2 jugadores
        if (sala.jugadores.size < 2) return false

        sala.enCurso = true
        sala.ganador = null
        asignarEmojis()
        iniciarNuevaRonda()
        iniciarTemporizador()

        agregarMensajeChat("Sistema", "🚀 ¡El juego ha comenzado!")

        return true
    }

    // Resto de métodos existentes (con algunas modificaciones)
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

        // Reiniciar temporizador para nueva ronda
        _tiempoRestante.value = 30
        iniciarTemporizador()

        agregarMensajeChat("Sistema", "🔄 Ronda ${sala.rondaActual?.numero} - Turno de: ${sala.rondaActual?.jugadorEnTurno?.nombre}")
    }

    fun procesarAdivinanza(jugadorId: String, emojiAdivinado: String): Boolean {
        val sala = salaActual ?: return false
        val jugador = sala.jugadores.find { it.id == jugadorId } ?: return false

        // Verificar si es el turno del jugador
        if (jugador.id != sala.rondaActual?.jugadorEnTurno?.id) {
            return false
        }

        val adivinoCorrecto = jugador.emojiAsignado?.codigo == emojiAdivinado

        if (adivinoCorrecto) {
            agregarMensajeChat("Sistema", "✅ ${jugador.nombre} adivinó correctamente!")
        } else {
            jugador.sigueEnJuego = false
            agregarMensajeChat("Sistema", "❌ ${jugador.nombre} falló y fue eliminado")
        }

        temporizadorActivo = false

        val jugadoresActivos = sala.jugadores.count { it.sigueEnJuego }
        if (jugadoresActivos == 1) {
            sala.ganador = sala.jugadores.find { it.sigueEnJuego }
            sala.enCurso = false
            agregarMensajeChat("Sistema", "🎉 ¡${sala.ganador?.nombre} es el GANADOR!")
        } else if (jugadoresActivos > 1) {
            pasarSiguienteTurno()
            asignarEmojis()
            agregarMensajeChat("Sistema", "🔄 Nuevos emojis asignados")
        }

        return adivinoCorrecto
    }

    private fun pasarSiguienteTurno() {
        val sala = salaActual ?: return
        val ronda = sala.rondaActual ?: return

        val jugadoresActivos = sala.jugadores.filter { it.sigueEnJuego }
        if (jugadoresActivos.isEmpty()) return

        val indiceActual = jugadoresActivos.indexOfFirst { it.id == ronda.jugadorEnTurno?.id }
        val siguienteIndice = (indiceActual + 1) % jugadoresActivos.size

        ronda.jugadorEnTurno = jugadoresActivos[siguienteIndice]
        ronda.tiempoRestante = 30

        agregarMensajeChat("Sistema", "🔄 Turno de: ${ronda.jugadorEnTurno?.nombre}")
    }

    // Sistema de chat
    fun enviarMensajeChat(nombreJugador: String, mensaje: String) {
        val nuevoMensaje = MensajeChat(
            jugadorId = "chat",
            nombreJugador = nombreJugador,
            mensaje = mensaje
        )
        _mensajesChat.value = _mensajesChat.value + nuevoMensaje
    }

    private fun agregarMensajeChat(nombreJugador: String, mensaje: String) {
        val nuevoMensaje = MensajeChat(
            jugadorId = "sistema",
            nombreJugador = nombreJugador,
            mensaje = mensaje
        )
        _mensajesChat.value = _mensajesChat.value + nuevoMensaje
    }

    // Sistema de temporizador
    private fun iniciarTemporizador() {
        temporizadorActivo = true
        _tiempoRestante.value = 30
    }

    fun actualizarTemporizador() {
        if (temporizadorActivo) {
            val tiempoActual = _tiempoRestante.value
            if (tiempoActual > 0) {
                _tiempoRestante.value = tiempoActual - 1
            } else {
                temporizadorActivo = false
                val jugadorEnTurno = salaActual?.rondaActual?.jugadorEnTurno
                jugadorEnTurno?.let { jugador ->
                    agregarMensajeChat("Sistema", "⏰ Tiempo agotado! ${jugador.nombre} fue eliminado")
                    jugador.sigueEnJuego = false
                    procesarFinDeTurno()
                }
            }
        }
    }

    private fun procesarFinDeTurno() {
        val sala = salaActual ?: return

        val jugadoresActivos = sala.jugadores.count { it.sigueEnJuego }
        if (jugadoresActivos == 1) {
            sala.ganador = sala.jugadores.find { it.sigueEnJuego }
            sala.enCurso = false
            agregarMensajeChat("Sistema", "🎉 ¡${sala.ganador?.nombre} es el GANADOR!")
        } else if (jugadoresActivos > 1) {
            pasarSiguienteTurno()
            asignarEmojis()
        }
    }

    // Obtener emojis visibles (de otros jugadores)
    fun obtenerEmojisVisibles(jugadorId: String): List<Pair<String, String>> {
        val sala = salaActual ?: return emptyList()

        return sala.jugadores
            .filter { it.id != jugadorId && it.sigueEnJuego }
            .map { jugador ->
                Pair(jugador.nombre, jugador.emojiAsignado?.codigo ?: "?")
            }
    }

    // Métodos existentes
    fun obtenerJugadores(): List<Jugador> {
        return salaActual?.jugadores ?: emptyList()
    }

    fun obtenerJugadorEnTurno(): Jugador? {
        return salaActual?.rondaActual?.jugadorEnTurno
    }

    fun obtenerSala(): Sala? {
        return salaActual
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
        temporizadorActivo = false
        _tiempoRestante.value = 30
    }

    fun obtenerMensajesChat(): List<MensajeChat> {
        return _mensajesChat.value
    }

    fun obtenerTiempoRestante(): Long {
        return _tiempoRestante.value
    }

    // NUEVO: Salir de la sala
    fun salirDeSala() {
        salaActual = null
        _mensajesChat.value = emptyList()
        temporizadorActivo = false
    }
}