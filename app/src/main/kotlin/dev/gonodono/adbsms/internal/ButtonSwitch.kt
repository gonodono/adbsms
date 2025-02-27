package dev.gonodono.adbsms.internal

import android.content.Context
import android.util.AttributeSet
import android.widget.Switch
import dev.gonodono.adbsms.R

class ButtonSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.buttonSwitchStyle
) : Switch(context, attrs, defStyleAttr) {

    init {
        super.setShowText(false)
        text = if (isChecked) textOn else textOff
    }

    override fun setShowText(showText: Boolean) {}

    override fun setChecked(checked: Boolean) {
        if (isChecked == checked) return
        text = if (checked) textOn else textOff
        super.setChecked(checked)
    }

    override fun toggle() {}
}