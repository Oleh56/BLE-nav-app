package com.example.BLE_nav_app

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.BLE_nav_app.databinding.ActivityAudioRecordBinding
import java.io.IOException


private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class AudioRecord : AppCompatActivity() {
    private lateinit var binding : ActivityAudioRecordBinding
    private var permissionsToRecordAudio = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var player: MediaPlayer? = null
    private var recorder: AudioRecord? = null
    private val LOG_TAG = "AudioRecord"
    private lateinit var fileName: String


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsToRecordAudio = if(requestCode == REQUEST_RECORD_AUDIO_PERMISSION){
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else{
            false
        }
        if(!permissionsToRecordAudio) finish()
    }

    private fun createAudioRecorder(): AudioRecord? {
        if (permissionsToRecordAudio) {
            // Check if RECORD_AUDIO permission is granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

                val minBufferSize = AudioRecord.getMinBufferSize(
                    32000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                // Permission granted, proceed with creating AudioRecord
                val recorder = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(32000)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(2 * minBufferSize)
                    .build()

                return recorder
            } else {
                // Permission not granted, request it
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO_PERMISSION
                )
            }
        }
        return null
    }


    private fun createMediaRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ){
            MediaRecorder(this@AudioRecord)
        } else MediaRecorder()
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }


    private fun startRecording() {
        val mediaRecorder = createMediaRecorder()
        val audioRecorder = createAudioRecorder()

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setAudioSamplingRate(44100)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "MediaRecorder prepare() failed")
            }
        }

        audioRecorder?.startRecording()
    }


    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }


    //TarsosDSP part below

//    initializeTarsosDSP(){
//        val audioInputStream = AudioInputStream(recorder?.audioSource)
//    }

    val minFrequency:Float = 18000.0F
    val maxFrequency:Float = 22000.0F

//    override fun handlePitch(pitchDetectionResult: PitchDetectionResult, audioEvent: AudioEvent){
//        val pitch = pitchDetectionResult.pitch
//        if(pitch >= minFrequency && pitch >= maxFrequency){
//
//        }
//    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        fileName = "${this@AudioRecord.filesDir}/recorded_audio.3gp"

        binding.btnRecord.setOnClickListener(){
            val isRecording = recorder != null

            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.btnPlay.setOnClickListener(){
            val isPlaying = player?.isPlaying ?: false

            if (isPlaying) {
                stopPlaying()
            } else {
                startPlaying()
            }
        }

    }
}
