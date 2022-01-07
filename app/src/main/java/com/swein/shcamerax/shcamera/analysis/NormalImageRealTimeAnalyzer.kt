package com.swein.shcamerax.shcamera.analysis

import android.graphics.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val data = ByteArray(remaining())
    get(data)
    return data
}

class NormalImageRealTimeAnalyzer(private val normalImageRealTimeAnalyzerDelegate: NormalImageRealTimeAnalyzerDelegate) : ImageAnalysis.Analyzer {

    interface NormalImageRealTimeAnalyzerDelegate {
        fun onBitmap(bitmap: Bitmap, degree: Int)
    }

    companion object {
        private const val TAG = "NormalImageRealTimeAnalyzer"
    }

    private val yuvFormats = mutableListOf(ImageFormat.YUV_420_888)

    private var finish: Boolean = false

    init {
        yuvFormats.addAll(listOf(ImageFormat.YUV_422_888, ImageFormat.YUV_444_888))
    }

    override fun analyze(image: ImageProxy) {

        if(finish) {
            return
        }

        // We are using YUV format because, ImageProxy internally uses ImageReader to get the image
        // by default ImageReader uses YUV format unless changed.
        if (image.format !in yuvFormats) {
            return
        }

        val bitmap = toBitmap(image)

        normalImageRealTimeAnalyzerDelegate.onBitmap(bitmap, image.imageInfo.rotationDegrees)
        image.close()
    }

    private fun toBitmap(image: ImageProxy): Bitmap {

        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
