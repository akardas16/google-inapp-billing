package games.moisoni.google_inapp_billing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class TextOutline @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    AppCompatTextView(
        context!!, attrs
    ) {
    private var mOutlineSize = 0
    private var mOutlineColor = 0
    private var mTextColor = 0
    private var mShadowRadius = 0f
    private var mShadowDx = 0f
    private var mShadowDy = 0f
    private var mShadowColor = 0

    init {
        setAttributes(attrs)
    }

    private fun setAttributes(attrs: AttributeSet?) {
        mOutlineSize = DEFAULT_OUTLINE_SIZE
        mOutlineColor = DEFAULT_OUTLINE_COLOR
        mTextColor = currentTextColor
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TextOutline)
            if (a.hasValue(R.styleable.TextOutline_outlineSize)) {
                mOutlineSize = a.getDimension(
                    R.styleable.TextOutline_outlineSize,
                    DEFAULT_OUTLINE_SIZE.toFloat()
                ).toInt()
            }
            if (a.hasValue(R.styleable.TextOutline_outlineColor)) {
                mOutlineColor =
                    a.getColor(R.styleable.TextOutline_outlineColor, DEFAULT_OUTLINE_COLOR)
            }
            if (a.hasValue(R.styleable.TextOutline_android_shadowRadius)
                || a.hasValue(R.styleable.TextOutline_android_shadowDx)
                || a.hasValue(R.styleable.TextOutline_android_shadowDy)
                || a.hasValue(R.styleable.TextOutline_android_shadowColor)
            ) {
                mShadowRadius = a.getFloat(R.styleable.TextOutline_android_shadowRadius, 0f)
                mShadowDx = a.getFloat(R.styleable.TextOutline_android_shadowDx, 0f)
                mShadowDy = a.getFloat(R.styleable.TextOutline_android_shadowDy, 0f)
                mShadowColor =
                    a.getColor(R.styleable.TextOutline_android_shadowColor, Color.TRANSPARENT)
            }
            a.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setPaintToOutline()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun setPaintToOutline() {
        val paint: Paint = paint
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = mOutlineSize.toFloat()
        super.setTextColor(mOutlineColor)
        super.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }

    private fun setPaintToRegular() {
        val paint: Paint = paint
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        super.setTextColor(mTextColor)
        super.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, mShadowColor)
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        mTextColor = color
    }

    override fun onDraw(canvas: Canvas) {
        setPaintToOutline()
        super.onDraw(canvas)
        setPaintToRegular()
        super.onDraw(canvas)
    }

    companion object {
        private const val DEFAULT_OUTLINE_SIZE = 0
        private const val DEFAULT_OUTLINE_COLOR = Color.TRANSPARENT
    }
}