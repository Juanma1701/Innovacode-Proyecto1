package com.example.innovacode_proyecto1.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Utilidad para descontar ingredientes del inventario al registrar una venta.
 *
 * NUEVO: valida que la unidad del ingrediente en la receta coincida con la
 * unidad_medida del producto en inventario antes de descontar.
 * Si no coincide, intenta convertir automáticamente entre unidades compatibles
 * (ej. g ↔ kg, ml ↔ L). Si la conversión es imposible, reporta el error.
 */
object DescuentoInventarioUtils {

    private val db = FirebaseFirestore.getInstance()

    // ── Conversiones entre unidades compatibles ───────────────────────────
    // Retorna el factor para convertir DE `desde` A `hasta`.
    // Ej: de "g" a "kg" → factor = 0.001  (dividir entre 1000)
    // Retorna null si las unidades no son convertibles.
    private fun factorConversion(desde: String, hasta: String): Double? {
        if (desde == hasta) return 1.0

        return when {
            // Masa
            desde == "g"  && hasta == "kg" -> 0.001
            desde == "kg" && hasta == "g"  -> 1000.0
            // Volumen
            desde == "ml" && hasta == "L"  -> 0.001
            desde == "L"  && hasta == "ml" -> 1000.0
            // Cualquier otra combinación no es convertible
            else -> null
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // FUNCIÓN PRINCIPAL
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Cuando se registra una venta de un producto que tiene receta,
     * descuenta automáticamente los ingredientes del inventario.
     *
     * @param nombreProducto  nombre_producto de la venta (ej: "Hamburguesa Clásica")
     * @param cantidadVendida cuántas unidades se vendieron
     * @param empresaId       empresa_id del negocio
     * @param onExito         callback cuando todo salió bien
     * @param onError         callback con mensaje de error
     */
    fun descontarIngredientes(
        nombreProducto: String,
        cantidadVendida: Double,
        empresaId: String,
        onExito: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 1. Buscar la receta del producto vendido
        db.collection("recetas")
            .whereEqualTo("nombre", nombreProducto)
            .whereEqualTo("empresa_id", empresaId)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    // Sin receta → no hay ingredientes que descontar
                    onExito()
                    return@addOnSuccessListener
                }

                val recetaDoc = snap.documents.first()

                @Suppress("UNCHECKED_CAST")
                val ingredientes = recetaDoc.get("ingredientes") as? List<Map<String, Any>>
                    ?: run { onExito(); return@addOnSuccessListener }

                // 2. Para cada ingrediente, verificar unidad y calcular descuento
                procesarIngredientes(
                    ingredientes    = ingredientes,
                    cantidadVendida = cantidadVendida,
                    nombreProducto  = nombreProducto,
                    empresaId       = empresaId,
                    onExito         = onExito,
                    onError         = onError
                )
            }
            .addOnFailureListener { e ->
                onError("Error buscando receta: ${e.message}")
            }
    }

    // ═════════════════════════════════════════════════════════════════════
    // PROCESAMIENTO CON VALIDACIÓN DE UNIDADES
    // ═════════════════════════════════════════════════════════════════════

