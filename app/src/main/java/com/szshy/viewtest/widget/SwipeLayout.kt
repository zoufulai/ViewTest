package com.szshy.viewtest.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout
import android.support.v4.widget.ViewDragHelper
import android.view.View
import android.view.ViewConfiguration
import com.szshy.viewtest.R
import android.view.MotionEvent
import android.view.Gravity
import android.support.v4.view.ViewCompat
import android.support.v4.view.GravityCompat
import android.view.ViewGroup
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.widget.AbsListView
import android.widget.AdapterView


/**
 * Created by szshy on 2018/1/25.
 */
class SwipeLayout: FrameLayout {

    @Deprecated("")
    val EMPTY_LAYOUT = -1
    private val DRAG_LEFT = 1
    private val DRAG_RIGHT = 2
    private val DRAG_TOP = 4
    private val DRAG_BOTTOM = 8
    private val DefaultDragEdge = DragEdge.Right

    private var mTouchSlop: Int = 0

    private var mCurrentDragEdge = DefaultDragEdge
    private var mDragHelper: ViewDragHelper? = null

    private var mDragDistance = 0
    private val mDragEdges = LinkedHashMap<DragEdge, View?>()
    private var mShowMode: ShowMode? = null

    private val mEdgeSwipesOffset = FloatArray(4)

    private val mSwipeListeners = ArrayList<SwipeListener>()
    private val mSwipeDeniers = ArrayList<SwipeDenier>()
    private val mRevealListeners = HashMap<View, ArrayList<OnRevealListener>>()
    private val mShowEntirely = HashMap< View,  Boolean>()
    private val mViewBoundCache = HashMap<View?, Rect>()//save all children's bound, restore in onLayout

    private var mDoubleClickListener: DoubleClickListener? = null

    private var mSwipeEnabled = true
    private val mSwipesEnabled = booleanArrayOf(true, true, true, true)
    private var mClickToClose = false
    private var mWillOpenPercentAfterOpen = 0.75f
    private var mWillOpenPercentAfterClose = 0.25f

    enum class DragEdge {
        Left,
        Top,
        Right,
        Bottom
    }

    enum class ShowMode {
        LayDown,
        PullOut
    }

    constructor(context: Context):super(context){

    }

    constructor(context: Context,attrs:AttributeSet):super(context,attrs){
        mDragHelper = ViewDragHelper.create(this, mDragHelperCallback)
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        val a = context.obtainStyledAttributes(attrs, R.styleable.SwipeLayout)
        val dragEdgeChoices = a.getInt(R.styleable.SwipeLayout_drag_edge, DRAG_RIGHT)
        mEdgeSwipesOffset[DragEdge.Left.ordinal] = a.getDimension(R.styleable.SwipeLayout_leftEdgeSwipeOffset, 0f)
        mEdgeSwipesOffset[DragEdge.Right.ordinal] = a.getDimension(R.styleable.SwipeLayout_rightEdgeSwipeOffset, 0f)
        mEdgeSwipesOffset[DragEdge.Top.ordinal] = a.getDimension(R.styleable.SwipeLayout_topEdgeSwipeOffset, 0f)
        mEdgeSwipesOffset[DragEdge.Bottom.ordinal] = a.getDimension(R.styleable.SwipeLayout_bottomEdgeSwipeOffset, 0f)
        setClickToClose(a.getBoolean(R.styleable.SwipeLayout_clickToClose, mClickToClose))

        if (dragEdgeChoices and DRAG_LEFT === DRAG_LEFT) {
            mDragEdges[DragEdge.Left] = null
        }
        if (dragEdgeChoices and DRAG_TOP === DRAG_TOP) {
            mDragEdges[DragEdge.Top] = null
        }
        if (dragEdgeChoices and DRAG_RIGHT === DRAG_RIGHT) {
            mDragEdges[DragEdge.Right] = null
        }
        if (dragEdgeChoices and DRAG_BOTTOM === DRAG_BOTTOM) {
            mDragEdges[DragEdge.Bottom] = null
        }
        val ordinal = a.getInt(R.styleable.SwipeLayout_show_mode, ShowMode.PullOut.ordinal)
        mShowMode = ShowMode.values()[ordinal]
        a.recycle()
    }

    constructor(context: Context,attrs:AttributeSet,defset:Int):super(context,attrs,defset){
        mDragHelper = ViewDragHelper.create(this, mDragHelperCallback)
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        val a = context.obtainStyledAttributes(attrs, R.styleable.SwipeLayout)
        val dragEdgeChoices = a.getInt(R.styleable.SwipeLayout_drag_edge, DRAG_RIGHT)
        mEdgeSwipesOffset[DragEdge.Left.ordinal] = a.getDimension(R.styleable.SwipeLayout_leftEdgeSwipeOffset, 0f)
        mEdgeSwipesOffset[DragEdge.Right.ordinal] = a.getDimension(R.styleable.SwipeLayout_rightEdgeSwipeOffset, 0f)
        mEdgeSwipesOffset[DragEdge.Top.ordinal] = a.getDimension(R.styleable.SwipeLayout_topEdgeSwipeOffset, 0f)
        mEdgeSwipesOffset[DragEdge.Bottom.ordinal] = a.getDimension(R.styleable.SwipeLayout_bottomEdgeSwipeOffset, 0f)
        setClickToClose(a.getBoolean(R.styleable.SwipeLayout_clickToClose, mClickToClose))

        if (dragEdgeChoices and DRAG_LEFT === DRAG_LEFT) {
            mDragEdges[DragEdge.Left] = null
        }
        if (dragEdgeChoices and DRAG_TOP === DRAG_TOP) {
            mDragEdges[DragEdge.Top] = null
        }
        if (dragEdgeChoices and DRAG_RIGHT === DRAG_RIGHT) {
            mDragEdges[DragEdge.Right] = null
        }
        if (dragEdgeChoices and DRAG_BOTTOM === DRAG_BOTTOM) {
            mDragEdges[DragEdge.Bottom] = null
        }
        val ordinal = a.getInt(R.styleable.SwipeLayout_show_mode, ShowMode.PullOut.ordinal)
        mShowMode = ShowMode.values()[ordinal]
        a.recycle()
    }


    interface SwipeListener {
        fun onStartOpen(layout: SwipeLayout)

        fun onOpen(layout: SwipeLayout)

        fun onStartClose(layout: SwipeLayout)

        fun onClose(layout: SwipeLayout)

        fun onUpdate(layout: SwipeLayout, leftOffset: Int, topOffset: Int)

        fun onHandRelease(layout: SwipeLayout, xvel: Float, yvel: Float)
    }

    fun addSwipeListener(l: SwipeListener) {
        mSwipeListeners.add(l)
    }

    fun removeSwipeListener(l: SwipeListener) {
        mSwipeListeners.remove(l)
    }

