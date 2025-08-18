package code.name.monkey.retromusic.preferences

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import android.widget.RadioButton
import android.widget.RadioGroup
import code.name.monkey.retromusic.adapter.MetadataFieldAdapter
import code.name.monkey.retromusic.databinding.PreferenceDialogNowPlayingMetadataBinding
import code.name.monkey.retromusic.model.EditableMetadataField
import code.name.monkey.retromusic.model.MetadataField
import code.name.monkey.retromusic.util.PreferenceUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NowPlayingMetadataPreferenceDialog : DialogFragment() {

    private var _binding: PreferenceDialogNowPlayingMetadataBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply a style that prevents the default DialogFragment window frame
        setStyle(STYLE_NO_FRAME, R.style.Theme_RetroMusic_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Return null to let onCreateDialog handle the view creation
        return null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = PreferenceDialogNowPlayingMetadataBinding.inflate(layoutInflater)

        val metadataFieldAdapter = MetadataFieldAdapter()
        // Initialize adapter with current preferences
        metadataFieldAdapter.metadataFields = PreferenceUtil.nowPlayingMetadataOrder.mapNotNull { fieldId ->
            MetadataField.fromId(fieldId)?.let { metadataField ->
                EditableMetadataField(metadataField, PreferenceUtil.nowPlayingMetadataVisibility.contains(fieldId))
            }
        }.toMutableList()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = metadataFieldAdapter
            metadataFieldAdapter.attachToRecyclerView(this)
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pref_header_now_playing_metadata) // Set title using builder
            .setCancelable(true)
            .setNeutralButton(
                R.string.reset_action
            ) { dialog, _ ->
                // Reset to default order and all visible
                val defaultOrder = MetadataField.values().map { it.id }
                val defaultVisibility = MetadataField.values().map { it.id }.toSet()
                updateMetadataFields(defaultOrder, defaultVisibility)
                metadataFieldAdapter.metadataFields = defaultOrder.mapNotNull { fieldId ->
                    MetadataField.fromId(fieldId)?.let { metadataField ->
                        EditableMetadataField(metadataField, defaultVisibility.contains(fieldId))
                    }
                }.toMutableList()
                metadataFieldAdapter.notifyDataSetChanged()
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dismiss()
            }
            .setPositiveButton(R.string.done) { dialog, _ ->
                // Save current order and visibility from adapter
                val currentOrder = metadataFieldAdapter.metadataFields.map { it.metadataField.id }
                val currentVisibility = metadataFieldAdapter.metadataFields.filter { it.isVisible }.map { it.metadataField.id }.toSet()
                updateMetadataFields(currentOrder, currentVisibility)
                metadataFieldAdapter.metadataFields = currentOrder.mapNotNull { fieldId ->
                    MetadataField.fromId(fieldId)?.let { metadataField ->
                        EditableMetadataField(metadataField, currentVisibility.contains(fieldId))
                    }
                }.toMutableList()
                metadataFieldAdapter.notifyDataSetChanged()

                dismiss()
            }
            .setView(binding.root) // Set view using builder

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    private fun updateMetadataFields(order: List<Int>, visibility: Set<Int>) {
        PreferenceUtil.nowPlayingMetadataOrder = order
        PreferenceUtil.nowPlayingMetadataVisibility = visibility
    }

    companion object {
        fun newInstance(): NowPlayingMetadataPreferenceDialog {
            return NowPlayingMetadataPreferenceDialog()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
