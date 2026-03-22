```markdown
# Design System Document: High-End Editorial Islamic Experience

## 1. Overview & Creative North Star
The creative North Star for this design system is **"The Digital Sanctuary."** 

Unlike generic utility apps, this system prioritizes serenity, spiritual focus, and an editorial rhythm. We move beyond the "grid of boxes" by embracing a sophisticated interplay of tonal depth and premium typography. The goal is to create an interface that feels like a curated, high-end publication—intentional, spacious, and calm. We break the template look by using generous white space (defined in our Spacing Scale), subtle overlapping surfaces, and a deliberate absence of harsh structural lines.

## 2. Colors
Our palette is rooted in a deep, scholarly green and an illuminated gold, set against a warm, paper-like foundation.

*   **Primary Palette:** `primary` (#0D631B) and `primary_container` (#2E7D32). These represent growth and life. Use gradients transitioning between these two for hero elements to add visual "soul."
*   **Secondary Palette:** `secondary` (#735C00) and `secondary_container` (#FED65B). This is our "Soft Gold." Use this sparingly for moments of enlightenment, such as prayer time indicators or active navigation states.
*   **The Foundation:** `background` (#FAFAF3) is a warm off-white that reduces eye strain and feels more premium than pure white (#FFFFFF).

### The "No-Line" Rule
**Explicit Instruction:** Do not use 1px solid borders to section off content. Boundaries must be defined through background color shifts. For example, a `surface_container_low` card sitting on a `surface` background provides all the separation a user needs without the visual noise of a stroke.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers, like stacked sheets of fine vellum.
*   **Level 0:** `surface` (The Base).
*   **Level 1:** `surface_container_low` (Large background sections).
*   **Level 2:** `surface_container` (Primary cards and interactive modules).
*   **Level 3:** `surface_container_highest` (Floating elements or active states).

### The "Glass & Gradient" Rule
For floating elements (like the media player or navigation bars), use **Glassmorphism**. Apply a semi-transparent `surface` color with a `backdrop-blur` effect. This integrates the component into the background, creating a cohesive, high-end feel.

## 3. Typography
We utilize a high-contrast scale to create an editorial feel.
*   **Serenity in Script:** `notoSerif` is used for all `display` and `headline` levels. This provides an elegant, authoritative weight to titles and Quranic verses.
*   **Modern Utility:** `manrope` is used for `title`, `body`, and `label` levels. Its clean, geometric nature ensures high legibility for navigation and long-form reading.

**Hierarchy Strategy:** 
Use `display-lg` (3.5rem) for significant spiritual headings to create an "asymmetric focal point." Pair it with `body-md` (0.875rem) in `on_surface_variant` (#40493D) for descriptions to create a clear, sophisticated hierarchy.

## 4. Elevation & Depth
In "The Digital Sanctuary," depth is a whisper, not a shout.

*   **The Layering Principle:** Avoid shadows where possible. Instead, "stack" surface tiers. A `surface_container_lowest` card placed on a `surface_container_low` background creates a natural, soft lift.
*   **Ambient Shadows:** When a floating effect is required (e.g., a "Play" button), use a shadow with a large blur (20px+) and low opacity (4%-8%). The shadow color must be a tinted version of `on_surface` (#1B1C18), never pure black.
*   **The "Ghost Border" Fallback:** If a border is required for accessibility, use `outline_variant` (#BFCABA) at **10% - 20% opacity**. Never use a 100% opaque border.

## 5. Components

### Media Player Interface
The media player should feel like a premium piece of hardware.
*   **Surface:** Use `surface_container_highest` or a Glassmorphism effect.
*   **Controls:** The play button should be a large circular `primary` element.
*   **Typography:** The Surah name should be `title-lg`, and the Reciter name in `label-md` with `secondary` (#735C00) coloring.
*   **Progress Bar:** Use a soft gradient from `secondary` to `secondary_fixed_dim`.

### Cards & Navigation
*   **Roundedness:** All cards must use `xl` (1.5rem) or `lg` (1rem) corner radii to maintain the "Soft Minimalism" aesthetic.
*   **No Dividers:** Forbid the use of divider lines in lists. Use `3.5` (1.2rem) spacing from the scale to separate list items.
*   **Cards:** Use `surface_container` for the card body. On hover or tap, shift to `surface_container_high`.

### Buttons
*   **Primary:** Solid `primary` with `on_primary` text. No border. `xl` roundedness.
*   **Secondary:** `surface_container_highest` background with `primary` text.
*   **Tertiary:** Ghost style; no background, `primary` text, underlined only on hover.

### Inputs & Fields
*   **Container:** Use `surface_container_low`.
*   **States:** On focus, do not use a heavy stroke. Use a "Ghost Border" of `primary` at 30% opacity and a subtle `surface_tint` glow.

## 6. Do's and Don'ts

### Do:
*   **Use Asymmetry:** Place titles slightly off-center or use varying card heights to create an organic, non-templated rhythm.
*   **Embrace "Breathing Room":** Use the higher ends of the spacing scale (`12` or `16`) between major sections to promote a sense of calm.
*   **Type Pairing:** Always pair a serif `notoSerif` headline with a sans-serif `manrope` body for that high-end editorial look.

### Don't:
*   **Don't use 1px solid borders:** This is the quickest way to make a design look "cheap" and "standard."
*   **Don't use pure black shadows:** Shadows should be light, airy, and tinted by the surface color.
*   **Don't crowd the UI:** If a screen feels full, increase the spacing rather than shrinking the components.
*   **Don't use high-contrast dividers:** If you must separate items, use a background color shift of one tier (e.g., from `surface` to `surface_container_lowest`).