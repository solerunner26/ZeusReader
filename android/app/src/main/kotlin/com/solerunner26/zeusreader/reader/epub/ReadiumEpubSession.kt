package com.solerunner26.zeusreader.reader.epub

import android.content.Context
import com.solerunner26.zeusreader.reader.core.ReaderCapabilities
import com.solerunner26.zeusreader.reader.core.ReaderEngine
import com.solerunner26.zeusreader.reader.core.ReaderEngineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService
import org.readium.r2.shared.publication.services.search.isSearchable
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.data.Container
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.TransformingResource
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

data class EpubTocItem(
    val title: String,
    val href: String,
    val depth: Int,
    val link: Link
)

data class EpubSearchResultItem(
    val chapterTitle: String,
    val snippet: String,
    val locator: Locator
)

/**
 * Container decorator that blocks publication-supplied JS resources and sanitizes XHTML content
 * via [EpubActiveContentSanitizer] while leaving Readium's own navigator JS bridge intact.
 */
private class SanitizingContainer(
    private val delegate: Container<Resource>
) : Container<Resource> by delegate {

    override fun get(url: Url): Resource? {
        val original = delegate[url] ?: return null
        val path = url.toString()
        if (EpubActiveContentSanitizer.shouldBlockPublicationResource(path)) {
            return object : TransformingResource(original, cacheBytes = true) {
                override suspend fun transform(data: Try<ByteArray, ReadError>): Try<ByteArray, ReadError> =
                    Try.success(ByteArray(0))
            }
        }
        val lower = path.lowercase()
        val isMarkup = lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")
        if (!isMarkup) return original

        return object : TransformingResource(original, cacheBytes = true) {
            override suspend fun transform(data: Try<ByteArray, ReadError>): Try<ByteArray, ReadError> =
                data.map { bytes ->
                    val cleaned = EpubActiveContentSanitizer.sanitizeXhtml(bytes.toString(Charsets.UTF_8))
                    cleaned.toByteArray(Charsets.UTF_8)
                }
        }
    }
}

class ReadiumEpubSession(
    private val context: Context
) : ReaderEngine, AutoCloseable {

    override val engineType: ReaderEngineType = ReaderEngineType.EPUB_READIUM
    override val capabilities: ReaderCapabilities = ReaderCapabilities.EPUB

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(
        contentResolver = context.contentResolver,
        httpClient = httpClient
    )
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = context.applicationContext,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null
        )
    )

    var publication: Publication? = null
        private set

    suspend fun open(localPath: String): Result<Publication> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(localPath)
            val url: AbsoluteUrl = file.toUrl(isDirectory = false)
            val asset = when (val res = assetRetriever.retrieve(url)) {
                is Try.Success -> res.value
                is Try.Failure -> error("Failed to retrieve EPUB asset: ${res.value.message}")
            }
            val pub = when (
                val res = publicationOpener.open(
                    asset = asset,
                    allowUserInteraction = false,
                    onCreatePublication = {
                        container = SanitizingContainer(container)
                    }
                )
            ) {
                is Try.Success -> res.value
                is Try.Failure -> error("Failed to open EPUB publication: ${res.value.message}")
            }
            publication = pub
            pub
        }
    }

    fun flattenedToc(): List<EpubTocItem> {
        val pub = publication ?: return emptyList()
        val items = mutableListOf<EpubTocItem>()
        fun walk(links: List<Link>, depth: Int) {
            for (link in links) {
                items.add(
                    EpubTocItem(
                        title = link.title?.takeIf { it.isNotBlank() } ?: link.href.toString(),
                        href = link.href.toString(),
                        depth = depth,
                        link = link
                    )
                )
                if (link.children.isNotEmpty()) {
                    walk(link.children, depth + 1)
                }
            }
        }
        walk(pub.tableOfContents, 0)
        return items
    }

    suspend fun search(query: String, limit: Int = 50): List<EpubSearchResultItem> =
        withContext(Dispatchers.IO) {
            val pub = publication ?: return@withContext emptyList()
            if (query.isBlank() || !pub.isSearchable) return@withContext emptyList()

            val iterator = pub.search(
                query = query,
                options = SearchService.Options(caseSensitive = false)
            ) ?: return@withContext emptyList()

            val results = mutableListOf<EpubSearchResultItem>()
            try {
                while (results.size < limit) {
                    val collection = when (val nextRes = iterator.next()) {
                        is Try.Success -> nextRes.value
                        is Try.Failure -> null
                    } ?: break
                    for (locator in collection.locators) {
                        val snippet = listOfNotNull(
                            locator.text.before,
                            locator.text.highlight,
                            locator.text.after
                        ).joinToString("").trim()
                        results.add(
                            EpubSearchResultItem(
                                chapterTitle = locator.title ?: locator.href.toString(),
                                snippet = snippet.ifBlank { locator.href.toString() },
                                locator = locator
                            )
                        )
                        if (results.size >= limit) break
                    }
                }
            } finally {
                iterator.close()
            }
            results
        }

    override fun close() {
        runCatching { publication?.close() }
        publication = null
    }
}
