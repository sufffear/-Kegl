package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.MetronomeTickStyle
import com.example.domain.SoundPack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.velarisDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "velaris_preferences"
)

data class BenchSignalSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val metronomeEnabled: Boolean = true,
    val signalVolume: Float = 0.80f,
    val metronomeVolume: Float = 0.35f,
    val soundPack: SoundPack = SoundPack.SOFT,
    val metronomeTickStyle: MetronomeTickStyle = MetronomeTickStyle.SOFT
)

/**
 * Хранилище пользовательских настроек звука, вибрации, звукового набора и тика метронома в DataStore.
 */
class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.velarisDataStore)

    val signalSettingsFlow: Flow<BenchSignalSettings> = dataStore.data.map { prefs ->
        BenchSignalSettings(
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
            metronomeEnabled = prefs[KEY_METRONOME_ENABLED] ?: true,
            signalVolume = (prefs[KEY_SIGNAL_VOLUME] ?: 0.80f).coerceIn(0f, 1f),
            metronomeVolume = (prefs[KEY_METRONOME_VOLUME] ?: 0.35f).coerceIn(0f, 1f),
            soundPack = SoundPack.fromId(prefs[KEY_SOUND_PACK_ID]),
            metronomeTickStyle = MetronomeTickStyle.fromId(prefs[KEY_METRONOME_STYLE_ID])
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setMetronomeEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_METRONOME_ENABLED] = enabled }
    }

    suspend fun setSignalVolume(volume: Float) {
        dataStore.edit { it[KEY_SIGNAL_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setMetronomeVolume(volume: Float) {
        dataStore.edit { it[KEY_METRONOME_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSoundPack(pack: SoundPack) {
        dataStore.edit { it[KEY_SOUND_PACK_ID] = pack.id }
    }

    suspend fun setMetronomeTickStyle(style: MetronomeTickStyle) {
        dataStore.edit { it[KEY_METRONOME_STYLE_ID] = style.id }
    }

    suspend fun updateSettings(settings: BenchSignalSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_SOUND_ENABLED] = settings.soundEnabled
            prefs[KEY_VIBRATION_ENABLED] = settings.vibrationEnabled
            prefs[KEY_METRONOME_ENABLED] = settings.metronomeEnabled
            prefs[KEY_SIGNAL_VOLUME] = settings.signalVolume.coerceIn(0f, 1f)
            prefs[KEY_METRONOME_VOLUME] = settings.metronomeVolume.coerceIn(0f, 1f)
            prefs[KEY_SOUND_PACK_ID] = settings.soundPack.id
            prefs[KEY_METRONOME_STYLE_ID] = settings.metronomeTickStyle.id
        }
    }

    companion object {
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val KEY_METRONOME_ENABLED = booleanPreferencesKey("metronome_enabled")
        private val KEY_SIGNAL_VOLUME = floatPreferencesKey("signal_volume")
        private val KEY_METRONOME_VOLUME = floatPreferencesKey("metronome_volume")
        private val KEY_SOUND_PACK_ID = stringPreferencesKey("sound_pack_id")
        private val KEY_METRONOME_STYLE_ID = stringPreferencesKey("metronome_tick_style_id")
    }
}
