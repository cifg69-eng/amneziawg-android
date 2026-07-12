/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.amnezia.awg.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.google.android.material.appbar.MaterialToolbar
import org.amnezia.awg.R
import org.amnezia.awg.fragment.TunnelDetailFragment
import org.amnezia.awg.fragment.TunnelEditorFragment
import org.amnezia.awg.model.ObservableTunnel

/** Main application activity. The VPN engine is not modified here. */
class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {
    private var actionBar: ActionBar? = null
    private lateinit var toolbar: MaterialToolbar
    private var isTwoPaneLayout = false
    private var backPressedCallback: OnBackPressedCallback? = null

    private fun handleBackPressed() {
        val backStackEntries = supportFragmentManager.backStackEntryCount
        if (isTwoPaneLayout && backStackEntries <= 1) {
            finish()
            return
        }
        if (backStackEntries >= 1) supportFragmentManager.popBackStack()
        if (backStackEntries == 1) selectedTunnel = null
    }

    override fun onBackStackChanged() {
        val entries = supportFragmentManager.backStackEntryCount
        backPressedCallback?.isEnabled = entries >= 1

        if (entries == 0) {
            // Product dashboard owns its header. GONE means no hidden ActionBar gap.
            toolbar.visibility = View.GONE
            actionBar?.setDisplayHomeAsUpEnabled(false)
        } else {
            toolbar.visibility = View.VISIBLE
            actionBar?.title = getString(R.string.cif_app_name)
            val minimumEntries = if (isTwoPaneLayout) 2 else 1
            actionBar?.setDisplayHomeAsUpEnabled(entries >= minimumEntries)
        }
        invalidateOptionsMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(toolbar)
        actionBar = supportActionBar

        isTwoPaneLayout = findViewById<View?>(R.id.master_detail_wrapper) != null
        supportFragmentManager.addOnBackStackChangedListener(this)
        backPressedCallback = onBackPressedDispatcher.addCallback(this) { handleBackPressed() }
        onBackStackChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (supportFragmentManager.backStackEntryCount == 0) return false
        menuInflater.inflate(R.menu.main_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.menu_action_edit -> {
                supportFragmentManager.commit {
                    replace(R.id.detail_container, TunnelEditorFragment())
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    addToBackStack(null)
                }
                true
            }
            R.id.menu_action_save -> false
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSelectedTunnelChanged(
        oldTunnel: ObservableTunnel?,
        newTunnel: ObservableTunnel?
    ): Boolean {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.isStateSaved) return false

        val entries = fragmentManager.backStackEntryCount
        if (newTunnel == null) {
            fragmentManager.popBackStackImmediate(0, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            return true
        }
        if (entries == 2) {
            fragmentManager.popBackStackImmediate()
        } else if (entries == 0) {
            fragmentManager.commit {
                add(R.id.detail_container, TunnelDetailFragment())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                addToBackStack(null)
            }
        }
        return true
    }
}
