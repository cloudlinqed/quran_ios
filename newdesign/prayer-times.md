# Prayer Times Screen — New Design Specification

## Design System: "The Digital Sanctuary"

Consistent with the main screen redesign. Dark green hero panels, warm backgrounds, gold accents.

---

## Color Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `ScreenBg` | `#F9F6EE` | Screen background |
| `BrandGreen` | `#2B4234` | Next prayer card, Qibla bar |
| `BrandGreenDark` | `#275239` | Top bar, prayer icon circles |
| `BrandGold` | `#CDAD70` | Top bar title, labels, accents |
| `HighlightBg` | `#EFE6D5` | Current/next prayer row highlight |
| `IconCircleBg` | `#F3ECE0` | Prayer icon circle backgrounds |
| `CardWhite` | `#FFFFFF` | Location card, prayer list card |
| Text primary | `#333333` | All body text |
| Text muted | `#666666` | Secondary labels |

---

## Layout Structure

```
┌────────────────────────────────────┐
│  TopAppBar: مواقيت الصلاة          │  ← #275239 bg, #CDAD70 gold text
│  [←]                     [⋮]      │
├────────────────────────────────────┤
│  ┌── Date / Location Card ──────┐  │  ← white card
│  │  📅 ٣ شوال ١٤٤٧              │  │
│  │  📍 Makkah, Saudi Arabia [◎✎]│  │  ← gold icons
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Next Prayer Card ──────────┐  │  ← #2B4234 dark green
│  │  ┌─متبقي──┐    الصلاة القادمة │  │
│  │  │٢١ س ٢٩ د│      الفجر 🌙   │  │  ← large 28sp name, icon circle
│  │  └────────┘    ١٠:٠٧ مساءً  │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Qibla Direction Bar ───────┐  │  ← #2B4234 dark green
│  │  ←    اتجاه القبلة 🧭         │  │  ← #CDAD70 gold text
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Prayer Times List ─────────┐  │  ← white card, rounded top 24dp
│  │      ✦ أوقات الصلاة ✦        │  │  ← #F9F5EC header bg
│  │──────────────────────────────│  │
│  │  🌙 الفجر         ١٠:٠٧ م   │  │  ← highlighted #EFE6D5
│  │  ☀️ الشروق        ١١:٢٣ م   │  │
│  │  ☀️ الظهر          ٥:٢٨ ص   │  │
│  │  🌅 العصر          ٨:٥٣ ص   │  │
│  │  🌅 المغرب        ١١:٣٢ ص   │  │
│  │  🌙 العشاء         ١:٠٢ م   │  │
│  └──────────────────────────────┘  │
└────────────────────────────────────┘
```

---

## Top App Bar

```kotlin
TopAppBarDefaults.topAppBarColors(
    containerColor = Color(0xFF1E3329),     // BrandGreenDark
    titleContentColor = Color(0xFFD3B378),  // BrandGold
    navigationIconContentColor = Color(0xFFD3B378),
    actionIconContentColor = Color(0xFFD3B378)
)
```

- Title: "مواقيت الصلاة" / "Prayer Times" in gold
- Navigation: back arrow in gold
- Actions: CommonOverflowMenu in gold

---

## Date / Location Card

White card, rounded 16dp, no borders.

```kotlin
Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
    Column(padding = 14.dp, spacing = 6.dp) {
        // Hijri date row
        Row {
            Icon(CalendarMonth, tint = BrandGold, size = 18.dp)
            Text(date, scheherazadeFont, 15sp, SemiBold, color = #333333)
        }
        // Location row
        Row {
            Icon(LocationOn, tint = BrandGold, size = 16.dp)
            Text(city + country, 13sp, color = #333333)
            Spacer(weight 1)
            IconButton(MyLocation, tint = BrandGold)    // detect
            IconButton(Edit, tint = BrandGold)          // manual
        }
    }
}
```

---

## Next Prayer Card

Dark green panel with prayer info and countdown.

```kotlin
Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF2B4234)) {
    Row(SpaceBetween, padding = 16.dp) {
        // Countdown box (start side)
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF3A5344)) {
            Column(center, padding = h16/v10) {
                Text("متبقي", 10sp, BrandGold@80%)
                Text("٢١ س ٢٩ د", 16sp, Bold, White)
            }
        }
        // Prayer info + icon (end side)
        Row {
            Column(alignEnd) {
                Text("الصلاة القادمة", 11sp, BrandGold)
                Row(alignBottom) {
                    Text("الفجر", scheherazade, 28sp, Bold, White)
                    Text("١٠:٠٧ مساءً", 12sp, White@70%)
                }
            }
            // Icon circle
            Surface(circle, 48.dp, BrandGreenDark) {
                Icon(prayer_icon, White, 28.dp)
            }
        }
    }
}
```

