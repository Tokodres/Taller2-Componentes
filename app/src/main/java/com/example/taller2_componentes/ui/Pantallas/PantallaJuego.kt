package com.example.taller2_componentes.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.taller2_componentes.Controlador.JuegoController
import kotlinx.coroutines.delay

@Composable
fun PantallaJuego(
    juegoController: JuegoController,
    onBack: () -> Unit,
    onVolverSalaEspera: () -> Unit
) {
    var jugadores by remember { mutableStateOf(juegoController.obtenerJugadores()) }
    var jugadorEnTurno by remember { mutableStateOf(juegoController.obtenerJugadorEnTurno()) }
    val jugadorActual = remember { juegoController.obtenerJugadorActual() }
    var mensaje by remember { mutableStateOf("") }
    var mostrarDialogoEmojis by remember { mutableStateOf(false) }
    var mostrarDialogoPreguntas by remember { mutableStateOf(false) }
    var ganador by remember { mutableStateOf(juegoController.obtenerSala()?.ganador) }

    // CORREGIDO: Usar el StateFlow de mensajes del controlador
    val mensajesChat by juegoController.mensajesChat.collectAsState()
    val preguntasUsadas by juegoController.preguntasUsadas.collectAsState()

    var mensajeChat by remember { mutableStateOf(TextFieldValue()) }
    var tiempoRestante by remember { mutableStateOf(juegoController.obtenerTiempoRestante()) }

    val esMiTurno = jugadorEnTurno?.id == jugadorActual?.id
    val esAnfitrion = juegoController.esJugadorActualAnfitrion()
    val preguntasDisponibles = juegoController.obtenerPreguntasDisponibles(jugadorActual?.id ?: "")

    // Temporizador automático
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L) // Actualizar cada segundo
            juegoController.actualizarTemporizador()
            tiempoRestante = juegoController.obtenerTiempoRestante()

            // Actualizar otros estados
            jugadores = juegoController.obtenerJugadores()
            jugadorEnTurno = juegoController.obtenerJugadorEnTurno()
            ganador = juegoController.obtenerSala()?.ganador
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header: Información del juego
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (ganador != null) {
                    Text(
                        text = "🎉 ¡GANADOR: ${ganador!!.nombre}! 🎉",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "¡Felicidades! Has ganado el juego",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Turno de: ${jugadorEnTurno?.nombre ?: "Nadie"}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Tiempo: ${tiempoRestante}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (tiempoRestante <= 10) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (esMiTurno) {
                                Text(
                                    text = "Preguntas disponibles: ${preguntasDisponibles.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = "Ronda: ${juegoController.obtenerSala()?.rondaActual?.numero ?: 1}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (mensaje.isNotEmpty()) {
                    Text(
                        text = mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mensaje.contains("Correcto")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Layout principal: Jugadores + Chat
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Columna de jugadores y emojis visibles
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Jugadores
                Text(
                    text = "Jugadores (${jugadores.size}):",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(jugadores) { jugador ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    jugador.id == jugadorEnTurno?.id -> MaterialTheme.colorScheme.primaryContainer
                                    !jugador.sigueEnJuego -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = jugador.nombre,
                                        style = if (jugador.id == jugadorEnTurno?.id)
                                            MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        else MaterialTheme.typography.bodyLarge
                                    )
                                    if (jugador.esAnfitrion) {
                                        Text(
                                            text = "👑 Anfitrión",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = if (jugador.sigueEnJuego) "✅" else "❌",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                // Emojis visibles de otros jugadores
                Text(
                    text = "Emojis de otros:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                val emojisVisibles = juegoController.obtenerEmojisVisibles(jugadorActual?.id ?: "")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(emojisVisibles) { (nombre, emoji) ->
                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = nombre,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Chat - CON RESTRICCIONES PARA JUGADOR EN TURNO
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Chat Global:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Mensajes del chat
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                            reverseLayout = true // Para que los mensajes nuevos aparezcan abajo
                        ) {
                            items(mensajesChat.reversed()) { mensajeChatItem ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            mensajeChatItem.jugadorId == "sistema" ->
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            mensajeChatItem.jugadorId == jugadorActual?.id ->
                                                MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = mensajeChatItem.nombreJugador,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when {
                                                mensajeChatItem.jugadorId == "sistema" ->
                                                    MaterialTheme.colorScheme.onTertiaryContainer
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        Text(
                                            text = mensajeChatItem.mensaje,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = when {
                                                mensajeChatItem.jugadorId == "sistema" ->
                                                    MaterialTheme.colorScheme.onTertiaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Input de chat - RESTRINGIDO PARA JUGADOR EN TURNO
                        if (!esMiTurno || ganador != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = mensajeChat,
                                    onValueChange = { mensajeChat = it },
                                    label = {
                                        Text(
                                            if (esMiTurno && ganador == null)
                                                "No puedes chatear durante tu turno"
                                            else "Escribe un mensaje..."
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    enabled = !esMiTurno || ganador != null
                                )

                                IconButton(
                                    onClick = {
                                        if (mensajeChat.text.isNotBlank() && (!esMiTurno || ganador != null)) {
                                            juegoController.enviarMensajeChat(mensajeChat.text)
                                            mensajeChat = TextFieldValue()
                                        }
                                    },
                                    enabled = mensajeChat.text.isNotBlank() && (!esMiTurno || ganador != null)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                                }
                            }
                        } else {
                            // Mensaje para jugador en turno
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = "⏳ Estás en tu turno. No puedes chatear ahora.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Botones de acción - MODIFICADO CON NUEVAS OPCIONES
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ganador == null) {
                // Botones durante el juego
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (esMiTurno) {
                        // Botones para jugador en turno
                        Button(
                            onClick = { mostrarDialogoEmojis = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Adivinar Emoji")
                        }

                        Button(
                            onClick = {
                                if (preguntasDisponibles.isNotEmpty()) {
                                    mostrarDialogoPreguntas = true
                                } else {
                                    mensaje = "No tienes preguntas disponibles"
                                }
                            },
                            enabled = preguntasDisponibles.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hacer Pregunta (${preguntasDisponibles.size})")
                        }
                    } else {
                        // Espacio para jugadores que no están en turno
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                text = if (jugadorActual?.sigueEnJuego == true)
                                    "Espera tu turno..."
                                else "Has sido eliminado",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            juegoController.salirDeSala()
                            onBack()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abandonar Juego")
                    }
                }
            } else {
                // Botones cuando hay ganador
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (esAnfitrion) {
                        Button(
                            onClick = {
                                val exito = juegoController.reiniciarJuego()
                                if (!exito) {
                                    mensaje = "Error al reiniciar el juego"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reiniciar Juego")
                        }

                        Button(
                            onClick = {
                                val exito = juegoController.volverASalaEspera()
                                if (exito) {
                                    onVolverSalaEspera()
                                } else {
                                    mensaje = "Solo el anfitrión puede volver a sala de espera"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Volver a Sala")
                        }
                    }

                    Button(
                        onClick = {
                            juegoController.salirDeSala()
                            onBack()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salir")
                    }
                }

                // Mensaje informativo para jugadores no anfitrión
                if (!esAnfitrion) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Esperando que el anfitrión reinicie el juego o vuelva a la sala de espera...",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // Diálogo para seleccionar emoji
    if (mostrarDialogoEmojis) {
        Dialog(
            onDismissRequest = { mostrarDialogoEmojis = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "¿Cuál es tu emoji?",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val emojis = listOf("😀", "😂", "🥰", "😎", "🤔", "😴", "🥳", "😡", "👻", "🤖")

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(emojis) { emoji ->
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clickable {
                                        jugadorEnTurno?.let { jugador ->
                                            val adivinoCorrecto = juegoController.procesarAdivinanza(jugador.id, emoji)

                                            mensaje = if (adivinoCorrecto) {
                                                "¡Correcto! Sigue en el juego"
                                            } else {
                                                "Incorrecto. Has sido eliminado"
                                            }

                                            mostrarDialogoEmojis = false
                                        }
                                    }
                            )
                        }
                    }

                    Button(
                        onClick = { mostrarDialogoEmojis = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }

    // Diálogo para seleccionar pregunta
    if (mostrarDialogoPreguntas) {
        Dialog(
            onDismissRequest = { mostrarDialogoPreguntas = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Selecciona una pregunta",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Puedes usar cada pregunta solo una vez por turno",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(preguntasDisponibles) { pregunta ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val exito = juegoController.usarPregunta(jugadorActual?.id ?: "", pregunta)
                                        if (exito) {
                                            mensaje = "Pregunta enviada al chat"
                                            mostrarDialogoPreguntas = false
                                        } else {
                                            mensaje = "Error al usar la pregunta"
                                        }
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Text(
                                    text = pregunta,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { mostrarDialogoPreguntas = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}