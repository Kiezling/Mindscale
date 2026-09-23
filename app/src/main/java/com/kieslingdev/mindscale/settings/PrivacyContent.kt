package com.kieslingdev.mindscale.settings

/**
 * Offline product and privacy information shown at the bottom of Settings.
 *
 * Keep this copy factual and aligned with the app's local-only implementation. It deliberately
 * contains no publisher address or hosted-policy URL: those are release gates, not values to
 * invent in the app.
 */
object PrivacyContent {
    const val HEADING = "Privacy and product information"
    const val LOCAL_STORAGE =
        "MindScale stores your records, Profile information, safety plan, and preferences on " +
            "this device. It has no accounts, analytics, or app network connection. Android " +
            "automatic backup is disabled."
    const val EXPORTS =
        "Exports are unencrypted files. You choose where to save them, and the document provider " +
            "you choose may sync them to a cloud service. Keep exported files private and delete " +
            "them when you no longer need them."
    const val EXTERNAL_ACTIONS =
        "Copy places the selected information on the system clipboard, where another app or " +
            "service may be able to access it. Share sends the selected information to the " +
            "receiving app you choose. Safety actions open your dialer, messaging app, or browser " +
            "with the selected number or link. MindScale cannot control what another app stores " +
            "or sends."
    const val ERASE =
        "Erasing data in MindScale removes it from this device. It does not remove exported files " +
            "or copies already received by another person or app."
    const val MEDICAL_DISCLAIMER =
        "MindScale is not a medical device. It does not diagnose, treat, cure, or prevent any " +
            "medical condition. It records what you choose to enter; it does not assess risk or " +
            "interpret your data. For medical questions, contact a healthcare professional."
}
