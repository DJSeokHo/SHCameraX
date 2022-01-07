package com.swein.shcamerax.shcamera.analysis

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import java.util.concurrent.ExecutorService

object SHCameraAnalysis {

    fun initImageAnalysis(cameraExecutor: ExecutorService, screenAspectRatio: Int, rotation: Int): ImageAnalysis {

        return ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, NormalImageRealTimeAnalyzer(object :
                    NormalImageRealTimeAnalyzer.NormalImageRealTimeAnalyzerDelegate {
                    override fun onBitmap(bitmap: Bitmap, degree: Int) {

//                context?.let {
//                    val photoFilePath = createFilePath(getOutputDirectory(it), PHOTO_EXTENSION)
//
//                    ILog.debug(TAG, photoFilePath)
//
//                    val bufferedOutputStream = BufferedOutputStream(FileOutputStream(File(photoFilePath)))
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferedOutputStream)
//
//                    BitmapUtil.compressImageWithFilePath(photoFilePath, 1, degree)
//
//                    ThreadUtil.startUIThread(0) {
//
//                        imageView.setImageBitmap(BitmapUtil.rotate(bitmap, degree))
//
//                    }
//                }

                    }
                }))
            }
    }

}