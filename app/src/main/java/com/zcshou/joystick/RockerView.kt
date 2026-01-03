package com.zcshou.joystick

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.zcshou.gogogo.R
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import java.lang.Math

class RockerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private lateinit var outerCirclePaint: Paint
    private lateinit var innerCirclePaint: Paint
    private lateinit var innerIconPaint: Paint

    /** 内圆中心x坐标 */
    private var innerCenterX: Float = 0f
    /** 内圆中心y坐标 */
    private var innerCenterY: Float = 0f
    /** view中心点x坐标 */
    private var viewCenterX: Float = 0f
    /** view中心点y左边 */
    private var viewCenterY: Float = 0f
    /** 外圆半径 */
    private var outerCircleRadius: Int = 0
    /** 内圆半径 */
    private var innerCircleRadius: Int = 0

    private var mRockerBitmap: Bitmap? = null
    private var isAuto = false
    private var isClick = false

    interface RockerViewClickListener {
        /**
         * 点击的角度信息
         */
        fun clickAngleInfo(auto: Boolean, angle: Double, r: Double)
    }

    private var mListener: RockerViewClickListener? = null

    private var srcRect: Rect? = null
    private var dstRect: Rect? = null

    init {
        init()
    }

    private fun init() {
        outerCirclePaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.grey)
            alpha = 180
            isAntiAlias = true
        }

        innerCirclePaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.lightgrey)
            alpha = 180
            isAntiAlias = true
        }

        innerIconPaint = Paint().apply {
            alpha = 200
            isAntiAlias = true
            isFilterBitmap = true
        }

        isAuto = true
        mRockerBitmap = scaleBitmap(getBitmap(context, R.drawable.ic_lock_close))
        mRockerBitmap?.let {
            srcRect = Rect(0, 0, it.width, it.height)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (dstRect == null) {
            val size = measuredWidth
            setMeasuredDimension(size, size)

            innerCenterX = size / 2f
            innerCenterY = size / 2f
            viewCenterX = size / 2f
            viewCenterY = size / 2f
            outerCircleRadius = size / 2
            innerCircleRadius = size / 5
            
            mRockerBitmap?.let {
                 dstRect = Rect(
                    (innerCenterX - it.width).toInt(),
                    (innerCenterY - it.height).toInt(),
                    (innerCenterX + it.width).toInt(),
                    (innerCenterY + it.height).toInt()
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(viewCenterX, viewCenterY, outerCircleRadius.toFloat(), outerCirclePaint)
        /* 摇杆的控制部分由两部分组成 */
        canvas.drawCircle(innerCenterX, innerCenterY, innerCircleRadius.toFloat(), innerCirclePaint)
        if (mRockerBitmap != null && srcRect != null && dstRect != null) {
            canvas.drawBitmap(mRockerBitmap!!, srcRect, dstRect!!, innerIconPaint)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                /* 如果初始点击位置 不再内圆中,返回 false 将不再继续处理后续事件 */
                if (event.x < innerCenterX - innerCircleRadius || event.x > innerCenterX + innerCircleRadius ||
                    event.y < innerCenterY - innerCircleRadius || event.y > innerCenterY + innerCircleRadius
                ) {
                    return true
                }
                isClick = true
            }
            MotionEvent.ACTION_MOVE -> {
                moveToPosition(event.x, event.y)
                isClick = false
            }
            MotionEvent.ACTION_UP -> {
                if (isClick) {
                    isClick = false
                    toggleLockCtrl()
                    invalidate()
                }
                if (!isAuto) {
                    moveToPosition(viewCenterX, viewCenterY)
                }
                performClick()
            }
        }
        return true
    }

    private fun moveToPosition(x: Float, y: Float) {
        val distance = sqrt((x - viewCenterX).pow(2) + (y - viewCenterY).pow(2))

        if (distance < outerCircleRadius - innerCircleRadius) {
            //在自由域之内，触摸点实时作为内圆圆心
            innerCenterX = x
            innerCenterY = y
        } else {
            //在自由域之外，内圆圆心在触摸点与外圆圆心的线段上
            val innerDistance = (outerCircleRadius - innerCircleRadius).toFloat()
            //相似三角形的性质，两个相似三角形各边比例相等得到等式
            innerCenterX = (x - viewCenterX) * innerDistance / distance + viewCenterX
            innerCenterY = (y - viewCenterY) * innerDistance / distance + viewCenterY
        }

        mRockerBitmap?.let {
             dstRect = Rect(
                (innerCenterX - it.width).toInt(),
                (innerCenterY - it.height).toInt(),
                (innerCenterX + it.width).toInt(),
                (innerCenterY + it.height).toInt()
            )
        }

        invalidate()
        val angle = java.lang.Math.toDegrees(atan2((innerCenterX - viewCenterX).toDouble(), (innerCenterY - viewCenterY).toDouble())) - 90
        val r = (sqrt((innerCenterX - viewCenterX).pow(2) + (innerCenterY - viewCenterY).pow(2)) / (outerCircleRadius - innerCircleRadius)).toDouble()
        mListener?.clickAngleInfo(true, angle, r)
    }

    private fun getBitmap(vectorDrawable: VectorDrawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }

    private fun getBitmap(context: Context, drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId)
        return when (drawable) {
            is BitmapDrawable -> BitmapFactory.decodeResource(context.resources, drawableId)
            is VectorDrawable -> getBitmap(drawable)
            else -> throw IllegalArgumentException("unsupported drawable type")
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val mMatrix = Matrix()
        mMatrix.postScale(0.45f, 0.45f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, mMatrix, true)
    }

    private fun toggleLockCtrl() {
        val bitmap: Bitmap
        isAuto = !isAuto

        bitmap = if (isAuto) {
            getBitmap(context, R.drawable.ic_lock_close)
        } else {
            getBitmap(context, R.drawable.ic_lock_open)
        }

        mRockerBitmap?.recycle()
        mRockerBitmap = scaleBitmap(bitmap)

        mRockerBitmap?.let {
             dstRect = Rect(
                (innerCenterX - it.width).toInt(),
                (innerCenterY - it.height).toInt(),
                (innerCenterX + it.width).toInt(),
                (innerCenterY + it.height).toInt()
            )
        }
    }

    fun setListener(listener: RockerViewClickListener) {
        this.mListener = listener
    }
}
