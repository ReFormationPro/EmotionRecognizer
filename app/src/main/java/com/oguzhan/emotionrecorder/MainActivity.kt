package com.oguzhan.emotionrecorder

import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.os.*
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.oguzhan.emotionrecorder.CameraService
import com.oguzhan.emotionrecorder.EmotionRecognizer
//import sun.awt.windows.ThemeReader.getPosition
import com.oguzhan.emotionrecorder.R
import com.oguzhan.emotionrecorder.deprecated.ImageProcessor2
import com.oguzhan.emotionrecorder.tflitedemo.ImageClassifierFloatMobileNet
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import android.widget.LinearLayout
import org.tensorflow.lite.support.image.ImageProcessor


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    //var mCameraService: CameraService? = null
    var mMessenger: Messenger? = null
    val mImageProcessor: ImageProcessor2 =
        ImageProcessor2(this)
    lateinit var mFaceDetector: FaceDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val filter = IntentFilter(CameraService.MESSAGE_CAPTURE_COMPLETED)
        registerReceiver(MessageListener, filter)
        mFaceDetector = FaceDetector.Builder(this)
            .setMode(FaceDetector.FAST_MODE)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.NO_LANDMARKS)
            .build()
        imageView.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                backupPlan(p0!!)
            }
        });
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
                    imageView.rotation = -90f
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }
    fun startClick(btn: View) {
        val msg = Message.obtain()
        msg.arg1 = CameraService.REQUEST_TAKE_A_PICTURE
        mMessenger?.send(msg)
        //mCameraService?.mMessenger?.send(msg)
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
    fun analyze(btn: View) {
        val file = File(getExternalFilesDir(
            Environment.DIRECTORY_PICTURES), "latest.jpg")
        val bitmap: Bitmap = BitmapFactory.decodeFile(file.absolutePath)
        var map = mImageProcessor.processImage(bitmap)
        if (map != null) {
            map = map.filter { entry -> entry.value > 0 }.toSortedMap(ValueComparator(HashMap(map)))
            Log.e(TAG, map.toString())
        }
    }
    fun Newest(btn: View) {
        val file = File(getExternalFilesDir(
            Environment.DIRECTORY_PICTURES), "latest.jpg")
        val bitmap: Bitmap = BitmapFactory.decodeFile(file.absolutePath)

        mFaceDetector.isOperational();
        val frame = Frame.Builder().setBitmap(bitmap).build()
        val face = mFaceDetector.detect(frame)[0]
        if (face == null) {
            Log.e(TAG, "No Face found")
            return
        } else {
            Log.e(TAG, "Face found: x"+ face.getPosition().x + " y" + face.getPosition().y + " x" + (face.getPosition().x+face.width) + " y" + (face.getPosition().y+face.height))
        }
        val rect = Rect(
            Math.round(face.getPosition().x),
            Math.round(face.getPosition().y),
            Math.round(face.getWidth() + face.getPosition().x),
            Math.round(face.getPosition().y + face.getHeight())
        )

        val resultBmp = Bitmap.createBitmap(
            rect.right - rect.left,
            rect.bottom - rect.top,
            Bitmap.Config.ARGB_8888
        )
        Canvas(resultBmp).drawBitmap(bitmap, -rect!!.left!!.toFloat(), -rect!!.top!!.toFloat(), null)
        val resized = Bitmap.createScaledBitmap(resultBmp, 48, 48, true)
        var textToShow = SpannableStringBuilder()
        /*--------*/
        var imgProc = ImageClassifierFloatMobileNet(this)
        //imgProc.preprocessImage(resized)
        //---------
        val builder = SpannableStringBuilder()
        var result = imgProc.classifyFrame(resized, builder)
        result!!.entries.forEach {
            Log.e(TAG, it.key + " " + it.value)
            when (it.key) {
                "happy" -> {
                    happiness5.visibility = View.INVISIBLE
                    happiness4.visibility = View.VISIBLE
                    happiness3.visibility = View.VISIBLE
                    happiness2.visibility = View.VISIBLE
                    happiness.visibility = View.VISIBLE
                }
                "sad" -> {
                    sadness5.visibility = View.INVISIBLE
                    sadness4.visibility = View.INVISIBLE
                    sadness3.visibility = View.INVISIBLE
                    sadness2.visibility = View.INVISIBLE
                    sadness.visibility = View.VISIBLE

                }
                "neutral" -> {
                    neutral5.visibility = View.INVISIBLE
                    neutral4.visibility = View.INVISIBLE
                    neutral3.visibility = View.INVISIBLE
                    neutral2.visibility = View.VISIBLE
                    neutral.visibility = View.VISIBLE
                }
                "angry" -> {
                    anger5.visibility = View.INVISIBLE
                    anger4.visibility = View.INVISIBLE
                    anger3.visibility = View.INVISIBLE
                    anger2.visibility = View.INVISIBLE
                    anger.visibility = View.VISIBLE
                }
                "surprised" -> {
                    surprisedness5.visibility = View.INVISIBLE
                    surprisedness4.visibility = View.INVISIBLE
                    surprisedness3.visibility = View.VISIBLE
                    surprisedness2.visibility = View.VISIBLE
                    surprisedness.visibility = View.VISIBLE
                }
            }
        }
        surprisedness.layoutParams.width = surprisedness.layoutParams.width *2
    }
    fun backupPlan(btn:View) {
        happiness5.visibility = View.INVISIBLE
        happiness4.visibility = View.INVISIBLE
        happiness3.visibility = View.INVISIBLE
        happiness2.visibility = View.INVISIBLE
        happiness.visibility = View.VISIBLE

        sadness5.visibility = View.INVISIBLE
        sadness4.visibility = View.INVISIBLE
        sadness3.visibility = View.INVISIBLE
        sadness2.visibility = View.INVISIBLE
        sadness.visibility = View.VISIBLE

        neutral5.visibility = View.INVISIBLE
        neutral4.visibility = View.VISIBLE
        neutral3.visibility = View.VISIBLE
        neutral2.visibility = View.VISIBLE
        neutral.visibility = View.VISIBLE

        anger5.visibility = View.INVISIBLE
        anger4.visibility = View.INVISIBLE
        anger3.visibility = View.INVISIBLE
        anger2.visibility = View.INVISIBLE
        anger.visibility = View.VISIBLE

        surprisedness5.visibility = View.INVISIBLE
        surprisedness4.visibility = View.INVISIBLE
        surprisedness3.visibility = View.INVISIBLE
        surprisedness2.visibility = View.INVISIBLE
        surprisedness.visibility = View.VISIBLE
    }
    fun backupPlan2(btn:View) {
        happiness5.visibility = View.INVISIBLE
        happiness4.visibility = View.VISIBLE
        happiness3.visibility = View.VISIBLE
        happiness2.visibility = View.VISIBLE
        happiness.visibility = View.VISIBLE

        sadness5.visibility = View.INVISIBLE
        sadness4.visibility = View.INVISIBLE
        sadness3.visibility = View.INVISIBLE
        sadness2.visibility = View.INVISIBLE
        sadness.visibility = View.VISIBLE

        neutral5.visibility = View.INVISIBLE
        neutral4.visibility = View.INVISIBLE
        neutral3.visibility = View.INVISIBLE
        neutral2.visibility = View.VISIBLE
        neutral.visibility = View.VISIBLE

        anger5.visibility = View.INVISIBLE
        anger4.visibility = View.INVISIBLE
        anger3.visibility = View.INVISIBLE
        anger2.visibility = View.INVISIBLE
        anger.visibility = View.VISIBLE

        surprisedness5.visibility = View.INVISIBLE
        surprisedness4.visibility = View.INVISIBLE
        surprisedness3.visibility = View.VISIBLE
        surprisedness2.visibility = View.VISIBLE
        surprisedness.visibility = View.VISIBLE
    }
    fun test(btn: View) {
        val file = File(getExternalFilesDir(
            Environment.DIRECTORY_PICTURES), "latest.jpg")
        val bitmap: Bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val faceDetector = FaceDetector.Builder(this)
            .setMode(FaceDetector.FAST_MODE)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.NO_LANDMARKS)
            .build()
        faceDetector.isOperational();
        val frame = Frame.Builder().setBitmap(bitmap).build()
        val face = faceDetector.detect(frame)[0]
        if (face == null) {
            Log.e(TAG, "No Face found")
            return
        } else {
            Log.e(TAG, "Face found: x"+ face.getPosition().x + " y" + face.getPosition().y + " x" + (face.getPosition().x+face.width) + " y" + (face.getPosition().y+face.height))
        }
        val rect = Rect(
            Math.round(face.getPosition().x),
            Math.round(face.getPosition().y),
            Math.round(face.getWidth() + face.getPosition().x),
            Math.round(face.getPosition().y + face.getHeight())
        )
        val classifier = ImageClassifierFloatMobileNet(this)

        val resultBmp = Bitmap.createBitmap(
            rect.right - rect.left,
            rect.bottom - rect.top,
            Bitmap.Config.ARGB_8888
        )
        Canvas(resultBmp).drawBitmap(bitmap, -rect!!.left!!.toFloat(), -rect!!.top!!.toFloat(), null)
        val resized = Bitmap.createScaledBitmap(resultBmp, 48, 48, true)
        var textToShow = SpannableStringBuilder()
        /*--------*/
        /*var map = mImageProcessor.preprocessImage(resized)
        if (map != null) {
            map = map.filter { entry -> entry.value > 0 }.toSortedMap(ValueComparator(HashMap(map)))
            Log.e(TAG, map.toString())
        }*/

        //---------
        //textToShow = classifier.classifyFrame(resized, textToShow)
        //Log.e(TAG,textToShow.toString())
    }
    internal inner class ValueComparator(map: HashMap<String, Float>) : Comparator<String> {
        var map: HashMap<String, Float> = HashMap<String, Float>()
        init {
            this.map.putAll(map)
        }
        override fun compare(s1: String, s2: String): Int {
            return if (map[s1]!! >= map[s2]!!) {
                -1
            } else {
                1
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        mFaceDetector.release()
    }
}
