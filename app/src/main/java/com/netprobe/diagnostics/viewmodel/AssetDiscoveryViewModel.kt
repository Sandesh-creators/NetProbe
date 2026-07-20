package com.netprobe.diagnostics.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netprobe.diagnostics.NetProbeApp
import com.netprobe.diagnostics.data.db.entity.DiscoveredAssetEntity
import com.netprobe.diagnostics.service.AssetDiscoveryService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AssetDiscoveryUiState(
    val isRunning: Boolean = false,
    val totalCount: Int = 0,
    val flaggedCount: Int = 0
)

class AssetDiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val assetDao = (application as NetProbeApp).database.assetDao()

    private val _uiState = MutableStateFlow(AssetDiscoveryUiState())
    val uiState: StateFlow<AssetDiscoveryUiState> = _uiState.asStateFlow()

    val allAssets: StateFlow<List<DiscoveredAssetEntity>> = assetDao.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unknownAssets: StateFlow<List<DiscoveredAssetEntity>> = assetDao.getUnknownAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flaggedAssets: StateFlow<List<DiscoveredAssetEntity>> = assetDao.getFlaggedAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allAssets.collect { assets ->
                _uiState.update {
                    it.copy(totalCount = assets.size)
                }
            }
        }
        viewModelScope.launch {
            flaggedAssets.collect { flagged ->
                _uiState.update {
                    it.copy(flaggedCount = flagged.size)
                }
            }
        }
    }

    fun toggleDiscovery() {
        val context = getApplication<Application>()
        if (_uiState.value.isRunning) {
            val intent = Intent(context, AssetDiscoveryService::class.java).apply {
                action = AssetDiscoveryService.ACTION_STOP
            }
            context.startService(intent)
            _uiState.update { it.copy(isRunning = false) }
        } else {
            val intent = Intent(context, AssetDiscoveryService::class.java).apply {
                action = AssetDiscoveryService.ACTION_START
            }
            context.startForegroundService(intent)
            _uiState.update { it.copy(isRunning = true) }
        }
    }

    fun markKnown(mac: String) {
        viewModelScope.launch { assetDao.setKnown(mac, true) }
    }

    fun markUnknown(mac: String) {
        viewModelScope.launch { assetDao.setKnown(mac, false) }
    }

    fun toggleFlag(mac: String, flagged: Boolean) {
        viewModelScope.launch { assetDao.setFlagged(mac, flagged) }
    }

    fun setNotes(mac: String, notes: String?) {
        viewModelScope.launch { assetDao.setNotes(mac, notes) }
    }

    fun pruneOld(daysOld: Int = 30) {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - (daysOld.toLong() * 86400000)
            assetDao.pruneStale(cutoff)
        }
    }
}
