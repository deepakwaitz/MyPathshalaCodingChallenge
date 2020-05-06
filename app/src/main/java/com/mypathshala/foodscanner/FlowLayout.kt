package com.mypathshala.foodscanner

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

class FlowLayout : ViewGroup {
    private var mLineHeight: Int = 0
    private var mVerticalSpacing: Int = 0
    private var mHorizontalSpacing: Int = 0

    /**
     * Constructor with no attributes
     *
     * @param context Context
     */
    constructor(context: Context) : super(context) {
        init(context, null)
    }

    /**
     * Constructor with attribute set
     *
     * @param context Context
     * @param attrs   attribute set
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout)
            mVerticalSpacing = array.getDimensionPixelSize(
                R.styleable.FlowLayout_verticalSpacing,
                context.resources.getDimension(R.dimen.size_8dp).toInt()
            )
            mHorizontalSpacing = array.getDimensionPixelSize(
                R.styleable.FlowLayout_horizontalSpacing,
                context.resources.getDimension(R.dimen.size_8dp).toInt()
            )
            array.recycle()
        } else {
            mVerticalSpacing = context.resources.getDimensionPixelSize(R.dimen.size_8dp)
            mHorizontalSpacing = context.resources.getDimensionPixelSize(R.dimen.size_8dp)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var height = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
        val count = childCount
        var lineHeight = 0

        var xpos = paddingLeft
        var ypos = paddingTop

        val childHeightMeasureSpec: Int
        childHeightMeasureSpec =
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
            } else {
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            }

        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility !== View.GONE) {
                child.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    childHeightMeasureSpec
                )
                val childMeasuredWidth = child.measuredWidth
                lineHeight = Math.max(lineHeight, child.measuredHeight + mVerticalSpacing)

                if (xpos + childMeasuredWidth > width) {
                    xpos = paddingLeft
                    ypos += lineHeight
                }

                xpos += childMeasuredWidth + mHorizontalSpacing
            }
        }
        this.mLineHeight = lineHeight

        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            height = ypos + lineHeight

        } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST && ypos + lineHeight < height) {
            height = ypos + lineHeight
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        val width = r - l
        var xpos = paddingLeft
        var ypos = paddingTop

        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility !== View.GONE) {
                val childMeasuredWidth = child.measuredWidth
                val childMeasuredHeight = child.measuredHeight
                if (xpos + childMeasuredWidth > width) {
                    xpos = paddingLeft
                    ypos += mLineHeight
                }
                child.layout(xpos, ypos, xpos + childMeasuredWidth, ypos + childMeasuredHeight)
                xpos += childMeasuredWidth + mHorizontalSpacing
            }
        }
    }
}
