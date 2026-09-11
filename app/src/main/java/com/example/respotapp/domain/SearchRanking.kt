package com.example.respotapp.domain

object SearchRanking {

    fun score(query: String, title: String, subtitle: String, type: SearchResultType): Int {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return 0

        val t = title.lowercase()
        val s = subtitle.lowercase()
        var score = 0

        when {
            t == q -> score += 1000
            t.startsWith(q) -> score += 600
            t.contains(q) -> score += 250
        }

        when {
            s == q -> score += 400
            s.startsWith(q) -> score += 200
            s.contains(q) -> score += 80
        }

        // Prefer tighter title matches over type ordering
        val words = q.split(" ").filter { it.isNotBlank() }
        if (words.isNotEmpty()) {
            val matchedWords = words.count { word -> t.contains(word) || s.contains(word) }
            score += matchedWords * 50
        }

        return score
    }

    fun sortResults(query: String, results: List<SearchResultItem>): List<SearchResultItem> {
        return results
            .map { item ->
                item.copy(
                    relevanceScore = score(query, item.title, item.subtitle, item.type)
                )
            }
            .filter { it.relevanceScore > 0 }
            .sortedWith(
                compareByDescending<SearchResultItem> { it.relevanceScore }
                    .thenBy { it.title.lowercase() }
            )
    }
}
