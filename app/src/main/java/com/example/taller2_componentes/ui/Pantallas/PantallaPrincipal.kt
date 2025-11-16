package com.example.taller2_componentes.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PantallaPrincipal(
    onCrearSala: () -> Unit,
    onUnirseSala: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎮 Emoji Guess",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Adivina tu emoji secreto",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Botón Crear Sala
        Button(
            onClick = onCrearSala,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Crear Sala", style = MaterialTheme.typography.titleMedium)
                Text("Ser anfitrión", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Botón Unirse a Sala
        OutlinedButton(
            onClick = onUnirseSala,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Unirse a Sala", style = MaterialTheme.typography.titleMedium)
                Text("Con código", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Instrucciones
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cómo jugar:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "• Crea una sala o únete con código\n" +
                            "• Cada jugador tiene un emoji secreto\n" +
                            "• Adivina tu emoji en tu turno\n" +
                            "• Último jugador en pie gana",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }
        }
    }
}