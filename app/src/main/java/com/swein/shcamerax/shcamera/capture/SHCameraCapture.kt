package com.swein.shcamerax.shcamera.capture

import androidx.camera.core.ImageCapture

object SHCameraCapture {

    fun initImageCapture(screenAspectRatio: Int, rotation: Int, flash: Boolean): ImageCapture {

        val flashMode = if(flash) {
            ImageCapture.FLASH_MODE_AUTO
        }
        else {
            ImageCapture.FLASH_MODE_OFF
        }

        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
    }

}