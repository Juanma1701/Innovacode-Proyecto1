package com.example.innovacode_proyecto1.auth.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.innovacode_proyecto1.R

class Login : AppCompatActivity() {

    // ── Vistas ────────────────────────────────────────────────────────────────
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnVerContrasena: ImageButton
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvOlvideContrasena: TextView
    private lateinit var btnGoogle: Button
    private lateinit var tvCrearCuenta: TextView

    // ── Estado ────────────────────────────────────────────────────────────────
    private var contrasenaVisible = false

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        window.statusBarColor = resources.getColor(android.R.color.black, null)

        inicializarVistas()
        configurarListeners()
    }

    // ── Inicializar vistas ────────────────────────────────────────────────────
    private fun inicializarVistas() {
        etCorreo           = findViewById(R.id.etCorreo)
        etContrasena       = findViewById(R.id.etContrasena)
        btnVerContrasena   = findViewById(R.id.btnVerContrasena)
        btnIniciarSesion   = findViewById(R.id.btnIniciarSesion)
        tvOlvideContrasena = findViewById(R.id.tvOlvideContrasena)
        btnGoogle          = findViewById(R.id.btnGoogle)
        tvCrearCuenta      = findViewById(R.id.tvCrearCuenta)
    }

    // ── Configurar listeners ──────────────────────────────────────────────────
    private fun configurarListeners() {

        // Ver / ocultar contraseña
        btnVerContrasena.setOnClickListener {
            contrasenaVisible = !contrasenaVisible
            if (contrasenaVisible) {
                etContrasena.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                btnVerContrasena.setImageResource(R.drawable.ic_ojo_abierto)
            } else {
                etContrasena.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                btnVerContrasena.setImageResource(R.drawable.ic_ojo_cerrado)
            }
            etContrasena.setSelection(etContrasena.text.length)
        }

        // Iniciar sesión
        btnIniciarSesion.setOnClickListener {
            val correo     = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()

            if (validarCampos(correo, contrasena)) {
                iniciarSesion(correo, contrasena)
            }
        }

        // Olvidé contraseña
        tvOlvideContrasena.setOnClickListener {
            Toast.makeText(this, "Redirigiendo a recuperar contraseña...", Toast.LENGTH_SHORT).show()
        }

        // Google
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Iniciando con Google...", Toast.LENGTH_SHORT).show()
        }

        // Ir a Registro
        tvCrearCuenta.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
    }

    // ── Validar campos ────────────────────────────────────────────────────────
    private fun validarCampos(correo: String, contrasena: String): Boolean {
        when {
            correo.isEmpty() -> {
                etCorreo.error = "Ingresa tu correo electrónico"
                etCorreo.requestFocus()
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> {
                etCorreo.error = "Correo electrónico inválido"
                etCorreo.requestFocus()
                return false
            }
            contrasena.isEmpty() -> {
                etContrasena.error = "Ingresa tu contraseña"
                etContrasena.requestFocus()
                return false
            }
            contrasena.length < 6 -> {
                etContrasena.error = "Mínimo 6 caracteres"
                etContrasena.requestFocus()
                return false
            }
        }
        return true
    }

    // ── Iniciar sesión ────────────────────────────────────────────────────────
    private fun iniciarSesion(correo: String, contrasena: String) {
        Toast.makeText(this, "Bienvenido: $correo", Toast.LENGTH_SHORT).show()

        // Navegar al home después del login exitoso:
        // val intent = Intent(this, MainActivity::class.java)
        // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // startActivity(intent)
        // finish()
    }
}