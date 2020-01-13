package com.oguzhan.emotionrecorder.deprecated

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.StateCallback
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.util.Log
import android.view.Surface
import java.io.*

/**
 * Camera Helper Service without a trigger. Could not make it to demo.
 */
class EmotionCam : IntentService("EmotionCamService") {
    private var mCamera: CameraDevice? = null
    lateinit var mCameraManager: CameraManager

    var mBackgroundThread: HandlerThread? = null
    var mBackgroundHandler: Handler? = null
    var mSession: CameraCaptureSession? = null
    var captureBuilder: CaptureRequest.Builder? = null
    val Messenger: Messenger = Messenger(ServiceHandler())

    companion object {
        val TAKE_PIC: Int = 1
    }

    private val TAG: String = "EmotionCamService"
    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent")

        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        closeCamera()
        stopBackgroundThread()
        startBackgroundThread()
        openCamera()
    }
    internal inner class ServiceHandler : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                TAKE_PIC -> {
                    Log.e("HANDLER",""+msg.arg1)
                    takeIt()
                    //takePicture()
                }
            }
            Log.e("HANDLER", msg.toString())
            //closeCamera()
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand"+intent.toString()+startId)

        return super.onStartCommand(intent, flags, startId)
    }
    override fun startService(service: Intent?): ComponentName? {
        Log.d(TAG, "Service Start")
        return super.startService(service)
    }

    override fun stopService(name: Intent?): Boolean {
        Log.d(TAG, "Service Stop")
        return super.stopService(name)
    }
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            mCameraManager.openCamera(mCameraManager.getCameraIdList()[0], object: StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    Log.d(TAG, "Opened cam")
                    mCamera = p0;
                    takePicture()
                }
                override fun onDisconnected(p0: CameraDevice) {
                    Log.d(TAG, "Camera disconnected. Reconnecting.")
                    closeCamera()
                    //TODO: Remove this callback?
                    openCamera()
                }
                override fun onError(p0: CameraDevice, p1: Int) {
                    Log.e(TAG, "Camera Open Error No:" + p1.toString())
                }
            }, mBackgroundHandler);
        } catch (ex: CameraAccessException) {
            ex.printStackTrace();
        }
    }
    private fun takePicture() {
        if(null == mCamera) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        (mCamera as CameraDevice).apply {
            val file = File(getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), "latest.jpg")
            Log.e(TAG, file.absolutePath)
            captureBuilder = createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            val reader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
            val outputSurfaces = ArrayList<Surface>(2)
            outputSurfaces.add(reader.surface)
            captureBuilder!!.addTarget(reader.surface);
            captureBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            reader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener {
                override fun onImageAvailable(reader: ImageReader) {
                    Log.e("READER", "Image Available");
                    var image: Image? = null
                    try {
                        image = reader.acquireLatestImage()
                        val buffer = image!!.getPlanes()[0].getBuffer()
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)
                        save(bytes)
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
                @Throws(IOException::class)
                private fun save(bytes: ByteArray) {
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
            }, mBackgroundHandler)
            createCaptureSession(outputSurfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    Log.e(TAG, "Capture Part 1");
                    mSession = session
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "CameraCaptureSession Configure Failed")
                }
            }, mBackgroundHandler)
        }
    }
    fun takeIt() {
        try {
            mSession!!.capture(captureBuilder!!.build(), object: CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result);
                    Log.e(TAG, "Capture Completed");
                }
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
    protected fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.getLooper())
    }
    protected fun stopBackgroundThread() {
        if (mBackgroundHandler != null && mBackgroundThread != null) {
            mBackgroundThread!!.quitSafely()
            try {
                mBackgroundThread!!.join()
                mBackgroundThread = null
                mBackgroundHandler = null
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
    private fun closeCamera() {
        if (null != mCamera) {
            (mCamera as CameraDevice).close()
            mCamera = null
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        Log.e(TAG, "BIND")
        return Messenger.binder;
    }
}
