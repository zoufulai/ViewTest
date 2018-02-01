package com.szshy.viewtest.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.ImageView
import com.szshy.viewtest.R
import android.view.ViewOutlineProvider
import android.os.Build
import android.support.annotation.RequiresApi
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.BitmapShader
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.ColorFilter
import android.support.annotation.DrawableRes
import android.support.annotation.ColorRes
import android.support.annotation.ColorInt
import android.net.Uri
import android.view.View


/**
 * Created by szshy on 2018/1/24.
 */
class CircleImageView:ImageView{


    private val SCALE_TYPE = ImageView.ScaleType.CENTER_CROP

    private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
    private val COLORDRAWABLE_DIMENSION = 2

    private val DEFAULT_BORDER_WIDTH = 0
    private val DEFAULT_BORDER_COLOR = Color.BLACK
    private val DEFAULT_CIRCLE_BACKGROUND_COLOR = Color.TRANSPARENT
    private val DEFAULT_BORDER_OVERLAY = false

    private val mDrawableRect = RectF()
    private val mBorderRect = RectF()

    private val mShaderMatrix = Matrix()
    private val mBitmapPaint = Paint()
    private val mBorderPaint = Paint()
    private val mCircleBackgroundPaint = Paint()

    private var mBorderColor = DEFAULT_BORDER_COLOR
    private var mBorderWidth = DEFAULT_BORDER_WIDTH
    private var mCircleBackgroundColor = DEFAULT_CIRCLE_BACKGROUND_COLOR

    private var mBitmap: Bitmap? = null
    private var mBitmapShader: BitmapShader? = null
    private var mBitmapWidth: Int = 0
    private var mBitmapHeight: Int = 0

    private var mDrawableRadius: Float = 0.toFloat()
    private var mBorderRadius: Float = 0.toFloat()

    private var mColorFilter: ColorFilter? = null

    private var mReady: Boolean = false
    private var mSetupPendings: Boolean = false
    private var mBorderOverlay: Boolean = false
    private var mDisableCircularTransformation: Boolean = false

    constructor(context: Context):super(context){
        init()
    }

    constructor(context: Context,attrs:AttributeSet):super(context,attrs){
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, 0, 0)

        mBorderWidth = a.getDimensionPixelSize(R.styleable.CircleImageView_civ_border_width, DEFAULT_BORDER_WIDTH)
        mBorderColor = a.getColor(R.styleable.CircleImageView_civ_border_color, DEFAULT_BORDER_COLOR)
        mBorderOverlay = a.getBoolean(R.styleable.CircleImageView_civ_border_overlay, DEFAULT_BORDER_OVERLAY)

        // Look for deprecated civ_fill_color if civ_circle_background_color is not set
        if (a.hasValue(R.styleable.CircleImageView_civ_circle_background_color)) {
            mCircleBackgroundColor = a.getColor(R.styleable.CircleImageView_civ_circle_background_color,
                    DEFAULT_CIRCLE_BACKGROUND_COLOR)
        } else if (a.hasValue(R.styleable.CircleImageView_civ_fill_color)) {
            mCircleBackgroundColor = a.getColor(R.styleable.CircleImageView_civ_fill_color,
                    DEFAULT_CIRCLE_BACKGROUND_COLOR)
        }

        a.recycle()

