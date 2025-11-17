package com.example.taller2_componentes.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    // CORREGIDO: Usar StateFlow para obtener jugadores en tiempo real
    val jugadores by juegoController.jugadores.collectAsState()
    val codigoSala = juegoController.obtenerCodigoSala()

    // CORREGIDO: Usar StateFlow para obtener el estado de anfitrión en tiempo real
    val esAnfitrion by juegoController.esAnfitrion.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var mostrarMensajeCopiado by remember { mutableStateOf(false) }
    var mostrarErrorIniciar by remember { mutableStateOf(false) }

    // CORREGIDO: Sistema de actualización mejorado usando StateFlow
    LaunchedEffect(key1 = codigoSala) {
        if (codigoSala.isNotEmpty()) {
            while (true) {
                delay(2000) // Mantener conexión activa

                // Verificar si el juego comenzó
                val sala = juegoController.obtenerSala()
                if (sala?.enCurso == true) {
                    println("🚀 Juego detectado como iniciado, navegando...")
                    onIniciarJuego()
                    break
                }
            }
        }
    }

    // Mostrar mensaje "Copiado" temporalmente
    if (mostrarMensajeCopiado) {
        LaunchedEffect(mostrarMensajeCopiado) {
            delay(2000)
            mostrarMensajeCopiado = false
        }
    }

    // Mostrar mensaje de error temporalmente
    if (mostrarErrorIniciar) {
        LaunchedEffect(mostrarErrorIniciar) {
            delay(3000)
            mostrarErrorIniciar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sala de Espera",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            )
        },
        floatingActionButton = {
            // CORREGIDO: Solo mostrar el botón si es anfitrión y hay suficientes jugadores
            if (esAnfitrion && jugadores.size >= 2) {
                FloatingActionButton(
                    onClick = {
                        // CORREGIDO: Verificación doble antes de iniciar
                        if (juegoController.esJugadorActualAnfitrion()) {
                            val exito = juegoController.iniciarJuego()
                            if (exito) {
                                onIniciarJuego()
                            } else {
                                mostrarErrorIniciar = true
                                println("❌ No se pudo iniciar el juego - verificación falló")
                            }
                        } else {
                            mostrarErrorIniciar = true
                            println("❌ No eres el anfitrión, no puedes iniciar el juego")
                        }
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, "Iniciar juego")
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
                    .padding(bottom = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Código de Sala",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = codigoSala,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    if (mostrarMensajeCopiado) {
                        Text(
                            text = "✅ Copiado al portapapeles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = "Comparte este código con otros jugadores",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(codigoSala))
                                mostrarMensajeCopiado = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copiar código",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar")
                        }

                        Button(
                            onClick = {
                                try {
                                    val shareIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(
                                            android.content.Intent.EXTRA_TEXT,
                                            "🎮 Únete a mi sala de Emoji Guess!\n\nCódigo: $codigoSala\n\n¡Descarga la app y únete!"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(
                                        android.content.Intent.createChooser(
                                            shareIntent,
                                            "Compartir código de sala"
                                        )
                                    )
                                } catch (e: Exception) {
                                    clipboardManager.setText(AnnotatedString(codigoSala))
                                    mostrarMensajeCopiado = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Compartir código",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compartir")
                        }
                    }
                }
            }

            // Información de estado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jugadores: ${jugadores.size}",
                    style = MaterialTheme.typography.titleMedium
                )

                // Indicador de estado
                Text(
                    text = if (esAnfitrion) "👑 Anfitrión" else "🎮 Jugador",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (esAnfitrion) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mensaje de error
            if (mostrarErrorIniciar) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "❌ Solo el anfitrión puede iniciar el juego",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Mensaje informativo para anfitrión
            if (esAnfitrion) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (jugadores.size >= 2)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = if (jugadores.size >= 2) {
                            "✅ Listo para comenzar! Toca 'Iniciar Juego'"
                        } else {
                            "⏳ Esperando más jugadores... (mínimo 2)"
                        },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Lista de jugadores
            Text(
                text = "Jugadores en sala:",
                style = MaterialTheme.typography.titleSmall,
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
                                jugador.esAnfitrion -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = jugador.nombre,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (jugador.esAnfitrion) {
                                    Text(
                                        text = "Anfitrión",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            if (jugador.esAnfitrion) {
                                Text("👑", style = MaterialTheme.typography.bodyLarge)
                            } else {
                                Text("🎮", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            // Mensaje para invitados
            if (!esAnfitrion) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📱 Eres un jugador invitado",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Esperando a que el anfitrión inicie el juego...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Información de conexión
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "🔄 Conectado en tiempo real...",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón de salir
            OutlinedButton(
                onClick = {
                    juegoController.salirDeSala()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Salir de la Sala")
            }
        }
    }
}