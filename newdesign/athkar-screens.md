# Athkar Screens — New Design Specification

## Covers: AthkarCategoriesScreen + AthkarListScreen

Source: `stitch_export/Athkar/` (screen.png + code.html)

---

## Design Colors (from Stitch)

| Token | Hex | Usage |
|-------|-----|-------|
| `AthkarDarkGreen` | `#1F3E33` | Icon tint, category text, bottom nav inactive |
| `AthkarGold` | `#D6C291` | Card border, active tab indicator |
| `AthkarIconBgBlue` | `#C6DFD2` | Icon square bg (alternating) |
| `AthkarIconBgYellow` | `#F1E3B8` | Icon square bg (alternating) |
| Screen bg | `#FCFAF3` / `#F9F6EE` | Warm cream background |
| Card bg | `#FFFFFF` | White card background |
| TopAppBar bg | `#275239` | Dark green header |
| TopAppBar text | `#CDAD70` | Gold title/icons |

---

## AthkarCategoriesScreen

### Layout (from Stitch screenshot)
```
┌────────────────────────────────────┐
│  [≡]      الأذكار          [→]    │  ← #275239 bg, rounded bottom 20dp
├────────────────────────────────────┤
│  Screen bg: #FCFAF3                │
│                                    │
│  ┌─────────────┐ ┌─────────────┐  │  ← 2-column grid
│  │   [☀️]      │ │   [🌙]      │  │
│  │ أذكار الصباح│ │ أذكار المساء │  │  ← gold border 1.5dp
│  └─────────────┘ └─────────────┘  │
│  ┌─────────────┐ ┌─────────────┐  │
│  │   [🕌]      │ │   [🌙]      │  │
│  │أذكار بعد الصلاة│ │أذكار قبل النوم│  │
│  └─────────────┘ └─────────────┘  │
│  ┌─────────────┐ ┌─────────────┐  │
│  │   [⏰]      │ │   [🏠]      │  │
│  │أذكار الاستيقاظ│ │دخول المنزل  │  │
│  └─────────────┘ └─────────────┘  │
│  ┌─────────────┐ ┌─────────────┐  │
│  │   [🚪]      │ │   [🍴]      │  │
│  │الخروج من المنزل│ │أذكار الطعام │  │
│  └─────────────┘ └─────────────┘  │
│                                    │
├──────────────────────────────────────┤
│ 🏠الرئيسية 📖القرآن 🕌الصلاة [الأذكار] ⋯المزيد│
└──────────────────────────────────────┘
```

### Category Card
```kotlin
Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = 1.dp,
    border = BorderStroke(1.5.dp, Color(0xFFD6C291))  // gold border
) {
    Column(center, padding = v20/h12) {
        // Icon in colored rounded square
        Box(64.dp, RoundedCornerShape(16.dp), bg = alternating blue/yellow) {
            Icon(28.dp, tint = #1F3E33)
        }
        Spacer(14.dp)
        Text(categoryName, 13sp, Bold, #1F3E33, center)
    }
}
```

### Icon Background Alternation
Cards alternate between two icon background colors:
- **Blue-green**: `#C6DFD2` — morning, after prayer, waking up, food
- **Yellow-cream**: `#F1E3B8` — evening, before sleep, entering home, leaving home

Logic: `if (category.id.hashCode() % 2 == 0) blue else yellow`

### Category Icons
| Category | Icon | Android | SF Symbol |
|----------|------|---------|-----------|
| أذكار الصباح | Sun | `WbSunny` | `sun.max.fill` |
| أذكار المساء | Moon | `NightsStay` | `moon.fill` |
| أذكار بعد الصلاة | Mosque | `AccountBalance` | `building.columns.fill` |
| أذكار قبل النوم | Moon | `Bedtime` | `moon.zzz.fill` |
| أذكار الاستيقاظ | Alarm | `Alarm` | `alarm.fill` |
| دخول المنزل | House | `Home` | `house.fill` |
| الخروج من المنزل | Door | `ExitToApp` | `door.left.hand.open` |
| أذكار الطعام | Utensils | `Restaurant` | `fork.knife` |

---

## Bottom Navigation (updated)

### Colors (from Stitch code.html)
- **Inactive tabs**: `#1F3E33` (dark green) — NOT gray
- **Active tab**: `#D6C291` (gold) with gold top indicator bar (3dp)
- **Background**: `#FFFFFF` white
- **Font weight**: Medium for inactive, Bold for active
- **Rounded top**: 24dp

### Dynamic behavior
- Current screen's tab is hidden from the bar
- On Athkar Categories: الأذكار tab hidden, 4 tabs visible

---

## AthkarListScreen (thikr reader)

Theme updated to match new system:
- TopAppBar: `#275239` bg, `#CDAD70` gold text
- Screen bg: `#F9F6EE`
- ThikrCard: white bg, rounded 24dp
- Tap-to-count with haptic
- Index badge: islamicGreen circle
- Count badge: islamicGreen pill
- Completed: lightGreen tint
- Bottom nav with athkarList route hidden

---

## iOS Notes
- Categories: `LazyVGrid` with 2 columns, `GridItem(.flexible())`
- Card: white bg + gold border via `.overlay(RoundedRectangle().stroke(goldColor, lineWidth: 1.5))`
- Icon squares: `.frame(64).background(alternatingColor).cornerRadius(16)`
- SF Symbols for all icons
- Bottom tab bar: `TabView` with dark green inactive, gold active
