package com.quranmedia.player.presentation.screens.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Qibla
import com.quranmedia.player.data.repository.SettingsRepository
import com.quranmedia.player.data.repository.UserSettings
import com.quranmedia.player.data.sensor.CompassSensorManager
import com.quranmedia.player.domain.model.UserLocation
import com.quranmedia.player.domain.repository.PrayerTimesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

data class QiblaUiState(
    val qiblaBearing: Float = 0f,
    val deviceAzimuth: Float = 0f,
    val location: UserLocation? = null,
    val distanceToMakkahKm: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val needsCalibration: Boolean = false,
    val isSensorAvailable: Boolean = true
)

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
    private val compassSensorManager: CompassSensorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    init {
        loadLocation()
        observeCompass()
    }

    private fun loadLocation() {
        viewModelScope.launch {
            prayerTimesRepository.getSavedLocation().collectLatest { location ->
                if (location != null) {
                    val coordinates = Coordinates(location.latitude, location.longitude)
                    val qiblaBearing = Qibla(coordinates).direction.toFloat()
                    val distance = haversineDistance(
                        location.latitude, location.longitude,
                        MAKKAH_LAT, MAKKAH_LNG
                    )
                    _uiState.value = _uiState.value.copy(
                        location = location,
                        qiblaBearing = qiblaBearing,
                        distanceToMakkahKm = distance,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "no_location"
                    )
                }
            }
        }
    }

    private fun observeCompass() {
        viewModelScope.launch {
            compassSensorManager.compassData.collectLatest { data ->
                _uiState.value = _uiState.value.copy(
                    deviceAzimuth = data.azimuth,
                    isSensorAvailable = data.isAvailable,
                    needsCalibration = data.accuracy <= android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW
                )
            }
        }
    }

    fun startCompass() {
        compassSensorManager.start()
    }

    fun stopCompass() {
        compassSensorManager.stop()
    }

    companion object {
        private const val MAKKAH_LAT = 21.4225
        private const val MAKKAH_LNG = 39.8262

        fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6371.0 // Earth radius in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLng / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
