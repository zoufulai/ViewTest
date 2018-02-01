package com.szshy.viewtest.widget

import android.content.Context
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup


/**
 * Created by szshy on 2018/1/25.
 */
class LeftDrawLayout:ViewGroup{

    companion object {
        private var MIN_DRAWER_MARGIN:Int=64

        private var MIN_FING_VELOGITY = 400
    }

    /**
     * drawer离父容器右边的最小边距
     */
    private var mMinDrawerMargin:Int?=0

    private var mLeftMenuView: View?=null
    private var mContentView:View?=null

    private var mHelper:ViewDragHelper?=null;

    /**
     * drawer显示出来的占自身的百分比
     */
    private var mLeftMenuOnScreen:Float=0f

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val menuView = mLeftMenuView
        val contentView = mContentView

        var lp = contentView!!.getLayoutParams() as ViewGroup.MarginLayoutParams
        contentView.layout(lp.leftMargin, lp.topMargin,
                lp.leftMargin + contentView.getMeasuredWidth(),
                lp.topMargin + contentView.getMeasuredHeight())

        lp = menuView!!.getLayoutParams() as ViewGroup.MarginLayoutParams

        val menuWidth = menuView.getMeasuredWidth()
        val childLeft = (-menuWidth + (menuWidth * mLeftMenuOnScreen)).toInt()
        menuView.layout(childLeft, lp.topMargin, childLeft + menuWidth,
                lp.topMargin + menuView.getMeasuredHeight())
    }


    constructor(context: Context,attrs:AttributeSet):super(context,attrs){
        val density= resources.displayMetrics.density
        val minVal = MIN_FING_VELOGITY*density

        mMinDrawerMargin = (MIN_DRAWER_MARGIN * density + 0.5f).toInt()

        mHelper = ViewDragHelper.create(this,1.0f,object :ViewDragHelper.Callback(){
            override fun tryCaptureView(child: View?, pointerId: Int): Boolean {
                 return child==mLeftMenuView
            }

            override fun clampViewPositionHorizontal(child: View?, left: Int, dx: Int): Int {
                var newLeft = Math.max(-child!!.width, Math.min(left, 0))
                return newLeft
            }

            override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
                mHelper!!.captureChildView(mLeftMenuView,pointerId)
            }

            override fun onViewReleased(releasedChild: View?, xvel: Float, yvel: Float) {
                val childWidth = releasedChild!!.width
                var offset = (childWidth+releasedChild.left)*1.0f/childWidth
                mHelper!!.settleCapturedViewAt(if (xvel > 0 || xvel === 0f && offset > 0.5f) 0 else -childWidth,releasedChild.top)
                invalidate()
            }

            override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
                var childWidth = changedView!!.width
                var offset = (childWidth+left).toFloat()/childWidth
                mLeftMenuOnScreen = offset
                changedView.visibility = if(offset==0f)View.INVISIBLE else View.VISIBLE
                invalidate()
            }

            override fun getViewHorizontalDragRange(child: View?): Int {
                if(mLeftMenuView==child){
                    return child!!.width
                }else {
                    return 0
                }
            }

        })

        mHelper!!.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT)
        mHelper!!.minVelocity = minVal
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSize,heightSize)
        var leftMenuView = getChildAt(1)
        var lp = leftMenuView.layoutParams as MarginLayoutParams
        var drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec,mMinDrawerMargin!!+lp.leftMargin+lp.rightMargin,lp.width)
        var drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec,lp.topMargin+lp.bottomMargin,lp.height)
        leftMenuView.measure(drawerWidthSpec,drawerHeightSpec)

        var contentView = getChildAt(0)
        lp = contentView.layoutParams as MarginLayoutParams
        var contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize-lp.leftMargin-lp.rightMargin,MeasureSpec.EXACTLY)
        var contentHeightSpec = MeasureSpec.makeMeasureSpec(heightSize-lp.topMargin-lp.bottomMargin,MeasureSpec.EXACTLY)
        contentView.measure(contentWidthSpec,contentHeightSpec)

        mLeftMenuView = leftMenuView
        mContentView = contentView
    }


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return mHelper!!.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mHelper!!.processTouchEvent(event)
        return true
    }


    override fun computeScroll() {
        if(mHelper!!.continueSettling(true)){
            invalidate()
        }
    }

    fun closeDrawer(){
        post {
            var menuView = mLeftMenuView
            mLeftMenuOnScreen = 0f
            mHelper!!.smoothSlideViewTo(menuView,-menuView!!.width,-menuView!!.top)
            invalidate()
        }

    }

    fun openDrawer(){
        post{
            var menuView = mLeftMenuView
            mLeftMenuOnScreen = 1f
            mHelper!!.smoothSlideViewTo(menuView,0,menuView!!.top)
            invalidate()
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }


    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context,attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return MarginLayoutParams(p)
    }




}