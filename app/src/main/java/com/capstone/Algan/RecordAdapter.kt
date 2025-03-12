package com.capstone.Algan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecordAdapter(private val records: MutableList<WorkTime>) :
    RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = records[position]
        holder.tvNumber.text = (position + 1).toString() // 번호 1부터 시작
        holder.tvDate.text = record.date
        holder.tvOpenTime.text = formatTimeForTable(record.clockIn)
        holder.tvCloseTime.text = formatTimeForTable(record.clockOut)
        holder.tvWorkedHours.text = record.workedHours
        holder.tvWorkerName.text = record.userName // 이름 추가
    }

    override fun getItemCount(): Int {
        return records.size
    }

    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvOpenTime: TextView = itemView.findViewById(R.id.tvClockIn)
        val tvCloseTime: TextView = itemView.findViewById(R.id.tvClockOut)
        val tvWorkedHours: TextView = itemView.findViewById(R.id.tvWorkedHours)
        val tvWorkerName: TextView = itemView.findViewById(R.id.tvWorkerName) // 이름 표시
    }

    // 표에 시간 형식 변경: 시:분만 표시
    private fun formatTimeForTable(time: String): String {
        val timeParts = time.split(":")
        return if (timeParts.size >= 2) {
            "${timeParts[0]}:${timeParts[1]}"
        } else {
            time
        }
    }
}