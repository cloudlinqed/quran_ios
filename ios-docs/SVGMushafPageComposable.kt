package com.quranmedia.player.presentation.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.quranmedia.player.data.model.QCFLineType
import com.quranmedia.player.data.model.QCFPageData
import com.quranmedia.player.data.repository.ReadingTheme
import com.quranmedia.player.data.source.QCFAssetLoader
import com.quranmedia.player.data.source.SVGAssetLoader
import com.quranmedia.player.domain.model.Ayah
import com.quranmedia.player.presentation.screens.reader.HighlightedAyah
import com.quranmedia.player.presentation.theme.CustomThemeColors
import com.quranmedia.player.presentation.theme.ReadingThemeColors
import com.quranmedia.player.presentation.theme.ReadingThemes
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.compose.koinInject
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

/**
 * SVG Mushaf Page Composable for iOS.
 *
 * Renders a full Quran page as an SVG vector graphic inside a WKWebView.
 * The SVG is recolored to match the current reading theme.
 *
 * Highlighting is done via JavaScript inside the WKWebView to ensure
 * pixel-perfect alignment with the SVG rendering (no coordinate mismatch).
 * A transparent Compose overlay handles gesture detection (tap/long-press).
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
fun SVGMushafPageComposable(
    pageNumber: Int,
    ayahs: List<Ayah>,
    highlightedAyah: HighlightedAyah?,
    readingTheme: ReadingTheme,
    customBackgroundColor: Color? = null,
    customTextColor: Color? = null,
    customHeaderColor: Color? = null,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onAyahLongPress: (Int, Int, Float, Float) -> Unit = { _, _, _, _ -> }
) {
    val svgAssetLoader: SVGAssetLoader = koinInject()
    val qcfAssetLoader: QCFAssetLoader = koinInject()

    val themeColors = if (readingTheme == ReadingTheme.CUSTOM &&
        customBackgroundColor != null && customTextColor != null && customHeaderColor != null
    ) {
        ReadingThemes.getTheme(
            readingTheme,
            CustomThemeColors(
                backgroundColor = customBackgroundColor,
                textColor = customTextColor,
                headerColor = customHeaderColor
            )
        )
    } else {
        ReadingThemes.getTheme(readingTheme)
    }

    // Load themed HTML content
    var htmlContent by remember(pageNumber, readingTheme) { mutableStateOf<String?>(null) }

    // Load QCF page data for ayah overlay mapping
    var qcfPageData by remember(pageNumber) { mutableStateOf<QCFPageData?>(null) }

    LaunchedEffect(pageNumber, readingTheme, customBackgroundColor, customTextColor) {
        htmlContent = svgAssetLoader.loadThemedHtml(pageNumber, themeColors, readingTheme.id)
        qcfPageData = qcfAssetLoader.loadPageData(pageNumber)
        // Preload nearby pages
        svgAssetLoader.preloadAround(pageNumber)
    }

    // Extract SVG aspect ratio from the HTML content's viewBox attribute
    val svgAspectRatio = remember(htmlContent) {
        extractSvgAspectRatio(htmlContent)
    }

    // Compute JavaScript highlight call from current highlight state + QCF data
    val highlightJs = remember(highlightedAyah, qcfPageData, themeColors.highlight) {
        computeHighlightJs(highlightedAyah, qcfPageData, themeColors)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        val isLandscape = maxWidth > maxHeight
        val safeRatio = svgAspectRatio.coerceAtLeast(0.1f)

        // Calculate SVG dimensions that fit within the available space.
        // On iPad portrait, width-based height (maxWidth / ratio) exceeds maxHeight,
        // so we use height as the constraint instead.
        // On iPhone portrait, width is the constraint (SVG fits with space above/below).
        // Landscape: use full width, height scrollable.
        val heightFromWidth = maxWidth / safeRatio
        val (svgWidthDp, svgHeightDp) = if (!isLandscape && heightFromWidth > maxHeight) {
            // Height-constrained (iPad portrait): scale width down to fit
            val constrainedWidth = maxHeight * safeRatio
            Pair(constrainedWidth, maxHeight)
        } else {
            // Width-constrained (iPhone portrait) or landscape: use full width
            Pair(maxWidth, heightFromWidth)
        }

        if (htmlContent != null) {
            // Common inner content: WKWebView + gesture overlay, sized to SVG dimensions.
            // The parent wrapper differs by orientation.
            val svgContent: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .width(svgWidthDp)
                        .height(svgHeightDp)
                ) {
                    SVGWebView(
                        htmlContent = htmlContent!!,
                        backgroundColor = themeColors.background,
                        highlightJs = highlightJs,
                        modifier = Modifier.fillMaxSize()
                    )
                    SVGGestureOverlay(
                        qcfPageData = qcfPageData,
                        ayahs = ayahs,
                        svgAspectRatio = safeRatio,
                        onTap = onTap,
                        onAyahLongPress = onAyahLongPress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isLandscape) {
                // Landscape: Compose handles vertical scrolling.
                // WKWebView is sized to full SVG height, placed inside a
                // scrollable Column. Content starts from the top.
                // Horizontal safe area padding keeps content away from the camera notch.
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .verticalScroll(scrollState)
                ) {
                    svgContent()
                }
            } else {
                // Portrait: SVG centered vertically.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    svgContent()
                }
            }
        } else {
            // Loading indicator
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = themeColors.accent,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * WKWebView wrapper that renders the recolored SVG HTML.
 *
 * Always non-interactive (interactive=false). Compose handles all gestures:
 * - Portrait: overlay handles tap/long-press
 * - Landscape: Compose verticalScroll handles scrolling, overlay handles tap/long-press
 *
 * WKWebView's internal scrolling is always disabled. In landscape, the WKWebView
 * is sized to the full SVG content height by the parent layout, and Compose's
 * verticalScroll moves the entire view.
 *
 * Only reloads HTML when content changes (page/theme switch).
 * Highlight updates on the same page use evaluateJavaScript.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
private fun SVGWebView(
    htmlContent: String,
    backgroundColor: Color,
    highlightJs: String?,
    modifier: Modifier = Modifier
) {
    val uiBgColor = UIColor(
        red = backgroundColor.red.toDouble(),
        green = backgroundColor.green.toDouble(),
        blue = backgroundColor.blue.toDouble(),
        alpha = 1.0
    )

    // Track which HTML is currently loaded to avoid unnecessary reloads
    var loadedHtml by remember { mutableStateOf("") }

    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            val webView = WKWebView(CGRectMake(0.0, 0.0, 400.0, 600.0), config)
            webView.setOpaque(false)

            // Always disable WKWebView's internal scrolling.
            // In landscape, Compose's verticalScroll handles scrolling externally.
            webView.scrollView.setScrollEnabled(false)
            webView.scrollView.setBounces(false)
            webView.scrollView.setShowsVerticalScrollIndicator(false)
            webView.scrollView.setShowsHorizontalScrollIndicator(false)
            webView.scrollView.setContentInset(platform.UIKit.UIEdgeInsetsMake(0.0, 0.0, 0.0, 0.0))

            webView.backgroundColor = uiBgColor
            webView.scrollView.backgroundColor = uiBgColor
            webView.underPageBackgroundColor = uiBgColor

            webView.loadHTMLString(htmlContent, null)
            loadedHtml = htmlContent

            // Apply initial highlight after page loads
            if (highlightJs != null) {
                webView.evaluateJavaScript(
                    "setTimeout(function(){$highlightJs},200)", null
                )
            }
            webView
        },
        update = { webView ->
            webView.backgroundColor = uiBgColor
            webView.scrollView.backgroundColor = uiBgColor
            webView.underPageBackgroundColor = uiBgColor

            if (htmlContent != loadedHtml) {
                // Page or theme changed - reload HTML
                webView.loadHTMLString(htmlContent, null)
                loadedHtml = htmlContent
                if (highlightJs != null) {
                    webView.evaluateJavaScript(
                        "setTimeout(function(){$highlightJs},200)", null
                    )
                }
            } else {
                // Same page - just update highlight via JavaScript (immediate)
                if (highlightJs != null) {
                    webView.evaluateJavaScript(highlightJs, null)
                } else {
                    webView.evaluateJavaScript("clrHl()", null)
                }
            }
        },
        interactive = false,
        modifier = modifier
    )
}

/**
 * Transparent gesture overlay for tap and long-press detection.
 *
 * Uses QCF page JSON data to map screen coordinates to ayah locations.
 * The Mushaf has a fixed 15-line layout per page (TOTAL_LINE_SLOTS = 15).
 *
 * Coordinate mapping follows the Android implementation:
 * 1. Map touch Y to line slot (1-based): lineSlot = floor(relativeY / lineSlotH) + 1
 * 2. Find matching TEXT line by line.line == lineSlot
 * 3. Map touch X to word index using even distribution (RTL)
 */
