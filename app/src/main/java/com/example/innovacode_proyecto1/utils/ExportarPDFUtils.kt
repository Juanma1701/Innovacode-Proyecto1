package com.example.innovacode_proyecto1.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportarPDFUtils {

    private const val PAGE_WIDTH  = 595   // A4 puntos
    private const val PAGE_HEIGHT = 842
    private const val MARGIN      = 40f
    private const val LINE_HEIGHT = 22f

    // ── Colores corporativos ──────────────────────────────────────────────────
    private val colorPrimario   = Color.parseColor("#6C63FF")
    private val colorSecundario = Color.parseColor("#29ABE2")
    private val colorTexto      = Color.parseColor("#1A1A2E")
    private val colorSubtexto   = Color.parseColor("#6B7280")
    private val colorFondo      = Color.parseColor("#F8F9FA")
    private val colorLinea      = Color.parseColor("#E5E7EB")
    private val colorVerde      = Color.parseColor("#059669")
    private val colorRojo       = Color.parseColor("#DC2626")

    data class DatosReporte(
        val totalVentas: Double             = 0.0,
        val totalCompras: Double            = 0.0,
        val ventasPorDia: Map<String, Double> = emptyMap(),
        val movimientos: List<Map<String, Any>> = emptyList(),
        val productos: List<Map<String, Any>>   = emptyList(),
        val proveedores: List<Map<String, Any>> = emptyList(),
        val periodo: String                 = "Mes actual",
        val empresaNombre: String           = "NovaCore",
        val barChart: BarChart?             = null,
        val lineChart: LineChart?           = null,
        val pieChart: PieChart?             = null
    )

    fun exportar(context: Context, datos: DatosReporte) {
        try {
            val doc  = PdfDocument()
            var pageNum = 1

            // ── Página 1: Portada + Resumen ───────────────────────────────────
            pageNum = dibujarPortada(doc, datos, pageNum)

            // ── Página 2: Ventas + Gráfica de barras ─────────────────────────
            pageNum = dibujarSeccionVentas(doc, datos, pageNum)

            // ── Página 3: Movimientos inventario + Gráfica línea ─────────────
            pageNum = dibujarSeccionMovimientos(doc, datos, pageNum)

            // ── Página 4: Inventario actual ───────────────────────────────────
            pageNum = dibujarSeccionInventario(doc, datos, pageNum)

            // ── Página 5: Proveedores ─────────────────────────────────────────
            dibujarSeccionProveedores(doc, datos, pageNum)

            // ── Guardar y compartir ───────────────────────────────────────────
            guardarYCompartir(context, doc)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generando PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── PORTADA ───────────────────────────────────────────────────────────────
    private fun dibujarPortada(doc: PdfDocument, datos: DatosReporte, pageNum: Int): Int {
        val page   = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        val canvas = page.canvas

        // Fondo degradado superior
        val gradPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, PAGE_WIDTH.toFloat(), 280f,
                colorPrimario, colorSecundario,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 280f, gradPaint)

        // Logo (círculo blanco con inicial)
        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(PAGE_WIDTH / 2f, 100f, 50f, logoPaint)
        canvas.drawText(
            datos.empresaNombre.first().toString(),
            PAGE_WIDTH / 2f, 115f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color     = colorPrimario
                textSize  = 48f
                textAlign = Paint.Align.CENTER
                typeface  = Typeface.DEFAULT_BOLD
            }
        )

        // Nombre empresa
        canvas.drawText(
            datos.empresaNombre,
            PAGE_WIDTH / 2f, 185f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color     = Color.WHITE
                textSize  = 28f
                textAlign = Paint.Align.CENTER
                typeface  = Typeface.DEFAULT_BOLD
            }
        )

        // Subtítulo
        canvas.drawText(
            "Reporte ERP — ${datos.periodo}",
            PAGE_WIDTH / 2f, 220f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color     = Color.parseColor("#E0D9FF")
                textSize  = 16f
                textAlign = Paint.Align.CENTER
            }
        )

        // Fecha
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText(
            "Generado el $fecha",
            PAGE_WIDTH / 2f, 248f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color     = Color.parseColor("#C4C0FF")
                textSize  = 13f
                textAlign = Paint.Align.CENTER
            }
        )

        // ── Tarjetas resumen ──────────────────────────────────────────────────
        var y = 320f
        canvas.drawText(
            "RESUMEN GENERAL",
            MARGIN, y,
            paintTitulo(colorSubtexto, 11f)
        )
        y += 24f

        val cardW = (PAGE_WIDTH - MARGIN * 2 - 16f) / 2f

        // Card Ventas
        dibujarCard(canvas, MARGIN, y, cardW, 80f, "💰 VENTAS TOTALES",
            FormatoUtils.formatearPrecio(datos.totalVentas), colorVerde)

        // Card Compras
        dibujarCard(canvas, MARGIN + cardW + 16f, y, cardW, 80f, "🛒 COMPRAS TOTALES",
            FormatoUtils.formatearPrecio(datos.totalCompras), colorRojo)

        y += 100f

        // Card Productos
        dibujarCard(canvas, MARGIN, y, cardW, 80f, "📦 PRODUCTOS",
            "${datos.productos.size} registrados", colorPrimario)

        // Card Proveedores
        dibujarCard(canvas, MARGIN + cardW + 16f, y, cardW, 80f, "🏢 PROVEEDORES",
            "${datos.proveedores.size} registrados", colorSecundario)

        // Productos críticos
        val criticos = datos.productos.count {
            val cant = (it["cantidad"] as? Long) ?: 0L
            val min  = (it["stock_min"] as? Long) ?: 0L
            cant <= min
        }
        y += 110f
        if (criticos > 0) {
            val alertPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FEF3C7") }
            canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 50f, 8f, 8f, alertPaint)
            canvas.drawText(
                "⚠️  $criticos producto(s) con stock crítico — revisar inventario",
                MARGIN + 16f, y + 32f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color    = Color.parseColor("#92400E")
                    textSize = 13f
                }
            )
        }

        dibujarPiePagina(canvas, pageNum)
        doc.finishPage(page)
        return pageNum + 1
    }

    // ── VENTAS ────────────────────────────────────────────────────────────────
    private fun dibujarSeccionVentas(doc: PdfDocument, datos: DatosReporte, pageNum: Int): Int {
        val page   = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        val canvas = page.canvas

        var y = dibujarEncabezadoSeccion(canvas, "Ventas por Día", "💰", colorVerde)

        // Tabla ventas
        if (datos.ventasPorDia.isEmpty()) {
            canvas.drawText("Sin datos de ventas para este período.", MARGIN, y + 20f,
                paintNormal(colorSubtexto))
            y += 40f
        } else {
            y = dibujarFilaTabla(canvas, y, listOf("Fecha", "Total"), encabezado = true)
            datos.ventasPorDia.entries.sortedBy { it.key }.forEach { (fecha, total) ->
                y = dibujarFilaTabla(canvas, y, listOf(fecha, FormatoUtils.formatearPrecio(total)))
                if (y > PAGE_HEIGHT - 120f) return@forEach
            }
            y += 16f
            canvas.drawText(
                "TOTAL: ${FormatoUtils.formatearPrecio(datos.totalVentas)}",
                PAGE_WIDTH - MARGIN, y,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color     = colorVerde
                    textSize  = 14f
                    textAlign = Paint.Align.RIGHT
                    typeface  = Typeface.DEFAULT_BOLD
                }
            )
            y += 32f
        }

        // Gráfica de barras
        datos.barChart?.let { chart ->
            y = dibujarGrafica(canvas, chart, y, "Gráfica: Ventas por Día")
        }

        dibujarPiePagina(canvas, pageNum)
        doc.finishPage(page)
        return pageNum + 1
    }

    // ── MOVIMIENTOS ───────────────────────────────────────────────────────────
    private fun dibujarSeccionMovimientos(doc: PdfDocument, datos: DatosReporte, pageNum: Int): Int {
        val page   = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        val canvas = page.canvas

        var y = dibujarEncabezadoSeccion(canvas, "Movimientos de Inventario", "📊", colorSecundario)

        if (datos.movimientos.isEmpty()) {
            canvas.drawText("Sin movimientos registrados.", MARGIN, y + 20f, paintNormal(colorSubtexto))
            y += 40f
        } else {
            y = dibujarFilaTabla(canvas, y,
                listOf("Producto", "Tipo", "Cantidad"), encabezado = true)
            datos.movimientos.take(20).forEach { mov ->
                val nombre   = (mov["nombreProducto"] as? String) ?: "-"
                val tipo     = (mov["tipo"] as? String) ?: "-"
                val cantidad = (mov["cantidad"] as? Long)?.toString() ?: "0"
                val color    = if (tipo == "entrada") colorVerde else colorRojo
                y = dibujarFilaTabla(canvas, y,
                    listOf(nombre.take(20), tipo.replaceFirstChar { it.uppercase() }, cantidad),
                    colorTercera = color)
                if (y > PAGE_HEIGHT - 200f) return@forEach
            }
            y += 16f
        }

        datos.lineChart?.let { chart ->
            y = dibujarGrafica(canvas, chart, y, "Gráfica: Entradas vs Salidas")
        }

        dibujarPiePagina(canvas, pageNum)
        doc.finishPage(page)
        return pageNum + 1
    }

    // ── INVENTARIO ────────────────────────────────────────────────────────────
    private fun dibujarSeccionInventario(doc: PdfDocument, datos: DatosReporte, pageNum: Int): Int {
        val page   = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        val canvas = page.canvas

        var y = dibujarEncabezadoSeccion(canvas, "Inventario Actual", "📦", colorPrimario)

        if (datos.productos.isEmpty()) {
            canvas.drawText("Sin productos registrados.", MARGIN, y + 20f, paintNormal(colorSubtexto))
        } else {
            y = dibujarFilaTabla(canvas, y,
                listOf("Producto", "Cantidad", "Stock Mín.", "Precio U.", "Estado"),
                encabezado = true)

            datos.productos.forEach { prod ->
                val nombre   = (prod["nombre_producto"] as? String) ?: "-"
                val cantidad = (prod["cantidad"] as? Long) ?: 0L
                val stockMin = (prod["stock_min"] as? Long) ?: 0L
                val precio   = (prod["precio_u"] as? Double) ?: 0.0
                val critico  = cantidad <= stockMin
                val estado   = if (critico) "⚠ Crítico" else "✓ OK"
                val colorEst = if (critico) colorRojo else colorVerde

                y = dibujarFilaTabla(canvas, y,
                    listOf(
                        nombre.take(18),
                        cantidad.toString(),
                        stockMin.toString(),
                        FormatoUtils.formatearPrecio(precio),
                        estado
                    ),
                    colorTercera = colorEst, columnaColor = 4)
                if (y > PAGE_HEIGHT - 80f) return@forEach
            }

            // Gráfica pie de productos más vendidos
            datos.pieChart?.let { chart ->
                if (y < PAGE_HEIGHT - 250f) {
                    dibujarGrafica(canvas, chart, y + 16f, "Gráfica: Productos más vendidos")
                }
            }
        }

        dibujarPiePagina(canvas, pageNum)
        doc.finishPage(page)
        return pageNum + 1
    }

    // ── PROVEEDORES ───────────────────────────────────────────────────────────
    private fun dibujarSeccionProveedores(doc: PdfDocument, datos: DatosReporte, pageNum: Int) {
        val page   = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        val canvas = page.canvas

        var y = dibujarEncabezadoSeccion(canvas, "Proveedores", "🏢", colorPrimario)

        if (datos.proveedores.isEmpty()) {
            canvas.drawText("Sin proveedores registrados.", MARGIN, y + 20f, paintNormal(colorSubtexto))
        } else {
            y = dibujarFilaTabla(canvas, y,
                listOf("Nombre", "NIT", "Contacto", "Email"), encabezado = true)

            datos.proveedores.forEach { prov ->
                val nombre   = (prov["nombre"]   as? String) ?: "-"
                val nit      = (prov["nit"]      as? String) ?: "-"
                val contacto = (prov["contacto"] as? String) ?: "-"
                val email    = (prov["email"]    as? String) ?: "-"

                y = dibujarFilaTabla(canvas, y,
                    listOf(nombre.take(16), nit.take(12), contacto.take(12), email.take(18)))
                if (y > PAGE_HEIGHT - 80f) return@forEach
            }
        }

        dibujarPiePagina(canvas, pageNum)
        doc.finishPage(page)
    }

    // ── HELPERS DE DIBUJO ─────────────────────────────────────────────────────

    private fun dibujarEncabezadoSeccion(canvas: Canvas, titulo: String, emoji: String, color: Int): Float {
        // Banda de color
        val paint = Paint().apply { this.color = color }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 6f, paint)

        // Título
        canvas.drawText(
            "$emoji  $titulo",
            MARGIN, 50f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = colorTexto
                textSize   = 22f
                typeface   = Typeface.DEFAULT_BOLD
            }
        )

        // Línea separadora
        val linea = Paint().apply { this.color = colorLinea; strokeWidth = 1f }
        canvas.drawLine(MARGIN, 62f, PAGE_WIDTH - MARGIN, 62f, linea)

        return 80f
    }

    private fun dibujarCard(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float,
        label: String, valor: String, color: Int
    ) {
        // Sombra
        val sombra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#1A000000")
        }
        canvas.drawRoundRect(x + 2f, y + 2f, x + w + 2f, y + h + 2f, 10f, 10f, sombra)

        // Fondo
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE }
        canvas.drawRoundRect(x, y, x + w, y + h, 10f, 10f, fondo)

        // Barra de color izquierda
        val barra = Paint().apply { this.color = color }
        canvas.drawRoundRect(x, y, x + 5f, y + h, 3f, 3f, barra)

        canvas.drawText(label, x + 14f, y + 24f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = colorSubtexto; textSize = 10f
            })
        canvas.drawText(valor, x + 14f, y + 56f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color; textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            })
    }

    private fun dibujarFilaTabla(
        canvas: Canvas, y: Float, celdas: List<String>,
        encabezado: Boolean = false,
        colorTercera: Int = colorTexto,
        columnaColor: Int = -1
    ): Float {
        val colW = (PAGE_WIDTH - MARGIN * 2) / celdas.size
        val fondoPaint = Paint().apply {
            color = if (encabezado) colorPrimario else if (y % (LINE_HEIGHT * 2) < LINE_HEIGHT) colorFondo else Color.WHITE
        }
        canvas.drawRect(MARGIN, y - LINE_HEIGHT + 4f, PAGE_WIDTH - MARGIN, y + 6f, fondoPaint)

        celdas.forEachIndexed { i, texto ->
            val cx = MARGIN + colW * i + 4f
            val useColor = when {
                encabezado    -> Color.WHITE
                i == columnaColor -> colorTercera
                else          -> colorTexto
            }
            canvas.drawText(texto, cx, y,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color    = useColor
                    textSize = if (encabezado) 12f else 11f
                    typeface = if (encabezado) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                })
        }
        return y + LINE_HEIGHT
    }

    private fun dibujarGrafica(canvas: Canvas, chart: Any, y: Float, titulo: String): Float {
        canvas.drawText(titulo, MARGIN, y + 16f, paintTitulo(colorSubtexto, 11f))

        val bitmap = when (chart) {
            is BarChart  -> chart.chartBitmap
            is LineChart -> chart.chartBitmap
            is PieChart  -> chart.chartBitmap
            else         -> return y
        }

        val maxW  = (PAGE_WIDTH - MARGIN * 2).toInt()
        val maxH  = 180
        val scale = minOf(maxW.toFloat() / bitmap.width, maxH.toFloat() / bitmap.height)
        val bw    = (bitmap.width * scale).toInt()
        val bh    = (bitmap.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, bw, bh, true)

        canvas.drawBitmap(scaled, MARGIN, y + 24f, Paint())
        return y + bh + 40f
    }

    private fun dibujarPiePagina(canvas: Canvas, pageNum: Int) {
        val paint = Paint().apply { color = colorLinea; strokeWidth = 1f }
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 30f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 30f, paint)
        canvas.drawText(
            "NovaCore ERP  •  Página $pageNum",
            PAGE_WIDTH / 2f, PAGE_HEIGHT - 14f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color     = colorSubtexto
                textSize  = 10f
                textAlign = Paint.Align.CENTER
            }
        )
    }

    private fun paintNormal(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = 13f
    }

    private fun paintTitulo(color: Int, size: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = size; typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.1f
    }

    // ── GUARDAR Y COMPARTIR ───────────────────────────────────────────────────
    private fun guardarYCompartir(context: Context, doc: PdfDocument) {
        val fecha     = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName  = "NovaCore_Reporte_$fecha.pdf"
        val dir       = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "NovaCore")
        dir.mkdirs()
        val file = File(dir, fileName)

        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type     = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte NovaCore ERP")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte PDF"))
        Toast.makeText(context, "PDF generado: $fileName", Toast.LENGTH_SHORT).show()
    }
}