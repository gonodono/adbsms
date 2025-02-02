package dev.gonodono.adbsms.internal

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.ViewSwitcher
import dev.gonodono.adbsms.R

class DualViewSwitcher @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null
) : ViewSwitcher(context, attrs) {

    private val inLeftAnimation =
        loadAnimation(context, R.anim.slide_in_left)
    private val outLeftAnimation =
        loadAnimation(context, R.anim.slide_out_left)
    private val inRightAnimation =
        loadAnimation(context, R.anim.slide_in_right)
    private val outRightAnimation =
        loadAnimation(context, R.anim.slide_out_right)

    private var isAnimating = false

    private val listener = object : AnimationListener {
        override fun onAnimationStart(anim: Animation) {}
        override fun onAnimationRepeat(anim: Animation) {}
        override fun onAnimationEnd(anim: Animation) {
            isAnimating = false
        }
    }

    override fun setDisplayedChild(whichChild: Int) {
        check(whichChild == FIRST_CHILD || whichChild == SECOND_CHILD)
        setChildAndAnimate(whichChild.coerceIn(0..1), true)
    }

    private fun setChildAndAnimate(whichChild: Int, doAnimate: Boolean) {
        if (displayedChild == whichChild) return
        setAnimationForChild(if (doAnimate) whichChild else null)
        isAnimating = doAnimate
        super.setDisplayedChild(whichChild)
    }

    private fun setAnimationForChild(whichChild: Int?) {
        when (whichChild) {
            FIRST_CHILD -> {
                inAnimation = inLeftAnimation
                outAnimation = outRightAnimation
                inRightAnimation.setAnimationListener(null)
                inLeftAnimation.setAnimationListener(listener)
            }
            SECOND_CHILD -> {
                inAnimation = inRightAnimation
                outAnimation = outLeftAnimation
                inRightAnimation.setAnimationListener(listener)
                inLeftAnimation.setAnimationListener(null)
            }
            else -> {
                inAnimation = null
                outAnimation = null
                inRightAnimation.setAnimationListener(null)
                inLeftAnimation.setAnimationListener(null)
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean =
        isAnimating || super.onInterceptTouchEvent(ev)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        check(childCount == 2) { "DualPaneView must have exactly two children" }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.whichChild = displayedChild
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        setChildAndAnimate(ss.whichChild, false)
    }

    private class SavedState : BaseSavedState {

        var whichChild: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            whichChild = `in`.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(whichChild)
        }

        companion object {

            @Suppress("unused")
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {

                override fun createFromParcel(`in`: Parcel): SavedState =
                    SavedState(`in`)

                override fun newArray(size: Int): Array<SavedState?> =
                    arrayOfNulls(size)
            }
        }
    }

    companion object {

        const val FIRST_CHILD: Int = 0
        const val SECOND_CHILD: Int = 1
    }
}