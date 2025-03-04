package com.capstone.Algan.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.R
import com.capstone.Algan.models.Message


class ChatAdapter(
    private val messageList: MutableList<Message>,
    private val currentUser: String // currentUser은 Firebase에서 가져온 현재 사용자 이름
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.tvMessageContent.text = message.content
        holder.tvMessageTimestamp.text = message.timestamp
        holder.tvUsername.text = message.username



        // 본인 메시지에만 삭제 버튼 표시
        if (message.username == currentUser) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                // 메시지 삭제 처리 ( 리스트에서 제거)
                if (messageList.size > position) { // 메시지가 리스트에 있을 경우만 삭제
                    (messageList as MutableList).removeAt(position)
                    notifyItemRemoved(position)
                }
            }
        } else {
            holder.btnDelete.visibility = View.GONE
        }


    }

    override fun getItemCount(): Int {
        return messageList.size
    }



    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessageContent: TextView = itemView.findViewById(R.id.tvMessageContent)
        val tvMessageTimestamp: TextView = itemView.findViewById(R.id.tvMessageTimestamp)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val btnProfile: ImageView = itemView.findViewById(R.id.btnProfile)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val imageViewMessageImage: ImageView = itemView.findViewById(R.id.imageViewMessageImage) // 이미지 뷰 추가
    }
}
