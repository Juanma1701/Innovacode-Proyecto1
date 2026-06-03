package com.example.innovacode_proyecto1.Inventario.Content

import com.example.innovacode_proyecto1.Inventarioform.Content.Inventarioform
import com.example.innovacode_proyecto1.Dashboard.Content.Dashboard
import com.example.innovacode_proyecto1.adapter_inventario.Content.ProductoAdapter
import com.example.innovacode_proyecto1.register_entrada_producto.Content.Register_Entrada_Producto
import com.example.innovacode_proyecto1.register_salida_producto.Content.Register_Salida_Producto
import com.example.innovacode_proyecto1.R

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class Inventario : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ProductoAdapter
    private val listaProductos = mutableListOf<Map<String, Any>>()

    // Lanzador del escaner para buscar producto por codigo
    private val escaner = registerForActivityResult(ScanContract()) { resultado ->
        if (resultado.contents != null) {
            buscarPorCodigoBarras(resultado.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack        = findViewById<ImageButton>(R.id.btnBack)
        val btnAddProducto = findViewById<Button>(R.id.btnAddProduct)
        val btnEscanear    = findViewById<ImageButton>(R.id.btnEscanearInventario)
        val etBuscar       = findViewById<EditText>(R.id.etBuscar)
        val recyclerView   = findViewById<RecyclerView>(R.id.recyclerProducts)

        adapter = ProductoAdapter(
            mutableListOf(),
            onEntradaClick = { productoId, nombre ->
                startActivity(
                    Intent(this, Register_Entrada_Producto::class.java).apply {
                        putExtra("productoId", productoId)
                        putExtra("nombre", nombre)
                    }
                )
            },
            onSalidaClick = { productoId, nombre, cantidadActual ->
                startActivity(
                    Intent(this, Register_Salida_Producto::class.java).apply {
                        putExtra("productoId", productoId)
                        putExtra("nombre", nombre)
                        putExtra("cantidadActual", cantidadActual)
                    }
                )
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnBack.setOnClickListener { startActivity(Intent(this, Dashboard::class.java)) }
        btnAddProducto.setOnClickListener { startActivity(Intent(this, Inventarioform::class.java)) }

        // Escaner: busca el producto por su codigo_barras en Firestore
        btnEscanear.setOnClickListener {
            val opciones = ScanOptions().apply {
                setPrompt("Escanea el codigo de barras del producto")
                setBeepEnabled(true)
                setOrientationLocked(false)
                setBarcodeImageEnabled(false)
            }
            escaner.launch(opciones)
        }

        etBuscar.addTextChangedListener { texto -> filtrarPorNombre(texto.toString().trim()) }

        cargarInventario()
    }

    // Busca el producto en la lista local por su codigo_barras
    // Si lo encuentra lo muestra solo en el RecyclerView
    private fun buscarPorCodigoBarras(codigo: String) {
        val encontrados = listaProductos.filter { producto ->
            producto["codigo_barras"]?.toString() == codigo
        }
        if (encontrados.isEmpty()) {
            Toast.makeText(
                this,
                "Ningún producto con código $codigo",
                Toast.LENGTH_LONG
            ).show()
            // Restaurar lista completa
            adapter.actualizarLista(listaProductos)
        } else {
            adapter.actualizarLista(encontrados)
            Toast.makeText(
                this,
                "${encontrados.size} producto(s) encontrado(s)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun filtrarPorNombre(query: String) {
        if (query.isEmpty()) { adapter.actualizarLista(listaProductos); return }
        val filtrados = listaProductos.filter { producto ->
            producto["nombre_producto"]?.toString()?.contains(query, ignoreCase = true) == true
        }
        adapter.actualizarLista(filtrados)
    }

    private fun cargarInventario() {
        db.collection("inventario")
            .whereEqualTo("empresa_id", "empresa_demo")
            .get()
            .addOnSuccessListener { documentos ->
                val lista = documentos.map { doc ->
                    doc.data.toMutableMap().also { it["id"] = doc.id }
                }
                listaProductos.clear()
                listaProductos.addAll(lista)
                adapter.actualizarLista(lista)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onResume() {
        super.onResume()
        cargarInventario()
    }
}