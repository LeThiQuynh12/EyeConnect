package com.example.eyeconnect.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class EyeIllustrationView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

  private fun drawBlob(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    color: Int,
    rotation: Float = 0f
  ) {
    canvas.save()
    canvas.rotate(rotation, cx, cy)

    paint.color = color
    paint.style = Paint.Style.FILL

    canvas.drawRoundRect(
      cx - rx,
      cy - ry,
      cx + rx,
      cy + ry,
      ry * 0.65f,
      ry * 0.65f,
      paint
    )

    canvas.restore()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    if (width == 0 || height == 0) return

    val sx = width / 360f
    val sy = height / 150f

    canvas.save()
    canvas.scale(sx, sy)

    // Light mint shapes
    drawBlob(
      canvas, 43f, 83f, 40f, 45f,
      Color.rgb(190, 235, 225), -12f
    )

    drawBlob(
      canvas, 104f, 89f, 43f, 34f,
      Color.rgb(164, 222, 211), -7f
    )

    drawBlob(
      canvas, 135f, 45f, 42f, 42f,
      Color.rgb(211, 242, 235)
    )

    // Teal shapes
    drawBlob(
      canvas, 222f, 58f, 43f, 40f,
      Color.rgb(91, 165, 169), -9f
    )

    drawBlob(
      canvas, 295f, 77f, 40f, 44f,
      Color.rgb(65, 143, 151), -8f
    )

    drawBlob(
      canvas, 197f, 98f, 43f, 42f,
      Color.rgb(91, 165, 169)
    )

    // White eye
    val eyePath = Path().apply {
      moveTo(150f, 75f)
      cubicTo(164f, 55f, 178f, 54f, 192f, 75f)
      cubicTo(178f, 96f, 164f, 95f, 150f, 75f)
      close()
    }

    paint.color = Color.WHITE
    canvas.drawPath(eyePath, paint)

    // Teal iris
    paint.color = Color.rgb(8, 127, 140)
    canvas.drawCircle(171f, 75f, 14f, paint)

    // Dark pupil
    paint.color = Color.rgb(24, 48, 55)
    canvas.drawCircle(173f, 72f, 8f, paint)

    // Eye highlight
    paint.color = Color.WHITE
    canvas.drawCircle(177f, 67f, 3f, paint)

    canvas.restore()
  }
}
