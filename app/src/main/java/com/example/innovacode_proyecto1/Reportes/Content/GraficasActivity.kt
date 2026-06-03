package com.example.innovacode_proyecto1.Reportes.Content

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.utils.FormatoUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class GraficasActivity : AppCompatActivity() {

    private lateinit var graficaVentas: BarChart
    private lateinit var graficaInventario: LineChart
    private lateinit var graficaPie: PieChart
    private lateinit var tvTotalVentas: TextView
    private lateinit var tvTotalCompras: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var periodoActual = "hoy"
    private var empresaId     = "empresa_demo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graficas)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarGraficas)) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(view.paddingLeft, statusBar.top, view.paddingRight, view.paddingBottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        inicializarVistas()
        configurarGraficas()
        configurarListeners()
        obtenerEmpresaId()
    }

    private fun obtenerEmpresaId() {
        val uid = auth.currentUser?.uid ?: run { cargarDatos(); return }
        db.collection("usuarios").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                empresaId = doc.getString("empresa_id") ?: "empresa_demo"
                cargarDatos()
            }
            .addOnFailureListener { cargarDatos() }
    }

    private fun inicializarVistas() {
        graficaVentas     = findViewById(R.id.graficaVentas)
        graficaInventario = findViewById(R.id.graficaInventario)
        graficaPie        = findViewById(R.id.graficaProductos)
        tvTotalVentas     = findViewById(R.id.tvTotalVentas)
        tvTotalCompras    = findViewById(R.id.tvTotalCompras)
    }

    private fun configurarGraficas() {
        listOf(graficaVentas, graficaInventario, graficaPie).forEach { chart ->
            chart.setBackgroundColor(Color.TRANSPARENT)
            chart.description.isEnabled = false
            chart.legend.textColor      = Color.parseColor("#A0A8C0")
            chart.setNoDataTextColor(Color.parseColor("#6B7280"))
            chart.setNoDataText("Sin datos para este período")
        }

        graficaVentas.apply {
            xAxis.apply {
                textColor     = Color.parseColor("#A0A8C0")
                position      = XAxis.XAxisPosition.BOTTOM
                gridColor     = Color.parseColor("#1E1E35")
                axisLineColor = Color.parseColor("#1E1E35")
                granularity   = 1f
            }
            axisLeft.apply {
                textColor = Color.parseColor("#A0A8C0")
                gridColor = Color.parseColor("#1E1E35")
            }
            axisRight.isEnabled = false
            setFitBars(true)
        }

        graficaInventario.apply {
            xAxis.apply {
                textColor = Color.parseColor("#A0A8C0")
                position  = XAxis.XAxisPosition.BOTTOM
                gridColor = Color.parseColor("#1E1E35")
            }
            axisLeft.apply {
                textColor = Color.parseColor("#A0A8C0")
                gridColor = Color.parseColor("#1E1E35")
            }
            axisRight.isEnabled = false
        }

        graficaPie.apply {
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            holeRadius       = 40f
            setHoleColor(Color.TRANSPARENT)
            legend.textColor = Color.parseColor("#A0A8C0")
        }
    }

    private fun configurarListeners() {
        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener { finish() }

        val btnHoy    = findViewById<Button>(R.id.btnHoy)
        val btnSemana = findViewById<Button>(R.id.btnSemana)
        val btnMes    = findViewById<Button>(R.id.btnMes)

        btnHoy.setOnClickListener {
            periodoActual = "hoy"
            resaltarBoton(btnHoy, btnSemana, btnMes)
            cargarDatos()
        }
        btnSemana.setOnClickListener {
            periodoActual = "semana"
            resaltarBoton(btnSemana, btnHoy, btnMes)
            cargarDatos()
        }
        btnMes.setOnClickListener {
            periodoActual = "mes"
            resaltarBoton(btnMes, btnHoy, btnSemana)
            cargarDatos()
        }
    }

    private fun resaltarBoton(activo: Button, vararg inactivos: Button) {
        activo.setBackgroundResource(R.drawable.fondo_boton_violeta)
        activo.setTextColor(Color.WHITE)
        inactivos.forEach {
            it.setBackgroundResource(R.drawable.fondo_boton_outline)
            it.setTextColor(Color.parseColor("#A0A8C0"))
        }
    }

    private fun cargarDatos() {
        val ahora = System.currentTimeMillis()
        val desde = when (periodoActual) {
            "hoy" -> Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            "semana" -> ahora - (7L * 24 * 60 * 60 * 1000)
            "mes"    -> ahora - (30L * 24 * 60 * 60 * 1000)
            else     -> ahora - (24L * 60 * 60 * 1000)
        }
        cargarGraficaVentas(desde)
        cargarGraficaMovimientos(desde)
        cargarGraficaProductos(desde)
    }

    // ── Ventas por día usando precio_total ────────────────────────────────────
    private fun cargarGraficaVentas(desde: Long) {
        db.collection("ventas")
            .whereEqualTo("empresa_id", empresaId)
            .get()
            .addOnSuccessListener { snap ->
                val ventasPorDia = mutableMapOf<String, Double>()
                var totalVentas  = 0.0

                snap.documents.forEach { doc ->
                    val total = doc.getDouble("precio_total") ?: 0.0
                    val fecha = doc.getString("fecha") ?: return@forEach
                    ventasPorDia[fecha] = (ventasPorDia[fecha] ?: 0.0) + total
                    totalVentas += total
                }

                tvTotalVentas.text = FormatoUtils.formatearPrecio(totalVentas)

                if (ventasPorDia.isEmpty()) {
                    graficaVentas.clear()
                    graficaVentas.invalidate()
                    return@addOnSuccessListener
                }

                val etiquetas = ventasPorDia.keys.toList()
                val entradas  = ventasPorDia.values.mapIndexed { i, v ->
                    BarEntry(i.toFloat(), v.toFloat())
                }

                val dataset = BarDataSet(entradas, "Ventas ($)").apply {
                    color          = Color.parseColor("#6C63FF")
                    valueTextColor = Color.parseColor("#A0A8C0")
                    valueTextSize  = 9f
                }

                graficaVentas.xAxis.valueFormatter = IndexAxisValueFormatter(etiquetas)
                graficaVentas.data = BarData(dataset)
                graficaVentas.animateY(600)
                graficaVentas.invalidate()
            }
    }

    // ── Movimientos de inventario ─────────────────────────────────────────────
    private fun cargarGraficaMovimientos(desde: Long) {
        db.collection("movimientos_inventario")
            .whereEqualTo("empresa_id", empresaId)
            .whereGreaterThan("fecha", desde)
            .get()
            .addOnSuccessListener { snap ->
                var totalMovimientos = 0.0
                val entradasLinea    = mutableListOf<Entry>()
                val salidasLinea     = mutableListOf<Entry>()

                snap.documents.forEachIndexed { i, doc ->
                    val cantidad = doc.getDouble("cantidad") ?: 0.0
                    val tipo     = doc.getString("tipo") ?: ""
                    totalMovimientos += cantidad
                    if (tipo == "entrada") entradasLinea.add(Entry(i.toFloat(), cantidad.toFloat()))
                    else salidasLinea.add(Entry(i.toFloat(), cantidad.toFloat()))
                }

                tvTotalCompras.text = "${totalMovimientos.toInt()} movimientos"

                if (entradasLinea.isEmpty() && salidasLinea.isEmpty()) {
                    graficaInventario.clear()
                    graficaInventario.invalidate()
                    return@addOnSuccessListener
                }

                val datasets = mutableListOf<ILineDataSet>()
                if (entradasLinea.isNotEmpty()) {
                    datasets.add(LineDataSet(entradasLinea, "Entradas").apply {
                        color          = Color.parseColor("#4ADE80")
                        setCircleColor(Color.parseColor("#4ADE80"))   // ← así
                        valueTextColor = Color.parseColor("#A0A8C0")
                        lineWidth      = 2f
                        circleRadius   = 4f
                    })
                }
                if (salidasLinea.isNotEmpty()) {
                    datasets.add(LineDataSet(salidasLinea, "Salidas").apply {
                        color          = Color.parseColor("#F87171")
                        setCircleColor(Color.parseColor("#F87171"))   // ← así
                        valueTextColor = Color.parseColor("#A0A8C0")
                        lineWidth      = 2f
                        circleRadius   = 4f
                    })
                }

                graficaInventario.data = LineData(datasets)
                graficaInventario.animateY(600)
                graficaInventario.invalidate()
            }
    }

    // ── Productos más vendidos usando nombre_producto ─────────────────────────
    private fun cargarGraficaProductos(desde: Long) {
        db.collection("ventas")
            .whereEqualTo("empresa_id", empresaId)
            .get()
            .addOnSuccessListener { snap ->
                val conteo = mutableMapOf<String, Float>()

                snap.documents.forEach { doc ->
                    val producto = doc.getString("nombre_producto") ?: "Desconocido"
                    val cantidad = (doc.getDouble("cantidad") ?: 0.0).toFloat()
                    conteo[producto] = (conteo[producto] ?: 0f) + cantidad
                }

                if (conteo.isEmpty()) {
                    graficaPie.clear()
                    graficaPie.invalidate()
                    return@addOnSuccessListener
                }

                val colores = listOf(
                    Color.parseColor("#6C63FF"), Color.parseColor("#29ABE2"),
                    Color.parseColor("#4ADE80"), Color.parseColor("#F87171"),
                    Color.parseColor("#FBBF24"), Color.parseColor("#FB923C")
                )

                val entradas = conteo.entries
                    .sortedByDescending { it.value }
                    .take(6)
                    .map { PieEntry(it.value, it.key) }

                val dataset = PieDataSet(entradas, "").apply {
                    this.colors    = colores
                    valueTextColor = Color.WHITE
                    valueTextSize  = 11f
                    sliceSpace     = 3f
                }

                graficaPie.data = PieData(dataset)
                graficaPie.animateY(600)
                graficaPie.invalidate()
            }
    }
}