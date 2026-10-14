package com.example.innovacode_proyecto1.Proveedores.Content

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.innovacode_proyecto1.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AgregarProveedorActivity : AppCompatActivity() {

    // ── Vistas ────────────────────────────────────────────────────────────────
    private lateinit var btnBack: View
    private lateinit var tvTitulo: TextView
    private lateinit var etNombre: EditText
    private lateinit var etNit: EditText
    private lateinit var etContacto: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnGuardar: Button

    // ── Firebase ──────────────────────────────────────────────────────────────
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Modo edición ──────────────────────────────────────────────────────────
    private var proveedorId: String? = null   // null = modo agregar
    private var esEdicion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_proveedor)

        initViews()
        verificarModoEdicion()
    }

    // ── Inicializar vistas ────────────────────────────────────────────────────
    private fun initViews() {
        btnBack    = findViewById(R.id.btnBack)
        tvTitulo   = findViewById(R.id.tvTituloFormulario)
        etNombre   = findViewById(R.id.etNombreProveedor)
        etNit      = findViewById(R.id.etNitProveedor)
        etContacto = findViewById(R.id.etContactoProveedor)
        etEmail    = findViewById(R.id.etEmailProveedor)
        btnGuardar = findViewById(R.id.btnGuardarProveedor)

        btnBack.setOnClickListener { finish() }
        btnGuardar.setOnClickListener { guardarProveedor() }
    }

    // ── Revisar si viene con datos para editar ────────────────────────────────
    private fun verificarModoEdicion() {
        proveedorId = intent.getStringExtra("proveedor_id")
        esEdicion   = !proveedorId.isNullOrEmpty()

        if (esEdicion) {
            tvTitulo.text = "Editar Proveedor"
            btnGuardar.text = "Actualizar"

            // Pre-llenar campos con datos existentes
            etNombre.setText(intent.getStringExtra("proveedor_nombre") ?: "")
            etNit.setText(intent.getStringExtra("proveedor_nit") ?: "")
            etContacto.setText(intent.getStringExtra("proveedor_contacto") ?: "")
            etEmail.setText(intent.getStringExtra("proveedor_email") ?: "")
        } else {
            tvTitulo.text = "Nuevo Proveedor"
        }
    }

    // ── Guardar o actualizar proveedor ────────────────────────────────────────
    private fun guardarProveedor() {
        val nombre   = etNombre.text.toString().trim()
        val nit      = etNit.text.toString().trim()
        val contacto = etContacto.text.toString().trim()
        val email    = etEmail.text.toString().trim()

        // Validación mínima
        if (nombre.isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            etNombre.requestFocus()
            return
        }

        btnGuardar.isEnabled = false
        btnGuardar.text = if (esEdicion) "Actualizando..." else "Guardando..."

        val empresaId = "empresa_demo"

        val datos = hashMapOf<String, Any>(
            "nombre"         to nombre,
            "nit"            to nit,
            "contacto"       to contacto,
            "email"          to email,
            "empresa_id"     to empresaId
        )

        if (esEdicion) {
            // ── Actualizar documento existente ────────────────────────────────
            db.collection("proveedores")
                .document(proveedorId!!)
                .update(datos as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Proveedor actualizado ✓", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Actualizar"
                }
        } else {
            // ── Crear nuevo documento ─────────────────────────────────────────
            datos["fecha_creacion"] = Timestamp.now()

            db.collection("proveedores")
                .add(datos)
                .addOnSuccessListener {
                    Toast.makeText(this, "Proveedor registrado ✓", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar"
                }
        }
    }
}