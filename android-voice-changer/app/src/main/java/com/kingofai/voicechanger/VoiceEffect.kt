package com.kingofai.voicechanger

/**
 * A named preset combining a pitch ratio, a formant ratio (timbre / vocal-tract
 * size — the key to natural, non-"chipmunk" voices) and optional effects.
 * Pitch/formant deviations from 1.0 are scaled by the user intensity slider.
 */
enum class VoiceEffect(
    val displayName: String,
    val emoji: String,
    val pitch: Float,
    /** Spectral-envelope (formant) ratio. 1.0 keeps natural timbre. */
    val formant: Float = 1.0f,
    val ringMod: Boolean = false,
    val ringFreq: Float = 80f,
    val tremolo: Boolean = false,
    val echo: Boolean = false,
    val lowPass: Boolean = false,
    /** One-pole low-pass coefficient (lower = more muffled/warmer). */
    val lowPassAlpha: Float = 0.20f
) {
    NONE("طبيعي", "🎙️", 1.0f),

    // --- الأصوات البشرية (Human voices): natural pitch + gentle formant shaping ---
    YOUNG_MAN("شاب", "🧑", 0.88f, formant = 0.92f),
    MAN("رجل", "👨", 0.78f, formant = 0.86f),
    OLD_MAN("رجل عجوز", "👴", 0.74f, formant = 0.82f, tremolo = true),
    GIRL("فتاة", "👧", 1.24f, formant = 1.12f),
    WOMAN("امرأة", "👩", 1.34f, formant = 1.18f),
    CHILD("طفل", "🧒", 1.45f, formant = 1.28f),

    // --- أصوات ممتعة (Fun voices) ---
    CHIPMUNK("سنجاب", "🐿️", 1.7f, formant = 1.7f),
    DEEP("صوت عميق", "🔊", 0.62f, formant = 0.78f),
    MONSTER("وحش", "👹", 0.55f, formant = 0.7f, lowPass = true, lowPassAlpha = 0.22f),
    ROBOT("روبوت", "🤖", 1.0f, ringMod = true, ringFreq = 110f),
    ALIEN("فضائي", "👽", 1.15f, formant = 1.2f, ringMod = true, ringFreq = 60f, tremolo = true),
    CAVE("كهف / صدى", "🕳️", 1.0f, echo = true),
    DEMON("شيطان", "😈", 0.62f, formant = 0.72f, ringMod = true, ringFreq = 50f, lowPass = true, lowPassAlpha = 0.3f);

    companion object {
        fun byNameOr(name: String?, default: VoiceEffect = NONE): VoiceEffect =
            entries.firstOrNull { it.name == name } ?: default
    }
}
