# Hadith Library Feature - iOS Implementation Reference

Complete reference for porting the Android Hadith Library feature to iOS (SwiftUI + Core Data / SwiftData).

---

## 1. Feature Overview

A 3-level navigation flow for browsing hadith collections:
1. **Library** (bookshelf grid) - shows "My Library" (bundled + downloaded) and "Available for Download"
2. **Book** (chapter TOC) - expandable chapter list with hadith counts
3. **Reader** (swipe pager) - horizontal paging through individual hadiths

Plus a **Search** screen with debounced full-text search across all downloaded books.

Entry points:
- Home screen FeatureCard (purple, `menubook` icon)
- Overflow menu item on every major screen (labeled "Hadith Library" / "المكتبة الحديثية")

---

## 2. Data Source

**GitHub repo**: [AhmedBaset/hadith-json](https://github.com/AhmedBaset/hadith-json)

Download URL pattern:
```
https://raw.githubusercontent.com/AhmedBaset/hadith-json/main/db/by_book/{folder}/{bookId}.json
```

Folders: `the_9_books`, `forties`, `other_books`

### JSON Schema

```json
{
  "id": 1,
  "metadata": {
    "id": 1,
    "length": 7563,
    "arabic": { "title": "...", "author": "...", "introduction": "..." },
    "english": { "title": "...", "author": "...", "introduction": "..." }
  },
  "chapters": [
    { "id": 1, "bookId": 1, "arabic": "...", "english": "..." }
  ],
  "hadiths": [
    {
      "id": 1,
      "idInBook": 1,
      "chapterId": 1,
      "bookId": 1,
      "arabic": "...",
      "english": {
        "narrator": "Narrated ...",
        "text": "..."
      }
    }
  ]
}
```

**Important**: The JSON `id` fields are numeric integers. The app maps them to string `bookId` keys internally (e.g., `"bukhari"`, `"muslim"`).

---

## 3. Book Registry (16 books)

| bookId | Arabic Title | English Title | Author (Arabic) | Author (English) | Count | Bundled | Folder |
|--------|-------------|---------------|-----------------|-------------------|-------|---------|--------|
| `bukhari` | صحيح البخاري | Sahih al-Bukhari | الإمام البخاري | Imam Bukhari | 7563 | YES | the_9_books |
| `muslim` | صحيح مسلم | Sahih Muslim | الإمام مسلم | Imam Muslim | 3033 | no | the_9_books |
| `tirmidhi` | جامع الترمذي | Jami at-Tirmidhi | الإمام الترمذي | Imam Tirmidhi | 3956 | no | the_9_books |
| `abudawud` | سنن أبي داود | Sunan Abi Dawud | الإمام أبو داود | Imam Abu Dawud | 5274 | no | the_9_books |
| `nasai` | سنن النسائي | Sunan an-Nasai | الإمام النسائي | Imam an-Nasai | 5758 | no | the_9_books |
| `ibnmajah` | سنن ابن ماجه | Sunan Ibn Majah | الإمام ابن ماجه | Imam Ibn Majah | 4341 | no | the_9_books |
| `malik` | موطأ مالك | Muwatta Malik | الإمام مالك | Imam Malik | 1832 | no | the_9_books |
| `ahmed` | مسند أحمد | Musnad Ahmad | الإمام أحمد | Imam Ahmad | 4305 | no | the_9_books |
| `darimi` | سنن الدارمي | Sunan ad-Darimi | الإمام الدارمي | Imam ad-Darimi | 3367 | no | the_9_books |
| `nawawi40` | الأربعون النووية | 40 Hadith Nawawi | الإمام النووي | Imam Nawawi | 42 | YES | forties |
| `qudsi40` | الأحاديث القدسية | 40 Hadith Qudsi | مجموعة علماء | Various Scholars | 40 | no | forties |
| `riyad_assalihin` | رياض الصالحين | Riyad as-Salihin | الإمام النووي | Imam Nawawi | 1896 | YES | other_books |
| `bulugh_almaram` | بلوغ المرام | Bulugh al-Maram | ابن حجر العسقلاني | Ibn Hajar al-Asqalani | 1358 | no | other_books |
| `aladab_almufrad` | الأدب المفرد | Al-Adab Al-Mufrad | الإمام البخاري | Imam Bukhari | 1322 | no | other_books |
| `mishkat_almasabih` | مشكاة المصابيح | Mishkat al-Masabih | الخطيب التبريزي | al-Khatib at-Tabrizi | 6285 | no | other_books |
| `shamail_muhammadiyah` | الشمائل المحمدية | Shamail Muhammadiyah | الإمام الترمذي | Imam Tirmidhi | 399 | no | other_books |

**Bundled books** (included in app bundle): `bukhari` (13 MB), `nawawi40` (70 KB), `riyad_assalihin` (2.1 MB). Total ~15.2 MB.

The bundled JSON files live at: `assets/hadith/{bookId}.json`

---

## 4. Database Schema

Three tables:

### hadith_books
```sql
CREATE TABLE hadith_books (
    id TEXT NOT NULL PRIMARY KEY,
    titleArabic TEXT NOT NULL,
    titleEnglish TEXT NOT NULL,
    authorArabic TEXT NOT NULL,
    authorEnglish TEXT NOT NULL,
    hadithCount INTEGER NOT NULL,
    isBundled INTEGER NOT NULL,
    isDownloaded INTEGER NOT NULL DEFAULT 0,
    downloadedAt INTEGER  -- epoch millis, nullable
);
```

### hadith_chapters
```sql
CREATE TABLE hadith_chapters (
    bookId TEXT NOT NULL,
    chapterId INTEGER NOT NULL,
    titleArabic TEXT NOT NULL,
    titleEnglish TEXT NOT NULL,
    PRIMARY KEY(bookId, chapterId)
);
```

### hadiths
```sql
CREATE TABLE hadiths (
    bookId TEXT NOT NULL,
    hadithId INTEGER NOT NULL,
    idInBook INTEGER NOT NULL,
    chapterId INTEGER NOT NULL,
    textArabic TEXT NOT NULL,
    narratorEnglish TEXT NOT NULL,
    textEnglish TEXT NOT NULL,
    PRIMARY KEY(bookId, hadithId)
);
CREATE INDEX index_hadiths_bookId_chapterId ON hadiths(bookId, chapterId);
```

### Search Queries (LIKE, not FTS)
FTS4 was removed due to ANR issues during migration on slow devices. Use simple LIKE queries:
```sql
-- Global search (limit 100)
SELECT * FROM hadiths WHERE textArabic LIKE '%' || ? || '%' OR textEnglish LIKE '%' || ? || '%' LIMIT 100

-- Scoped to book
SELECT * FROM hadiths WHERE bookId = ? AND (textArabic LIKE '%' || ? || '%' OR textEnglish LIKE '%' || ? || '%') LIMIT 100
```

---

## 5. JSON Parsing Strategy

Use **streaming parser** (not load-all-into-memory) for large books like Bukhari (13 MB, 7500+ hadiths):
- Parse chapters array first, accumulate into list
- Parse hadiths array with **batch inserts** every 500 hadiths
- Insert remaining hadiths after loop ends
- Insert chapters after all hadiths

iOS equivalent: Use `JSONDecoder` with streaming or parse in chunks. For very large files, consider `JSONSerialization` with incremental reading.

---

## 6. Initialization Flow

On first launch of HadithLibraryScreen:
1. Check if `hadith_books` table has any rows
2. If empty, insert all 16 book metadata rows (catalog)
3. Load the 3 bundled books from app assets using streaming parser
4. Mark bundled books as `isDownloaded = true`

This runs in `init` of HadithLibraryViewModel on a background thread.

---

## 7. Download Flow

For non-bundled books:
1. Set download state to `DOWNLOADING`, progress 0
2. Download JSON from GitHub URL to temp file with progress tracking (0% - 70%)
3. Rename temp file to final file
4. Set state to `PARSING`, progress 75%
5. Stream-parse JSON and batch-insert into DB
6. Update book row: `isDownloaded = true`, `downloadedAt = now`
7. Set state to `COMPLETED`, progress 100%

Downloaded files stored at: `{app_files}/hadith/{bookId}.json`

Progress model:
```
state: IDLE | DOWNLOADING | PARSING | COMPLETED | ERROR
progress: Float (0.0 - 1.0)
bookId: String?
errorMessage: String?
```

---

## 8. Colors

### Book Card Background Colors (used for gradient cards, white text on top)

| bookId | Light Mode (Card BG) | Hex |
|--------|---------------------|-----|
| bukhari | deep green | `#1B5E20` |
| muslim | deep blue | `#0D47A1` |
| nawawi40 | brown | `#4E342E` |
| riyad_assalihin | indigo | `#1A237E` |
| tirmidhi | dark pink | `#880E4F` |
| abudawud | teal | `#004D40` |
| nasai | deep purple | `#311B92` |
| ibnmajah | deep orange | `#BF360C` |
| malik | light green dark | `#33691E` |
| ahmed | light blue dark | `#01579B` |
| darimi | brown dark | `#3E2723` |
| qudsi40 | cyan dark | `#006064` |
| bulugh_almaram | blue grey dark | `#263238` |
| aladab_almufrad | indigo | `#1A237E` |
| mishkat_almasabih | brown | `#4E342E` |
| shamail_muhammadiyah | dark pink | `#880E4F` |
| fallback | blue grey | `#37474F` |

### Book Text Accent Colors (for text/icons on dark backgrounds in dark mode)

These lighter variants are used wherever the accent color appears as **text or icon tint** (chapter numbers, "Read Hadiths" button, reader nav icons, chapter name bar, search headers). In light mode, the card background color doubles as the text color. In dark mode, use these bright variants:

| bookId | Dark Mode Text Color | Hex |
|--------|---------------------|-----|
| bukhari | light green | `#81C784` |
| muslim | light blue | `#64B5F6` |
| nawawi40 | light brown | `#BCAAA4` |
| riyad_assalihin | light indigo | `#9FA8DA` |
| tirmidhi | light pink | `#F48FB1` |
| abudawud | light teal | `#80CBC4` |
| nasai | light purple | `#B39DDB` |
| ibnmajah | light orange | `#FFAB91` |
| malik | light green | `#A5D6A7` |
| ahmed | light blue | `#81D4FA` |
| darimi | light brown | `#BCAAA4` |
| qudsi40 | light cyan | `#80DEEA` |
| bulugh_almaram | light blue grey | `#B0BEC5` |
| aladab_almufrad | light indigo | `#9FA8DA` |
| mishkat_almasabih | light brown | `#BCAAA4` |
| shamail_muhammadiyah | light pink | `#F48FB1` |
| fallback | light blue grey | `#B0BEC5` |

**Rule**: For card backgrounds/gradients, always use the dark accent with `Color.White` text on top (works in both modes). For text/icon tints on the app's screen/card backgrounds, use dark accent in light mode and bright accent in dark mode.

---

## 9. Screen Details

### 9.1 Hadith Library Screen (Bookshelf)

**Route**: `hadithLibrary`

**Layout**:
- TopAppBar with title "المكتبة الحديثية" / "Hadith Library", search icon, overflow menu
- `LazyVerticalGrid` with 2 columns, 12dp spacing
- Section header "مكتبتي" / "My Library" (books where `isBundled || isDownloaded`)
- Section header "متاح للتحميل" / "Available for Download" (books where `!isBundled && !isDownloaded`)

**BookCoverCard**:
- Aspect ratio: `0.85` (width:height)
- Rounded corners: 12dp
- Vertical gradient background using book accent color (85% alpha top, 100% bottom)
- White text, centered layout:
  - Title (Arabic): Scheherazade font, 18sp bold, max 3 lines
  - Author (Arabic): 12sp, 70% alpha white
  - Bottom: hadith count badge (pill shape, 20% alpha white bg) OR download icon (`CloudDownload`)
- During download: `CircularProgressIndicator` with progress value
- Elevation: 4dp
- Haptic feedback on tap

### 9.2 Hadith Book Screen (Chapter TOC)

**Route**: `hadithBook/{bookId}`

**Layout**:
- TopAppBar with book title, back button, search icon
- Subtitle bar: author name + "N حديث" / "N hadiths", centered
- `LazyColumn` of chapter cards

**Chapter Card**:
- Rounded corners: 10dp, card background
- Row: chapter number circle (36dp, accent bg at 15% alpha, accent text) + chapter title + hadith count + expand icon
- Chapter number uses `textAccentColor` (bright in dark mode)
- Expandable: on tap, shows divider + "قراءة الأحاديث ←" / "Read Hadiths →" button in `textAccentColor`
- In English mode: shows Arabic subtitle below English chapter title
- `animateContentSize()` for smooth expand/collapse

### 9.3 Hadith Reader Screen (Swipe Pager)

**Route**: `hadithReader/{bookId}/{chapterId}/{hadithIndex}`

**Layout**:
- TopAppBar: book title + "N/total" subtitle, back button, share button
- Chapter name bar: light accent background (10% alpha), `textAccentColor` text
- `HorizontalPager` for swiping between hadiths
- Each page: hadith number badge + HadithCard (scrollable)
- Bottom bar: prev/next `AccessibleIconButton` (auto-mirrored arrows) + "N / total" indicator

**HadithCard** (bilingual display):
- **Arabic mode** (`isArabic = true`): Arabic text first, divider, English text
- **English mode** (`isArabic = false`): English text first, divider, Arabic text
- Arabic: Scheherazade font, 24sp, 44sp line height, RTL direction, `textPrimary` color
- English narrator: 14sp bold, `textSecondary` color
- English text: 15sp, 24sp line height, `textPrimary` color
- Divider: 0.5dp, `divider` color

**Share format**:
```
{Arabic text}

{Narrator (if present)}
{English text}

— {Book English Title}, Hadith {idInBook}
```

### 9.4 Hadith Search Screen

**Route**: `hadithSearch?bookId={bookId}` (bookId optional, scopes search to one book)

**Layout**:
- TopAppBar with inline `OutlinedTextField`, auto-focused with 300ms delay
- Debounced search: 300ms delay after typing stops
- Results grouped by book with book title header in `textAccentColor`
- Each result: card with hadith number, Arabic preview (2 lines), English preview (1 line)
- Loading spinner during search
- "لا توجد نتائج" / "No results found" empty state

---

## 10. Navigation Routes

```
hadithLibrary                                    → HadithLibraryScreen
hadithBook/{bookId}                              → HadithBookScreen
hadithReader/{bookId}/{chapterId}/{hadithIndex}   → HadithReaderScreen
hadithSearch?bookId={bookId}                      → HadithSearchScreen
```

All routes support Arabic/English via `AppLanguage` setting with RTL/LTR layout direction switching.

---

## 11. Accessibility (TalkBack / VoiceOver)

- BookCoverCard: `contentDescription = "{titleArabic}, {hadithCount} أحاديث"`, `role = Button`
- Chapter cards: `contentDescription = "باب {titleArabic}, {count} أحاديث"`, `stateDescription = "مفتوح"/"مغلق"`
- Section headers: marked as `heading()`
- HadithCard: `contentDescription = {textArabic}`
- All icon buttons use `AccessibleIconButton` wrapper with Arabic/English descriptions
- Navigation icons use auto-mirrored variants for RTL support

---

## 12. Typography

- Arabic titles and body text: **Scheherazade** font (same as Quran reader)
- English text: system default font
- Font sizes follow the spec in each screen section above

---

## 13. Home Screen Integration

Add a FeatureCard in the home screen grid:
- Label: "الحديث" / "Hadith"
- Icon: `MenuBook` (auto-mirrored)
- Color: `purple` from AppTheme.colors
- Position: in the features grid, between existing feature rows

---

## 14. Overflow Menu Integration

Add "المكتبة الحديثية" / "Hadith Library" item to CommonOverflowMenu:
- Icon: `MenuBook` (auto-mirrored)
- Callback: `onNavigateToHadith`
- Hide flag: `hideHadith = true` when already on the Hadith Library screen
- Added to all 10+ screens that use CommonOverflowMenu

---

## 15. Key Implementation Notes

1. **No FTS**: Full-text search was intentionally removed. Use SQL LIKE queries with LIMIT 100. FTS4 virtual table creation caused ANR on slow devices during DB migration.

2. **Streaming JSON parser**: Essential for Bukhari (13 MB). Do not load entire file into memory.

3. **Batch inserts**: Insert hadiths in batches of 500 to avoid memory pressure and keep the UI responsive.

4. **First-launch catalog**: All 16 books are registered in the catalog (DB) on first launch, even non-bundled ones. This lets the "Available for Download" section appear immediately.

5. **Language-aware text ordering**: HadithCard shows Arabic first in Arabic mode, English first in English mode. This is controlled by the `isArabic` parameter.

6. **Dark mode accent colors**: Book accent colors are dark (Material 800-900 tones) for card backgrounds. A separate lighter palette (Material 200-300 tones) is used for text/icon tints in dark mode to maintain readability.

7. **Download progress**: Progress 0-70% = download, 75% = parsing state, 75-100% = DB insertion. Exposed as a reactive state (StateFlow on Android, use `@Published`/Combine on iOS).

8. **File storage**: Downloaded books saved to app's files directory under `hadith/` subfolder. Temp files use `{bookId}_temp.json` pattern, renamed on completion.
