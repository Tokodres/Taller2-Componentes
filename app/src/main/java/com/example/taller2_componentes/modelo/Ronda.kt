package com.example.taller2_componentes.modelo

data class Ronda(
    var numero: Int = 1,
    var jugadorEnTurno: Jugador? = null,
    var tiempoRestante: Long = 30
)