    fun removeAllSwipeListener() {
        mSwipeListeners.clear()
    }

    interface SwipeDenier {
        /*
         * Called in onInterceptTouchEvent Determines if this swipe event should
         * be denied Implement this interface if you are using views with swipe
         * gestures As a child of SwipeLayout
         *
         * @return true deny false allow
         */
        fun shouldDenySwipe(ev: MotionEvent): Boolean
    }

    fun addSwipeDenier(denier: SwipeDenier) {
        mSwipeDeniers.add(denier)
    }

    fun removeSwipeDenier(denier: SwipeDenier) {
        mSwipeDeniers.remove(denier)
    }

    fun removeAllSwipeDeniers() {
        mSwipeDeniers.clear()
    }

    interface OnRevealListener {
        fun onReveal(child: View, edge: DragEdge, fraction: Float, distance: Int)
    }

    /**
     * bind a view with a specific
     * [com.daimajia.swipe.SwipeLayout.OnRevealListener]
     *
     * @param childId the view id.
     * @param l       the target
     * [com.daimajia.swipe.SwipeLayout.OnRevealListener]
     */
    fun addRevealListener(childId: Int, l: OnRevealListener) {
        val child = findViewById<View>(childId)
                ?: throw IllegalArgumentException("Child does not belong to SwipeListener.")

        if (!mShowEntirely.containsKey(child)) {
            mShowEntirely[child] = false
        }
        if (mRevealListeners[child] == null)
            mRevealListeners[child] = ArrayList()

        mRevealListeners[child]!!.add(l)
    }

    /**
     * bind multiple views with an
     * [com.daimajia.swipe.SwipeLayout.OnRevealListener].
     *
     * @param childIds the view id.
     * @param l        the [com.daimajia.swipe.SwipeLayout.OnRevealListener]
     */
    fun addRevealListener(childIds: IntArray, l: OnRevealListener) {
        for (i in childIds)
            addRevealListener(i, l)
    }

    fun removeRevealListener(childId: Int, l: OnRevealListener) {
        val child = findViewById<View>(childId) ?: return

        mShowEntirely.remove(child)
        if (mRevealListeners.containsKey(child)) mRevealListeners[child]!!.remove(l)
    }

    fun removeAllRevealListeners(childId: Int) {
        val child = findViewById<View>(childId)
        if (child != null) {
            mRevealListeners.remove(child)
            mShowEntirely.remove(child)
        }
    }


    private val mDragHelperCallback = object : ViewDragHelper.Callback() {

        internal var isCloseBeforeDrag = true

        override fun clampViewPositionHorizontal(child: View?, left: Int, dx: Int): Int {
            if (child === getSurfaceView()) {
                when (mCurrentDragEdge) {
                    DragEdge.Top, DragEdge.Bottom -> return paddingLeft
                    DragEdge.Left -> {
                        if (left < paddingLeft) return paddingLeft
                        if (left > paddingLeft + mDragDistance)
                            return paddingLeft + mDragDistance
                    }
                    DragEdge.Right -> {
                        if (left > paddingLeft) return paddingLeft
                        if (left < paddingLeft - mDragDistance)
                            return paddingLeft - mDragDistance
                    }
                }
            } else if (getCurrentBottomView() === child) {

                when (mCurrentDragEdge) {
                    DragEdge.Top, DragEdge.Bottom -> return paddingLeft
                    DragEdge.Left -> if (mShowMode === ShowMode.PullOut) {
                        if (left > paddingLeft) return paddingLeft
                    }
                    DragEdge.Right -> if (mShowMode === ShowMode.PullOut) {
                        if (left < measuredWidth - mDragDistance) {
                            return measuredWidth - mDragDistance
                        }
                    }
                }
            }
            return left
        }

        override fun clampViewPositionVertical(child: View?, top: Int, dy: Int): Int {
            if (child === getSurfaceView()) {
                when (mCurrentDragEdge) {
                    DragEdge.Left, DragEdge.Right -> return paddingTop
                    DragEdge.Top -> {
                        if (top < paddingTop) return paddingTop
                        if (top > paddingTop + mDragDistance)
                            return paddingTop + mDragDistance
                    }
                    DragEdge.Bottom -> {
                        if (top < paddingTop - mDragDistance) {
                            return paddingTop - mDragDistance
                        }
                        if (top > paddingTop) {
                            return paddingTop
                        }
                    }
                }
            } else {
                val surfaceView = getSurfaceView()
                val surfaceViewTop = if (surfaceView == null) 0 else surfaceView!!.getTop()
                when (mCurrentDragEdge) {
                    DragEdge.Left, DragEdge.Right -> return paddingTop
                    DragEdge.Top -> if (mShowMode === ShowMode.PullOut) {
                        if (top > paddingTop) return paddingTop
                    } else {
                        if (surfaceViewTop + dy < paddingTop)
                            return paddingTop
                        if (surfaceViewTop + dy > paddingTop + mDragDistance)
                            return paddingTop + mDragDistance
                    }
                    DragEdge.Bottom -> if (mShowMode === ShowMode.PullOut) {
                        if (top < measuredHeight - mDragDistance)
                            return measuredHeight - mDragDistance
                    } else {
                        if (surfaceViewTop + dy >= paddingTop)
                            return paddingTop
                        if (surfaceViewTop + dy <= paddingTop - mDragDistance)
                            return paddingTop - mDragDistance
                    }
                }
            }
            return top
        }

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            val result = child === getSurfaceView() || getBottomViews().contains(child)
            if (result) {
                isCloseBeforeDrag = getOpenStatus() === Status.Close
            }
            return result
        }

        override fun getViewHorizontalDragRange(child: View?): Int {
            return mDragDistance
        }

        override fun getViewVerticalDragRange(child: View?): Int {
            return mDragDistance
        }

        override fun onViewReleased(releasedChild: View?, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            processHandRelease(xvel, yvel, isCloseBeforeDrag)
            for (l in mSwipeListeners) {
                l.onHandRelease(this@SwipeLayout, xvel, yvel)
            }

            invalidate()
        }

