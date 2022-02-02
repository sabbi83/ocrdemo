package com.harvinder.ocrdemo.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.harvinder.ocrdemo.R
import com.harvinder.ocrdemo.model.ScanText
import android.R.attr.name





class ScanDataAdapter : RecyclerView.Adapter<ScanDataAdapter.ScanViewHolder>() {

    inner class ScanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val diffCallback = object : DiffUtil.ItemCallback<ScanText>() {
        override fun areItemsTheSame(oldItem: ScanText, newItem: ScanText): Boolean {
            return oldItem.txt == newItem.txt
        }

        override fun areContentsTheSame(oldItem: ScanText, newItem: ScanText): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<ScanText>) = differ.submitList(list)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        return ScanViewHolder(
            LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.scan_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {

        val item = differ.currentList[position]

        holder.itemView.apply {
            rootView.findViewById<TextView>(R.id.tv_data).setText(item.txt)



        }

    }
}