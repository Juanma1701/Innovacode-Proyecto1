package com.example.innovacode_proyecto1.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportarExcelUtils {

    data class DatosReporte(
        val totalVentas: Double                 = 0.0,
        val totalCompras: Double                = 0.0,
        val ventasPorDia: Map<String, Double>   = emptyMap(),
        val movimientos: List<Map<String, Any>> = emptyList(),
        val productos: List<Map<String, Any>>   = emptyList(),
        val proveedores: List<Map<String, Any>> = emptyList(),
        val periodo: String                     = "Mes actual",
        val empresaNombre: String               = "NovaCore"
    )

    fun exportar(context: Context, datos: DatosReporte) {
        try {
            val wb = XSSFWorkbook()
            crearHojaResumen(wb, datos)
            crearHojaVentas(wb, datos)
            crearHojaMovimientos(wb, datos)
            crearHojaInventario(wb, datos)
            crearHojaProveedores(wb, datos)
            guardarYCompartir(context, wb, datos.empresaNombre)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generando Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── ESTILOS — solo IndexedColors, sin XSSFColor custom ───────────────────

    private fun estiloEncabezado(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        val font  = wb.createFont()
        font.bold = true
        font.color = IndexedColors.WHITE.index
        font.fontHeightInPoints = 12
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.VIOLET.index
        style.fillPattern  = FillPatternType.SOLID_FOREGROUND
        style.alignment    = HorizontalAlignment.CENTER
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderTop(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        return style
    }

    private fun estiloDato(wb: XSSFWorkbook, alterno: Boolean = false): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        if (alterno) {
            style.fillForegroundColor = IndexedColors.LAVENDER.index
            style.fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        val font = wb.createFont()
        font.fontHeightInPoints = 11
        style.setFont(font)
        return style
    }

    private fun estiloMoneda(wb: XSSFWorkbook, alterno: Boolean = false): XSSFCellStyle {
        val style = estiloDato(wb, alterno) as XSSFCellStyle
        val fmt   = wb.createDataFormat()
        style.dataFormat = fmt.getFormat("#,##0.00")
        return style
    }

    private fun estiloTitulo(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        val font  = wb.createFont()
        font.bold = true
        font.fontHeightInPoints = 16
        font.color = IndexedColors.WHITE.index
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.VIOLET.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment   = HorizontalAlignment.CENTER
        return style
    }

    private fun estiloCritico(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        style.fillForegroundColor = IndexedColors.ROSE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        val font = wb.createFont()
        font.color = IndexedColors.RED.index
        font.bold  = true
        style.setFont(font)
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        return style
    }

    private fun estiloOk(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        style.fillForegroundColor = IndexedColors.LIGHT_GREEN.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        val font = wb.createFont()
        font.color = IndexedColors.GREEN.index
        font.bold  = true
        style.setFont(font)
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        return style
    }

    private fun estiloTotal(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        style.fillForegroundColor = IndexedColors.VIOLET.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        val font = wb.createFont()
        font.color = IndexedColors.WHITE.index
        font.bold  = true
        style.setFont(font)
        return style
    }

    // ── HOJA 1: RESUMEN ───────────────────────────────────────────────────────
    private fun crearHojaResumen(wb: XSSFWorkbook, datos: DatosReporte) {
        val sheet = wb.createSheet("Resumen")
        sheet.setColumnWidth(0, 8000)
        sheet.setColumnWidth(1, 6000)

        val rowTitulo = sheet.createRow(0)
        rowTitulo.heightInPoints = 30f
        val cellTitulo = rowTitulo.createCell(0)
        cellTitulo.setCellValue("${datos.empresaNombre} — Reporte ERP")
        cellTitulo.cellStyle = estiloTitulo(wb)
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 1))

        sheet.createRow(1).apply {
            createCell(0).setCellValue("Período:")
            createCell(1).setCellValue(datos.periodo)
        }
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        sheet.createRow(2).apply {
            createCell(0).setCellValue("Generado:")
            createCell(1).setCellValue(fecha)
        }
        sheet.createRow(3)

        val enc = estiloEncabezado(wb)
        sheet.createRow(4).apply {
            createCell(0).apply { setCellValue("Métrica"); cellStyle = enc }
            createCell(1).apply { setCellValue("Valor");   cellStyle = enc }
        }

        val metricas = listOf(
            "Total Ventas"      to FormatoUtils.formatearPrecio(datos.totalVentas),
            "Total Compras"     to FormatoUtils.formatearPrecio(datos.totalCompras),
            "Total Productos"   to "${datos.productos.size}",
            "Total Proveedores" to "${datos.proveedores.size}",
            "Stock Critico"     to "${datos.productos.count {
                val c = (it["cantidad"] as? Long) ?: 0L
                val m = (it["stock_min"] as? Long) ?: 0L
                c <= m
            }} producto(s)"
        )

        metricas.forEachIndexed { i, (label, valor) ->
            sheet.createRow(5 + i).apply {
                createCell(0).apply { setCellValue(label); cellStyle = estiloDato(wb, i % 2 == 0) }
                createCell(1).apply { setCellValue(valor); cellStyle = estiloDato(wb, i % 2 == 0) }
            }
        }
    }

    // ── HOJA 2: VENTAS ────────────────────────────────────────────────────────
    private fun crearHojaVentas(wb: XSSFWorkbook, datos: DatosReporte) {
        val sheet = wb.createSheet("Ventas")
        sheet.setColumnWidth(0, 5000)
        sheet.setColumnWidth(1, 5000)

        val enc = estiloEncabezado(wb)
        sheet.createRow(0).apply {
            createCell(0).apply { setCellValue("Fecha");        cellStyle = enc }
            createCell(1).apply { setCellValue("Total Ventas"); cellStyle = enc }
        }

        datos.ventasPorDia.entries.sortedBy { it.key }.forEachIndexed { i, (fecha, total) ->
            sheet.createRow(i + 1).apply {
                createCell(0).apply { setCellValue(fecha); cellStyle = estiloDato(wb, i % 2 == 0) }
                createCell(1).apply { setCellValue(total); cellStyle = estiloMoneda(wb, i % 2 == 0) }
            }
        }

        val et = estiloTotal(wb)
        sheet.createRow(datos.ventasPorDia.size + 2).apply {
            createCell(0).apply { setCellValue("TOTAL");           cellStyle = et }
            createCell(1).apply { setCellValue(datos.totalVentas); cellStyle = et }
        }
    }

    // ── HOJA 3: MOVIMIENTOS ───────────────────────────────────────────────────
    private fun crearHojaMovimientos(wb: XSSFWorkbook, datos: DatosReporte) {
        val sheet = wb.createSheet("Movimientos")
        listOf(6000, 4000, 4000, 5000).forEachIndexed { i, w -> sheet.setColumnWidth(i, w) }

        val enc = estiloEncabezado(wb)
        sheet.createRow(0).apply {
            listOf("Producto", "Tipo", "Cantidad", "Fecha").forEachIndexed { i, label ->
                createCell(i).apply { setCellValue(label); cellStyle = enc }
            }
        }

        datos.movimientos.forEachIndexed { i, mov ->
            val nombre   = (mov["nombreProducto"] as? String) ?: "-"
            val tipo     = (mov["tipo"] as? String) ?: "-"
            val cantidad = (mov["cantidad"] as? Long) ?: 0L
            val fecha    = try {
                val ts = mov["fecha"] as? com.google.firebase.Timestamp
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(ts?.toDate() ?: Date())
            } catch (e: Exception) { "-" }

            val alt = i % 2 == 0
            sheet.createRow(i + 1).apply {
                createCell(0).apply { setCellValue(nombre);             cellStyle = estiloDato(wb, alt) }
                createCell(1).apply {
                    setCellValue(tipo.replaceFirstChar { it.uppercase() })
                    cellStyle = if (tipo == "entrada") estiloOk(wb) else estiloCritico(wb)
                }
                createCell(2).apply { setCellValue(cantidad.toDouble()); cellStyle = estiloDato(wb, alt) }
                createCell(3).apply { setCellValue(fecha);              cellStyle = estiloDato(wb, alt) }
            }
        }
    }

    // ── HOJA 4: INVENTARIO ────────────────────────────────────────────────────
    private fun crearHojaInventario(wb: XSSFWorkbook, datos: DatosReporte) {
        val sheet = wb.createSheet("Inventario")
        listOf(6000, 4000, 4000, 5000, 4000).forEachIndexed { i, w -> sheet.setColumnWidth(i, w) }

        val enc = estiloEncabezado(wb)
        sheet.createRow(0).apply {
            listOf("Producto", "Cantidad", "Stock Min.", "Precio U.", "Estado").forEachIndexed { i, label ->
                createCell(i).apply { setCellValue(label); cellStyle = enc }
            }
        }

        datos.productos.forEachIndexed { i, prod ->
            val nombre   = (prod["nombre_producto"] as? String) ?: "-"
            val cantidad = (prod["cantidad"] as? Long) ?: 0L
            val stockMin = (prod["stock_min"] as? Long) ?: 0L
            val precio   = (prod["precio_u"] as? Double) ?: 0.0
            val critico  = cantidad <= stockMin
            val alt      = i % 2 == 0

            sheet.createRow(i + 1).apply {
                createCell(0).apply { setCellValue(nombre);              cellStyle = estiloDato(wb, alt) }
                createCell(1).apply { setCellValue(cantidad.toDouble()); cellStyle = estiloDato(wb, alt) }
                createCell(2).apply { setCellValue(stockMin.toDouble()); cellStyle = estiloDato(wb, alt) }
                createCell(3).apply { setCellValue(precio);              cellStyle = estiloMoneda(wb, alt) }
                createCell(4).apply {
                    setCellValue(if (critico) "Critico" else "OK")
                    cellStyle = if (critico) estiloCritico(wb) else estiloOk(wb)
                }
            }
        }
    }

    // ── HOJA 5: PROVEEDORES ───────────────────────────────────────────────────
    private fun crearHojaProveedores(wb: XSSFWorkbook, datos: DatosReporte) {
        val sheet = wb.createSheet("Proveedores")
        listOf(6000, 4000, 5000, 7000).forEachIndexed { i, w -> sheet.setColumnWidth(i, w) }

        val enc = estiloEncabezado(wb)
        sheet.createRow(0).apply {
            listOf("Nombre", "NIT", "Contacto", "Email").forEachIndexed { i, label ->
                createCell(i).apply { setCellValue(label); cellStyle = enc }
            }
        }

        datos.proveedores.forEachIndexed { i, prov ->
            val alt = i % 2 == 0
            sheet.createRow(i + 1).apply {
                createCell(0).apply { setCellValue((prov["nombre"]   as? String) ?: "-"); cellStyle = estiloDato(wb, alt) }
                createCell(1).apply { setCellValue((prov["nit"]      as? String) ?: "-"); cellStyle = estiloDato(wb, alt) }
                createCell(2).apply { setCellValue((prov["contacto"] as? String) ?: "-"); cellStyle = estiloDato(wb, alt) }
                createCell(3).apply { setCellValue((prov["email"]    as? String) ?: "-"); cellStyle = estiloDato(wb, alt) }
            }
        }
    }

    // ── GUARDAR Y COMPARTIR ───────────────────────────────────────────────────
    private fun guardarYCompartir(context: Context, wb: XSSFWorkbook, empresaNombre: String) {
        val fecha    = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "${empresaNombre}_Reporte_$fecha.xlsx"
        val dir      = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "NovaCore")
        dir.mkdirs()
        val file = File(dir, fileName)

        FileOutputStream(file).use { wb.write(it) }
        wb.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type     = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte $empresaNombre ERP")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte Excel"))
        Toast.makeText(context, "Excel generado: $fileName", Toast.LENGTH_SHORT).show()
    }
}