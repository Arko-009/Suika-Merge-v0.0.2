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

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var totalMerges: Int
        get() = prefs.getInt(KEY_TOTAL_MERGES, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_MERGES, value).apply()

    companion object {
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_HIGHEST_LEVEL = "highest_level"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_TOTAL_MERGES = "total_merges"
    }
}

