package com.capstone.Algan.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.capstone.Algan.R
import com.capstone.Algan.models.Alam

class AlamAdapter(context: Context, private val alamList: List<Alam>) :
    ArrayAdapter<Alam>(context, 0, alamList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_alam, parent, false)

        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvContent: TextView = view.findViewById(R.id.tvContent)

        val alam = alamList[position]
        tvDate.text = alam.date
        tvTime.text = alam.time
        tvContent.text = alam.content

        return view
    }
}
