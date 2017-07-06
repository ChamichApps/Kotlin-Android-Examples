package com.example.tamaskozmer.kotlinrxexample.view.adapters

import android.support.v4.util.SparseArrayCompat
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.example.tamaskozmer.kotlinrxexample.model.entities.User
import com.example.tamaskozmer.kotlinrxexample.view.adapters.viewtypes.ViewType

/**
 * Created by Tamas_Kozmer on 7/6/2017.
 */
class DetailsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: MutableList<ViewType>
    private var delegateAdapters = SparseArrayCompat<ViewTypeDelegateAdapter>()

    init {
        items = ArrayList()
        delegateAdapters.put(AdapterConstants.USER_DETAILS, UserDetailsDelegateAdapter())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = delegateAdapters[viewType].onCreateViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
            = delegateAdapters[getItemViewType(position)].onBindViewHolder(holder, items[position])

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].getViewType()
    }

    fun addItems(newItems: List<ViewType>) {
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: ViewType) {
        items.add(item)
        notifyDataSetChanged()
    }
}