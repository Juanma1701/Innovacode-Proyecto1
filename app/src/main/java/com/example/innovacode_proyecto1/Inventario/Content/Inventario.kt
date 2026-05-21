package com.example.innovacode_proyecto1.Inventario.Content
import com.example.innovacode_proyecto1.Inventarioform.Content.Inventarioform
import com.example.innovacode_proyecto1.Dashboard.Content.Dashboard
import com.example.innovacode_proyecto1.adapter_inventario.Content.ProductoAdapter
import com.example.innovacode_proyecto1.register_entrada_producto.Content.Register_Entrada_Producto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.innovacode_proyecto1.Ventas.Content.MainVentas
import com.example.innovacode_proyecto1.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.widget.addTextChangedListener
import com.example.innovacode_proyecto1.register_salida_producto.Content.Register_Salida_Producto


class Inventario : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ProductoAdapter
    private val listaProductos = mutableListOf<Map<String, Any>>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventario)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnback = findViewById< ImageButton>(R.id.btnBack)
        val btnaddproducto = findViewById<Button>(R.id.btnAddProduct)
        val etBuscar = findViewById<EditText>(R.id.etBuscar)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerProducts) // ajusta el ID a tu XML
        adapter = ProductoAdapter(
            mutableListOf(),
            onEntradaClick = { productoId, nombre ->
                val intent = Intent(this, Register_Entrada_Producto::class.java)
                intent.putExtra("productoId", productoId)
                intent.putExtra("nombre", nombre)
                startActivity(intent)
            },
            onSalidaClick = { productoId, nombre, cantidadActual ->
                val intent = Intent(this, Register_Salida_Producto::class.java)
                intent.putExtra("productoId", productoId)
                intent.putExtra("nombre", nombre)
                intent.putExtra("cantidadActual", cantidadActual)
                startActivity(intent)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnaddproducto.setOnClickListener {
            val intent = Intent(this, Inventarioform::class.java)
            startActivity(intent)
        }
        btnback.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
        }
        cargarInventario()

        etBuscar.addTextChangedListener { texto ->
            filtrarPorNombre(texto.toString().trim())
        }

    }
    private fun filtrarPorNombre(query: String) {
        if (query.isEmpty()) {
            adapter.actualizarLista(listaProductos)
            return
        }

        val filtrados = listaProductos.filter { producto ->
            val nombre = producto["nombre_producto"]?.toString() ?: ""
            nombre.contains(query, ignoreCase = true)
        }

        adapter.actualizarLista(filtrados)
    }
    private fun cargarInventario() {
        db.collection("inventario")
            .whereEqualTo("empresa_id", "empresa_demo")
            .get()
            .addOnSuccessListener { documentos ->
                val lista = documentos.map { doc ->
                    val data = doc.data.toMutableMap()
                    data["id"] = doc.id
                    data
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