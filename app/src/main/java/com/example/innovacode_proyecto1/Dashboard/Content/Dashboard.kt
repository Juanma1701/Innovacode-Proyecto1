package com.example.innovacode_proyecto1.Dashboard.Content
import com.example.innovacode_proyecto1.Inventario.Content.Inventario
import com.example.innovacode_proyecto1.Perfil.Content.Perfil
import com.example.innovacode_proyecto1.Ventas.Content.MainVentas


import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.innovacode_proyecto1.R
import android.content.Intent
import android.widget.ImageButton
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import android.view.View
import android.widget.LinearLayout
import com.example.innovacode_proyecto1.ComprasActivity
import com.example.innovacode_proyecto1.Reportes.Content.Reportes
import com.example.innovacode_proyecto1.alertas.Content.Alertas

class Dashboard : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    // ← declarados a nivel de clase, no dentro del onCreate
    private lateinit var cardAlertas: LinearLayout
    private lateinit var tvResumenAlertas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btninventario = findViewById<Button>(R.id.btninvent)
        val btnperfil = findViewById<ImageButton>(R.id.btnperfil)
        val btncompras = findViewById<Button>(R.id.btncompras)
        val btnventas = findViewById<Button>(R.id.btnventas)
        val btnreportes = findViewById<Button>(R.id.btnreportes)

        // ← se inicializan aquí
        cardAlertas = findViewById(R.id.cardAlertas)
        tvResumenAlertas = findViewById(R.id.tvResumenAlertas)

        btninventario.setOnClickListener {
            startActivity(Intent(this, Inventario::class.java))
        }
        btnreportes.setOnClickListener {
            startActivity(Intent(this, Reportes::class.java))
        }
        btnperfil.setOnClickListener {
            startActivity(Intent(this, Perfil::class.java))
        }
        btncompras.setOnClickListener {
            startActivity(Intent(this, ComprasActivity::class.java))
        }
        btnventas.setOnClickListener {
            startActivity(Intent(this, MainVentas::class.java))
        }

        verificarAlertas() // ← se llama aquí
    }

    override fun onResume() {
        super.onResume()
        verificarAlertas()
    }

    private fun verificarAlertas() {
        db.collection("inventario")
            .whereEqualTo("empresa_id", "empresa_demo")
            .get()
            .addOnSuccessListener { documentos ->
                val totalCriticos = documentos.count { doc ->
                    val cantidad = doc.getLong("cantidad") ?: 0
                    val stockMin = doc.getLong("stock_min") ?: 0
                    cantidad <= stockMin
                }

                if (totalCriticos > 0) {
                    cardAlertas.visibility = View.VISIBLE
                    tvResumenAlertas.text = "⚠️ $totalCriticos productos por agotarse → Ver"
                    cardAlertas.setOnClickListener {
                        startActivity(Intent(this, Alertas::class.java))
                    }
                } else {
                    cardAlertas.visibility = View.GONE
                }
            }
    }
}