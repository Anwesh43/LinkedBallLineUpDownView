package com.anwesh.uiprojects.balllineupdownview

/**
 * Created by anweshmishra on 29/06/19.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color

val nodes : Int = 5
val lines : Int = 3
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#0D47A1")
val backColor : Int = Color.parseColor("#BDBDBD")
val rFactor : Float = 4f
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawBallLineUp(x : Float, y : Float, x1 : Float, y1 : Float, size : Float, paint : Paint) {
    val r : Float = size / rFactor
    drawCircle(x, y, r, paint)
    drawLine(x1, y1, x, y, paint)
}

fun Canvas.drawBallsLineUp(sc : Float, size : Float, paint : Paint) {
    var gap : Float = (2 * size) / lines
    var x : Float = -size
    var y : Float = -size
    var x1 : Float = x
    var y1 : Float = y
    for (j in 0..(lines - 1)) {
        val scj : Float = sc.divideScale(j, lines)
        if (scj > 0) {
            x = x1 + gap * scj
            y = y1 + 2 * size * (1 - 2 * (j % 2)) * scj
        }
        drawBallLineUp(x, y, x1, y1, size, paint)
        x1 += gap * Math.floor(scj.toDouble()).toFloat()
        y1 += 2 * size * (1 - 2 * (j % 2)) * Math.floor(scj.toDouble()).toFloat()
    }
}

fun Canvas.drawBLUDNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    paint.color = foreColor
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    save()
    translate(w / 2, gap * (i + 1))
    rotate(90f * sc2)
    drawBallsLineUp(sc1, size, paint)
    restore()
}

class BallLineUpDownView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)
    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {
                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class BLUDNode(var i : Int, val state : State = State()) {

        private var next : BLUDNode? = null
        private var prev : BLUDNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = BLUDNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawBLUDNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : BLUDNode {
            var curr : BLUDNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class BallLineUpDown(var i : Int) {

        private val root : BLUDNode = BLUDNode(0)
        private var curr : BLUDNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : BallLineUpDownView) {

        private val animator : Animator = Animator(view)
        private val blud : BallLineUpDown = BallLineUpDown(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            blud.draw(canvas, paint)
            animator.animate {
                blud.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            blud.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : BallLineUpDownView {
            val view : BallLineUpDownView = BallLineUpDownView(activity)
            activity.setContentView(view)
            return view
        }
    }
}