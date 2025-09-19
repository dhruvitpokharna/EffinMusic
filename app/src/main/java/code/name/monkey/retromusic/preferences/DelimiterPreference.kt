package code.name.monkey.retromusic.preferences

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.appthemehelper.common.prefs.supportv7.ATEDialogPreference
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.DelimiterAdapter
import code.name.monkey.retromusic.databinding.PreferenceDialogLibraryCategoriesBinding
import code.name.monkey.retromusic.extensions.colorButtons
import code.name.monkey.retromusic.extensions.colorControlNormal
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.util.PreferenceUtil

class DelimiterPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ATEDialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    init {
        icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            context.colorControlNormal(),
            SRC_IN
        )
        dialogTitle = context.getString(R.string.pref_title_artist_delimiters)
    }
}

class DelimiterPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = PreferenceDialogLibraryCategoriesBinding.inflate(layoutInflater)

        val adapter = DelimiterAdapter(
            PreferenceUtil.defaultDelimiters,
            PreferenceUtil.artistDelimiters?.toMutableSet() ?: mutableSetOf()
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        return materialDialog(R.string.pref_title_artist_delimiters) 
            .setNeutralButton(R.string.reset_action) { _, _ ->
                adapter.resetSelection()
                updateDelimiters(emptyList())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.done) { _, _ ->
                updateDelimiters(adapter.getSelectedDelimiters())
                showToast("Rebuild Custom Libary")
            }
            .setView(binding.root)
            .create()
            .colorButtons()
    }

    private fun updateDelimiters(delimiters: List<String>) {
        PreferenceUtil.artistDelimiters = delimiters
    }

    companion object {
        fun newInstance(): DelimiterPreferenceDialog {
            return DelimiterPreferenceDialog()
        }
    }
}
