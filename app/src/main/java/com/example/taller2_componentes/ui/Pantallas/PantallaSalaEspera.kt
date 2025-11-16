package com.example.taller2_componentes.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taller2_componentes.Controlador.JuegoController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSalaEspera(
    juegoController: JuegoController,
    onBack: () -> Unit,
    onIniciarJuego: () -> Unit
) {
    var jugadores by remember { mutableStateOf(juegoController.obtenerJugadores()) }
    val codigoSala = juegoController.obtenerCodigoSala()
    val jugadorActual = juegoController.obtenerJugadorActual()
    val esAnfitrion = jugadorActual?.let { juegoController.esAnfitrion(it.id) } ?: false

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            jugadores = juegoController.obtenerJugadores()

            if (juegoController.obtenerSala()?.enCurso == true) {
                onIniciarJuego()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sala de Espera") })
        },
        floatingActionButton = {
            if (esAnfitrion) {
                FloatingActionButton(
                    onClick = {
                        if (jugadores.size >= 2) {
                            juegoController.iniciarJuego()
                            onIniciarJuego()
                        }
                    },

                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar juego")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            // Código de sala
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Código de sala",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = codigoSala,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Comparte este código con otros jugadores",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = "Jugadores en sala: ${jugadores.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (esAnfitrion && jugadores.size < 2) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Esperando más jugadores... (mínimo 2)",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Lista de jugadores
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(jugadores) { jugador ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = jugador.nombre)

                            if (jugador.esAnfitrion) {
                                Text(text = "👑 Anfitrión")
                            } else {
                                Text(text = "Jugador")
                            }
                        }
                    }
                }
            }

            if (!esAnfitrion) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Esperando a que el anfitrión inicie el juego...",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}