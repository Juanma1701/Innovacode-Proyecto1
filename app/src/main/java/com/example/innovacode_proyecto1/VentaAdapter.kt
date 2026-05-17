package com.example.innovacode_proyecto1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VentaAdapter(private var lista: List<MainVentas.Venta>) :
    RecyclerView.Adapter<VentaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProducto: TextView = view.findViewById(R.id.tvProducto)
        val tvCliente: TextView = view.findViewById(R.id.tvCliente)
        val tvCantidad: TextView = view.findViewById(R.id.tvCantidad)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venta, parent, false)
        return ViewHolder(vista)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val venta = lista[position]

        holder.tvProducto.text = venta.producto
        holder.tvCliente.text = "Cliente: ${venta.cliente}"
        holder.tvCantidad.text = "Cantidad: ${venta.cantidad}"
        holder.tvPrecio.text = "$${venta.precio}"
        holder.tvFecha.text = "Fecha: ${venta.fecha}"
    }

    fun actualizarLista(nuevaLista: List<MainVentas.Venta>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}