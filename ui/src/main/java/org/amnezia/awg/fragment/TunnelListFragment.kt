package org.amnezia.awg.fragment

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.qrcode.QRCodeReader
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.amnezia.awg.Application
import org.amnezia.awg.R
import org.amnezia.awg.activity.SettingsActivity
import org.amnezia.awg.activity.TunnelCreatorActivity
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.databinding.ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler
import org.amnezia.awg.databinding.TunnelListFragmentBinding
import org.amnezia.awg.databinding.TunnelListItemBinding
import org.amnezia.awg.model.ObservableTunnel
import org.amnezia.awg.util.ErrorMessages
import org.amnezia.awg.util.QrCodeFromFileScanner
import org.amnezia.awg.util.TunnelImporter
import org.amnezia.awg.viewmodel.ConfigProxy
import org.amnezia.awg.widget.CifVpnDashboardView
import org.amnezia.awg.widget.MultiselectableRelativeLayout

class TunnelListFragment : BaseFragment() {
    private val actionModeListener = ActionModeListener()
    private var actionMode: ActionMode? = null
    private var backPressedCallback: OnBackPressedCallback? = null
    private var binding: TunnelListFragmentBinding? = null
    private val dashboardJobs = mutableMapOf<String, Job>()
    private val sessionStartedAt = mutableMapOf<String, Long>()

    private val tunnelFileImportResultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { data ->
            if (data == null) return@registerForActivityResult
            val activity = activity ?: return@registerForActivityResult
            val resolver = activity.contentResolver
            activity.lifecycleScope.launch {
                if (QrCodeFromFileScanner.validContentType(resolver, data)) {
                    try {
                        val scanner = QrCodeFromFileScanner(resolver, QRCodeReader())
                        TunnelImporter.importTunnel(parentFragmentManager, scanner.scan(data).text) { showSnackbar(it) }
                    } catch (e: Exception) {
                        val message = Application.get().resources.getString(R.string.import_error, ErrorMessages[e])
                        Log.e(TAG, message, e)
                        showSnackbar(message)
                    }
                } else {
                    TunnelImporter.importTunnel(resolver, data) { showSnackbar(it) }
                }
            }
        }

