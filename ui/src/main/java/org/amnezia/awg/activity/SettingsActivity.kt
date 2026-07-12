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
                val quickTile = preferenceManager.findPreference<Preference>("quick_tile")
                quickTile?.parent?.removePreference(quickTile)
                --preferenceScreen.initialExpandedChildrenCount
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val darkTheme = preferenceManager.findPreference<Preference>("dark_theme")
                darkTheme?.parent?.removePreference(darkTheme)
                --preferenceScreen.initialExpandedChildrenCount
            }

            if (AdminKnobs.disableConfigExport) {
                val zipExporter = preferenceManager.findPreference<Preference>("zip_exporter")
                zipExporter?.parent?.removePreference(zipExporter)
            }

            val toolsInstaller =
                preferenceManager.findPreference<Preference>("tools_installer")
            val restoreOnBoot =
                preferenceManager.findPreference<Preference>("restore_on_boot")
            val multipleTunnels =
                preferenceManager.findPreference<Preference>("multiple_tunnels")

            val awgQuickOnlyPrefs =
                listOfNotNull(toolsInstaller, restoreOnBoot, multipleTunnels)

            awgQuickOnlyPrefs.forEach { it.isVisible = false }

            lifecycleScope.launch {
                if (Application.getBackend() is AwgQuickBackend) {
                    ++preferenceScreen.initialExpandedChildrenCount
                    awgQuickOnlyPrefs.forEach { it.isVisible = true }
                } else {
                    awgQuickOnlyPrefs.forEach { preference ->
                        preference.parent?.removePreference(preference)
                    }
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
