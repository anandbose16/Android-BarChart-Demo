package com.anandbose.demo.charts

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.anandbose.demo.charts.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    var layoutDirectionOverride = View.LAYOUT_DIRECTION_LTR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.chart.apply {
            xAxisValues = listOf("jan", "feb", "mar", "apr", "may", "june", "july", "aug", "sep", "oct", "nov", "dec")
            yAxisValues = listOf(100f, 80f, 120f, 110f, 90f ,26f, 33f, 95f, 90f ,26f, 33f, 95f)
            valueFormatter = { value -> String.format(Locale.US, "%.0fK", value)}
            postInvalidate()
        }
        binding.btnToggleRtl.setOnClickListener {
            val newLayoutDirection = when (layoutDirectionOverride) {
                View.LAYOUT_DIRECTION_LTR -> View.LAYOUT_DIRECTION_RTL
                View.LAYOUT_DIRECTION_RTL -> View.LAYOUT_DIRECTION_LTR
                else -> View.LAYOUT_DIRECTION_LTR
            }
            binding.chart.layoutDirection = newLayoutDirection
            binding.chartScrollContainer.layoutDirection = newLayoutDirection
            layoutDirectionOverride = newLayoutDirection
            binding.chart.postInvalidate()
        }
    }
}
