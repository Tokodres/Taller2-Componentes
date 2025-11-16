package com.example.taller2_componentes.repositorio

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.taller2_componentes.modelo.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseRepository {
    private val database = FirebaseDatabase.getInstance()
    private val salasRef = database.getReference("salas")
    private val mensajesRef = database.getReference("mensajes")

    // Sala functions
    fun crearSala(sala: Sala): Flow<String> = callbackFlow {
        val key = salasRef.push().key
        if (key != null) {
            salasRef.child(key).setValue(sala).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(key)
                } else {
                    close(task.exception ?: Exception("Error desconocido"))
                }
            }
        }
        awaitClose()
    }

    fun obtenerSala(salaId: String): Flow<Sala> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sala = snapshot.getValue(Sala::class.java)
                if (sala != null) {
                    trySend(sala)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        salasRef.child(salaId).addValueEventListener(listener)
        awaitClose { salasRef.child(salaId).removeEventListener(listener) }
    }

    fun actualizarSala(salaId: String, sala: Sala): Flow<Boolean> = callbackFlow {
        salasRef.child(salaId).setValue(sala).addOnCompleteListener { task ->
            trySend(task.isSuccessful)
            close()
        }
        awaitClose()
    }

    // Chat functions
    fun enviarMensaje(salaId: String, mensaje: MensajeChat): Flow<Boolean> = callbackFlow {
        val key = mensajesRef.child(salaId).push().key
        if (key != null) {
            mensajesRef.child(salaId).child(key).setValue(mensaje).addOnCompleteListener { task ->
                trySend(task.isSuccessful)
                close()
            }
        }
        awaitClose()
    }

    fun obtenerMensajes(salaId: String): Flow<List<MensajeChat>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mensajes = snapshot.children.mapNotNull { it.getValue(MensajeChat::class.java) }
                trySend(mensajes)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        mensajesRef.child(salaId).addValueEventListener(listener)
        awaitClose { mensajesRef.child(salaId).removeEventListener(listener) }
    }

    // Eliminar sala cuando el juego termina
    fun eliminarSala(salaId: String): Flow<Boolean> = callbackFlow {
        salasRef.child(salaId).removeValue().addOnCompleteListener { task ->
            trySend(task.isSuccessful)
            close()
        }
        awaitClose()
    }
}