package com.swein.shcamerax

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.swein.shcamerax.framework.utility.permission.PermissionManager
import com.swein.shcamerax.shcamera.SHCameraFragment

class MainActivity : AppCompatActivity() {

    private val permissionManager = PermissionManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {

            permissionManager.requestPermission(
                "Permission",
                "permissions are necessary",
                "setting",
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
            ) {
                SHCameraFragment.startFragment(this)
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        SHCameraFragment.removeFragment(this)
    }
}