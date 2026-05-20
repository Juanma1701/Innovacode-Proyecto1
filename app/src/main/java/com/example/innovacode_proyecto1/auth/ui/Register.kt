package com.example.innovacode_proyecto1.auth.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.innovacode_proyecto1.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {

    private lateinit var etNombreUsuario: EditText
    private lateinit var spinnerTipoDocumento: Spinner
    private lateinit var etNumeroDocumento: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText
    private lateinit var btnVerContrasena: ImageButton
    private lateinit var btnVerConfirmarContrasena: ImageButton
    private lateinit var btnCrearCuenta: Button
    private lateinit var btnCancelar: Button
    private lateinit var tvIniciarSesion: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var contrasenaVisible          = false
    private var confirmarContrasenaVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        window.statusBarColor = resources.getColor(android.R.color.black, null)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        inicializarVistas()
        configurarSpinner()
        configurarListeners()
    }

    private fun inicializarVistas() {
        etNombreUsuario           = findViewById(R.id.etNombreUsuario)
        spinnerTipoDocumento      = findViewById(R.id.spinnerTipoDocumento)
        etNumeroDocumento         = findViewById(R.id.etNumeroDocumento)
        etTelefono                = findViewById(R.id.etTelefono)
        etCorreo                  = findViewById(R.id.etCorreo)
        etContrasena              = findViewById(R.id.etContrasena)
        etConfirmarContrasena     = findViewById(R.id.etConfirmarContrasena)
        btnVerContrasena          = findViewById(R.id.btnVerContrasena)
        btnVerConfirmarContrasena = findViewById(R.id.btnVerConfirmarContrasena)
        btnCrearCuenta            = findViewById(R.id.btnCrearCuenta)
        btnCancelar               = findViewById(R.id.btnCancelar)
        tvIniciarSesion           = findViewById(R.id.tvIniciarSesion)
    }

    private fun configurarSpinner() {
        val tipos = listOf(
            "Selecciona el tipo",
            "Cédula de ciudadanía",
            "Cédula de extranjería",
            "Pasaporte",
            "Tarjeta de identidad",
            "NIT",
            "RUT"
        )

        val adaptador = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            tipos
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val vista = super.getView(position, convertView, parent)
                val tv = vista.findViewById<TextView>(android.R.id.text1)
                tv.setTextColor(
                    if (position == 0) resources.getColor(android.R.color.darker_gray, null)
                    else resources.getColor(android.R.color.white, null)
                )
                tv.textSize = 14f
                return vista
            }

            override fun isEnabled(position: Int) = position != 0

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val vista = super.getDropDownView(position, convertView, parent)
                val tv = vista.findViewById<TextView>(android.R.id.text1)
                tv.setTextColor(
                    if (position == 0) resources.getColor(android.R.color.darker_gray, null)
                    else resources.getColor(android.R.color.white, null)
                )
                tv.setPadding(24, 20, 24, 20)
                tv.textSize = 14f
                return vista
            }
        }

        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoDocumento.adapter = adaptador
    }

    private fun configurarListeners() {

        btnVerContrasena.setOnClickListener {
            contrasenaVisible = !contrasenaVisible
            alternarVisibilidad(etContrasena, btnVerContrasena, contrasenaVisible)
        }

        btnVerConfirmarContrasena.setOnClickListener {
            confirmarContrasenaVisible = !confirmarContrasenaVisible
            alternarVisibilidad(etConfirmarContrasena, btnVerConfirmarContrasena, confirmarContrasenaVisible)
        }

        btnCrearCuenta.setOnClickListener {
            val nombre     = etNombreUsuario.text.toString().trim()
            val tipoDoc    = spinnerTipoDocumento.selectedItemPosition
            val numDoc     = etNumeroDocumento.text.toString().trim()
            val telefono   = etTelefono.text.toString().trim()
            val correo     = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()
            val confirmar  = etConfirmarContrasena.text.toString().trim()

            if (validarCampos(nombre, tipoDoc, numDoc, telefono, correo, contrasena, confirmar)) {
                registrarEnFirebase(nombre, tipoDoc, numDoc, telefono, correo, contrasena)
            }
        }

        btnCancelar.setOnClickListener { finish() }

        tvIniciarSesion.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun registrarEnFirebase(
        nombre: String,
        tipoDoc: Int,
        numDoc: String,
        telefono: String,
        correo: String,
        contrasena: String
    ) {
        btnCrearCuenta.isEnabled = false
        btnCrearCuenta.text = "Creando cuenta..."

        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid ?: return@addOnSuccessListener

                val tipoDocNombre = spinnerTipoDocumento.getItemAtPosition(tipoDoc).toString()

                val datosUsuario = hashMapOf(
                    "uid"             to uid,
                    "nombre"          to nombre,
                    "tipoDocumento"   to tipoDocNombre,
                    "numeroDocumento" to numDoc,
                    "telefono"        to telefono,
                    "correo"          to correo,
                    "fechaRegistro"   to com.google.firebase.Timestamp.now()
                )

                db.collection("usuarios")
                    .document(uid)
                    .set(datosUsuario)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show()
                        // Ir al Login después del registro
                        val intent = Intent(this, Login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                        btnCrearCuenta.isEnabled = true
                        btnCrearCuenta.text = "Crear Cuenta"
                    }
            }
            .addOnFailureListener { e ->
                val mensaje = when {
                    e.message?.contains("email address is already in use") == true ->
                        "Este correo ya está registrado"
                    e.message?.contains("badly formatted") == true ->
                        "El correo no tiene un formato válido"
                    e.message?.contains("password is invalid") == true ->
                        "La contraseña debe tener al menos 6 caracteres"
                    else -> "Error: ${e.message}"
                }
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                btnCrearCuenta.isEnabled = true
                btnCrearCuenta.text = "Crear Cuenta"
            }
    }

    private fun alternarVisibilidad(campo: EditText, boton: ImageButton, visible: Boolean) {
        if (visible) {
            campo.transformationMethod = HideReturnsTransformationMethod.getInstance()
            boton.setImageResource(R.drawable.ic_ojo_abierto)
        } else {
            campo.transformationMethod = PasswordTransformationMethod.getInstance()
            boton.setImageResource(R.drawable.ic_ojo_cerrado)
        }
        campo.setSelection(campo.text.length)
    }

    private fun validarCampos(
        nombre: String, tipoDoc: Int, numDoc: String,
        telefono: String, correo: String, contrasena: String, confirmar: String
    ): Boolean {
        when {
            nombre.isEmpty() -> {
                etNombreUsuario.error = "Ingresa tu nombre de usuario"
                etNombreUsuario.requestFocus(); return false
            }
            nombre.length < 3 -> {
                etNombreUsuario.error = "Mínimo 3 caracteres"
                etNombreUsuario.requestFocus(); return false
            }
            tipoDoc == 0 -> {
                Toast.makeText(this, "Selecciona el tipo de documento", Toast.LENGTH_SHORT).show()
                return false
            }
            numDoc.isEmpty() -> {
                etNumeroDocumento.error = "Ingresa tu número de documento"
                etNumeroDocumento.requestFocus(); return false
            }
            numDoc.length < 10 -> {
                etNumeroDocumento.error = "Número de documento inválido"
                etNumeroDocumento.requestFocus(); return false
            }
            telefono.isEmpty() -> {
                etTelefono.error = "Ingresa tu número de teléfono"
                etTelefono.requestFocus(); return false
            }
            telefono.length < 10 -> {
                etTelefono.error = "Número de teléfono inválido"
                etTelefono.requestFocus(); return false
            }
            correo.isEmpty() -> {
                etCorreo.error = "Ingresa tu correo electrónico"
                etCorreo.requestFocus(); return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> {
                etCorreo.error = "Correo electrónico inválido"
                etCorreo.requestFocus(); return false
            }
            contrasena.isEmpty() -> {
                etContrasena.error = "Ingresa una contraseña"
                etContrasena.requestFocus(); return false
            }
            contrasena.length < 6 -> {
                etContrasena.error = "Mínimo 6 caracteres"
                etContrasena.requestFocus(); return false
            }
            confirmar.isEmpty() -> {
                etConfirmarContrasena.error = "Confirma tu contraseña"
                etConfirmarContrasena.requestFocus(); return false
            }
            contrasena != confirmar -> {
                etConfirmarContrasena.error = "Las contraseñas no coinciden"
                etConfirmarContrasena.requestFocus(); return false
            }
        }
        return true
    }
}