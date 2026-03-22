# Hadith Screens — New Design Specification

Source: `stitch_export/Hadith/` (screen.png + code.html)

## Covers: HadithLibraryScreen, HadithBookScreen, HadithReaderScreen, HadithSearchScreen

---

## Design Colors (from Stitch Hadith design)

| Token | Hex | Usage |
|-------|-----|-------|
| `brand-green` (header) | `#275239` | TopAppBar background — **app-wide standard** |
| `brand-gold` | `#CDAD70` | TopAppBar text, section headers, gold accents |
| `bg-cream` | `#F9F6F0` / `#F9F6EE` | Screen background |
| `book-blue` | `#233B65` | Book cover color 1 |
| `book-brown` | `#6D4321` | Book cover color 2 |
| `book-green` | `#265338` | Book cover color 3 |
| `book-teal` | `#287A76` | Book cover color 4 |
| `badge-bg` | `#E6CE93` | Hadith count badge background |
| `BrandGold` (inner) | `#CDAD70` | Book title text, inner border, author pill |

---

## HadithLibraryScreen (Bookshelf)

### Header
- Background: `#275239` (brand-green)
- Title: "المكتبة الحديثية" in gold `#CDAD70`
- Search icon in gold
- **No overflow menu** — all nav via bottom bar

### Grid: **3 columns** (was 2)
- Gap: 8dp horizontal, 8dp vertical
- Padding: 12dp

### Section Headers
- "مكتبتي" / "متاح للتحميل"
- Font: 20sp bold, scheherazade
- Color: `#CDAD70` (gold)
- Span: full 3 columns

### BookCoverCard (Stitch design)

```
┌───────────────┐
│  ┌───────────┐│  ← 5dp padding (book edge)
│  │  gold bdr ││  ← 1dp gold border
│  │           ││
│  │  رياض     ││  ← gold title, 16sp bold, scheherazade
│  │ الصالحين  ││
│  │           ││
│  │ ┌───────┐ ││  ← author in gold bordered pill, 7sp
│  │ │النووي │ ││
│  │ └───────┘ ││
│  └───────────┘│
│         [1896]│  ← badge-bg (#E6CE93), bottom-right, 9sp bold black
└───────────────┘
```

**Structure:**
```kotlin
Card(
    aspectRatio = 0.75f,   // 3:4 proportion
    shape = RoundedCornerShape(6.dp),
    color = bookColor,     // rotating: blue, brown, green, teal
    elevation = 2.dp
) {
    Box(padding = 5.dp) {       // book edge
        Box(border = 1.dp gold, rounded 4.dp) {  // inner frame
            Column(padding = 6.dp, center, spaceBetween) {
                Text(title, gold, 16sp, Bold, scheherazade, center, max 3 lines)
                Spacer(weight 1)
                Box(border = 0.5.dp gold) { Text(author, gold, 7sp) }  // author pill
            }
            // Badge: bottom-right, offset(4,4), #E6CE93 bg, black text 9sp bold
            Box(align = BottomEnd, offset = (4,4)) {
                Text(hadithCount, black, 9sp, Bold, #E6CE93 bg)
            }
        }
    }
}
```

### Book Color Rotation
4 colors cycle based on `bookId.hashCode() % 4`:
- `#233B65` (blue)
- `#6D4321` (brown)
- `#265338` (green)
- `#287A76` (teal)

### Download State
- Not downloaded: CloudDownload icon in gold at 70% alpha (instead of author pill + badge)
- Downloading: CircularProgressIndicator in gold

---

## HadithBookScreen (Chapter TOC)

- TopAppBar: `#275239` bg, `#CDAD70` gold text
- Screen bg: `#F9F6EE`
- Chapter cards: white bg, book accent color for number circle
- Expanded: "Read Hadiths" + "Search" buttons
- Bottom nav with currentRoute = "hadithBook"

---

## HadithReaderScreen (Pager) — NO Bottom Nav

- TopAppBar: `#275239` bg, `#CDAD70` gold
- Copy + Share buttons in header
- Chapter name bar with accent bg
- Per-hadith action row: Copy Arabic + Share
- Go-to-hadith dialog
- Chapter prev/next navigation in bottom bar

---

## HadithSearchScreen — NO Bottom Nav

- TopAppBar: `#275239` bg, gold accents
- Inline search field with gold cursor/placeholder
- Results grouped by book with gold book title headers
- Copy/Share per result

---

## Bottom Navigation
- Hadith Library: shows bottom bar, Hadith hidden from "More"
- Hadith Book: shows bottom bar
- Hadith Reader: no bottom bar (own navigation)
- Hadith Search: no bottom bar (keyboard)

---

## iOS Notes
- 3-column grid: `LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())])`
- Book card: `ZStack` with colored bg, inner `RoundedRectangle` stroke in gold
- Badge: `.overlay(alignment: .bottomTrailing)` with offset
- Cairo font or SF Arabic for titles
