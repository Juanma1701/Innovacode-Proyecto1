package com.example.innovacode_proyecto1.register_salida_producto.Content

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.innovacode_proyecto1.R

class Register_Salida_Producto : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_salida_producto)

        val productoId     = intent.getStringExtra("productoId") ?: ""
        val nombre         = intent.getStringExtra("nombre") ?: ""
        val cantidadActual = intent.getLongExtra("cantidadActual", 0L)

        val tvNombre   = findViewById<TextView>(R.id.tvNombreSalida)
        val tvStock    = findViewById<TextView>(R.id.tvStockActual)
        val etCantidad = findViewById<EditText>(R.id.etCantidadSalida)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmarSalida)
        val btnCancelar  = findViewById<Button>(R.id.btnCancelarSalida)

        tvNombre.text = nombre
        tvStock.text  = "Existencias actuales: $cantidadActual"

        btnCancelar.setOnClickListener { finish() }

        btnConfirmar.setOnClickListener {
            val cantidad = etCantidad.text.toString().toIntOrNull()

            if (cantidad == null || cantidad <= 0) {
                Toast.makeText(this, "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación clave: no puede salir más de lo que hay
            if (cantidad > cantidadActual) {
                Toast.makeText(this, "No hay suficientes existencias", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registrarSalida(productoId, cantidad)
        }
    }

    private fun registrarSalida(productoId: String, cantidad: Int) {
        val actualizacion = hashMapOf<String, Any>(
            "cantidad"                 to FieldValue.increment(-cantidad.toLong()),
            "ultimo_movimiento"        to "salida",
            "fecha_ultimo_movimiento"  to com.google.firebase.Timestamp.now()
        )

        db.collection("inventario").document(productoId)
            .update(actualizacion)
            .addOnSuccessListener {
                Toast.makeText(this, "Salida registrada ✓", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}