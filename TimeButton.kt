package com.md.matur.utils.customview

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import com.google.android.flexbox.FlexboxLayoutManager
import com.md.matur.R
import com.md.matur.utils.dpToPx
import com.md.matur.utils.pxToDp


class TimeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : : AppCompatButton(context) {
    var timeSelected: Boolean = false
    set(selected) {
        if (selected) applySelectedStyle() else applyEnabledStyle()
        field = selected
    }

    var timeStamp: Long = -1

    override fun setEnabled(enabled: Boolean) {
        if (enabled) applyEnabledStyle()
        else applyDisabledStyle()
        super.setEnabled(enabled)
    }

    init {
        if (isEnabled)
            applyEnabledStyle()
        else applyDisabledStyle()
        minimumHeight = 0
        minimumWidth = 0
        minWidth = 0
        minHeight = 0
        val verticalMargin = 16.dpToPx()
        val horizontalPadding = 7.dpToPx()
        val verticalPadding = 10.dpToPx()

        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels.pxToDp()
        val howMuchButtonsInOneRow = if (width >= 360) 4 else 4

        val buttonWidth = ((width - (16 * 5)) / howMuchButtonsInOneRow)
        layoutParams = FlexboxLayoutManager.LayoutParams(
            buttonWidth.dpToPx(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(16.dpToPx(), 0, 0.dpToPx(), verticalMargin)
        }
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        setTextSize(COMPLEX_UNIT_DIP, 16f)
    }

    private fun applyEnabledStyle() {
        setBackgroundResource(R.drawable.shape_time_enabled)
        setTextAppearance(context, R.style.TimeButtonEnabled)
        setTextColor(resources.getColor(R.color.timeButtonColor))
    }

    private fun applyDisabledStyle() {
        setBackgroundResource(R.drawable.shape_time_disabled)
        setTextAppearance(context, R.style.TimeButtonDisabled)
        setTextColor(resources.getColor(R.color.colorStdGrey))
    }

    private fun applySelectedStyle() {
        setBackgroundResource(R.drawable.shape_time_selected)
        setTextAppearance(context, R.style.TimeButtonEnabled)
        setTextColor(resources.getColor(R.color.colorPrimary))
    }
}