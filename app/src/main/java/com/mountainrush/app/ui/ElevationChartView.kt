package com.mountainrush.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.mountainrush.app.data.TrackPoint

/**
 * Profilo altimetrico stile telemetria racing.
 * Riempimento gradient rosso→arancio, linea bianca brillante, griglia, etichette min/max.
 */
class ElevationChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var points: List<TrackPoint> = emptyList()
    private val padding = 24f

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2A38")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 180
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD93D")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A0A0B0")
        textSize = 22f
        isFakeBoldText = true
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#606070")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    fun setData(pts: List<TrackPoint>) {
        points = pts
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) {
            canvas.drawText("NO DATA", width / 2f, height / 2f, emptyPaint)
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val chartW = w - padding * 2
        val chartH = h - padding * 2

        // Estremi altitudine
        val alts = points.map { it.altitude }
        val minA = alts.min()
        val maxA = alts.max()
        val rangeA = (maxA - minA).coerceAtLeast(1.0)

        // 4 linee orizzontali di griglia
        for (i in 0..4) {
            val y = padding + chartH * i / 4
            canvas.drawLine(padding, y, w - padding, y, gridPaint)
        }

        // Costruisce il path
        val linePath = Path()
        val fillPath = Path()
        val n = points.size

        points.forEachIndexed { i, p ->
            val x = padding + chartW * i / (n - 1).toFloat()
            val normalized = ((p.altitude - minA) / rangeA).toFloat()
            val y = padding + chartH * (1f - normalized)
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, padding + chartH)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w - padding, padding + chartH)
        fillPath.close()

        // Gradient verticale per il fill
        fillPaint.shader = LinearGradient(
            0f, padding, 0f, padding + chartH,
            intArrayOf(
                Color.parseColor("#FF1F3D"),
                Color.parseColor("#80FF6B1F"),
                Color.parseColor("#10FF6B1F")
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        // Etichette min/max
        canvas.drawText("${maxA.toInt()}m", padding + 4, padding + 20, labelPaint)
        canvas.drawText("${minA.toInt()}m", padding + 4, h - padding - 6, labelPaint)
    }
}
