package com.example.prog7314_part2.ui.performance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part2.data.PerformanceSession
import com.example.prog7314_part2.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.prog7314_part2.data.PerformanceMetrics

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
        val measurementText = if (session.metrics.isEmpty()) {
            "Demo entry — no sport measurements"
        } else {
            PerformanceMetrics.forSport(session.sportId)
                .mapNotNull { metric ->
                    session.metrics[metric.key]?.let { value ->
                        "${metric.label}: $value"
                    }
                }
                .joinToString(" · ")
        }

        holder.binding.sessionScore.text = listOf(
            measurementText,
            session.notes
        ).filter { it.isNotBlank() }.joinToString("\n")
    }

    override fun getItemCount(): Int = sessions.size
}
