# Cat Flask Merge (Suika-Style Android Game) 🐱⚗️

An end-to-end, high-performance Android physics merge game developed with **Kotlin**, **Android Gradle Plugin (AGP)**, and **Android Studio**.

Inspired by the Suika (Watermelon) merge mechanics, set inside a **handcrafted chemical glass flask** with humorous progressive **meme cat avatars** (from tiny kitten to ultra buff gigachad cat) and satisfying level-up sound effects!

---

## 🎮 Game Mechanics & Features

1. **Chemical Flask Container**:
   - Geometrically modeled after the Florence / chemical flask with a vertical neck funnel, smooth transition shoulders, and a large circular bottom bulb chamber.
   - Glassmorphic neon rendering with glowing boundaries and dynamic danger line.

2. **Drop System**:
   - **Continuous Flow**: Drops a Level 0 ball into the flask every **1.0 second**, as specified.
   - **Tactical Aiming**: Players can touch and drag horizontally across the flask opening to guide drop placement, or tap to drop immediately.
   - **Visual Countdown**: Sleek circular progress timer around the queued ball indicating when the next auto-drop will trigger.
   - **Upcoming Ball Preview**: Shows the next ball in the queue in the top HUD.

3. **Physics Engine**:
   - **Zero-Tunneling Verlet Multi-Substep Solver**: 8 substeps per frame for continuous collision detection and rock-solid stacking stability.
   - Circle-Circle collision and continuous Line Segment & Arc boundary constraints.
   - Elasticity, surface friction, mass scaling ($m \propto r^2$), and rolling angular velocity.
   - **Stir / Shake Flask**: Interactive "🌀 STIR" button that applies a fluid vortex impulse to loosen stacked balls.

4. **Merge Progression (14 Distinct Levels)**:
   - When two balls of the same level touch, they merge at their midpoint into a single ball of the next level.
   - Spawns the next level ball with an elastic bouncy pop animation ($1.38\times \to 1.0\times$).
   - Plays the level's dedicated audio track (`level_0.mp3` through `level_13.mp3`) with **SoundPool** for zero latency.
   - Triggers particle bursts (golden stars, neon sparks, shockwave rings) and floating score texts.
   - Combo multiplier: Merges within 1.2s award combo bonuses ($x2, x3, x4\dots$).

5. **Danger & Game Over**:
   - Dynamic danger line across the flask neck.
   - When balls pile up above the danger line, a warning vignette flashes with a 3.0-second countdown.
   - If not resolved in 3 seconds, triggers **Game Over** overlay showcasing the final score, new high score banner, and the highest cat unlocked.

---

## 🐱 Cat Evolution Hierarchy

| Level | Avatar Title | Description | Score Value |
| :---: | :--- | :--- | :---: |
| **0** | **Bonnet Kitten** | Cute tiny kitten in orange knit hood | 0 (Base) |
| **1** | **Golden Kitten** | Wide-eyed cute golden kitten | 10 pts |
| **2** | **Striped Tabby** | Playful tabby cat | 20 pts |
| **3** | **Polite Tuxedo Cat** | Iconic polite cat with awkward smile | 40 pts |
| **4** | **Smug White Cat** | White cat with confident smirk | 80 pts |
| **5** | **Squinty Grey Cat** | Squinting grey cat | 160 pts |
| **6** | **Smiling Void Cat** | Black cat with beaming human grin | 320 pts |
| **7** | **Ginger Chad Cat** | Chiseled jawline ginger cat | 640 pts |
| **8** | **Tuxedo Chad Cat** | Sophisticated gigachad tuxedo cat | 1,280 pts |
| **9** | **White Chad Cat** | Radiant white gigachad cat | 2,560 pts |
| **10** | **Grey Chad Cat** | Monochromatic gigachad cat | 5,120 pts |
| **11** | **Bearded Guru Cat** | Majestic calico cat with beard | 10,240 pts |
| **12** | **Tabby Chad Boss** | Ultimate tabby alpha cat | 20,480 pts |
| **13** | **ULTRA BUFF CAT** | God-tier bodybuilder shredded cat | 40,960 pts |

---

## 🛠 Project Structure

```
Suika-Merge-v0.0.2/
├── app/
│   ├── build.gradle.kts                    # App-level build config (compileSdk 36, Java 17, ViewBinding)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml         # Fullscreen portrait configuration
│   │   │   ├── java/com/suika/catmerge/
│   │   │   │   ├── MainActivity.kt         # Immersive lifecycle, UI bindings, and haptics
│   │   │   │   └── game/
│   │   │   │       ├── GameEngine.kt       # State machine, drop timing, combos, scoring
│   │   │   │       ├── GameView.kt         # 60-120 FPS hardware-accelerated SurfaceView
│   │   │   │       ├── audio/
│   │   │   │       │   └── SoundManager.kt # Low-latency SoundPool manager for 14 levels
│   │   │   │       ├── data/
│   │   │   │       │   └── GamePreferences.kt # Persistent High Score and settings
│   │   │   │       ├── effects/
│   │   │   │       │   └── ParticleSystem.kt  # Sparkles, stars, shockwaves, floating text
│   │   │   │       └── physics/
│   │   │   │           ├── Vector2D.kt        # 2D vector operations
│   │   │   │           ├── Ball.kt            # Ball rigid body, radius scaling, mass
│   │   │   │           ├── FlaskContainer.kt  # Mathematical flask collision model & rendering
│   │   │   │           └── PhysicsEngine.kt   # Verlet substepping, collision & merge solver
│   │   │   └── res/
│   │   │       ├── drawable/               # Antialiased circular cat balls (0..13) and UI drawables
│   │   │       ├── raw/                    # Low-latency level sounds (level_0.mp3 .. level_13.mp3)
│   │   │       ├── layout/                 # activity_main.xml (HUD, pause modal, game over dialog)
│   │   │       └── values/                 # colors.xml, strings.xml, themes.xml
│   │   └── test/java/com/suika/catmerge/
│   │       └── GamePhysicsTest.kt          # Unit test suite verifying math and merge logic
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew & gradlew.bat
└── README.md
```

---

## 🚀 Building and Running

### 1. In Android Studio:
1. Launch Android Studio.
2. Select **Open** and choose `c:\Codeing_Files\Projects\Suika-Merge-v0.0.2`.
3. Let Gradle sync (configured for Gradle 8.13 + AGP 8.13.2).
4. Connect an Android device (or launch an Emulator) and click **Run (Shift + F10)**.

### 2. From Terminal:
- **Run Unit Tests**:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
- **Build Debug APK**:
  ```powershell
  .\gradlew.bat assembleDebug
  ```
  The generated APK is located at:
  `app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Pre-Built APK
The APK has been compiled and verified:
- **Location**: `app/build/outputs/apk/debug/app-debug.apk`
- **Package**: `com.suika.catmerge`
- **Target OS**: Android 8.0 (API 26) through Android 15 / 16 (API 36)

