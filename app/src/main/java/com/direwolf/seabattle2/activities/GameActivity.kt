package com.direwolf.seabattle2.activities

import android.content.Intent
import android.graphics.Color
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.direwolf.seabattle2.R
import com.direwolf.seabattle2.objects.game.AI
import com.direwolf.seabattle2.objects.game.AIGrid
import com.direwolf.seabattle2.objects.game.PlayerGrid
import java.lang.Integer.min
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GameActivity : DefaultActivity() {
    private lateinit var playerGrid: PlayerGrid
    private lateinit var aiGrid: AIGrid
    private lateinit var ai: AI
    private var playerTurn = true
    private var shotsPlayer = 0
    private var shotsAI = 0
    private var gameEnd = false
    private var cellSize = 0
    private var soundPool: SoundPool? = null
    private var hitSoundId = 0
    private var missSoundId = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isBotThinking = false
    private var playerWeapons = com.direwolf.seabattle2.objects.game.Weapons()
    private var aiWeapons = com.direwolf.seabattle2.objects.game.Weapons()
    private var radarMode = false
    private var airstrikeMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Initialize sound pool
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .build()
        try {
            hitSoundId = soundPool?.load(this, R.raw.hit, 1) ?: 0
            missSoundId = soundPool?.load(this, R.raw.miss, 1) ?: 0
        } catch (e: Exception) {
            // Sound files might not exist yet
        }

        val layout = findViewById<ConstraintLayout>(R.id.game_layout)
        cellSize = min(screenHeight / 12, screenWidth / 23)

        val top = screenHeight / 2 - 5 * cellSize
        playerGrid = PlayerGrid(this, layout, cellSize, 10, 10, cellSize, top, ::playSound)
        aiGrid = AIGrid(
            this,
            layout,
            cellSize,
            10,
            10,
            screenWidth - cellSize * 11,
            top,
            ::playerListener
        )
        val advancedDifficulty = intent.extras?.getBoolean("advancedDifficulty", false) ?: false
        ai = AI(this, advancedDifficulty)
        val ships1 = ai.setShips()
        aiGrid.setShips(ships1)

        val ships2 = intent.extras?.get("grid") as Array<Int>
        playerGrid.setShips(ships2)
        
        // Record player placement for advanced AI
        if (advancedDifficulty) {
            ai.recordPlayerPlacement(ships2)
        }
        
        // Add weapon buttons
        setupWeaponButtons(layout, top)
    }
    
    private fun setupWeaponButtons(layout: ConstraintLayout, top: Int) {
        val btnWidth = cellSize * 4
        val btnHeight = (cellSize * 0.9).toInt()
        val spacing = 20

        // Кнопка Радара
        val radarButton = Button(this)
        radarButton.id = android.view.View.generateViewId()
        radarButton.text = "Радар"
        radarButton.setTextColor(Color.WHITE)
        radarButton.setBackgroundColor(Color.parseColor("#00838F"))

        // ЦЕНТРИРОВАНИЕ ТЕКСТА
        radarButton.gravity = Gravity.CENTER
        radarButton.setPadding(0, 0, 0, 0)
        radarButton.includeFontPadding = false // Убирает системные отступы шрифта

        val radarParams = ConstraintLayout.LayoutParams(btnWidth, btnHeight)
        radarParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        radarParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        radarParams.bottomMargin = 50
        radarParams.rightMargin = spacing / 2

        // Кнопка Авиаудара
        val airstrikeButton = Button(this)
        airstrikeButton.id = android.view.View.generateViewId()
        airstrikeButton.text = "Авиаудар"
        airstrikeButton.setTextColor(Color.WHITE)
        airstrikeButton.setBackgroundColor(Color.parseColor("#B71C1C"))

        // ЦЕНТРИРОВАНИЕ ТЕКСТА
        airstrikeButton.gravity = Gravity.CENTER
        airstrikeButton.setPadding(0, 0, 0, 0)
        airstrikeButton.includeFontPadding = false

        val airParams = ConstraintLayout.LayoutParams(btnWidth, btnHeight)
        airParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        airParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        airParams.bottomMargin = 50
        airParams.leftMargin = spacing / 2

        // Создание цепочки для центрирования пары кнопок
        radarParams.endToStart = airstrikeButton.id
        airParams.startToEnd = radarButton.id
        radarParams.horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED

        radarButton.layoutParams = radarParams
        airstrikeButton.layoutParams = airParams

        // 4. Set Listeners
        radarButton.setOnClickListener {
            if (playerTurn && !isBotThinking && playerWeapons.canUseRadar()) {
                radarMode = true
                airstrikeMode = false
                Toast.makeText(this, "Выберите зону 3x3", Toast.LENGTH_SHORT).show()
            }
        }

        airstrikeButton.setOnClickListener {
            if (playerTurn && !isBotThinking && playerWeapons.canUseAirstrike()) {
                airstrikeMode = true
                radarMode = false
                Toast.makeText(this, "Выберите линию 3x1", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Add to layout
        layout.addView(radarButton)
        layout.addView(airstrikeButton)
    }

    private fun playSound(isHit: Boolean) {
        val soundId = if (isHit) hitSoundId else missSoundId
        if (soundId != 0) {
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun playerListener(x: Int, y: Int) {
        if (gameEnd || isBotThinking) {
            return
        }
        if (playerTurn) {
            if (radarMode) {
                useRadar(x, y)
                radarMode = false
                return
            }
            if (airstrikeMode) {
                useAirstrike(x, y)
                airstrikeMode = false
                return
            }
            
            val res = aiGrid.boom(x, y)
            playSound(res.first)
            //Log.w("boom", "$x $y $res")
            if (res.first) {
                shotsPlayer += 1
                if (shotsPlayer == 5) { // Only 5 single-deck ships
                    //Log.w("end", "player")
                    endGame(true)
                    return
                }
            } else {
                playerTurn = false
                // Add delay before bot thinks
                handler.postDelayed({
                    botTurn()
                }, 1000) // 1 second delay
            }
        }
    }
    
    private fun useRadar(x: Int, y: Int) {
        if (!playerWeapons.useRadar()) {
            Toast.makeText(this, "Радар уже использован", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show radar scan 3x3 area
        val layout = findViewById<ConstraintLayout>(R.id.game_layout)
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0..9 && ny in 0..9) {
                    val hasShip = aiGrid.hasShipAt(nx, ny)
                    val indicator = TextView(this)
                    indicator.text = if (hasShip) "✓" else "✗"
                    indicator.setTextColor(if (hasShip) Color.GREEN else Color.RED)
                    indicator.textSize = 20f
                    indicator.gravity = android.view.Gravity.CENTER
                    val params = ConstraintLayout.LayoutParams(cellSize, cellSize)
                    params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    params.leftMargin = screenWidth - cellSize * 11 + nx * cellSize
                    params.topMargin = screenHeight / 2 - 5 * cellSize + ny * cellSize
                    indicator.layoutParams = params
                    layout.addView(indicator)
                    
                    // Remove indicator after 2 seconds
                    handler.postDelayed({
                        layout.removeView(indicator)
                    }, 2000)
                }
            }
        }
        Toast.makeText(this, "Радар использован", Toast.LENGTH_SHORT).show()
    }
    
    private fun useAirstrike(x: Int, y: Int) {
        if (!playerWeapons.useAirstrike()) {
            Toast.makeText(this, "Авиаудар уже использован", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Airstrike hits 3 cells in a row (horizontal)
        var hits = 0
        for (dx in 0..2) {
            val nx = x + dx
            if (nx in 0..9) {
                val res = aiGrid.boom(nx, y)
                if (res.first) {
                    hits++
                    shotsPlayer++
                    playSound(true)
                } else {
                    playSound(false)
                }
            }
        }
        
        if (shotsPlayer >= 5) {
            endGame(true)
            return
        }
        
        Toast.makeText(this, "Авиаудар нанесен! Попаданий: $hits", Toast.LENGTH_SHORT).show()
        
        if (hits == 0) {
            playerTurn = false
            handler.postDelayed({
                botTurn()
            }, 1000)
        }
    }

    private fun botTurn() {
        if (gameEnd) return
        isBotThinking = true
        
        // Bot thinking delay
        handler.postDelayed({
            // Bot can use weapons randomly
            if (!aiWeapons.radarUsed && (0..100).random() < 30) {
                useBotRadar()
                return@postDelayed
            }
            if (!aiWeapons.airstrikeUsed && (0..100).random() < 20) {
                useBotAirstrike()
                return@postDelayed
            }
            
            var coor = ai.boom()
            var res2 = playerGrid.boom(coor.first, coor.second)
            playSound(res2[0] as Boolean)
            
            ai.setResult(
                res2[0] as Boolean,
                coor.first,
                coor.second,
                res2[1] as Boolean,
                res2[2] as Int
            )
            
            if (res2[0] as Boolean) {
                shotsAI += 1
                if (shotsAI == 5) { // Only 5 single-deck ships
                    endGame(false)
                    isBotThinking = false
                    return@postDelayed
                }
                // If bot hit, show the hit and continue after delay
                handler.postDelayed({
                    botTurn() // Continue bot's turn
                }, 800) // Delay between bot's hits
            } else {
                // Bot missed, player's turn
                isBotThinking = false
                playerTurn = true
            }
        }, 1500) // Bot thinking time
    }
    
    private fun useBotRadar() {
        if (!aiWeapons.useRadar()) return
        
        val x = (0..7).random()
        val y = (0..7).random()
        
        Toast.makeText(this, "Бот использует радар!", Toast.LENGTH_SHORT).show()
        
        // Bot learns from radar
        for (dx in 0..2) {
            for (dy in 0..2) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0..9 && ny in 0..9) {
                    val hasShip = playerGrid.hasShipAt(nx, ny)
                    if (hasShip) {
                        // Bot found a ship, prioritize this area
                        ai.learnFromRadar(nx, ny)
                    }
                }
            }
        }
        
        handler.postDelayed({
            botTurn()
        }, 2000)
    }
    
    private fun useBotAirstrike() {
        if (!aiWeapons.useAirstrike()) return
        
        val x = (0..7).random()
        val y = (0..9).random()
        
        Toast.makeText(this, "Бот использует авиаудар!", Toast.LENGTH_SHORT).show()
        
        var hits = 0
        for (dx in 0..2) {
            val nx = x + dx
            if (nx in 0..9) {
                val res = playerGrid.boom(nx, y)
                if (res[0] as Boolean) {
                    hits++
                    shotsAI++
                    playSound(true)
                    ai.setResult(
                        res[0] as Boolean,
                        nx,
                        y,
                        res[1] as Boolean,
                        res[2] as Int
                    )
                } else {
                    playSound(false)
                    ai.setResult(false, nx, y, false, 0)
                }
            }
        }
        
        if (shotsAI >= 5) {
            endGame(false)
            isBotThinking = false
            return
        }
        
        handler.postDelayed({
            if (hits > 0) {
                botTurn() // Continue if hit
            } else {
                isBotThinking = false
                playerTurn = true
            }
        }, 1500)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    private fun endGame(player: Boolean) {
        val text: String
        if (player) {
            text = "Player win!"
            gameEnd = true
        } else {
            text = "AI win!"
            gameEnd = true
            aiGrid.showShips()
        }
        isBotThinking = false
        val duration = Toast.LENGTH_LONG

        val toast = Toast.makeText(applicationContext, text, duration)
        toast.show()
        val layout = findViewById<ConstraintLayout>(R.id.game_layout)
        val button = ImageView(this)
        button.setImageResource(R.drawable.home)

        val params = ConstraintLayout.LayoutParams(cellSize * 2,cellSize * 2)
        params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        params.leftMargin = screenWidth / 2 - params.width / 2
        params.topMargin = screenHeight / 2 - params.height / 2
        button.layoutParams = params
        button.scaleType = ImageView.ScaleType.FIT_CENTER
        layout.addView(button)

        button.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
        }
    }
}