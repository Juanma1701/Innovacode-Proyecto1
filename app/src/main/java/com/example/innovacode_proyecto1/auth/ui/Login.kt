package com.example.innovacode_proyecto1.auth.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.innovacode_proyecto1.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {

    // ── Vistas ────────────────────────────────────────────────────────────────
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnVerContrasena: ImageButton
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvOlvideContrasena: TextView
    private lateinit var btnGoogle: Button
    private lateinit var tvCrearCuenta: TextView

    // ── Firebase ──────────────────────────────────────────────────────────────
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    // ── Estado ────────────────────────────────────────────────────────────────
    private var contrasenaVisible = false

    // ── Launcher para Google Sign-In ──────────────────────────────────────────
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val cuenta = task.getResult(ApiException::class.java)
            autenticarConGoogle(cuenta.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Error con Google: ${e.message}", Toast.LENGTH_LONG).show()
            btnGoogle.isEnabled = true
            btnGoogle.text = "Continuar con Google"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        window.statusBarColor = resources.getColor(android.R.color.black, null)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        // Si ya hay sesión activa, ir directo al home
        if (auth.currentUser != null) {
            auth.signOut()
        }

        configurarGoogleSignIn()
        inicializarVistas()
        configurarListeners()

        // ── Fix: quitar foco y teclado al abrir la pantalla ──────────────────
        window.decorView.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    // ── Configurar Google Sign-In ─────────────────────────────────────────────
    private fun configurarGoogleSignIn() {
        val opciones = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("TU_WEB_CLIENT_ID_AQUI")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, opciones)
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

        // Iniciar sesión con correo
        btnIniciarSesion.setOnClickListener {
            val correo     = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()
            if (validarCampos(correo, contrasena)) {
                iniciarSesionFirebase(correo, contrasena)
            }
        }

        // Olvidé contraseña
        tvOlvideContrasena.setOnClickListener {
            startActivity(Intent(this, RecuperarContrasena::class.java))
        }

        // Iniciar sesión con Google
        btnGoogle.setOnClickListener {
            btnGoogle.isEnabled = false
            btnGoogle.text = "Conectando con Google..."
            val intent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(intent)
        }

        // Ir a Registro
        tvCrearCuenta.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }

    // ── Autenticar con Google en Firebase ────────────────────────────────────
    private fun autenticarConGoogle(idToken: String) {
        val credencial = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credencial)
            .addOnSuccessListener { resultado ->
                val usuario = resultado.user ?: return@addOnSuccessListener
                val esNuevo = resultado.additionalUserInfo?.isNewUser ?: false

                if (esNuevo) {
                    val datosUsuario = hashMapOf(
                        "uid"             to usuario.uid,
                        "nombre"          to (usuario.displayName ?: ""),
                        "correo"          to (usuario.email ?: ""),
                        "tipoDocumento"   to "",
                        "numeroDocumento" to "",
                        "telefono"        to "",
                        "fechaRegistro"   to com.google.firebase.Timestamp.now()
                    )

                    db.collection("usuarios")
                        .document(usuario.uid)
                        .set(datosUsuario)
                        .addOnSuccessListener {
                            Toast.makeText(this, "¡Bienvenido ${usuario.displayName}!", Toast.LENGTH_SHORT).show()
                            irAlHome()
                        }
                        .addOnFailureListener {
                            irAlHome()
                        }
                } else {
                    Toast.makeText(this, "¡Bienvenido de nuevo ${usuario.displayName}!", Toast.LENGTH_SHORT).show()
                    irAlHome()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al autenticar con Google: ${e.message}", Toast.LENGTH_LONG).show()
                btnGoogle.isEnabled = true
                btnGoogle.text = "Continuar con Google"
            }
    }

    // ── Iniciar sesión con Firebase correo/contraseña ─────────────────────────
    private fun iniciarSesionFirebase(correo: String, contrasena: String) {
        btnIniciarSesion.isEnabled = false
        btnIniciarSesion.text = "Iniciando sesión..."

        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                irAlHome()
            }
            .addOnFailureListener { e ->
                val mensaje = when {
                    e.message?.contains("no user record") == true ->
                        "No existe una cuenta con este correo"
                    e.message?.contains("password is invalid") == true ||
                            e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                        "Correo o contraseña incorrectos"
                    e.message?.contains("blocked") == true ->
                        "Demasiados intentos fallidos, intenta más tarde"
                    e.message?.contains("network") == true ->
                        "Sin conexión a internet"
                    else -> "Error: ${e.message}"
                }
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                btnIniciarSesion.isEnabled = true
                btnIniciarSesion.text = "Iniciar Sesión"
            }
    }

    // ── Recuperar contraseña ──────────────────────────────────────────────────
    private fun recuperarContrasena() {
        val correo = etCorreo.text.toString().trim()

        if (correo.isEmpty()) {
            etCorreo.error = "Ingresa tu correo para recuperar la contraseña"
            etCorreo.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Ingresa un correo válido"
            etCorreo.requestFocus()
            return
        }

        auth.sendPasswordResetEmail(correo)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Se envió un correo de recuperación a $correo",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No se encontró una cuenta con ese correo",
                    Toast.LENGTH_LONG
                ).show()
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

    // ── Ir al Home ────────────────────────────────────────────────────────────
    private fun irAlHome() {
        // TODO: reemplazar con la pantalla principal cuando esté lista
        Toast.makeText(this, "¡Login exitoso!", Toast.LENGTH_SHORT).show()
    }
}