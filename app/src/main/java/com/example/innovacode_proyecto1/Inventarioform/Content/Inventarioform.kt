package com.example.innovacode_proyecto1.Inventarioform.Content

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.innovacode_proyecto1.Inventario.Content.Inventario
import com.example.innovacode_proyecto1.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Calendar

class Inventarioform : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var etNombre: EditText
    private lateinit var etCantidad: EditText
    private lateinit var spinnerUnidad: Spinner
    private lateinit var etCategoria: EditText
    private lateinit var etPrecio: EditText
    private lateinit var etDesc: EditText
    private lateinit var etStockMin: EditText
    private lateinit var etCodigoBarras: EditText
    private lateinit var btnEscanearCodigo: ImageButton
    private lateinit var checkFecha: CheckBox
    private lateinit var layoutFecha: LinearLayout
    private lateinit var btnSeleccionarFecha: Button
    private lateinit var tvFechaSeleccionada: TextView

    private var fechaVencimientoStr: String? = null
    private val unidades = listOf("und", "g", "kg", "ml", "L", "taza", "cucharada", "cucharadita")

    // Lanzador del escaner ZXing
    private val escaner = registerForActivityResult(ScanContract()) { resultado ->
        if (resultado.contents != null) {
            etCodigoBarras.setText(resultado.contents)
            Toast.makeText(this, "Codigo escaneado: ${resultado.contents}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventarioform)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        bindVistas()
        configurarSpinnerUnidad()
        configurarFechaVencimiento()

        btnEscanearCodigo.setOnClickListener { abrirEscaner() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            startActivity(Intent(this, Inventario::class.java))
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener { guardarEnInventario() }
    }

    private fun bindVistas() {
        etNombre            = findViewById(R.id.etName)
        etCantidad          = findViewById(R.id.etQuantity)
        spinnerUnidad       = findViewById(R.id.spinnerUnidad)
        etCategoria         = findViewById(R.id.spinnerCategory)
        etPrecio            = findViewById(R.id.etPrice)
        etDesc              = findViewById(R.id.etDescription)
        etStockMin          = findViewById(R.id.etMinStock)
        etCodigoBarras      = findViewById(R.id.etCodigoBarras)
        btnEscanearCodigo   = findViewById(R.id.btnEscanearCodigo)
        checkFecha          = findViewById(R.id.checkFechaVencimiento)
        layoutFecha         = findViewById(R.id.layoutFechaVencimiento)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada)
    }

    private fun configurarSpinnerUnidad() {
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            unidades
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val vista = super.getView(position, convertView, parent)
                val tv = vista.findViewById<android.widget.TextView>(android.R.id.text1)
                vista.setBackgroundColor(android.graphics.Color.parseColor("#0D0D1A"))
                tv.setTextColor(android.graphics.Color.WHITE)
                tv.textSize = 14f
                return vista
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val vista = super.getDropDownView(position, convertView, parent)
                val tv = vista.findViewById<android.widget.TextView>(android.R.id.text1)
                vista.setBackgroundColor(android.graphics.Color.parseColor("#13131F"))
                tv.setTextColor(android.graphics.Color.WHITE)
                tv.setPadding(32, 24, 32, 24)
                tv.textSize = 14f
                return vista
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnidad.adapter = adapter
    }

    private fun configurarFechaVencimiento() {
        checkFecha.setOnCheckedChangeListener { _, isChecked ->
            layoutFecha.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                fechaVencimientoStr = null
                tvFechaSeleccionada.text = ""
                btnSeleccionarFecha.text = "Seleccionar fecha"
            }
        }
        btnSeleccionarFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, anio, mes, dia ->
                fechaVencimientoStr = "%04d-%02d-%02d".format(anio, mes + 1, dia)
                tvFechaSeleccionada.text = "Vence el %02d/%02d/%04d".format(dia, mes + 1, anio)
                btnSeleccionarFecha.text = "Cambiar fecha"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.minDate = System.currentTimeMillis() }.show()
        }
    }

    private fun abrirEscaner() {
        val opciones = ScanOptions().apply {
            setPrompt("Apunta la camara al codigo de barras")
            setBeepEnabled(true)
            setOrientationLocked(false)
            setBarcodeImageEnabled(false)
        }
        escaner.launch(opciones)
    }

    private fun guardarEnInventario() {
        val nombre       = etNombre.text.toString().trim()
        val cantidad     = etCantidad.text.toString().toDoubleOrNull() ?: 0.0
        val unidad       = spinnerUnidad.selectedItem.toString()
        val precio       = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
        val categoria    = etCategoria.text.toString().trim()
        val desc         = etDesc.text.toString().trim()
        val stockMin     = etStockMin.text.toString().toDoubleOrNull() ?: 0.0
        val codigoBarras = etCodigoBarras.text.toString().trim()

        if (nombre.isEmpty()) { etNombre.error = "El nombre es obligatorio"; etNombre.requestFocus(); return }
        if (cantidad <= 0)    { etCantidad.error = "Cantidad invalida"; etCantidad.requestFocus(); return }
        if (checkFecha.isChecked && fechaVencimientoStr == null) {
            Toast.makeText(this, "Selecciona la fecha de vencimiento", Toast.LENGTH_SHORT).show(); return
        }

        val producto = hashMapOf<String, Any>(
            "nombre_producto"         to nombre,
            "cantidad"                to cantidad,
            "unidad_medida"           to unidad,
            "precio_u"                to precio,
            "categoria"               to categoria,
            "descripcion"             to desc,
            "stock_min"               to stockMin,
            "empresa_id"              to "empresa_demo",
            "fecha_creacion"          to Timestamp.now(),
            "fecha_ultimo_movimiento" to Timestamp.now(),
            "ultimo_movimiento"       to "entrada"
        )
        if (codigoBarras.isNotEmpty()) producto["codigo_barras"] = codigoBarras
        fechaVencimientoStr?.let { producto["fecha_vencimiento"] = it }

        db.collection("inventario").add(producto)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto registrado", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Inventario::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}