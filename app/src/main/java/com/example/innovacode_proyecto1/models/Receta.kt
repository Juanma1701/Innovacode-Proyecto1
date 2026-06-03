package com.example.innovacode_proyecto1.models

data class Receta(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val descripcion: String = "",
    val ingredientes: List<IngredienteReceta> = emptyList(),
    val empresa_id: String = "",
    val fechaCreacion: Long = System.currentTimeMillis()
)

data class IngredienteReceta(
    val productoId: String = "",       // ID del documento en inventario
    val nombre_producto: String = "",  // nombre_producto del inventario
    val cantidad: Double = 0.0,        // cuánto se usa por unidad vendida
    val unidad: String = "und"         // g, kg, ml, L, und
)