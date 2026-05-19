package com.example.innovacode_proyecto1.adapter_inventario.Content

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp
import com.example.innovacode_proyecto1.R

class ProductoAdapter(
    private val lista: MutableList<Map<String, Any>>,
    private val onEntradaClick: (productoId: String, nombre: String) -> Unit,
    private val onSalidaClick: (productoId: String, nombre: String, cantidadActual: Long) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProducto   : TextView = itemView.findViewById(R.id.tvProducto)
        val tvCantidad   : TextView = itemView.findViewById(R.id.tvCantidad)
        val tvPrecio     : TextView = itemView.findViewById(R.id.tvPrecio)
        val tvFecha      : TextView = itemView.findViewById(R.id.tvFecha)
        val tvCategoria  : TextView = itemView.findViewById(R.id.tvCategoria)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcio)

        val btnEntrada   : Button = itemView.findViewById(R.id.btnRegistrarEntrada)
        val btnSalida: Button = itemView.findViewById(R.id.btnRegistrarSalida)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventario, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = lista[position]

        holder.tvProducto.text    = producto["nombre_producto"]?.toString() ?: "Sin nombre"
        holder.tvCantidad.text    = "Cantidad: ${producto["cantidad"] ?: 0}"
        holder.tvPrecio.text      = "$ ${producto["precio_u"] ?: 0.0}"
        holder.tvCategoria.text   = "Categoría: ${producto["categoria"] ?: "-"}"
        holder.tvDescripcion.text = "Descripción: ${producto["descripcion"] ?: "-"}"

        // Formatea el Timestamp de Firebase a fecha legible
        val timestamp = producto["fecha_creacion"] as? Timestamp
        if (timestamp != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            holder.tvFecha.text = "Fecha: ${sdf.format(timestamp.toDate())}"
        } else {
            holder.tvFecha.text = "Fecha: -"
        }
        holder.btnEntrada.setOnClickListener {
            val productoId = lista[position]["id"]?.toString() ?: return@setOnClickListener
            val nombre     = lista[position]["nombre_producto"]?.toString() ?: ""
            onEntradaClick(productoId, nombre)
        }
        holder.btnSalida.setOnClickListener {
            val productoId = lista[position]["id"]?.toString() ?: return@setOnClickListener
            val nombre     = lista[position]["nombre_producto"]?.toString() ?: ""
            val cantidadActual = (lista[position]["cantidad"] as? Long) ?: 0L
            onSalidaClick(productoId, nombre, cantidadActual)
        }
    }

    override fun getItemCount(): Int = lista.size

    // Llamas esto desde el Activity cuando llegan los datos de Firestore
    fun actualizarLista(nuevaLista: List<Map<String, Any>>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}