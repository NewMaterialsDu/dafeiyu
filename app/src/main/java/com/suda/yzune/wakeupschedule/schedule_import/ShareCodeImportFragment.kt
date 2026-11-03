package com.suda.yzune.wakeupschedule.schedule_import

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.base_view.BaseFragment
import com.suda.yzune.wakeupschedule.utils.ViewUtils
import es.dmoral.toasty.Toasty

/**
 * 分享码导入：粘贴一段 WKP1:... 的文本，离线还原成一份课表。
 * 与「从分享/导出文件导入」共用同一份数据格式，只是载体从文件变成了纯文本。
 */
class ShareCodeImportFragment : BaseFragment() {

    companion object {
        private const val REQUEST_PICK_QR_IMAGE = 301
        private const val REQUEST_SCAN_QR = 302
    }
    private val v_status: android.view.View
        get() = requireView().findViewById(R.id.v_status)
    private val ib_back: androidx.appcompat.widget.AppCompatImageButton
        get() = requireView().findViewById(R.id.ib_back)
    private val tv_title: androidx.appcompat.widget.AppCompatTextView
        get() = requireView().findViewById(R.id.tv_title)
    private val input_code: com.google.android.material.textfield.TextInputLayout
        get() = requireView().findViewById(R.id.input_code)
    private val et_code: com.google.android.material.textfield.TextInputEditText
        get() = requireView().findViewById(R.id.et_code)
    private val tv_scan: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_scan)
    private val tv_scan_gallery: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_scan_gallery)
    private val tv_paste: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_paste)
    private val tv_import: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_import)

    private val viewModel by activityViewModels<ImportViewModel>()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PICK_QR_IMAGE) {
            if (resultCode == Activity.RESULT_OK && data?.data != null) {
                decodeQrFromUri(data.data!!)
            }
            return
        }
        if (requestCode == REQUEST_SCAN_QR) {
            val text = data?.getStringExtra(ScanActivity.EXTRA_RESULT)
            if (text.isNullOrBlank()) {
                Toasty.info(context!!, "没扫到，可以用「从相册选二维码图片」").show()
            } else {
                et_code.setText(text)
                input_code.error = null
                Toasty.success(context!!, "扫到了，点「导入」就行").show()
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /** 从相册图片里识别二维码 */
    private fun decodeQrFromUri(uri: android.net.Uri) {
        try {
            val input = context!!.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(input)
            input?.close()
            if (bitmap == null) {
                Toasty.error(context!!, "这张图读不出来呢").show()
                return
            }
            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val source = com.google.zxing.RGBLuminanceSource(w, h, pixels)
            val bitmapZxing = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            val reader = com.google.zxing.MultiFormatReader()
            reader.setHints(mapOf(
                    com.google.zxing.DecodeHintType.TRY_HARDER to true,
                    com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE)
            ))
            val resultText = try {
                reader.decodeWithState(bitmapZxing).text
            } finally {
                reader.reset()
            }
            et_code.setText(resultText)
            input_code.error = null
            Toasty.success(context!!, "识别到了，点「导入」就行").show()
        } catch (e: Exception) {
            Toasty.error(context!!, "这张图里没找到二维码>_<\n换个清楚点的截图试试", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_share_code_import, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewUtils.resizeStatusBar(context!!.applicationContext, v_status)

        ib_back.setOnClickListener {
            activity!!.finish()
        }

        tv_scan.setOnClickListener {
            // 用自己写的扫描页（强制竖屏 + 连续对焦），库自带的在小米上会横屏
            startActivityForResult(Intent(context, ScanActivity::class.java), REQUEST_SCAN_QR)
        }

        tv_scan_gallery.setOnClickListener {
            // 手机拍屏/截图里存下来的二维码也能认，比对着屏幕扫好使得多
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            try {
                startActivityForResult(intent, REQUEST_PICK_QR_IMAGE)
            } catch (e: Exception) {
                Toasty.error(context!!, "打不开相册>_<").show()
            }
        }

        tv_paste.setOnClickListener {
            val cm = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString() ?: ""
            if (text.isBlank()) {
                Toasty.info(context!!, "剪贴板里没有内容哦").show()
            } else {
                et_code.setText(text)
                input_code.error = null
            }
        }

        tv_import.setOnClickListener {
            val code = et_code.text?.toString()?.trim() ?: ""
            if (code.isEmpty()) {
                input_code.error = "先粘贴分享码吧"
                return@setOnClickListener
            }
            input_code.error = null
            launch {
                try {
                    viewModel.importFromShareCode(code)
                    Toasty.success(activity!!, "导入成功(ﾟ▽ﾟ)/请在右侧栏切换后查看").show()
                    activity!!.setResult(Activity.RESULT_OK)
                    activity!!.finish()
                } catch (e: Exception) {
                    Toasty.error(activity!!, "导入失败>_<\n${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
