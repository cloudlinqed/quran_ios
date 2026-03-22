# Bottom Navigation Bar — New Design Specification

## Overview

A shared bottom navigation bar replaces the per-screen top overflow menus. The bar appears on **all screens except**: QuranReader, QuranIndex, Onboarding, Recite/ReciteReader, Player, and Settings sub-screens.

The bar is **dynamic** — the current screen's tab is hidden from the bar, so the user only sees navigation options to OTHER screens.

---

## Color Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `NavBg` | `#FFFFFF` | Bar background |
| `NavGold` | `#D6C291` | Active tab icon/text + top indicator |
| `NavDarkGreen` | `#1F3E33` | Inactive tab icon/text — dark green, NOT gray |
| Font weight inactive | `Medium` | Not faded — crisp and readable |
| Font weight active | `Bold` | Emphasized |

---

## Layout

```
┌─────────────────────────────────────┐
│  Screen Content                      │
│                                      │
├──────────────────────────────────────┤  ← rounded top 24dp, white bg, shadow
│  🏠      📖      🕌      ☆     ⋯   │  ← 5 tabs (4 visible + More)
│ الرئيسية  القرآن   الصلاة  الأذكار المزيد │
└──────────────────────────────────────┘
```

### Dynamic Behavior
When on the Home screen, "الرئيسية" tab is hidden — only 4 tabs show:
```
│  📖      🕌      ☆     ⋯            │
│  القرآن   الصلاة  الأذكار  المزيد     │
```

When on Prayer Times, "الصلاة" is hidden:
```
│  🏠      📖      ☆     ⋯            │
│ الرئيسية  القرآن  الأذكار  المزيد     │
```

---

## Tab Definitions

### Primary Tabs (always in bar, hidden when active)

| Tab | Arabic | English | Icon (Material) | Icon (SF Symbols) | Route |
|-----|--------|---------|-----------------|-------------------|-------|
| Home | الرئيسية | Home | `Icons.Default.Home` | `house.fill` | `home` |
| Quran | القرآن | Quran | `Icons.Default.MenuBook` | `book.fill` | `quranIndex` |
| Prayer | الصلاة | Prayer | `Icons.Default.Schedule` | `clock.fill` | `prayerTimes` |
| Athkar | الأذكار | Athkar | `Icons.Default.StarOutline` | `star` | `athkarCategories` |
| More | المزيد | More | `Icons.Default.MoreHoriz` | `ellipsis` | (opens dropdown) |

### "More" Dropdown Items

| Item | Arabic | English | Icon | Route |
|------|--------|---------|------|-------|
| Hadith | الأحاديث | Hadith | `HistoryEdu` | `hadithLibrary` |
| Tracker | المتابعة اليومية | Daily Tracker | `CheckBox` | `tracker` |
| Bookmarks | المفضلة | Bookmarks | `FavoriteBorder` | `bookmarks` |
| Downloads | التنزيلات | Downloads | `Download` | `downloads` |
| Qibla | اتجاه القبلة | Qibla | `Explore` | `qibla` |
| Settings | الإعدادات | Settings | `Settings` | `settings` |
| About | حول التطبيق | About | `Info` | `about` |

---

## Route Matching Logic

A tab is considered "active" (and hidden) based on the current route:

```kotlin
fun isRouteActive(currentRoute: String, tabRoute: String): Boolean = when (tabRoute) {
    "home" -> currentRoute == "home"
    "quranIndex" -> currentRoute in listOf("quranIndex", "quranReader")
    "prayerTimes" -> currentRoute in listOf("prayerTimes", "athanSettings")
    "athkarCategories" -> currentRoute.startsWith("athkar")
    else -> false
}
```

---

## Styling

### Bar Container
```kotlin
Surface(
    modifier = Modifier.fillMaxWidth(),
    color = Color(0xFFFFFFFF),           // white
    shadowElevation = 8.dp,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
)
```

### Tab Item (inactive)
```kotlin
Column(center) {
    Icon(icon, tint = Color(0xFF1F3E33), size = 22.dp)  // dark green — NOT gray
    Text(label, fontSize = 10sp, fontWeight = Medium, color = Color(0xFF1F3E33))
}
```

