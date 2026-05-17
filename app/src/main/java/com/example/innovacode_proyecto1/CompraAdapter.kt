package com.example.innovacode_proyecto1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CompraAdapter(private var lista: List<ComprasActivity.Compra>) :
    RecyclerView.Adapter<CompraAdapter.ViewHolder>() {

    // 🔹 1. ViewHolder (conecta XML con Kotlin)
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProducto: TextView = view.findViewById(R.id.tvProducto)
        val tvProveedor: TextView = view.findViewById(R.id.tvProveedor)
        val tvCantidad: TextView = view.findViewById(R.id.tvCantidad)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    // 🔹 2. Crear la vista (usa item_compra.xml)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compra, parent, false)

        return ViewHolder(vista)
    }

    // 🔹 3. Cuántos items hay
    override fun getItemCount(): Int {
        return lista.size
    }

    // 🔹 4. Conectar datos con la vista
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val compra = lista[position]

        holder.tvProducto.text = compra.producto
        holder.tvProveedor.text = "Proveedor: ${compra.proveedor}"
        holder.tvCantidad.text = "Cantidad: ${compra.cantidad}"
        holder.tvPrecio.text = "$${compra.precio}"
        holder.tvFecha.text = "Fecha: ${compra.fecha}"
    }

    // 🔹 5. Actualizar lista (CLAVE para filtros)
    fun actualizarLista(nuevaLista: List<ComprasActivity.Compra>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}