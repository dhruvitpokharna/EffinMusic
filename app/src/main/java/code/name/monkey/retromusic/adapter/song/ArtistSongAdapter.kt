package code.name.monkey.retromusic.adapter.song

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.databinding.ItemSongBinding
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil

class ArtistSongAdapter(
    private val activity: FragmentActivity
) : ListAdapter<Song, ArtistSongAdapter.SongViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song) {
            val fixedTrackNumber = MusicUtil.getFixedTrackNumber(song.trackNumber)

            // Track number as imageText
            binding.imageText.text = if (fixedTrackNumber > 0) fixedTrackNumber.toString() else "-"

            // Duration
            binding.time.text = String.format(
                "%s | %s",
                fixedTrackNumber,
                MusicUtil.getReadableDurationString(song.duration)
            )

            // Title
            val songTextSize = PreferenceUtil.songTextSize.toFloat()
            binding.songTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, songTextSize)
            binding.songTitle.text = song.title

            // Artist
            if (PreferenceUtil.showArtistInSongs) {
                binding.songArtist.text = song.allArtists
                binding.songArtist.isVisible = true
                val artistTextSize = PreferenceUtil.artistTextSize.toFloat()
                binding.songArtist.setTextSize(TypedValue.COMPLEX_UNIT_SP, artistTextSize)
            } else {
                binding.songArtist.isVisible = false
            }

            // TODO: if your layout had album art / imageView, bind it here too.
            // e.g., binding.songImage.load(song.albumArtUri)
        }
    }
}
