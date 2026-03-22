# SVG Ayah Tap-and-Hold Interaction

Complete implementation reference for the long-press gesture system on SVG Mushaf pages.
Covers touch detection, ayah identification, highlight rendering, context menu, audio playback, and tafseer display.

---

## Architecture Overview

```
User Long-Press on SVG Page
         |
         v
  SvgAyahOverlay (transparent Canvas over the rendered SVG bitmap)
         |
    detectTapGestures(onLongPress)
         |
         v
  Screen Coordinates --> SVG Picture Coordinates --> Line Slot --> Word Index --> Ayah
         |
         v
  onAyahLongPress(ayah, globalOffset) callback to QuranReaderScreen
         |
         v
  Context Menu (floating pill with 5 icon buttons)
         |
    +----+----+----+----+----+
    | Play | Copy | Bookmark | Tafseer | Repeat |
    +----+----+----+----+----+
```

**Key files:**
- `SVGPageComposable.kt` — SVG rendering, content bounds detection, overlay + highlight drawing, gesture handling
- `QuranReaderScreen.kt` — Context menu UI, action dispatching
- `QuranReaderViewModel.kt` — `playFromAyah()`, `showTafseer()`, `HighlightedAyah` model
- `QCFPageData.kt` — Data models (`QCFPageData`, `QCFLineData`, `QCFWordData`)

---

## 1. Data Models

### QCFPageData (mushaf-layout JSON)

Each Mushaf page has a corresponding JSON entry loaded from bundled `mushaf-layout` data:

```kotlin
data class QCFPageData(
    val page: Int,               // Page number (1-604)
    val lines: List<QCFLineData> // 15 lines per page
)

data class QCFLineData(
    val line: Int?,              // Line number (1-15)
    val type: QCFLineType,       // SURAH_HEADER, BASMALA, or TEXT
    val surah: String?,
    val verseRange: String?,
    val words: List<QCFWordData>? // Only for TEXT lines
)

data class QCFWordData(
    val location: String,   // "surah:verse:wordIndex" e.g. "2:255:1"
    val word: String,       // Arabic Unicode text
    val qpcV2: String,      // QCF V2 glyph code
    val qpcV1: String,      // QCF V1 glyph code
    val isEnd: Boolean?     // True if word is end-of-verse marker (ayah number ornament)
) {
    fun getSurahNumber(): Int? = location.split(":").getOrNull(0)?.toIntOrNull()
    fun getVerseNumber(): Int? = location.split(":").getOrNull(1)?.toIntOrNull()
    fun getWordIndex(): Int? = location.split(":").getOrNull(2)?.toIntOrNull()
}
```

### HighlightedAyah

```kotlin
data class HighlightedAyah(
    val surahNumber: Int,
    val ayahNumber: Int
)
```

### Ayah (domain model)

The `Ayah` domain object contains full ayah data including `surahNumber`, `ayahNumber`, and `textArabic`. The list of ayahs for the current page is passed to `SVGPageComposable`.

### extractAyahRefs() — Page Layout Helper

Extracts unique (surah, ayah) pairs from a page's word data in reading order:

```kotlin
fun QCFPageData.extractAyahRefs(): List<Pair<Int, Int>> {
    val refs = linkedSetOf<Pair<Int, Int>>()
    for (line in lines) {
        val words = line.words ?: continue
        for (word in words) {
            val surah = word.getSurahNumber() ?: continue
            val verse = word.getVerseNumber() ?: continue
            refs.add(surah to verse)
        }
    }
    return refs.toList()
}
```

Used by the ayah augmentation system (see section 1b below).

---

## 1b. Ayah Augmentation (Cross-Page Surah Fix)

### Problem

The database query `SELECT * FROM ayahs WHERE page = :pageNumber` assigns each ayah to a single page. When a surah spans two pages, ayahs that visually appear on the second page may have `page` set to the first page (where the surah starts). This causes:

- **Play button** plays the wrong ayah (skips ayahs not in the DB result)
- **Long-press** can't find the ayah in the list (returns no context menu)

Example: Page 599 visually shows Surah 98:6–8 (lines 1–5), but the DB assigns them to page 598 (where Surah 98 begins).

### Solution

`augmentAyahsFromPageLayout()` cross-references the QCF page layout JSON (authoritative source for what appears on each visual page) with the database results:

