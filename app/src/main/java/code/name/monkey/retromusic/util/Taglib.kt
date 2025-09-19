package code.name.monkey.retromusic.util

import android.content.Context
import code.name.monkey.retromusic.model.Song
import com.kyant.taglib.Metadata
import com.kyant.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Taglib {
    suspend fun getAllTags(context: Context, song: Song): Map<String, List<String>> =
        withContext(Dispatchers.IO) {
            val uri = UriUtil.getUriFromPath(context, song.data)
            val resolver = context.contentResolver

            resolver.openFileDescriptor(uri, "r")?.use { fdParcel ->
                val metadata: Metadata? =
                    TagLib.getMetadata(fdParcel.dup().detachFd(), readPictures = false)
                metadata?.propertyMap?.mapValues { it.value.toList() } ?: emptyMap()
            } ?: emptyMap()
        }
}
