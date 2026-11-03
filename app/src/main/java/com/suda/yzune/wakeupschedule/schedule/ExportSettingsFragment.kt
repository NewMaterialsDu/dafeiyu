package com.suda.yzune.wakeupschedule.schedule

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.suda.yzune.wakeupschedule.utils.QrShareCache
import com.suda.yzune.wakeupschedule.utils.QrUtils
import androidx.fragment.app.BaseDialogFragment
import androidx.fragment.app.activityViewModels
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.ShareCodec
import com.suda.yzune.wakeupschedule.utils.Utils
import es.dmoral.toasty.Toasty

class ExportSettingsFragment : BaseDialogFragment() {
    private val tv_export: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_export)
    private val tv_export_ics: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_export_ics)
    private val tv_share_code: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_share_code)
    private val tv_share_qr: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_share_qr)
    private val tv_cancel: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_cancel)


    override val layoutId: Int
        get() = R.layout.fragment_export_settings

    private val viewModel by activityViewModels<ScheduleViewModel>()

    val tableName by lazy(LazyThreadSafetyMode.NONE) {
        if (viewModel.table.tableName == "") {
            "我的课表"
        } else {
            viewModel.table.tableName
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false

        tv_export.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, "$tableName.wakeup_schedule")
            }
            Toasty.info(activity!!, "请自行选择导出的地方\n不要修改文件的扩展名哦", Toasty.LENGTH_LONG).show()
            activity?.startActivityForResult(intent, Const.REQUEST_CODE_EXPORT)
            dismiss()
        }

        tv_export_ics.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/calendar"
                putExtra(Intent.EXTRA_TITLE, "日历-$tableName")
            }
            Toasty.info(activity!!, "请自行选择导出的地方\n不要修改文件的扩展名哦", Toasty.LENGTH_LONG).show()
            activity?.startActivityForResult(intent, Const.REQUEST_CODE_EXPORT_ICS)
            dismiss()
        }

        tv_share_code.setOnClickListener {
            launch {
                try {
                    val code = ShareCodec.encode(viewModel.buildExportString())
                    val cm = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("课表分享码", code))
                    Toasty.success(activity!!, "分享码已复制，发给对方粘贴导入即可", Toasty.LENGTH_LONG).show()
                    dismiss()
                } catch (e: Exception) {
                    Toasty.error(activity!!, "生成分享码失败>_<\n${e.message}", Toasty.LENGTH_LONG).show()
                }
            }
        }

        tv_share_qr.setOnClickListener {
            // 生成二维码要读数据库（挂起），不能在弹窗自己的协程里做：
            // 弹窗一 dismiss，Fragment 的 scope 就取消，协程会抛 UndispatchedCoroutine was cancelled。
            // 交给 Activity 的 lifecycleScope 继续跑。
            val act = activity as? ScheduleActivity
            dismiss()
            act?.showQrShare()
        }

        tv_cancel.setOnClickListener {
            dismiss()
        }
    }
}