```kotlin
private suspend fun augmentAyahsFromPageLayout(pageNumber: Int, dbAyahs: List<Ayah>): List<Ayah> {
    val pageData = qcfAssetLoader.loadPageData(pageNumber) ?: return dbAyahs
    val layoutRefs = pageData.extractAyahRefs()
    if (layoutRefs.isEmpty()) return dbAyahs

    val existingRefs = dbAyahs.map { it.surahNumber to it.ayahNumber }.toSet()
    val missingRefs = layoutRefs.filter { it !in existingRefs }
    if (missingRefs.isEmpty()) return dbAyahs

    val additionalAyahs = missingRefs.mapNotNull { (surah, verse) ->
        quranRepository.getAyah(surah, verse)
    }
    val allAyahs = (dbAyahs + additionalAyahs).associateBy { it.surahNumber to it.ayahNumber }
    return layoutRefs.mapNotNull { ref -> allAyahs[ref] }
}
```

Applied in both `loadPage()` and `getAyahsForPage()`:

```kotlin
val dbAyahs = quranRepository.getAyahsByPage(pageNumber).first()
val ayahs = augmentAyahsFromPageLayout(pageNumber, dbAyahs)
```

**Why this matters for iOS:** Any platform using a `page`-column DB query for ayahs must augment results with the QCF page layout data. The layout JSON is the source of truth for which ayahs visually appear on each page.

---

## 2. SVG Rendering Pipeline

### Step 1: Render SVG to Bitmap

```
SVG String --> recolorSvg() (apply theme colors) --> SVG.getFromString()
    --> renderToPicture(picW, picH)
    --> detectHorizontalBounds() (pixel scan for left/right)
    --> parseTextAreaFromSvg() (per-page clip path for top/bottom)
    --> scale & center onto Bitmap
```

The SVG is rendered to an Android `Picture` at its native dimensions (`picW x picH`), then painted onto a `Bitmap` at the device screen size with scaling and offset.

### Step 2: Content Bounds Detection

Content bounds are detected using two complementary methods:

#### Horizontal Bounds — Pixel Scanning

`detectHorizontalBounds()` pixel-scans the rendered picture to find left/right text edges:

```kotlin
private fun detectHorizontalBounds(
    picture: Picture,
    picW: Int, picH: Int,
    bgColor: Int
): Pair<Int, Int>  // (left, right) in picture coordinates
```

1. Renders the picture onto a probe bitmap
2. Scans every pixel, counting foreground pixels per column
3. Foreground = pixel with alpha > 16 AND not near the background color (tolerance=10)
4. Finds left/right bounds using column coverage thresholds (strict 8%, relaxed 2%)

#### Vertical Bounds — Per-Page SVG Clip Path Parsing

Each SVG page contains a rectangular clip path that defines the 15-line text content area. These clip paths **vary per page** (top ranges 93–103, bottom ranges 627–637 in post-transform SVG coordinates). Using fixed constants causes up to ~20% line-height shifting on some pages.

`parseTextAreaFromSvg()` extracts the per-page text area at render time:

```kotlin
private fun parseTextAreaFromSvg(svgContent: String): Pair<Float, Float>?
```

1. Uses regex to find all `M x1,y1 H x2 V y2 H x1 Z` rectangular paths in the SVG
2. Filters to rectangles with area 80K–110K (text area range), excluding the universal outer frame
3. Applies the shared SVG transform (`y_post = -1.3333333 * y_pre + 729.448`) to get post-transform Y coordinates
4. Returns `(topSvg, bottomSvg)` or null if no matching clip path found

The post-transform coordinates are then mapped to picture pixel coordinates via the viewBox:

```kotlin
val textArea = parseTextAreaFromSvg(svgContent)
val textAreaTop = textArea?.first ?: TEXT_AREA_TOP_SVG      // fallback: 98.115
val textAreaBottom = textArea?.second ?: TEXT_AREA_BOTTOM_SVG // fallback: 630.741

contentTop = ((textAreaTop - vb.top) * picH / vb.height()).toInt()
contentBottom = ((textAreaBottom - vb.top) * picH / vb.height()).toInt()
```

**Coverage:** 554 of 604 pages (92%) have parseable clip paths. The remaining 48 pages use fallback constants (averaged from all pages).

