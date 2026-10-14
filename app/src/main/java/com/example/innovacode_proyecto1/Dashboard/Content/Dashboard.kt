package com.example.innovacode_proyecto1.Dashboard.Content

// ─── Módulos del proyecto ─────────────────────────────────────────────────
import com.example.innovacode_proyecto1.Inventario.Content.Inventario
import com.example.innovacode_proyecto1.Perfil.Content.Perfil
import com.example.innovacode_proyecto1.Ventas.Content.MainVentas
import com.example.innovacode_proyecto1.Proveedores.Content.ProveedoresActivity
import com.example.innovacode_proyecto1.recetas.RecetasActivity
import com.example.innovacode_proyecto1.ComprasActivity
import com.example.innovacode_proyecto1.Reportes.Content.Reportes
import com.example.innovacode_proyecto1.alertas.Content.Alertas
import com.example.innovacode_proyecto1.R

// ─── Android / Jetpack ────────────────────────────────────────────────────
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// ─── Firebase ─────────────────────────────────────────────────────────────
import com.google.firebase.firestore.FirebaseFirestore

// ─── Kotlin ───────────────────────────────────────────────────────────────
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard principal de NovaCore.
 *
 * Módulos activos:
 *  1. Alertas expandidas  → stock mínimo, productos próximos a vencer, ventas inusuales
 *  2. KPIs del mes        → margen, proyección, top producto, mejor día
 *  3. Meta mensual        → ProgressBar editable, guardada en Firestore colección "metas"
 */
class Dashboard : AppCompatActivity() {

    // ── Firestore ─────────────────────────────────────────────────────────
    private val db = FirebaseFirestore.getInstance()
    private val EMPRESA_ID = "empresa_demo"

    // ── Formato moneda COP ────────────────────────────────────────────────
    private val fmt: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    // ── Vistas — Alertas ──────────────────────────────────────────────────
    private lateinit var seccionAlertas: LinearLayout
    private lateinit var cardAlertas: LinearLayout
    private lateinit var tvResumenAlertas: TextView
    private lateinit var cardAlertasVencimiento: LinearLayout
    private lateinit var tvResumenVencimiento: TextView
    private lateinit var cardAlertasVentasInusuales: LinearLayout
    private lateinit var tvResumenVentasInusuales: TextView

    // ── Vistas — KPIs ─────────────────────────────────────────────────────
    private lateinit var tvKpiMargen: TextView
    private lateinit var tvKpiProyeccion: TextView
    private lateinit var tvKpiTopProducto: TextView
    private lateinit var tvKpiMejorDia: TextView

    // ── Vistas — Meta ─────────────────────────────────────────────────────
    private lateinit var progressMeta: ProgressBar
    private lateinit var tvMetaActual: TextView
    private lateinit var tvMetaObjetivo: TextView
    private lateinit var tvMetaPorcentaje: TextView
    private lateinit var tvMetaFaltante: TextView
    private lateinit var btnEditarMeta: ImageButton

    // ── Estado interno ────────────────────────────────────────────────────
    private var ventasMesActual: Double = 0.0   // ventas totales del mes
    private var comprasMesActual: Double = 0.0  // compras totales del mes
    private var metaObjetivo: Double = 0.0      // meta guardada en Firestore

