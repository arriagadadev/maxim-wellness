@file:Suppress("NOTHING_TO_INLINE")


package com.maximintegrated.maximsensorsapp.exts

import android.content.Context
import android.util.TypedValue
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.maximintegrated.maximsensorsapp.R

inline val Context.accentColor
    get() = color(R.color.colorAccent)

inline fun Context.color(@ColorRes colorResId: Int) = ContextCompat.getColor(this, colorResId)

inline fun Context.windowBackgroundColor(): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
    return typedValue.data
}

@ColorInt
fun Context.getThemeColor(@AttrRes attributeColor: Int): Int {
    val value = TypedValue()
    theme.resolveAttribute(attributeColor, value, true)
    return value.data
}

inline fun Context.drawable(@DrawableRes drawableResId: Int) =
    ContextCompat.getDrawable(this, drawableResId)

inline fun FragmentActivity.addFragment(fragment: Fragment, @IdRes containerId: Int = R.id.fragmentContainer) {
    supportFragmentManager.beginTransaction()
        .setCustomAnimations(
            R.anim.bottom_sheet_slide_in,
            R.anim.bottom_sheet_slide_out,
            R.anim.bottom_sheet_slide_in,
            R.anim.bottom_sheet_slide_out
        )
        .add(containerId, fragment)
        .addToBackStack(null)
        .commit()
}

inline fun FragmentActivity.replaceFragment(fragment: Fragment, @IdRes containerId: Int = R.id.fragmentContainer) {
    supportFragmentManager.beginTransaction()
        .replace(containerId, fragment)
        .commit()
}

inline fun  FragmentActivity.getCurrentFragment(): Fragment? {
    return supportFragmentManager.findFragmentById(R.id.fragmentContainer)
}

