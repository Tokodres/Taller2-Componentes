package com.example.taller2_componentes.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.example.taller2_componentes.Controlador.JuegoController
import com.example.taller2_componentes.ui.pantallas.*
import com.example.taller2_componentes.ui.theme.Taller2ComponentesTheme

class MainActivity : ComponentActivity() {
    private val juegoController by lazy { JuegoController() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Taller2ComponentesTheme {
                EmojiGuessApp(juegoController)
            }
        }
    }
}

@Composable
fun EmojiGuessApp(juegoController: JuegoController) {
    var currentScreen by remember { mutableStateOf("principal") }

    when (currentScreen) {
        "principal" -> PantallaPrincipal(
            onCrearSala = { currentScreen = "crearSala" },
            onUnirseSala = { currentScreen = "unirseSala" }
        )
        "crearSala" -> PantallaCrearSala(
            juegoController = juegoController,
            onBack = {
                juegoController.salirDeSala()
                currentScreen = "principal"
            },
            onSalaCreada = { currentScreen = "salaEspera" }
        )
        "unirseSala" -> PantallaUnirseSala(
            juegoController = juegoController,
            onBack = { currentScreen = "principal" },
            onUnirseExitoso = { currentScreen = "salaEspera" }
        )
        "salaEspera" -> PantallaSalaEspera(
            juegoController = juegoController,
            onBack = {
                juegoController.salirDeSala()
                currentScreen = "principal"
            },
            onIniciarJuego = { currentScreen = "juego" }
        )
        "juego" -> PantallaJuego(
            juegoController = juegoController,
            onBack = {
                juegoController.salirDeSala()
                currentScreen = "principal"
            }
        )
    }
}