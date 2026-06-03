package com.example.innovacode_proyecto1.Ventas.Content

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.Dashboard.Content.Dashboard
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.Ventas.Adapter.VentaAdapter
import com.example.innovacode_proyecto1.utils.DescuentoInventarioUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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

    private lateinit var spProducto: Spinner
    private lateinit var spCliente: Spinner
    private lateinit var spFecha: Spinner
    private lateinit var spProductoVenta: Spinner

    // Guarda nombre -> documentId de cada producto
    private val mapaProductos = mutableMapOf<String, String>()
    // Guarda codigo_barras -> nombre del producto
    private val mapaCodigoBarras = mutableMapOf<String, String>()
    // Lista ordenada de nombres para el spinner (sin el placeholder)
    private val nombresProductos = mutableListOf<String>()

    private val empresaId = "empresa_demo"

    // Lanzador del escaner para seleccionar producto en ventas
    private val escaner = registerForActivityResult(ScanContract()) { resultado ->
        if (resultado.contents != null) {
            seleccionarProductoPorCodigo(resultado.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_ventas)

        spProductoVenta = findViewById(R.id.spProductoVenta)
        val etCliente    = findViewById<EditText>(R.id.etCliente)
        val etCantidad   = findViewById<EditText>(R.id.etCantidadVenta)
        val etPrecio     = findViewById<EditText>(R.id.etPrecioVenta)
        val etFecha      = findViewById<EditText>(R.id.etFechaVenta)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarVenta)
        val btnBack      = findViewById<ImageButton>(R.id.btpatras)
        val btnEscanear  = findViewById<ImageButton>(R.id.btnEscanearVenta)

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

        // DatePicker
        val calendar = Calendar.getInstance()
        etFecha.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                etFecha.setText(String.format("%02d/%02d/%d", d, m + 1, y))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.maxDate = System.currentTimeMillis() }.show()
        }

        btnBack.setOnClickListener { startActivity(Intent(this, Dashboard::class.java)) }

        // Escanear codigo de barras → autoselecciona el producto en el spinner
        btnEscanear.setOnClickListener {
            val opciones = ScanOptions().apply {
                setPrompt("Escanea el código del producto a vender")
                setBeepEnabled(true)
                setOrientationLocked(false)
                setBarcodeImageEnabled(false)
            }
            escaner.launch(opciones)
        }

        btnClearProducto.setOnClickListener { spProducto.setSelection(0) }
        btnClearCliente.setOnClickListener  { spCliente.setSelection(0) }
        btnClearFecha.setOnClickListener    { spFecha.setSelection(0) }

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) { filtrar() }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
        spProducto.onItemSelectedListener = listener
        spCliente.onItemSelectedListener  = listener
        spFecha.onItemSelectedListener    = listener

        btnRegistrar.setOnClickListener {
            val nombreProducto = spProductoVenta.selectedItem?.toString() ?: ""
            if (nombreProducto == "Seleccionar Producto") {
                Toast.makeText(this, "Selecciona un producto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val productoId = mapaProductos[nombreProducto] ?: ""
            val cliente    = etCliente.text.toString().trim()
            val cantidad   = etCantidad.text.toString().toIntOrNull() ?: 0
            val precio     = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val fecha      = etFecha.text.toString()

            if (cliente.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (cantidad <= 0 || precio <= 0.0) {
                Toast.makeText(this, "Cantidad y precio deben ser mayores a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            registrarVenta(nombreProducto, productoId, cliente, cantidad, precio, fecha)
            limpiarFormulario(etCliente, etCantidad, etPrecio, etFecha)
        }

        cargarProductosEnSpinner()
        cargarVentasDesdeFirestore()
    }

    // Busca el codigo escaneado en mapaCodigoBarras y selecciona el producto en el spinner
    private fun seleccionarProductoPorCodigo(codigo: String) {
        val nombreProducto = mapaCodigoBarras[codigo]
        if (nombreProducto == null) {
            Toast.makeText(this, "Producto no encontrado para código $codigo", Toast.LENGTH_LONG).show()
            return
        }
        val index = nombresProductos.indexOf(nombreProducto)
        if (index >= 0) {
            // +1 porque el spinner tiene "Seleccionar Producto" en posición 0
            spProductoVenta.setSelection(index + 1)
            Toast.makeText(this, "Producto: $nombreProducto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registrarVenta(
        producto: String, productoId: String, cliente: String,
        cantidad: Int, precio: Double, fecha: String
    ) {
        db.collection("inventario").document(productoId).get()
            .addOnSuccessListener { doc ->
                val stockActual = doc.getDouble("cantidad") ?: 0.0
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
                    "empresa_id"      to empresaId,
                    "origen"          to "modulo_ventas"
                )
                db.collection("ventas").add(venta)
                    .addOnSuccessListener {
                        db.collection("inventario").document(productoId).update(
                            "cantidad", FieldValue.increment(-cantidad.toLong()),
                            "ultimo_movimiento", "salida",
                            "fecha_ultimo_movimiento", Timestamp.now()
                        )
                        db.collection("inventario").document(productoId)
                            .collection("movimientos")
                            .add(hashMapOf(
                                "tipo"            to "salida",
                                "cantidad"        to cantidad.toLong(),
                                "precio_u"        to precio,
                                "nombre_producto" to producto,
                                "empresa_id"      to empresaId,
                                "fecha"           to Timestamp.now(),
                                "origen"          to "modulo_ventas"
                            ))
                        DescuentoInventarioUtils.descontarIngredientes(
                            nombreProducto  = producto,
                            cantidadVendida = cantidad.toDouble(),
                            empresaId       = empresaId,
                            onError = { error ->
                                runOnUiThread {
                                    Toast.makeText(this, "Venta registrada, error en receta: $error", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                        Toast.makeText(this, "Venta registrada", Toast.LENGTH_SHORT).show()
                        cargarVentasDesdeFirestore()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }

    private fun limpiarFormulario(
        etCliente: EditText, etCantidad: EditText,
        etPrecio: EditText, etFecha: EditText
    ) {
        etCliente.text.clear(); etCantidad.text.clear()
        etPrecio.text.clear();  etFecha.text.clear()
        spProductoVenta.setSelection(0)
        spProducto.setSelection(0); spCliente.setSelection(0); spFecha.setSelection(0)
    }

    private fun cargarVentasDesdeFirestore() {
        db.collection("ventas").whereEqualTo("empresa_id", empresaId).get()
            .addOnSuccessListener { docs ->
                listaVentas.clear()
                docs.forEach { doc ->
                    listaVentas.add(Venta(
                        producto = doc.getString("nombre_producto") ?: "",
                        cliente  = doc.getString("nombre_cliente")  ?: "",
                        cantidad = (doc.getLong("cantidad") ?: 0L).toInt(),
                        precio   = doc.getDouble("precio_u") ?: 0.0,
                        fecha    = doc.getString("fecha") ?: ""
                    ))
                }
                cargarFiltros(); filtrar()
            }
    }

    private fun cargarFiltros() {
        configurarSpinnerConEstilo(spProducto, listOf("Todos") + listaVentas.map { it.producto }.distinct())
        configurarSpinnerConEstilo(spCliente,  listOf("Todos") + listaVentas.map { it.cliente }.distinct())
        configurarSpinnerConEstilo(spFecha,    listOf("Todos") + listaVentas.map { it.fecha }.distinct())
    }

    private fun filtrar() {
        val p = spProducto.selectedItem?.toString() ?: "Todos"
        val c = spCliente.selectedItem?.toString()  ?: "Todos"
        val f = spFecha.selectedItem?.toString()    ?: "Todos"
        listaFiltrada.clear()
        listaVentas.filter {
            (p == "Todos" || it.producto == p) &&
                    (c == "Todos" || it.cliente  == c) &&
                    (f == "Todos" || it.fecha    == f)
        }.forEach { listaFiltrada.add(it) }
        adapter.actualizarLista(listaFiltrada)
    }

    private fun cargarProductosEnSpinner() {
        db.collection("inventario").whereEqualTo("empresa_id", empresaId).get()
            .addOnSuccessListener { docs ->
                mapaProductos.clear()
                mapaCodigoBarras.clear()
                nombresProductos.clear()

                val spinnerItems = mutableListOf("Seleccionar Producto")
                docs.forEach { doc ->
                    val nombre = doc.getString("nombre_producto") ?: return@forEach
                    val codigo = doc.getString("codigo_barras")

                    mapaProductos[nombre] = doc.id
                    nombresProductos.add(nombre)
                    spinnerItems.add(nombre)

                    // Solo registrar en el mapa si tiene codigo_barras
                    if (!codigo.isNullOrEmpty()) mapaCodigoBarras[codigo] = nombre
                }
                configurarSpinnerConEstilo(spProductoVenta, spinnerItems)
            }
    }

    private fun configurarSpinnerConEstilo(spinner: Spinner, items: List<String>) {
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getView(position, convertView, parent) as TextView).apply {
                    setTextColor(if (position == 0) Color.parseColor("#555565") else Color.WHITE)
                }
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                    setBackgroundColor(Color.parseColor("#12121A"))
                    setTextColor(if (position == 0) Color.parseColor("#555565") else Color.WHITE)
                }
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
}