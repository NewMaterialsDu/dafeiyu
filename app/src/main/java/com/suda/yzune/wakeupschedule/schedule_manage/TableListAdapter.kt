package com.suda.yzune.wakeupschedule.schedule_manage

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.suda.yzune.wakeupschedule.utils.TableCoverStore
import com.suda.yzune.wakeupschedule.utils.WallpaperUtils
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.bean.TableSelectBean

class TableListAdapter(layoutResId: Int, data: MutableList<TableSelectBean>) :
        BaseQuickAdapter<TableSelectBean, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: TableSelectBean?) {
        if (item == null) return
        if (item.type == 1) {
            helper.getView<View>(R.id.ib_delete).visibility = View.GONE
        } else {
            helper.getView<View>(R.id.ib_delete).visibility = View.VISIBLE
        }

        if (item.tableName != "") {
            helper.setText(R.id.tv_table_name, item.tableName)
        } else {
            helper.setText(R.id.tv_table_name, "我的课表")
        }
        val imageView = helper.getView<AppCompatImageView>(R.id.iv_pic)
        // 优先用「课表卡片封面图」里自选的图片（课表设置里可换/恢复默认）
        val customCover = TableCoverStore.get(context, item.id)
        if (customCover.isNotEmpty()) {
            imageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            Glide.with(context)
                    .load(customCover)
                    .override(400, 600)
                    .centerCrop()
                    .into(imageView)
        } else if (item.background != "" && !WallpaperUtils.paintSolid(imageView, item.background)) {
            Glide.with(context)
                    .load(WallpaperUtils.model(item.background))
                    .override(400, 600)
                    .into(imageView)
        } else {
            imageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            Glide.with(context)
                    .load(R.drawable.main_background_2020_1)
                    .override(400, 600)
                    .into(imageView)
        }
    }
}