package com.example.model

data class Lecture(
    val id: Int,
    val title: String,
    val link: String = "",
    val sheetLink: String = "",
    val localVideoPath: String = "",
    val completed: Boolean = false,
    val isFavorite: Boolean = false,
    val lectureTag: String = ""
)

data class Chapter(
    val name: String,
    val lectures: List<Lecture> = emptyList()
)

data class Paper(
    val paperName: String,
    val chapters: List<Chapter> = emptyList()
)

data class Subject(
    val id: String,
    val name: String,
    val icon: String,
    val isTextIcon: Boolean = false,
    val colorStart: Long = 0xFF059669,
    val colorEnd: Long = 0xFF14B8A6,
    val papers: List<Paper> = emptyList(),
    val activePaperIndex: Int = 0,
    val activeChapterIndex: Int = 0,
    val notes: String = ""
)

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class ParsedChapterName(
    val cycle: String = "",
    val title: String = ""
)

object StudyUtils {
    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun parseChapterName(name: String): ParsedChapterName {
        val trimmed = name.trim()
        val cycleRegex = Regex("""^(CY:?\s*\d+|Cycle\s*\d+|সাইকেল\s*[\d০-৯]+)\s*[•:\-–]\s*(.+)$""", RegexOption.IGNORE_CASE)
        val match = cycleRegex.find(trimmed)
        return if (match != null) {
            ParsedChapterName(
                cycle = match.groupValues[1].trim(),
                title = match.groupValues[2].trim()
            )
        } else {
            ParsedChapterName(cycle = "", title = trimmed)
        }
    }

    fun toBengaliDigits(number: Int): String {
        return number.toString().map { char ->
            if (char in '0'..'9') bengaliDigits[char - '0'] else char
        }.joinToString("")
    }

    fun extractYouTubeVideoId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        if (trimmed.length == 11 && trimmed.matches(Regex("[a-zA-Z0-9_-]{11}"))) {
            return trimmed
        }
        // Match youtu.be/<id> with optional query params like ?si=... or ?
        val youtuBeRegex = Regex("""youtu\.be/([a-zA-Z0-9_-]{11})""", RegexOption.IGNORE_CASE)
        val youtuBeMatch = youtuBeRegex.find(trimmed)
        if (youtuBeMatch != null) {
            return youtuBeMatch.groupValues[1]
        }

        // Match v=<id>, /v/<id>, /embed/<id>, /shorts/<id>, /live/<id>
        val standardRegex = Regex(
            """(?:youtube\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=|shorts/|live/))([a-zA-Z0-9_-]{11})""",
            RegexOption.IGNORE_CASE
        )
        val standardMatch = standardRegex.find(trimmed)
        if (standardMatch != null) {
            return standardMatch.groupValues[1]
        }