**Why this matters for iOS:** The content bounds are essential for accurate touch-to-ayah mapping. Horizontal bounds handle variable left/right padding. Vertical bounds must be per-page to avoid cumulative line-slot shifting — do NOT use a single fixed constant for all pages.

### Step 3: Scale and Offset Calculation

```kotlin
val contentW = hBounds.second - hBounds.first + 1       // From horizontal pixel scanning
val scale = containerW.toFloat() / contentW.toFloat()    // Scale content to fill screen width
val offsetX = -hBounds.first * scale                     // Shift left edge to x=0
val offsetY = (containerH - picH * scale) / 2f           // Center vertically
```

These values (`scale`, `offsetX`, `offsetY`) are stored in `RenderedSvgPage` and used by both the overlay gesture handler and the highlight renderer.

### RenderedSvgPage

```kotlin
private data class RenderedSvgPage(
    val bitmap: Bitmap,
    val outputHeightPx: Int,
    val scale: Float,          // SVG-to-screen scale factor
    val offsetX: Float,        // Canvas X translation
    val offsetY: Float,        // Canvas Y translation
    val picH: Int,             // SVG picture height
    val contentLeft: Int,      // Content bounds in SVG coords
    val contentRight: Int,
    val contentTop: Int,
    val contentBottom: Int
)
```

---

## 3. Gesture Detection (Long-Press to Ayah)

The `SvgAyahOverlay` composable is a transparent `Canvas` layered on top of the SVG bitmap image. It handles both tap (toggle UI) and long-press (ayah selection).

### Coordinate Transformation Pipeline

```
Screen Touch (pixels)
    |
    | Step 1: Reverse the canvas transform to get SVG picture coordinates
    v
SVG Picture Coordinates
    |
    | Step 2: Map Y to line slot (1-15)
    v
Line Slot --> Find matching TEXT line in QCFPageData
    |
    | Step 3: Map X to word index (RTL-aware)
    v
Word Index --> Extract surah:verse from QCFWordData.location
    |
    | Step 4: Find Ayah object from page ayahs list
    v
Ayah object + global offset --> callback to parent
```

### Step 1: Screen to SVG Picture Coordinates

```kotlin
val pictureY = (localOffset.y - page.offsetY) / page.scale
val pictureX = (localOffset.x - page.offsetX) / page.scale
```

This reverses the `canvas.translate(offsetX, offsetY)` + `canvas.scale(scale, scale)` applied during rendering.

### Step 2: Y-Coordinate to Line Slot

Every Mushaf page has exactly **15 line slots** (constant `TOTAL_LINE_SLOTS = 15`), dividing the content region into equal-height rows:

```kotlin
val contentTopPic = page.contentTop.toFloat()
val contentHeightPic = (page.contentBottom - page.contentTop).toFloat()
val lineSlotH = contentHeightPic / TOTAL_LINE_SLOTS  // Height of each slot

val lineSlot = floor((pictureY - contentTopPic) / lineSlotH).toInt() + 1  // 1-based
```

Then find the matching TEXT line:

```kotlin
val textLine = pageData.lines.find {
    it.line == lineSlot && it.type == QCFLineType.TEXT
}
val words = textLine?.words
if (words.isNullOrEmpty()) return  // Tapped on header/basmala/empty line
```

### Step 3: X-Coordinate to Word Index (RTL)

Quran text is **right-to-left**. Word 0 (first word) is at the right edge, last word at the left edge:

```kotlin
val contentWidth = (page.contentRight - page.contentLeft + 1).toFloat()
val relativeX = ((pictureX - page.contentLeft) / contentWidth).coerceIn(0f, 1f)

// RTL: right side (relativeX=1.0) = first word (index 0)
//      left side  (relativeX=0.0) = last word  (index N-1)
val wordIndex = ((1f - relativeX) * words.size).toInt().coerceIn(0, words.lastIndex)
```

### Step 4: Word to Ayah

```kotlin
val tappedWord = words[wordIndex]
val surah = tappedWord.getSurahNumber()  // Parse "2:255:1" -> 2
val verse = tappedWord.getVerseNumber()  // Parse "2:255:1" -> 255

val ayah = ayahs.find { it.surahNumber == surah && it.ayahNumber == verse }
if (ayah != null) {
    val globalOffset = Offset(
        x = overlayPosition.x + localOffset.x,  // Convert to root coordinates for menu positioning
        y = overlayPosition.y + localOffset.y
    )
    onAyahLongPress(ayah, globalOffset)
}
```

