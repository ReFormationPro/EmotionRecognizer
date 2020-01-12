/* Copyright 2018 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.oguzhan.emotionrecorder.tflitedemo

import android.app.Activity
import android.util.Log
import java.io.IOException

/** This classifier works with the float MobileNet model.  */
//class ImageClassifierFloatMobileNet
/**
 * Initializes an `ImageClassifierFloatMobileNet`.
 *
 * @param activity
 */
class ImageClassifierFloatMobileNet(activity: Activity) : ImageClassifier(activity) {
    override val modelPath: String
        get() = "converted_model.tflite"

    /**
     * An array to hold inference results, to be feed into Tensorflow Lite as outputs. This isn't part
     * of the super class, because we need a primitive array here.
     */
    private var labelProbArray: Array<FloatArray>? = null

    init {
        labelProbArray = Array(1) { FloatArray(numLabels) }
        Log.e("Label Prob Array", numLabels.toString())
    }

    override val labelPath: String
        get() = "labels_emotion.txt"

    override val imageSizeX: Int
    get() = 48

    override val imageSizeY: Int
    get() = 48

    override val numBytesPerChannel: Int
        get() = 4

    override fun addPixelValue(pixelValue: Int) {
        val ret =
            ((pixelValue shr 16 and 0xFF) + (pixelValue shr 8 and 0xFF) + (pixelValue and 0xFF)).toFloat() / 3.0f / 127.0f
        imgData!!.putFloat(ret - 1.0f)
    }

    override fun getProbability(labelIndex: Int): Float {
        return labelProbArray!![0][labelIndex]
    }

    override fun setProbability(labelIndex: Int, value: Number) {
        labelProbArray!![0][labelIndex] = value.toFloat()
    }

    override fun getNormalizedProbability(labelIndex: Int): Float {
        return labelProbArray!![0][labelIndex]
    }

    override fun runInference() {
        tflite!!.run(imgData, labelProbArray)
    }
}
