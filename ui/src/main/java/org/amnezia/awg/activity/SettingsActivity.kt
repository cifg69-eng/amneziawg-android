/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.amnezia.awg.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.amnezia.awg.Application
import org.amnezia.awg.QuickTileService
import org.amnezia.awg.R
import org.amnezia.awg.backend.AwgQuickBackend
import org.amnezia.awg.preference.PreferencesPreferenceDataStore
import org.amnezia.awg.util.AdminKnobs

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            title = getString(R.string.cif_settings_title)
            setDisplayHomeAsUpEnabled(true)
        }
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.commit {
                add(android.R.id.content, SettingsFragment())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, key: String?) {
            preferenceManager.preferenceDataStore =
                PreferencesPreferenceDataStore(
                    lifecycleScope,
                    Application.getPreferencesDataStore()
                )
            addPreferencesFromResource(R.xml.preferences)
            preferenceScreen.initialExpandedChildrenCount = 5

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || QuickTileService.isAdded) {
                preferenceManager.findPreference<Preference>("quick_tile")
                    ?.parent
                    ?.removePreference(preferenceManager.findPreference("quick_tile"))
                --preferenceScreen.initialExpandedChildrenCount
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                preferenceManager.findPreference<Preference>("dark_theme")
                    ?.parent
                    ?.removePreference(preferenceManager.findPreference("dark_theme"))
                --preferenceScreen.initialExpandedChildrenCount
            }

            if (AdminKnobs.disableConfigExport) {
                preferenceManager.findPreference<Preference>("zip_exporter")
                    ?.parent
                    ?.removePreference(preferenceManager.findPreference("zip_exporter"))
            }

            val awgQuickOnlyPrefs = arrayOf(
                preferenceManager.findPreference("tools_installer"),
                preferenceManager.findPreference("restore_on_boot"),
                preferenceManager.findPreference<Preference>("multiple_tunnels")
            ).filterNotNull()

            awgQuickOnlyPrefs.forEach { it.isVisible = false }

            lifecycleScope.launch {
                if (Application.getBackend() is AwgQuickBackend) {
                    ++preferenceScreen.initialExpandedChildrenCount
                    awgQuickOnlyPrefs.forEach { it.isVisible = true }
                } else {
                    awgQuickOnlyPrefs.forEach { it.parent?.removePreference(it) }
                }
            }

            preferenceManager.findPreference<Preference>("log_viewer")
                ?.setOnPreferenceClickListener {
                    startActivity(Intent(requireContext(), LogViewerActivity::class.java))
                    true
                }

            val kernelPreference =
                preferenceManager.findPreference<Preference>("kernel_module_enabler")

            if (AwgQuickBackend.hasKernelSupport()) {
                lifecycleScope.launch {
                    if (Application.getBackend() !is AwgQuickBackend) {
                        try {
                            withContext(Dispatchers.IO) {
                                Application.getRootShell().start()
                            }
                        } catch (_: Throwable) {
                            kernelPreference?.parent?.removePreference(kernelPreference)
                        }
                    }
                }
            } else {
                kernelPreference?.parent?.removePreference(kernelPreference)
            }
        }
    }
}