        init()
    }

    constructor(context: Context,attrs: AttributeSet,defStyleAttr:Int):super(context,attrs,defStyleAttr){
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyleAttr, 0)

        mBorderWidth = a.getDimensionPixelSize(R.styleable.CircleImageView_civ_border_width, DEFAULT_BORDER_WIDTH)
        mBorderColor = a.getColor(R.styleable.CircleImageView_civ_border_color, DEFAULT_BORDER_COLOR)
        mBorderOverlay = a.getBoolean(R.styleable.CircleImageView_civ_border_overlay, DEFAULT_BORDER_OVERLAY)

        // Look for deprecated civ_fill_color if civ_circle_background_color is not set
        if (a.hasValue(R.styleable.CircleImageView_civ_circle_background_color)) {
            mCircleBackgroundColor = a.getColor(R.styleable.CircleImageView_civ_circle_background_color,
                    DEFAULT_CIRCLE_BACKGROUND_COLOR)
        } else if (a.hasValue(R.styleable.CircleImageView_civ_fill_color)) {
            mCircleBackgroundColor = a.getColor(R.styleable.CircleImageView_civ_fill_color,
                    DEFAULT_CIRCLE_BACKGROUND_COLOR)
        }

        a.recycle()

        init()
    }


    private fun init(){
        super.setScaleType(SCALE_TYPE)
        mReady = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = OutlineProvider()
        }

        if (mSetupPendings) {
            setup()
            mSetupPendings = false
        }
    }

    override fun getScaleType(): ImageView.ScaleType {
        return SCALE_TYPE
    }

    override fun setScaleType(scaleType: ImageView.ScaleType) {
        if (scaleType != SCALE_TYPE) {
            throw IllegalArgumentException(String.format("ScaleType %s not supported.", scaleType))
        }
    }

    override fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        if (adjustViewBounds) {
            throw IllegalArgumentException("adjustViewBounds not supported.")
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (mDisableCircularTransformation) {
            super.onDraw(canvas)
            return
        }

        if (mBitmap == null) {
            return
        }

        if (mCircleBackgroundColor !== Color.TRANSPARENT) {
            canvas.drawCircle(mDrawableRect.centerX(), mDrawableRect.centerY(), mDrawableRadius, mCircleBackgroundPaint)
        }
        canvas.drawCircle(mDrawableRect.centerX(), mDrawableRect.centerY(), mDrawableRadius, mBitmapPaint)
        if (mBorderWidth > 0) {
            canvas.drawCircle(mBorderRect.centerX(), mBorderRect.centerY(), mBorderRadius, mBorderPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setup()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        setup()
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        setup()
    }

    fun getBorderColor(): Int {
        return mBorderColor
    }

    fun setBorderColor(@ColorInt borderColor: Int) {
        if (borderColor == mBorderColor) {
            return
        }

        mBorderColor = borderColor
        mBorderPaint.color = mBorderColor
        invalidate()
    }


    @Deprecated("Use {@link #setBorderColor(int)} instead")
    fun setBorderColorResource(@ColorRes borderColorRes: Int) {
        setBorderColor(context.resources.getColor(borderColorRes))
    }

    fun getCircleBackgroundColor(): Int {
        return mCircleBackgroundColor
    }

    fun setCircleBackgroundColor(@ColorInt circleBackgroundColor: Int) {
        if (circleBackgroundColor == mCircleBackgroundColor) {
            return
        }

        mCircleBackgroundColor = circleBackgroundColor
        mCircleBackgroundPaint.color = circleBackgroundColor
        invalidate()
    }

    fun setCircleBackgroundColorResource(@ColorRes circleBackgroundRes: Int) {
        setCircleBackgroundColor(context.resources.getColor(circleBackgroundRes))
    }

    /**
     * Return the color drawn behind the circle-shaped drawable.
     *
     * @return The color drawn behind the drawable
     *
     */
    @Deprecated("Use {@link #getCircleBackgroundColor()} instead.")
    fun getFillColor(): Int {
        return getCircleBackgroundColor()
    }

    /**
     * Set a color to be drawn behind the circle-shaped drawable. Note that
     * this has no effect if the drawable is opaque or no drawable is set.
     *
     * @param fillColor The color to be drawn behind the drawable
     *
     */
    @Deprecated("Use {@link #setCircleBackgroundColor(int)} instead.")
    fun setFillColor(@ColorInt fillColor: Int) {
        setCircleBackgroundColor(fillColor)
    }

    /**
     * Set a color to be drawn behind the circle-shaped drawable. Note that
     * this has no effect if the drawable is opaque or no drawable is set.
     *
     * @param fillColorRes The color resource to be resolved to a color and
     * drawn behind the drawable
     *
     */
    @Deprecated("Use {@link #setCircleBackgroundColorResource(int)} instead.")
    fun setFillColorResource(@ColorRes fillColorRes: Int) {
        setCircleBackgroundColorResource(fillColorRes)
    }

    fun getBorderWidth(): Int {
        return mBorderWidth
    }

    fun setBorderWidth(borderWidth: Int) {
        if (borderWidth == mBorderWidth) {
            return
        }

        mBorderWidth = borderWidth
        setup()
    }

    fun isBorderOverlay(): Boolean {
        return mBorderOverlay
    }

    fun setBorderOverlay(borderOverlay: Boolean) {
        if (borderOverlay == mBorderOverlay) {
            return
        }

        mBorderOverlay = borderOverlay
        setup()
    }

    fun isDisableCircularTransformation(): Boolean {
        return mDisableCircularTransformation
    }

    fun setDisableCircularTransformation(disableCircularTransformation: Boolean) {
        if (mDisableCircularTransformation === disableCircularTransformation) {
            return
        }

        mDisableCircularTransformation = disableCircularTransformation
        initializeBitmap()
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        initializeBitmap()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initializeBitmap()
    }

    override fun setImageResource(@DrawableRes resId: Int) {
        super.setImageResource(resId)
        initializeBitmap()
    }

    override fun setImageURI(uri: Uri) {
        super.setImageURI(uri)
        initializeBitmap()
    }

    override fun setColorFilter(cf: ColorFilter) {
        if (cf === mColorFilter) {
            return
        }

        mColorFilter = cf
        applyColorFilter()
        invalidate()
    }

    override fun getColorFilter(): ColorFilter? {
        return mColorFilter
    }

    private fun applyColorFilter() {
        if (mBitmapPaint != null) {
            mBitmapPaint.colorFilter = mColorFilter
        }
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }

        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        try {
            val bitmap: Bitmap

            if (drawable is ColorDrawable) {
                bitmap = Bitmap.createBitmap(COLORDRAWABLE_DIMENSION, COLORDRAWABLE_DIMENSION, BITMAP_CONFIG)
            } else {
                bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, BITMAP_CONFIG)
            }

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    private fun initializeBitmap() {
        if (mDisableCircularTransformation) {
            mBitmap = null
        } else {
            mBitmap = getBitmapFromDrawable(drawable)
        }
        setup()
    }

    private fun setup() {
        if (!mReady) {
            mSetupPendings = true
            return
        }

        if (width == 0 && height == 0) {
            return
        }

        if (mBitmap == null) {
            invalidate()
            return
        }

        mBitmapShader = BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        mBitmapPaint.isAntiAlias = true
        mBitmapPaint.shader = mBitmapShader

        mBorderPaint.style = Paint.Style.STROKE
        mBorderPaint.isAntiAlias = true
        mBorderPaint.color = mBorderColor
        mBorderPaint.setStrokeWidth(mBorderWidth.toFloat())

        mCircleBackgroundPaint.style = Paint.Style.FILL
        mCircleBackgroundPaint.isAntiAlias = true
        mCircleBackgroundPaint.color = mCircleBackgroundColor

        mBitmapHeight = mBitmap!!.height
        mBitmapWidth = mBitmap!!.width

        mBorderRect.set(calculateBounds())
        mBorderRadius = Math.min((mBorderRect.height() - mBorderWidth) / 2.0f, (mBorderRect.width() - mBorderWidth) / 2.0f)

        mDrawableRect.set(mBorderRect)
        if (!mBorderOverlay && mBorderWidth > 0) {
            mDrawableRect.inset(mBorderWidth - 1.0f, mBorderWidth - 1.0f)
        }
        mDrawableRadius = Math.min(mDrawableRect.height() / 2.0f, mDrawableRect.width() / 2.0f)

        applyColorFilter()
        updateShaderMatrix()
        invalidate()
    }

    private fun calculateBounds(): RectF {
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom

        val sideLength = Math.min(availableWidth, availableHeight)

        val left = paddingLeft + (availableWidth - sideLength) / 2f
        val top = paddingTop + (availableHeight - sideLength) / 2f

        return RectF(left, top, left + sideLength, top + sideLength)
    }

    private fun updateShaderMatrix() {
        val scale: Float
        var dx = 0f
        var dy = 0f

        mShaderMatrix.set(null)

        if (mBitmapWidth * mDrawableRect.height() > mDrawableRect.width() * mBitmapHeight) {
            scale = mDrawableRect.height() / mBitmapHeight
            dx = (mDrawableRect.width() - mBitmapWidth * scale) * 0.5f
        } else {
            scale = mDrawableRect.width() / mBitmapWidth
            dy = (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f
        }

        mShaderMatrix.setScale(scale, scale)
        mShaderMatrix.postTranslate((dx + 0.5f).toInt() + mDrawableRect.left, (dy + 0.5f).toInt() + mDrawableRect.top)

        mBitmapShader!!.setLocalMatrix(mShaderMatrix)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private inner class OutlineProvider : ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            val bounds = Rect()
            mBorderRect.roundOut(bounds)
            outline.setRoundRect(bounds, bounds.width() / 2.0f)
        }

    }
}