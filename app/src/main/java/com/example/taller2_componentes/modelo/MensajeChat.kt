package com.example.taller2_componentes.modelo

data class MensajeChat(
    val id: String = "",
    val jugadorId: String = "",
    val nombreJugador: String = "",
    val mensaje: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
