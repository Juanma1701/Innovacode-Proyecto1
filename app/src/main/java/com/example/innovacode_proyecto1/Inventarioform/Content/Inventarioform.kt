package com.example.innovacode_proyecto1.Inventarioform.Content
import android.content.Intent
import com.example.innovacode_proyecto1.Inventario.Content.Inventario

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.innovacode_proyecto1.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.EditText
import android.widget.Toast

class Inventarioform : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventarioform)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btn_cancelar = findViewById<Button>(R.id.btnCancel)
        val btn_Guardar=findViewById<Button>(R.id.btnSave)
        val etNombre    = findViewById<EditText>(R.id.etName)
        val etCantidad  = findViewById<EditText>(R.id.etQuantity)
        val etPrecio    = findViewById<EditText>(R.id.etPrice)
        val etCategoria = findViewById<EditText>(R.id.spinnerCategory)
        val etDesc      = findViewById<EditText>(R.id.etDescription)
        val etStockMin  = findViewById<EditText>(R.id.etMinStock)

        btn_cancelar.setOnClickListener {
            val intent = Intent(this, Inventario::class.java)
            startActivity(intent)
        }
        btn_Guardar.setOnClickListener {
            guardarEnInventario(
                nombre    = etNombre.text.toString().trim(),
                cantidad  = etCantidad.text.toString().toIntOrNull() ?: 0,
                precio    = etPrecio.text.toString().toDoubleOrNull() ?: 0.0,
                categoria = etCategoria.text.toString().trim(),
                desc      = etDesc.text.toString().trim(),
                stockMin  = etStockMin.text.toString().toIntOrNull() ?: 0
            )
        }
    }
    private fun guardarEnInventario(
        nombre: String,
        cantidad: Int,
        precio: Double,
        categoria: String,
        desc: String,
        stockMin: Int
    ) {
        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val producto = hashMapOf(
            "nombre_producto" to nombre,
            "cantidad"        to cantidad,
            "precio_u"        to precio,
            "categoria"       to categoria,
            "descripcion"     to desc,
            "stock_min"       to stockMin,
            "empresa_id"      to "empresa_demo",
            "fecha_creacion"  to Timestamp.now()
        )

        db.collection("inventario")
            .add(producto)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto registrado ✓", Toast.LENGTH_SHORT).show()
                // Regresa al inventario después de guardar
                val intent = Intent(this, Inventario::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

}