        // Fallback for any 11 char ID in URL containing youtube
        if (trimmed.contains("youtu", ignoreCase = true)) {
            val tokenRegex = Regex("""([a-zA-Z0-9_-]{11})""")
            for (match in tokenRegex.findAll(trimmed)) {
                val token = match.value
                if (!token.equals("youtube", ignoreCase = true) &&
                    !token.equals("channel", ignoreCase = true) &&
                    !token.equals("feature", ignoreCase = true)
                ) {
                    return token
                }
            }
        }
        return null
    }

    fun getYouTubeThumbnailUrl(videoId: String?): String? {
        return if (!videoId.isNullOrBlank()) {
            "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
        } else {
            null
        }
    }

    fun extractUrls(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val formatted = text.replace(Regex("(?<!^)(?<!\n)(https?://)"), "\n$1")
        val urlRegex = Regex("(https?://[^\\s]+)")
        return urlRegex.findAll(formatted).map { it.value.trim() }.toList()
    }

    fun fetchYouTubeTitleSync(urlOrVideoId: String): String? {
        val videoId = extractYouTubeVideoId(urlOrVideoId) ?: return null
        return try {
            val oEmbedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val conn = java.net.URL(oEmbedUrl).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(responseText)
                val title = json.optString("title", "").trim()
                if (title.isNotEmpty()) title else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun formatSheetMappingText(text: String): String {
        if (text.isBlank()) return ""
        // Separate concatenated https:// and N/A tokens onto newlines
        var s = text.replace(Regex("(?i)(?<!^)(?<!\n)(https?://)"), "\n$1")
        s = s.replace(Regex("(?i)(?<!^)(?<!\n)(N/A)"), "\n$1")
        s = s.replace(Regex("(?i)(N/A)(https?://)"), "$1\n$2")
        return s.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    fun parseSheetEntries(rawText: String): List<String> {
        val formatted = formatSheetMappingText(rawText)
        val entries = mutableListOf<String>()
        for (line in formatted.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.equals("N/A", ignoreCase = true)) {
                entries.add("N/A")
            } else {
                val urls = extractUrls(trimmed)
                if (urls.isNotEmpty()) {
                    entries.add(urls.first())
                } else if (trimmed.contains("N/A", ignoreCase = true)) {
                    entries.add("N/A")
                } else {
                    entries.add(trimmed)
                }
            }
        }
        return entries
    }

    fun formatDrivePreviewUrl(url: String): String {
        if (url.isBlank()) return url
        val trimmed = url.trim()
        if (trimmed.contains("drive.google.com", ignoreCase = true)) {
            val fileIdRegex = Regex("""/file/d/([a-zA-Z0-9_-]+)""")
            val match = fileIdRegex.find(trimmed)
            if (match != null) {
                val fileId = match.groupValues[1]
                return "https://drive.google.com/file/d/$fileId/preview"
            }
            val idParamRegex = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")
            val idMatch = idParamRegex.find(trimmed)
            if (idMatch != null) {
                val fileId = idMatch.groupValues[1]
                return "https://drive.google.com/file/d/$fileId/preview"
            }
        }
        if (trimmed.endsWith(".pdf", ignoreCase = true) && !trimmed.contains("docs.google.com/gview", ignoreCase = true)) {
            return "https://docs.google.com/gview?embedded=true&url=$trimmed"
        }
        return trimmed
    }

    fun getDefaultSubjects(): List<Subject> {
        return listOf(
            Subject(
                id = "sub_1",
                name = "বাংলা",
                icon = "অ",
                isTextIcon = true,
                colorStart = 0xFFE11D48,
                colorEnd = 0xFFBE123C,
                papers = listOf(
                    Paper(
                        paperName = "প্রথম পত্র",
                        chapters = listOf(
                            "১. অপরিচিতা", "২. বিলাসী", "৩. আমার পথ", "৪. মানব কল্যাণ",
                            "৫. মাসি-পিসি", "৬. বায়ান্নর দিনগুলো", "৭. রেইনকোট", "৮. সোনার তরী",
                            "৯. বিদ্রোহী", "১০. প্রতিদান", "১১. তাহারেই পড়ে মনে", "১২. আঠারো বছর বয়স"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    ),
                    Paper(
                        paperName = "দ্বিতীয় পত্র",
                        chapters = listOf(
                            "১. বাংলা উচ্চারণের নিয়ম", "২. বাংলা বানানের নিয়ম", "৩. বাংলা ভাষার ব্যাকরণিক শব্দশ্রেণি",
                            "৪. বাংলা শব্দ গঠন (উপসর্গ ও সমাস)", "৫. বাক্যতত্ত্ব", "৬. অপপ্রয়োগ ও শুদ্ধ প্রয়োগ",
                            "৭. পারিভাষিক শব্দ ও অনুবাদ", "৮. দিনলিপি ও প্রতিবেদন", "৯. আবেদনপত্র", "১০. ভাবসম্প্রসারণ"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    )
                )
            ),
            Subject(
                id = "sub_2",
                name = "ইংরেজি",
                icon = "En",
                isTextIcon = true,
                colorStart = 0xFF38BDF8,
                colorEnd = 0xFF3B82F6,
                papers = listOf(
                    Paper(
                        paperName = "প্রথম পত্র",
                        chapters = listOf(
                            "Unit 1: People or Institutions Making History",
                            "Unit 2: Dreams",
                            "Unit 3: Lifestyle",
                            "Unit 4: Youthful Achievers",
                            "Unit 5: Adolescence",
                            "Unit 6: Relationships",
                            "Unit 7: Human Rights",
                            "Unit 8: Peace and Conflict",
                            "Unit 9: Tours and Travels"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    ),
                    Paper(
                        paperName = "দ্বিতীয় পত্র",
                        chapters = listOf(
                            "1. Gap filling without clues (Prepositions & Articles)",
                            "2. Words and Phrases",
                            "3. Completing Sentences",
                            "4. Right form of verbs",
                            "5. Narrative Style (Direct & Indirect Speech)",
                            "6. Modifiers",
                            "7. Sentence Connectors",
                            "8. Synonym and Antonym",
                            "9. Punctuation",
                            "10. Formal Letter & Report Writing"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    )
                )
            ),
            Subject(
                id = "sub_3",
                name = "আইসিটি",
                icon = "brain",
                isTextIcon = false,
                colorStart = 0xFFD97706,
                colorEnd = 0xFFB45309,
                papers = listOf(
                    Paper(
                        paperName = "মূল বই",
                        chapters = listOf(
                            Chapter(
                                name = "অধ্যায় ১: বিশ্ব ও বাংলাদেশ প্রেক্ষিত",
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            ),
                            Chapter(
                                name = "অধ্যায় ২: কমিউনিকেশন সিস্টেমস ও নেটওয়ার্কিং",
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            ),
                            Chapter(
                                name = "অধ্যায় ৩: সংখ্যা পদ্ধতি ও ডিজিটাল ডিভাইস",
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            ),
                            Chapter(
                                name = "অধ্যায় ৪: ওয়েব ডিজাইন পরিচিতি এবং HTML",
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            ),
                            Chapter(
                                name = "অধ্যায় ৫: প্রোগ্রামিং ভাষা",
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            ),
                            Chapter(
                                name = "অধ্যায় ৬: ডেটাবেজ ম্যানেজমেন্ট সিস্টেম",
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        )
                    )
                )
            ),
            Subject(
                id = "sub_4",
                name = "পদার্থবিজ্ঞান",
                icon = "atom",
                isTextIcon = false,
                colorStart = 0xFF7C3AED,
                colorEnd = 0xFF6D28D9,
                papers = listOf(
                    Paper(
                        paperName = "প্রথম পত্র",
                        chapters = listOf(
                            "১. ভৌত জগৎ ও পরিমাপ", "২. ভেক্টর", "৩. গতিবিদ্যা", "৪. নিউটনীয় বলবিদ্যা",
                            "৫. কাজ, শক্তি ও ক্ষমতা", "৬. মহাকর্ষ ও অভিকর্ষ", "৭. পদার্থের গাঠনিক ধর্ম",
                            "৮. পর্যায়বৃত্ত গতি", "৯. তরঙ্গ", "১০. আদর্শ গ্যাস ও গ্যাসের গতি তত্ত্ব"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    ),
                    Paper(
                        paperName = "দ্বিতীয় পত্র",
                        chapters = listOf(
                            "১. তাপগতিবিদ্যা", "২. স্থির তড়িৎ", "৩. চল তড়িৎ", "৪. তড়িৎ প্রবাহের চৌম্বক ক্রিয়া ও চৌম্বকত্ব",
                            "৫. তাড়িতচৌম্বক আবেশ ও পরিবর্তী প্রবাহ", "৬. জ্যামিতিক আলোকবিজ্ঞান", "৭. ভৌত আলোকবিজ্ঞান",
                            "৮. আধুনিক পদার্থবিজ্ঞানের সূচনা", "৯. পরমাণুর মডেল এবং নিউক্লীয় পদার্থবিজ্ঞান",
                            "১০. সেমিকন্ডাক্টর ও ইলেকট্রনিক্স", "১১. জ্যোতির্বিজ্ঞান"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    )
                )
            ),
            Subject(
                id = "sub_5",
                name = "রসায়ন",
                icon = "flask",
                isTextIcon = false,
                colorStart = 0xFF059669,
                colorEnd = 0xFF047857,
                papers = listOf(
                    Paper(
                        paperName = "প্রথম পত্র",
                        chapters = listOf(
                            "অধ্যায় ১: পরিবেশ রসায়ন", "অধ্যায় ২: গুণগত রসায়ন", "অধ্যায় ৩: মৌলের পর্যায়বৃত্ত ধর্ম ও রাসায়নিক বন্ধন",
                            "অধ্যায় ৪: রাসায়নিক পরিবর্তন", "অধ্যায় ৫: কর্মমুখী রসায়ন"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    ),
                    Paper(
                        paperName = "দ্বিতীয় পত্র",
                        chapters = listOf(
                            "অধ্যায় ১: পরিবেশ রসায়ন", "অধ্যায় ২: জৈব রসায়ন", "অধ্যায় ৩: পরিমাণগত রসায়ন",
                            "অধ্যায় ৪: তড়িৎ রসায়ন", "অধ্যায় ৫: অর্থনৈতিক রসায়ন"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    )
                )
            ),
            Subject(
                id = "sub_6",
                name = "জীববিজ্ঞান",
                icon = "dna",
                isTextIcon = false,
                colorStart = 0xFF16A34A,
                colorEnd = 0xFF15803D,
                papers = listOf(
                    Paper(
                        paperName = "প্রথম পত্র",
                        chapters = listOf(
                            "১. কোষ ও এর গঠন", "২. কোষ বিভাজন", "৩. কোষ রসায়ন", "৪. অণুজীব",
                            "৫. শৈবাল ও ছত্রাক", "৬. ব্রায়োফাইটা ও টেরিডোফাইটা", "৭. নগ্নবীজী ও আবৃতবীজী উদ্ভিদ",
                            "৮. টিস্যু ও টিস্যুতন্ত্র", "৯. উদ্ভিদ শারীরতত্ত্ব", "১০. উদ্ভিদ প্রজনন",
                            "১১. জীবপ্রযুক্তি", "১২. পরিবেশ ও বাস্তুতন্ত্র"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    ),
                    Paper(
                        paperName = "দ্বিতীয় পত্র",
                        chapters = listOf(
                            "প্রথম অধ্যায়: প্রাণীর বিভিন্নতা ও শ্রেণিবিন্যাস", "দ্বিতীয় অধ্যায়: প্রাণীর পরিচিতি",
                            "তৃতীয় অধ্যায়: পরিপাক ও শোষণ", "চতুর্থ অধ্যায়: রক্ত ও সঞ্চালন",
                            "পঞ্চম অধ্যায়: শ্বাসক্রিয়া ও শ্বসন", "ষষ্ঠ অধ্যায়: বর্জ্য ও নিষ্কাশন",
                            "সপ্তম অধ্যায়: চলন ও অঙ্গচালনা", "অষ্টম অধ্যায়: সমন্বয় ও নিয়ন্ত্রণ",
                            "নবম অধ্যায়: মানব জীবনের ধারাবাহিকতা", "দশম অধ্যায়: প্রাণীবিজ্ঞান, জিনতত্ত্ব ও বিবর্তন",
                            "একাদশ অধ্যায়: প্রাণী প্রজনন ও জৈবপ্রযুক্তি", "দ্বাদশ অধ্যায়: প্রাণীর আচরণ ও পরিবেশ"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    )
                )
            ),
            Subject(
                id = "sub_7",
                name = "উচ্চতর গণিত",
                icon = "calculator",
                isTextIcon = false,
                colorStart = 0xFF2563EB,
                colorEnd = 0xFF1D4ED8,
                papers = listOf(
                    Paper(
                        paperName = "প্রথম পত্র",
                        chapters = listOf(
                            "১. ম্যাট্রিক্স ও নির্ণায়ক", "২. ভেক্টর", "৩. সরল রেখা", "৪. বৃত্ত",
                            "৫. বিন্যাস ও সমাবেশ", "৬. ত্রিকোণমিতিক অনুপাত", "৭. সংযুক্ত কোণের ত্রিকোণমিতিক অনুপাত",
                            "৮. ফাংশন ও ফাংশনের লেখচিত্র", "৯. অন্তরীকরণ", "১০. সমাকলন"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    ),
                    Paper(
                        paperName = "দ্বিতীয় পত্র",
                        chapters = listOf(
                            "অধ্যায় ১: বাস্তব সংখ্যা ও অসমতা",
                            "অধ্যায় ২: যোগাশ্রয়ী প্রোগ্রাম",
                            "অধ্যায় ৩: জটিল সংখ্যা",
                            "অধ্যায় ৪: বহুপদী ও বহুপদী সমীকরণ",
                            "অধ্যায় ৫: দ্বিপদী বিস্তৃতি",
                            "অধ্যায় ৬: কনিক",
                            "অধ্যায় ৭: বিপরীত ত্রিকোণমিতিক ফাংশন ও ত্রিকোণমিতিক সমীকরণ",
                            "অধ্যায় ৮: স্থিতিবিদ্যা",
                            "অধ্যায় ৯: সমতলে বস্তুকনার গতি",
                            "অধ্যায় ১০: সম্ভাবনা"
                        ).map { chName ->
                            Chapter(
                                name = chName,
                                lectures = (1..2).map { Lecture(id = it, title = "লেকচার ${StudyUtils.toBengaliDigits(it)}") }
                            )
                        }
                    )
                )
            )
        )
    }
}
