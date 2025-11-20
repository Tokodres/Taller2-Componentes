package com.example.taller2_componentes.modelo

import android.os.CountDownTimer

class Temporizador(
    private val duracion: Long,
    private val onTick: (Long) -> Unit,
    private val onFinish: () -> Unit
) {

    private var timer: CountDownTimer? = null

    fun iniciar() {
        timer = object : CountDownTimer(duracion, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                onTick(millisUntilFinished)
            }

            override fun onFinish() {
                onFinish()
            }

        }.start()
    }

    fun cancelar() {
        timer?.cancel()
    }
}