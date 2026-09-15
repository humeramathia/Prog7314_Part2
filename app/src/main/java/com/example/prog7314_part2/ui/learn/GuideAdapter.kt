package com.example.prog7314_part2.ui.learn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_part2.data.LearnGuide
import com.example.prog7314_part2.data.label
import com.example.prog7314_part2.databinding.ItemGuideBinding

class GuideAdapter(
    private val onClick: (LearnGuide) -> Unit
) : RecyclerView.Adapter<GuideAdapter.Holder>() {

    private val items = mutableListOf<LearnGuide>()

    fun submit(guides: List<LearnGuide>) {
        items.clear()
        items.addAll(guides)
        notifyDataSetChanged()
    }

    inner class Holder(val binding: ItemGuideBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemGuideBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val guide = items[position]
        holder.binding.guideTitle.text = guide.title
        holder.binding.guideCategory.text = guide.category.label().replaceFirstChar { it.uppercase() }
        holder.itemView.setOnClickListener { onClick(guide) }
    }

    override fun getItemCount(): Int = items.size
}
