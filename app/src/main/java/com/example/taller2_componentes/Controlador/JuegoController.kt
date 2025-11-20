package com.example.taller2_componentes.Controlador

import com.example.taller2_componentes.modelo.*
import com.example.taller2_componentes.repositorio.FirebaseRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import java.util.*

class JuegoController {
    private var salaActual: Sala? = null
    private var jugadorActual: Jugador? = null
    private val emojisDisponibles = listOf("😀", "😂", "🥰", "😎", "🤔", "😴", "🥳", "😡", "👻", "🤖")
    private val database = FirebaseDatabase.getInstance()
    private val salasRef = database.getReference("salas")
    private val firebaseRepository = FirebaseRepository()

    // Flujos para estado compartido
    private val _mensajesChat = MutableStateFlow<List<MensajeChat>>(emptyList())
    val mensajesChat: StateFlow<List<MensajeChat>> = _mensajesChat.asStateFlow()

    private val _jugadores = MutableStateFlow<List<Jugador>>(emptyList())
    val jugadores: StateFlow<List<Jugador>> = _jugadores.asStateFlow()

    private val _salaState = MutableStateFlow<Sala?>(null)
    val salaState: StateFlow<Sala?> = _salaState.asStateFlow()

    private val _esAnfitrion = MutableStateFlow<Boolean>(false)
    val esAnfitrion: StateFlow<Boolean> = _esAnfitrion.asStateFlow()

    private val _tiempoRestante = MutableStateFlow(30L)
    val tiempoRestante: StateFlow<Long> = _tiempoRestante.asStateFlow()

    // NUEVO: Preguntas disponibles
    private val preguntasDisponibles = listOf(
        "¿Mi emoji es una cara?",
        "¿Mi emoji es de color amarillo?",
        "¿Mi emoji tiene ojos?",
        "¿Mi emoji representa una emoción?",
        "¿Mi emoji es un objeto?"
    )

    private val _preguntasUsadas = MutableStateFlow<Set<String>>(emptySet())
    val preguntasUsadas: StateFlow<Set<String>> = _preguntasUsadas.asStateFlow()

    private var escuchaMensajesJob: Job? = null
    private var escuchaSalaJob: Job? = null

    // ===== MÉTODOS DE GESTIÓN DE SALA =====

