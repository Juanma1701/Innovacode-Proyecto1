package com.example.innovacode_proyecto1.register_entrada_producto.Content

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.innovacode_proyecto1.R

class Register_Entrada_Producto : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_entrada_producto)

        // Recibe los datos del producto desde Inventario
        val productoId = intent.getStringExtra("productoId") ?: ""
        val nombre     = intent.getStringExtra("nombre") ?: ""

        val tvNombre   = findViewById<TextView>(R.id.tvNombreProducto)
        val etCantidad = findViewById<EditText>(R.id.etCantidadEntrada)
        val etPrecio   = findViewById<EditText>(R.id.etPrecioEntrada)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmarEntrada)
        val btnCancelar  = findViewById<Button>(R.id.btnCancelarEntrada)

        tvNombre.text = nombre

        btnCancelar.setOnClickListener { finish() }

        btnConfirmar.setOnClickListener {
            val cantidad = etCantidad.text.toString().toIntOrNull()
            val precio   = etPrecio.text.toString().toDoubleOrNull()

            if (cantidad == null || cantidad <= 0) {
                Toast.makeText(this, "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registrarEntrada(productoId, cantidad, precio)
        }
    }

    private fun registrarEntrada(productoId: String, cantidad: Int, precio: Double?) {
        val actualizacion = mutableMapOf<String, Any>(
            "cantidad" to FieldValue.increment(cantidad.toLong())
        )
        if (precio != null) {
            actualizacion["precio_u"] = precio
        }

        db.collection("inventario").document(productoId)
            .update(actualizacion)
            .addOnSuccessListener {
                Toast.makeText(this, "Entrada registrada ✓", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}