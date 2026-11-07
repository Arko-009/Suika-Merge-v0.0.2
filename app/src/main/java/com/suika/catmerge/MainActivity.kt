package com.suika.catmerge

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.suika.catmerge.databinding.ActivityMainBinding
import com.suika.catmerge.game.GameEngine
import com.suika.catmerge.game.audio.SoundManager
import com.suika.catmerge.game.data.GamePreferences
import com.suika.catmerge.game.effects.ParticleSystem
import com.suika.catmerge.game.physics.FlaskContainer
import com.suika.catmerge.game.physics.PhysicsEngine

class MainActivity : AppCompatActivity(), GameEngine.GameEngineListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var prefs: GamePreferences
    private lateinit var soundManager: SoundManager
    private lateinit var flask: FlaskContainer
    private lateinit var physics: PhysicsEngine
    private lateinit var particleSystem: ParticleSystem
    private lateinit var engine: GameEngine

    private var vibrator: Vibrator? = null

    private val catNames = arrayOf(
        "Level 0: Bonnet Kitten",
        "Level 1: Golden Kitten",
        "Level 2: Striped Tabby",
        "Level 3: Polite Tuxedo Cat",
        "Level 4: Smug White Cat",
        "Level 5: Squinty Grey Cat",
        "Level 6: Smiling Void Cat",
        "Level 7: Ginger Chad Cat",
        "Level 8: Tuxedo Chad Cat",
        "Level 9: White Chad Cat",
        "Level 10: Grey Chad Cat",
        "Level 11: Bearded Guru Cat",
        "Level 12: Tabby Chad Boss",
        "Level 13: ULTRA BUFF BODYBUILDER CAT"
    )

    private val ballDrawables = intArrayOf(
        R.drawable.ball_0,
        R.drawable.ball_1,
        R.drawable.ball_2,
        R.drawable.ball_3,
        R.drawable.ball_4,
        R.drawable.ball_5,
        R.drawable.ball_6,
        R.drawable.ball_7,
        R.drawable.ball_8,
        R.drawable.ball_9,
        R.drawable.ball_10,
        R.drawable.ball_11,
        R.drawable.ball_12,
        R.drawable.ball_13
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive sticky fullscreen mode
        enableFullscreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initVibrator()
        initGame()
        setupListeners()
    }

    private fun enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun initGame() {
        prefs = GamePreferences(this)
        soundManager = SoundManager(this).apply {
            isSoundEnabled = prefs.isSoundEnabled
        }
        flask = FlaskContainer()
        physics = PhysicsEngine(flask, baseRadius = 36f)
        particleSystem = ParticleSystem()
        engine = GameEngine(flask, physics, soundManager, particleSystem, prefs).apply {
            listener = this@MainActivity
        }

        binding.gameView.engine = engine

        // UI initial values
        binding.txtScore.text = "0"
        binding.txtHighScore.text = "${prefs.highScore}"
        binding.btnSound.text = if (soundManager.isSoundEnabled) "🔊" else "🔇"
        binding.imgNextBall.setImageResource(ballDrawables[0])

        engine.startNewGame()
    }

    private fun setupListeners() {
        // Stir button: swirls / shakes flask to loosen balls
        binding.btnStir.setOnClickListener {
            engine.stirFlask()
        }

        // Sound toggle button
        binding.btnSound.setOnClickListener {
            soundManager.isSoundEnabled = !soundManager.isSoundEnabled
            prefs.isSoundEnabled = soundManager.isSoundEnabled
            binding.btnSound.text = if (soundManager.isSoundEnabled) "🔊" else "🔇"
        }

        // Pause button
        binding.btnPause.setOnClickListener {
            engine.pause()
            binding.layoutPause.visibility = View.VISIBLE
        }

        // Resume button in pause modal
        binding.btnResume.setOnClickListener {
            binding.layoutPause.visibility = View.GONE
            engine.resume()
        }

        // Restart from pause modal
        binding.btnRestartFromPause.setOnClickListener {
            binding.layoutPause.visibility = View.GONE
            engine.startNewGame()
        }

        // Play Again button in game over modal
        binding.btnPlayAgain.setOnClickListener {
            binding.layoutGameOver.visibility = View.GONE
            engine.startNewGame()
        }
    }

    override fun onScoreChanged(score: Int, highScore: Int) {
        runOnUiThread {
            binding.txtScore.text = "%,d".format(score)
            binding.txtHighScore.text = "%,d".format(highScore)
        }
    }

    override fun onNextBallChanged(nextLevel: Int) {
        // Next ball preview is rendered directly at the dropper in GameView
    }

    override fun onCurrentMaxBallChanged(maxLevel: Int) {
        runOnUiThread {
            val idx = maxLevel.coerceIn(0, ballDrawables.size - 1)
            binding.imgNextBall.setImageResource(ballDrawables[idx])
            // Celebratory pop animation when a new highest cat level is achieved
            if (maxLevel > 0) {
                binding.imgNextBall.animate()
                    .scaleX(1.25f)
                    .scaleY(1.25f)
                    .setDuration(120)
                    .withEndAction {
                        binding.imgNextBall.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(120)
                            .start()
                    }
                    .start()
            }
        }
    }

    override fun onCombo(combo: Int) {
        // Haptic combo pulse
        triggerVibration(false)
    }

    override fun onGameOver(finalScore: Int, highestLevel: Int, isNewHighScore: Boolean) {
        runOnUiThread {
            binding.txtFinalScore.text = "%,d".format(finalScore)
            binding.txtNewHighScoreBanner.visibility = if (isNewHighScore) View.VISIBLE else View.GONE

            val idx = highestLevel.coerceIn(0, ballDrawables.size - 1)
            binding.imgFinalHighestCat.setImageResource(ballDrawables[idx])
            binding.txtHighestCatName.text = catNames[idx]

            binding.layoutGameOver.visibility = View.VISIBLE
        }
    }

    override fun onHapticFeedback(heavy: Boolean) {
        triggerVibration(heavy)
    }

    private fun triggerVibration(heavy: Boolean) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val duration = if (heavy) 50L else 20L
            val amplitude = if (heavy) VibrationEffect.DEFAULT_AMPLITUDE else 80
            vib.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(if (heavy) 40L else 15L)
        }
    }

    override fun onResume() {
        super.onResume()
        enableFullscreen()
        binding.gameView.resumeGame()
    }

    override fun onPause() {
        super.onPause()
        binding.gameView.pauseGame()
        engine.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

