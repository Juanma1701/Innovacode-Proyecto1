package com.example.innovacode_proyecto1.Perfil.Content

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.innovacode_proyecto1.R
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import android.content.Intent
import com.example.innovacode_proyecto1.Dashboard.Content.Dashboard
import com.example.innovacode_proyecto1.auth.ui.Login
import android.util.Log
import android.widget.EditText
import android.widget.TextView

class Perfil : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid
        val tvNombre = findViewById<TextView>(R.id.tvNombre)
        val tvTelefono = findViewById<TextView>(R.id.tvTelefono)
        val tvCorreo = findViewById<TextView>(R.id.tvCorreo)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)
        val btnvolver = findViewById<Button>(R.id.btnbackdash)

        if (uid != null) {
            db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val nombre = document.getString("nombre")
                        val correo = document.getString("correo")
                        val telefono = document.getString("telefono")

                        tvNombre.text = nombre ?: "Sin nombre"
                        tvCorreo.text = correo ?: "Sin correo"
                        tvTelefono.text = telefono ?: "Sin teléfono"
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("Firestore", "Error al obtener datos: ${e.message}")
                }
        }

        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val usuarioActual = FirebaseAuth.getInstance().currentUser
            if (usuarioActual == null) {
                Log.d("Firebase", "Sesión cerrada correctamente")
            } else {
                Log.d("Firebase", "ERROR: sigue con sesión activa")
            }
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        btnvolver.setOnClickListener{
            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
        }
    }
}