`overlayPosition` is tracked via `Modifier.onGloballyPositioned { coordinates -> overlayPosition = coordinates.positionInRoot() }`.

### Haptic Feedback

Long-press triggers haptic feedback immediately, before coordinate calculation:

```kotlin
haptic.performHapticFeedback(HapticFeedbackType.LongPress)
```

---

## 4. Ayah Highlight Rendering

The highlight is drawn in the same `Canvas` that handles gestures. It renders a semi-transparent rounded rectangle over each line segment that belongs to the highlighted ayah.

### Multi-Line Highlight

An ayah can span multiple lines. The system iterates all lines and highlights word ranges independently per line:

```kotlin
if (highlightedAyah != null && pageData != null) {
    // Convert content bounds to bitmap coordinates
    val contentLeftBm = page.contentLeft * page.scale + page.offsetX
    val contentRightBm = (page.contentRight + 1) * page.scale + page.offsetX
    val bitmapContentW = contentRightBm - contentLeftBm
    val cornerR = CornerRadius(4.dp.toPx())

    pageData.lines.forEach { line ->
        if (line.type != QCFLineType.TEXT) return@forEach
        val words = line.words ?: return@forEach
        val lineNum = line.line ?: return@forEach

        // Find first and last word of highlighted ayah in this line
        val firstIdx = words.indexOfFirst { word ->
            word.getSurahNumber() == highlightedAyah.surahNumber &&
                word.getVerseNumber() == highlightedAyah.ayahNumber
        }
        if (firstIdx < 0) return@forEach  // Ayah not on this line

        val lastIdx = words.indexOfLast { word ->
            word.getSurahNumber() == highlightedAyah.surahNumber &&
                word.getVerseNumber() == highlightedAyah.ayahNumber
        }

        // Vertical: line slot bounds in bitmap coordinates
        val lineTopPic = contentTopPic + (lineNum - 1) * lineSlotH
        val lineBottomPic = contentTopPic + lineNum * lineSlotH
        val lineTopBm = lineTopPic * page.scale + page.offsetY
        val lineBottomBm = lineBottomPic * page.scale + page.offsetY

        // Horizontal: word range in bitmap coordinates (RTL layout)
        val totalWords = words.size.toFloat()
        val highlightLeft = contentRightBm - ((lastIdx + 1) / totalWords) * bitmapContentW
        val highlightRight = contentRightBm - (firstIdx / totalWords) * bitmapContentW

        drawRoundRect(
            color = highlightColor.copy(alpha = 0.3f),
            topLeft = Offset(highlightLeft, lineTopBm),
            size = Size(highlightRight - highlightLeft, lineBottomBm - lineTopBm),
            cornerRadius = cornerR
        )
    }
}
```

### RTL Highlight Positioning

The horizontal position formula maps word indices to screen X using RTL logic:

```
contentRightBm ---|--- word 0 (first word, rightmost)
                  |--- word 1
                  |--- ...
contentLeftBm  ---|--- word N-1 (last word, leftmost)
```

```
highlightLeft  = contentRightBm - ((lastIdx + 1) / totalWords) * contentWidth
highlightRight = contentRightBm - (firstIdx / totalWords) * contentWidth
```

This means:
- `firstIdx = 0` (rightmost word) → `highlightRight = contentRightBm`
- `lastIdx = N-1` (leftmost word) → `highlightLeft = contentLeftBm`

### Highlight Appearance

- Color: `highlightColor.copy(alpha = 0.3f)` — semi-transparent overlay using the theme's highlight color
- Shape: `RoundedRect` with 4dp corner radius
- Covers the full line height for each line segment

---

## 5. Context Menu

When `onAyahLongPress` fires, `QuranReaderScreen` shows a floating context menu at the touch position.

### Menu State

```kotlin
var selectedAyah by remember { mutableStateOf<Ayah?>(null) }
var menuPosition by remember { mutableStateOf(Offset.Zero) }
var showAyahMenu by remember { mutableStateOf(false) }

// In SVGPageComposable callback:
onAyahLongPress = { ayah, position ->
    selectedAyah = ayah
    menuPosition = position
    showAyahMenu = true
}
```