### Prayer Icons:
| Prayer | Icon | Color in circle |
|--------|------|----------------|
| Fajr | `NightsStay` | White on `#275239` |
| Sunrise | `WbSunny` | White on `#275239` |
| Dhuhr | `WbSunny` | White on `#275239` |
| Asr | `WbTwilight` | White on `#275239` |
| Maghrib | `WbTwilight` | White on `#275239` |
| Isha | `NightsStay` | White on `#275239` |

---

## Qibla Direction Bar

Clickable dark green bar navigating to Qibla screen.

```kotlin
Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF2B4234)) {
    Row(SpaceBetween, padding = h16/v14) {
        Icon(ArrowForward, BrandGold, 20.dp)   // start side
        Row {
            Text("اتجاه القبلة", scheherazade, 16sp, Bold, BrandGold)
            Icon(Explore, BrandGold, 20.dp)     // compass
        }
    }
}
```

---

## Prayer Times List Card

White card with rounded top, section header, and 6 prayer rows.

### Section Header
```kotlin
Box(bg = Color(0xFFF9F5EC), padding = v10, center) {
    Row {
        Text("✦", BrandGold, 10sp)
        Text("أوقات الصلاة", scheherazade, 16sp, Bold, #333333)
        Text("✦", BrandGold, 10sp)
    }
}
```

### Prayer Time Row

Each row has prayer name + icon on the **start side** and time on the **end side**. In Arabic RTL, names appear on the right and times on the left.

```kotlin
Row(SpaceBetween, padding = h10/v10, bg = if(isNext) #EFE6D5 else Transparent) {
    // Start side: icon circle + prayer name
    Row {
        Box(32.dp, circle, bg = #F3ECE0) {
            Icon(prayer_icon, tint = if(fajr/isha) #2B4234 else #CDAD70)
        }
        Spacer(10.dp)
        Text(prayerName, scheherazade, 16sp, Bold, #333333)
    }
    // End side: time pill
    Surface(rounded 10.dp, bg = if(isNext) BrandGold@20% else Transparent) {
        Text(time, 14sp, Bold/Medium, #333333, padding = h10/v4)
    }
}
```

### Row Highlight
- Next prayer row: background `#EFE6D5`, time pill `BrandGold@20%`
- Other rows: transparent background, no time pill bg

### Icon Colors in Prayer List
| Prayer | Icon Tint |
|--------|----------|
| Fajr | `#2B4234` (BrandGreen — dark, night prayer) |
| Sunrise | `#CDAD70` (BrandGold — daytime) |
| Dhuhr | `#CDAD70` |
| Asr | `#CDAD70` |
| Maghrib | `#CDAD70` |
| Isha | `#2B4234` (dark, night prayer) |

---

## Typography

| Element | Size | Weight | Font | Color |
|---------|------|--------|------|-------|
| Top bar title | System | Bold | Scheherazade | `#CDAD70` |
| Hijri date | 15sp | SemiBold | Scheherazade | `#333333` |
| Location text | 13sp | Normal | Scheherazade/System | `#333333` |
| "الصلاة القادمة" | 11sp | Normal | System | `#CDAD70` |
| Next prayer name | 28sp | Bold | Scheherazade | White |
| Next prayer time | 12sp | Normal | System | White@70% |
| Countdown label | 10sp | Normal | System | `#CDAD70`@80% |
| Countdown value | 16sp | Bold | System | White |
| Qibla text | 16sp | Bold | Scheherazade | `#CDAD70` |
| Section header | 16sp | Bold | Scheherazade | `#333333` |
| Prayer name | 16sp | Bold | Scheherazade | `#333333` |
| Prayer time | 14sp | Bold/Medium | System | `#333333` |

---

## RTL/LTR Behavior

- Whole screen wrapped in `CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection())`
- Prayer rows: name+icon on **start** (right in AR), time on **end** (left in AR)
- Next prayer card: countdown on **start**, prayer info on **end**
- Qibla bar: arrow on **start**, text+compass on **end**
- All `Arrangement.SpaceBetween` — RTL naturally handled by Compose

---

## Removed from Old Design

- Ramadan accent colors (RamadanNight, RamadanPurple) on cards — kept as constants but unused
- Bottom decorative "☪ ۩ ☪" element
- Old gradient decorative gold line
- Old NightsStay icon in section header
- Old green-tinted card backgrounds from AppTheme
- isRamadanPrayer styling on individual rows
