package com.suda.yzune.wakeupschedule.schedule_import

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import es.dmoral.toasty.Toasty

/**
 * 扫码导入用的扫描页。
 *
 * 为什么不用库自带的 CaptureActivity：它在某些机型上会跟着传感器转成横屏，
 * 而且取景/解码参数不好调。自己起一个页面，强制竖屏、开自动对焦、连续解码。
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val root = FrameLayout(this)
        barcodeView = DecoratedBarcodeView(this)
        root.addView(barcodeView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val tip = TextView(this).apply {
            text = "对准二维码，保持 10~20cm；扫不动就退回上一页用「从相册选二维码图片」"
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(48, 120, 48, 0)
            gravity = android.view.Gravity.CENTER
        }
        root.addView(tip, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)

        barcodeView.setStatusText("")
        barcodeView.barcodeView.cameraSettings.isAutoFocusEnabled = true
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(
                listOf(BarcodeFormat.QR_CODE),
                mapOf(com.google.zxing.DecodeHintType.TRY_HARDER to true),
                "UTF-8", 0)

        if (hasCameraPermission()) {
            startDecode()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_PERMISSION)
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startDecode() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (handled) return
                val text = result?.text ?: return
                handled = true
                setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT, text))
                finish()
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDecode()
            } else {
                Toasty.error(this, "没有相机权限，也可以用「从相册选二维码图片」").show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    companion object {
        const val EXTRA_RESULT = "scan_result"
        private const val REQ_PERMISSION = 401
    }
}
