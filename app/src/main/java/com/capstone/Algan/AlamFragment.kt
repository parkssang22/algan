package com.capstone.Algan

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import android.widget.ListView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.capstone.Algan.adapters.AlamAdapter
import com.capstone.Algan.models.Alam
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AlamFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alam, container, false)

        val listView: ListView = view.findViewById(R.id.alam_list)

        // 샘플 데이터
        val alamList = listOf(
            Alam("2025-03-11", "14:00", "알람 1."),
            Alam("2025-03-10", "09:30", "알람 2."),
            Alam("2025-03-09", "18:45", "알람 3")
        )

        // 커스텀 어댑터 설정
        val adapter = AlamAdapter(requireContext(), alamList)
        listView.adapter = adapter

        return view
    }
}
