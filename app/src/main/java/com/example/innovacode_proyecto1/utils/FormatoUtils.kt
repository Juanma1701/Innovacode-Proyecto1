package com.example.innovacode_proyecto1.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatoUtils {

    fun formatearPrecio(valor: Double): String {
        val formato = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        return formato.format(valor)
    }

    fun formatearPrecio(valor: Long): String   = formatearPrecio(valor.toDouble())
    fun formatearPrecio(valor: Int): String    = formatearPrecio(valor.toDouble())
    fun formatearPrecio(valor: Number): String = formatearPrecio(valor.toDouble())

    fun formatearFecha(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO")).format(Date(timestamp))
    }

    fun formatearFechaHora(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CO")).format(Date(timestamp))
    }

    fun formatearCantidad(cantidad: Double, unidad: String): String {
        return when (unidad.lowercase()) {
            "kg"          -> "${"%.3f".format(cantidad)} kg"
            "g"           -> "${"%.0f".format(cantidad)} g"
            "l"           -> "${"%.3f".format(cantidad)} L"
            "ml"          -> "${"%.0f".format(cantidad)} ml"
            "taza"        -> "${"%.1f".format(cantidad)} taza(s)"
            "cucharada"   -> "${"%.1f".format(cantidad)} cda(s)"
            "cucharadita" -> "${"%.1f".format(cantidad)} cdita(s)"
            else          -> "${"%.0f".format(cantidad)} und"
        }
    }
}