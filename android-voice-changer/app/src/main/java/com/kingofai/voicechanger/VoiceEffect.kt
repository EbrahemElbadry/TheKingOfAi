package com.kingofai.voicechanger

/**
 * A named preset that combines a base pitch ratio with optional effects.
 * [pitch] deviation from 1.0 is scaled by a user "intensity" slider at runtime.
 */
enum class VoiceEffect(
    val displayName: String,
    val emoji: String,
    val pitch: Float,
    val ringMod: Boolean = false,
    val ringFreq: Float = 80f,
    val tremolo: Boolean = false,
    val echo: Boolean = false,
    val lowPass: Boolean = false,
    /** One-pole low-pass coefficient (lower = more muffled/warmer). */
    val lowPassAlpha: Float = 0.20f
) {
    NONE("طبيعي", "🎙️", 1.0f),

    // --- الأصوات البشرية (Human voices) ---
    YOUNG_MAN("شاب", "🧑", 0.90f, lowPass = true, lowPassAlpha = 0.55f),
    MAN("رجل", "👨", 0.80f, lowPass = true, lowPassAlpha = 0.45f),
    OLD_MAN("رجل عجوز", "👴", 0.72f, lowPass = true, lowPassAlpha = 0.35f, tremolo = true),
    GIRL("فتاة", "👧", 1.18f),
    WOMAN("امرأة", "👩", 1.30f),
    CHILD("طفل", "🧒", 1.5f),

    // --- أصوات ممتعة (Fun voices) ---
    CHIPMUNK("سنجاب", "🐿️", 1.7f),
    DEEP("صوت عميق", "🔊", 0.65f, lowPass = true, lowPassAlpha = 0.20f),
    MONSTER("وحش", "👹", 0.55f, lowPass = true, lowPassAlpha = 0.18f, echo = true),
    ROBOT("روبوت", "🤖", 1.0f, ringMod = true, ringFreq = 90f),
    ALIEN("فضائي", "👽", 1.2f, ringMod = true, ringFreq = 55f, tremolo = true),
    CAVE("كهف / صدى", "🕳️", 1.0f, echo = true),
    DEMON("شيطان", "😈", 0.5f, ringMod = true, ringFreq = 40f, echo = true, lowPass = true, lowPassAlpha = 0.18f);

    companion object {
        fun byNameOr(name: String?, default: VoiceEffect = NONE): VoiceEffect =
            entries.firstOrNull { it.name == name } ?: default
    }
}
