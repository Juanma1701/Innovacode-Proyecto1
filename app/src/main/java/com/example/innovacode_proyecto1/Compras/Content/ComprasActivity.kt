package com.example.innovacode_proyecto1

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.Compras.Adapter.CompraAdapter
import com.example.innovacode_proyecto1.Dashboard.Content.Dashboard
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ComprasActivity : AppCompatActivity() {

    data class Compra(
        val producto: String,
        val proveedor: String,
        val cantidad: Int,
        val precio: Double,
        val fecha: String
    )

    private val db = FirebaseFirestore.getInstance()
    private val listaCompras = mutableListOf<Compra>()
    private val listaFiltrada = mutableListOf<Compra>()
    private lateinit var adapter: CompraAdapter
    private lateinit var spProducto: Spinner
    private lateinit var spProveedor: Spinner
    private lateinit var spFecha: Spinner

    // Mapa nombre → ID para saber qué producto actualizar
    private val mapaProductos = mutableMapOf<String, String>()
    private lateinit var spProductoCompra: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_compras)

        // Spinner selector de producto desde inventario
        spProductoCompra = findViewById(R.id.spProductoCompra)

        val etProveedor  = findViewById<EditText>(R.id.etProveedor)
        val etCantidad   = findViewById<EditText>(R.id.etCantidadCompra)
        val etPrecio     = findViewById<EditText>(R.id.etPrecioCompra)
        val etFecha      = findViewById<EditText>(R.id.etFechaCompra)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarCompra)
        val btnback      = findViewById<ImageButton>(R.id.btnBack1)

        spProducto  = findViewById(R.id.spProducto)
        spProveedor = findViewById(R.id.spProveedor)
        spFecha     = findViewById(R.id.spFecha)

        val btnClearProducto  = findViewById<ImageButton>(R.id.btnClearProducto)
        val btnClearProveedor = findViewById<ImageButton>(R.id.btnClearProveedor)
        val btnClearFecha     = findViewById<ImageButton>(R.id.btnClearFecha)

        val rvCompras = findViewById<RecyclerView>(R.id.rvCompras)
        rvCompras.layoutManager = LinearLayoutManager(this)
        adapter = CompraAdapter(listaFiltrada)
        rvCompras.adapter = adapter

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) { filtrar() }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spProducto.onItemSelectedListener  = listener
        spProveedor.onItemSelectedListener = listener
        spFecha.onItemSelectedListener     = listener

        btnback.setOnClickListener { startActivity(Intent(this, Dashboard::class.java)) }
        btnClearProducto.setOnClickListener  { spProducto.setSelection(0) }
        btnClearProveedor.setOnClickListener { spProveedor.setSelection(0) }
        btnClearFecha.setOnClickListener     { spFecha.setSelection(0) }

        etFecha.setOnClickListener {
            val calendario = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    etFecha.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnRegistrar.setOnClickListener {
            val nombreProducto = spProductoCompra.selectedItem?.toString() ?: ""
            val productoId     = mapaProductos[nombreProducto] ?: ""
            val proveedor      = etProveedor.text.toString().trim()
            val cantidad       = etCantidad.text.toString().toIntOrNull() ?: 0
            val precio         = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val fecha          = etFecha.text.toString()

            if (nombreProducto.isEmpty() || proveedor.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (cantidad <= 0 || precio <= 0.0) {
                Toast.makeText(this, "Cantidad y precio deben ser mayores a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (productoId.isEmpty()) {
                Toast.makeText(this, "Selecciona un producto válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registrarCompra(nombreProducto, productoId, proveedor, cantidad, precio, fecha)

            etProveedor.text.clear()
            etCantidad.text.clear()
            etPrecio.text.clear()
            etFecha.text.clear()
        }

        cargarProductosEnSpinner()
        cargarComprasDesdeFirestore()
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
                spProductoCompra.adapter = ArrayAdapter(
                    this, android.R.layout.simple_spinner_item, nombres
                )
            }
    }

    private fun registrarCompra(
        nombreProducto: String,
        productoId: String,
        proveedor: String,
        cantidad: Int,
        precio: Double,
        fecha: String
    ) {
        // 1. Crea documento en /compras
        val compra = hashMapOf(
            "nombre_producto"  to nombreProducto,
            "nombre_proveedor" to proveedor,
            "cantidad"         to cantidad.toLong(),
            "precio_u"         to precio,
            "precio_total"     to (cantidad * precio),
            "fecha"            to fecha,
            "empresa_id"       to "empresa_demo",
            "origen"           to "modulo_compras"
        )

        db.collection("compras").add(compra)
            .addOnSuccessListener {

                // 2. Actualiza cantidad en /inventario
                db.collection("inventario").document(productoId)
                    .update(
                        "cantidad", FieldValue.increment(cantidad.toLong()),
                        "ultimo_movimiento", "entrada",
                        "fecha_ultimo_movimiento", Timestamp.now()
                    )

                // 3. Crea movimiento en subcolección
                val movimiento = hashMapOf(
                    "tipo"     to "entrada",
                    "cantidad" to cantidad.toLong(),
                    "precio_u" to precio,
                    "fecha"    to Timestamp.now(),
                    "origen"   to "modulo_compras"
                )
                db.collection("inventario").document(productoId)
                    .collection("movimientos")
                    .add(movimiento)

                Toast.makeText(this, "Compra registrada ✓", Toast.LENGTH_SHORT).show()
                cargarComprasDesdeFirestore()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarComprasDesdeFirestore() {
        db.collection("compras")
            .whereEqualTo("empresa_id", "empresa_demo")
            .get()
            .addOnSuccessListener { documentos ->
                listaCompras.clear()
                for (doc in documentos) {
                    listaCompras.add(
                        Compra(
                            producto  = doc.getString("nombre_producto")  ?: "",
                            proveedor = doc.getString("nombre_proveedor") ?: "",
                            cantidad  = (doc.getLong("cantidad") ?: 0L).toInt(),
                            precio    = doc.getDouble("precio_u") ?: 0.0,
                            fecha     = doc.getString("fecha") ?: ""
                        )
                    )
                }
                cargarFiltros()
                filtrar()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun cargarFiltros() {
        val productos   = listOf("Todos") + listaCompras.map { it.producto }.distinct()
        val proveedores = listOf("Todos") + listaCompras.map { it.proveedor }.distinct()
        val fechas      = listOf("Todos") + listaCompras.map { it.fecha }.distinct()

        spProducto.adapter  = ArrayAdapter(this, android.R.layout.simple_spinner_item, productos)
        spProveedor.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, proveedores)
        spFecha.adapter     = ArrayAdapter(this, android.R.layout.simple_spinner_item, fechas)
    }

    fun filtrar() {
        val productoSel  = spProducto.selectedItem?.toString()  ?: "Todos"
        val proveedorSel = spProveedor.selectedItem?.toString() ?: "Todos"
        val fechaSel     = spFecha.selectedItem?.toString()     ?: "Todos"

        listaFiltrada.clear()
        for (compra in listaCompras) {
            if (
                (productoSel  == "Todos" || compra.producto  == productoSel)  &&
                (proveedorSel == "Todos" || compra.proveedor == proveedorSel) &&
                (fechaSel     == "Todos" || compra.fecha     == fechaSel)
            ) {
                listaFiltrada.add(compra)
            }
        }
        adapter.actualizarLista(listaFiltrada)
    }
}