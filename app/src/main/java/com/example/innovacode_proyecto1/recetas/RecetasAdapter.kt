package com.example.innovacode_proyecto1.recetas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.models.Receta
import com.example.innovacode_proyecto1.utils.FormatoUtils

class RecetasAdapter(
    private var recetas: MutableList<Receta>,
    private val onEditar: (Receta) -> Unit,
    private val onEliminar: (Receta) -> Unit
) : RecyclerView.Adapter<RecetasAdapter.ViewHolder>() {

    inner class ViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val tvNombre: TextView               = vista.findViewById(R.id.tvNombreReceta)
        val tvCategoria: TextView            = vista.findViewById(R.id.tvCategoriaReceta)
        val tvDescripcion: TextView          = vista.findViewById(R.id.tvDescripcionReceta)
        val layoutIngredientes: LinearLayout = vista.findViewById(R.id.layoutIngredientes)
        val btnEditar: Button                = vista.findViewById(R.id.btnEditarReceta)
        val btnEliminar: Button              = vista.findViewById(R.id.btnEliminarReceta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receta, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val receta = recetas[position]

        holder.tvNombre.text      = receta.nombre
        holder.tvCategoria.text   = receta.categoria
        holder.tvDescripcion.text = if (receta.descripcion.isNotEmpty())
            receta.descripcion else "Sin descripción"

        // Mostrar ingredientes
        holder.layoutIngredientes.removeAllViews()
        if (receta.ingredientes.isEmpty()) {
            val tv = TextView(holder.itemView.context).apply {
                text     = "Sin ingredientes registrados"
                textSize = 12f
                setTextColor(Color.parseColor("#3D3D5C"))
                setPadding(0, 2, 0, 2)
            }
            holder.layoutIngredientes.addView(tv)
        } else {
            receta.ingredientes.forEach { ing ->
                val tv = TextView(holder.itemView.context).apply {
                    text = "• ${ing.nombre_producto}: ${FormatoUtils.formatearCantidad(ing.cantidad, ing.unidad)}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#A0A8C0"))
                    setPadding(0, 2, 0, 2)
                }
                holder.layoutIngredientes.addView(tv)
            }
        }

        holder.btnEditar.setOnClickListener   { onEditar(receta) }
        holder.btnEliminar.setOnClickListener { onEliminar(receta) }
    }

    override fun getItemCount() = recetas.size

    fun actualizarLista(nuevaLista: List<Receta>) {
        recetas.clear()
        recetas.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}