    private fun procesarIngredientes(
        ingredientes: List<Map<String, Any>>,
        cantidadVendida: Double,
        nombreProducto: String,
        empresaId: String,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Necesitamos leer cada producto del inventario para verificar su unidad_medida.
        // Usamos un contador para saber cuándo terminamos todas las lecturas.
        val total   = ingredientes.size
        var leidos  = 0
        var hayError = false

        // Lista final de descuentos ya validados y convertidos

        val descuentos = mutableListOf<Descuento>()

        ingredientes.forEach { ing ->
            val productoId        = ing["productoId"] as? String ?: run {
                leidos++
                return@forEach
            }
            val nombreIng         = ing["nombre_producto"] as? String ?: ""
            val cantidadReceta    = (ing["cantidad"] as? Double) ?: 0.0
            val unidadReceta      = (ing["unidad"] as? String)?.lowercase() ?: "und"
            val cantidadNecesaria = cantidadReceta * cantidadVendida

            // Leer el producto del inventario para obtener su unidad_medida actual
            db.collection("inventario").document(productoId)
                .get()
                .addOnSuccessListener { doc ->
                    if (!hayError) {
                        val unidadInventario = (doc.getString("unidad_medida") ?: "und").lowercase()
                        val factor = factorConversion(unidadReceta, unidadInventario)

                        if (factor == null) {
                            // Unidades incompatibles → reportar error claro
                            hayError = true
                            onError(
                                "Unidad incompatible en '$nombreIng': " +
                                        "la receta usa '$unidadReceta' pero el inventario maneja '$unidadInventario'. " +
                                        "Edita la receta o el producto para que coincidan."
                            )
                            return@addOnSuccessListener
                        }

                        val cantidadFinal = cantidadNecesaria * factor

                        synchronized(descuentos) {
                            descuentos.add(
                                Descuento(
                                    productoId       = productoId,
                                    nombreProducto   = nombreIng,
                                    cantidadFinal    = cantidadFinal,
                                    unidadInventario = unidadInventario
                                )
                            )
                        }
                    }

                    leidos++
                    if (leidos == total && !hayError) {
                        // Todos los ingredientes verificados — ejecutar el batch
                        ejecutarDescuento(
                            descuentos      = descuentos,
                            ingredientes    = ingredientes,
                            nombreProducto  = nombreProducto,
                            cantidadVendida = cantidadVendida,
                            empresaId       = empresaId,
                            onExito         = onExito,
                            onError         = onError
                        )
                    }
                }
                .addOnFailureListener { e ->
                    if (!hayError) {
                        hayError = true
                        onError("Error leyendo inventario: ${e.message}")
                    }
                }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // EJECUTAR BATCH DE DESCUENTOS
    // ═════════════════════════════════════════════════════════════════════

    private data class Descuento(
        val productoId: String,
        val nombreProducto: String,
        val cantidadFinal: Double,
        val unidadInventario: String
    )

    private fun ejecutarDescuento(
        descuentos: List<Descuento>,
        ingredientes: List<Map<String, Any>>,
        nombreProducto: String,
        cantidadVendida: Double,
        empresaId: String,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        val batch = db.batch()

        descuentos.forEach { d ->
            val ref = db.collection("inventario").document(d.productoId)
            batch.update(ref, "cantidad", FieldValue.increment(-d.cantidadFinal))
            batch.update(ref, "fecha_ultimo_movimiento", Timestamp.now())
            batch.update(ref, "ultimo_movimiento", "salida")
        }

        batch.commit()
            .addOnSuccessListener {
                registrarMovimientos(
                    descuentos      = descuentos,
                    nombreProducto  = nombreProducto,
                    cantidadVendida = cantidadVendida,
                    empresaId       = empresaId
                )
                verificarStockMinimo(descuentos, empresaId)
                onExito()
            }
            .addOnFailureListener { e ->
                onError("Error al descontar ingredientes: ${e.message}")
            }
    }

    // ═════════════════════════════════════════════════════════════════════
    // HISTORIAL DE MOVIMIENTOS
    // ═════════════════════════════════════════════════════════════════════

    private fun registrarMovimientos(
        descuentos: List<Descuento>,
        nombreProducto: String,
        cantidadVendida: Double,
        empresaId: String
    ) {
        descuentos.forEach { d ->
            val movimiento = hashMapOf(
                "productoId"      to d.productoId,
                "nombre_producto" to d.nombreProducto,
                "tipo"            to "salida",
                "cantidad"        to d.cantidadFinal,
                "unidad"          to d.unidadInventario,
                "motivo"          to "Venta de $nombreProducto (x$cantidadVendida)",
                "empresa_id"      to empresaId,
                "fecha"           to System.currentTimeMillis()
            )
            db.collection("movimientos_inventario").add(movimiento)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // VERIFICAR STOCK MÍNIMO
    // ═════════════════════════════════════════════════════════════════════

    private fun verificarStockMinimo(descuentos: List<Descuento>, empresaId: String) {
        descuentos.forEach { d ->
            db.collection("inventario").document(d.productoId)
                .get()
                .addOnSuccessListener { doc ->
                    val cantidadActual = doc.getDouble("cantidad") ?: 0.0
                    val stockMin       = doc.getDouble("stock_min") ?: 0.0
                    val nombre         = doc.getString("nombre_producto") ?: ""

                    if (cantidadActual <= stockMin && stockMin > 0) {
                        val alerta = hashMapOf(
                            "productoId"      to d.productoId,
                            "nombre_producto" to nombre,
                            "cantidad_actual" to cantidadActual,
                            "stock_min"       to stockMin,
                            "empresa_id"      to empresaId,
                            "leida"           to false,
                            "fecha"           to System.currentTimeMillis()
                        )
                        db.collection("alertas_stock")
                            .whereEqualTo("productoId", d.productoId)
                            .whereEqualTo("leida", false)
                            .get()
                            .addOnSuccessListener { snap ->
                                if (snap.isEmpty) db.collection("alertas_stock").add(alerta)
                            }
                    }
                }
        }
    }
}