package com.oguzhan.emotionrecorder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.ArrayList

class EmotionRecognizer(val mContext: Context) {
    private val tfliteOptions = Interpreter.Options()
    private var tfliteModel: MappedByteBuffer? = null
    private var tflite: Interpreter
    private lateinit var mImgData: ByteBuffer
    private val mImageWidth = 48
    private val mImageHeight = 48
    private var mFilterLabelProbability: Array<FloatArray>
    private var mLabelList: List<String> //Array<Float>
    private val mFilterFactor =  0.4f
    private val mFilterStages = 3
    private val TAG = "Image Processor 3"
    private var mLabelProbArray: Array<FloatArray>? = null
    private val mFaceDetector: FaceDetector

    init {
        tfliteModel = loadModelFile( "converted_model.tflite")
        tflite = Interpreter(tfliteModel!!, tfliteOptions)
        mLabelList = loadLabelList("labels_emotion.txt" )
        mFilterLabelProbability = Array(3) { FloatArray(3){0f} }
        mFaceDetector = FaceDetector.Builder(mContext)
            .setProminentFaceOnly(true)
            .setMode(FaceDetector.FAST_MODE)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.NO_LANDMARKS)
            .build()
    }
    fun analyzeImage(bitmap: Bitmap):HashMap<String, Float>? {
        val frame = Frame.Builder().setBitmap(bitmap).build()
        val faces = mFaceDetector.detect(frame)
        val face = faces[0]
        if (face == null) {
            Log.v(TAG, "No Face found")
            return null
        } else {
            Log.v(TAG, "Face found: x"+ face.getPosition().x + " y" + face.getPosition().y + " x" + (face.getPosition().x+face.width) + " y" + (face.getPosition().y+face.height))
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
        Canvas(resultBmp).drawBitmap(bitmap, -rect.left.toFloat(), -rect.top.toFloat(), null)
        val resized = Bitmap.createScaledBitmap(resultBmp, 48, 48, true)
        preprocessImage(resized)
        return classifyFrame(resized)
    }
    fun finishSafely() {
        mFaceDetector.release()
    }
    // ---- Recognition Code ------
    fun preprocessImage(bitmap: Bitmap, numBytesPerChannel: Int = 4) {
        mFilterLabelProbability = Array(3) { FloatArray(7){0f} }
        mLabelProbArray = Array(1) { FloatArray(mLabelList.size) }
        mImgData = ByteBuffer.allocateDirect(mImageWidth * mImageHeight * numBytesPerChannel)
        mImgData!!.order(ByteOrder.nativeOrder())
    }
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileDescriptor = mContext.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    private fun loadLabelList(labelPath: String): List<String> {
        val labelList = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(mContext.assets.open(labelPath)))
        var line: String? = reader.readLine()
        while (line != null) {
            labelList.add(line)
            line = reader.readLine()
        }
        reader.close()
        return labelList
    }
    private fun convertBitmapToByteBuffer(bitmap: Bitmap, imgData: ByteBuffer) {
        if (imgData == null) {
            return
        }
        imgData!!.rewind()
        val intValues = IntArray(mImageHeight*mImageWidth)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point.
        var pixel = 0
        for (i in 0 until mImageWidth) {
            for (j in 0 until mImageHeight) {
                val `val` = intValues[pixel++]
                addPixelValue(`val`, imgData)
            }
        }
    }
    private fun addPixelValue(pixelValue: Int, imgData: ByteBuffer) {
        val ret =
            ((pixelValue shr 16 and 0xFF) + (pixelValue shr 8 and 0xFF) + (pixelValue and 0xFF)).toFloat() / 3.0f / 127.0f
        imgData!!.putFloat(ret - 1.0f)
    }
    private fun applyFilter() {
        val numLabels = mLabelList.size
        // Low pass filter `labelProbArray` into the first stage of the filter.
        for (j in 0 until numLabels) {
            mFilterLabelProbability!![0][j] += mFilterFactor * (getProbability(j) - mFilterLabelProbability!![0][j])
        }
        // Low pass filter each stage into the next.
        for (i in 1 until mFilterStages) {
            for (j in 0 until numLabels) {
                mFilterLabelProbability!![i][j] += mFilterFactor * (mFilterLabelProbability!![i - 1][j] - mFilterLabelProbability!![i][j])
            }
        }
        // Copy the last stage filter output back to `labelProbArray`.
        for (j in 0 until numLabels) {
            setProbability(j, mFilterLabelProbability!![mFilterStages - 1][j])
        }
    }
    fun classifyFrame(bitmap: Bitmap): HashMap<String, Float> {
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
        }
        convertBitmapToByteBuffer(bitmap, mImgData)
        tflite!!.run(mImgData, mLabelProbArray)
        applyFilter()
        val result = HashMap<String, Float>()
        for (i in 0 until mLabelList.size) {
            result.put(mLabelList[i], mLabelProbArray!![0][i])
        }
        //------DEBUG-------
        var max1 = 0f
        var max2 = 0f
        var str1 = ""
        var str2 = ""
        for (i in 0 until mLabelList.size) {
            if (getNormalizedProbability(i) > max1) {
                max2 = max1
                str2 = str1
                max1 = getNormalizedProbability(i)
                str1 = mLabelList[i]
            }
        }
        Log.e("TEST", "Max: " + max1 + " Str:" + str1 + ", Max: " + max2 + " Str: " + str2 + " " + result.size)
        return result
    }
    private fun getProbability(labelIndex: Int): Float {
        return mLabelProbArray!![0][labelIndex]
    }

    private fun setProbability(labelIndex: Int, value: Number) {
        mLabelProbArray!![0][labelIndex] = value.toFloat()
    }

    private fun getNormalizedProbability(labelIndex: Int): Float {
        return mLabelProbArray!![0][labelIndex]
    }
}