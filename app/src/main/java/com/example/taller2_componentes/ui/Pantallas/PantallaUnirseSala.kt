package com.example.taller2_componentes.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taller2_componentes.Controlador.JuegoController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaUnirseSala(
    juegoController: JuegoController,
    onBack: () -> Unit,
    onUnirseExitoso: () -> Unit
) {
    var codigoSala by remember { mutableStateOf("") }
    var nombreJugador by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unirse a Sala") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Unirse a Sala Existente",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Ingresa el código de la sala y tu nombre",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (mensajeError.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = mensajeError,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            OutlinedTextField(
                value = codigoSala,
                onValueChange = {
                    codigoSala = it.uppercase().take(6)
                    mensajeError = ""
                },
                label = { Text("Código de sala") },
                placeholder = { Text("Ej: A1B2C3") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = nombreJugador,
                onValueChange = {
                    nombreJugador = it
                    mensajeError = ""
                },
                label = { Text("Tu nombre") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    if (codigoSala.length != 6) {
                        mensajeError = "El código debe tener 6 caracteres"
                        return@Button
                    }

                    if (nombreJugador.length < 2) {
                        mensajeError = "Ingresa un nombre válido (mínimo 2 caracteres)"
                        return@Button
                    }

                    isLoading = true
                    mensajeError = ""

                    // CORREGIDO: Usar callback para manejar resultado asíncrono
                    juegoController.unirseASala(codigoSala, nombreJugador) { exito ->
                        isLoading = false
                        if (exito) {
                            onUnirseExitoso()
                        } else {
                            mensajeError = "No se pudo unir a la sala. Verifica:\n• El código es correcto\n• La sala existe\n• El juego no ha comenzado"
                        }
                    }
                },
                enabled = !isLoading && codigoSala.length == 6 && nombreJugador.length >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Unirse a Sala")
                }
            }

            // Información adicional
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Conectando con la sala...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}