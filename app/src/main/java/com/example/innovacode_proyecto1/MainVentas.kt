package com.example.innovacode_proyecto1

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import android.app.DatePickerDialog

class MainVentas : AppCompatActivity() {

    // 🔹 Modelo
    data class Venta(
        val producto: String,
        val cliente: String,
        val cantidad: Int,
        val precio: Double,
        val fecha: String
    )

    // 🔹 Listas
    private val listaVentas = mutableListOf<Venta>()
    private val listaFiltrada = mutableListOf<Venta>()

    private lateinit var adapter: VentaAdapter

    // este es el contador de registros
    fun obtenerTotalVentas(): Int {
        return listaVentas.size
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_ventas)

        val etProducto = findViewById<EditText>(R.id.etProductoVenta)
        val etCliente = findViewById<EditText>(R.id.etCliente)
        val etCantidad = findViewById<EditText>(R.id.etCantidadVenta)
        val etPrecio = findViewById<EditText>(R.id.etPrecioVenta)
        val etFecha = findViewById<EditText>(R.id.etFechaVenta)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarVenta)

        val calendar = Calendar.getInstance()

        etFecha.setOnClickListener {

            val datePicker = DatePickerDialog(this,
                { _, selectedYear, selectedMonth, selectedDay ->

                    calendar.set(selectedYear, selectedMonth, selectedDay)

                    val fechaFormateada = String.format(
                        "%02d/%02d/%d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )

                    etFecha.setText(fechaFormateada)

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // 🔥 Evitar fechas futuras (muy recomendado)
            datePicker.datePicker.maxDate = System.currentTimeMillis()

            datePicker.show()
        }

        val spProducto = findViewById<Spinner>(R.id.spProductoVenta)
        val spCliente = findViewById<Spinner>(R.id.spCliente)
        val spFecha = findViewById<Spinner>(R.id.spFechaVenta)

        val btnClearProducto = findViewById<ImageButton>(R.id.btnClearProductoVenta)
        val btnClearCliente = findViewById<ImageButton>(R.id.btnClearCliente)
        val btnClearFecha = findViewById<ImageButton>(R.id.btnClearFechaVenta)

        // 🔹 RecyclerView
        val rvVentas = findViewById<RecyclerView>(R.id.rvVentas)
        adapter = VentaAdapter(listaFiltrada)
        rvVentas.layoutManager = LinearLayoutManager(this)
        rvVentas.adapter = adapter

        // 🔹 Datos de prueba
        listaVentas.addAll(
            listOf(
                Venta("Laptop", "Juan", 2, 2500000.0, "01/06/2026"),
                Venta("Mouse", "Ana", 5, 50000.0, "02/06/2026"),
                Venta("Teclado", "Carlos", 3, 120000.0, "03/06/2026"),
                Venta("Monitor", "Juan", 1, 800000.0, "04/06/2026")
            )
        )

        // 🔹 Cargar filtros
        fun cargarFiltros() {
            val productos = listOf("Todos") + listaVentas.map { it.producto }.distinct()
            val clientes = listOf("Todos") + listaVentas.map { it.cliente }.distinct()
            val fechas = listOf("Todos") + listaVentas.map { it.fecha }.distinct()

            val adapterProductos = ArrayAdapter(this, android.R.layout.simple_spinner_item, productos)
            adapterProductos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            val adapterClientes = ArrayAdapter(this, android.R.layout.simple_spinner_item, clientes)
            adapterClientes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            val adapterFechas = ArrayAdapter(this, android.R.layout.simple_spinner_item, fechas)
            adapterFechas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spProducto.adapter = adapterProductos
            spCliente.adapter = adapterClientes
            spFecha.adapter = adapterFechas
        }

        // 🔹 Filtrar
        fun filtrar() {
            val productoSel = spProducto.selectedItem?.toString() ?: "Todos"
            val clienteSel = spCliente.selectedItem?.toString() ?: "Todos"
            val fechaSel = spFecha.selectedItem?.toString() ?: "Todos"

            listaFiltrada.clear()

            for (venta in listaVentas) {
                if (
                    (productoSel == "Todos" || venta.producto == productoSel) &&
                    (clienteSel == "Todos" || venta.cliente == clienteSel) &&
                    (fechaSel == "Todos" || venta.fecha == fechaSel)
                ) {
                    listaFiltrada.add(venta)
                }
            }

            adapter.actualizarLista(listaFiltrada)
        }

        // 🔹 Botón registrar
        btnRegistrar.setOnClickListener {

            val producto = etProducto.text.toString()
            val cliente = etCliente.text.toString()
            val cantidad = etCantidad.text.toString().toIntOrNull() ?: 0
            val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val fecha = etFecha.text.toString()

            if (producto.isEmpty() || cliente.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cantidad <= 0 || precio <= 0.0) {
                Toast.makeText(this, "Cantidad y precio deben ser mayores a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevaVenta = Venta(producto, cliente, cantidad, precio, fecha)

            listaVentas.add(nuevaVenta)

            cargarFiltros()
            filtrar()

            // 🔹 Limpiar campos
            etProducto.text.clear()
            etCliente.text.clear()
            etCantidad.text.clear()
            etPrecio.text.clear()
            etFecha.text.clear()

            // 🔹 Reset filtros
            spProducto.setSelection(0)
            spCliente.setSelection(0)
            spFecha.setSelection(0)

            Toast.makeText(this, "Venta registrada", Toast.LENGTH_SHORT).show()
        }

        // 🔹 Listener filtros
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filtrar()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spProducto.onItemSelectedListener = listener
        spCliente.onItemSelectedListener = listener
        spFecha.onItemSelectedListener = listener

        // 🔹 Botones limpiar
        btnClearProducto.setOnClickListener { spProducto.setSelection(0) }
        btnClearCliente.setOnClickListener { spCliente.setSelection(0) }
        btnClearFecha.setOnClickListener { spFecha.setSelection(0) }

        // 🔹 Inicializar
        cargarFiltros()
        filtrar()

    }
}