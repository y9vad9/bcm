@file:OptIn(ValidationDelicateApi::class)

package com.y9vad9.starix.data

import com.y9vad9.starix.core.brawlstars.entity.club.value.ClubTag
import com.y9vad9.starix.core.system.entity.ClubSettings
import com.y9vad9.starix.core.system.entity.Settings
import com.y9vad9.starix.core.system.repository.SettingsRepository
import com.y9vad9.starix.foundation.validation.annotations.ValidationDelicateApi
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

class SettingsRepositoryImpl(
    private val configFile: Path,
    private val json: Json = Json,
) : SettingsRepository {
    private val cache = Cache.Builder<Unit, Settings>()
        .expireAfterWrite(5.minutes)
        .build()

    override suspend fun getSettings(): Settings {
        return cache.get(Unit) {
            withContext(Dispatchers.IO) {
                json.decodeFromString<Settings>(configFile.readText())
            }
        }
    }

    override suspend fun setSettings(settings: Settings): Unit = withContext(Dispatchers.IO) {
        configFile.writeText(json.encodeToString(settings))
        cache.put(Unit, settings)
    }

    override suspend fun allowClub(
        tag: ClubTag,
        settings: ClubSettings,
    ): Boolean {
        return try {
            val globalSettings = getSettings()

            if (globalSettings.allowedClubs.containsKey(tag))
                return false

            setSettings(
                globalSettings.copy(
                    allowedClubs = globalSettings.allowedClubs.toMutableMap()
                        .apply {
                            put(tag, settings)
                        }
                        .toMap()
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
