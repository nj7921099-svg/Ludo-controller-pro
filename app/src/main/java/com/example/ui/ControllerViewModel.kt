package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.controller.BoxChannelState
import com.example.controller.ControllerRepository
import com.example.controller.ProtocolLogEntry
import com.example.controller.settings.ControllerSettings
import com.example.controller.settings.ControllerSettingsManager
import com.example.futureludo.FutureLudoGameState
import com.example.network.ConnectionDiagnostics
import com.example.network.ConnectionState
import com.example.network.DiagnosticLogItem
import com.example.network.DiscoveredLudoHost
import com.example.network.WifiConnectionManager
import kotlinx.coroutines.flow.StateFlow

class ControllerViewModel(application: Application) : AndroidViewModel(application) {

    val wifiManager = WifiConnectionManager(application, viewModelScope)
    val repository = ControllerRepository(wifiManager, viewModelScope)
    val settingsManager = ControllerSettingsManager(application)

    // Observable states
    val connectionState: StateFlow<ConnectionState> = wifiManager.connectionState
    val diagnostics: StateFlow<ConnectionDiagnostics> = wifiManager.diagnostics
    val diagnosticLogs: StateFlow<List<DiagnosticLogItem>> = wifiManager.diagnosticLogs
    val discoveredHosts: StateFlow<List<DiscoveredLudoHost>> = wifiManager.discoveredHosts
    val numberTestState = wifiManager.numberTestState

    val boxCount: StateFlow<Int> = repository.boxCount
    val boxes: StateFlow<Map<String, BoxChannelState>> = repository.boxes
    val selectedBoxForInput: StateFlow<String?> = repository.selectedBoxForInput
    val lastSentInfo: StateFlow<Pair<String, Int>?> = repository.lastSentInfo
    val lastReceivedInfo: StateFlow<String?> = repository.lastReceivedInfo
    val lastResultStatus: StateFlow<String> = repository.lastResultStatus
    val logs: StateFlow<List<ProtocolLogEntry>> = repository.logs
    val systemAlert: StateFlow<String?> = repository.systemAlert
    val isSimulationMode: StateFlow<Boolean> = repository.isSimulationMode
    val futureLudoState: StateFlow<FutureLudoGameState> = repository.futureLudoState
    val settings: StateFlow<ControllerSettings> = settingsManager.settings

    // Settings Actions
    fun setHapticEnabled(enabled: Boolean) = settingsManager.setHapticEnabled(enabled)
    fun setSoundEffectsEnabled(enabled: Boolean) = settingsManager.setSoundEffectsEnabled(enabled)
    fun setAnimationsEnabled(enabled: Boolean) = settingsManager.setAnimationsEnabled(enabled)
    fun setRememberLastMode(enabled: Boolean) = settingsManager.setRememberLastMode(enabled)
    fun setLastMode(mode: String) = settingsManager.setLastMode(mode)

    // Network & Connection Actions
    fun openHotspotSettings() = wifiManager.openHotspotSettings()

    fun refreshNetworkStatus() = wifiManager.refreshNetworkStatus()

    fun startDiscovery() = wifiManager.startDiscovery()

    fun stopDiscovery() = wifiManager.stopDiscovery()

    fun connectToHost(host: DiscoveredLudoHost) = wifiManager.connectToHost(host)

    fun connectWithManualIp(ip: String, port: Int) = wifiManager.connectWithManualIp(ip, port)

    fun testConnection() = wifiManager.testConnection()

    fun sendNumberTest(number: Int) = wifiManager.sendNumberTest(number)

    fun disconnect() = wifiManager.disconnect()

    fun clearDiagnosticsLog() = wifiManager.clearDiagnosticsLog()

    // Controller Box Actions
    fun onBoxTap(controllerId: String) = repository.onBoxTap(controllerId)

    fun sendNumberForBox(controllerId: String, number: Int) = repository.sendNumberForBox(controllerId, number)

    fun closeNumberPanel() = repository.closeNumberPanel()

    fun requestSyncConfig() = repository.requestSyncConfig()

    fun setSimulationBoxCount(count: Int) = repository.applyBoxCountFromApp1(count, "Simulation Picker")

    fun simulateApp1FallbackRoll(boxId: String? = null) = repository.simulateApp1FallbackRoll(boxId)

    fun toggleSimulationMode(enabled: Boolean) = repository.toggleSimulationMode(enabled)

    fun dismissAlert() = repository.dismissAlert()

    fun clearLogs() = repository.clearLogs()

    override fun onCleared() {
        super.onCleared()
        wifiManager.destroy()
    }
}
