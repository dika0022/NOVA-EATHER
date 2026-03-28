package com.aether.nova.phantom.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sprite_prefs")

data class SpritePosition(
    val x: Float = 0f,
    val y: Float = 0f
)

class SpritePrefs(private val context: Context) {

    companion object {
        private val AETHER_X = floatPreferencesKey("aether_x")
        private val AETHER_Y = floatPreferencesKey("aether_y")
        private val NOVA_X = floatPreferencesKey("nova_x")
        private val NOVA_Y = floatPreferencesKey("nova_y")
    }

    suspend fun saveAetherPosition(x: Float, y: Float) {
        context.dataStore.edit { prefs ->
            prefs[AETHER_X] = x
            prefs[AETHER_Y] = y
        }
    }

    suspend fun saveNovaPosition(x: Float, y: Float) {
        context.dataStore.edit { prefs ->
            prefs[NOVA_X] = x
            prefs[NOVA_Y] = y
        }
    }

    suspend fun getAetherPosition(): SpritePosition {
        val prefs = context.dataStore.data.first()
        return SpritePosition(
            x = prefs[AETHER_X] ?: -1f,
            y = prefs[AETHER_Y] ?: -1f
        )
    }

    suspend fun getNovaPosition(): SpritePosition {
        val prefs = context.dataStore.data.first()
        return SpritePosition(
            x = prefs[NOVA_X] ?: -1f,
            y = prefs[NOVA_Y] ?: -1f
        )
    }

    suspend fun savePositions(aetherX: Float, aetherY: Float, novaX: Float, novaY: Float) {
        context.dataStore.edit { prefs ->
            prefs[AETHER_X] = aetherX
            prefs[AETHER_Y] = aetherY
            prefs[NOVA_X] = novaX
            prefs[NOVA_Y] = novaY
        }
    }
}