### "More" Dropdown
- Background: `#F9F6EE` (warm cream — matches app theme, NOT default white)
- Standard `DropdownMenu` with `DropdownMenuItem` items
- Each with `leadingIcon` (18dp) and text (14sp)

### No Duplicate Overflow
- **All screens with bottom nav: NO overflow/3-dot menu in the TopAppBar**
- Navigation is exclusively via the bottom bar tabs and "More" dropdown
- **Only exception: QuranReaderScreen** — retains its custom overflow menu because it has no bottom nav bar (full-screen reading mode)

---

## Integration Pattern

### Android (Compose)

Each screen receives an `onNavigateByRoute: (String) -> Unit` callback:

```kotlin
// In screen composable
Scaffold(
    bottomBar = {
        BottomNavBar(
            currentRoute = "prayerTimes",  // current screen's route
            language = language,
            onNavigate = { route -> onNavigateByRoute(route) }
        )
    }
) { padding -> ... }
```

In the NavGraph, wire with a shared helper:

```kotlin
private fun navigateByRoute(navController: NavHostController, route: String) {
    val target = when (route) {
        "home" -> Screen.Home.route
        "quranIndex" -> Screen.QuranIndex.route
        "prayerTimes" -> Screen.PrayerTimes.route
        "athkarCategories" -> Screen.AthkarCategories.route
        "hadithLibrary" -> Screen.HadithLibrary.route
        "tracker" -> Screen.Tracker.route
        "bookmarks" -> Screen.Bookmarks.route
        "downloads" -> Screen.Downloads.route
        "qibla" -> Screen.Qibla.route
        "settings" -> Screen.Settings.route
        "about" -> Screen.About.route
        else -> route
    }
    navController.navigate(target) { launchSingleTop = true }
}
```

### iOS (SwiftUI)

```swift
TabView(selection: $selectedTab) {
    // Dynamic: only show tabs that aren't the current screen
    ForEach(tabs.filter { $0.route != currentRoute }) { tab in
        tab.view
            .tabItem {
                Image(systemName: tab.sfSymbol)
                Text(isArabic ? tab.labelArabic : tab.labelEnglish)
            }
            .tag(tab.route)
    }
}
```

---

## Screens WITH Bottom Bar

| Screen | currentRoute |
|--------|-------------|
| Home | `home` |
| Prayer Times | `prayerTimes` |
| Athkar Categories | `athkarCategories` |
| Athkar List | `athkarList/{id}` |
| Hadith Library | `hadithLibrary` |
| Hadith Book | `hadithBook/{id}` |
| Tracker | `tracker` |
| Bookmarks | `bookmarks` |
| Downloads | `downloads` |
| Qibla | `qibla` |
| Quran Index | `quranIndex` |
| About | `about` |

## Screens WITHOUT Bottom Bar

| Screen | Reason |
|--------|--------|
| Quran Reader | Full-screen immersive reading |
| Onboarding | First-run setup |
| Recite / ReciteReader | Full-screen recitation |
| Player | Full-screen audio player |
| Settings | Sub-screen / utility |
| Athan Settings | Sub-screen of Prayer |
| Hadith Reader | Full-screen reading |
| Hadith Search | Keyboard-focused |

---

## RTL Behavior

- The Row uses `Arrangement.SpaceAround` which auto-distributes in both LTR and RTL
- Tab labels use `scheherazadeFont` in Arabic mode
- "More" dropdown follows layout direction naturally

## Dark Mode Colors
| Element | Light | Dark |
|---------|-------|------|
| Nav bg | `#FFFFFF` | `#1E1E1E` |
| Tab icon/text | `#275239` (dark green) | `#CDAD70` (gold) |
| More menu bg | `#F9F6EE` (cream) | `#111111` (near-black) |

Note: Dark mode toggle is in screen **headers**, not in the bottom nav bar.

---

## TalkBack Accessibility

- **Nav bar container**: `semantics { contentDescription = "شريط التنقل" / "Navigation bar" }`
- **Each tab**: `semantics { contentDescription = tabLabel; role = Role.Tab }`
- **More dropdown items**: standard `DropdownMenuItem` with text — TalkBack reads the label automatically
- Tab items are `clickable` which makes them focusable by TalkBack
- Dynamic tab filtering doesn't confuse TalkBack — it only sees the visible tabs
