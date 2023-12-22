/*
 * Copyright 2023 AutoPilot AI. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.autopilotai.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.internal.synchronized
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.*
import org.jetbrains.kotlinx.multik.api.math.argMax
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros

class CaptionHelper(
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context,
    var maxResults: Int = 35,
    val captionListerer: CaptionListener?
) {

    private var interpreter_VGG: Interpreter? = null

    private var outputCategoryCount_VGG = Any()
    private var inputHeight_VGG: Int = 0
    private var inputWidth_VGG: Int = 0
    private var inputState_VGG = HashMap<String, Any>()

    private var interpreter: Interpreter? = null

    private var outputCategoryCount = Any()
    private var inputHeight: Int = 0
    private var inputWidth: Int = 0
    private var inputState = HashMap<String, Any>()

    //This lock guarantees that only one thread is performing training and
    //inference at any point in time.
    private val lock = Any()

    init {
        //setupObjectDetector()
        if (setupCaptionModel()) {
            outputCategoryCount_VGG = interpreter_VGG!!
                .getOutputTensorFromSignature(VGG_OUTPUT_NAME, SIGNATURE_KEY)
                .shape()[1]
            inputHeight_VGG = interpreter_VGG!!
                .getInputTensorFromSignature(VGG_INPUT_NAME, SIGNATURE_KEY)
                .shape()[2]
            inputWidth_VGG = interpreter_VGG!!
                .getInputTensorFromSignature(VGG_INPUT_NAME, SIGNATURE_KEY)
                .shape()[3]
            inputState_VGG = HashMap<String, Any>()
            inputState_VGG = initializeVGGInput()

            outputCategoryCount = interpreter!!
                .getOutputTensorFromSignature(OUTPUT_NAME, SIGNATURE_KEY)
                .shape()[1]
            inputHeight = interpreter!!
                .getInputTensorFromSignature(INPUT_NAME1, SIGNATURE_KEY)
                .shape()[2]
            inputWidth = interpreter!!
                .getInputTensorFromSignature(INPUT_NAME1, SIGNATURE_KEY)
                .shape()[3]
            inputState = initializeInput()
        } else {
            captionListerer?.onError("TFLite failed to init.")
        }
    }

    /**
     * Close the interpreter when it's no longer needed.
     */
    fun close() {
        interpreter_VGG = null
        interpreter = null
    }

    /**
     * Clear the internal state of the model.
     *
     * Call this function if the future inputs is unrelated to the past inputs. (e.g. when changing
     * to a new video sequence)
     */
    fun reset() {
        // Ensure that no inference is running when the state is being cleared.
        synchronized(lock) {
            inputState_VGG = initializeVGGInput()
            inputState = initializeInput()
        }
    }

    private fun setupCaptionModel(): Boolean {
        val options = Interpreter.Options()
        options.numThreads = numThreads
        return try {
            val modelFile = FileUtil.loadMappedFile(context, "my_model.tflite")
            interpreter = Interpreter(modelFile, options)
            val modelFileVGG = FileUtil.loadMappedFile(context, "vgg_model.tflite")
            interpreter_VGG = Interpreter(modelFileVGG, options)
            true
        } catch (e: IOException) {
            captionListerer?.onError(
                "Model personalization failed to " +
                        "initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
            false
        }
    }

    /**
     * Initialize the input objects and fill them with zeros.
     */
    private fun initializeVGGInput(): HashMap<String, Any> {
        val inputs = HashMap<String, Any>()
        for (inputName in interpreter_VGG!!.getSignatureInputs(SIGNATURE_KEY)) {
            // Skip the input image tensor as it'll be fed in later.
            if (inputName.equals(VGG_INPUT_NAME))
                continue

            // Initialize a ByteBuffer filled with zeros as an initial input of the TFLite model.
            val tensor = interpreter_VGG!!.getInputTensorFromSignature(inputName, SIGNATURE_KEY)
            val byteBuffer = ByteBuffer.allocateDirect(tensor.numBytes())
            byteBuffer.order(ByteOrder.nativeOrder())
            inputs[inputName] = byteBuffer
        }

        return inputs
    }

    /**
     * Initialize the output objects to store the TFLite model outputs.
     */
    private fun initializeVGGOutput(): HashMap<String, Any> {
        val outputs = HashMap<String, Any>()
        for (outputName in interpreter_VGG!!.getSignatureOutputs(SIGNATURE_KEY)) {
            // Initialize a ByteBuffer to store the output of the TFLite model.
            val tensor = interpreter_VGG!!.getOutputTensorFromSignature(outputName, SIGNATURE_KEY)
            val byteBuffer = ByteBuffer.allocateDirect(tensor.numBytes())
            byteBuffer.order(ByteOrder.nativeOrder())
            outputs[outputName] = byteBuffer
        }

        return outputs
    }

    /**
     * Initialize the input objects and fill them with zeros.
     */
    private fun initializeInput(): HashMap<String, Any> {
        val inputs = HashMap<String, Any>()
        for (inputName in interpreter!!.getSignatureInputs(SIGNATURE_KEY)) {
            // Skip the input image tensor as it'll be fed in later.
            if (inputName.equals(INPUT_NAME1)) //TAKE A LOOK AT THIS LOGIC
                continue

            // Initialize a ByteBuffer filled with zeros as an initial input of the TFLite model.
            val tensor = interpreter!!.getInputTensorFromSignature(inputName, SIGNATURE_KEY)
            val byteBuffer = ByteBuffer.allocateDirect(tensor.numBytes())
            byteBuffer.order(ByteOrder.nativeOrder())
            inputs[inputName] = byteBuffer
        }

        return inputs
    }

    /**
     * Initialize the output objects to store the TFLite model outputs.
     */
    private fun initializeOutput(): HashMap<String, Any> {
        val outputs = HashMap<String, Any>()
        for (outputName in interpreter!!.getSignatureOutputs(SIGNATURE_KEY)) {
            // Initialize a ByteBuffer to store the output of the TFLite model.
            val tensor = interpreter!!.getOutputTensorFromSignature(outputName, SIGNATURE_KEY)
            val byteBuffer = ByteBuffer.allocateDirect(tensor.numBytes())
            byteBuffer.order(ByteOrder.nativeOrder())
            outputs[outputName] = byteBuffer
        }

        return outputs
    }

    // Invokes inference on the given image batches.
    fun caption(bitmap: Bitmap, rotation: Int) {
        processInputImage(bitmap, rotation)?.let { image ->
            synchronized(lock) {
                if (interpreter_VGG == null || interpreter == null) {
                    setupCaptionModel()
                }

                // Inference time is the difference between the system time at the start and finish of the
                // process
                var inferenceTime = SystemClock.uptimeMillis()

                inputState_VGG[VGG_INPUT_NAME] = image.buffer

                // Initialize a placeholder to store the output objects.
                val outputs_VGG = initializeVGGOutput()
                val outputs = initializeOutput()

                // Post-process the outputs.
                var attributes = TensorBuffer.createFixedSize(
                    intArrayOf(1, 4),
                    DataType.FLOAT32
                )
                outputs_VGG[VGG_OUTPUT_NAME] = attributes.buffer

                // Run inference using the TFLite model.
                interpreter_VGG?.runSignature(inputState_VGG, outputs_VGG)

                var sequence: Int = 0
                var in_text = "startseq"
                for (i in 0..maxResults) {
                    //integer encode input sequence
                    sequence = tokenizer.texts_to_sequences([in_text])[0]
                    //pad input
                    sequence = pad_sequences([sequence], maxlen=maxResults)

                    //predict next work
                    inputState[INPUT_NAME1] = attributes
                    inputState[INPUT_NAME2] = sequence
                    var yhat = TensorBuffer.createFixedSize(
                        intArrayOf(1, 4),
                        DataType.FLOAT32
                    )
                    outputs[OUTPUT_NAME] = yhat.buffer
                    interpreter?.runSignature(inputState, outputs)

                    //convert probability to integer
                    yhat = argmax(yhat)
                    //map integer to word
                    var word = word_for_id(yhat, tokenizer)
                    if (word == "None")
                        break
                    //append as input for generating the next word
                    in_text += ' ' + word
                    //we will stop if we predict the endseq
                    if (word == "endseq")
                        break
                }

                inferenceTime = SystemClock.uptimeMillis() - inferenceTime
                captionListerer?.onResults(in_text, inferenceTime)
            }
        }
    }

    fun argmax(a: NDArray<Double>, axis: Int? = null, out: NDArray<Int>? = null, keepdims: Boolean = false): NDArray<Int> {
        val reducedAxis = axis ?: if (keepdims) null else a.argmax()
        val resultShape = if (keepdims) {
            val shapeList = a.shape.toMutableList()
            reducedAxis?.let { shapeList[it] = 1 }
            shapeList.toIntArray()
        } else {
            val shapeList = a.shape.toMutableList()
            reducedAxis?.let { shapeList.removeAt(it) }
            shapeList.toIntArray()
        }

        val resultArray = out ?: zeros(*resultShape, dtype = Int::class)

        // Calculate the argmax along the specified axis
        for (index in resultArray.iterateIndices()) {
            val slicedArray = a.slice(index)
            val maxIndex = slicedArray.argmax()
            resultArray[index] = maxIndex
        }

        return resultArray
    }

    fun padSequences(
        sequences: List<List<Int>>,
        maxlen: Int? = null,
        dtype: DType<Int> = Int::class,
        padding: String = "pre",
        truncating: String = "pre",
        value: Int = 0
    ): NDArray<Int> {
        require(sequences.isNotEmpty()) { "Input sequences must not be empty." }

        val numSamples = sequences.size

        // Find the maximum sequence length if not provided
        val lengths = sequences.map { it.size }
        val maxLength = maxlen ?: lengths.maxOrNull() ?: 0

        // Create an empty array of the specified dtype with shape (numSamples, maxLength)
        val x = zeros(numSamples, maxLength, dtype)

        for (idx in 0 until numSamples) {
            val s = sequences[idx]

            // Handle truncation
            val trunc = when (truncating) {
                "pre" -> s.takeLast(maxLength)
                "post" -> s.take(maxLength)
                else -> throw IllegalArgumentException("Invalid truncating type: $truncating")
            }

            // Handle padding
            val startIdx = when (padding) {
                "pre" -> maxLength - trunc.size
                "post" -> 0
                else -> throw IllegalArgumentException("Invalid padding type: $padding")
            }

            for (i in 0 until trunc.size) {
                x[idx, startIdx + i] = trunc[i]
            }
        }

        return x
    }

    // Pad the given sequence to maxlen with zeros.
    fun pad_sequences ( sequence : IntArray ) : IntArray {
        val maxlen = this.maxlen
        if ( sequence.size > maxlen ) {
            return sequence.sliceArray( 0..maxlen )
        }
        else if ( sequence.size < maxlen ) {
            val array = ArrayList<Int>()
            array.addAll( sequence.asList() )
            for ( i in array.size until maxlen ){
                array.add(0)
            }
            return array.toIntArray()
        }
        else{
            return sequence
        }
    }

    private fun word_for_id(
        integer: Int,
        tokenizer: Any
    ): String {
        for ((word, index) in tokenizer.word_index.items()){
            if (index == integer)
                return word
        }
        return "None"
    }

    // Preprocess the image and convert it into a TensorImage for classification.
    private fun processInputImage(
        image: Bitmap,
        imageRotation: Int
    ): TensorImage? {
        val height = image.height
        val width = image.width
        val cropSize = min(height, width)
        val imageProcessor = ImageProcessor.Builder()
            .add(Rot90Op(-imageRotation / 90))
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(
                ResizeOp(
                    inputHeight_VGG,
                    inputWidth_VGG,
                    ResizeOp.ResizeMethod.BILINEAR
                )
            )
            .add(NormalizeOp(0f, 255f))
            .build()
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(image)
        return imageProcessor.process(tensorImage)
    }

    interface CaptionListener {
        fun onError(error: String)
        fun onResults(results: String, inferenceTime: Long)
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTDETV0 = 1
        const val MODEL_EFFICIENTDETV1 = 2
        const val MODEL_EFFICIENTDETV2 = 3
        const val MODEL_CUSTOM = 4

        const val VGG_INPUT_NAME = "input_4"
        const val VGG_OUTPUT_NAME = "fc2"
        const val INPUT_NAME1 = "input_2"
        const val INPUT_NAME2 = "input_3"
        const val OUTPUT_NAME = "dense_2"
        const val SIGNATURE_KEY = "serving_default"
        const val INPUT_MEAN = 0f
        const val INPUT_STD = 255f
    }
}
