package com.kardani.partymonster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kardani.partymonster.MainActivity
import com.kardani.partymonster.R

class PartyMonsterService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var isPartying = false
    private lateinit var vibrator: Vibrator
    private lateinit var cameraManager: CameraManager
    private lateinit var audioManager: AudioManager
    private var originalVolume = 0
    private var originalBrightness = 0
    private var partyCounter = 0
    private var hasFlashlight = false
    private var isFlashlightOn = false
    private var mediaPlayer: MediaPlayer? = null

    private val partyRunnable = object : Runnable {
        override fun run() {
            if (isPartying) {
                partyCounter++
                executePartyStep()
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        _serviceStatus.postValue(true)
        startForeground(NOTIFICATION_ID, createNotification())

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mediaPlayer = MediaPlayer.create(this, R.raw.music).apply {
            isLooping = true
            setVolume(0.5f, 0.5f)
        }

        // Store original states
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        originalBrightness = Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )

        // Check if device has flashlight
        hasFlashlight = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        Log.d("PartyMonster", "Service created with flashlight: $hasFlashlight")
        // Listen for torch mode changes
        if (hasFlashlight) {
            cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    super.onTorchModeChanged(cameraId, enabled)
                    isFlashlightOn = enabled
                }
            }, handler)
        }
    }

    private fun executePartyStep() {
        Log.d("PartyMonster", "Party step: $partyCounter: ${partyCounter % 4}")
        when (partyCounter % 4) {
            0 -> discoBeat()
            1 -> colorCrazy()
            2 -> vibrateParty()
        }
        flashlightPulse()
    }

    private fun discoBeat() {
        // Create a funky vibration pattern
        val pattern = longArrayOf(0, 100, 50, 100, 50, 100)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))

        // Pulse the screen brightness
        val brightness = (Math.sin(partyCounter.toDouble() * 0.5) * 127 + 128).toInt()
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            brightness
        )
    }

    private fun colorCrazy() {
        // Change system accent color (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val manager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                manager.setApplicationNightMode(
                    if (partyCounter % 2 == 0)
                        UiModeManager.MODE_NIGHT_YES
                    else UiModeManager.MODE_NIGHT_NO
                )
            } catch (e: Exception) {
                // Fallback for older devices
            }
        }
    }

    private fun vibrateParty() {
        // Create a dance-like vibration pattern
        val pattern = when ((partyCounter / 4) % 3) {
            0 -> longArrayOf(0, 100, 50, 100, 50, 300) // Techno beat
            1 -> longArrayOf(0, 200, 100, 200, 100)    // House beat
            else -> longArrayOf(0, 50, 50, 50, 50, 50) // Drum roll
        }

        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun flashlightPulse() {
        if (!hasFlashlight) return

        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return

            // Only toggle if current state is different from desired state
            val desiredState = partyCounter % 2 == 0
            if (isFlashlightOn != desiredState) {
                cameraManager.setTorchMode(cameraId, desiredState)
                isFlashlightOn = desiredState
            }
        } catch (e: Exception) {
            Log.e("PartyMonster", "Flashlight error", e)
            // Disable flashlight feature if there's an error
            hasFlashlight = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_PARTY -> {
                isPartying = true
                partyCounter = 0
                mediaPlayer?.start()
                handler.post(partyRunnable)
            }
            ACTION_STOP_PARTY -> {
                stopParty()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "party_monster_channel"

        val channel = NotificationChannel(
            channelId,
            "Party Monster",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controls the Party Monster feature"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Party Monster")
            .setContentText("Party mode is active! 🎉")
            .setSmallIcon(R.drawable.ic_party_active)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun stopParty() {
        isPartying = false
        handler.removeCallbacks(partyRunnable)

        mediaPlayer?.pause()

        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            originalBrightness
        )

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            80,
            0
        )

        // Turn off flashlight
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        _serviceStatus.postValue(false)
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            originalVolume,
            0
        )
        // Make sure flashlight is off when service is destroyed
        if (hasFlashlight && isFlashlightOn) {
            try {
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, false)
            } catch (e: Exception) {
                Log.e("PartyMonster", "Error turning off flashlight", e)
            }
        }
        stopParty()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_START_PARTY = "com.example.action.START_PARTY"
        const val ACTION_STOP_PARTY = "com.example.action.STOP_PARTY"
        private const val NOTIFICATION_ID = 1

        private val _serviceStatus = MutableLiveData(false)
        val serviceStatus: LiveData<Boolean> = _serviceStatus
    }
}