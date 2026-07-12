/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.amnezia.awg.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import org.amnezia.awg.BuildConfig
import org.amnezia.awg.R

class VersionPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private val cleanVersion = BuildConfig.VERSION_NAME
        .removeSuffix("-debug")
        .removeSuffix("-release")
        .removeSuffix("-googleplay")

    override fun getTitle(): CharSequence =
        context.getString(R.string.cif_version_title, cleanVersion)

    override fun getSummary(): CharSequence =
        context.getString(R.string.cif_version_summary)

    init {
        isSelectable = false
    }
}
