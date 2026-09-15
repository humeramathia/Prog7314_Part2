package com.example.prog7314_part2.ui.performance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part2.data.PerformanceSession
import com.example.prog7314_part2.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(
    private val sessions: List<PerformanceSession>
) : RecyclerView.Adapter<SessionAdapter.Holder>() {

    private val format = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    inner class Holder(val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemSessionBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val session = sessions[position]
        holder.binding.sessionDate.text = format.format(Date(session.recordedAt))
        holder.binding.sessionScore.text = "${session.score.toInt()}  ·  ${session.notes}"
    }

    override fun getItemCount(): Int = sessions.size
}
