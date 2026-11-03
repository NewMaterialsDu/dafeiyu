package com.suda.yzune.wakeupschedule.intro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.base_view.BaseBlurTitleActivity
import com.suda.yzune.wakeupschedule.utils.UpdateUtils
import splitties.dimensions.dip

class AboutActivity : BaseBlurTitleActivity() {
    private val tv_version: androidx.appcompat.widget.AppCompatTextView
        get() = findViewById(R.id.tv_version)
    private val tv_contact: androidx.appcompat.widget.AppCompatTextView
        get() = findViewById(R.id.tv_contact)
    private val tv_sponsor: com.google.android.material.button.MaterialButton
        get() = findViewById(R.id.tv_sponsor)

    override val layoutId: Int
        get() = R.layout.activity_about

    override fun onSetupSubButton(tvButton: AppCompatTextView): AppCompatTextView? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            tv_version.text = "版本号：${UpdateUtils.getVersionName(this)}"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tv_contact.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:19231331237@163.com")
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 商店版不展示二创署名段（该版本已不含二创素材）
        if (!resources.getBoolean(R.bool.show_fanart_credit)) {
            findViewById<android.widget.TextView?>(R.id.tv_fanart_credit)?.visibility = android.view.View.GONE
            findViewById<android.widget.TextView?>(R.id.tv_fanart_title)?.visibility = android.view.View.GONE
        }

        // 商店版不展示赞赏入口（应用商店普遍禁止站外支付引导）
        if (resources.getBoolean(R.bool.show_sponsor)) {
            tv_sponsor.setOnClickListener { showSponsorDialog() }
        } else {
            tv_sponsor.visibility = android.view.View.GONE
        }
    }

    /** 赞助：微信 / 支付宝两个收款码 */
    private fun showSponsorDialog() {
        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dip(16), dip(8), dip(16), dip(8))
        }
        scroll.addView(container)
        // 这两张码是收款二维码，App 里长按识别不了（也不该假装能），所以只放图
        listOf(R.drawable.sponsor_wechat, R.drawable.sponsor_alipay).forEach { resId ->
            container.addView(ImageView(this).apply {
                setImageResource(resId)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dip(8)
                    bottomMargin = dip(8)
                }
            })
        }
        MaterialAlertDialogBuilder(this)
                .setTitle("喜欢的话赞助一下吧~")
                .setView(scroll)
                .setPositiveButton("谢谢老板") { d, _ -> d.dismiss() }
                .show()
    }
}
