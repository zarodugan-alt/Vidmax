package com.comet.engine

/**
 * Failure taxonomy (Part 7.5): maps raw yt-dlp stderr to honest user copy + an action.
 */
enum class FailureKind {
    AGE_GATE,
    RATE_LIMITED,
    UNSUPPORTED,
    EXTRACTOR_BROKEN,
    GEO_BLOCKED,
    NETWORK,
    NO_SPACE,
    UNKNOWN,
}

enum class FailureAction {
    NONE,
    IMPORT_COOKIES,
    RETRY,
    COPY_ERROR,
    UPDATE_ENGINE,
    OPEN_STORAGE_SETTINGS,
}

data class EngineFailure(
    val kind: FailureKind,
    val userMessage: String,
    val action: FailureAction,
    val rawMessage: String,
)

object FailureMapper {

    fun map(rawMessage: String?): EngineFailure {
        val raw = rawMessage.orEmpty().trim()
        val lower = raw.lowercase()

        return when {
            containsAny(
                lower,
                "sign in to confirm your age",
                "age-restricted",
                "age restricted",
                "confirm your age",
                "inappropriate for some users",
            ) -> EngineFailure(
                kind = FailureKind.AGE_GATE,
                userMessage = "This video needs sign-in",
                action = FailureAction.IMPORT_COOKIES,
                rawMessage = raw,
            )

            containsAny(lower, "http error 429", "429 too many requests", "too many requests") ->
                EngineFailure(
                    kind = FailureKind.RATE_LIMITED,
                    userMessage = "Site is rate-limiting — try again later",
                    action = FailureAction.RETRY,
                    rawMessage = raw,
                )

            containsAny(lower, "unsupported url") -> EngineFailure(
                kind = FailureKind.UNSUPPORTED,
                userMessage = "This site isn't supported",
                action = FailureAction.COPY_ERROR,
                rawMessage = raw,
            )

            containsAny(
                lower,
                "unable to extract",
                "is not a valid url",
                "no video formats found",
            ) -> EngineFailure(
                kind = FailureKind.EXTRACTOR_BROKEN,
                userMessage = "This site changed — update the engine",
                action = FailureAction.UPDATE_ENGINE,
                rawMessage = raw,
            )

            containsAny(
                lower,
                "not available in your country",
                "geo-restricted",
                "geo restricted",
                "geo not available",
                "geographically restricted",
            ) -> EngineFailure(
                kind = FailureKind.GEO_BLOCKED,
                userMessage = "Not available in your region",
                action = FailureAction.NONE,
                rawMessage = raw,
            )

            containsAny(
                lower,
                "no space left",
                "enospc",
                "errno 28",
            ) -> EngineFailure(
                kind = FailureKind.NO_SPACE,
                userMessage = "Storage full",
                action = FailureAction.OPEN_STORAGE_SETTINGS,
                rawMessage = raw,
            )

            containsAny(
                lower,
                "timed out",
                "timeout",
                "connection reset",
                "connection refused",
                "network unreachable",
                "unable to download webpage",
                "unable to download api data",
                "temporary failure in name resolution",
                "getaddrinfo failed",
            ) -> EngineFailure(
                kind = FailureKind.NETWORK,
                userMessage = "Connection failed",
                action = FailureAction.RETRY,
                rawMessage = raw,
            )

            else -> EngineFailure(
                kind = FailureKind.UNKNOWN,
                userMessage = raw.lineSequence().firstOrNull { it.isNotBlank() }
                    ?.take(200)
                    ?: "Something went wrong",
                action = FailureAction.COPY_ERROR,
                rawMessage = raw,
            )
        }
    }

    private fun containsAny(haystack: String, vararg needles: String): Boolean =
        needles.any { haystack.contains(it) }
}
