package com.example.innovacode_proyecto1.Proveedores.Content

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.Proveedores.Adapter.ProveedoresAdapter
import com.example.innovacode_proyecto1.Proveedores.Proveedor
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProveedoresActivity : AppCompatActivity() {

    // ── Vistas ────────────────────────────────────────────────────────────────
    private lateinit var btnBack: View
    private lateinit var etBuscar: EditText
    private lateinit var rvProveedores: RecyclerView
    private lateinit var fabAgregar: FloatingActionButton
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvContador: TextView

    // ── Firebase ──────────────────────────────────────────────────────────────
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Adapter y datos ───────────────────────────────────────────────────────
    private val listaCompleta  = mutableListOf<Proveedor>()
    private val listaFiltrada  = mutableListOf<Proveedor>()
    private lateinit var adapter: ProveedoresAdapter

    // ── Constante de resultado para saber si se agregó/editó ─────────────────
    companion object {
        const val REQUEST_AGREGAR = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proveedores)

        initViews()
        configurarRecyclerView()
        configurarBusqueda()
        cargarProveedores()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Recargar lista si se agregó o editó un proveedor
        if (requestCode == REQUEST_AGREGAR && resultCode == RESULT_OK) {
            cargarProveedores()
        }
    }

    // ── Inicializar vistas ────────────────────────────────────────────────────
    private fun initViews() {
        btnBack      = findViewById(R.id.btnBack)
        etBuscar     = findViewById(R.id.etBuscarProveedor)
        rvProveedores = findViewById(R.id.rvProveedores)
        fabAgregar   = findViewById(R.id.fabAgregarProveedor)
        layoutEmpty  = findViewById(R.id.layoutEmptyProveedores)
        tvContador   = findViewById(R.id.tvContadorProveedores)

        btnBack.setOnClickListener { finish() }

        fabAgregar.setOnClickListener {
            startActivityForResult(
                Intent(this, AgregarProveedorActivity::class.java),
                REQUEST_AGREGAR
            )
        }
    }

    // ── Configurar RecyclerView ───────────────────────────────────────────────
    private fun configurarRecyclerView() {
        adapter = ProveedoresAdapter(
            proveedores = listaFiltrada,
            onEditar    = { proveedor -> abrirEdicion(proveedor) },
            onEliminar  = { proveedor -> confirmarEliminar(proveedor) }
        )
        rvProveedores.layoutManager = LinearLayoutManager(this)
        rvProveedores.adapter = adapter
    }

    // ── Búsqueda en tiempo real ───────────────────────────────────────────────
    private fun configurarBusqueda() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtrar(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filtrar(query: String) {
        val texto = query.lowercase().trim()
        listaFiltrada.clear()

        if (texto.isEmpty()) {
            listaFiltrada.addAll(listaCompleta)
        } else {
            listaFiltrada.addAll(listaCompleta.filter {
                it.nombre.lowercase().contains(texto) ||
                        it.nit.lowercase().contains(texto) ||
                        it.contacto.lowercase().contains(texto)
            })
        }

        adapter.notifyDataSetChanged()
        actualizarEstadoVacio()
    }

    // ── Cargar proveedores desde Firestore ────────────────────────────────────
    private fun cargarProveedores() {
        val empresaId = "empresa_demo"

        db.collection("proveedores")
            .whereEqualTo("empresa_id", empresaId)
            .get()
            .addOnSuccessListener { snapshot ->
                listaCompleta.clear()

                for (doc in snapshot.documents) {
                    val proveedor = Proveedor(
                        id            = doc.id,
                        nombre        = doc.getString("nombre") ?: "",
                        nit           = doc.getString("nit") ?: "",
                        contacto      = doc.getString("contacto") ?: "",
                        email         = doc.getString("email") ?: "",
                        empresa_id    = doc.getString("empresa_id") ?: "",
                        fecha_creacion = doc.getTimestamp("fecha_creacion")
                    )
                    listaCompleta.add(proveedor)
                }

                listaFiltrada.clear()
                listaFiltrada.addAll(listaCompleta)
                adapter.notifyDataSetChanged()
                actualizarEstadoVacio()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar proveedores", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Abrir formulario de edición ───────────────────────────────────────────
    private fun abrirEdicion(proveedor: Proveedor) {
        val intent = Intent(this, AgregarProveedorActivity::class.java).apply {
            putExtra("proveedor_id",      proveedor.id)
            putExtra("proveedor_nombre",  proveedor.nombre)
            putExtra("proveedor_nit",     proveedor.nit)
            putExtra("proveedor_contacto", proveedor.contacto)
            putExtra("proveedor_email",   proveedor.email)
        }
        startActivityForResult(intent, REQUEST_AGREGAR)
    }

    // ── Confirmar eliminación ─────────────────────────────────────────────────
    private fun confirmarEliminar(proveedor: Proveedor) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar proveedor")
            .setMessage("¿Eliminar a ${proveedor.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> eliminarProveedor(proveedor) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarProveedor(proveedor: Proveedor) {
        db.collection("proveedores")
            .document(proveedor.id)
            .delete()
            .addOnSuccessListener {
                adapter.eliminarItem(proveedor)
                listaCompleta.removeAll { it.id == proveedor.id }
                actualizarEstadoVacio()
                Toast.makeText(this, "Proveedor eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Estado vacío ──────────────────────────────────────────────────────────
    private fun actualizarEstadoVacio() {
        val cantidad = listaFiltrada.size
        tvContador.text = "$cantidad proveedor${if (cantidad != 1) "es" else ""}"

        if (listaFiltrada.isEmpty()) {
            rvProveedores.visibility = View.GONE
            layoutEmpty.visibility   = View.VISIBLE
        } else {
            rvProveedores.visibility = View.VISIBLE
            layoutEmpty.visibility   = View.GONE
        }
    }
}