@Composable
private fun SVGGestureOverlay(
    qcfPageData: QCFPageData?,
    ayahs: List<Ayah>,
    svgAspectRatio: Float,
    onTap: () -> Unit,
    onAyahLongPress: (Int, Int, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerPosition by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                containerPosition = coordinates.positionInRoot()
                containerSize = Size(
                    coordinates.size.width.toFloat(),
                    coordinates.size.height.toFloat()
                )
            }
            .pointerInput(qcfPageData, svgAspectRatio) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { offset ->
                        if (containerSize.height <= 0 || svgAspectRatio <= 0f || qcfPageData == null) return@detectTapGestures

                        val bounds = calculateContentBounds(containerSize.width, containerSize.height, svgAspectRatio)

                        val relativeY = offset.y - bounds.contentTopY
                        if (relativeY < 0 || relativeY > bounds.contentHeight) {
                            onTap()
                            return@detectTapGestures
                        }

                        // Map Y to line slot (1-based), matching Android
                        val lineSlot = (relativeY / bounds.lineSlotH).toInt() + 1

                        // Find matching TEXT (or word-bearing BASMALA) line by line number
                        val textLine = qcfPageData.lines.find { entry ->
                            entry.line == lineSlot &&
                            entry.type != QCFLineType.SURAH_HEADER &&
                            !entry.words.isNullOrEmpty()
                        }
                        val words = textLine?.words
                        if (words.isNullOrEmpty()) {
                            onTap()
                            return@detectTapGestures
                        }

                        // Map X to word index: even distribution, RTL (matching Android)
                        val relativeX = ((offset.x - bounds.contentLeftX) / bounds.contentWidth).coerceIn(0f, 1f)
                        val wordIndex = ((1f - relativeX) * words.size).toInt().coerceIn(0, words.lastIndex)

                        val tappedWord = words[wordIndex]
                        val surah = tappedWord.getSurahNumber() ?: ayahs.firstOrNull()?.surahNumber ?: 1
                        val verse = tappedWord.getVerseNumber() ?: ayahs.firstOrNull()?.ayahNumber ?: 1

                        val globalX = containerPosition.x + offset.x
                        val globalY = containerPosition.y + offset.y
                        onAyahLongPress(surah, verse, globalX, globalY)
                    }
                )
            }
    )
}

