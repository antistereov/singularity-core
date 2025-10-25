package io.stereov.singularity.auth.oauth2.util

fun getWellKnownProvider(providerName: String): String = when(providerName.lowercase()) {
    "google" -> "Google"
    "github" -> "GitHub"
    "gitlab" -> "GitLab"
    "microsoft" -> "Microsoft"
    "facebook" -> "Facebook"
    "apple" -> "Apple"
    "linkedin" -> "LinkedIn"
    "slack" -> "Slack"
    "twitter" -> "Twitter"
    "amazon" -> "Amazon"
    "salesforce" -> "Salesforce"
    "atlassian" -> "Atlassian"
    "discord" -> "Discord"
    "zoom" -> "Zoom"
    "okta" -> "Okta"
    "azure" -> "Azure"
    "autodesk" -> "Autodesk"
    "spotify" -> "Spotify"

    else -> providerName
}