    // ═════════════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        bindViews()
        bindNavigation()
        btnEditarMeta.setOnClickListener { mostrarDialogoMeta() }
    }

    override fun onResume() {
        super.onResume()
        cargarTodo()
    }

    // ═════════════════════════════════════════════════════════════════════
    // BINDING
    // ═════════════════════════════════════════════════════════════════════

    private fun bindViews() {
        // Alertas
        seccionAlertas              = findViewById(R.id.seccionAlertas)
        cardAlertas                 = findViewById(R.id.cardAlertas)
        tvResumenAlertas            = findViewById(R.id.tvResumenAlertas)
        cardAlertasVencimiento      = findViewById(R.id.cardAlertasVencimiento)
        tvResumenVencimiento        = findViewById(R.id.tvResumenVencimiento)
        cardAlertasVentasInusuales  = findViewById(R.id.cardAlertasVentasInusuales)
        tvResumenVentasInusuales    = findViewById(R.id.tvResumenVentasInusuales)
        // KPIs
        tvKpiMargen      = findViewById(R.id.tvKpiMargen)
        tvKpiProyeccion  = findViewById(R.id.tvKpiProyeccion)
        tvKpiTopProducto = findViewById(R.id.tvKpiTopProducto)
        tvKpiMejorDia    = findViewById(R.id.tvKpiMejorDia)
        // Meta
        progressMeta      = findViewById(R.id.progressMeta)
        tvMetaActual      = findViewById(R.id.tvMetaActual)
        tvMetaObjetivo    = findViewById(R.id.tvMetaObjetivo)
        tvMetaPorcentaje  = findViewById(R.id.tvMetaPorcentaje)
        tvMetaFaltante    = findViewById(R.id.tvMetaFaltante)
        btnEditarMeta     = findViewById(R.id.btnEditarMeta)
    }

    private fun bindNavigation() {
        findViewById<Button>(R.id.btninvent).setOnClickListener {
            startActivity(Intent(this, Inventario::class.java))
        }
        findViewById<Button>(R.id.btnreportes).setOnClickListener {
            startActivity(Intent(this, Reportes::class.java))
        }
        findViewById<ImageButton>(R.id.btnperfil).setOnClickListener {
            startActivity(Intent(this, Perfil::class.java))
        }
        findViewById<Button>(R.id.btncompras).setOnClickListener {
            startActivity(Intent(this, ComprasActivity::class.java))
        }
        findViewById<Button>(R.id.btnventas).setOnClickListener {
            startActivity(Intent(this, MainVentas::class.java))
        }
        findViewById<Button>(R.id.btnproveedores).setOnClickListener {
            startActivity(Intent(this, ProveedoresActivity::class.java))
        }
        findViewById<Button>(R.id.btnrecetas).setOnClickListener {
            startActivity(Intent(this, RecetasActivity::class.java))
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // CARGA PRINCIPAL
    // ═════════════════════════════════════════════════════════════════════

    private fun cargarTodo() {
        verificarAlertasStock()
        verificarAlertasVencimiento()
        cargarKpisYMeta()
        cargarMeta()
    }

    // ═════════════════════════════════════════════════════════════════════
    // MÓDULO 1 — ALERTAS
    // ═════════════════════════════════════════════════════════════════════

    /** Alerta 1: productos con cantidad <= stock_min */
    private fun verificarAlertasStock() {
        db.collection("inventario")
            .whereEqualTo("empresa_id", EMPRESA_ID)
            .get()
            .addOnSuccessListener { docs ->
                val criticos = docs.count { doc ->
                    val cantidad = doc.getLong("cantidad") ?: 0
                    val stockMin = doc.getLong("stock_min") ?: 0
                    cantidad <= stockMin
                }
                if (criticos > 0) {
                    cardAlertas.visibility = View.VISIBLE
                    tvResumenAlertas.text = "$criticos producto(s) por agotarse → Ver"
                    cardAlertas.setOnClickListener {
                        startActivity(Intent(this, Alertas::class.java))
                    }
                } else {
                    cardAlertas.visibility = View.GONE
                }
                actualizarVisibilidadSeccion()
            }
    }

    /**
     * Alerta 2: productos con fecha_vencimiento en los próximos 7 días.
     * El campo en Firestore es "fecha_vencimiento" (String con formato "yyyy-MM-dd").
     * Si el campo no existe en un documento, se ignora silenciosamente.
     */
    private fun verificarAlertasVencimiento() {
        val sdf    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoy    = Calendar.getInstance()
        val limite = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }

        db.collection("inventario")
            .whereEqualTo("empresa_id", EMPRESA_ID)
            .get()
            .addOnSuccessListener { docs ->
                val proximos = docs.filter { doc ->
                    val fechaStr = doc.getString("fecha_vencimiento") ?: return@filter false
                    try {
                        val fecha = sdf.parse(fechaStr) ?: return@filter false
                        val cal   = Calendar.getInstance().apply { time = fecha }
                        cal.after(hoy) && cal.before(limite)
                    } catch (e: Exception) { false }
                }
                if (proximos.isNotEmpty()) {
                    cardAlertasVencimiento.visibility = View.VISIBLE
                    tvResumenVencimiento.text =
                        "${proximos.size} producto(s) vencen en menos de 7 días"
                } else {
                    cardAlertasVencimiento.visibility = View.GONE
                }
                actualizarVisibilidadSeccion()
            }
    }

    /** Actualiza visibilidad del contenedor padre de alertas */
    private fun actualizarVisibilidadSeccion() {
        val hayAlertas = listOf(cardAlertas, cardAlertasVencimiento, cardAlertasVentasInusuales)
            .any { it.visibility == View.VISIBLE }
        seccionAlertas.visibility = if (hayAlertas) View.VISIBLE else View.GONE
    }

    // ═════════════════════════════════════════════════════════════════════
    // MÓDULO 2 — KPIs  +  MÓDULO 3 ALERTA VENTAS INUSUALES
    // Se calculan juntos porque ambos leen la colección "ventas".
    // ═════════════════════════════════════════════════════════════════════

    private fun cargarKpisYMeta() {
        // Rango del mes actual
        val cal       = Calendar.getInstance()
        val anio      = cal.get(Calendar.YEAR)
        val mes       = cal.get(Calendar.MONTH) + 1          // 1-based
        val diasMes   = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val diaActual = cal.get(Calendar.DAY_OF_MONTH)

        val prefixMes = "%04d-%02d".format(anio, mes)        // ej. "2025-06"

        // ── Ventas del mes ────────────────────────────────────────────────
        db.collection("ventas")
            .whereEqualTo("empresa_id", EMPRESA_ID)
            .get()
            .addOnSuccessListener { ventasDocs ->

                // Filtrar las del mes actual (campo "fecha" → "yyyy-MM-dd ..." o Timestamp)
                val ventasMes = ventasDocs.filter { doc ->
                    obtenerFechaString(doc)?.startsWith(prefixMes) == true
                }

                // Acumular total ventas
                ventasMesActual = ventasMes.sumOf { doc ->
                    (doc.getDouble("precio_total") ?: 0.0)
                }

                // ── Alerta 3: ventas inusuales ─────────────────────────────
                detectarVentasInusuales(ventasMes.map {
                    it.getDouble("precio_total") ?: 0.0
                })

                // ── Top producto ───────────────────────────────────────────
                val productoConteo = mutableMapOf<String, Double>()
                ventasMes.forEach { doc ->
                    val nombre = doc.getString("nombre_producto") ?: "?"
                    val total  = doc.getDouble("precio_total") ?: 0.0
                    productoConteo[nombre] = (productoConteo[nombre] ?: 0.0) + total
                }
                val topProducto = productoConteo.maxByOrNull { it.value }?.key ?: "—"
                tvKpiTopProducto.text = topProducto

                // ── Mejor día de la semana ─────────────────────────────────
                val diasSemanaEs = mapOf(
                    Calendar.MONDAY    to "Lunes",
                    Calendar.TUESDAY   to "Martes",
                    Calendar.WEDNESDAY to "Miércoles",
                    Calendar.THURSDAY  to "Jueves",
                    Calendar.FRIDAY    to "Viernes",
                    Calendar.SATURDAY  to "Sábado",
                    Calendar.SUNDAY    to "Domingo"
                )
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val ventasPorDia = mutableMapOf<Int, Double>()
                ventasMes.forEach { doc ->
                    val fechaStr = obtenerFechaString(doc) ?: return@forEach
                    try {
                        val date = sdf.parse(fechaStr.take(10)) ?: return@forEach
                        val c    = Calendar.getInstance().apply { time = date }
                        val dow  = c.get(Calendar.DAY_OF_WEEK)
                        val tot  = doc.getDouble("precio_total") ?: 0.0
                        ventasPorDia[dow] = (ventasPorDia[dow] ?: 0.0) + tot
                    } catch (_: Exception) {}
                }
                val mejorDow = ventasPorDia.maxByOrNull { it.value }?.key
                tvKpiMejorDia.text = if (mejorDow != null) diasSemanaEs[mejorDow] ?: "—" else "—"

                // ── Proyección lineal del mes ──────────────────────────────
                val proyeccion = if (diaActual > 0)
                    (ventasMesActual / diaActual) * diasMes
                else 0.0
                tvKpiProyeccion.text = fmt.format(proyeccion)

                // ── Cargar compras para calcular margen ────────────────────
                cargarComprasYMargen(prefixMes)

                // ── Actualizar barra de meta (ventas ya disponibles) ───────
                actualizarBarraMeta()
            }
    }

    private fun cargarComprasYMargen(prefixMes: String) {
        db.collection("compras")
            .whereEqualTo("empresa_id", EMPRESA_ID)
            .get()
            .addOnSuccessListener { comprasDocs ->
                comprasMesActual = comprasDocs
                    .filter { obtenerFechaString(it)?.startsWith(prefixMes) == true }
                    .sumOf { it.getDouble("precio_total") ?: 0.0 }

                val margen = if (ventasMesActual > 0) {
                    ((ventasMesActual - comprasMesActual) / ventasMesActual) * 100
                } else 0.0

                tvKpiMargen.text = "%.1f%%".format(margen)
                // Color dinámico: verde si > 20%, amarillo si > 0%, rojo si negativo
                tvKpiMargen.setTextColor(
                    resources.getColor(
                        when {
                            margen >= 20 -> android.R.color.holo_green_light
                            margen > 0   -> android.R.color.holo_orange_light
                            else         -> android.R.color.holo_red_light
                        }, theme
                    )
                )
            }
    }

    /**
     * Detecta ventas individuales cuyo precio_total > promedio * 3.
     * Muestra alerta si hay al menos una.
     */
    private fun detectarVentasInusuales(totales: List<Double>) {
        if (totales.isEmpty()) {
            cardAlertasVentasInusuales.visibility = View.GONE
            return
        }
        val promedio = totales.average()
        val umbral   = promedio * 3
        val inusuales = totales.count { it > umbral }

        if (inusuales > 0) {
            cardAlertasVentasInusuales.visibility = View.VISIBLE
            tvResumenVentasInusuales.text =
                "$inusuales venta(s) inusual(es) detectada(s) este mes"
        } else {
            cardAlertasVentasInusuales.visibility = View.GONE
        }
        actualizarVisibilidadSeccion()
    }

    // ═════════════════════════════════════════════════════════════════════
    // MÓDULO 3 — META MENSUAL
    // Colección: "metas"  Documento: "empresa_demo_<yyyy-MM>"
    // Campos: empresa_id, meta, periodo
    // ═════════════════════════════════════════════════════════════════════

    private fun cargarMeta() {
        val periodo = periodoActual()
        db.collection("metas")
            .document("${EMPRESA_ID}_$periodo")
            .get()
            .addOnSuccessListener { doc ->
                metaObjetivo = doc.getDouble("meta") ?: 0.0
                actualizarBarraMeta()
            }
    }

    private fun actualizarBarraMeta() {
        tvMetaActual.text   = fmt.format(ventasMesActual)
        tvMetaObjetivo.text = fmt.format(metaObjetivo)

        if (metaObjetivo > 0) {
            val pct = ((ventasMesActual / metaObjetivo) * 100).toInt().coerceIn(0, 100)
            progressMeta.progress = pct
            tvMetaPorcentaje.text  = "$pct%"
            val faltante = (metaObjetivo - ventasMesActual).coerceAtLeast(0.0)
            tvMetaFaltante.text = if (faltante > 0) "Faltan ${fmt.format(faltante)}" else "¡Meta alcanzada! 🎉"
        } else {
            progressMeta.progress = 0
            tvMetaPorcentaje.text  = "0%"
            tvMetaFaltante.text    = "Sin meta configurada"
        }
    }

    /** Diálogo para editar la meta. Guarda en Firestore. */
    private fun mostrarDialogoMeta() {
        val input = EditText(this).apply {
            hint        = "Ej: 5000000"
            inputType   = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(48, 32, 48, 32)
            if (metaObjetivo > 0) setText(metaObjetivo.toLong().toString())
        }

        AlertDialog.Builder(this)
            .setTitle("🎯 Meta mensual de ventas")
            .setMessage("Ingresa el objetivo de ventas para este mes (COP):")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevaMeta = input.text.toString().toDoubleOrNull()
                if (nuevaMeta != null && nuevaMeta > 0) {
                    guardarMeta(nuevaMeta)
                } else {
                    Toast.makeText(this, "Ingresa un valor válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarMeta(valor: Double) {
        val periodo = periodoActual()
        val data = mapOf(
            "empresa_id" to EMPRESA_ID,
            "meta"       to valor,
            "periodo"    to periodo
        )
        db.collection("metas")
            .document("${EMPRESA_ID}_$periodo")
            .set(data)
            .addOnSuccessListener {
                metaObjetivo = valor
                actualizarBarraMeta()
                Toast.makeText(this, "✅ Meta actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar la meta", Toast.LENGTH_SHORT).show()
            }
    }

    // ═════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═════════════════════════════════════════════════════════════════════

    /** Devuelve "yyyy-MM" del mes en curso */
    private fun periodoActual(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }

    /**
     * Extrae la fecha como String de un documento Firestore.
     * Soporta campo "fecha" como String ("yyyy-MM-dd ...") o como Timestamp.
     */
    private fun obtenerFechaString(doc: com.google.firebase.firestore.DocumentSnapshot): String? {
        val raw = doc.get("fecha")
        return when (raw) {
            is String -> raw
            is com.google.firebase.Timestamp -> {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(raw.toDate())
            }
            else -> null
        }
    }
}