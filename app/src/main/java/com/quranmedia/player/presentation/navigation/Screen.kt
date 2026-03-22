package com.quranmedia.player.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Onboarding : Screen("onboarding")
    object Reciters : Screen("reciters")
    object Surahs : Screen("surahs")
    object Player : Screen("player/{reciterId}/{surahNumber}?resume={resume}&startAyah={startAyah}") {
        fun createRoute(reciterId: String, surahNumber: Int, resume: Boolean = false, startAyah: Int? = null) =
            "player/$reciterId/$surahNumber?resume=$resume${startAyah?.let { "&startAyah=$it" } ?: ""}"
    }
    object Bookmarks : Screen("bookmarks")
    object Downloads : Screen("downloads")
    object Search : Screen("search")
    object Settings : Screen("settings?tab={tab}") {
        fun createRoute(tab: String = "reading") = "settings?tab=$tab"
    }
    object About : Screen("about")

    // Quran Reader screens
    object QuranReader : Screen("quranReader?page={page}&surah={surah}&reciter={reciter}&highlightSurah={highlightSurah}&highlightAyah={highlightAyah}") {
        fun createRoute(page: Int = 1, surahNumber: Int? = null, reciterId: String? = null, highlightSurah: Int? = null, highlightAyah: Int? = null): String {
            var route = "quranReader?page=$page"
            if (surahNumber != null) route += "&surah=$surahNumber"
            if (reciterId != null) route += "&reciter=$reciterId"
            if (highlightSurah != null) route += "&highlightSurah=$highlightSurah"
            if (highlightAyah != null) route += "&highlightAyah=$highlightAyah"
            return route
        }
    }
    object QuranIndex : Screen("quranIndex")

    // Athkar screens
    object AthkarCategories : Screen("athkarCategories")
    object AthkarList : Screen("athkarList/{categoryId}") {
        fun createRoute(categoryId: String) = "athkarList/$categoryId"
    }

    // Prayer Times screen
    object PrayerTimes : Screen("prayerTimes")

    // Qibla Direction screen
    object Qibla : Screen("qibla")

    // Athan Settings screen
    object AthanSettings : Screen("athanSettings")

    // Daily Tracker screen
    object Tracker : Screen("tracker")

    // Ramadan Imsakiya screen (TODO: Remove after Ramadan)
    object Imsakiya : Screen("imsakiya")

    // Hadith Library screens
    object HadithLibrary : Screen("hadithLibrary")
    object HadithBook : Screen("hadithBook/{bookId}") {
        fun createRoute(bookId: String) = "hadithBook/$bookId"
    }
    object HadithReader : Screen("hadithReader/{bookId}/{chapterId}/{hadithIndex}") {
        fun createRoute(bookId: String, chapterId: Int, hadithIndex: Int = 0) =
            "hadithReader/$bookId/$chapterId/$hadithIndex"
    }
    object HadithSearch : Screen("hadithSearch?bookId={bookId}") {
        fun createRoute(bookId: String? = null) =
            if (bookId != null) "hadithSearch?bookId=$bookId" else "hadithSearch"
    }

    // Recite (تسميع) screens - Full Mushaf experience for memorization review
    object Recite : Screen("recite")  // Legacy - redirects to ReciteIndex
    object ReciteIndex : Screen("reciteIndex")  // Same as QuranIndex but for recite flow
    object ReciteReader : Screen("reciteReader?page={page}") {
        fun createRoute(page: Int = 1) = "reciteReader?page=$page"
    }
}
