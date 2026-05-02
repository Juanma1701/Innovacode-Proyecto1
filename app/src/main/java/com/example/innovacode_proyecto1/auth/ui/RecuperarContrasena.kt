package com.example.innovacode_proyecto1.auth.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.innovacode_proyecto1.R
import com.google.firebase.auth.FirebaseAuth

class RecuperarContrasena : AppCompatActivity() {

    // ── Vistas ────────────────────────────────────────────────────────────────
    private lateinit var btnVolver: ImageButton
    private lateinit var etCorreo: EditText
    private lateinit var btnEnviarEnlace: Button
    private lateinit var layoutConfirmacion: LinearLayout
    private lateinit var tvCorreoEnviado: TextView
    private lateinit var tvVolverLogin: TextView

    // ── Firebase ──────────────────────────────────────────────────────────────
    private lateinit var auth: FirebaseAuth

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)

        window.statusBarColor = resources.getColor(android.R.color.black, null)

        auth = FirebaseAuth.getInstance()

        inicializarVistas()
        configurarListeners()

        // Quitar foco al abrir
        window.decorView.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    // ── Inicializar vistas ────────────────────────────────────────────────────
    private fun inicializarVistas() {
        btnVolver          = findViewById(R.id.btnVolver)
        etCorreo           = findViewById(R.id.etCorreo)
        btnEnviarEnlace    = findViewById(R.id.btnEnviarEnlace)
        layoutConfirmacion = findViewById(R.id.layoutConfirmacion)
        tvCorreoEnviado    = findViewById(R.id.tvCorreoEnviado)
        tvVolverLogin      = findViewById(R.id.tvVolverLogin)
    }

    // ── Configurar listeners ──────────────────────────────────────────────────
    private fun configurarListeners() {

        // Volver atrás
        btnVolver.setOnClickListener {
            finish()
        }

        // Enviar enlace de recuperación
        btnEnviarEnlace.setOnClickListener {
            val correo = etCorreo.text.toString().trim()

            if (validarCorreo(correo)) {
                enviarEnlaceRecuperacion(correo)
            }
        }

        // Volver al login
        tvVolverLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    // ── Validar correo ────────────────────────────────────────────────────────
    private fun validarCorreo(correo: String): Boolean {
        when {
            correo.isEmpty() -> {
                etCorreo.error = "Ingresa tu correo electrónico"
                etCorreo.requestFocus()
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> {
                etCorreo.error = "Ingresa un correo válido"
                etCorreo.requestFocus()
                return false
            }
        }
        return true
    }

    // ── Enviar enlace de recuperación con Firebase ────────────────────────────
    private fun enviarEnlaceRecuperacion(correo: String) {
        btnEnviarEnlace.isEnabled = false
        btnEnviarEnlace.text = "Enviando..."

        auth.sendPasswordResetEmail(correo)
            .addOnSuccessListener {
                // Mostrar confirmación
                layoutConfirmacion.visibility = View.VISIBLE
                tvCorreoEnviado.text =
                    "Enviamos un enlace de recuperación a:\n$correo\n\nRevisa tu bandeja de entrada y sigue las instrucciones."

                // Deshabilitar campo y botón
                etCorreo.isEnabled = false
                btnEnviarEnlace.text = "¡Enlace enviado!"

                Toast.makeText(
                    this,
                    "Correo de recuperación enviado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                val mensaje = when {
                    e.message?.contains("no user record") == true ||
                            e.message?.contains("INVALID_EMAIL") == true ->
                        "No existe una cuenta con este correo"
                    e.message?.contains("network") == true ->
                        "Sin conexión a internet"
                    else -> "Error al enviar el correo: ${e.message}"
                }
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                btnEnviarEnlace.isEnabled = true
                btnEnviarEnlace.text = "Enviar enlace de recuperación"
            }
    }
}