    fun crearSalaComoAnfitrion(nombreAnfitrion: String): String {
        val codigoSala = generarCodigoSala()
        val anfitrionId = UUID.randomUUID().toString()
        val anfitrion = Jugador(
            id = anfitrionId,
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
        jugadorActual = anfitrion
        _jugadores.value = nuevaSala.jugadores.toList()
        _salaState.value = nuevaSala
        _esAnfitrion.value = true

        // Guardar en Firebase
        salasRef.child(codigoSala).setValue(nuevaSala)

        // Escuchar cambios en la sala
        iniciarEscuchaSala(codigoSala)

        // Iniciar escucha de mensajes del chat
        iniciarEscuchaMensajes(codigoSala)

        // Enviar mensaje de sistema a Firebase
        enviarMensajeSistema("🎮 Sala creada con código: $codigoSala")
        enviarMensajeSistema("👑 $nombreAnfitrion es el anfitrión")

        println("✅ Sala creada por anfitrión: $nombreAnfitrion (ID: $anfitrionId)")

        return codigoSala
    }

    fun unirseASala(codigoSala: String, nombreJugador: String, onResult: (Boolean) -> Unit) {
        println("🔄 Intentando unirse a sala: $codigoSala")

        // Verificar si la sala existe en Firebase
        salasRef.child(codigoSala).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val sala = snapshot.getValue(Sala::class.java)
                if (sala != null && !sala.enCurso) {
                    println("✅ Sala encontrada: ${sala.codigo}")
                    println("🔍 Jugadores actuales en sala:")
                    sala.jugadores.forEach { jugador ->
                        println("   👤 ${jugador.nombre} (Anfitrión: ${jugador.esAnfitrion}, ID: ${jugador.id})")
                    }

                    // CORREGIDO: Verificar que el jugador no esté ya en la sala
                    val jugadorExistente = sala.jugadores.find { it.nombre == nombreJugador }
                    if (jugadorExistente != null) {
                        println("❌ Ya existe un jugador con ese nombre en la sala")
                        onResult(false)
                        return@addOnSuccessListener
                    }

                    val nuevoJugadorId = UUID.randomUUID().toString()
                    val nuevoJugador = Jugador(
                        id = nuevoJugadorId,
                        nombre = nombreJugador,
                        esAnfitrion = false // IMPORTANTE: Siempre false para jugadores que se unen
                    )

                    // CORREGIDO: Agregar jugador sin modificar el estado de anfitrión existente
                    sala.jugadores.add(nuevoJugador)
                    salaActual = sala
                    jugadorActual = nuevoJugador
                    _jugadores.value = sala.jugadores.toList()
                    _salaState.value = sala
                    _esAnfitrion.value = false // NUNCA true para jugadores que se unen

                    // CORREGIDO: Actualizar solo el campo de jugadores en Firebase
                    val updates = mapOf("jugadores" to sala.jugadores)
                    salasRef.child(codigoSala).updateChildren(updates).addOnSuccessListener {
                        println("✅ Jugador agregado: $nombreJugador (NO anfitrión)")
                        println("🔍 Estado después de unirse:")
                        println("   - Jugador actual: ${jugadorActual?.nombre} (Anfitrión: ${_esAnfitrion.value})")
                        println("   - Total jugadores: ${sala.jugadores.size}")

                        // Escuchar cambios en la sala
                        iniciarEscuchaSala(codigoSala)
                        // Iniciar escucha de mensajes del chat
                        iniciarEscuchaMensajes(codigoSala)

                        // Enviar mensaje de sistema a Firebase
                        enviarMensajeSistema("🎉 $nombreJugador se unió a la sala")
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

    // Método para escuchar cambios en la sala en tiempo real
    private fun iniciarEscuchaSala(codigoSala: String) {
        escuchaSalaJob?.cancel()
        escuchaSalaJob = CoroutineScope(Dispatchers.IO).launch {
            firebaseRepository.obtenerSala(codigoSala).collect { sala ->
                if (sala != null) {
                    salaActual = sala
                    _jugadores.value = sala.jugadores.toList()
                    _salaState.value = sala

                    // CORREGIDO: Actualizar el estado de anfitrión basado en el jugador actual
                    actualizarEstadoAnfitrion(sala)

                    println("🔄 Sala actualizada: ${sala.jugadores.size} jugadores")
                    sala.jugadores.forEach { jugador ->
                        println("   👤 ${jugador.nombre} (Anfitrión: ${jugador.esAnfitrion}, ID: ${jugador.id})")
                    }
                    println("   👤 Jugador actual: ${jugadorActual?.nombre} (Anfitrión: ${_esAnfitrion.value})")
                }
            }
        }
    }

    // CORREGIDO: Método mejorado para actualizar el estado de anfitrión
    private fun actualizarEstadoAnfitrion(sala: Sala) {
        val jugadorActualId = jugadorActual?.id
        if (jugadorActualId != null) {
            val jugadorEnSala = sala.jugadores.find { it.id == jugadorActualId }
            val nuevoEstadoAnfitrion = jugadorEnSala?.esAnfitrion ?: false

            // Solo actualizar si cambió el estado
            if (_esAnfitrion.value != nuevoEstadoAnfitrion) {
                _esAnfitrion.value = nuevoEstadoAnfitrion
                println("🔍 Estado anfitrión ACTUALIZADO: ${_esAnfitrion.value} para jugador: ${jugadorActual?.nombre}")
            }
        } else {
            _esAnfitrion.value = false
        }
    }

    // Método para escuchar mensajes del chat en Firebase
    private fun iniciarEscuchaMensajes(codigoSala: String) {
        escuchaMensajesJob?.cancel()
        escuchaMensajesJob = CoroutineScope(Dispatchers.IO).launch {
            firebaseRepository.obtenerMensajes(codigoSala).collect { mensajes ->
                _mensajesChat.value = mensajes
                println("📨 Mensajes actualizados: ${mensajes.size} mensajes")
            }
        }
    }

    // ===== MÉTODOS DE OBTENCIÓN DE DATOS =====

    fun obtenerSala(): Sala? {
        return salaActual
    }

    fun obtenerJugadores(): List<Jugador> {
        return _jugadores.value
    }

    fun obtenerCodigoSala(): String {
        return salaActual?.codigo ?: ""
    }

    fun obtenerJugadorActual(): Jugador? {
        return jugadorActual
    }

    // CORREGIDO: Usar el StateFlow en lugar de calcularlo
    fun esJugadorActualAnfitrion(): Boolean {
        return _esAnfitrion.value
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

    // NUEVO: Método para obtener preguntas disponibles
    fun obtenerPreguntasDisponibles(jugadorId: String): List<String> {
        val sala = salaActual ?: return emptyList()

        // Solo el jugador en turno puede ver preguntas disponibles
        if (sala.rondaActual?.jugadorEnTurno?.id != jugadorId) {
            return emptyList()
        }

        return preguntasDisponibles.filter { !_preguntasUsadas.value.contains(it) }
    }

    // ===== MÉTODOS DE JUEGO =====

    fun iniciarJuego(): Boolean {
        val sala = salaActual ?: return false

        // CORREGIDO: Verificación más estricta del anfitrión
        if (!_esAnfitrion.value) {
            println("❌ SOLO EL ANFITRIÓN puede iniciar el juego. Estado actual: ${_esAnfitrion.value}")
            println("❌ Jugador actual: ${jugadorActual?.nombre} (ID: ${jugadorActual?.id})")
            println("❌ Anfitrión real: ${sala.jugadores.find { it.esAnfitrion }?.nombre}")
            return false
        }

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

        // Enviar mensaje de sistema a Firebase
        enviarMensajeSistema("🚀 ¡El juego ha comenzado!")
        println("✅ Juego iniciado por anfitrión: ${jugadorActual?.nombre}")

        return true
    }

    // NUEVO: Método para verificar ganador
    fun verificarGanador(): Jugador? {
        val sala = salaActual ?: return null
        val jugadoresActivos = sala.jugadores.filter { it.sigueEnJuego }

        return if (jugadoresActivos.size == 1) {
            jugadoresActivos.first().also { ganador ->
                sala.ganador = ganador
                sala.enCurso = false
                enviarMensajeSistema("🎉 ¡${ganador.nombre} es el GANADOR!")

                // Sincronizar con Firebase
                salasRef.child(sala.codigo).setValue(sala)
            }
        } else {
            null
        }
    }

    // NUEVO: Método para avanzar turno
    fun avanzarTurno() {
        val sala = salaActual ?: return
        val ronda = sala.rondaActual ?: return

        // Verificar si hay ganador primero
        if (verificarGanador() != null) {
            return
        }

        val jugadoresActivos = sala.jugadores.filter { it.sigueEnJuego }
        if (jugadoresActivos.isEmpty()) return

        val jugadorActualIndex = jugadoresActivos.indexOfFirst { it.id == ronda.jugadorEnTurno?.id }
        val siguienteIndex = (jugadorActualIndex + 1) % jugadoresActivos.size

        // NUEVO: Reasignar emojis para la nueva ronda
        reasignarEmojis()

        // Reiniciar preguntas para el nuevo turno
        reiniciarPreguntas()

        sala.rondaActual = ronda.copy(
            jugadorEnTurno = jugadoresActivos[siguienteIndex],
            tiempoRestante = 30
        )

        // Sincronizar con Firebase
        salasRef.child(sala.codigo).setValue(sala)

        // Enviar mensaje de sistema
        enviarMensajeSistema("🔄 Turno de: ${sala.rondaActual?.jugadorEnTurno?.nombre}")
    }


    fun procesarAdivinanza(jugadorId: String, emojiAdivinado: String): Boolean {
        val sala = salaActual ?: return false
        val jugador = sala.jugadores.find { it.id == jugadorId } ?: return false

        val adivinoCorrecto = jugador.emojiAsignado?.codigo == emojiAdivinado

        if (adivinoCorrecto) {
            enviarMensajeSistema("✅ ${jugador.nombre} adivinó correctamente!")
            // Avanzar al siguiente turno
            avanzarTurno()
        } else {
            jugador.sigueEnJuego = false
            enviarMensajeSistema("❌ ${jugador.nombre} falló y fue eliminado")

            // Verificar si hay ganador
            if (verificarGanador() == null) {
                // Si no hay ganador, avanzar turno
                avanzarTurno()
            }
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
            // Sincronizar con Firebase
            salasRef.child(sala.codigo).child("rondaActual").child("tiempoRestante").setValue(ronda.tiempoRestante)
        } else {
            // Tiempo agotado, eliminar jugador
            ronda.jugadorEnTurno?.sigueEnJuego = false
            enviarMensajeSistema("⏰ Tiempo agotado para ${ronda.jugadorEnTurno?.nombre}")

            // Verificar si hay ganador
            if (verificarGanador() == null) {
                // Si no hay ganador, avanzar turno
                avanzarTurno()
            }
        }
    }

    // ===== MÉTODOS DE CHAT =====

    fun enviarMensajeChat(mensaje: String) {
        val jugador = jugadorActual ?: return
        val sala = salaActual ?: return

        val mensajeChat = MensajeChat(
            id = UUID.randomUUID().toString(),
            jugadorId = jugador.id,
            nombreJugador = jugador.nombre,
            mensaje = mensaje,
            timestamp = System.currentTimeMillis()
        )

        // Enviar mensaje a Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firebaseRepository.enviarMensaje(sala.codigo, mensajeChat).collect { exito ->
                if (exito) {
                    println("✅ Mensaje enviado: $mensaje")
                } else {
                    println("❌ Error al enviar mensaje")
                }
            }
        }
    }

    // NUEVO: Método para usar una pregunta
// NUEVO: Método para usar una pregunta
    // NUEVO: Método para usar una pregunta
    fun usarPregunta(jugadorId: String, pregunta: String): Boolean {
        val sala = salaActual ?: return false
        val jugador = sala.jugadores.find { it.id == jugadorId }

        // Verificar que el jugador existe, es el turno del jugador y la pregunta está disponible
        if (jugador == null || jugador.id != sala.rondaActual?.jugadorEnTurno?.id) {
            return false
        }

        if (_preguntasUsadas.value.contains(pregunta)) {
            return false
        }

        // Marcar pregunta como usada
        _preguntasUsadas.value = _preguntasUsadas.value + pregunta

        // Enviar pregunta al chat como mensaje del sistema
        enviarMensajeSistema("❓ ${jugador.nombre} pregunta: $pregunta")

        return true
    }

    // NUEVO: Método para reiniciar preguntas al cambiar de turno
    private fun reiniciarPreguntas() {
        _preguntasUsadas.value = emptySet()
    }

    // Método para enviar mensajes del sistema
    private fun enviarMensajeSistema(mensaje: String) {
        val sala = salaActual ?: return

        val mensajeChat = MensajeChat(
            id = UUID.randomUUID().toString(),
            jugadorId = "sistema",
            nombreJugador = "Sistema",
            mensaje = mensaje,
            timestamp = System.currentTimeMillis()
        )

        // Enviar mensaje a Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firebaseRepository.enviarMensaje(sala.codigo, mensajeChat).collect { exito ->
                if (exito) {
                    println("✅ Mensaje de sistema enviado: $mensaje")
                } else {
                    println("❌ Error al enviar mensaje de sistema")
                }
            }
        }
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

        // NUEVO: Usar reasignarEmojis en lugar de asignarEmojis
        reasignarEmojis()

        sala.rondaActual = Ronda(
            numero = (sala.rondaActual?.numero ?: 0) + 1,
            jugadorEnTurno = jugadoresActivos.first(),
            tiempoRestante = 30
        )
    }

    // NUEVO: Método para cuando un jugador abandona la sala
    fun jugadorAbandonaSala(jugadorId: String) {
        val sala = salaActual ?: return
        val jugador = sala.jugadores.find { it.id == jugadorId }

        if (jugador != null) {
            // Si el jugador estaba en juego, marcarlo como eliminado
            if (jugador.sigueEnJuego) {
                jugador.sigueEnJuego = false
                enviarMensajeSistema("🚪 ${jugador.nombre} abandonó la sala")

                // Si era el jugador en turno, avanzar al siguiente
                if (sala.rondaActual?.jugadorEnTurno?.id == jugadorId) {
                    avanzarTurno()
                }

                // Verificar si hay ganador
                verificarGanador()
            }

            // Remover jugador de la lista
            sala.jugadores.remove(jugador)

            // Sincronizar con Firebase
            salasRef.child(sala.codigo).setValue(sala)

            // Actualizar estado local
            _jugadores.value = sala.jugadores.toList()
        }
    }

    // NUEVO: Método mejorado para reiniciar juego
    fun reiniciarJuego(): Boolean {
        val sala = salaActual ?: return false

        // CORREGIDO: Solo el anfitrión puede reiniciar
        if (!_esAnfitrion.value) {
            println("❌ SOLO EL ANFITRIÓN puede reiniciar el juego")
            return false
        }

        sala.jugadores.forEach { jugador ->
            jugador.sigueEnJuego = true
            jugador.emojiAsignado = null
        }
        sala.enCurso = false
        sala.rondaActual = null
        sala.ganador = null

        // NUEVO: Usar reasignarEmojis en lugar de asignarEmojis
        reasignarEmojis()
        sala.enCurso = true
        iniciarNuevaRonda()

        // Sincronizar con Firebase
        salasRef.child(sala.codigo).setValue(sala)

        // Limpiar mensajes del chat
        _mensajesChat.value = emptyList()

        // Limpiar mensajes en Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firebaseRepository.eliminarMensajesSala(sala.codigo).collect()
        }

        enviarMensajeSistema("🔄 ¡Juego reiniciado! Nueva partida comenzada")

        return true
    }

