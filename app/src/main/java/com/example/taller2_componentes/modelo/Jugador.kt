package com.example.taller2_componentes.modelo

data class Jugador(
    val id: String = "",
    val nombre: String = "",
    var emojiAsignado: Emoji? = null,
    var sigueEnJuego: Boolean = true,
    val esAnfitrion: Boolean = false
) {
    constructor() : this("", "", null, true, false)
}