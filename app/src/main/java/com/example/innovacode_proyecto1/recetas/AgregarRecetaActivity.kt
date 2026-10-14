package com.example.innovacode_proyecto1.recetas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.innovacode_proyecto1.R
import com.example.innovacode_proyecto1.models.IngredienteReceta
import com.example.innovacode_proyecto1.models.Receta
import com.example.innovacode_proyecto1.utils.FormatoUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class AgregarRecetaActivity : AppCompatActivity() {

    private lateinit var tvTitulo: TextView
    private lateinit var etNombre: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var etDescripcion: EditText
    private lateinit var layoutIngredientes: LinearLayout
    private lateinit var tvSinIngredientes: TextView
    private lateinit var btnGuardar: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val ingredientesAgregados = mutableListOf<IngredienteReceta>()
    // Pair: (documentId, nombre_producto)
    private val productosDisponibles  = mutableListOf<Pair<String, String>>()
    private var recetaId: String?     = null
    private var empresaId: String     = "empresa_demo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_receta)

        auth      = FirebaseAuth.getInstance()
        db        = FirebaseFirestore.getInstance()
        recetaId  = intent.getStringExtra("recetaId")
        empresaId = intent.getStringExtra("empresaId") ?: "empresa_demo"

        inicializarVistas()
        configurarSpinner()
        configurarListeners()
        cargarProductosInventario()

        if (recetaId != null) cargarRecetaExistente()
    }

    private fun inicializarVistas() {
        tvTitulo           = findViewById(R.id.tvTituloFormulario)
        etNombre           = findViewById(R.id.etNombreReceta)
        spinnerCategoria   = findViewById(R.id.spinnerCategoria)
        etDescripcion      = findViewById(R.id.etDescripcionReceta)
        layoutIngredientes = findViewById(R.id.layoutIngredientes)
        tvSinIngredientes  = findViewById(R.id.tvSinIngredientes)
        btnGuardar         = findViewById(R.id.btnGuardarReceta)

        if (recetaId != null) tvTitulo.text = "Editar Receta"
    }

    private fun configurarSpinner() {
        val categorias = resources.getStringArray(R.array.categorias_receta).toList()
        val adaptador  = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adaptador
    }

    private fun configurarListeners() {
        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAgregarIngrediente).setOnClickListener {
            mostrarDialogoIngrediente()
        }
        btnGuardar.setOnClickListener { guardarReceta() }
    }

    // ── Cargar productos del inventario usando empresa_id ─────────────────────
    private fun cargarProductosInventario() {
        db.collection("inventario")
            .whereEqualTo("empresa_id", empresaId)
            .get()
            .addOnSuccessListener { snap ->
                productosDisponibles.clear()
                snap.documents.forEach { doc ->
                    val nombre = doc.getString("nombre_producto") ?: return@forEach
                    productosDisponibles.add(Pair(doc.id, nombre))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error cargando inventario", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Diálogo para agregar ingrediente ──────────────────────────────────────
    private fun mostrarDialogoIngrediente() {
        if (productosDisponibles.isEmpty()) {
            Toast.makeText(this, "No hay productos en el inventario", Toast.LENGTH_SHORT).show()
            return
        }

        val vista           = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_ingrediente, null)
        val spinnerProducto = vista.findViewById<Spinner>(R.id.spinnerProductoIngrediente)
        val etCantidad      = vista.findViewById<EditText>(R.id.etCantidadIngrediente)
        val spinnerUnidad   = vista.findViewById<Spinner>(R.id.spinnerUnidadIngrediente)

        val nombresProductos = productosDisponibles.map { it.second }
        val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresProductos)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProducto.adapter = adaptador

        android.app.AlertDialog.Builder(this)
            .setView(vista)
            .setTitle("Agregar ingrediente")
            .setPositiveButton("Agregar") { _, _ ->
                val pos      = spinnerProducto.selectedItemPosition
                val cantidad = etCantidad.text.toString().toDoubleOrNull() ?: 0.0
                val unidad   = spinnerUnidad.selectedItem.toString()

                if (cantidad <= 0) {
                    Toast.makeText(this, "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val ingrediente = IngredienteReceta(
                    productoId     = productosDisponibles[pos].first,
                    nombre_producto= productosDisponibles[pos].second,
                    cantidad       = cantidad,
                    unidad         = unidad
                )
                ingredientesAgregados.add(ingrediente)
                agregarVistaIngrediente(ingrediente)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Vista de ingrediente en el formulario ─────────────────────────────────
    private fun agregarVistaIngrediente(ingrediente: IngredienteReceta) {
        tvSinIngredientes.visibility = View.GONE

        val vista = LayoutInflater.from(this)
            .inflate(R.layout.item_ingrediente_form, layoutIngredientes, false)

        vista.findViewById<TextView>(R.id.tvNombreIngrediente).text =
            ingrediente.nombre_producto
        vista.findViewById<TextView>(R.id.tvCantidadIngrediente).text =
            FormatoUtils.formatearCantidad(ingrediente.cantidad, ingrediente.unidad)

        vista.findViewById<ImageButton>(R.id.btnEliminarIngrediente).setOnClickListener {
            layoutIngredientes.removeView(vista)
            ingredientesAgregados.remove(ingrediente)
            if (ingredientesAgregados.isEmpty()) tvSinIngredientes.visibility = View.VISIBLE
        }

        layoutIngredientes.addView(vista)
    }

    // ── Guardar receta en Firestore ───────────────────────────────────────────
    private fun guardarReceta() {
        val nombre      = etNombre.text.toString().trim()
        val categoria   = spinnerCategoria.selectedItem.toString()
        val descripcion = etDescripcion.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombre.error = "Ingresa el nombre de la receta"
            etNombre.requestFocus()
            return
        }
        if (ingredientesAgregados.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un ingrediente", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardar.isEnabled = false
        btnGuardar.text      = "Guardando..."

        val datos = hashMapOf(
            "nombre"       to nombre,
            "categoria"    to categoria,
            "descripcion"  to descripcion,
            "empresa_id"   to empresaId,
            "ingredientes" to ingredientesAgregados.map {
                mapOf(
                    "productoId"      to it.productoId,
                    "nombre_producto" to it.nombre_producto,
                    "cantidad"        to it.cantidad,
                    "unidad"          to it.unidad
                )
            },
            "fechaCreacion" to System.currentTimeMillis()
        )

        val tarea = if (recetaId != null)
            db.collection("recetas").document(recetaId!!).set(datos)
        else
            db.collection("recetas").add(datos)

        tarea
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Receta guardada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                btnGuardar.isEnabled = true
                btnGuardar.text      = "Guardar Receta"
            }
    }

    // ── Cargar receta existente para editar ───────────────────────────────────
    private fun cargarRecetaExistente() {
        db.collection("recetas").document(recetaId!!)
            .get()
            .addOnSuccessListener { doc ->
                etNombre.setText(doc.getString("nombre") ?: "")
                etDescripcion.setText(doc.getString("descripcion") ?: "")

                @Suppress("UNCHECKED_CAST")
                val ings = doc.get("ingredientes") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                ings.forEach { mapa ->
                    val ing = IngredienteReceta(
                        productoId      = mapa["productoId"] as? String ?: "",
                        nombre_producto = mapa["nombre_producto"] as? String ?: "",
                        cantidad        = (mapa["cantidad"] as? Double) ?: 0.0,
                        unidad          = mapa["unidad"] as? String ?: "und"
                    )
                    ingredientesAgregados.add(ing)
                    agregarVistaIngrediente(ing)
                }
            }
    }
}