package com.example.data.repository

import com.example.data.local.DataKopiStore
import com.example.data.local.KopiDao
import com.example.data.model.SyncDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SyncRepository(
    private val dao: KopiDao,
    private val dataKopiStore: DataKopiStore
) {
    private val _devices = MutableStateFlow<List<SyncDevice>>(
        listOf(
            SyncDevice(
                id = "dev_local_1",
                name = "Kopi Android (Это устройство)",
                type = "Android Phone (Chromium)",
                lastSyncTime = System.currentTimeMillis(),
                isCurrentDevice = true
            ),
            SyncDevice(
                id = "dev_pc_2",
                name = "Kopi Workstation PC",
                type = "Windows / Linux (Chromium)",
                lastSyncTime = System.currentTimeMillis() - 3600_000 * 4,
                isCurrentDevice = false
            ),
            SyncDevice(
                id = "dev_tab_3",
                name = "Kopi Tablet OLED",
                type = "Android Tablet",
                lastSyncTime = System.currentTimeMillis() - 3600_000 * 12,
                isCurrentDevice = false
            )
        )
    )
    val devices: StateFlow<List<SyncDevice>> = _devices.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun getSyncCode(): String = dataKopiStore.getSyncCode()

    fun getLastSyncTime(): Long = dataKopiStore.getLastSyncTime()

    suspend fun performSync(): Boolean {
        _isSyncing.value = true
        kotlinx.coroutines.delay(1200) // realistic sync animation
        val now = System.currentTimeMillis()
        dataKopiStore.updateLastSyncTime(now)

        _devices.value = _devices.value.map { dev ->
            if (dev.isCurrentDevice) dev.copy(lastSyncTime = now) else dev
        }
        _isSyncing.value = false
        return true
    }

    fun pairNewDevice(deviceCode: String, deviceName: String = "Новое устройство"): Boolean {
        if (deviceCode.isBlank()) return false
        val newDev = SyncDevice(
            id = "dev_" + UUID.randomUUID().toString().take(6),
            name = if (deviceName.isNotBlank()) deviceName else "Kopi Device ($deviceCode)",
            type = "Chromium Client",
            lastSyncTime = System.currentTimeMillis(),
            isCurrentDevice = false
        )
        _devices.value = _devices.value + newDev
        dataKopiStore.updateLastSyncTime()
        return true
    }

    fun removeDevice(deviceId: String) {
        _devices.value = _devices.value.filter { it.id != deviceId }
    }
}