### Menu Positioning

The menu is positioned near the touch point, clamped to screen bounds:

```kotlin
val menuWidthPx = 220.dp   // 5 icon buttons
val menuHeightPx = 48.dp
val padding = 8.dp

// Center horizontally on touch, position above touch
val rawX = menuPosition.x - (menuWidthPx / 2)
val rawY = menuPosition.y - menuHeightPx - padding

// Clamp X to screen bounds
val menuX = rawX.coerceIn(padding, screenWidth - menuWidthPx - padding)

// Clamp Y: if too close to top, show below touch instead
val menuY = if (rawY < padding) {
    menuPosition.y + padding  // Below touch
} else {
    rawY                       // Above touch (preferred)
}
```

### Menu UI

```
+-------------------------------------------+
|  [Play] [Copy] [Bookmark] [Tafseer] [Repeat] |
+-------------------------------------------+
```

- **Shape:** `RoundedCornerShape(24.dp)` — pill shape
- **Background:** White surface with 8dp shadow elevation
- **Icons:** 40x40dp icon buttons, 20-22dp icon size, tinted with `themeColors.accent`
- **Dismiss:** Tap anywhere outside the menu to dismiss (full-screen invisible clickable overlay)

### 5 Actions

| Icon | Action | Implementation |
|------|--------|----------------|
| `PlayArrow` | Play from this ayah | `viewModel.playFromAyah(surah, ayah)` |
| `ContentCopy` | Copy Arabic text | `clipboardManager.setText(ayah.textArabic)` |
| `Bookmark` / `BookmarkBorder` | Toggle page bookmark | `viewModel.toggleBookmarkCurrentPage()` |
| `MenuBook` | Show tafseer | `viewModel.showTafseer(ayah)` |
| `Repeat` | Custom recitation dialog | Opens custom recitation dialog |

---

## 6. Audio Playback (Play from Ayah)

### QuranReaderViewModel.playFromAyah()

```kotlin
fun playFromAyah(surahNumber: Int, ayahNumber: Int) {
    viewModelScope.launch {
        val reciter = state.selectedReciter ?: return@launch
        val surah = quranRepository.getSurahByNumber(surahNumber) ?: return@launch
        val audioVariants = quranRepository.getAudioVariants(reciter.id, surahNumber).first()
        val audioUrl = audioVariants.first().url

        playbackController.playAudio(
            reciterId = reciter.id,
            surahNumber = surahNumber,
            audioUrl = audioUrl,
            surahNameArabic = surah.nameArabic,
            surahNameEnglish = surah.nameEnglish,
            reciterName = reciter.name,
            startFromAyah = ayahNumber,       // <-- Key: starts from this specific ayah
            startFromPositionMs = null         // Auto-calculated from ayah timing data
        )
    }
}
```

The `PlaybackController` uses ayah timing data (timestamps per ayah within the surah audio file) to seek to the correct position.

---

## 7. Tafseer Display

### QuranReaderViewModel.showTafseer()

```kotlin
fun showTafseer(ayah: Ayah) {
    viewModelScope.launch {
        val appLanguage = settings.value.appLanguage.name.lowercase()
        val allSorted = AvailableTafseers.getSortedByLanguage(appLanguage)
        val downloadedTafseers = tafseerRepository.getDownloadedTafseerIds()

        // Show modal immediately with loading spinner
        _tafseerState.value = TafseerModalState(
            isVisible = true,
            surah = ayah.surahNumber,
            ayah = ayah.ayahNumber,
            surahName = surahNamesArabic[ayah.surahNumber] ?: "",
            ayahText = ayah.textArabic,
            allTafseers = allSorted,
            downloadedIds = downloadedTafseers,
            isLoading = true
        )

        // Load tafseer content
        val tafseers = tafseerRepository.getAllTafseersForAyah(ayah.surahNumber, ayah.ayahNumber)

        // Sort by language preference, update modal
        _tafseerState.value = _tafseerState.value.copy(
            isLoading = false,
            availableTafseers = sortedTafseers,
            selectedTafseerId = sortedTafseers.firstOrNull()?.first?.id
        )
    }
}
```

### TafseerModalState

