package com.example.innovacode_proyecto1.adapter_alertas.Content

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.R

class AlertasAdapter(
    private val lista: MutableList<Map<String, Any>>
) : RecyclerView.Adapter<AlertasAdapter.AlertaViewHolder>() {

    inner class AlertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre  : TextView = itemView.findViewById(R.id.tvNombreAlerta)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidadAlerta)
        val tvStockMin: TextView = itemView.findViewById(R.id.tvStockMinAlerta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alertas, parent, false)
        return AlertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        val producto = lista[position]
        holder.tvNombre.text   = producto["nombre_producto"].toString()
        holder.tvCantidad.text = "Existencias actuales: ${producto["cantidad"]}"
        holder.tvStockMin.text = "Stock mínimo: ${producto["stock_min"]}"
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<Map<String, Any>>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}