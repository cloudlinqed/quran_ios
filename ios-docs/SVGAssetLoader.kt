package com.quranmedia.player.data.source

import androidx.compose.ui.graphics.Color
import com.quranmedia.player.presentation.theme.ReadingThemeColors
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/**
 * Loads and caches SVG Quran page content from the iOS bundle.
 * SVGs are stored in Resources/quran-svg/001.svg ... 604.svg
 *
 * Supports:
 * - Loading raw SVG string from bundle
 * - Recoloring SVGs to match reading theme
 * - LRU caching of ~12 pages around current position
 */
class SVGAssetLoader {

    // Simple cache for raw SVG strings (keyed by page number)
    private val svgCache = mutableMapOf<Int, String>()
    private val svgCacheMutex = Mutex()
    private val maxCacheSize = 12

    // Cache for recolored SVG+HTML (keyed by "page_themeId")
    private val htmlCache = mutableMapOf<String, String>()
    private val htmlCacheMutex = Mutex()
    private val maxHtmlCacheSize = 12

    /**
     * Check if SVG assets are available (extracted to disk or in bundle).
     * Tests for existence of page 001.svg.
     */
    fun isSvgAvailable(): Boolean {
        return try {
            // Check extracted directory first (Library/Application Support/quran-svg/)
            val extractedDir = SVGAssetExtractor.getExtractedSvgDirectory()
            val extractedPath = "$extractedDir/001.svg"
            if (NSFileManager.defaultManager.fileExistsAtPath(extractedPath)) return true

            // Fallback: check bundle directly
            val bundle = NSBundle.mainBundle
            val path = bundle.pathForResource("001", "svg", "quran-svg")
                ?: run {
                    val bundlePath = bundle.bundlePath
                    val directPath = "$bundlePath/quran-svg/001.svg"
                    if (NSFileManager.defaultManager.fileExistsAtPath(directPath)) directPath else null
                }
            path != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Load raw SVG content for a given page number (1-604).
     * Returns cached content if available, otherwise loads from bundle on IO dispatcher.
     */
    suspend fun loadSvgContent(pageNumber: Int): String? {
        if (pageNumber !in 1..604) return null

        // Check cache
        svgCacheMutex.withLock {
            svgCache[pageNumber]?.let { return it }
        }

        return withContext(Dispatchers.IO) {
            loadSvgFromBundle(pageNumber)?.also { content ->
                svgCacheMutex.withLock {
                    if (svgCache.size >= maxCacheSize) {
                        val keysToRemove = svgCache.keys.take(svgCache.size - maxCacheSize + 1)
                        keysToRemove.forEach { svgCache.remove(it) }
                    }
                    svgCache[pageNumber] = content
                }
            }
        }
    }

    /**
     * Get the full HTML string with recolored SVG, ready for WKWebView.
     * Caches the result per page+theme combination.
     */
    suspend fun loadThemedHtml(
        pageNumber: Int,
        themeColors: ReadingThemeColors,
        themeId: String
    ): String? {
        val cacheKey = "${pageNumber}_$themeId"

        htmlCacheMutex.withLock {
            htmlCache[cacheKey]?.let { return it }
        }

        val svgContent = loadSvgContent(pageNumber) ?: return null
        val recolored = recolorSvg(svgContent, themeColors, pageNumber)
        val html = wrapSvgInHtml(recolored, themeColors)

        htmlCacheMutex.withLock {
            if (htmlCache.size >= maxHtmlCacheSize) {
                val keysToRemove = htmlCache.keys.take(htmlCache.size - maxHtmlCacheSize + 1)
                keysToRemove.forEach { htmlCache.remove(it) }
            }
            htmlCache[cacheKey] = html
        }

        return html
    }

    /**
     * Recolor SVG content to match the current reading theme.
     *
     * Source colors in SVGs (3 total):
     * - #231f20: Text and ayah number glyphs -> theme textPrimary
     * - #bfe8c1: Ornamental borders/frames -> blended ornament color
     * - #ffffff: White decorative fills in surah headers (pages 7, 50, 77) -> theme background
     */
    fun recolorSvg(svg: String, themeColors: ReadingThemeColors, pageNumber: Int): String {
        val textHex = colorToHex(themeColors.textPrimary)
        val bgHex = colorToHex(themeColors.background)

        // Ornament color: blend accent with background
        // Pages 1-2 (Al-Fatiha, start of Al-Baqarah) have more subtle ornaments
        val ornamentColor = if (pageNumber <= 2) {
            blendColors(themeColors.accent, themeColors.background, 0.20f)
        } else {
            blendColors(themeColors.accent, themeColors.background, 0.40f)
        }
        val ornamentHex = colorToHex(ornamentColor)

        // Replace all 3 fill colors (all SVGs use style="fill:..." format only)
        var result = svg
        result = result.replace(Regex("""fill\s*:\s*#231[fF]20"""), "fill:$textHex")
        result = result.replace(Regex("""fill\s*:\s*#[bB][fF][eE]8[cC]1"""), "fill:$ornamentHex")
        result = result.replace(Regex("""fill\s*:\s*#[fF]{6}"""), "fill:$bgHex")

        return result
    }

    /**
     * Build HTML wrapper that renders the SVG correctly in both orientations.
     *
     * The SVG viewBox is pre-cropped to content bounds. We scale to fill width
     * and let height follow proportionally. Background fills any remaining space.
     *
     * Portrait: SVG fills viewport width, centered vertically, no overflow.
     * Landscape: SVG fills viewport width, starts from top, vertical scroll enabled.
     */
    private fun wrapSvgInHtml(svgContent: String, themeColors: ReadingThemeColors): String {
        val bgHex = colorToHex(themeColors.background)
        return """<!DOCTYPE html>
<html><head>
<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no,viewport-fit=cover">
<style>
*{margin:0;padding:0;box-sizing:border-box;}
html{margin:0;padding:0;background:$bgHex;width:100%;overflow-x:hidden;}
body{margin:0;padding:0;background:$bgHex;width:100%;}
svg{width:100%;height:auto;display:block;}
.container{position:relative;width:100%;}
#hl{position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:10;}
.hr{position:absolute;border-radius:4px;pointer-events:none;}
</style></head><body>
<div class="container">$svgContent<div id="hl"></div></div>
<script>
function cb(){
  var s=document.querySelector('svg');
  if(!s)return null;
  var w=s.clientWidth,h=s.clientHeight;
  var vb=s.getAttribute('viewBox');
  if(!vb)return{ct:h*0.01,cb:h*0.99,cl:w*0.016,cr:w*0.984};
  var p=vb.split(/\s+/);
  var vx=+p[0],vy=+p[1],vw=+p[2],vh=+p[3];
  return{ct:(98-vy)/vh*h,cb:(632.5-vy)/vh*h,cl:w*0.016,cr:w*0.984};
}
function hlAyah(d,c){
  clrHl();
  var b=cb();if(!b)return;
  var ch=b.cb-b.ct,cw=b.cr-b.cl,sh=ch/15;
  var o=document.getElementById('hl');
  if(!o)return;
  try{var ls=JSON.parse(d);}catch(e){return;}
  ls.forEach(function(l){
    var e=document.createElement('div');
    e.className='hr';
    var t=b.ct+(l.n-1)*sh;
    var lf=b.cr-(l.ce/l.ct)*cw;
    var rt=b.cr-(l.cs/l.ct)*cw;
    e.style.cssText='top:'+t+'px;left:'+lf+'px;width:'+(rt-lf)+'px;height:'+sh+'px;background:'+c+';';
    o.appendChild(e);
  });
}
function clrHl(){var o=document.getElementById('hl');if(o)o.innerHTML='';}
document.body.addEventListener('click',function(){
  try{window.webkit.messageHandlers.tap.postMessage('tap');}catch(e){}
});
</script>
</body></html>"""
    }

    /**
     * Preload pages around the current page for smooth swiping.
     */
    suspend fun preloadAround(currentPage: Int, range: Int = 3) {
        val start = (currentPage - range).coerceAtLeast(1)
        val end = (currentPage + range).coerceAtMost(604)
        for (page in start..end) {
            val cached = svgCacheMutex.withLock { svgCache.containsKey(page) }
            if (!cached) {
                loadSvgContent(page)
            }
        }
    }

    /**
     * Clear all caches.
     */
    suspend fun clearCache() {
        svgCacheMutex.withLock { svgCache.clear() }
        htmlCacheMutex.withLock { htmlCache.clear() }
    }

    // --- Private helpers ---

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun loadSvgFromBundle(pageNumber: Int): String? {
        val fileName = pageNumber.toString().padStart(3, '0')

        // Try extracted directory first (Library/Application Support/quran-svg/)
        val extractedDir = SVGAssetExtractor.getExtractedSvgDirectory()
        val extractedPath = "$extractedDir/$fileName.svg"
        if (NSFileManager.defaultManager.fileExistsAtPath(extractedPath)) {
            return try {
                val data = NSData.dataWithContentsOfFile(extractedPath)
                if (data != null && data.length.toInt() > 0) {
                    val bytes = ByteArray(data.length.toInt())
                    bytes.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), data.bytes, data.length)
                    }
                    bytes.decodeToString()
                } else null
            } catch (e: Exception) {
                println("SVGAssetLoader: Error loading extracted page $pageNumber: ${e.message}")
                null
            }
        }