```kotlin
data class TafseerModalState(
    val isVisible: Boolean = false,
    val surah: Int = 1,
    val ayah: Int = 1,
    val surahName: String = "",
    val ayahText: String = "",
    val availableTafseers: List<Pair<TafseerInfo, TafseerContent>> = emptyList(),
    val allTafseers: List<TafseerInfo> = emptyList(),
    val downloadedIds: Set<String> = emptySet(),
    val downloadingTafseerId: String? = null,
    val downloadProgress: Float = 0f,
    val selectedTafseerId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### Tafseer Modal UI

The tafseer modal shows:
1. Ayah text in a rounded card at the top
2. Tab selector for switching between available tafseers
3. Scrollable tafseer content
4. Download button for tafseers not yet downloaded
5. Copy button for content
6. Animated entrance/exit (scale + fade)

---

## 8. iOS/KMP Implementation Notes

### SVG Rendering
- Android uses `com.caverock.androidsvg` (`SVG.getFromString()`, `renderToPicture()`)
- iOS equivalent: Use `SVGKit` or render SVG via `WKWebView` / Core Graphics
- The `recolorSvg()` string replacement approach (find/replace hex colors) is platform-agnostic

### Content Bounds Detection
- **Horizontal bounds:** Android scans bitmap pixels to find left/right foreground content edges
  - iOS: Same approach — render SVG to `CGImage`, scan pixel buffer columns for non-background pixels
  - Constants: `FOREGROUND_ALPHA_THRESHOLD = 16`, `BG_COLOR_TOLERANCE = 10`
- **Vertical bounds:** Parsed from each SVG's text area clip path (NOT pixel scanning)
  - Each SVG has a rectangular clip path (`M x1,y1 H x2 V y2 H x1 Z`) with area ~80K–110K that defines the 15-line text area
  - These coordinates vary per page (top: 93–103, bottom: 627–637 in post-transform SVG coords)
  - Transform: `y_post = -1.3333333 * y_pre + 729.448` (shared by all 604 pages)
  - Fallback for ~48 pages without parseable clip paths: `top=98.115, bottom=630.741`
  - iOS: Parse the SVG string with regex before rendering, same transform math
- `TOTAL_LINE_SLOTS = 15` (always 15 equal-height slots per page)

### Gesture Handling
- Android: Compose `detectTapGestures(onLongPress)`
- iOS (SwiftUI): `.onLongPressGesture(minimumDuration:)` with `DragGesture` for position
- iOS (UIKit): `UILongPressGestureRecognizer`
- The coordinate math is identical: `(touchY - offsetY) / scale` to reverse the rendering transform

### Coordinate System
- The 15-line-slot system and RTL word indexing are pure math — directly portable
- Key formula: `wordIndex = ((1.0 - relativeX) * wordCount).toInt()`
- Content bounds (`left`, `right`, `top`, `bottom`) are in SVG picture coordinates

### Highlight Rendering
- Android: Compose `Canvas.drawRoundRect()`
- iOS (SwiftUI): Overlay `RoundedRectangle` shapes
- iOS (Core Graphics): `CGContext.fill(roundedRect:)`
- Same alpha (0.3) and corner radius (4dp equivalent)

### Page Data & Ayah Augmentation
- `mushaf-layout` JSON is platform-agnostic — parse with Kotlinx Serialization in KMP shared module
- `QCFWordData.location` format `"surah:verse:wordIndex"` is a plain string split
- **Critical:** The DB `page` column for ayahs can be wrong for cross-page surahs. Always augment `getAyahsByPage()` results with ayahs extracted from the QCF page layout JSON (see section 1b). The layout JSON is the authoritative source for which ayahs appear on each visual page.

### Context Menu
- Android: Custom floating `Surface` with `Row` of `IconButton`s
- iOS: Could use `.contextMenu` modifier, or custom positioned popup for visual parity
- Position calculation (center above touch, clamp to screen) is the same

### Audio Playback
- The `startFromAyah` parameter tells the playback controller which ayah to seek to
- Ayah timing data (per-ayah timestamps in the audio file) is needed for seeking
- This data comes from the audio variant metadata

### Tafseer
- Tafseer data is stored locally per surah/ayah
- The repository pattern (`getAllTafseersForAyah`) can be shared in KMP
- UI is a modal bottom sheet with tab selection — standard SwiftUI pattern
