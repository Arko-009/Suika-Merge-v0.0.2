package com.suika.catmerge.game.data

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cat_merge_prefs", Context.MODE_PRIVATE)

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) {
            if (value > highScore) {
                prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()
            }
        }

    var highestLevelUnlocked: Int
        get() = prefs.getInt(KEY_HIGHEST_LEVEL, 0)
        set(value) {
            if (value > highestLevelUnlocked) {
                prefs.edit().putInt(KEY_HIGHEST_LEVEL, value).apply()
            }
        }

    var selectedShape: String
        get() = prefs.getString(KEY_SELECTED_SHAPE, "FLASK") ?: "FLASK"
        set(value) = prefs.edit().putString(KEY_SELECTED_SHAPE, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isTiltEnabled: Boolean
        get() = prefs.getBoolean(KEY_TILT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TILT_ENABLED, value).apply()

    var totalMerges: Int
        get() = prefs.getInt(KEY_TOTAL_MERGES, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_MERGES, value).apply()

    var longestCombo: Int
        get() = prefs.getInt(KEY_LONGEST_COMBO, 1)
        set(value) {
            if (value > longestCombo) {
                prefs.edit().putInt(KEY_LONGEST_COMBO, value).apply()
            }
        }

    var gamesPlayed: Int
        get() = prefs.getInt(KEY_GAMES_PLAYED, 0)
        set(value) = prefs.edit().putInt(KEY_GAMES_PLAYED, value).apply()

    fun isCatUnlocked(level: Int): Boolean {
        if (level == 0) return true
        return level <= highestLevelUnlocked
    }

    companion object {
        private const val KEY_SELECTED_SHAPE = "selected_shape"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_HIGHEST_LEVEL = "highest_level"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_TILT_ENABLED = "tilt_enabled"
        private const val KEY_TOTAL_MERGES = "total_merges"
        private const val KEY_LONGEST_COMBO = "longest_combo"
        private const val KEY_GAMES_PLAYED = "games_played"
    }
}
