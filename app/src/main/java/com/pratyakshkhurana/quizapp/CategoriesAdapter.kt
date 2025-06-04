package com.pratyakshkhurana.quizapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pratyakshkhurana.quizapp.databinding.EachCategoryViewBinding

class CategoriesAdapter(
    private val data: ArrayList<CategoryView>,
    private val listener: OnClicked
) :
    RecyclerView.Adapter<CategoriesAdapter.CategoriesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriesViewHolder {
        val binding = EachCategoryViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = CategoriesViewHolder(binding)
        binding.root.setOnClickListener {
            listener.categoryClicked(data[viewHolder.adapterPosition])
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: CategoriesViewHolder, position: Int) {
        val curr = data[position]
        holder.binding.categoryText.text = curr.category
        holder.binding.cardView.background = curr.cardImage
        Glide.with(holder.itemView.context).load(curr.icon).into(holder.binding.image)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class CategoriesViewHolder(val binding: EachCategoryViewBinding) : RecyclerView.ViewHolder(binding.root)
}

interface OnClicked {
    fun categoryClicked(s: CategoryView)
}