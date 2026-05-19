package com.example.innovacode_proyecto1.Reportes.Content



import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.adapter_reportes.Content.MovimientosAdapter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class Reportes : AppCompatActivity() {

    private lateinit var tvTotalProductos: TextView
    private lateinit var tvStockCritico: TextView
    private lateinit var tvValorInventario: TextView
    private lateinit var layoutTopProductos: LinearLayout
    private lateinit var rvMovimientos: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout

    private val db = FirebaseFirestore.getInstance()
    private val EMPRESA_ID = "empresa_demo"

    private val listaMovimientos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: MovimientosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        tvTotalProductos   = findViewById(R.id.tvTotalProductos)
        tvStockCritico     = findViewById(R.id.tvStockCritico)
        tvValorInventario  = findViewById(R.id.tvValorInventario)
        layoutTopProductos = findViewById(R.id.layoutTopProductos)
        rvMovimientos      = findViewById(R.id.rvMovimientos)
        layoutEmptyState   = findViewById(R.id.layoutEmptyState)

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        rvMovimientos.layoutManager = LinearLayoutManager(this)
        adapter = MovimientosAdapter(listaMovimientos)
        rvMovimientos.adapter = adapter

        cargarMetricas()
        cargarHistorialMovimientos()
    }


    private fun cargarMetricas() {
        db.collection("inventario")
            .whereEqualTo("empresa_id", EMPRESA_ID)
            .get()
            .addOnSuccessListener { result ->
                var totalProductos = 0
                var stockCritico   = 0
                var valorTotal     = 0.0

                for (doc in result) {
                    totalProductos++
                    val cantidad  = doc.getLong("cantidad") ?: 0L
                    val stockMin  = doc.getLong("stock_min") ?: 0L
                    val precioU   = doc.getDouble("precio_u") ?: 0.0

                    if (cantidad <= stockMin) stockCritico++
                    valorTotal += cantidad * precioU
                }

                tvTotalProductos.text = totalProductos.toString()
                tvStockCritico.text   = stockCritico.toString()

                val formato = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                tvValorInventario.text = formato.format(valorTotal)

                // Cargar top productos después de tener los productos
                cargarTopProductos(result.documents.map { it.id to (it.getString("nombre_producto") ?: "Sin nombre") })
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    // ── SECCIÓN 2: PRODUCTOS MÁS ACTIVOS ────────────────────────────────────
    private fun cargarTopProductos(productos: List<Pair<String, String>>) {
        layoutTopProductos.removeAllViews()

        if (productos.isEmpty()) {
            agregarTextoVacio("Sin productos disponibles")
            return
        }

        val conteoMovimientos = mutableMapOf<String, Int>()  // productoId → conteo
        val nombresProductos  = mutableMapOf<String, String>() // productoId → nombre
        var consultasPendientes = productos.size

        for ((productoId, nombreProducto) in productos) {
            nombresProductos[productoId] = nombreProducto

            db.collection("inventario")
                .document(productoId)
                .collection("movimientos")
                .get()
                .addOnSuccessListener { movResult ->
                    conteoMovimientos[productoId] = movResult.size()
                    consultasPendientes--


                    if (consultasPendientes == 0) {
                        mostrarTopProductos(conteoMovimientos, nombresProductos)
                    }
                }
                .addOnFailureListener {
                    consultasPendientes--
                    if (consultasPendientes == 0) {
                        mostrarTopProductos(conteoMovimientos, nombresProductos)
                    }
                }
        }
    }

    private fun mostrarTopProductos(
        conteo: Map<String, Int>,
        nombres: Map<String, String>
    ) {
        layoutTopProductos.removeAllViews()

        val top3 = conteo.entries
            .sortedByDescending { it.value }
            .take(3)

        if (top3.isEmpty()) {
            agregarTextoVacio("Sin movimientos registrados")
            return
        }

        top3.forEachIndexed { index, entry ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 12, 0, 12)
            }

            val tvPosicion = TextView(this).apply {
                text      = "${index + 1}"
                textSize  = 13f
                setTextColor(Color.parseColor("#9B7FD4"))
                width = 28
            }

            val tvNombre = TextView(this).apply {
                text      = nombres[entry.key] ?: "Producto"
                textSize  = 14f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvConteo = TextView(this).apply {
                text      = "${entry.value} mov."
                textSize  = 13f
                setTextColor(Color.parseColor("#B0B0CC"))
            }

            fila.addView(tvPosicion)
            fila.addView(tvNombre)
            fila.addView(tvConteo)
            layoutTopProductos.addView(fila)

            // Separador excepto en el último
            if (index < top3.size - 1) {
                val separador = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).also { it.setMargins(0, 4, 0, 4) }
                    setBackgroundColor(Color.parseColor("#2A2A3E"))
                }
                layoutTopProductos.addView(separador)
            }
        }
    }

    // ── SECCIÓN 3: HISTORIAL DE MOVIMIENTOS (últimos 7 días) ─────────────────
    private fun cargarHistorialMovimientos() {
        db.collection("inventario")
            .whereEqualTo("empresa_id", EMPRESA_ID)
            .get()
            .addOnSuccessListener { inventarioResult ->
                val productos = inventarioResult.documents
                if (productos.isEmpty()) {
                    mostrarEstadoVacio()
                    return@addOnSuccessListener
                }

                // Fecha límite: hace 7 días
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val fechaLimite = Timestamp(cal.time)

                val movimientosRecolectados = mutableListOf<Map<String, Any>>()
                var pendientes = productos.size

                for (productoDoc in productos) {
                    val nombreProducto = productoDoc.getString("nombre_producto") ?: "Sin nombre"

                    productoDoc.reference
                        .collection("movimientos")
                        .whereGreaterThanOrEqualTo("fecha", fechaLimite)
                        .get()
                        .addOnSuccessListener { movResult ->
                            for (movDoc in movResult) {
                                val movMap = mutableMapOf<String, Any>()
                                movMap["nombreProducto"] = nombreProducto
                                movMap["tipo"]           = movDoc.getString("tipo") ?: "entrada"
                                movMap["cantidad"]       = movDoc.getLong("cantidad") ?: 0L
                                movMap["fecha"]          = movDoc.getTimestamp("fecha") ?: Timestamp.now()
                                movimientosRecolectados.add(movMap)
                            }

                            pendientes--
                            if (pendientes == 0) {
                                // Ordenar por fecha descendente
                                val ordenados = movimientosRecolectados.sortedByDescending {
                                    (it["fecha"] as Timestamp).seconds
                                }

                                listaMovimientos.clear()
                                listaMovimientos.addAll(ordenados)
                                adapter.notifyDataSetChanged()

                                if (listaMovimientos.isEmpty()) {
                                    mostrarEstadoVacio()
                                } else {
                                    layoutEmptyState.visibility = View.GONE
                                    rvMovimientos.visibility    = View.VISIBLE
                                }
                            }
                        }
                        .addOnFailureListener {
                            pendientes--
                            if (pendientes == 0 && listaMovimientos.isEmpty()) {
                                mostrarEstadoVacio()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                mostrarEstadoVacio()
            }
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────
    private fun mostrarEstadoVacio() {
        layoutEmptyState.visibility = View.VISIBLE
        rvMovimientos.visibility    = View.GONE
    }

    private fun agregarTextoVacio(mensaje: String) {
        val tv = TextView(this).apply {
            text = mensaje
            setTextColor(Color.parseColor("#B0B0CC"))
            textSize = 14f
        }
        layoutTopProductos.addView(tv)
    }
}