package com.example.myapplication

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // UI components
    private lateinit var team1Button: Button
    private lateinit var team2Button: Button
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button

    // Media player for alarm sound
    private var alarmSound: MediaPlayer? = null

    // Timer variables
    private var team1TimeInSeconds = 0L
    private var team2TimeInSeconds = 0L
    private var activeTeam = 0 // 0 = none, 1 = team1, 2 = team2
    private var isPaused = false
    private var isRunning = false

    // Game duration (default 5 minutes)
    private var gameDurationInSeconds = 300L
    private var remainingGameTimeInSeconds = 300L

    // Handler for updating timers
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    // Countdown timer for overall game time
    private var gameCountDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        team1Button = findViewById(R.id.button)
        team2Button = findViewById(R.id.button2)
        startButton = findViewById(R.id.button3)
        pauseButton = findViewById(R.id.button4)
        resetButton = findViewById(R.id.button5)

        // Initialize alarm sound
        initializeAlarmSound()

        setupClickListeners()
        updateButtonDisplay()
    }

    private fun initializeAlarmSound() {
        // Create MediaPlayer with the alarm sound resource
        alarmSound = MediaPlayer.create(this, R.raw.alarm_sound)
        // Loop the sound
        alarmSound?.isLooping = false
    }

    private fun setupClickListeners() {
        team1Button.setOnClickListener {
            if (!isRunning || isPaused) return@setOnClickListener

            if (activeTeam != 1) {
                activeTeam = 1
                updateButtonDisplay()
            }
        }

        team2Button.setOnClickListener {
            if (!isRunning || isPaused) return@setOnClickListener

            if (activeTeam != 2) {
                activeTeam = 2
                updateButtonDisplay()
            }
        }

        startButton.setOnClickListener {
            if (!isRunning) {
                showGameDurationDialog()
            } else if (isPaused) {
                isPaused = false
                updateButtonDisplay()
                resumeTimers()
            }
        }

        pauseButton.setOnClickListener {
            if (isRunning && !isPaused) {
                isPaused = true
                updateButtonDisplay()
                pauseTimers()
            }
        }

        resetButton.setOnClickListener {
            resetGame()
            stopAlarmSound() // Ensure alarm is stopped when resetting
        }
    }

    private fun showGameDurationDialog() {
        val options = arrayOf("5 minutes", "10 minutes", "15 minutes", "20 minutes")
        val durations = arrayOf(300L, 600L, 900L, 1200L)

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Game Duration")
            .setItems(options) { _, which ->
                gameDurationInSeconds = durations[which]
                remainingGameTimeInSeconds = gameDurationInSeconds
                startGame()
            }
            .show()
    }

    private fun startGame() {
        isRunning = true
        isPaused = false

        // Reset team times
        team1TimeInSeconds = 0
        team2TimeInSeconds = 0
        activeTeam = 0 // No active team initially

        updateButtonDisplay()

        // Set up timer runnable
        timerRunnable = object : Runnable {
            override fun run() {
                if (isRunning && !isPaused) {
                    when (activeTeam) {
                        1 -> team1TimeInSeconds++
                        2 -> team2TimeInSeconds++
                    }
                    updateButtonDisplay()
                }
                handler.postDelayed(this, 1000)
            }
        }

        // Start the handler
        handler.post(timerRunnable!!)

        // Start the countdown timer for the overall game
        gameCountDownTimer = object : CountDownTimer(remainingGameTimeInSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingGameTimeInSeconds = millisUntilFinished / 1000
            }

            override fun onFinish() {
                endGame()
            }
        }.start()
    }

    private fun pauseTimers() {
        gameCountDownTimer?.cancel()
    }

    private fun resumeTimers() {
        // Restart the countdown timer with remaining time
        gameCountDownTimer = object : CountDownTimer(remainingGameTimeInSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingGameTimeInSeconds = millisUntilFinished / 1000
            }

            override fun onFinish() {
                endGame()
            }
        }.start()
    }

    private fun resetGame() {
        // Stop all timers
        gameCountDownTimer?.cancel()
        handler.removeCallbacks(timerRunnable ?: return)

        // Reset variables
        isRunning = false
        isPaused = false
        team1TimeInSeconds = 0
        team2TimeInSeconds = 0
        remainingGameTimeInSeconds = gameDurationInSeconds
        activeTeam = 0

        updateButtonDisplay()
    }

    private fun playAlarmSound() {
        alarmSound?.apply {
            // Reset to start if it was played before
            if (isPlaying) {
                stop()
                prepare()
            }
            start()
        }
    }

    private fun stopAlarmSound() {
        alarmSound?.apply {
            if (isPlaying) {
                stop()
                prepare()
            }
        }
    }

    private fun endGame() {
        isRunning = false
        isPaused = true
        activeTeam = 0

        // Stop the handler
        handler.removeCallbacks(timerRunnable ?: return)

        updateButtonDisplay()

        // Play alarm sound
        playAlarmSound()

        // Show results dialog
        showResultsDialog()
    }

    private fun showResultsDialog() {
        val team1Time = formatTime(team1TimeInSeconds)
        val team2Time = formatTime(team2TimeInSeconds)

        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("Results:\n\nTeam ALPHA: $team1Time\nTeam SIGMA: $team2Time")
            .setPositiveButton("OK") { dialog, _ ->
                stopAlarmSound()
                dialog.dismiss()
            }
            .setNeutralButton("Reset") { _, _ ->
                stopAlarmSound()
                resetGame()
            }
            .setCancelable(false)
            .show()
    }

    private fun updateButtonDisplay() {
        // Update team button texts with current times
        team1Button.text = "ALPHA\n${formatTime(team1TimeInSeconds)}"
        team2Button.text = "SIGMA\n${formatTime(team2TimeInSeconds)}"

        // Highlight active team
        team1Button.alpha = if (activeTeam == 1) 1.0f else 0.7f
        team2Button.alpha = if (activeTeam == 2) 1.0f else 0.7f

        // Update control buttons
        startButton.isEnabled = !isRunning || isPaused
        pauseButton.isEnabled = isRunning && !isPaused
        resetButton.isEnabled = isRunning || team1TimeInSeconds > 0 || team2TimeInSeconds > 0

        // Update button text
        startButton.text = if (!isRunning) "START" else if (isPaused) "RESUME" else "START"
    }

    private fun formatTime(seconds: Long): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        gameCountDownTimer?.cancel()
        handler.removeCallbacks(timerRunnable ?: return)

        // Release media player resources
        alarmSound?.release()
        alarmSound = null
    }
}