// ---- Helper functions ----

/**
 * Compute a JavaScript call string for highlighting the given ayah.
 * Returns "hlAyah('...','...')" or "clrHl()" or null.
 *
 * The JS function runs INSIDE the WKWebView, measuring the actual SVG
 * element dimensions to position highlights in the correct coordinate system.
 */
private fun computeHighlightJs(
    highlightedAyah: HighlightedAyah?,
    qcfPageData: QCFPageData?,
    themeColors: ReadingThemeColors
): String? {
    if (highlightedAyah == null || qcfPageData == null) return null

    val lines = mutableListOf<String>()
    qcfPageData.lines.forEach { line ->
        if (line.type == QCFLineType.SURAH_HEADER) return@forEach
        val words = line.words ?: return@forEach
        if (words.isEmpty()) return@forEach
        val lineNum = line.line ?: return@forEach

        val firstIdx = words.indexOfFirst { w ->
            w.getSurahNumber() == highlightedAyah.surahNumber &&
                    w.getVerseNumber() == highlightedAyah.ayahNumber
        }
        if (firstIdx < 0) return@forEach
        val lastIdx = words.indexOfLast { w ->
            w.getSurahNumber() == highlightedAyah.surahNumber &&
                    w.getVerseNumber() == highlightedAyah.ayahNumber
        }

        // Character-weighted positioning for more accurate horizontal alignment.
        // Each word's width is proportional to its character count (+1 for inter-word space).
        val wordWeights = words.map { it.word.length + 1 }
        val totalWeight = wordWeights.sum()
        val charsBefore = wordWeights.take(firstIdx).sum()
        val charsHighlighted = wordWeights.subList(firstIdx, lastIdx + 1).sum()

        // n=lineNum, cs=char start, ce=char end, ct=char total
        lines.add("""{"n":$lineNum,"cs":$charsBefore,"ce":${charsBefore + charsHighlighted},"ct":$totalWeight}""")
    }

    if (lines.isEmpty()) return null

    val r = (themeColors.highlight.red * 255).toInt().coerceIn(0, 255)
    val g = (themeColors.highlight.green * 255).toInt().coerceIn(0, 255)
    val b = (themeColors.highlight.blue * 255).toInt().coerceIn(0, 255)
    val color = "rgba($r,$g,$b,0.3)"
    val jsonArray = "[${lines.joinToString(",")}]"
    return "hlAyah('$jsonArray','$color')"
}

