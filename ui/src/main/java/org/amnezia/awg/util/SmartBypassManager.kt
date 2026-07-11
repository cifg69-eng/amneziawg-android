package org.amnezia.awg.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.amnezia.awg.model.ObservableTunnel
import org.amnezia.awg.viewmodel.ConfigProxy
import java.util.Locale

/**
 * Applies a real Android app-level split-tunneling preset.
 * Only installed and visible applications are added to ExcludedApplications.
 */
object SmartBypassManager {

    data class Candidate(val packageName: String, val label: String)
    data class ApplyResult(val added: Int, val totalExcluded: Int)

    private val exactPackages = setOf(
        "ru.sberbankmobile",
        "com.idamob.tinkoff.android",
        "ru.vtb24.mobilebanking.android",
        "ru.alfabank.mobile.android",
        "ru.gazprombank.android.mobilebank.app",
        "com.raiffeisennews",
        "ru.mtsbank.mobile",
        "ru.pochtabank.mobile",
        "ru.sovcomcard.halva",
        "ru.mkb.mobile",
        "ru.rshb.dbo",
        "ru.rosbank.android",
        "ru.gosuslugi.mobile",
        "ru.gosuslugi.pos",
        "ru.oneme.app",
        "ru.vk.android",
        "ru.ok.android",
        "ru.mail.mailapp",
        "ru.ozon.app.android",
        "ru.wildberries",
        "ru.yandex.market",
        "com.avito.android",
        "ru.yandex.searchplugin",
        "ru.yandex.yandexmaps",
        "ru.yandex.taxi",
        "ru.yandex.disk",
        "ru.yandex.mail",
        "ru.mts.mymts",
        "ru.beeline.services",
        "ru.megafon.mlk",
        "ru.tele2.mytele2",
        "ru.rt.client"
    )

    private val labelKeywords = listOf(
        "сбер", "sber", "т-банк", "t-bank", "tinkoff", "тинькофф",
        "втб", "альфа-банк", "alfabank", "газпромбанк", "райффайзен",
        "мтс банк", "почта банк", "совкомбанк", "халва", "мкб",
        "россельхозбанк", "росбанк", "госуслуги", "госключ",
        "max", "макс", "вконтакте", "vk", "одноклассники", "mail.ru",
        "ozon", "озон", "wildberries", "вайлдберриз", "яндекс маркет",
        "avito", "авито", "яндекс карты", "яндекс go", "яндекс такси",
        "мой мтс", "билайн", "мегафон", "tele2", "t2", "ростелеком"
    )

    fun findCandidates(context: Context): List<Candidate> {
        val pm = context.packageManager
        return getInternetPackages(pm)
            .asSequence()
            .mapNotNull { info ->
                val packageName = info.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                val label = info.applicationInfo?.loadLabel(pm)?.toString()?.trim().orEmpty()
                val normalized = label.lowercase(Locale.ROOT)
                val matches = packageName in exactPackages || labelKeywords.any { normalized.contains(it) }
                if (matches) Candidate(packageName, label.ifBlank { packageName }) else null
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.ROOT) }
            .toList()
    }

    suspend fun applyPreset(tunnel: ObservableTunnel, packages: Collection<String>): ApplyResult {
        val proxy = ConfigProxy(tunnel.getConfigAsync())
        val existing = proxy.`interface`.excludedApplications.toMutableSet()
        val before = existing.size
        existing.addAll(packages)

        proxy.`interface`.includedApplications.clear()
        proxy.`interface`.excludedApplications.clear()
        proxy.`interface`.excludedApplications.addAll(existing.sorted())

        tunnel.setConfigAsync(proxy.resolve())
        return ApplyResult(existing.size - before, existing.size)
    }

    suspend fun saveManualSelection(
        tunnel: ObservableTunnel,
        selectedPackages: Collection<String>,
        excludedMode: Boolean
    ) {
        val proxy = ConfigProxy(tunnel.getConfigAsync())
        if (excludedMode) {
            proxy.`interface`.includedApplications.clear()
            proxy.`interface`.excludedApplications.clear()
            proxy.`interface`.excludedApplications.addAll(selectedPackages.sorted())
        } else {
            proxy.`interface`.excludedApplications.clear()
            proxy.`interface`.includedApplications.clear()
            proxy.`interface`.includedApplications.addAll(selectedPackages.sorted())
        }
        tunnel.setConfigAsync(proxy.resolve())
    }

    private fun getInternetPackages(pm: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackagesHoldingPermissions(
                arrayOf(Manifest.permission.INTERNET),
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackagesHoldingPermissions(arrayOf(Manifest.permission.INTERNET), 0)
        }
    }
}
