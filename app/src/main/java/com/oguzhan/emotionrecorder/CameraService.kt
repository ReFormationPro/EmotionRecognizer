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
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.app.PendingIntent
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import androidx.core.app.NotificationCompat
import android.content.IntentFilter
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.content.BroadcastReceiver


class CameraService(mContext: Context) {
    private lateinit var mCameraManager: CameraManager
    private var mCamera: CameraDevice? = null
    private val TAG = "CameraService"
    private val mImageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
    private var mCaptureSession: CameraCaptureSession? = null
    private lateinit var mCaptureRequest: CaptureRequest

    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null
    private var mIsRecording: Boolean = true

    init {
        mCameraManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        startBackground()
        openCamera()
        mImageReader.setOnImageAvailableListener(onImageAvailable, mBackgroundHandler)
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
    fun finishSafely() {
        closeCamera()
        stopBackground()
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
    private fun getJpegOrientation(c: CameraCharacteristics, deviceOrientation: Int): Int {
        var deviceOrientation = deviceOrientation
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
    companion object {
        //Requests
        val REQUEST_TAKE_A_PICTURE = 1
        val REQUEST_START_RECORDING = 2
        val REQUEST_STOP_RECORDING = 3
        //Broadcasted Messages
        val MESSAGE_CAPTURE_COMPLETED = "com.oguzhan.MESSAGE_CAPTURE_COMPLETED"
        //
        val file: File = File("MANA", "latest.jpg")
        val onImageAvailable = object: ImageReader.OnImageAvailableListener {
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
    }
}