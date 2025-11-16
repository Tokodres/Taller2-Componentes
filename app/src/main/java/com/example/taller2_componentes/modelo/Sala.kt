package com.example.taller2_componentes.modelo

data class Sala(
    val id: String = "",
    val jugadores: MutableList<Jugador> = mutableListOf(),
    var rondaActual: Ronda? = null,
    var enCurso: Boolean = false,
    var ganador: Jugador? = null,
    val codigo: String = "" // NUEVO: Código de la sala
)