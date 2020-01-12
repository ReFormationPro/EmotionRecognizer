package com.oguzhan.emotionrecorder

import android.app.IntentService
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Camera
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.NotificationCompat

// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
const val ACTION_FOO = "com.oguzhan.emotionrecorder.action.FOO"
const val ACTION_BAZ = "com.oguzhan.emotionrecorder.action.BAZ"

const val EXTRA_PARAM1 = "com.oguzhan.emotionrecorder.extra.PARAM1"
const val EXTRA_PARAM2 = "com.oguzhan.emotionrecorder.extra.PARAM2"

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions and extra parameters.
 */
class TriggerService : IntentService("TriggerService") {
    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null
    private var mIsRecording: Boolean = true
    private lateinit var mCameraHelper: CameraService

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_FOO -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
            }
            ACTION_BAZ -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
            }
        }
    }
    companion object {
        private val TAG = "Trigger Service"
        val mScreenListener = object: BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                Log.e(TAG, "Received XXX!")
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.w(TAG, "onStartCommand")
        makeForeground()
        val screenStateFilter = IntentFilter()
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON)
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(mScreenListener, screenStateFilter)
        return Service.START_STICKY
    }
    override fun onCreate() {
        Log.w(TAG, "onCreate")
        mCameraHelper = CameraService(this)
    }
    override fun onDestroy() {
        Log.w(TAG, "onDestroy")
        mCameraHelper.finishSafely()
        super.onDestroy()
    }
    // Essential Functions
    fun takePicture() {
        mCameraHelper.takePicture()
    }
    private fun timer() {
        Handler().postDelayed(
            {
                Log.e(TAG, "This'll run 1000*30 milliseconds later")
                //takePicture()
                if (mIsRecording) {
                    timer()
                }
            },
            1000*30
        )
    }
    // Background Thread Functions
    private fun startBackground() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }
    private fun stopBackground() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
    // Needs care
    private fun makeForeground() {
        val notificationIntent = Intent(this, CameraService::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        val Notif_ID = 1
        val NOTIF_CHANNEL_ID = "123"
        startForeground(
            Notif_ID,
            NotificationCompat.Builder(
                this,
                NOTIF_CHANNEL_ID
            ) // don't forget create a notification channel first
                .setOngoing(true)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .build()
        )
    }
}
