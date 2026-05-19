package com.example.innovacode_proyecto1.adapter_reportes.Content



import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class MovimientosAdapter(
    private val movimientos: MutableList<Map<String, Any>>
) : RecyclerView.Adapter<MovimientosAdapter.MovimientoViewHolder>() {

    inner class MovimientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTipoIcono: TextView      = itemView.findViewById(R.id.tvTipoIcono)
        val tvNombreProducto: TextView = itemView.findViewById(R.id.tvNombreProducto)
        val tvTipoMovimiento: TextView = itemView.findViewById(R.id.tvTipoMovimiento)
        val tvCantidad: TextView       = itemView.findViewById(R.id.tvCantidad)
        val tvFecha: TextView          = itemView.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reportes, parent, false)
        return MovimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) {
        val mov = movimientos[position]

        val tipo          = mov["tipo"] as? String ?: "entrada"
        val nombreProducto = mov["nombreProducto"] as? String ?: "Sin nombre"
        val cantidad      = (mov["cantidad"] as? Long) ?: 0L
        val fecha         = mov["fecha"] as? Timestamp

        val esEntrada = tipo.lowercase() == "entrada"

        if (esEntrada) {
            holder.tvTipoIcono.text = "↑"
            holder.tvTipoIcono.setTextColor(Color.parseColor("#4ADE80"))
            holder.tvCantidad.text = "+$cantidad und"
            holder.tvCantidad.setTextColor(Color.parseColor("#4ADE80"))
        } else {
            holder.tvTipoIcono.text = "↓"
            holder.tvTipoIcono.setTextColor(Color.parseColor("#FF6B6B"))
            holder.tvCantidad.text = "-$cantidad und"
            holder.tvCantidad.setTextColor(Color.parseColor("#FF6B6B"))
        }

        holder.tvNombreProducto.text  = nombreProducto
        holder.tvTipoMovimiento.text  = if (esEntrada) "Entrada de stock" else "Salida de stock"

        if (fecha != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            holder.tvFecha.text = sdf.format(fecha.toDate())
        } else {
            holder.tvFecha.text = "--/--/----"
        }
    }

    override fun getItemCount(): Int = movimientos.size
}