package com.suda.yzune.wakeupschedule.schedule_import

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.base_view.BaseFragment
import com.suda.yzune.wakeupschedule.utils.ViewUtils
import es.dmoral.toasty.Toasty

class SchoolInfoFragment : BaseFragment() {
    private val v_status: android.view.View
        get() = requireView().findViewById(R.id.v_status)
    private val ib_back: androidx.appcompat.widget.AppCompatImageButton
        get() = requireView().findViewById(R.id.ib_back)
    private val et_school: androidx.appcompat.widget.AppCompatEditText
        get() = requireView().findViewById(R.id.et_school)
    private val et_type: androidx.appcompat.widget.AppCompatEditText
        get() = requireView().findViewById(R.id.et_type)
    private val chip_urp: com.google.android.material.chip.Chip
        get() = requireView().findViewById(R.id.chip_urp)
    private val et_qq: androidx.appcompat.widget.AppCompatEditText
        get() = requireView().findViewById(R.id.et_qq)
    private val tv_next: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_next)


    private val viewModel by activityViewModels<ImportViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_school_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewUtils.resizeStatusBar(context!!.applicationContext, v_status)
        initEvent()
    }

    private fun initEvent() {
        ib_back.setOnClickListener {
            activity!!.finish()
        }

        chip_urp.setOnCheckedChangeListener { _, isChecked ->
            viewModel.isUrp = isChecked
        }

        tv_next.setOnClickListener {
            if (et_school.text.toString() != "") {
                viewModel.schoolInfo[0] = et_school.text.toString()
                viewModel.schoolInfo[1] = et_type.text.toString()
                viewModel.schoolInfo[2] = et_qq.text.toString()
                val fragment = WebViewLoginFragment.newInstance()
                val transaction = parentFragmentManager.beginTransaction()
                transaction.hide(this)
                transaction.add(android.R.id.content, fragment, "webLogin")
                transaction.commit()
            } else {
                Toasty.error(activity!!, "请填写学校全称").show()
            }
        }
    }

}
