package com.example.innovacode_proyecto1.Compras.Content

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.Compras.Adapter.CompraAdapter
import com.example.innovacode_proyecto1.R
import java.util.Calendar

class ComprasActivity : AppCompatActivity() {

    data class Compra(
        val producto: String,
        val proveedor: String,
        val cantidad: Int,
        val precio: Double,
        val fecha: String
    )

    private val listaCompras = mutableListOf<Compra>()
    private val listaFiltrada = mutableListOf<Compra>()

    private lateinit var adapter: CompraAdapter

    // 🔹 Función para contar compras
    fun obtenerTotalCompras(): Int {
        return listaCompras.size
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_compras)

        // 🔹 Inputs
        val etProducto = findViewById<EditText>(R.id.etProductoCompra)
        val etProveedor = findViewById<EditText>(R.id.etProveedor)
        val etCantidad = findViewById<EditText>(R.id.etCantidadCompra)
        val etPrecio = findViewById<EditText>(R.id.etPrecioCompra)
        val etFecha = findViewById<EditText>(R.id.etFechaCompra)

        // 🔹 DatePicker
        etFecha.setOnClickListener {

            val calendario = Calendar.getInstance()

            val año = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->

                    val fechaSeleccionada =
                        String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)

                    etFecha.setText(fechaSeleccionada)
                },
                año,
                mes,
                dia
            )

            datePicker.show()
        }

        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarCompra)

        // 🔹 Filtros
        val spProducto = findViewById<Spinner>(R.id.spProducto)
        val spProveedor = findViewById<Spinner>(R.id.spProveedor)
        val spFecha = findViewById<Spinner>(R.id.spFecha)

        val btnClearProducto = findViewById<ImageButton>(R.id.btnClearProducto)
        val btnClearProveedor = findViewById<ImageButton>(R.id.btnClearProveedor)
        val btnClearFecha = findViewById<ImageButton>(R.id.btnClearFecha)

        // 🔹 RecyclerView
        val rvCompras = findViewById<RecyclerView>(R.id.rvCompras)
        rvCompras.layoutManager = LinearLayoutManager(this)

        adapter = CompraAdapter(listaFiltrada)
        rvCompras.adapter = adapter

        // 🔹 Cargar filtros
        fun cargarFiltros() {
            val productos = listOf("Todos") + listaCompras.map { it.producto }.distinct()
            val proveedores = listOf("Todos") + listaCompras.map { it.proveedor }.distinct()
            val fechas = listOf("Todos") + listaCompras.map { it.fecha }.distinct()

            spProducto.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productos)
            spProveedor.adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, proveedores)
            spFecha.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fechas)
        }

        // 🔹 Filtrar
        fun filtrar() {
            val productoSel = spProducto.selectedItem?.toString() ?: "Todos"
            val proveedorSel = spProveedor.selectedItem?.toString() ?: "Todos"
            val fechaSel = spFecha.selectedItem?.toString() ?: "Todos"

            listaFiltrada.clear()

            for (compra in listaCompras) {
                if (
                    (productoSel == "Todos" || compra.producto == productoSel) &&
                    (proveedorSel == "Todos" || compra.proveedor == proveedorSel) &&
                    (fechaSel == "Todos" || compra.fecha == fechaSel)
                ) {
                    listaFiltrada.add(compra)
                }
            }

            adapter.actualizarLista(listaFiltrada)
        }

        // 🔹 Listener de filtros
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filtrar()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spProducto.onItemSelectedListener = listener
        spProveedor.onItemSelectedListener = listener
        spFecha.onItemSelectedListener = listener

        // 🔹 Botones limpiar
        btnClearProducto.setOnClickListener { spProducto.setSelection(0) }
        btnClearProveedor.setOnClickListener { spProveedor.setSelection(0) }
        btnClearFecha.setOnClickListener { spFecha.setSelection(0) }

        // 🔹 Registrar compra
        btnRegistrar.setOnClickListener {

            val producto = etProducto.text.toString()
            val proveedor = etProveedor.text.toString()
            val cantidad = etCantidad.text.toString().toIntOrNull() ?: 0
            val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val fecha = etFecha.text.toString()

            // Validaciones
            if (producto.isEmpty() || proveedor.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cantidad <= 0 || precio <= 0.0) {
                Toast.makeText(this, "Cantidad y precio deben ser mayores a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nueva = Compra(producto, proveedor, cantidad, precio, fecha)

            // 🔹 Simulación local (igual que ventas)
            listaCompras.add(nueva)

            // 🔹 Refrescar UI
            cargarFiltros()
            filtrar()

            // 🔹 Limpiar campos
            etProducto.text.clear()
            etProveedor.text.clear()
            etCantidad.text.clear()
            etPrecio.text.clear()
            etFecha.text.clear()

            // 🔹 Reset filtros
            spProducto.setSelection(0)
            spProveedor.setSelection(0)
            spFecha.setSelection(0)

            Toast.makeText(this, "Compra registrada", Toast.LENGTH_SHORT).show()
        }

        // Inicial
        // 🔹 Datos de prueba
        listaCompras.addAll(
            listOf(
                Compra("Arroz", "Proveedor A", 10, 2500.0, "01/05/2026"),
                Compra("Frijoles", "Proveedor B", 5, 3000.0, "02/05/2026"),
                Compra("Arroz", "Proveedor B", 8, 2400.0, "01/05/2026"),
                Compra("Aceite", "Proveedor A", 3, 8000.0, "03/05/2026"),
                Compra("Azúcar", "Proveedor C", 6, 2700.0, "02/05/2026")
            )
        )

// 🔹 Inicializar filtros y lista
        cargarFiltros()
        filtrar()
    }
}