/**
 * Content area bounds within the overlay, accounting for SVG centering and frame borders.
 * Used for gesture detection (approximate coordinate mapping).
 */
private data class ContentBounds(
    val contentTopY: Float,
    val contentHeight: Float,
    val contentLeftX: Float,
    val contentRightX: Float,
    val contentWidth: Float,
    val lineSlotH: Float
)

/**
 * Calculate the content area where the 15 Mushaf lines are rendered.
 * Used for gesture detection in the Compose overlay.
 *
 * Note: Highlighting uses JavaScript inside the WKWebView for pixel-perfect
 * alignment. This function only needs to be approximate for gesture detection.
 */
private fun calculateContentBounds(
    containerWidth: Float,
    containerHeight: Float,
    svgAspectRatio: Float
): ContentBounds {
    val svgRenderedHeight = containerWidth / svgAspectRatio
    val svgVerticalOffset = ((containerHeight - svgRenderedHeight) / 2f).coerceAtLeast(0f)

    val contentTopY = svgVerticalOffset + svgRenderedHeight * 0.01f
    val contentBottomY = svgVerticalOffset + svgRenderedHeight * 0.99f
    val contentHeight = contentBottomY - contentTopY
    val contentLeftX = containerWidth * 0.016f
    val contentRightX = containerWidth * 0.984f
    val contentWidth = contentRightX - contentLeftX
    val lineSlotH = contentHeight / 15f

    return ContentBounds(contentTopY, contentHeight, contentLeftX, contentRightX, contentWidth, lineSlotH)
}

/**
 * Extract the SVG aspect ratio (width/height) from the HTML content's viewBox attribute.
 * Falls back to a typical Mushaf page ratio if not found.
 */
private fun extractSvgAspectRatio(html: String?): Float {
    if (html == null) return 0.622f
    val viewBoxMatch = Regex("""viewBox\s*=\s*"([^"]+)"""").find(html)
    val parts = viewBoxMatch?.groupValues?.get(1)?.trim()?.split(Regex("\\s+"))
    if (parts != null && parts.size >= 4) {
        val width = parts[2].toFloatOrNull() ?: return 0.622f
        val height = parts[3].toFloatOrNull() ?: return 0.622f
        if (height > 0f) return width / height
    }
    return 0.622f
}
