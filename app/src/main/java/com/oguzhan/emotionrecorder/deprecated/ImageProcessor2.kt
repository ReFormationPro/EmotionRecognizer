package com.oguzhan.emotionrecorder.deprecated

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.IOException
import java.nio.MappedByteBuffer
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.TensorProcessor
import java.lang.Exception
import java.nio.ByteBuffer

//Image
class ImageProcessor2(val mContext: Context) {
    private val mImageWidth = 48
    private val mImageHeight = 48

    private val mImageProcessor: ImageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(mImageHeight, mImageWidth, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    fun processImage(image: Bitmap): Map<String, Float>? {
        var tImage = TensorImage(DataType.UINT8)
        tImage.load(image);
        tImage = mImageProcessor.process(tImage);

        val probabilityBuffer = TensorBuffer.createFixedSize(intArrayOf(7), DataType.FLOAT32)

        // Initialise the model
        lateinit var tfliteModel: MappedByteBuffer
        lateinit var tflite: Interpreter
        try{
            tfliteModel = FileUtil.loadMappedFile(mContext,
                "converted_model.tflite");
            tflite = Interpreter(tfliteModel)
        } catch (e:Exception){
            Log.e("tfliteSupport", "Error reading model", e);
        }

        // Running inference
        if(null != tflite) {
            tflite.run(tImage.getBuffer(), probabilityBuffer.getBuffer());
        }

        val ASSOCIATED_AXIS_LABELS = "labels_emotion.txt"
        var associatedAxisLabels: List<String>? = null

        try {
            associatedAxisLabels = FileUtil.loadLabels(mContext, ASSOCIATED_AXIS_LABELS)
        } catch (e: IOException) {
            Log.e("tfliteSupport", "Error reading label file", e)
        }

        // Post-processor which dequantize the result
        val probabilityProcessor = TensorProcessor.Builder().add(NormalizeOp(0f, 255f)).build()

        if (null != associatedAxisLabels) {
            // Map of labels and their corresponding probability
            val labels = TensorLabel(
                associatedAxisLabels,
                probabilityProcessor.process(probabilityBuffer)
            )

            // Create a map to access the result based on label
            val floatMap = labels.mapWithFloatValue
            return floatMap
        }
        return null
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
    fun addPixelValue(pixelValue: Int, imgData: ByteBuffer) {
        val ret =
            ((pixelValue shr 16 and 0xFF) + (pixelValue shr 8 and 0xFF) + (pixelValue and 0xFF)).toFloat() / 3.0f / 127.0f
        imgData!!.putFloat(ret - 1.0f)
    }
}