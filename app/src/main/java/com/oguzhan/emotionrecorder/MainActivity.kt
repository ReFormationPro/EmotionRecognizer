package com.oguzhan.emotionrecorder

import android.content.*
import android.os.*
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.*
import android.content.Intent


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    var mMessenger: Messenger? = null
    lateinit var mEmotionRecognizer: EmotionRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val filter = IntentFilter(CameraService.MESSAGE_CAPTURE_COMPLETED)
        registerReceiver(MessageListener, filter)
        mEmotionRecognizer = EmotionRecognizer(this)
    }
    private val mCameraConn = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            Log.i(TAG, "onServiceConnected");
            //mCameraService = (service as CameraService.LocalBinder).service
            mMessenger = Messenger(service)
        }
        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.e(TAG, "onServiceDisconnected")
            //mCameraService = null
        }
        override fun onBindingDied(name: ComponentName?) {
            Log.e(TAG, "onBindingDied")
        }
        override fun onNullBinding(name: ComponentName?) {
            Log.e(TAG, "onNullBinding")
        }
    }
    private val MessageListener = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                CameraService.MESSAGE_CAPTURE_COMPLETED -> {
                    Log.e(TAG, "Message Capture Completed")
                    val file = File(getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES), "latest.jpg")
                    val bitmap: Bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }
    // ----- Button Actions ------
    fun takeAPicture(btn: View) {
        val msg = Message.obtain()
        msg.arg1 = CameraService.REQUEST_TAKE_A_PICTURE
        mMessenger?.send(msg)
    }
    fun openGraph(btn: View) {
        val intentMain = Intent(
            this@MainActivity,
            GraphActivity::class.java
        )
        this@MainActivity.startActivity(intentMain)
    }
    fun startRecording(btn: View) {
        val msg = Message.obtain()
        msg.arg1 = CameraService.REQUEST_START_RECORDING
        mMessenger?.send(msg)
    }
    fun stopRecording(btn: View) {
        val msg = Message.obtain()
        msg.arg1 = CameraService.REQUEST_STOP_RECORDING
        mMessenger?.send(msg)
    }
    //----- Analyzing Images ----
    fun AnalyzeLatest(btn: View?) {
        analyzeLatest()
    }
    fun analyzeLatest():HashMap<String, Float>? {
        val file = File(getExternalFilesDir(
            Environment.DIRECTORY_PICTURES), "latest.jpg")
        val bitmap: Bitmap = BitmapFactory.decodeFile(file.absolutePath)
        return mEmotionRecognizer.analyzeImage(bitmap)
    }
    override fun onDestroy() {
        super.onDestroy()
        mEmotionRecognizer.finishSafely()
    }
    override fun onResume() {
        super.onResume()
        if (bindService(Intent(this, CameraService::class.java)
                , mCameraConn
                , Context.BIND_AUTO_CREATE)) {
            Log.i(TAG,"CameraService bind fine")
        } else {
            Log.e(TAG, "CameraService bind fail")
        }
    }
    override fun onPause() {
        super.onPause()
        unbindService(mCameraConn)
    }

}
