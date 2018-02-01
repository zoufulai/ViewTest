package com.szshy.viewtest.widget

import android.content.Context
import android.graphics.Point
import android.support.v4.widget.ViewDragHelper
import android.support.v4.widget.ViewDragHelper.create
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout

/**
 * Created by szshy on 2018/1/25.
 */
class VDHLayout: LinearLayout {

    private var mDragger: ViewDragHelper?=null
    private var mAutoBackView:View?=null
    private var mDragView:View?= null
    private var mEdgeTrackerView:View?=null
    private val mAutoBackOriginPos = Point()

    constructor(context:Context,attrs:AttributeSet):super(context,attrs){

        mDragger = create(this,1.0f,object :ViewDragHelper.Callback(){
            override fun tryCaptureView(child: View?, pointerId: Int): Boolean {
                mDragView = child
               return true
            }

            override fun clampViewPositionHorizontal(child: View?, left: Int, dx: Int): Int {
                return left
            }

            override fun clampViewPositionVertical(child: View?, top: Int, dy: Int): Int {
                return top
            }

            override fun onViewReleased(releasedChild: View?, xvel: Float, yvel: Float) {
                super.onViewReleased(releasedChild, xvel, yvel)
                if(mAutoBackView===releasedChild){
                    mDragger!!.settleCapturedViewAt(mAutoBackOriginPos.x, mAutoBackOriginPos.y);
                    invalidate();
                }
            }

            override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
                super.onEdgeDragStarted(edgeFlags, pointerId)
                mDragger!!.captureChildView(mEdgeTrackerView, pointerId);
            }

        })

        mDragger!!.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
    }


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return mDragger!!.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mDragger!!.processTouchEvent(event)
        return true
    }


    override fun computeScroll() {
        if(mDragger!!.continueSettling(true)){
            invalidate()
        }
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        mAutoBackOriginPos.x = mAutoBackView!!.left
        mAutoBackOriginPos.y = mAutoBackView!!.top
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mDragView = getChildAt(0);
        mAutoBackView = getChildAt(1);
        mEdgeTrackerView = getChildAt(2);
    }


}