        // Fallback: try bundle directly (in case SVGs are uncompressed in bundle)
        val bundle = NSBundle.mainBundle
        val path = bundle.pathForResource(fileName, "svg", "quran-svg")
            ?: run {
                val bundlePath = bundle.bundlePath
                val directPath = "$bundlePath/quran-svg/$fileName.svg"
                if (NSFileManager.defaultManager.fileExistsAtPath(directPath)) directPath else null
            }

        if (path == null) return null

        return try {
            val data = NSData.dataWithContentsOfFile(path)
            if (data != null && data.length.toInt() > 0) {
                val bytes = ByteArray(data.length.toInt())
                bytes.usePinned { pinned ->
                    memcpy(pinned.addressOf(0), data.bytes, data.length)
                }
                bytes.decodeToString()
            } else {
                null
            }
        } catch (e: Exception) {
            println("SVGAssetLoader: Error loading page $pageNumber: ${e.message}")
            null
        }
    }

    companion object {
        /**
         * Blend two colors: result = foreground * ratio + background * (1 - ratio)
         */
        fun blendColors(foreground: Color, background: Color, ratio: Float): Color {
            return Color(
                red = foreground.red * ratio + background.red * (1f - ratio),
                green = foreground.green * ratio + background.green * (1f - ratio),
                blue = foreground.blue * ratio + background.blue * (1f - ratio),
                alpha = 1f
            )
        }

        /**
         * Convert Compose Color to hex string like "#rrggbb"
         */
        fun colorToHex(color: Color): String {
            val r = (color.red * 255).toInt().coerceIn(0, 255)
            val g = (color.green * 255).toInt().coerceIn(0, 255)
            val b = (color.blue * 255).toInt().coerceIn(0, 255)
            return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}"
        }
    }
}
