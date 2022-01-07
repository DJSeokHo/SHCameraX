package com.swein.shcamerax.shcamera.controller

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.swein.shcamerax.R

@SuppressLint("ViewConstructor")
class SHCameraControllerView(
    context: Context,
    private val onShutter: () -> Unit,
    private val onFlash: () -> Unit,
    private val onSwitch: () -> Unit
): ConstraintLayout(context) {

    companion object {

        private const val TAG = "SHCameraControllerView"

        fun createController(
            context: Context,
            onShutter: () -> Unit,
            onFlash: () -> Unit,
            onSwitch: () -> Unit
        ): SHCameraControllerView {
            return SHCameraControllerView(context, onShutter, onFlash, onSwitch).apply {
                this.tag = TAG
            }
        }

        fun updateFlashImage(parent: ViewGroup, flash: Boolean) {
            parent.findViewWithTag<SHCameraControllerView>(TAG)?.also {
                it.updateFlashImage(flash)
            }
        }

        fun enableSwitcher(parent: ViewGroup, enable: Boolean) {
            parent.findViewWithTag<SHCameraControllerView>(TAG)?.also {
                it.enableSwitcher(enable)
            }
        }

    }

    private lateinit var viewShutter: View
    private lateinit var imageViewFlash: ImageView
    private lateinit var imageViewSwitch: ImageView

    init {
        inflate(context, R.layout.view_s_h_camera_controller, this)
        findView()

        setListener()
    }

    private fun findView() {
        viewShutter = findViewById(R.id.viewShutter)
        imageViewFlash = findViewById(R.id.imageViewFlash)
        imageViewSwitch = findViewById(R.id.imageViewSwitch)
    }

    private fun setListener() {

        viewShutter.setOnClickListener {
            onShutter()
        }

        imageViewFlash.setOnClickListener {
            onFlash()
        }

        imageViewSwitch.setOnClickListener {
            onSwitch()
        }
    }

    private fun updateFlashImage(flash: Boolean) {

        if (flash) {
            imageViewFlash.setImageResource(R.mipmap.ti_flash_on)
        }
        else {
            imageViewFlash.setImageResource(R.mipmap.ti_flash_off)
        }
    }

    private fun enableSwitcher(enable: Boolean) {
        imageViewSwitch.isClickable = enable
        imageViewSwitch.setImageResource(if (enable) {
            R.mipmap.ti_switch_camera
        }
        else {
            R.mipmap.ti_switch_camera_disable
        })
    }
}