        override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
            val surfaceView = getSurfaceView() ?: return
            val currentBottomView = getCurrentBottomView()
            val evLeft = surfaceView!!.getLeft()
            val evRight = surfaceView!!.getRight()
            val evTop = surfaceView!!.getTop()
            val evBottom = surfaceView!!.getBottom()
            if (changedView === surfaceView) {

                if (mShowMode === ShowMode.PullOut && currentBottomView != null) {
                    if (mCurrentDragEdge === DragEdge.Left || mCurrentDragEdge === DragEdge.Right) {
                        currentBottomView!!.offsetLeftAndRight(dx)
                    } else {
                        currentBottomView!!.offsetTopAndBottom(dy)
                    }
                }

            } else if (getBottomViews().contains(changedView)) {

                if (mShowMode === ShowMode.PullOut) {
                    surfaceView!!.offsetLeftAndRight(dx)
                    surfaceView!!.offsetTopAndBottom(dy)
                } else {
                    val rect = computeBottomLayDown(mCurrentDragEdge)
                    if (currentBottomView != null) {
                        currentBottomView!!.layout(rect.left, rect.top, rect.right, rect.bottom)
                    }

                    var newLeft = surfaceView!!.getLeft() + dx
                    var newTop = surfaceView!!.getTop() + dy

                    if (mCurrentDragEdge === DragEdge.Left && newLeft < paddingLeft)
                        newLeft = paddingLeft
                    else if (mCurrentDragEdge === DragEdge.Right && newLeft > paddingLeft)
                        newLeft = paddingLeft
                    else if (mCurrentDragEdge === DragEdge.Top && newTop < paddingTop)
                        newTop = paddingTop
                    else if (mCurrentDragEdge === DragEdge.Bottom && newTop > paddingTop)
                        newTop = paddingTop

                    surfaceView!!.layout(newLeft, newTop, newLeft + measuredWidth, newTop + measuredHeight)
                }
            }

            dispatchRevealEvent(evLeft, evTop, evRight, evBottom)

            dispatchSwipeEvent(evLeft, evTop, dx, dy)

            invalidate()

