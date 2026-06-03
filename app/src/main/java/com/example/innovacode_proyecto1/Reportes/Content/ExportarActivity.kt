package com.example.innovacode_proyecto1.Reportes.Content

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.utils.ExportarExcelUtils
import com.example.innovacode_proyecto1.utils.ExportarPDFUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ExportarActivity : AppCompatActivity() {

    private lateinit var btnVolver: ImageButton
    private lateinit var btnExportarPDF: Button
    private lateinit var btnExportarExcel: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEstado: TextView
    private lateinit var layoutOpciones: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var empresaId     = "empresa_demo"
    private var empresaNombre = "NovaCore"
    private var periodoActual = "mes"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exportar)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        inicializarVistas()
        obtenerEmpresaId()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarExportar)) { view, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(view.paddingLeft, sb.top, view.paddingRight, view.paddingBottom)
            insets
        }
    }

    private fun inicializarVistas() {
        btnVolver       = findViewById(R.id.btnVolver)
        btnExportarPDF  = findViewById(R.id.btnExportarPDF)
        btnExportarExcel= findViewById(R.id.btnExportarExcel)
        progressBar     = findViewById(R.id.progressExportar)
        tvEstado        = findViewById(R.id.tvEstadoExportar)
        layoutOpciones  = findViewById(R.id.layoutOpcionesPeriodo)

        btnVolver.setOnClickListener { finish() }

        // Botones de período
        val btnHoy    = findViewById<Button>(R.id.btnPeriodoHoy)
        val btnSemana = findViewById<Button>(R.id.btnPeriodoSemana)
        val btnMes    = findViewById<Button>(R.id.btnPeriodoMes)

        btnHoy.setOnClickListener {
            periodoActual = "hoy"
            resaltarBoton(btnHoy, btnSemana, btnMes)
        }
        btnSemana.setOnClickListener {
            periodoActual = "semana"
            resaltarBoton(btnSemana, btnHoy, btnMes)
        }
        btnMes.setOnClickListener {
            periodoActual = "mes"
            resaltarBoton(btnMes, btnHoy, btnSemana)
        }

        btnExportarPDF.setOnClickListener   { recolectarDatosYExportar(formato = "pdf") }
        btnExportarExcel.setOnClickListener { recolectarDatosYExportar(formato = "excel") }
    }

    private fun obtenerEmpresaId() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                empresaId     = doc.getString("empresa_id")    ?: "empresa_demo"
                empresaNombre = doc.getString("nombre_empresa") ?: "NovaCore"
            }
    }

    private fun resaltarBoton(activo: Button, vararg inactivos: Button) {
        activo.setBackgroundResource(R.drawable.fondo_boton_violeta)
        activo.setTextColor(android.graphics.Color.WHITE)
        inactivos.forEach {
            it.setBackgroundResource(R.drawable.fondo_boton_outline)
            it.setTextColor(android.graphics.Color.parseColor("#A0A8C0"))
        }
    }

    // ── Recolectar todos los datos de Firestore ───────────────────────────────
    private fun recolectarDatosYExportar(formato: String) {
        mostrarCargando(true, "Recolectando datos...")
        btnExportarPDF.isEnabled   = false
        btnExportarExcel.isEnabled = false

        val ahora = System.currentTimeMillis()
        val desde = when (periodoActual) {
            "hoy"    -> Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);       set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            "semana" -> ahora - (7L * 24 * 60 * 60 * 1000)
            else     -> ahora - (30L * 24 * 60 * 60 * 1000)
        }
        val periodoLabel = when (periodoActual) {
            "hoy"    -> "Hoy"
            "semana" -> "Última semana"
            else     -> "Último mes"
        }

        // Acumulador de datos
        var totalVentas             = 0.0
        var totalCompras            = 0.0
        val ventasPorDia            = mutableMapOf<String, Double>()
        val movimientos             = mutableListOf<Map<String, Any>>()
        var productos               = listOf<Map<String, Any>>()
        var proveedores             = listOf<Map<String, Any>>()
        var pendientes              = 4  // 4 consultas paralelas

        fun intentarExportar() {
            pendientes--
            if (pendientes > 0) return

            mostrarCargando(true, "Generando archivo...")

            val datosPDF = ExportarPDFUtils.DatosReporte(
                totalVentas   = totalVentas,
                totalCompras  = totalCompras,
                ventasPorDia  = ventasPorDia,
                movimientos   = movimientos,
                productos     = productos,
                proveedores   = proveedores,
                periodo       = periodoLabel,
                empresaNombre = empresaNombre
            )

            val datosExcel = ExportarExcelUtils.DatosReporte(
                totalVentas   = totalVentas,
                totalCompras  = totalCompras,
                ventasPorDia  = ventasPorDia,
                movimientos   = movimientos,
                productos     = productos,
                proveedores   = proveedores,
                periodo       = periodoLabel,
                empresaNombre = empresaNombre
            )

            when (formato) {
                "pdf"   -> ExportarPDFUtils.exportar(this, datosPDF)
                "excel" -> ExportarExcelUtils.exportar(this, datosExcel)
            }

            mostrarCargando(false)
            btnExportarPDF.isEnabled   = true
            btnExportarExcel.isEnabled = true
        }

        // ── Consulta 1: Ventas ────────────────────────────────────────────────
        db.collection("ventas")
            .whereEqualTo("empresa_id", empresaId)
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { doc ->
                    val total = doc.getDouble("precio_total") ?: 0.0
                    val fecha = doc.getString("fecha") ?: return@forEach
                    ventasPorDia[fecha] = (ventasPorDia[fecha] ?: 0.0) + total
                    totalVentas += total
                }
                intentarExportar()
            }
            .addOnFailureListener { intentarExportar() }

        // ── Consulta 2: Movimientos ───────────────────────────────────────────
        db.collection("movimientos_inventario")
            .whereEqualTo("empresa_id", empresaId)
            .whereGreaterThan("fecha", desde)
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { doc ->
                    val cantidad = (doc.getDouble("cantidad") ?: 0.0)
                    val tipo     = doc.getString("tipo") ?: "entrada"
                    if (tipo == "compra" || tipo == "entrada") totalCompras += cantidad
                    movimientos.add(mapOf(
                        "nombreProducto" to (doc.getString("nombre_producto") ?: "-"),
                        "tipo"           to tipo,
                        "cantidad"       to (cantidad.toLong()),
                        "fecha"          to (doc.getTimestamp("fecha") ?: com.google.firebase.Timestamp.now())
                    ))
                }
                intentarExportar()
            }
            .addOnFailureListener { intentarExportar() }

        // ── Consulta 3: Inventario ────────────────────────────────────────────
        db.collection("inventario")
            .whereEqualTo("empresa_id", empresaId)
            .get()
            .addOnSuccessListener { snap ->
                productos = snap.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.also { it["id"] = doc.id }
                }
                intentarExportar()
            }
            .addOnFailureListener { intentarExportar() }

        // ── Consulta 4: Proveedores ───────────────────────────────────────────
        db.collection("proveedores")
            .whereEqualTo("empresa_id", empresaId)
            .get()
            .addOnSuccessListener { snap ->
                proveedores = snap.documents.mapNotNull { it.data }
                intentarExportar()
            }
            .addOnFailureListener { intentarExportar() }
    }

    private fun mostrarCargando(cargando: Boolean, mensaje: String = "") {
        progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
        tvEstado.visibility    = if (cargando) View.VISIBLE else View.GONE
        tvEstado.text          = mensaje
    }
}