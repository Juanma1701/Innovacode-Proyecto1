package com.example.innovacode_proyecto1.recetas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.models.Receta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecetasActivity : AppCompatActivity() {

    private lateinit var recyclerRecetas: RecyclerView
    private lateinit var etBuscar: EditText
    private lateinit var btnVolver: ImageButton
    private lateinit var btnAgregar: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val todasLasRecetas = mutableListOf<Receta>()
    private lateinit var adaptador: RecetasAdapter

    private var empresaId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recetas)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarRecetas)) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        obtenerEmpresaId()
        inicializarVistas()
        configurarRecycler()
        configurarListeners()
    }

    // ── Obtener empresa_id del usuario actual ─────────────────────────────────
    private fun obtenerEmpresaId() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                empresaId = doc.getString("empresa_id") ?: "empresa_demo"
                cargarRecetas()
            }
            .addOnFailureListener {
                empresaId = "empresa_demo"
                cargarRecetas()
            }
    }

    private fun inicializarVistas() {
        recyclerRecetas = findViewById(R.id.recyclerRecetas)
        etBuscar        = findViewById(R.id.etBuscarReceta)
        btnVolver       = findViewById(R.id.btnVolver)
        btnAgregar      = findViewById(R.id.btnAgregarReceta)
    }

    private fun configurarRecycler() {
        adaptador = RecetasAdapter(
            recetas    = todasLasRecetas,
            onEditar   = { receta -> abrirFormulario(receta) },
            onEliminar = { receta -> confirmarEliminar(receta) }
        )
        recyclerRecetas.layoutManager = LinearLayoutManager(this)
        recyclerRecetas.adapter       = adaptador
    }

    private fun configurarListeners() {
        btnVolver.setOnClickListener { finish() }
        btnAgregar.setOnClickListener { abrirFormulario(null) }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filtrarRecetas(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun cargarRecetas() {
        db.collection("recetas")
            .whereEqualTo("empresa_id", empresaId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Toast.makeText(this, "Error cargando recetas", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                todasLasRecetas.clear()
                snap?.documents?.forEach { doc ->
                    val receta = doc.toObject(Receta::class.java)
                    if (receta != null) todasLasRecetas.add(receta.copy(id = doc.id))
                }
                adaptador.actualizarLista(todasLasRecetas)
            }
    }

    private fun filtrarRecetas(query: String) {
        val filtradas = if (query.isEmpty()) todasLasRecetas
        else todasLasRecetas.filter {
            it.nombre.contains(query, ignoreCase = true) ||
                    it.categoria.contains(query, ignoreCase = true)
        }
        adaptador.actualizarLista(filtradas)
    }

    private fun abrirFormulario(receta: Receta?) {
        val intent = Intent(this, AgregarRecetaActivity::class.java)
        if (receta != null) {
            intent.putExtra("recetaId", receta.id)
            intent.putExtra("empresaId", empresaId)
        } else {
            intent.putExtra("empresaId", empresaId)
        }
        startActivity(intent)
    }

    private fun confirmarEliminar(receta: Receta) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Eliminar receta")
            .setMessage("¿Eliminar '${receta.nombre}'? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> eliminarReceta(receta) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarReceta(receta: Receta) {
        db.collection("recetas").document(receta.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Receta eliminada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }
}