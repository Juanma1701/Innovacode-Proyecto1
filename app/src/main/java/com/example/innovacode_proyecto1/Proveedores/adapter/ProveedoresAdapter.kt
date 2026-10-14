package com.example.innovacode_proyecto1.Proveedores.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.Proveedores.Proveedor

class ProveedoresAdapter(
    private val proveedores: MutableList<Proveedor>,
    private val onEditar: (Proveedor) -> Unit,
    private val onEliminar: (Proveedor) -> Unit
) : RecyclerView.Adapter<ProveedoresAdapter.ProveedorViewHolder>() {

    inner class ProveedorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView       = itemView.findViewById(R.id.tvNombreProveedor)
        val tvNit: TextView          = itemView.findViewById(R.id.tvNitProveedor)
        val tvContacto: TextView     = itemView.findViewById(R.id.tvContactoProveedor)
        val tvEmail: TextView        = itemView.findViewById(R.id.tvEmailProveedor)
        val btnEditar: ImageButton   = itemView.findViewById(R.id.btnEditarProveedor)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarProveedor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProveedorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proveedor, parent, false)
        return ProveedorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProveedorViewHolder, position: Int) {
        val proveedor = proveedores[position]

        holder.tvNombre.text   = proveedor.nombre.ifEmpty { "Sin nombre" }
        holder.tvNit.text      = if (proveedor.nit.isNotEmpty()) "NIT: ${proveedor.nit}" else "NIT: —"
        holder.tvContacto.text = if (proveedor.contacto.isNotEmpty()) "📞 ${proveedor.contacto}" else "📞 Sin contacto"
        holder.tvEmail.text    = if (proveedor.email.isNotEmpty()) "✉️ ${proveedor.email}" else "✉️ Sin email"

        holder.btnEditar.setOnClickListener   { onEditar(proveedor) }
        holder.btnEliminar.setOnClickListener { onEliminar(proveedor) }
    }

    override fun getItemCount(): Int = proveedores.size

    // ── Helpers para actualizar la lista sin recargar Firestore ──────────────
    fun actualizarLista(nuevaLista: List<Proveedor>) {
        proveedores.clear()
        proveedores.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    fun eliminarItem(proveedor: Proveedor) {
        val index = proveedores.indexOfFirst { it.id == proveedor.id }
        if (index != -1) {
            proveedores.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}