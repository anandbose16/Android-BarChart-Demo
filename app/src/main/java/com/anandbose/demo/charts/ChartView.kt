package com.anandbose.demo.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import java.util.*
import kotlin.math.max

private const val TAG_CHARTVIEW = "ChartView"
private const val ANCHOR_LEFT = 0
private const val ANCHOR_RIGHT = 1
private const val ANCHOR_TOP = 2
private const val ANCHOR_BOTTOM = 3

class ChartView : View {
    var xAxisValues: List<String>? = null
    var yAxisValues: List<Float>? = null
    private val isRtl: Boolean
        get() = (layoutDirection == View.LAYOUT_DIRECTION_RTL)
    var valueFormatter: ((Float) -> String) = this::defaultValueFormatter
    
    private var maxBarValue = 0f
    private var pixelPerValue = 0f

    private var dp = 0f
    private var leftPadding = 0f
    private var barSpacing = 0f
    private var barWidth = 0f
    private var bottomPadding = 0f
    private var topPadding = 0f
    private var minHeight = 0f
    private var gridlinesCount = 0

    private val xAxisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yAxisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textBoundsRect = Rect()


    constructor(context: Context) : super(context){
        init(context)
    }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        init(context)
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    private fun init(context: Context) {
        dp = context.resources.displayMetrics.density
        val res = context.resources
        leftPadding = res.getDimension(R.dimen.chartview_left_padding)
        barSpacing = res.getDimension(R.dimen.chartview_bar_spacing)
        barWidth = res.getDimension(R.dimen.chartview_bar_width)
        bottomPadding = res.getDimension(R.dimen.chartview_bottom_padding)
        topPadding = res.getDimension(R.dimen.chartview_top_padding)
        minHeight = res.getDimension(R.dimen.chartview_min_height)
        gridlinesCount = res.getInteger(R.integer.chartview_num_gridlines)
        
        xAxisTextPaint.textSize = res.getDimension(R.dimen.chartview_x_axis_text_size)
        xAxisTextPaint.color = ResourcesCompat.getColor(res, R.color.chartview_x_axis_text_color, null)
        yAxisTextPaint.textSize = res.getDimension(R.dimen.chartview_y_axis_text_size)
        yAxisTextPaint.color = ResourcesCompat.getColor(res, R.color.chartview_y_axis_text_color, null)
        barTextPaint.textSize = res.getDimension(R.dimen.chartview_bar_text_size)
        barTextPaint.color = ResourcesCompat.getColor(res, R.color.chartview_bar_text_color, null)

        gridLinePaint.color = ResourcesCompat.getColor(res, R.color.chartview_grid_line_color, null)
        gridLinePaint.style = Paint.Style.STROKE
        gridLinePaint.strokeWidth = 1f*dp

        barPaint.color = ResourcesCompat.getColor(res, R.color.chartview_bar_color, null)
        barPaint.style = Paint.Style.FILL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (checkValueConsistency()) {
            val barCount = xAxisValues!!.size
            val possibleWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
            val calculatedWidth = leftPadding + barCount*(barSpacing + barWidth) + barSpacing
            val currentHeight = MeasureSpec.getSize(heightMeasureSpec).toFloat()
            val requiredWidth = max(possibleWidth, calculatedWidth)
            val requiredHeight = max(currentHeight, minHeight)
            Log.d(TAG_CHARTVIEW, "onMeasure #1 $requiredWidth x $requiredHeight")
            setMeasuredDimension(requiredWidth.toInt(), requiredHeight.toInt())
        } else {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            val h = MeasureSpec.getSize(heightMeasureSpec)
            Log.d(TAG_CHARTVIEW, "onMeasure #2 $w x $h")
            setMeasuredDimension(w,h)
        }
    }

    override fun onDraw(canvas: Canvas) {
        initialCompute()
        drawHorizontalGridLines(canvas)
        drawHorizontalGridLineText(canvas)
        drawBars(canvas)
    }

    private fun checkValueConsistency(): Boolean {
        val xAxisValues = this.xAxisValues
        val yAxisValues = this.yAxisValues
        if (xAxisValues != null && yAxisValues != null) {
            if (xAxisValues.isEmpty()) {
                Log.e(TAG_CHARTVIEW, "xAxisValues is empty")
                return false
            }
            if (yAxisValues.isEmpty()) {
                Log.e(TAG_CHARTVIEW, "yAxisValues is empty")
                return false
            }
            if (xAxisValues.size != yAxisValues.size) {
                Log.e(TAG_CHARTVIEW, "Both xAxisValues and yAxisValues counts should be same")
                return false
            }
        } else {
            Log.e(TAG_CHARTVIEW, "Both xAxisValues and yAxisValues are required to be null")
            return false
        }
        return true
    }
    
