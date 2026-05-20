package com.example.innovacode_proyecto1.Ventas.Content

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.Dashboard.Content.Dashboard
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.Ventas.Adapter.VentaAdapter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class MainVentas : AppCompatActivity() {

    data class Venta(
        val producto: String,
        val cliente: String,
        val cantidad: Int,
        val precio: Double,
        val fecha: String
    )

    private val db = FirebaseFirestore.getInstance()
    private val listaVentas = mutableListOf<Venta>()
    private val listaFiltrada = mutableListOf<Venta>()
    private lateinit var adapter: VentaAdapter

    // Spinners a nivel de clase
    private lateinit var spProducto: Spinner
    private lateinit var spCliente: Spinner
    private lateinit var spFecha: Spinner

    private val mapaProductos = mutableMapOf<String, String>()
    private lateinit var spProductoVenta: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_ventas)

        spProductoVenta = findViewById(R.id.spProductoVenta)
        val etCliente    = findViewById<EditText>(R.id.etCliente)
        val etCantidad   = findViewById<EditText>(R.id.etCantidadVenta)
        val etPrecio     = findViewById<EditText>(R.id.etPrecioVenta)
        val etFecha      = findViewById<EditText>(R.id.etFechaVenta)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarVenta)
        val btnback      = findViewById<ImageButton>(R.id.btpatras)

        spProducto = findViewById(R.id.spProductoVenta2)
        spCliente  = findViewById(R.id.spCliente)
        spFecha    = findViewById(R.id.spFechaVenta)

        val btnClearProducto = findViewById<ImageButton>(R.id.btnClearProductoVenta)
        val btnClearCliente  = findViewById<ImageButton>(R.id.btnClearCliente)
        val btnClearFecha    = findViewById<ImageButton>(R.id.btnClearFechaVenta)

        val rvVentas = findViewById<RecyclerView>(R.id.rvVentas)
        adapter = VentaAdapter(listaFiltrada)
        rvVentas.layoutManager = LinearLayoutManager(this)
        rvVentas.adapter = adapter

        val calendar = Calendar.getInstance()
        etFecha.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val fechaFormateada = String.format(
                        "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear
                    )
                    etFecha.setText(fechaFormateada)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }

        btnback.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
        }

        btnClearProducto.setOnClickListener { spProducto.setSelection(0) }
        btnClearCliente.setOnClickListener  { spCliente.setSelection(0) }
        btnClearFecha.setOnClickListener    { spFecha.setSelection(0) }

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filtrar()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spProducto.onItemSelectedListener = listener
        spCliente.onItemSelectedListener  = listener
        spFecha.onItemSelectedListener    = listener

        btnRegistrar.setOnClickListener {
            val nombreProducto = spProductoVenta.selectedItem?.toString() ?: ""
            val productoId     = mapaProductos[nombreProducto] ?: ""
            val cliente  = etCliente.text.toString().trim()
            val cantidad = etCantidad.text.toString().toIntOrNull() ?: 0
            val precio   = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val fecha    = etFecha.text.toString()

            if (nombreProducto.isEmpty() || cliente.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (cantidad <= 0 || precio <= 0.0) {
                Toast.makeText(this, "Cantidad y precio deben ser mayores a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registrarVenta(nombreProducto, productoId, cliente, cantidad, precio, fecha)


            etCliente.text.clear()
            etCantidad.text.clear()
            etPrecio.text.clear()
            etFecha.text.clear()
            spProducto.setSelection(0)
            spCliente.setSelection(0)
            spFecha.setSelection(0)
        }

        cargarProductosEnSpinner()
        cargarVentasDesdeFirestore()
    }

    private fun registrarVenta(
        producto: String,
        productoId: String,
        cliente: String,
        cantidad: Int,
        precio: Double,
        fecha: String
    ) {
        db.collection("inventario").document(productoId)
            .get()
            .addOnSuccessListener { doc ->
                val stockActual = doc.getLong("cantidad") ?: 0

                if (cantidad > stockActual) {
                    Toast.makeText(this, "Stock insuficiente. Disponible: $stockActual", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val venta = hashMapOf(
                    "nombre_producto" to producto,
                    "nombre_cliente"  to cliente,
                    "cantidad"        to cantidad.toLong(),
                    "precio_u"        to precio,
                    "precio_total"    to (cantidad * precio),
                    "fecha"           to fecha,
                    "empresa_id"      to "empresa_demo",
                    "origen"          to "modulo_ventas"
                )

                db.collection("ventas").add(venta)
                    .addOnSuccessListener {
                        db.collection("inventario").document(productoId)
                            .update(
                                "cantidad", FieldValue.increment(-cantidad.toLong()),
                                "ultimo_movimiento", "salida",
                                "fecha_ultimo_movimiento", Timestamp.now()
                            )

                        val movimiento = hashMapOf(
                            "tipo"     to "salida",
                            "cantidad" to cantidad.toLong(),
                            "precio_u" to precio,
                            "fecha"    to Timestamp.now(),
                            "origen"   to "modulo_ventas"
                        )
                        db.collection("inventario").document(productoId)
                            .collection("movimientos")
                            .add(movimiento)

                        Toast.makeText(this, "Venta registrada ✓", Toast.LENGTH_SHORT).show()
                        cargarVentasDesdeFirestore()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }

    private fun cargarVentasDesdeFirestore() {
        db.collection("ventas")
            .whereEqualTo("empresa_id", "empresa_demo")
            .get()
            .addOnSuccessListener { documentos ->
                listaVentas.clear()
                for (doc in documentos) {
                    val venta = Venta(
                        producto = doc.getString("nombre_producto") ?: "",
                        cliente  = doc.getString("nombre_cliente")  ?: "",
                        cantidad = (doc.getLong("cantidad") ?: 0L).toInt(),
                        precio   = doc.getDouble("precio_u") ?: 0.0,
                        fecha    = doc.getString("fecha") ?: ""
                    )
                    listaVentas.add(venta)
                }
                cargarFiltros()
                filtrar()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarFiltros() {
        val productos = listOf("Todos") + listaVentas.map { it.producto }.distinct()
        val clientes  = listOf("Todos") + listaVentas.map { it.cliente }.distinct()
        val fechas    = listOf("Todos") + listaVentas.map { it.fecha }.distinct()

        spProducto.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productos)
        spCliente.adapter  = ArrayAdapter(this, android.R.layout.simple_spinner_item, clientes)
        spFecha.adapter    = ArrayAdapter(this, android.R.layout.simple_spinner_item, fechas)
    }

    private fun filtrar() {
        val productoSel = spProducto.selectedItem?.toString() ?: "Todos"
        val clienteSel  = spCliente.selectedItem?.toString()  ?: "Todos"
        val fechaSel    = spFecha.selectedItem?.toString()    ?: "Todos"

        listaFiltrada.clear()
        for (venta in listaVentas) {
            if (
                (productoSel == "Todos" || venta.producto == productoSel) &&
                (clienteSel  == "Todos" || venta.cliente  == clienteSel)  &&
                (fechaSel    == "Todos" || venta.fecha    == fechaSel)
            ) {
                listaFiltrada.add(venta)
            }
        }
        adapter.actualizarLista(listaFiltrada)
    }
    private fun cargarProductosEnSpinner() {
        db.collection("inventario")
            .whereEqualTo("empresa_id", "empresa_demo")
            .get()
            .addOnSuccessListener { documentos ->
                mapaProductos.clear()
                val nombres = mutableListOf<String>()
                for (doc in documentos) {
                    val nombre = doc.getString("nombre_producto") ?: continue
                    mapaProductos[nombre] = doc.id
                    nombres.add(nombre)
                }
                spProductoVenta.adapter = ArrayAdapter(
                    this, android.R.layout.simple_spinner_item, nombres
                )
            }
    }
}