            captureChildrenBound()
        }
    }

    /**
     * save children's bounds, so they can restore the bound in [.onLayout]
     */
    private fun captureChildrenBound() {
        val currentBottomView = getCurrentBottomView()
        if (getOpenStatus() === Status.Close) {
            mViewBoundCache.remove(currentBottomView)
            return
        }

        val views = arrayOf<View?>(getSurfaceView(), currentBottomView)
        for (child in views) {
            var rect = mViewBoundCache[child]
            if (rect == null) {
                rect = Rect()
                mViewBoundCache[child] = rect
            }
            rect.left = child!!.left
            rect.top = child.top
            rect.right = child.right
            rect.bottom = child.bottom
        }
    }

    /**
     * the dispatchRevealEvent method may not always get accurate position, it
     * makes the view may not always get the event when the view is totally
     * show( fraction = 1), so , we need to calculate every time.
     */
    protected fun isViewTotallyFirstShowed(child: View, relativePosition: Rect, edge: DragEdge, surfaceLeft: Int,
                                           surfaceTop: Int, surfaceRight: Int, surfaceBottom: Int): Boolean {
        if (mShowEntirely[child]!!) return false
        val childLeft = relativePosition.left
        val childRight = relativePosition.right
        val childTop = relativePosition.top
        val childBottom = relativePosition.bottom
        var r = false
        if (getShowMode() === ShowMode.LayDown) {
            if (edge === DragEdge.Right && surfaceRight <= childLeft
                    || edge === DragEdge.Left && surfaceLeft >= childRight
                    || edge === DragEdge.Top && surfaceTop >= childBottom
                    || edge === DragEdge.Bottom && surfaceBottom <= childTop)
                r = true
        } else if (getShowMode() === ShowMode.PullOut) {
            if (edge === DragEdge.Right && childRight <= width
                    || edge === DragEdge.Left && childLeft >= paddingLeft
                    || edge === DragEdge.Top && childTop >= paddingTop
                    || edge === DragEdge.Bottom && childBottom <= height)
                r = true
        }
        return r
    }

    protected fun isViewShowing(child: View, relativePosition: Rect, availableEdge: DragEdge, surfaceLeft: Int,
                                surfaceTop: Int, surfaceRight: Int, surfaceBottom: Int): Boolean {
        val childLeft = relativePosition.left
        val childRight = relativePosition.right
        val childTop = relativePosition.top
        val childBottom = relativePosition.bottom
        if (getShowMode() === ShowMode.LayDown) {
            when (availableEdge) {
                SwipeLayout.DragEdge.Right -> if (surfaceRight > childLeft && surfaceRight <= childRight) {
                    return true
                }
                SwipeLayout.DragEdge.Left -> if (surfaceLeft < childRight && surfaceLeft >= childLeft) {
                    return true
                }
                SwipeLayout.DragEdge.Top -> if (surfaceTop >= childTop && surfaceTop < childBottom) {
                    return true
                }
                SwipeLayout.DragEdge.Bottom -> if (surfaceBottom > childTop && surfaceBottom <= childBottom) {
                    return true
                }
            }
        } else if (getShowMode() === ShowMode.PullOut) {
            when (availableEdge) {
                SwipeLayout.DragEdge.Right -> if (childLeft <= width && childRight > width) return true
                SwipeLayout.DragEdge.Left -> if (childRight >= paddingLeft && childLeft < paddingLeft) return true
                SwipeLayout.DragEdge.Top -> if (childTop < paddingTop && childBottom >= paddingTop) return true
                SwipeLayout.DragEdge.Bottom -> if (childTop < height && childTop >= paddingTop) return true
            }
        }
        return false
    }

    protected fun getRelativePosition(child: View): Rect {
        var t = child
        val r = Rect(t.left, t.top, 0, 0)
        while (t.parent != null && t !== rootView) {
            t = t.parent as View
            if (t === this) break
            r.left += t.left
            r.top += t.top
        }
        r.right = r.left + child.measuredWidth
        r.bottom = r.top + child.measuredHeight
        return r
    }

    private var mEventCounter = 0

    protected fun dispatchSwipeEvent(surfaceLeft: Int, surfaceTop: Int, dx: Int, dy: Int) {
        val edge = getDragEdge()
        var open = true
        if (edge === DragEdge.Left) {
            if (dx < 0) open = false
        } else if (edge === DragEdge.Right) {
            if (dx > 0) open = false
        } else if (edge === DragEdge.Top) {
            if (dy < 0) open = false
        } else if (edge === DragEdge.Bottom) {
            if (dy > 0) open = false
        }

        dispatchSwipeEvent(surfaceLeft, surfaceTop, open)
    }

    protected fun dispatchSwipeEvent(surfaceLeft: Int, surfaceTop: Int, open: Boolean) {
        safeBottomView()
        val status = getOpenStatus()

        if (!mSwipeListeners.isEmpty()) {
            mEventCounter++
            for (l in mSwipeListeners) {
                if (mEventCounter == 1) {
                    if (open) {
                        l.onStartOpen(this)
                    } else {
                        l.onStartClose(this)
                    }
                }
                l.onUpdate(this@SwipeLayout, surfaceLeft - paddingLeft, surfaceTop - paddingTop)
            }

            if (status === Status.Close) {
                for (l in mSwipeListeners) {
                    l.onClose(this@SwipeLayout)
                }
                mEventCounter = 0
            }

            if (status === Status.Open) {
                val currentBottomView = getCurrentBottomView()
                if (currentBottomView != null) {
                    currentBottomView!!.setEnabled(true)
                }
                for (l in mSwipeListeners) {
                    l.onOpen(this@SwipeLayout)
                }
                mEventCounter = 0
            }
        }
    }


    /**
     * prevent bottom view get any touch event. Especially in LayDown mode.
     */
    private fun safeBottomView() {
        val status = getOpenStatus()
        val bottoms = getBottomViews()

        if (status === Status.Close) {
            for (bottom in bottoms) {
                if (bottom != null && bottom!!.getVisibility() != View.INVISIBLE) {
                    bottom!!.setVisibility(View.INVISIBLE)
                }
            }
        } else {
            val currentBottomView = getCurrentBottomView()
            if (currentBottomView != null && currentBottomView!!.getVisibility() != View.VISIBLE) {
                currentBottomView!!.setVisibility(View.VISIBLE)
            }
        }
    }

    protected fun dispatchRevealEvent(surfaceLeft: Int, surfaceTop: Int, surfaceRight: Int,
                                      surfaceBottom: Int) {
        if (mRevealListeners.isEmpty()) return
        for (entry in mRevealListeners.entries) {
            val child = entry.key
            val rect = getRelativePosition(child)
            if (isViewShowing(child, rect, mCurrentDragEdge, surfaceLeft, surfaceTop,
                            surfaceRight, surfaceBottom)) {
                mShowEntirely[child] = false
                var distance = 0
                var fraction = 0f
                if (getShowMode() === ShowMode.LayDown) {
                    when (mCurrentDragEdge) {
                        DragEdge.Left -> {
                            distance = rect.left - surfaceLeft
                            fraction = distance / child.getWidth().toFloat()
                        }
                        DragEdge.Right -> {
                            distance = rect.right - surfaceRight
                            fraction = distance / child.getWidth().toFloat()
                        }
                        DragEdge.Top -> {
                            distance = rect.top - surfaceTop
                            fraction = distance / child.getHeight().toFloat()
                        }
                        DragEdge.Bottom -> {
                            distance = rect.bottom - surfaceBottom
                            fraction = distance / child.getHeight().toFloat()
                        }
                    }
                } else if (getShowMode() === ShowMode.PullOut) {
                    when (mCurrentDragEdge) {
                        DragEdge.Left -> {
                            distance = rect.right - paddingLeft
                            fraction = distance / child.getWidth().toFloat()
                        }
                        DragEdge.Right -> {
                            distance = rect.left - width
                            fraction = distance / child.getWidth().toFloat()
                        }
                        DragEdge.Top -> {
                            distance = rect.bottom - paddingTop
                            fraction = distance / child.getHeight().toFloat()
                        }
                        DragEdge.Bottom -> {
                            distance = rect.top - height
                            fraction = distance / child.getHeight().toFloat()
                        }
                    }
                }

                for (l in entry.value) {
                    l.onReveal(child, mCurrentDragEdge, Math.abs(fraction), distance)
                    if (Math.abs(fraction) == 1f) {
                        mShowEntirely[child] = true
                    }
                }
            }

            if (isViewTotallyFirstShowed(child, rect, mCurrentDragEdge, surfaceLeft, surfaceTop,
                            surfaceRight, surfaceBottom)) {
                mShowEntirely[child] = true
                for (l in entry.value) {
                    if (mCurrentDragEdge === DragEdge.Left || mCurrentDragEdge === DragEdge.Right)
                        l.onReveal(child, mCurrentDragEdge, 1f, child.getWidth())
                    else
                        l.onReveal(child, mCurrentDragEdge, 1f, child.getHeight())
                }
            }

        }
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mDragHelper!!.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    /**
     * [android.view.View.OnLayoutChangeListener] added in API 11. I need
     * to support it from API 8.
     */
    interface OnLayout {
        fun onLayout(v: SwipeLayout)
    }

    private var mOnLayoutListeners: MutableList<OnLayout>? = null

    fun addOnLayoutListener(l: OnLayout) {
        if (mOnLayoutListeners == null) mOnLayoutListeners = ArrayList()
        mOnLayoutListeners!!.add(l)
    }

    fun removeOnLayoutListener(l: OnLayout) {
        if (mOnLayoutListeners != null) mOnLayoutListeners!!.remove(l)
    }

    fun clearDragEdge() {
        mDragEdges.clear()
    }

    fun setDrag(dragEdge: DragEdge, childId: Int) {
        clearDragEdge()
        addDrag(dragEdge, childId)
    }

    fun setDrag(dragEdge: DragEdge, child: View) {
        clearDragEdge()
        addDrag(dragEdge, child)
    }

    fun addDrag(dragEdge: DragEdge, childId: Int) {
        addDrag(dragEdge, findViewById(childId), null)
    }

    fun addDrag(dragEdge: DragEdge, child: View) {
        addDrag(dragEdge, child, null)
    }

    fun addDrag(dragEdge: DragEdge, child: View?, params: ViewGroup.LayoutParams?) {
        var params = params
        if (child == null) return

        if (params == null) {
            params = generateDefaultLayoutParams()
        }
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params)
        }
        var gravity = -1
        when (dragEdge) {
            SwipeLayout.DragEdge.Left -> gravity = Gravity.LEFT
            SwipeLayout.DragEdge.Right -> gravity = Gravity.RIGHT
            SwipeLayout.DragEdge.Top -> gravity = Gravity.TOP
            SwipeLayout.DragEdge.Bottom -> gravity = Gravity.BOTTOM
        }
        if (params is FrameLayout.LayoutParams) {
            params.gravity = gravity
        }
        addView(child, 0, params!!)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams) {
        if (child == null) return
        var gravity = Gravity.NO_GRAVITY
        try {
            gravity = params.javaClass.getField("gravity").get(params) as Int
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (gravity > 0) {
            gravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this))

            if (gravity and Gravity.LEFT == Gravity.LEFT) {
                mDragEdges[DragEdge.Left] = child
            }
            if (gravity and Gravity.RIGHT == Gravity.RIGHT) {
                mDragEdges[DragEdge.Right] = child
            }
            if (gravity and Gravity.TOP == Gravity.TOP) {
                mDragEdges[DragEdge.Top] = child
            }
            if (gravity and Gravity.BOTTOM == Gravity.BOTTOM) {
                mDragEdges[DragEdge.Bottom] = child
            }
        } else {
            for (entry in mDragEdges.entries) {
                if (entry.value == null) {
                    //means used the drag_edge attr, the no gravity child should be use set
                    mDragEdges[entry.key] = child
                    break
                }
            }
        }
        if (child.parent === this) {
            return
        }
        super.addView(child, index, params)
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        updateBottomViews()

        if (mOnLayoutListeners != null)
            for (i in 0 until mOnLayoutListeners!!.size) {
                mOnLayoutListeners!!.get(i).onLayout(this)
            }
    }

    fun layoutPullOut() {
        val surfaceView = getSurfaceView()
        var surfaceRect = mViewBoundCache[surfaceView]
        if (surfaceRect == null) surfaceRect = computeSurfaceLayoutArea(false)
        if (surfaceView != null) {
            surfaceView.layout(surfaceRect.left, surfaceRect.top, surfaceRect.right, surfaceRect.bottom)
            bringChildToFront(surfaceView)
        }
        val currentBottomView = getCurrentBottomView()
        var bottomViewRect = mViewBoundCache[currentBottomView]
        if (bottomViewRect == null)
            bottomViewRect = computeBottomLayoutAreaViaSurface(ShowMode.PullOut, surfaceRect)
        currentBottomView?.layout(bottomViewRect.left, bottomViewRect.top, bottomViewRect.right, bottomViewRect.bottom)
    }

    fun layoutLayDown() {
        val surfaceView = getSurfaceView()
        var surfaceRect = mViewBoundCache[surfaceView]
        if (surfaceRect == null) surfaceRect = computeSurfaceLayoutArea(false)
        if (surfaceView != null) {
            surfaceView.layout(surfaceRect.left, surfaceRect.top, surfaceRect.right, surfaceRect.bottom)
            bringChildToFront(surfaceView)
        }
        val currentBottomView = getCurrentBottomView()
        var bottomViewRect = mViewBoundCache[currentBottomView]
        if (bottomViewRect == null)
            bottomViewRect = computeBottomLayoutAreaViaSurface(ShowMode.LayDown, surfaceRect)
        currentBottomView?.layout(bottomViewRect.left, bottomViewRect.top, bottomViewRect.right, bottomViewRect.bottom)
    }

    private var mIsBeingDragged: Boolean = false

    private fun checkCanDrag(ev: MotionEvent) {
        if (mIsBeingDragged) return
        if (getOpenStatus() == Status.Middle) {
            mIsBeingDragged = true
            return
        }
        val status = getOpenStatus()
        val distanceX = ev.rawX - sX
        val distanceY = ev.rawY - sY
        var angle = Math.abs(distanceY / distanceX)
        angle = Math.toDegrees(Math.atan(angle.toDouble())).toFloat()
        if (getOpenStatus() == Status.Close) {
            val dragEdge: DragEdge
            if (angle < 45) {
                if (distanceX > 0 && isLeftSwipeEnabled()) {
                    dragEdge = DragEdge.Left
                } else if (distanceX < 0 && isRightSwipeEnabled()) {
                    dragEdge = DragEdge.Right
                } else
                    return

            } else {
                if (distanceY > 0 && isTopSwipeEnabled()) {
                    dragEdge = DragEdge.Top
                } else if (distanceY < 0 && isBottomSwipeEnabled()) {
                    dragEdge = DragEdge.Bottom
                } else
                    return
            }
            setCurrentDragEdge(dragEdge)
        }

        var doNothing = false
        if (mCurrentDragEdge === DragEdge.Right) {
            var suitable = status == Status.Open && distanceX > mTouchSlop || status == Status.Close && distanceX < -mTouchSlop
            suitable = suitable || status == Status.Middle

            if (angle > 30 || !suitable) {
                doNothing = true
            }
        }

        if (mCurrentDragEdge === DragEdge.Left) {
            var suitable = status == Status.Open && distanceX < -mTouchSlop || status == Status.Close && distanceX > mTouchSlop
            suitable = suitable || status == Status.Middle

            if (angle > 30 || !suitable) {
                doNothing = true
            }
        }

        if (mCurrentDragEdge === DragEdge.Top) {
            var suitable = status == Status.Open && distanceY < -mTouchSlop || status == Status.Close && distanceY > mTouchSlop
            suitable = suitable || status == Status.Middle

            if (angle < 60 || !suitable) {
                doNothing = true
            }
        }

        if (mCurrentDragEdge === DragEdge.Bottom) {
            var suitable = status == Status.Open && distanceY > mTouchSlop || status == Status.Close && distanceY < -mTouchSlop
            suitable = suitable || status == Status.Middle

            if (angle < 60 || !suitable) {
                doNothing = true
            }
        }
        mIsBeingDragged = !doNothing
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isSwipeEnabled()) {
            return false
        }
        if (mClickToClose && getOpenStatus() == Status.Open && isTouchOnSurface(ev)) {
            return true
        }
        for (denier in mSwipeDeniers) {
            if (denier != null && denier.shouldDenySwipe(ev)) {
                return false
            }
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mDragHelper!!.processTouchEvent(ev)
                mIsBeingDragged = false
                sX = ev.rawX
                sY = ev.rawY
                //if the swipe is in middle state(scrolling), should intercept the touch
                if (getOpenStatus() == Status.Middle) {
                    mIsBeingDragged = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val beforeCheck = mIsBeingDragged
                checkCanDrag(ev)
                if (mIsBeingDragged) {
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (!beforeCheck && mIsBeingDragged) {
                    //let children has one chance to catch the touch, and request the swipe not intercept
                    //useful when swipeLayout wrap a swipeLayout or other gestural layout
                    return false
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mIsBeingDragged = false
                mDragHelper!!.processTouchEvent(ev)
            }
            else//handle other action, such as ACTION_POINTER_DOWN/UP
            -> mDragHelper!!.processTouchEvent(ev)
        }
        return mIsBeingDragged
    }

    private var sX = -1f
    private var sY = -1f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isSwipeEnabled()) return super.onTouchEvent(event)

        val action = event.actionMasked
        gestureDetector.onTouchEvent(event)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mDragHelper!!.processTouchEvent(event)
                sX = event.rawX
                sY = event.rawY
                run {
                    //the drag state and the direction are already judged at onInterceptTouchEvent
                    checkCanDrag(event)
                    if (mIsBeingDragged) {
                        parent.requestDisallowInterceptTouchEvent(true)
                        mDragHelper!!.processTouchEvent(event)
                    }
                }
            }


            MotionEvent.ACTION_MOVE -> {
                checkCanDrag(event)
                if (mIsBeingDragged) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    mDragHelper!!.processTouchEvent(event)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mDragHelper!!.processTouchEvent(event)
            }

            else//handle other action, such as ACTION_POINTER_DOWN/UP
            -> mDragHelper!!.processTouchEvent(event)
        }

        return super.onTouchEvent(event) || mIsBeingDragged || action == MotionEvent.ACTION_DOWN
    }

    fun isClickToClose(): Boolean {
        return mClickToClose
    }

    fun setClickToClose(mClickToClose: Boolean) {
        this.mClickToClose = mClickToClose
    }

    fun setSwipeEnabled(enabled: Boolean) {
        mSwipeEnabled = enabled
    }

    fun isSwipeEnabled(): Boolean {
        return mSwipeEnabled
    }

    fun isLeftSwipeEnabled(): Boolean {
        val bottomView = mDragEdges[DragEdge.Left]
        return (bottomView != null && bottomView.parent === this
                && bottomView !== getSurfaceView() && mSwipesEnabled[DragEdge.Left.ordinal])
    }

    fun setLeftSwipeEnabled(leftSwipeEnabled: Boolean) {
        this.mSwipesEnabled[DragEdge.Left.ordinal] = leftSwipeEnabled
    }

    fun isRightSwipeEnabled(): Boolean {
        val bottomView = mDragEdges[DragEdge.Right]
        return (bottomView != null && bottomView.parent === this
                && bottomView !== getSurfaceView() && mSwipesEnabled[DragEdge.Right.ordinal])
    }

    fun setRightSwipeEnabled(rightSwipeEnabled: Boolean) {
        this.mSwipesEnabled[DragEdge.Right.ordinal] = rightSwipeEnabled
    }

    fun isTopSwipeEnabled(): Boolean {
        val bottomView = mDragEdges[DragEdge.Top]
        return (bottomView != null && bottomView.parent === this
                && bottomView !== getSurfaceView() && mSwipesEnabled[DragEdge.Top.ordinal])
    }

    fun setTopSwipeEnabled(topSwipeEnabled: Boolean) {
        this.mSwipesEnabled[DragEdge.Top.ordinal] = topSwipeEnabled
    }

    fun isBottomSwipeEnabled(): Boolean {
        val bottomView = mDragEdges[DragEdge.Bottom]
        return (bottomView != null && bottomView.parent === this
                && bottomView !== getSurfaceView() && mSwipesEnabled[DragEdge.Bottom.ordinal])
    }

    fun setBottomSwipeEnabled(bottomSwipeEnabled: Boolean) {
        this.mSwipesEnabled[DragEdge.Bottom.ordinal] = bottomSwipeEnabled
    }

    /***
     * Returns the percentage of revealing at which the view below should the view finish opening
     * if it was already open before dragging
     *
     * @returns The percentage of view revealed to trigger, default value is 0.25
     */
    fun getWillOpenPercentAfterOpen(): Float {
        return mWillOpenPercentAfterOpen
    }

    /***
     * Allows to stablish at what percentage of revealing the view below should the view finish opening
     * if it was already open before dragging
     *
     * @param willOpenPercentAfterOpen The percentage of view revealed to trigger, default value is 0.25
     */
    fun setWillOpenPercentAfterOpen(willOpenPercentAfterOpen: Float) {
        this.mWillOpenPercentAfterOpen = willOpenPercentAfterOpen
    }

    /***
     * Returns the percentage of revealing at which the view below should the view finish opening
     * if it was already closed before dragging
     *
     * @returns The percentage of view revealed to trigger, default value is 0.25
     */
    fun getWillOpenPercentAfterClose(): Float {
        return mWillOpenPercentAfterClose
    }

    /***
     * Allows to stablish at what percentage of revealing the view below should the view finish opening
     * if it was already closed before dragging
     *
     * @param willOpenPercentAfterClose The percentage of view revealed to trigger, default value is 0.75
     */
    fun setWillOpenPercentAfterClose(willOpenPercentAfterClose: Float) {
        this.mWillOpenPercentAfterClose = willOpenPercentAfterClose
    }

    private fun insideAdapterView(): Boolean {
        return getAdapterView() != null
    }

    private fun getAdapterView(): AdapterView<*>? {
        val t = parent
        return t as? AdapterView<*>
    }

    private fun performAdapterViewItemClick() {
        if (getOpenStatus() != Status.Close) return
        val t = parent
        if (t is AdapterView<*>) {
            val p = t.getPositionForView(this@SwipeLayout)
            if (p != AdapterView.INVALID_POSITION) {
                t.performItemClick(t.getChildAt(p - t.firstVisiblePosition), p, t
                        .adapter.getItemId(p))
            }
        }
    }

    private fun performAdapterViewItemLongClick(): Boolean {
        if (getOpenStatus() != Status.Close) return false
        val t = parent
        if (t is AdapterView<*>) {
            val p = t.getPositionForView(this@SwipeLayout)
            if (p == AdapterView.INVALID_POSITION) return false
            val vId = t.getItemIdAtPosition(p)
            var handled = false
            try {
                val m = AbsListView::class.java.getDeclaredMethod("performLongPress", View::class.java, Int::class.javaPrimitiveType, Long::class.javaPrimitiveType)
                m.isAccessible = true
                handled = m.invoke(t, this@SwipeLayout, p, vId) as Boolean

            } catch (e: Exception) {
                e.printStackTrace()

                if (t.onItemLongClickListener != null) {
                    handled = t.onItemLongClickListener.onItemLongClick(t, this@SwipeLayout, p, vId)
                }
                if (handled) {
                    t.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }

            return handled
        }
        return false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (insideAdapterView()) {
            if (clickListener == null) {
                setOnClickListener { performAdapterViewItemClick() }
            }
            if (longClickListener == null) {
                setOnLongClickListener {
                    performAdapterViewItemLongClick()
                    true
                }
            }
        }
    }

    var clickListener: View.OnClickListener? = null

    override fun setOnClickListener(l: View.OnClickListener?) {
        super.setOnClickListener(l)
        clickListener = l
    }

    var longClickListener: View.OnLongClickListener? = null

    override fun setOnLongClickListener(l: View.OnLongClickListener?) {
        super.setOnLongClickListener(l)
        longClickListener = l
    }

    private var hitSurfaceRect: Rect? = null

    private fun isTouchOnSurface(ev: MotionEvent): Boolean {
        val surfaceView = getSurfaceView() ?: return false
        if (hitSurfaceRect == null) {
            hitSurfaceRect = Rect()
        }
        surfaceView.getHitRect(hitSurfaceRect)
        return hitSurfaceRect!!.contains(ev.x.toInt(), ev.y.toInt())
    }

    private val gestureDetector = GestureDetector(context, SwipeDetector())

    internal inner class SwipeDetector : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (mClickToClose && isTouchOnSurface(e)) {
                close()
            }
            return super.onSingleTapUp(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (mDoubleClickListener != null) {
                val target: View?
                val bottom = getCurrentBottomView()
                val surface = getSurfaceView()
                if (bottom != null && e.x > bottom.left && e.x < bottom.right
                        && e.y > bottom.top && e.y < bottom.bottom) {
                    target = bottom
                } else {
                    target = surface
                }
                mDoubleClickListener!!.onDoubleClick(this@SwipeLayout, target === surface)
            }
            return true
        }
    }

    /**
     * set the drag distance, it will force set the bottom view's width or
     * height via this value.
     *
     * @param max max distance in dp unit
     */
    fun setDragDistance(max: Int) {
        var max = max
        if (max < 0) max = 0
        mDragDistance = dp2px(max.toFloat())
        requestLayout()
    }

    /**
     * There are 2 diffirent show mode.
     * [com.daimajia.swipe.SwipeLayout.ShowMode].PullOut and
     * [com.daimajia.swipe.SwipeLayout.ShowMode].LayDown.
     *
     * @param mode
     */
    fun setShowMode(mode: ShowMode) {
        mShowMode = mode
        requestLayout()
    }

    fun getDragEdge(): DragEdge {
        return mCurrentDragEdge
    }

    fun getDragDistance(): Int {
        return mDragDistance
    }

    fun getShowMode(): ShowMode {
        return mShowMode!!
    }

    /**
     * return null if there is no surface view(no children)
     */
    fun getSurfaceView(): View? {
        return if (childCount == 0) null else getChildAt(childCount - 1)
    }

    /**
     * return null if there is no bottom view
     */
    fun getCurrentBottomView(): View? {
        val bottoms = getBottomViews()
        return if (mCurrentDragEdge.ordinal < bottoms.size) {
            bottoms[mCurrentDragEdge.ordinal]
        } else null
    }

    /**
     * @return all bottomViews: left, top, right, bottom (may null if the edge is not set)
     */
    fun getBottomViews(): List<View?> {
        val bottoms = ArrayList<View?>()
        for (dragEdge in DragEdge.values()) {
            bottoms.add(mDragEdges[dragEdge])
        }
        return bottoms
    }

    enum class Status {
        Middle,
        Open,
        Close
    }

    /**
     * get the open status.
     *
     * @return [com.daimajia.swipe.SwipeLayout.Status] Open , Close or
     * Middle.
     */
    fun getOpenStatus(): Status {
        val surfaceView = getSurfaceView() ?: return Status.Close
        val surfaceLeft = surfaceView.left
        val surfaceTop = surfaceView.top
        if (surfaceLeft == paddingLeft && surfaceTop == paddingTop) return Status.Close

        return if (surfaceLeft == paddingLeft - mDragDistance || surfaceLeft == paddingLeft + mDragDistance
                || surfaceTop == paddingTop - mDragDistance || surfaceTop == paddingTop + mDragDistance) Status.Open else Status.Middle

    }


    /**
     * Process the surface release event.
     *
     * @param xvel                 xVelocity
     * @param yvel                 yVelocity
     * @param isCloseBeforeDragged the open state before drag
     */
    protected fun processHandRelease(xvel: Float, yvel: Float, isCloseBeforeDragged: Boolean) {
        val minVelocity = mDragHelper!!.getMinVelocity()
        val surfaceView = getSurfaceView()
        val currentDragEdge = mCurrentDragEdge
        if (currentDragEdge == null || surfaceView == null) {
            return
        }
        val willOpenPercent = if (isCloseBeforeDragged) mWillOpenPercentAfterClose else mWillOpenPercentAfterOpen
        if (currentDragEdge === DragEdge.Left) {
            if (xvel > minVelocity)
                open()
            else if (xvel < -minVelocity)
                close()
            else {
                val openPercent = 1f * getSurfaceView()!!.left / mDragDistance
                if (openPercent > willOpenPercent)
                    open()
                else
                    close()
            }
        } else if (currentDragEdge === DragEdge.Right) {
            if (xvel > minVelocity)
                close()
            else if (xvel < -minVelocity)
                open()
            else {
                val openPercent = 1f * -getSurfaceView()!!.left / mDragDistance
                if (openPercent > willOpenPercent)
                    open()
                else
                    close()
            }
        } else if (currentDragEdge === DragEdge.Top) {
            if (yvel > minVelocity)
                open()
            else if (yvel < -minVelocity)
                close()
            else {
                val openPercent = 1f * getSurfaceView()!!.top / mDragDistance
                if (openPercent > willOpenPercent)
                    open()
                else
                    close()
            }
        } else if (currentDragEdge === DragEdge.Bottom) {
            if (yvel > minVelocity)
                close()
            else if (yvel < -minVelocity)
                open()
            else {
                val openPercent = 1f * -getSurfaceView()!!.top / mDragDistance
                if (openPercent > willOpenPercent)
                    open()
                else
                    close()
            }
        }
    }

    /**
     * smoothly open surface.
     */
    fun open() {
        open(true, true)
    }

    fun open(smooth: Boolean) {
        open(smooth, true)
    }

    fun open(smooth: Boolean, notify: Boolean) {
        val surface = getSurfaceView()
        val bottom = getCurrentBottomView()
        if (surface == null) {
            return
        }
        val dx: Int
        val dy: Int
        val rect = computeSurfaceLayoutArea(true)
        if (smooth) {
            mDragHelper!!.smoothSlideViewTo(surface, rect.left, rect.top)
        } else {
            dx = rect.left - surface.left
            dy = rect.top - surface.top
            surface.layout(rect.left, rect.top, rect.right, rect.bottom)
            if (getShowMode() === ShowMode.PullOut) {
                val bRect = computeBottomLayoutAreaViaSurface(ShowMode.PullOut, rect)
                bottom?.layout(bRect.left, bRect.top, bRect.right, bRect.bottom)
            }
            if (notify) {
                dispatchRevealEvent(rect.left, rect.top, rect.right, rect.bottom)
                dispatchSwipeEvent(rect.left, rect.top, dx, dy)
            } else {
                safeBottomView()
            }
        }
        invalidate()
    }

    fun open(edge: DragEdge) {
        setCurrentDragEdge(edge)
        open(true, true)
    }

    fun open(smooth: Boolean, edge: DragEdge) {
        setCurrentDragEdge(edge)
        open(smooth, true)
    }

    fun open(smooth: Boolean, notify: Boolean, edge: DragEdge) {
        setCurrentDragEdge(edge)
        open(smooth, notify)
    }

    /**
     * smoothly close surface.
     */
    fun close() {
        close(true, true)
    }

    fun close(smooth: Boolean) {
        close(smooth, true)
    }

    /**
     * close surface
     *
     * @param smooth smoothly or not.
     * @param notify if notify all the listeners.
     */
    fun close(smooth: Boolean, notify: Boolean) {
        val surface = getSurfaceView() ?: return
        val dx: Int
        val dy: Int
        if (smooth)
            mDragHelper!!.smoothSlideViewTo(getSurfaceView(), paddingLeft, paddingTop)
        else {
            val rect = computeSurfaceLayoutArea(false)
            dx = rect.left - surface.left
            dy = rect.top - surface.top
            surface.layout(rect.left, rect.top, rect.right, rect.bottom)
            if (notify) {
                dispatchRevealEvent(rect.left, rect.top, rect.right, rect.bottom)
                dispatchSwipeEvent(rect.left, rect.top, dx, dy)
            } else {
                safeBottomView()
            }
        }
        invalidate()
    }

    fun toggle() {
        toggle(true)
    }

    fun toggle(smooth: Boolean) {
        if (getOpenStatus() == Status.Open)
            close(smooth)
        else if (getOpenStatus() == Status.Close) open(smooth)
    }


    /**
     * a helper function to compute the Rect area that surface will hold in.
     *
     * @param open open status or close status.
     */
    private fun computeSurfaceLayoutArea(open: Boolean): Rect {
        var l = paddingLeft
        var t = paddingTop
        if (open) {
            if (mCurrentDragEdge === DragEdge.Left)
                l = paddingLeft + mDragDistance
            else if (mCurrentDragEdge === DragEdge.Right)
                l = paddingLeft - mDragDistance
            else if (mCurrentDragEdge === DragEdge.Top)
                t = paddingTop + mDragDistance
            else
                t = paddingTop - mDragDistance
        }
        return Rect(l, t, l + measuredWidth, t + measuredHeight)
    }

    private fun computeBottomLayoutAreaViaSurface(mode: ShowMode, surfaceArea: Rect): Rect {
        val bottomView = getCurrentBottomView()

        var bl = surfaceArea.left
        var bt = surfaceArea.top
        var br = surfaceArea.right
        var bb = surfaceArea.bottom
        if (mode === ShowMode.PullOut) {
            if (mCurrentDragEdge === DragEdge.Left)
                bl = surfaceArea.left - mDragDistance
            else if (mCurrentDragEdge === DragEdge.Right)
                bl = surfaceArea.right
            else if (mCurrentDragEdge === DragEdge.Top)
                bt = surfaceArea.top - mDragDistance
            else
                bt = surfaceArea.bottom

            if (mCurrentDragEdge === DragEdge.Left || mCurrentDragEdge === DragEdge.Right) {
                bb = surfaceArea.bottom
                br = bl + (bottomView?.measuredWidth ?: 0)
            } else {
                bb = bt + (bottomView?.measuredHeight ?: 0)
                br = surfaceArea.right
            }
        } else if (mode === ShowMode.LayDown) {
            if (mCurrentDragEdge === DragEdge.Left)
                br = bl + mDragDistance
            else if (mCurrentDragEdge === DragEdge.Right)
                bl = br - mDragDistance
            else if (mCurrentDragEdge === DragEdge.Top)
                bb = bt + mDragDistance
            else
                bt = bb - mDragDistance

        }
        return Rect(bl, bt, br, bb)

    }

    private fun computeBottomLayDown(dragEdge: DragEdge): Rect {
        var bl = paddingLeft
        var bt = paddingTop
        val br: Int
        val bb: Int
        if (dragEdge === DragEdge.Right) {
            bl = measuredWidth - mDragDistance
        } else if (dragEdge === DragEdge.Bottom) {
            bt = measuredHeight - mDragDistance
        }
        if (dragEdge === DragEdge.Left || dragEdge === DragEdge.Right) {
            br = bl + mDragDistance
            bb = bt + measuredHeight
        } else {
            br = bl + measuredWidth
            bb = bt + mDragDistance
        }
        return Rect(bl, bt, br, bb)
    }

    fun setOnDoubleClickListener(doubleClickListener: DoubleClickListener) {
        mDoubleClickListener = doubleClickListener
    }

    interface DoubleClickListener {
        fun onDoubleClick(layout: SwipeLayout, surface: Boolean)
    }

    private fun dp2px(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }


    /**
     * Deprecated, use [.setDrag]
     */
    @Deprecated("")
    fun setDragEdge(dragEdge: DragEdge) {
        clearDragEdge()
        if (childCount >= 2) {
            mDragEdges[dragEdge] = getChildAt(childCount - 2)
        }
        setCurrentDragEdge(dragEdge)
    }

    override fun onViewRemoved(child: View) {
        for (entry in HashMap<DragEdge, View>(mDragEdges).entries) {
            if (entry.value === child) {
                mDragEdges.remove(entry.key)
            }
        }
    }

    fun getDragEdgeMap(): Map<DragEdge, View?> {
        return mDragEdges
    }

    /**
     * Deprecated, use [.getDragEdgeMap]
     */
    @Deprecated("")
    fun getDragEdges(): List<DragEdge> {
        return ArrayList<DragEdge>(mDragEdges.keys)
    }

    /**
     * Deprecated, use [.setDrag]
     */
    @Deprecated("")
    fun setDragEdges(dragEdges: List<DragEdge>) {
        clearDragEdge()
        var i = 0
        val size = Math.min(dragEdges.size, childCount - 1)
        while (i < size) {
            val dragEdge = dragEdges[i]
            mDragEdges[dragEdge] = getChildAt(i)
            i++
        }
        if (dragEdges.size == 0 || dragEdges.contains(DefaultDragEdge)) {
            setCurrentDragEdge(DefaultDragEdge)
        } else {
            setCurrentDragEdge(dragEdges[0])
        }
    }

    /**
     * Deprecated, use [.addDrag]
     */
    @Deprecated("")
    fun setDragEdges(vararg mDragEdges: DragEdge) {
        clearDragEdge()
        setDragEdges(mDragEdges.toList())
    }

    /**
     * Deprecated, use [.addDrag]
     * When using multiple drag edges it's a good idea to pass the ids of the views that
     * you're using for the left, right, top bottom views (-1 if you're not using a particular view)
     */
    @Deprecated("")
    fun setBottomViewIds(leftId: Int, rightId: Int, topId: Int, bottomId: Int) {
        addDrag(DragEdge.Left, findViewById<View>(leftId))
        addDrag(DragEdge.Right, findViewById<View>(rightId))
        addDrag(DragEdge.Top, findViewById<View>(topId))
        addDrag(DragEdge.Bottom, findViewById<View>(bottomId))
    }

    private fun getCurrentOffset(): Float {
        return if (mCurrentDragEdge == null) 0f else mEdgeSwipesOffset[mCurrentDragEdge.ordinal]
    }

    private fun setCurrentDragEdge(dragEdge: DragEdge) {
        mCurrentDragEdge = dragEdge
        updateBottomViews()
    }

    private fun updateBottomViews() {
        val currentBottomView = getCurrentBottomView()
        if (currentBottomView != null) {
            if (mCurrentDragEdge === DragEdge.Left || mCurrentDragEdge === DragEdge.Right) {
                mDragDistance = currentBottomView.measuredWidth - dp2px(getCurrentOffset())
            } else {
                mDragDistance = currentBottomView.measuredHeight - dp2px(getCurrentOffset())
            }
        }

        if (mShowMode === ShowMode.PullOut) {
            layoutPullOut()
        } else if (mShowMode === ShowMode.LayDown) {
            layoutLayDown()
        }

        safeBottomView()
    }

}