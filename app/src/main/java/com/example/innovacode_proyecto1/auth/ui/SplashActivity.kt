package com.example.innovacode_proyecto1.auth.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.innovacode_proyecto1.R
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var imgLogo: ImageView
    private lateinit var tvNombre: TextView
    private lateinit var tvSlogan: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        window.statusBarColor = resources.getColor(android.R.color.black, null)

        auth = FirebaseAuth.getInstance()

        imgLogo     = findViewById(R.id.imgLogo)
        tvNombre    = findViewById(R.id.tvNombre)
        tvSlogan    = findViewById(R.id.tvSlogan)
        progressBar = findViewById(R.id.progressBar)

        iniciarAnimaciones()
    }

    private fun iniciarAnimaciones() {
        // Logo aparece primero
        imgLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(700)
            .setStartDelay(200)
            .withEndAction {
                // Nombre aparece después del logo
                tvNombre.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(100)
                    .start()

                // Slogan aparece después del nombre
                tvSlogan.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(300)
                    .start()

                // ProgressBar aparece al final
                progressBar.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay(500)
                    .withEndAction {
                        // Navegar después de las animaciones
                        navegarSiguientePantalla()
                    }
                    .start()
            }
            .start()

        // Posición inicial para animación de entrada
        tvNombre.translationY = 30f
        tvSlogan.translationY = 30f
    }

    private fun navegarSiguientePantalla() {
        // Esperar 1 segundo más para que se vea el loading
        window.decorView.postDelayed({
            val intent = if (auth.currentUser != null) {
                // Si hay sesión activa → ir al home
                // Intent(this, HomeActivity::class.java)
                // Por ahora va al Login hasta que tengas el home listo
                Intent(this, Login::class.java)
            } else {
                // Sin sesión → ir al Login
                Intent(this, Login::class.java)
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Transición suave al salir del splash
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1000)
    }
}