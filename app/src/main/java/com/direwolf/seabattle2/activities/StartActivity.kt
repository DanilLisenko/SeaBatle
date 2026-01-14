package com.direwolf.seabattle2.activities

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import com.direwolf.seabattle2.R

class StartActivity : DefaultActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // Start background music
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Sound file might not exist yet
        }

        val startButton = findViewById<Button>(R.id.startButton)

        startButton.setOnClickListener {
            mediaPlayer?.release()
            val intent = Intent(this, PlacementActivity::class.java)
            // Убрали привязку к чекбоксу, передаем false по умолчанию
            intent.putExtra("advancedDifficulty", false)
            startActivity(intent)
        }
        Toast.makeText(this, "Добро пожаловать в игру SeaBattle!", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}