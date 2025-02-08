package dev.gonodono.adbsms.internal

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import dev.gonodono.adbsms.R
import kotlin.math.roundToInt

class ButtonSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwitchCompat(context, attrs) {

    init {
        minHeight = (48 * resources.displayMetrics.density).roundToInt()
        switchPadding = (10 * resources.displayMetrics.density).roundToInt()
        val textColor = resources.getColorStateList(
            R.color.button_switch_text,
            context.theme
        )
        setTextColor(textColor)
    }

    override fun setChecked(checked: Boolean) {
        setText(if (checked) R.string.label_revert else R.string.label_enable)
        super.setChecked(checked)
    }

    override fun toggle() {}
}