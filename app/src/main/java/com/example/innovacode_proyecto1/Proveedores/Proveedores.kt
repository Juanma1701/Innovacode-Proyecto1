package com.example.innovacode_proyecto1.Proveedores

import com.google.firebase.Timestamp

/**
 * Modelo mapeado 1:1 con la colección "proveedores" de Firestore
 * Campos: contacto, email, empresa_id, fecha_creacion, nit, nombre
 */
data class Proveedor(
    val id: String = "",
    val nombre: String = "",
    val nit: String = "",
    val contacto: String = "",
    val email: String = "",
    val empresa_id: String = "",
    val fecha_creacion: Timestamp? = null
)