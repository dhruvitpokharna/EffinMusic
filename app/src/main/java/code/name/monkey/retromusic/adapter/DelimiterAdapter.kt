package code.name.monkey.retromusic.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import code.name.monkey.retromusic.databinding.PreferenceDialogDelimiterListitemBinding

class DelimiterAdapter(
    delimiters: List<String>,
    private val selected: MutableSet<String>
) : RecyclerView.Adapter<DelimiterAdapter.ViewHolder>() {

    private val delimiterList = delimiters.toMutableList()

    @SuppressLint("NotifyDataSetChanged")
    fun updateDelimiters(newDelimiters: List<String>) {
        delimiterList.clear()
        delimiterList.addAll(newDelimiters)
        notifyDataSetChanged()
    }

    fun getSelectedDelimiters(): List<String> = selected.toList()

    fun resetSelection() {
        selected.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = delimiterList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val delimiter = delimiterList[position]
        holder.binding.checkbox.isChecked = selected.contains(delimiter)
        holder.binding.title.text = delimiter

        holder.itemView.setOnClickListener {
            if (selected.contains(delimiter)) {
                selected.remove(delimiter)
                holder.binding.checkbox.isChecked = false
            } else {
                selected.add(delimiter)
                holder.binding.checkbox.isChecked = true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            PreferenceDialogDelimiterListitemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    class ViewHolder(val binding: PreferenceDialogDelimiterListitemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.checkbox.buttonTintList =
                ColorStateList.valueOf(accentColor(binding.checkbox.context))
        }
    }
}
