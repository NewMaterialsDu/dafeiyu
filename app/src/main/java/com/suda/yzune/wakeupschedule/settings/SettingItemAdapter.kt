package com.suda.yzune.wakeupschedule.settings

import com.chad.library.adapter.base.BaseProviderMultiAdapter
import com.suda.yzune.wakeupschedule.settings.items.BaseSettingItem
import com.suda.yzune.wakeupschedule.settings.provider.*

class SettingItemAdapter : BaseProviderMultiAdapter<BaseSettingItem>() {

    init {
        addItemProvider(CategoryItemProvider())
        addItemProvider(HorizontalItemProvider())
        addItemProvider(SeekBarItemProvider())
        addItemProvider(SwitchItemProvider())
        addItemProvider(VerticalItemProvider())
    }

    override fun getItemType(data: List<BaseSettingItem>, position: Int): Int {
        return data[position].getType()
    }

    // BRVAH 3.x 把 data 标记为 internal，跨模块无法直接赋值，这里开一个公开入口
    fun setItems(list: MutableList<BaseSettingItem>) {
        setNewData(list)
    }

}