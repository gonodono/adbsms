package dev.gonodono.adbsms.internal

import android.content.Context
import android.util.AttributeSet
import android.view.SoundEffectConstants
import androidx.appcompat.widget.SwitchCompat
import dev.gonodono.adbsms.R

class ButtonSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.switchStyle
) : SwitchCompat(context, attrs, defStyleAttr) {

    private var isChanging: Boolean = false

    private var onClickListener: OnClickListener? = null

    fun setCheckedActual(checked: Boolean) {
        isChanging = true
        isChecked = checked
        isChanging = false
    }

    override fun setChecked(checked: Boolean) {
        if (isChanging) {
            setText(
                if (checked) R.string.label_revert
                else R.string.label_enable
            )
            super.setChecked(checked)
        } else {
            playSoundEffect(SoundEffectConstants.CLICK)
            onClickListener?.onClick(this)
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        onClickListener = l
    }
}