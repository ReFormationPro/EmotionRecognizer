package com.oguzhan.emotionrecorder.delayed;

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.util.Log
import java.io.*
import java.util.*

class CameraInterface (val mImageFile: File, val mCameraManager: CameraManager) {
    //private lateinit var mCameraManager: CameraManager
    private var mCamera: CameraDevice? = null
    private val TAG = "CameraService"
    private val mImageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
    private var mCaptureSession: CameraCaptureSession? = null
    private lateinit var mCaptureRequest: CaptureRequest
    //private lateinit var mImageFile: File
    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null


    @SuppressLint("MissingPermission")
    fun openCamera() {
        try {
            mCameraManager.openCamera(mCameraManager.getCameraIdList()[1], mCameraStateCallback, mBackgroundHandler)
        } catch (ex: CameraAccessException) {
            ex.printStackTrace()
        }
    }
    fun destroy() {
        mCamera?.close()
        mCamera = null
        stopBackground()
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
                output = FileOutputStream(mImageFile)
                output.write(bytes)
            } finally {
                if (null != output) {
                    output.close()
                }
            }
        }
    }
    init {
        Log.i(TAG, "CameraInterface onCreate")
        //mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        startBackground()
        openCamera()
        //mImageFile = File(getExternalFilesDir(
        //    Environment.DIRECTORY_PICTURES), "latest.jpg")
        mImageReader.setOnImageAvailableListener(onImageAvailable, mBackgroundHandler)
    }
    // Background Thread Functions
    private fun startBackground() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.getLooper())
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
}
