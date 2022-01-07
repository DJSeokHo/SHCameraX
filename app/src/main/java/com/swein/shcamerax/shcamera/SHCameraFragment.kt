package com.swein.shcamerax.shcamera

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.window.layout.WindowMetricsCalculator
import com.google.common.util.concurrent.ListenableFuture
import com.swein.shcamerax.R
import com.swein.shcamerax.framework.utility.debug.ILog
import com.swein.shcamerax.framework.utility.window.WindowUtility
import com.swein.shcamerax.shcamera.analysis.SHCameraAnalysis
import com.swein.shcamerax.shcamera.capture.SHCameraCapture
import com.swein.shcamerax.shcamera.controller.SHCameraControllerView
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class SHCameraFragment : Fragment() {

    companion object {

        private const val TAG = "SHCameraFragment"

        private fun newInstance() =
            SHCameraFragment().apply {

            }

        fun startFragment(activity: AppCompatActivity) {

            activity.supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.container, newInstance(), TAG)
                .commitAllowingStateLoss()

        }

        fun removeFragment(activity: AppCompatActivity): Boolean {
            val fragmentManager = activity.supportFragmentManager
            fragmentManager.findFragmentByTag(TAG)?.let { thisFragment ->
                fragmentManager.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .remove(thisFragment).commitAllowingStateLoss()

                return true
            }

            return false
        }

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private lateinit var frameLayoutRoot: FrameLayout

    private lateinit var previewView: PreviewView
    private lateinit var preview: Preview
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider

    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private var flash = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var displayId: Int = -1


    /** Blocking camera operations are performed using this executor */
    private var cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    /**
     * add android:configChanges="keyboardHidden|orientation|screenSize"
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bindCameraUseCases()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toggleFullScreen()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_s_h_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findView(view)
        initController()
        initCamera(
            onLensCheck = { front: Boolean, back: Boolean ->
                if (!front && !back) {
                    activity?.finish()
                }

                if (front && back) {
                    SHCameraControllerView.enableSwitcher(frameLayoutRoot, true)
                }
                else {
                    SHCameraControllerView.enableSwitcher(frameLayoutRoot, false)
                }
            }
        )
    }

    private fun findView(view: View) {
        frameLayoutRoot = view.findViewById(R.id.frameLayoutRoot)
        previewView = view.findViewById(R.id.previewView)
    }

    private fun initCamera(onLensCheck: (front: Boolean, back: Boolean) -> Unit) {

        previewView.post {

            displayId = previewView.display.displayId

            activity?.let { activity ->
                cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
                cameraProviderFuture.addListener({

                    cameraProvider = cameraProviderFuture.get()

                    onLensCheck(
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA),
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    )

                    (activity.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).registerDisplayListener(object : DisplayManager.DisplayListener {
                        override fun onDisplayAdded(displayId: Int) = Unit
                        override fun onDisplayRemoved(displayId: Int) = Unit
                        override fun onDisplayChanged(displayId: Int) = view?.let { view ->

                            if (displayId == this@SHCameraFragment.displayId) {
                                ILog.debug(TAG, "Rotation changed: ${view.display.rotation}")
                                imageCapture?.targetRotation = view.display.rotation
                                imageAnalysis?.targetRotation = view.display.rotation
                            }

                        } ?: Unit
                    }, null)

                    bindCameraUseCases()

                }, ContextCompat.getMainExecutor(activity))
            }

        }
    }

    private fun bindCameraUseCases() {

        cameraProvider.unbindAll()

        val screenAspectRatio = calculateScreenAspectRatio()
        val rotation = previewView.display.rotation
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()


        imageCapture = SHCameraCapture.initImageCapture(screenAspectRatio, rotation, flash)
        imageAnalysis = SHCameraAnalysis.initImageAnalysis(cameraExecutor, screenAspectRatio, rotation)

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )

            // Attach the viewfinder's surface provider to preview use case
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }
        catch (e: Exception) {
            e.printStackTrace()
            ILog.debug(TAG, "Use case binding failed ${e.message}")
        }
    }

    private fun initController() {

        SHCameraControllerView.createController(
            frameLayoutRoot.context,
            onShutter = {

            },
            onFlash = {
                toggleFlash()
            },
            onSwitch = {
                switchCamera()
            }
        ).apply {
            frameLayoutRoot.addView(this)
        }
    }

    private fun toggleFlash() {

        imageCapture?.let { imageCapture ->

            flash = !flash

            imageCapture.flashMode = if(flash) {
                ImageCapture.FLASH_MODE_AUTO
            }
            else {
                ImageCapture.FLASH_MODE_OFF
            }

            SHCameraControllerView.updateFlashImage(frameLayoutRoot, flash)
        }
    }

    private fun switchCamera() {

        lensFacing = if (CameraSelector.LENS_FACING_BACK == lensFacing) {
            CameraSelector.LENS_FACING_FRONT
        }
        else {
            CameraSelector.LENS_FACING_BACK
        }

        try {

            previewView.post {
                bindCameraUseCases()
            }

        }
        catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }

    }

    private fun calculateScreenAspectRatio(): Int {

        val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(requireActivity())
        val currentBounds = windowMetrics.bounds
        ILog.debug(TAG, "Screen metrics: ${currentBounds.width()} x ${currentBounds.height()}")

        val screenAspectRatio = aspectRatio(currentBounds.width(), currentBounds.height())
        ILog.debug(TAG, "Preview aspect ratio: $screenAspectRatio")

        return screenAspectRatio
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun toggleFullScreen() {

        activity?.let {
            WindowUtility.layoutFullScreen(it)
            WindowUtility.setStateBarToDarkTheme(it)
            WindowUtility.setStatusBarColor(it, Color.TRANSPARENT)
            WindowUtility.setNavigationBarColor(it, Color.TRANSPARENT)
        }

    }

}