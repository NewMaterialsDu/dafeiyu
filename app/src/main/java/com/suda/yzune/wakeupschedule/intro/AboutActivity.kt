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
    private val tv_website: com.google.android.material.button.MaterialButton
        get() = findViewById(R.id.tv_website)
    private val tv_check_update: com.google.android.material.button.MaterialButton
        get() = findViewById(R.id.tv_check_update)

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

        tv_website.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://newmaterialsdu.github.io/dafeiyu/")))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        tv_check_update.setOnClickListener { checkUpdate() }

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

    /** 手动检查更新：只在这里联网请求 GitHub Releases，平时完全不打扰用户 */
    private fun checkUpdate() {
        val btn = tv_check_update
        btn.isEnabled = false
        val oldText = btn.text
        btn.text = "检查中…"
        val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        val request = okhttp3.Request.Builder()
                .url("https://api.github.com/repos/NewMaterialsDu/dafeiyu/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    btn.isEnabled = true
                    btn.text = oldText
                    es.dmoral.toasty.Toasty.error(this@AboutActivity, "检查失败，请检查网络后重试").show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = try {
                    response.body()?.string().orEmpty()
                } catch (e: Exception) {
                    ""
                }
                runOnUiThread {
                    btn.isEnabled = true
                    btn.text = oldText
                    try {
                        val tag = org.json.JSONObject(body).optString("tag_name").trim()
                                .removePrefix("v").removePrefix("V")
                        val current = UpdateUtils.getVersionName(this@AboutActivity).substringBefore("-")
                        when {
                            tag.isEmpty() -> es.dmoral.toasty.Toasty
                                    .error(this@AboutActivity, "检查失败，请稍后再试").show()
                            tag != current -> MaterialAlertDialogBuilder(this@AboutActivity)
                                    .setTitle("发现新版本 $tag")
                                    .setMessage("当前版本：$current\n最新版本：$tag\n\n是否前往下载页？")
                                    .setPositiveButton("去下载") { _, _ ->
                                        try {
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                                                    "https://newmaterialsdu.github.io/dafeiyu/")))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    .setNegativeButton("以后再说", null)
                                    .show()
                            else -> es.dmoral.toasty.Toasty
                                    .info(this@AboutActivity, "已是最新版本 $current").show()
                        }
                    } catch (e: Exception) {
                        es.dmoral.toasty.Toasty.error(this@AboutActivity, "检查失败，请稍后再试").show()
                    }
                }
            }
        })
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
