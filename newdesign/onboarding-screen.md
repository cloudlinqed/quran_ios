# Onboarding Screen — New Design Specification

## Overview
First-run setup screen. Collects permissions (location, notifications, battery) and prayer calculation method. No bottom navigation bar.

---

## Theme Colors

| Element | Hex | Usage |
|---------|-----|-------|
| Screen background | `#F9F6EE` | Warm cream (ScreenBg) |
| Welcome title | `#275239` | BrandGreen |
| Buttons | `#275239` | BrandGreen container, white text |
| Permission granted icons | `#275239` | BrandGreen checkmarks |
| Permission pending icons | `LightGray` | Unchecked radio |
| Info card bg | `#E8F5E9` | LightGreen |
| Warning card bg | `#FFF3E0` | LightOrange |
| Card surfaces | `#FFFFFF` | White |
| Dropdown border focused | `#275239` | BrandGreen |
| Subtitle text | AppTheme.colors.textSecondary | Muted |

---

## Layout

```
┌────────────────────────────────────┐
│  Screen bg: #F9F6EE                │
│                                    │
│                    [English/العربية]│  ← language toggle
│                                    │
│     مرحباً بك في الفرقان            │  ← 22sp bold, #275239
│          إعداد التطبيق              │  ← 16sp, muted
│   اسمح بالصلاحيات واختر إعداداتك    │  ← 12sp, muted
│                                    │
│  ┌── Permissions Card ──────────┐  │  ← white / lightGreen when all granted
│  │  📍 الموقع          ✓/○      │  │
│  │  🔔 الإشعارات       ✓/○      │  │
│  │  🔋 البطارية        ✓/○      │  │
│  │  [السماح بالصلاحيات]          │  │  ← #275239 button
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Warning Card ──────────────┐  │  ← #FFF3E0 orange bg
│  │  ⚠️ Battery/notification     │  │
│  │     warning text              │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Prayer Method Selector ────┐  │  ← white card
│  │  طريقة حساب مواقيت الصلاة    │  │
│  │  [أم القرى - مكة        ▼]   │  │  ← dropdown
│  └──────────────────────────────┘  │
│                                    │
│  ┌── Info Card ─────────────────┐  │  ← #E8F5E9 green bg
│  │  ℹ️ Auto-enabled features    │  │
│  └──────────────────────────────┘  │
│                                    │
│  [ابدأ]                            │  ← #275239 full-width button
│  يمكنك تغيير الإعدادات لاحقاً       │  ← 11sp hint
│                                    │
│  No bottom navigation bar          │
└────────────────────────────────────┘
```

---

## Components

### Permission Status Row
```kotlin
Row {
    Icon(permIcon, tint = if(granted) BrandGreen else iconDefault)
    Text(label, 14sp)
    if (extraInfo) Text(extraInfo, 12sp, BrandGreen)
    Icon(if(granted) CheckCircle else RadioButtonUnchecked,
         tint = if(granted) BrandGreen else LightGray)
}
```

### Allow Permissions Button
- Full width, `#275239` background, white text
- Rounded 8dp
- Triggers location + notification permissions + battery exemption

### Prayer Method Dropdown
- `ExposedDropdownMenuBox` with `OutlinedTextField`
- Focused border: `#275239`
- 14 calculation methods supported
- Default: Umm Al-Qura (Makkah)

### Start Button
- Full width, `#275239` background
- Shows `CircularProgressIndicator` while completing
- Triggers `viewModel.completeOnboarding()`

---

## No Bottom Navigation
Onboarding is a one-time setup flow — no bottom bar.

---

## iOS Notes
- Use `NavigationStack` or modal presentation
- Permissions: `CLLocationManager.requestWhenInUseAuthorization()`, `UNUserNotificationCenter.requestAuthorization()`
- Battery optimization not applicable on iOS
- Prayer method: `Picker` with `.menu` style
- SF Symbols: `location.fill`, `bell.fill`, `battery.100`, `checkmark.circle.fill`, `circle`
