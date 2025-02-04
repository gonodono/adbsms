package dev.gonodono.adbsms.internal

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import dev.gonodono.adbsms.R

class ButtonSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwitchCompat(context, attrs) {

    override fun setChecked(checked: Boolean) {
        setText(if (checked) R.string.label_revert else R.string.label_enable)
        super.setChecked(checked)
    }

    override fun toggle() {}
}