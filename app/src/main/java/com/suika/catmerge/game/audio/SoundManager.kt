package com.suika.catmerge.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.suika.catmerge.R

/**
 * Manages low-latency audio playback for all 14 level sounds and SFX using SoundPool.
 */
class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val levelSoundIds = IntArray(14)
    private val loadedSoundIds = HashSet<Int>()

    var isSoundEnabled: Boolean = true

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            }
        }

        loadAllSounds()
    }

    private fun loadAllSounds() {
        val rawResIds = intArrayOf(
            R.raw.level_0,
            R.raw.level_1,
            R.raw.level_2,
            R.raw.level_3,
            R.raw.level_4,
            R.raw.level_5,
            R.raw.level_6,
            R.raw.level_7,
            R.raw.level_8,
            R.raw.level_9,
            R.raw.level_10,
            R.raw.level_11,
            R.raw.level_12,
            R.raw.level_13
        )

        for (i in rawResIds.indices) {
            try {
                levelSoundIds[i] = soundPool.load(context, rawResIds[i], 1)
            } catch (e: Exception) {
                Log.e("SoundManager", "Failed to load sound for level $i", e)
            }
        }
    }

    /**
     * Plays the sound for the given level (0..13) when balls merge or appear.
     */
    fun playLevelSound(level: Int, volume: Float = 1.0f) {
        if (!isSoundEnabled) return
        val idx = level.coerceIn(0, levelSoundIds.size - 1)
        val soundId = levelSoundIds[idx]
        if (loadedSoundIds.contains(soundId)) {
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
        }
    }

    /**
     * Subtle drop / release sound.
     */
    fun playDropSound() {
        if (!isSoundEnabled) return
        // Use level 0 with slightly higher pitch and lower volume
        val soundId = levelSoundIds[0]
        if (loadedSoundIds.contains(soundId)) {
            soundPool.play(soundId, 0.45f, 0.45f, 0, 0, 1.4f)
        }
    }

    /**
     * Bounce sound against flask wall.
     */
    fun playBounceSound(intensity: Float = 0.3f) {
        if (!isSoundEnabled) return
        val soundId = levelSoundIds[0]
        if (loadedSoundIds.contains(soundId)) {
            val vol = (intensity * 0.4f).coerceIn(0.1f, 0.4f)
            soundPool.play(soundId, vol, vol, 0, 0, 1.8f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
