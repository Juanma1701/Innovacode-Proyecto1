package com.example.innovacode_proyecto1.alertas.Content

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.Dashboard.Content.Dashboard
import com.google.firebase.firestore.FirebaseFirestore
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.adapter_alertas.Content.AlertasAdapter
import com.example.innovacode_proyecto1.register_entrada_producto.Content.Register_Entrada_Producto

class Alertas : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: AlertasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alertas)

        val recyclerView = findViewById<RecyclerView>(R.id.rvAlertas)
        adapter = AlertasAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        cargarAlertas()
        val btnvolver = findViewById<Button>(R.id.btnvolverd)

        btnvolver.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarAlertas()
    }

    private fun cargarAlertas() {
        db.collection("inventario")
            .whereEqualTo("empresa_id", "empresa_demo")
            .get()
            .addOnSuccessListener { documentos ->
                val criticos = documentos.filter { doc ->
                    val cantidad = doc.getLong("cantidad") ?: 0
                    val stockMin = doc.getLong("stock_min") ?: 0
                    cantidad <= stockMin
                }.map { doc ->
                    mapOf(
                        "id"              to doc.id,
                        "nombre_producto" to (doc.getString("nombre_producto") ?: ""),
                        "cantidad"        to (doc.getLong("cantidad") ?: 0),
                        "stock_min"       to (doc.getLong("stock_min") ?: 0)
                    )
                }
                adapter.actualizarLista(criticos)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}