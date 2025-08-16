package com.voiceassistant.app.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.voiceassistant.app.R
import com.voiceassistant.app.databinding.ItemConversationBinding
import com.voiceassistant.app.domain.model.ConversationItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * 對話歷史適配器
 */
class ConversationAdapter : ListAdapter<ConversationItem, ConversationAdapter.ConversationViewHolder>(
    ConversationDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding: ItemConversationBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_conversation,
            parent,
            false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ConversationViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(item: ConversationItem) {
            binding.apply {
                userText.text = "使用者: ${item.userInput}"
                aiText.text = "助理: ${item.aiResponse}"
                timestampText.text = dateFormat.format(Date(item.timestamp))
                
                executePendingBindings()
            }
        }
    }

    private class ConversationDiffCallback : DiffUtil.ItemCallback<ConversationItem>() {
        override fun areItemsTheSame(oldItem: ConversationItem, newItem: ConversationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ConversationItem, newItem: ConversationItem): Boolean {
            return oldItem == newItem
        }
    }
}
