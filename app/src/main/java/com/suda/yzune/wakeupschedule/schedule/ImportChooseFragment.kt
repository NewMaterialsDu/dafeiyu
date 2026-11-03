package com.suda.yzune.wakeupschedule.schedule

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.BaseDialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.schedule_import.LoginWebActivity
import com.suda.yzune.wakeupschedule.schedule_import.SchoolListActivity
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.Utils
import splitties.activities.start

class ImportChooseFragment : BaseDialogFragment() {
    private val ib_close: androidx.appcompat.widget.AppCompatTextView
        get() = requireView().findViewById(R.id.ib_close)
    private val tv_school: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_school)
    private val tv_excel: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_excel)
    private val tv_html: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_html)
    private val tv_file: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_file)
    private val tv_feedback: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_feedback)


    override val layoutId: Int
        get() = R.layout.fragment_import_choose

    private val viewModel by activityViewModels<ScheduleViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initEvent()
    }

    private fun initEvent() {
        ib_close.setOnClickListener {
            dismiss()
        }

        tv_file.setOnClickListener {
            showSAFTips {
                activity!!.startActivityForResult(
                        Intent(activity, LoginWebActivity::class.java).apply {
                            putExtra("import_type", "file")
                        },
                        Const.REQUEST_CODE_IMPORT)
                this.dismiss()
            }
        }

        tv_html.setOnClickListener {
            showSAFTips {
                activity!!.startActivityForResult(
                        Intent(activity, LoginWebActivity::class.java).apply {
                            putExtra("import_type", "html")
                            putExtra("tableId", viewModel.table.id)
                        },
                        Const.REQUEST_CODE_IMPORT)
                this.dismiss()
            }
        }

        tv_excel.setOnClickListener {
            showSAFTips {
                activity!!.startActivityForResult(
                        Intent(activity, LoginWebActivity::class.java).apply {
                            putExtra("import_type", "excel")
                            putExtra("tableId", viewModel.table.id)
                        },
                        Const.REQUEST_CODE_IMPORT)
                this.dismiss()
            }
        }

        tv_school.setOnClickListener {
            //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            activity!!.startActivityForResult(
                    Intent(activity, SchoolListActivity::class.java),
                    Const.REQUEST_CODE_IMPORT)
            dismiss()
        }

        tv_feedback.setOnClickListener {
            activity!!.startActivityForResult(
                    Intent(activity, LoginWebActivity::class.java).apply {
                        putExtra("import_type", "share")
                        putExtra("tableId", viewModel.table.id)
                    },
                    Const.REQUEST_CODE_IMPORT)
            dismiss()
        }
    }

    private fun showSAFTips(block: () -> Unit) {
        MaterialAlertDialogBuilder(activity!!)
                .setTitle("提示")
                .setMessage("为了避免使用敏感的外部存储读写权限，本应用采用了系统级的文件选择器来选择文件。如果找不到路径，请点选择器右上角的三个点，选择「显示内部存储设备」，然后通过侧栏选择路径。")
                .setPositiveButton(R.string.sure) { _, _ ->
                    block.invoke()
                }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .show()
    }

}
