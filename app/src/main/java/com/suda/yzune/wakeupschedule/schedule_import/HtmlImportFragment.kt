package com.suda.yzune.wakeupschedule.schedule_import

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.suda.yzune.wakeupschedule.utils.Docs
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.base_view.BaseFragment
import com.suda.yzune.wakeupschedule.schedule_import.Common.TYPE_QZ
import com.suda.yzune.wakeupschedule.schedule_import.Common.TYPE_ZF
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.Utils
import com.suda.yzune.wakeupschedule.utils.ViewUtils
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import splitties.snackbar.longSnack
import java.nio.charset.Charset

class HtmlImportFragment : BaseFragment() {
    private val v_status: android.view.View
        get() = requireView().findViewById(R.id.v_status)
    private val ll_bar: androidx.appcompat.widget.LinearLayoutCompat
        get() = requireView().findViewById(R.id.ll_bar)
    private val ib_back: androidx.appcompat.widget.AppCompatImageButton
        get() = requireView().findViewById(R.id.ib_back)
    private val tv_title: androidx.appcompat.widget.AppCompatTextView
        get() = requireView().findViewById(R.id.tv_title)
    private val sv_content: android.widget.ScrollView
        get() = requireView().findViewById(R.id.sv_content)
    private val tv_way: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_way)
    private val tv_type: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_type)
    private val tv_self: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_self)
    private val cp_utf: com.google.android.material.chip.Chip
        get() = requireView().findViewById(R.id.cp_utf)
    private val cp_gbk: com.google.android.material.chip.Chip
        get() = requireView().findViewById(R.id.cp_gbk)
    private val cg_qz: com.google.android.material.chip.ChipGroup
        get() = requireView().findViewById(R.id.cg_qz)
    private val chip_qz1: com.google.android.material.chip.Chip
        get() = requireView().findViewById(R.id.chip_qz1)
    private val chip_qz2: com.google.android.material.chip.Chip
        get() = requireView().findViewById(R.id.chip_qz2)
    private val chip_qz3: com.google.android.material.chip.Chip
        get() = requireView().findViewById(R.id.chip_qz3)
    private val chip_qz4: com.google.android.material.chip.Chip
        get() = requireView().findViewById(R.id.chip_qz4)
    private val cg_zf: com.google.android.material.chip.ChipGroup
        get() = requireView().findViewById(R.id.cg_zf)
    private val chip_zf1: com.google.android.material.chip.Chip
        get() = requireView().findViewById(R.id.chip_zf1)
    private val chip_zf2: com.google.android.material.chip.Chip
        get() = requireView().findViewById(R.id.chip_zf2)
    private val fab_import: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.fab_import)


    private val viewModel by activityViewModels<ImportViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_html_import, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewUtils.resizeStatusBar(context!!.applicationContext, v_status)

        tv_way.setOnClickListener {
            Docs.show(activity!!, "怎么拿到网页源码", Docs.WEB_SOURCE)
        }

        tv_type.setOnClickListener {
            startActivityForResult(Intent(activity, SchoolListActivity::class.java).apply {
                putExtra("fromLocal", true)
            }, Const.REQUEST_CODE_CHOOSE_SCHOOL)
        }

        cp_utf.isChecked = true

        cp_utf.setOnClickListener {
            cp_utf.isChecked = true
            cp_gbk.isChecked = false
        }

        cp_gbk.setOnClickListener {
            cp_gbk.isChecked = true
            cp_utf.isChecked = false
        }

        var qzChipId = 0
        cg_qz.setOnCheckedChangeListener { chipGroup, id ->
            when (id) {
                R.id.chip_qz1 -> {
                    qzChipId = id
                    viewModel.qzType = 0
                }
                R.id.chip_qz2 -> {
                    qzChipId = id
                    viewModel.qzType = 1
                }
                R.id.chip_qz3 -> {
                    qzChipId = id
                    viewModel.qzType = 2
                }
                R.id.chip_qz4 -> {
                    qzChipId = id
                    viewModel.qzType = 3
                }
                else -> {
                    chipGroup.findViewById<Chip>(qzChipId).isChecked = true
                }
            }
        }

        var zfChipId = 0
        cg_zf.setOnCheckedChangeListener { chipGroup, id ->
            when (id) {
                R.id.chip_zf1 -> {
                    zfChipId = id
                    viewModel.zfType = 0
                }
                R.id.chip_zf2 -> {
                    zfChipId = id
                    viewModel.zfType = 1
                }
                else -> {
                    chipGroup.findViewById<Chip>(zfChipId).isChecked = true
                }
            }
        }

        tv_self.setOnClickListener {
            if (viewModel.importType.equals("html")) {
                getView()?.longSnack("请先点击第二个按钮选择类型哦")
            } else {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/*"
                }
                try {
                    startActivityForResult(intent, Const.REQUEST_CODE_IMPORT_HTML)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        ib_back.setOnClickListener {
            activity!!.finish()
        }

        fab_import.setOnClickListener {
            if (viewModel.htmlUri == null) {
                it.longSnack("还没有选择文件呢>_<")
                return@setOnClickListener
            }
            launch {
                try {
                    val html = withContext(Dispatchers.IO) {
                        activity!!.contentResolver.openInputStream(viewModel.htmlUri!!)!!.bufferedReader(
                                if (cp_utf.isChecked) Charsets.UTF_8 else Charset.forName("gbk")
                        ).readText()
                    }
                    val result = viewModel.importSchedule(html)
                    Toasty.success(activity!!,
                            "成功导入 $result 门课程(ﾟ▽ﾟ)/\n请在右侧栏切换后查看").show()
                    activity!!.setResult(RESULT_OK)
                    activity!!.finish()
                } catch (e: Exception) {
                    Toasty.error(activity!!,
                            "导入失败>_<\n${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Const.REQUEST_CODE_IMPORT_HTML && resultCode == RESULT_OK) {
            viewModel.htmlUri = data?.data
        }
        if (requestCode == Const.REQUEST_CODE_CHOOSE_SCHOOL && resultCode == RESULT_OK) {
            viewModel.importType = data!!.getStringExtra("type")
            when (viewModel.importType) {
                TYPE_ZF -> {
                    chip_zf1.isChecked = true
                    cg_qz.visibility = View.GONE
                    cg_zf.visibility = View.VISIBLE
                }
                TYPE_QZ -> {
                    chip_qz1.isChecked = true
                    cg_qz.visibility = View.VISIBLE
                    cg_zf.visibility = View.GONE
                }
                else -> {
                    cg_qz.visibility = View.GONE
                    cg_zf.visibility = View.GONE
                }
            }
        }
    }

}