    private val qrImportResultLauncher = registerForActivityResult(ScanContract()) { result ->
        val qr = result.contents
        val activity = activity
        if (qr != null && activity != null) {
            activity.lifecycleScope.launch {
                TunnelImporter.importTunnel(parentFragmentManager, qr) { showSnackbar(it) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        super.onCreateView(inflater, container, state)
        binding = TunnelListFragmentBinding.inflate(inflater, container, false)
        val sheet = AddTunnelsSheet()
        binding?.createFab?.setOnClickListener {
            if (childFragmentManager.findFragmentByTag("BOTTOM_SHEET") != null) return@setOnClickListener
            childFragmentManager.setFragmentResultListener(
                AddTunnelsSheet.REQUEST_KEY_NEW_TUNNEL,
                viewLifecycleOwner
            ) { _, bundle ->
                when (bundle.getString(AddTunnelsSheet.REQUEST_METHOD)) {
                    AddTunnelsSheet.REQUEST_CREATE ->
                        startActivity(Intent(requireActivity(), TunnelCreatorActivity::class.java))
                    AddTunnelsSheet.REQUEST_IMPORT -> tunnelFileImportResultLauncher.launch("*/*")
                    AddTunnelsSheet.REQUEST_SCAN -> qrImportResultLauncher.launch(
                        ScanOptions()
                            .setOrientationLocked(false)
                            .setBeepEnabled(false)
                            .setPrompt(getString(R.string.qr_code_hint))
                    )
                }
            }
            sheet.showNow(childFragmentManager, "BOTTOM_SHEET")
        }
        backPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(this) { actionMode?.finish() }
        backPressedCallback?.isEnabled = false
        return binding?.root
    }

    override fun onDestroyView() {
        dashboardJobs.values.forEach { it.cancel() }
        dashboardJobs.clear()
        binding = null
        super.onDestroyView()
    }

    override fun onSelectedTunnelChanged(oldTunnel: ObservableTunnel?, newTunnel: ObservableTunnel?) {
        binding ?: return
        lifecycleScope.launch {
            val tunnels = Application.getTunnelManager().getTunnels()
            if (newTunnel != null) viewForTunnel(newTunnel, tunnels)?.setSingleSelected(true)
            if (oldTunnel != null) viewForTunnel(oldTunnel, tunnels)?.setSingleSelected(false)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding ?: return
        binding!!.fragment = this
        lifecycleScope.launch { binding!!.tunnels = Application.getTunnelManager().getTunnels() }
        binding!!.rowConfigurationHandler =
            object : RowConfigurationHandler<TunnelListItemBinding, ObservableTunnel> {
                override fun onConfigureRow(row: TunnelListItemBinding, item: ObservableTunnel, position: Int) {
                    row.fragment = this@TunnelListFragment
                    row.root.setOnLongClickListener {
                        actionModeListener.toggleItemChecked(position)
                        true
                    }

                    row.dashboardView.onMenuClick = {
                        if (actionMode == null) {
                            startActivity(Intent(requireContext(), SettingsActivity::class.java))
                        } else {
                            actionModeListener.toggleItemChecked(position)
                        }
                    }
                    row.dashboardView.onHeaderSettingsClick = {
                        if (actionMode == null) {
                            showAccentPicker(row.dashboardView)
                        } else {
                            actionModeListener.toggleItemChecked(position)
                        }
                    }

                    row.dashboardView.onPowerClick = {
                        if (actionMode != null) {
                            actionModeListener.toggleItemChecked(position)
                        } else {
                            setTunnelState(row.root, item.state != Tunnel.State.UP)
                        }
                    }
                    row.dashboardView.onWhitelistClick = {
                        if (actionMode == null) showWhitelist(item) else actionModeListener.toggleItemChecked(position)
                    }
                    row.dashboardView.onImportClick = {
                        if (actionMode == null) binding?.createFab?.performClick() else actionModeListener.toggleItemChecked(position)
                    }
                    row.dashboardView.onSettingsClick = {
                        if (actionMode == null) showAccentPicker(row.dashboardView) else actionModeListener.toggleItemChecked(position)
                    }

                    startDashboardUpdates(row.dashboardView, item)

                    if (actionMode != null)
                        (row.root as MultiselectableRelativeLayout).setMultiSelected(actionModeListener.checkedItems.contains(position))
                    else
                        (row.root as MultiselectableRelativeLayout).setSingleSelected(selectedTunnel == item)
                }
            }
    }

    private fun startDashboardUpdates(view: CifVpnDashboardView, item: ObservableTunnel) {
        dashboardJobs.remove(item.name)?.cancel()
        dashboardJobs[item.name] = viewLifecycleOwner.lifecycleScope.launch {
            var lastBytes = 0L
            var lastAt = SystemClock.elapsedRealtime()
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val isUp = item.state == Tunnel.State.UP
                val uiState = when {
                    item.connectionStatus == ObservableTunnel.ConnectionStatus.CONNECTING ->
                        CifVpnDashboardView.UiState.CONNECTING
                    isUp -> CifVpnDashboardView.UiState.ON
                    else -> CifVpnDashboardView.UiState.OFF
                }

                if (isUp && sessionStartedAt[item.name] == null) sessionStartedAt[item.name] = now
                if (!isUp) sessionStartedAt.remove(item.name)

                var speed = 0.0
                if (isUp) {
                    try {
                        val stats = item.getStatisticsAsync()
                        val bytes = stats.totalRx() + stats.totalTx()
                        val elapsedMs = (now - lastAt).coerceAtLeast(1L)
                        if (lastBytes > 0L && bytes >= lastBytes) {
                            speed = (bytes - lastBytes).toDouble() * 8.0 / elapsedMs.toDouble() / 1000.0
                        }
                        lastBytes = bytes
                        lastAt = now
                    } catch (e: Throwable) {
                        Log.d(TAG, "Statistics update skipped", e)
                    }
                } else {
                    lastBytes = 0L
                    lastAt = now
                }

                val bypassCount = try {
                    item.getConfigAsync().`interface`.excludedApplications.size
                } catch (_: Throwable) {
                    0
                }

                val accent = requireContext()
                    .getSharedPreferences(PREFS_UI, 0)
                    .getInt(KEY_ACCENT, DEFAULT_ACCENT)

                view.update(
                    uiState = uiState,
                    profile = item.name,
                    speed = speed,
                    elapsedSeconds = sessionStartedAt[item.name]?.let { (now - it) / 1000L } ?: 0L,
                    bypassCount = bypassCount,
                    accent = accent
                )
                delay(1000L)
            }
        }
    }

    private fun showWhitelist(item: ObservableTunnel) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val installed = requireContext().packageManager
                    .getInstalledApplications(0)
                    .map { it.packageName }
                    .toSet()

                val currentConfig = item.getConfigAsync()
                val selected = LinkedHashSet<String>()
                selected.addAll(currentConfig.`interface`.excludedApplications)
                selected.addAll(SMART_BYPASS_PACKAGES.filter { installed.contains(it) })

                val dialog = AppListDialogFragment.newInstance(ArrayList(selected), true)
                childFragmentManager.setFragmentResultListener(
                    AppListDialogFragment.REQUEST_SELECTION,
                    viewLifecycleOwner
                ) { _, result ->
                    val packages = result.getStringArray(AppListDialogFragment.KEY_SELECTED_APPS) ?: emptyArray()
                    val excluded = result.getBoolean(AppListDialogFragment.KEY_IS_EXCLUDED, true)
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val proxy = ConfigProxy(item.getConfigAsync())
                            if (excluded) {
                                proxy.`interface`.includedApplications.clear()
                                proxy.`interface`.excludedApplications.clear()
                                proxy.`interface`.excludedApplications.addAll(packages)
                            } else {
                                proxy.`interface`.excludedApplications.clear()
                                proxy.`interface`.includedApplications.clear()
                                proxy.`interface`.includedApplications.addAll(packages)
                            }
                            item.setConfigAsync(proxy.resolve())
                            showSnackbar(getString(R.string.cif_bypass_saved))
                        } catch (e: Throwable) {
                            showSnackbar(getString(R.string.cif_bypass_error, ErrorMessages[e]))
                        }
                    }
                }
                dialog.show(childFragmentManager, "CIF_BYPASS")
            } catch (e: Throwable) {
                showSnackbar(getString(R.string.cif_bypass_error, ErrorMessages[e]))
            }
        }
    }

    private fun showAccentPicker(view: CifVpnDashboardView) {
        val names = arrayOf(
            "Бирюзовый", "Синий", "Фиолетовый",
            "Зелёный", "Оранжевый", "Розовый"
        )
        val colors = intArrayOf(
            Color.rgb(33, 229, 197),
            Color.rgb(46, 144, 255),
            Color.rgb(147, 86, 255),
            Color.rgb(57, 214, 120),
            Color.rgb(255, 153, 51),
            Color.rgb(255, 79, 154)
        )
        val prefs = requireContext().getSharedPreferences(PREFS_UI, 0)
        val current = prefs.getInt(KEY_ACCENT, DEFAULT_ACCENT)
        var checked = colors.indexOf(current).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cif_color_title)
            .setSingleChoiceItems(names, checked) { _, which -> checked = which }
            .setPositiveButton(R.string.cif_apply) { _, _ ->
                prefs.edit().putInt(KEY_ACCENT, colors[checked]).apply()
                view.invalidate()
                binding?.tunnelList?.adapter?.notifyDataSetChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onTunnelDeletionFinished(count: Int, throwable: Throwable?) {
        val ctx = activity ?: Application.get()
        val message = if (throwable == null) {
            ctx.resources.getQuantityString(R.plurals.delete_success, count, count)
        } else {
            ctx.resources.getQuantityString(R.plurals.delete_error, count, count, ErrorMessages[throwable])
        }
        showSnackbar(message)
    }

    private fun showSnackbar(message: CharSequence) {
        val b = binding
        if (b != null) Snackbar.make(b.mainContainer, message, Snackbar.LENGTH_LONG).setAnchorView(b.createFab).show()
        else Toast.makeText(activity ?: Application.get(), message, Toast.LENGTH_SHORT).show()
    }

    private fun viewForTunnel(tunnel: ObservableTunnel, tunnels: List<*>): MultiselectableRelativeLayout? =
        binding?.tunnelList
            ?.findViewHolderForAdapterPosition(tunnels.indexOf(tunnel))
            ?.itemView as? MultiselectableRelativeLayout

    private inner class ActionModeListener : ActionMode.Callback {
        val checkedItems: MutableCollection<Int> = HashSet()
        private var resources: Resources? = null

        fun getCheckedItems(): ArrayList<Int> = ArrayList(checkedItems)
        fun toggleItemChecked(position: Int) = setItemChecked(position, !checkedItems.contains(position))

        fun setItemChecked(position: Int, checked: Boolean) {
            if (checked) checkedItems.add(position) else checkedItems.remove(position)
            val adapter = binding?.tunnelList?.adapter
            if (actionMode == null && checkedItems.isNotEmpty() && activity != null)
                (activity as AppCompatActivity).startSupportActionMode(this)
            else if (actionMode != null && checkedItems.isEmpty())
                actionMode!!.finish()
            adapter?.notifyItemChanged(position)
            updateTitle(actionMode)
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            backPressedCallback?.isEnabled = true
            resources = activity?.resources
            mode.menuInflater.inflate(R.menu.tunnel_list_action_mode, menu)
            binding?.tunnelList?.adapter?.notifyDataSetChanged()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            backPressedCallback?.isEnabled = false
            resources = null
            checkedItems.clear()
            binding?.tunnelList?.adapter?.notifyDataSetChanged()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_action_delete -> {
                    val checked = HashSet(checkedItems)
                    lifecycleScope.launch {
                        try {
                            val tunnels = Application.getTunnelManager().getTunnels()
                            val jobs = checked.map { async(SupervisorJob()) { tunnels[it].deleteAsync() } }
                            onTunnelDeletionFinished(jobs.awaitAll().size, null)
                        } catch (e: Throwable) {
                            onTunnelDeletionFinished(0, e)
                        }
                    }
                    checkedItems.clear()
                    mode.finish()
                    true
                }
                R.id.menu_action_select_all -> {
                    lifecycleScope.launch {
                        val tunnels = Application.getTunnelManager().getTunnels()
                        for (i in 0 until tunnels.size) setItemChecked(i, true)
                    }
                    true
                }
                else -> false
            }
        }

        private fun updateTitle(mode: ActionMode?) {
            if (mode == null || resources == null) return
            val count = checkedItems.size
            mode.title = if (count == 0) "" else resources!!.getQuantityString(R.plurals.delete_title, count, count)
        }
    }

    companion object {
        private const val TAG = "CifVPN/Dashboard"
        private const val PREFS_UI = "cif_ui"
        private const val KEY_ACCENT = "accent_color"
        private val DEFAULT_ACCENT = Color.rgb(33, 229, 197)

        private val SMART_BYPASS_PACKAGES = setOf(
            "ru.sberbankmobile",
            "com.idamob.tinkoff.android",
            "ru.vtb24.mobilebanking.android",
            "ru.alfabank.mobile.android",
            "ru.gazprombank.android.mobilebank.app",
            "ru.mtsbank.mobile",
            "ru.gosuslugi.mobile",
            "ru.max.android",
            "ru.vk.android",
            "ru.ok.android",
            "ru.mail.mailapp",
            "ru.ozon.app.android",
            "ru.wildberries",
            "ru.yandex.market",
            "com.avito.android",
            "ru.yandex.yandexmaps",
            "ru.yandex.taxi",
            "ru.yandex.searchplugin"
        )
    }
}