    // NUEVO: Método para volver a sala de espera después del juego
    fun volverASalaEspera(): Boolean {
        val sala = salaActual ?: return false

        // Solo el anfitrión puede volver a sala de espera
        if (!_esAnfitrion.value) {
            return false
        }

        // Reiniciar estado del juego pero mantener jugadores
        sala.jugadores.forEach { jugador ->
            jugador.sigueEnJuego = true
            jugador.emojiAsignado = null
        }
        sala.enCurso = false
        sala.rondaActual = null
        sala.ganador = null

        // Sincronizar con Firebase
        salasRef.child(sala.codigo).setValue(sala)

        enviarMensajeSistema("🏠 Volviendo a sala de espera...")

        return true
    }

    // NUEVO: Método para obtener estado del juego
    fun obtenerEstadoJuego(): String {
        val sala = salaActual ?: return "No hay sala"

        return when {
            sala.ganador != null -> "Ganador: ${sala.ganador?.nombre}"
            sala.enCurso -> "En curso - Ronda: ${sala.rondaActual?.numero ?: 1}"
            else -> "En espera"
        }
    }

    fun salirDeSala() {
        val jugadorId = jugadorActual?.id
        val salaCodigo = salaActual?.codigo

        // Cancelar escucha de mensajes y sala
        escuchaMensajesJob?.cancel()
        escuchaSalaJob?.cancel()
        escuchaMensajesJob = null
        escuchaSalaJob = null

        // Notificar que el jugador abandonó (si estaba en una sala)
        if (jugadorId != null && salaCodigo != null) {
            jugadorAbandonaSala(jugadorId)
        }

        salaActual = null
        jugadorActual = null
        _mensajesChat.value = emptyList()
        _jugadores.value = emptyList()
        _salaState.value = null
        _esAnfitrion.value = false
        _preguntasUsadas.value = emptySet()
    }
    private fun reasignarEmojis() {
        val sala = salaActual ?: return
        val emojisMezclados = emojisDisponibles.shuffled()

        sala.jugadores.forEachIndexed { index, jugador ->
            if (jugador.sigueEnJuego) {
                jugador.emojiAsignado = Emoji(emojisMezclados[index % emojisMezclados.size])
            }
        }

        // Enviar mensaje de sistema informando del cambio
        enviarMensajeSistema("🔄 ¡Nuevos emojis asignados para esta ronda!")
    }
}