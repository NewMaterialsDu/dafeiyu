package com.suda.yzune.wakeupschedule.schedule_import

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.base_view.BaseFragment
import com.suda.yzune.wakeupschedule.utils.Const
import com.suda.yzune.wakeupschedule.utils.ViewUtils

class FileImportFragment : BaseFragment() {
    private val v_status: android.view.View
        get() = requireView().findViewById(R.id.v_status)
    private val ib_back: androidx.appcompat.widget.AppCompatImageButton
        get() = requireView().findViewById(R.id.ib_back)
    private val tv_title: androidx.appcompat.widget.AppCompatTextView
        get() = requireView().findViewById(R.id.tv_title)
    private val tv_self: com.google.android.material.button.MaterialButton
        get() = requireView().findViewById(R.id.tv_self)


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_file_import, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewUtils.resizeStatusBar(context!!.applicationContext, v_status)

        tv_self.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            try {
                activity?.startActivityForResult(intent, Const.REQUEST_CODE_IMPORT_FILE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        ib_back.setOnClickListener {
            activity!!.finish()
        }
    }
}
