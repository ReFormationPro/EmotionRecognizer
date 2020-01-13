package com.oguzhan.emotionrecorder

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.util.Log
import java.io.*
import java.util.*
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.content.IntentFilter
import android.content.BroadcastReceiver

/**
 * Camera Helper + Trigger Service hybrid.
 * We had to revert to this for demo.
 */
class CameraService : IntentService("CameraService") {
    private lateinit var mCameraManager: CameraManager
    private var mCamera: CameraDevice? = null
    private val TAG = "CameraService"
    private val mImageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
    private var mCaptureSession: CameraCaptureSession? = null
    private lateinit var mCaptureRequest: CaptureRequest
    private lateinit var file: File
    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null
    private var mIsRecording: Boolean = true

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "CameraService onCreate")
        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        startBackground()
        openCamera()
        file = File(getExternalFilesDir(
            Environment.DIRECTORY_PICTURES), "latest.jpg")
        mImageReader.setOnImageAvailableListener(onImageAvailable, mBackgroundHandler)

        val screenStateFilter = IntentFilter()
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON)
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(mScreenListener, screenStateFilter)

        //timer()
    }
    private fun timer() {
        Handler().postDelayed(
            {
                Log.e("tag", "This'll run 1000*30 milliseconds later")
                takePicture()
                if (mIsRecording) {
                    timer()
                }
            },
            1000*30
        )
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "CameraService Destroy")
        closeCamera()
        stopBackground()
        unregisterReceiver(mScreenListener)
    }
    //--------- Interface ---------
    /**
     * Creates an image capture request and saves it to latest image file.
     */
    fun takePicture() {
        try {
            if (mCaptureSession == null) {
                Log.e(TAG, "Take Picture: Session is closed")
                mIsRecording = false
                return
            }
            mCaptureSession?.apply {

                stopRepeating()
                abortCaptures()
                capture(mCaptureRequest,
                    object: CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(session: CameraCaptureSession,
                                                        request: CaptureRequest,
                                                        result: TotalCaptureResult) {
                            Log.i(TAG, "Capture Completed")
                            Intent().also { intent ->
                                intent.setAction(MESSAGE_CAPTURE_COMPLETED)
                                sendBroadcast(intent)
                            }

                        }
                    }, mBackgroundHandler)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }
    fun startRecording() {
        openCamera()

    }
    fun stopRecording() {
        closeCamera()
    }
    //-------- Camera State Functions --------
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            mCameraManager.openCamera(mCameraManager.getCameraIdList()[1], mCameraStateCallback, mBackgroundHandler)
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        }
    }
    private fun closeCamera() {
        mCamera?.close()
        mCamera = null
    }
    /**
     * Creates a camera Capture Session.
     * Only create once after camera gets opened.
     */
    private fun createSession() {
        try {
            mCamera?.createCaptureSession(
                Arrays.asList(mImageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        if (mCamera == null) return
                        mCaptureSession = cameraCaptureSession
                        val captureBuilder = mCamera!!.createCaptureRequest(
                            CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                            addTarget(mImageReader.surface)
                            // Use the same AE and AF modes as the preview.
                            //set(CaptureRequest.CONTROL_AF_MODE,
                            //    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            //set(CaptureRequest.JPEG_ORIENTATION, mCameraManager.getCameraCharacteristics(mCamera!!.id).get(CameraCharacteristics.SENSOR_ORIENTATION))
                            set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(mCameraManager.getCameraCharacteristics(mCamera!!.id), 90))
                            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        }
                        mCaptureRequest = captureBuilder.build()
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Create Session Failure")
                    }
                }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }
    private fun getJpegOrientation(c: CameraCharacteristics, deviceOrientation_par: Int): Int {
        var deviceOrientation = deviceOrientation_par
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90
        // Reverse device orientation for front-facing cameras
        val facingFront =
            c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation
        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360
    }
    private val mCameraStateCallback = object: CameraDevice.StateCallback() {
        override fun onOpened(p0: CameraDevice) {
            Log.d(TAG, "CameraDevice Opened")
            mCamera = p0;
            createSession()
        }
        override fun onDisconnected(p0: CameraDevice) {
            Log.d(TAG, "CameraDevice Disconnected")
        }
        override fun onError(p0: CameraDevice, p1: Int) {
            Log.e(TAG, "CameraDevice Error: " + p1.toString())
        }

        override fun onClosed(camera: CameraDevice) {
            super.onClosed(camera)
            Log.d(TAG, "CameraDevice closed")
            mCamera = null
            mCaptureSession = null
        }
    }
    private val onImageAvailable = object: ImageReader.OnImageAvailableListener {
        /**
         * Fired when latest image request is completed.
         * Saving and then reading the image can be done here.
         */
        override fun onImageAvailable(reader: ImageReader) {
            Log.i("ImageReader", "Image Available");
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                val buffer = image!!.getPlanes()[0].getBuffer()
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                saveImage(bytes)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (image != null) {
                    image.close()
                }
            }
        }
        /**
         * Saves any byte array to the latest image file.
         * Used only for images.
         */
        @Throws(IOException::class)
        private fun saveImage(bytes: ByteArray) {
            var output: OutputStream? = null
            try {
                output = FileOutputStream(file)
                output.write(bytes)
            } finally {
                if (null != output) {
                    output.close()
                }
            }
        }
    }
    override fun onHandleIntent(p0: Intent?) {
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
    /**
     * A handler to handle requests to the service from app.
     */
    private val ServiceHandler = object: Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.arg1) {
                REQUEST_TAKE_A_PICTURE -> {
                    takePicture()
                }
                REQUEST_START_RECORDING -> {
                    startRecording()
                }
                REQUEST_STOP_RECORDING -> {
                    stopRecording()
                }
            }
        }
    }
    // Not so important stuff
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        makeForeground()
        return Service.START_STICKY
    }
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
                .setContentTitle("Deprecated Notification")
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .build()
        )
    }
    // ???
    val mMessenger = Messenger(ServiceHandler)
    override fun onBind(intent: Intent): IBinder? {
        //return mBinder
        return mMessenger.binder
    }
    companion object {
        //Requests
        val REQUEST_TAKE_A_PICTURE = 1
        val REQUEST_START_RECORDING = 2
        val REQUEST_STOP_RECORDING = 3
        //Broadcasted Messages
        val MESSAGE_CAPTURE_COMPLETED = "com.oguzhan.MESSAGE_CAPTURE_COMPLETED"

        //Screen Listener
        val mScreenListener = object: BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                Log.e("CamServ", "Intent received")
            }
        }
    }
}