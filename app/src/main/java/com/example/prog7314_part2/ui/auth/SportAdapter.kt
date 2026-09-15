package com.example.prog7314_part2.ui.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part2.R
import com.example.prog7314_part2.data.Sport
import com.example.prog7314_part2.databinding.ItemSportBinding

class SportAdapter(
    private val sports: List<Sport>,
    private var selectedId: String,
    private val onSelect: (Sport) -> Unit
) : RecyclerView.Adapter<SportAdapter.Holder>() {

    inner class Holder(val binding: ItemSportBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemSportBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val sport = sports[position]
        holder.binding.sportName.text = sport.name
        val selected = sport.id == selectedId
        val color = if (selected) R.color.teal else R.color.slate
        holder.binding.sportName.setTextColor(ContextCompat.getColor(holder.itemView.context, color))
        holder.binding.root.strokeWidth = if (selected) 3 else 1
        holder.itemView.setOnClickListener {
            selectedId = sport.id
            notifyDataSetChanged()
            onSelect(sport)
        }
    }

    override fun getItemCount(): Int = sports.size
}
