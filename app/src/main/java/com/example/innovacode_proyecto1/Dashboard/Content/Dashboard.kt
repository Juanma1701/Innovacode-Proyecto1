package com.example.innovacode_proyecto1.Dashboard.Content
import com.example.innovacode_proyecto1.Inventario.Content.Inventario

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.innovacode_proyecto1.R
import android.content.Intent
import android.widget.ImageButton
import com.example.innovacode_proyecto1.Perfil.Content.Perfil

class Dashboard : AppCompatActivity() {
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
        val btnperfil = findViewById< ImageButton>(R.id.btnperfil)

        btninventario.setOnClickListener {
            val intent = Intent(this, Inventario::class.java)
            startActivity(intent)
        }

        btnperfil.setOnClickListener {
            val intent = Intent(this, Perfil::class.java)
            startActivity(intent)
        }

    }
}