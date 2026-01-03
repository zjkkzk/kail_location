package com.zcshou.joystick

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.zcshou.gogogo.R

class ButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface ButtonViewClickListener {
        /**
         * 点击的角度信息
         */
        fun clickAngleInfo(auto: Boolean, angle: Double, r: Double)
    }

    private var mListener: ButtonViewClickListener? = null
    private var isCenter = true
    private lateinit var btnCenter: ImageButton
    private var isNorth = false
    private lateinit var btnNorth: ImageButton
    private var isSouth = false
    private lateinit var btnSouth: ImageButton
    private var isWest = false
    private lateinit var btnWest: ImageButton
    private var isEast = false
    private lateinit var btnEast: ImageButton
    private var isEastNorth = false
    private lateinit var btnEastNorth: ImageButton
    private var isEastSouth = false
    private lateinit var btnEastSouth: ImageButton
    private var isWestNorth = false
    private lateinit var btnWestNorth: ImageButton
    private var isWestSouth = false
    private lateinit var btnWestSouth: ImageButton

    init {
        LayoutInflater.from(context).inflate(R.layout.joystick_button, this)
        initButtonView()
    }

    private fun initButtonView() {
        btnCenter = findViewById(R.id.btn_center)
        btnCenter.setOnClickListener {
            if (!isCenter) {
                isCenter = true
                btnCenter.setImageResource(R.drawable.ic_lock_close)
                btnCenter.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))
            } else {
                isCenter = false
                btnCenter.setImageResource(R.drawable.ic_lock_open)
                btnCenter.setColorFilter(ContextCompat.getColor(context, R.color.black))

                if (isNorth) {
                    isNorth = false
                    btnNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                }
                if (isSouth) {
                    isSouth = false
                    btnSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                }
                if (isWest) {
                    isWest = false
                    btnWest.setColorFilter(ContextCompat.getColor(context, R.color.black))
                }
                if (isEast) {
                    isEast = false
                    btnEast.setColorFilter(ContextCompat.getColor(context, R.color.black))
                }
                if (isEastNorth) {
                    isEastNorth = false
                    btnEastNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                }
                if (isEastSouth) {
                    isEastSouth = false
                    btnEastSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                }
                if (isWestNorth) {
                    isWestNorth = false
                    btnWestNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                }
                if (isWestSouth) {
                    isWestSouth = false
                    btnWestSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                }
                mListener?.clickAngleInfo(false, 0.0, 0.0)
            }
        }
        /* 默认 */
        isCenter = true
        btnCenter.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))

        isNorth = false
        btnNorth = findViewById(R.id.btn_north)
        btnNorth.setOnClickListener {
            if (isCenter) {
                if (!isNorth) {
                    isNorth = true
                    btnNorth.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))

                    isSouth = false
                    btnSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWest = false
                    btnWest.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEast = false
                    btnEast.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastNorth = false
                    btnEastNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastSouth = false
                    btnEastSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestNorth = false
                    btnWestNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestSouth = false
                    btnWestSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    mListener?.clickAngleInfo(true, 90.0, 1.0)
                } else {
                    isNorth = false
                    btnNorth.setImageResource(R.drawable.ic_up)
                    mListener?.clickAngleInfo(false, 90.0, 0.0)
                }
            } else {
                mListener?.clickAngleInfo(false, 90.0, 1.0)
            }
        }

        isSouth = false
        btnSouth = findViewById(R.id.btn_south)
        btnSouth.setOnClickListener {
            if (isCenter) {
                if (!isSouth) {
                    isSouth = true
                    btnSouth.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))

                    isNorth = false
                    btnNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWest = false
                    btnWest.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEast = false
                    btnEast.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastNorth = false
                    btnEastNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastSouth = false
                    btnEastSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestNorth = false
                    btnWestNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestSouth = false
                    btnWestSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))

                    mListener?.clickAngleInfo(true, 270.0, 1.0)
                } else {
                    isSouth = false
                    btnSouth.setImageResource(R.drawable.ic_down)
                    mListener?.clickAngleInfo(false, 270.0, 0.0)
                }
            } else {
                mListener?.clickAngleInfo(false, 270.0, 1.0)
            }
        }

        isWest = false
        btnWest = findViewById(R.id.btn_west)
        btnWest.setOnClickListener {
            if (isCenter) {
                if (!isWest) {
                    isWest = true
                    btnWest.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))

                    isSouth = false
                    btnSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isNorth = false
                    btnNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEast = false
                    btnEast.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastNorth = false
                    btnEastNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastSouth = false
                    btnEastSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestNorth = false
                    btnWestNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestSouth = false
                    btnWestSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))

                    mListener?.clickAngleInfo(true, 180.0, 1.0)
                } else {
                    isWest = false
                    btnWest.setImageResource(R.drawable.ic_left)
                    mListener?.clickAngleInfo(false, 180.0, 0.0)
                }
            } else {
                mListener?.clickAngleInfo(false, 180.0, 1.0)
            }
        }

        isEast = false
        btnEast = findViewById(R.id.btn_east)
        btnEast.setOnClickListener {
            if (isCenter) {
                if (!isEast) {
                    isEast = true
                    btnEast.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))

                    isSouth = false
                    btnSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isNorth = false
                    btnNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWest = false
                    btnWest.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastNorth = false
                    btnEastNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastSouth = false
                    btnEastSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestNorth = false
                    btnWestNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestSouth = false
                    btnWestSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))

                    mListener?.clickAngleInfo(true, 0.0, 1.0)
                } else {
                    isEast = false
                    btnEast.setImageResource(R.drawable.ic_right)
                    mListener?.clickAngleInfo(false, 0.0, 0.0)
                }
            } else {
                mListener?.clickAngleInfo(false, 0.0, 1.0)
            }
        }

        isEastNorth = false
        btnEastNorth = findViewById(R.id.btn_north_east)
        btnEastNorth.setOnClickListener {
            if (isCenter) {
                if (!isEastNorth) {
                    isEastNorth = true
                    btnEastNorth.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))

                    isSouth = false
                    btnSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isNorth = false
                    btnNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWest = false
                    btnWest.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEast = false
                    btnEast.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastSouth = false
                    btnEastSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestNorth = false
                    btnWestNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestSouth = false
                    btnWestSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))

                    mListener?.clickAngleInfo(true, 45.0, 1.0)
                } else {
                    isEastNorth = false
                    btnEastNorth.setImageResource(R.drawable.ic_right_up)
                    mListener?.clickAngleInfo(false, 45.0, 0.0)
                }
            } else {
                mListener?.clickAngleInfo(false, 45.0, 1.0)
            }
        }

        isEastSouth = false
        btnEastSouth = findViewById(R.id.btn_south_east)
        btnEastSouth.setOnClickListener {
            if (isCenter) {
                if (!isEastSouth) {
                    isEastSouth = true
                    btnEastSouth.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))

                    isSouth = false
                    btnSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isNorth = false
                    btnNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWest = false
                    btnWest.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEast = false
                    btnEast.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastNorth = false
                    btnEastNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestNorth = false
                    btnWestNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestSouth = false
                    btnWestSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))

                    mListener?.clickAngleInfo(true, 315.0, 1.0)
                } else {
                    isEastSouth = false
                    btnEastSouth.setImageResource(R.drawable.ic_right_down)
                    mListener?.clickAngleInfo(false, 315.0, 0.0)
                }
            } else {
                mListener?.clickAngleInfo(false, 315.0, 1.0)
            }
        }

        isWestNorth = false
        btnWestNorth = findViewById(R.id.btn_north_west)
        btnWestNorth.setOnClickListener {
            if (isCenter) {
                if (!isWestNorth) {
                    isWestNorth = true
                    btnWestNorth.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))

                    isSouth = false
                    btnSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isNorth = false
                    btnNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWest = false
                    btnWest.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEast = false
                    btnEast.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastNorth = false
                    btnEastNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastSouth = false
                    btnEastSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestSouth = false
                    btnWestSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))

                    mListener?.clickAngleInfo(true, 135.0, 1.0)
                } else {
                    isWestNorth = false
                    btnWestNorth.setImageResource(R.drawable.ic_left_up)
                    mListener?.clickAngleInfo(false, 135.0, 0.0)
                }
            } else {
                mListener?.clickAngleInfo(false, 135.0, 1.0)
            }
        }

        isWestSouth = false
        btnWestSouth = findViewById(R.id.btn_south_west)
        btnWestSouth.setOnClickListener {
            if (isCenter) {
                if (!isWestSouth) {
                    isWestSouth = true
                    btnWestSouth.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent))

                    isSouth = false
                    btnSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isNorth = false
                    btnNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWest = false
                    btnWest.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEast = false
                    btnEast.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastNorth = false
                    btnEastNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isEastSouth = false
                    btnEastSouth.setColorFilter(ContextCompat.getColor(context, R.color.black))
                    isWestNorth = false
                    btnWestNorth.setColorFilter(ContextCompat.getColor(context, R.color.black))

                    mListener?.clickAngleInfo(true, 225.0, 1.0)
                } else {
                    isWestSouth = false
                    btnWestSouth.setImageResource(R.drawable.ic_left_down)
                    mListener?.clickAngleInfo(false, 225.0, 0.0)
                }
            } else {
                mListener?.clickAngleInfo(false, 225.0, 1.0)
            }
        }
    }

    fun setListener(listener: ButtonViewClickListener) {
        this.mListener = listener
    }
}
