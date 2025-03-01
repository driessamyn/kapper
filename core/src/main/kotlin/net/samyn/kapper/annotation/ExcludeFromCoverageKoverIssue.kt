package net.samyn.kapper.annotation

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
)
annotation class ExcludeFromCoverageKoverIssue(val reason: String = "", val issueLink: String = "")