    private fun initialCompute() {
        val max = (yAxisValues?.maxByOrNull { it } ?: 0).toFloat()
        maxBarValue = max + max * 0.25f
        val viewportHeight = height.toFloat() - (topPadding + bottomPadding)
        pixelPerValue = viewportHeight / maxBarValue
    }
    
    private fun drawTextAtAnchor(canvas: Canvas, paint: Paint, text: String, anchor: Int, px: Float, py: Float) {
        paint.getTextBounds(text, 0, text.length, textBoundsRect)
        val w = textBoundsRect.width()
        val h = textBoundsRect.height()
        when (anchor) {
            ANCHOR_LEFT -> {
                val x = px
                val y = py + h.toFloat()/2f
                canvas.drawText(text, x, y,paint)
            }
            ANCHOR_RIGHT -> {
                val x = (px - w)
                val y = py + h.toFloat()/2f
                canvas.drawText(text, x, y,paint)
            }
            ANCHOR_TOP -> {
                val x = px - w.toFloat()/2f
                val y = py + paint.textSize
                canvas.drawText(text, x, y,paint)
            }
            ANCHOR_BOTTOM -> {
                val x = px - w.toFloat()/2f
                val y = py
                canvas.drawText(text, x, y,paint)
            }
        }
    }

    private fun drawHorizontalGridLines(canvas: Canvas) {
        val startX = if (isRtl) 0f else leftPadding
        val endX = if (isRtl) width.toFloat() - leftPadding else width.toFloat()
        val valueDelta = maxBarValue / gridlinesCount.toFloat()
        val pixelDelta = valueDelta * pixelPerValue
        for (i in 0..gridlinesCount) {
            val startY = topPadding + i*pixelDelta
            Log.d(TAG_CHARTVIEW, "drawHorizontalGridLines i:$i $startX $startY $endX $startY")
            canvas.drawLine(startX, startY,endX, startY, gridLinePaint)
        }
    }

    private fun drawHorizontalGridLineText(canvas: Canvas) {
        val x = if (isRtl) canvas.width.toFloat() - leftPadding else leftPadding
        val valueDelta = maxBarValue / gridlinesCount.toFloat()
        val pixelDelta = valueDelta * pixelPerValue
        val xShift = if (isRtl) 8f*dp else -8f*dp
        for (i in 0..gridlinesCount) {
            val y = topPadding + i*pixelDelta
            val value = (maxBarValue - i*valueDelta)
            drawTextAtAnchor(
                canvas,
                yAxisTextPaint,
                valueFormatter(value),
                if (isRtl) ANCHOR_LEFT else ANCHOR_RIGHT,
                x + xShift,
                y
            )
        }
    }

    private fun drawBars(canvas: Canvas) {
        val xAxisValues = this.xAxisValues!!
        val yAxisValues = this.yAxisValues!!
        val top = topPadding
        val bottom = height.toFloat() - bottomPadding

        for (i in yAxisValues.indices) {
            val startLtr = leftPadding + i*(barWidth + barSpacing) + barSpacing
            val endLtr = startLtr + barWidth
            val startRtl = width.toFloat() - leftPadding - i*(barWidth + barSpacing) - barSpacing - barWidth
            val endRtl = startRtl + barWidth
            val start = if (isRtl) startRtl else startLtr
            val end = if (isRtl) endRtl else endLtr
            val value = yAxisValues[i]
            val topShift = (maxBarValue - value) * pixelPerValue
            canvas.drawRect(start, top + topShift, end, bottom, barPaint)
            val key = xAxisValues[i]
            drawTextAtAnchor(canvas, yAxisTextPaint,key, ANCHOR_TOP, start + (end - start)/2f, bottom + 8f*dp)
            drawTextAtAnchor(canvas, barTextPaint, valueFormatter(value), ANCHOR_BOTTOM, start + (end - start)/2f, top + topShift - 8f*dp)
        }
    }

    private fun defaultValueFormatter(x: Float): String = String.format(Locale.US, "%.0f", x)
}