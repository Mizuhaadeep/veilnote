package com.glassnote.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.glassnote.R
import com.glassnote.ui.theme.isDarkGlassTheme

/**
 * URL detection and rendering shared by note cards and the editor.
 *
 * The editor only needs visual highlighting (clicking a link in a text field
 * conflicts with caret placement). Note cards pass an onUrlClick callback and
 * receive real [LinkAnnotation.Url] links.
 */
object UrlText {

    private val UrlRegex = Regex(
        // ASCII-only host/path classes. This deliberately stops at the first
        // Chinese character, space, or punctuation that is not a valid URL char.
        pattern = """(?i)\b(?:https?://|www\.)[a-z0-9.-]+\.[a-z]{2,}(?::[0-9]{2,5})?(?:[/?#][a-z0-9\-._~%!&'()*+,;=:@/?#\[\]]*)?""",
    )

    private val TrailingPunctuation = charArrayOf(
        '.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"',
    )

    data class UrlMatch(
        val value: String,
        val start: Int,
        val endExclusive: Int,
    )

    fun findUrls(text: String): List<UrlMatch> {
        val matches = mutableListOf<UrlMatch>()
        UrlRegex.findAll(text).forEach { match ->
            var end = match.range.last + 1
            while (end > match.range.first && text[end - 1] in TrailingPunctuation) {
                end--
            }
            if (end <= match.range.first) return@forEach

            val value = text.substring(match.range.first, end)
            val prefixLength = when {
                value.startsWith("https://", ignoreCase = true) -> 8
                value.startsWith("http://", ignoreCase = true) -> 7
                value.startsWith("www.", ignoreCase = true) -> 4
                else -> 0
            }
            if (value.length <= prefixLength) return@forEach

            matches += UrlMatch(
                value = value,
                start = match.range.first,
                endExclusive = end,
            )
        }
        return matches
    }

    fun normalizeUrl(rawUrl: String): String =
        if (rawUrl.startsWith("www.", ignoreCase = true)) {
            "https://$rawUrl"
        } else {
            rawUrl
        }

    /**
     * Builds a blue + underlined AnnotatedString.
     *
     * [onUrlClick] is deliberately optional: the editor passes null so links are
     * visual-only, while note cards pass a browser-opening callback.
     */
    fun buildUrlAnnotatedString(
        text: String,
        linkColor: Color,
        onUrlClick: ((String) -> Unit)? = null,
    ): AnnotatedString = buildUrlAnnotatedString(
        text = AnnotatedString(text),
        linkColor = linkColor,
        onUrlClick = onUrlClick,
    )

    /**
     * AnnotatedString overload used by TextField visual transformations so the
     * existing composing/selection spans are preserved instead of replaced.
     */
    fun buildUrlAnnotatedString(
        text: AnnotatedString,
        linkColor: Color,
        onUrlClick: ((String) -> Unit)? = null,
    ): AnnotatedString {
        val rawText = text.text
        val linkStyle = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
        )
        return buildAnnotatedString {
            append(text)
            findUrls(rawText).forEach { match ->
                if (match.start >= length || match.endExclusive > length) return@forEach

                if (onUrlClick == null) {
                    addStyle(linkStyle, match.start, match.endExclusive)
                } else {
                    val normalized = normalizeUrl(match.value)
                    addLink(
                        LinkAnnotation.Url(
                            url = normalized,
                            styles = TextLinkStyles(style = linkStyle),
                            linkInteractionListener = LinkInteractionListener { link ->
                                val clickedUrl = (link as? LinkAnnotation.Url)?.url
                                if (clickedUrl != null) onUrlClick(clickedUrl)
                            },
                        ),
                        start = match.start,
                        end = match.endExclusive,
                    )
                    addStyle(linkStyle, match.start, match.endExclusive)
                }
            }
        }
    }

    fun openInBrowser(context: Context, rawUrl: String) {
        val url = normalizeUrl(rawUrl)
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)

        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                R.string.url_open_failed,
                Toast.LENGTH_SHORT,
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                context,
                R.string.url_open_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}

/** Blue link colour, brighter in dark mode. */
@Composable
fun rememberUrlLinkColor(): Color {
    val dark = isDarkGlassTheme()
    return if (dark) Color(0xFF9CC7FF) else Color(0xFF1A73E8)
}

/**
 * TextField visual transformation: blue + underline only. No LinkAnnotation is
 * added, so tapping inside the field remains ordinary caret placement.
 */
class UrlVisualTransformation(
    private val linkColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = UrlText.buildUrlAnnotatedString(
                text = text,
                linkColor = linkColor,
            ),
            offsetMapping = OffsetMapping.Identity,
        )
    }
}

@Composable
fun rememberUrlVisualTransformation(): VisualTransformation {
    val linkColor = rememberUrlLinkColor()
    return remember(linkColor) {
        UrlVisualTransformation(linkColor)
    }
}
