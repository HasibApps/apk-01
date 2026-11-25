package com.example.model

/**
 * (subjectId + chapterName + lectureId) দিয়ে নির্দিষ্ট লেকচারের localVideoPath সেট করে।
 * লেকচার আইডি প্রতিটি অধ্যায়ে ১,২,৩… তাই শুধু আইডি যথেষ্ট নয় — অধ্যায়ের নামও মেলানো হয়।
 * ব্যবহারকারী অন্য অধ্যায়/পেজে চলে গেলেও ডাউনলোড সঠিক লেকচারেই সেভ হয়।
 * কিছু বদলালে নতুন লিস্ট, না বদলালে null রিটার্ন করে।
 */
fun List<Subject>.withLocalVideoPath(
    subjectId: String,
    chapterName: String,
    lectureId: Int,
    path: String
): List<Subject>? {
    var done = false
    val result = map { sub ->
        if (done || sub.id != subjectId) sub
        else sub.copy(
            papers = sub.papers.map { paper ->
                if (done) paper
                else paper.copy(
                    chapters = paper.chapters.map { ch ->
                        if (done || ch.name != chapterName) ch
                        else ch.copy(
                            lectures = ch.lectures.map { lec ->
                                if (!done && lec.id == lectureId) {
                                    done = true
                                    lec.copy(localVideoPath = path)
                                } else lec
                            }
                        )
                    }
                )
            }
        )
    }
    return if (done) result else null
}
