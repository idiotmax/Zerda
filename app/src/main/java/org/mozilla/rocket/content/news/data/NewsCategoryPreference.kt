/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content.news.data

import android.content.Context
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import org.mozilla.focus.R

class NewsCategoryPreference @JvmOverloads constructor(context: Context, attributes: AttributeSet? = null) :
    Preference(context, attributes) {

    init {
        layoutResource = R.layout.content_tab_new_setting
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val list: RecyclerView = holder.findViewById(R.id.news_setting_cat_list) as RecyclerView
        val cats = listOf<NewsCategory>()
        list.adapter = NewsCatSettingCatAdapter(cats)
        list.layoutManager = GridLayoutManager(context, 2)
    }
}

class NewsCatSettingCatAdapter(var cats: List<NewsCategory>) :
    RecyclerView.Adapter<CategorySettingItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): CategorySettingItemViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.content_tab_new_setting_item, parent, false)
        return CategorySettingItemViewHolder(v)
    }

    override fun onBindViewHolder(vh: CategorySettingItemViewHolder, pos: Int) {
        vh.button.textOff = cats[pos].categoryId
        vh.button.textOn = cats[pos].categoryId
        vh.button.text = cats[pos].categoryId
    }

    override fun getItemCount(): Int {
        return cats.size
    }
}

class CategorySettingItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var button: ToggleButton = view.findViewById(R.id.news_setting_cat_item)
}
