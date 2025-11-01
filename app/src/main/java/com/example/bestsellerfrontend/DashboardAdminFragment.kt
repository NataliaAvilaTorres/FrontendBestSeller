package com.example.bestsellerfrontend

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class DashboardAdminFragment : Fragment() {

    private lateinit var apiService: ApiService

    // TextViews de cards
    private lateinit var tvTotalUsuarios: TextView
    private lateinit var tvTotalProductos: TextView
    private lateinit var tvTotalOfertas: TextView
    private lateinit var tvTotalTiendas: TextView
    private lateinit var tvUltimaActividad: TextView

    // Charts
    private lateinit var chartOfertas: LineChart
    private lateinit var chartCategorias: PieChart

    // RecyclerView
    private lateinit var recyclerTopOfertas: RecyclerView
    private lateinit var topOfertasAdapter: TopOfertasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_dashboard_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarVistas(view)
        configurarRetrofit()
        configurarBotones(view)
        cargarDatos()
    }

    private fun inicializarVistas(view: View) {
        // TextViews
        tvTotalUsuarios = view.findViewById(R.id.tvTotalUsuarios)
        tvTotalProductos = view.findViewById(R.id.tvTotalProductos)
        tvTotalOfertas = view.findViewById(R.id.tvTotalOfertas)
        tvTotalTiendas = view.findViewById(R.id.tvTotalTiendas)
        tvUltimaActividad = view.findViewById(R.id.tvUltimaActividad)

        // Charts
        chartOfertas = view.findViewById(R.id.chartOfertas)
        chartCategorias = view.findViewById(R.id.chartCategorias)

        // RecyclerView
        recyclerTopOfertas = view.findViewById(R.id.recyclerTopOfertas)
        recyclerTopOfertas.layoutManager = LinearLayoutManager(requireContext())
        topOfertasAdapter = TopOfertasAdapter(emptyList())
        recyclerTopOfertas.adapter = topOfertasAdapter

        configurarEstiloCharts()
    }

    private fun configurarRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8090/")
            //.baseUrl("http://192.168.1.13:8090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    private fun configurarBotones(view: View) {

        view.findViewById<ImageView>(R.id.btnRefresh).setOnClickListener {
            cargarDatos()
            Toast.makeText(requireContext(), "Actualizando...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                // Cargar datos en paralelo
                val usuarios = apiService.listarUsuarios()
                val productos = apiService.listarProductos()
                val ofertas = apiService.listarOfertas()
                val tiendas = apiService.listarTiendas()
                val notificaciones = apiService.listarNotificaciones()

                // Actualizar cards
                tvTotalUsuarios.text = usuarios.size.toString()
                tvTotalProductos.text = productos.size.toString()
                tvTotalOfertas.text = ofertas.size.toString()
                tvTotalTiendas.text = tiendas.size.toString()

                // Actualizar gráficos
                actualizarGraficoOfertas(ofertas)
                actualizarGraficoCategorias(productos)
                actualizarTopOfertas(ofertas)
                actualizarUltimaActividad(notificaciones)

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar datos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun configurarEstiloCharts() {
        // Estilo LineChart
        chartOfertas.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
        }

        // Estilo PieChart
        chartCategorias.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(true)
            setEntryLabelTextSize(12f)
            legend.isEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 45f
        }
    }

    private fun actualizarGraficoOfertas(ofertas: List<Oferta>) {
        // Agrupar ofertas por día (últimos 7 días)
        val calendario = Calendar.getInstance()
        val hoy = calendario.timeInMillis
        val hace7Dias = hoy - (7 * 24 * 60 * 60 * 1000)

        val ofertasPorDia = mutableMapOf<String, Int>()
        val formatoDia = SimpleDateFormat("dd/MM", Locale.getDefault())

        // Inicializar últimos 7 días con 0
        for (i in 6 downTo 0) {
            calendario.timeInMillis = hoy - (i * 24 * 60 * 60 * 1000)
            val dia = formatoDia.format(calendario.time)
            ofertasPorDia[dia] = 0
        }

        // Contar ofertas por día
        ofertas.filter { it.fechaOferta >= hace7Dias }.forEach { oferta ->
            val dia = formatoDia.format(Date(oferta.fechaOferta))
            ofertasPorDia[dia] = ofertasPorDia.getOrDefault(dia, 0) + 1
        }

        // Crear entradas para el gráfico
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        ofertasPorDia.entries.forEachIndexed { index, entry ->
            entries.add(Entry(index.toFloat(), entry.value.toFloat()))
            labels.add(entry.key)
        }

        val dataSet = LineDataSet(entries, "Ofertas publicadas").apply {
            color = Color.parseColor("#FF9800")
            setCircleColor(Color.parseColor("#FF9800"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = Color.parseColor("#FF9800")
            fillAlpha = 50
        }

        chartOfertas.data = LineData(dataSet)
        chartOfertas.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() < labels.size) labels[value.toInt()] else ""
            }
        }
        chartOfertas.invalidate()
    }

    private fun actualizarGraficoCategorias(productos: List<Producto>) {
        // Contar productos por categoría
        val productosPorCategoria = mutableMapOf<String, Int>()

        productos.forEach { producto ->
            val categoria = producto.marca.categoria
            productosPorCategoria[categoria] = productosPorCategoria.getOrDefault(categoria, 0) + 1
        }

        // Crear entradas para el gráfico
        val entries = ArrayList<PieEntry>()
        productosPorCategoria.forEach { (categoria, cantidad) ->
            entries.add(PieEntry(cantidad.toFloat(), categoria))
        }

        val dataSet = PieDataSet(entries, "Categorías").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }

        chartCategorias.data = PieData(dataSet)
        chartCategorias.invalidate()
    }

    private fun actualizarTopOfertas(ofertas: List<Oferta>) {
        // Ordenar ofertas por likes (descendente) y tomar las 5 primeras
        val topOfertas = ofertas.sortedByDescending { it.likes }.take(5)
        topOfertasAdapter.actualizarLista(topOfertas)
    }

    private fun actualizarUltimaActividad(notificaciones: List<Notificacion>) {
        if (notificaciones.isEmpty()) {
            tvUltimaActividad.text = "No hay actividad reciente"
            return
        }

        // Ordenar por timestamp (más reciente primero)
        val recientes = notificaciones
            .sortedByDescending { it.timestamp ?: 0 }
            .take(3)

        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val texto = StringBuilder()

        recientes.forEach { notif ->
            val fecha = notif.timestamp?.let { Date(it) }
            texto.append("• ${notif.mensaje}\n")
            texto.append("  ${fecha?.let { formato.format(it) } ?: "Fecha desconocida"}\n\n")
        }

        tvUltimaActividad.text = texto.toString().trim()
    }
}