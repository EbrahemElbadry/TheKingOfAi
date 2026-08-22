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
    val lowPass: Boolean = false
) {
    NONE("طبيعي", "🎙️", 1.0f),
    MAN("رجل", "👨", 0.82f),
    WOMAN("امرأة", "👩", 1.28f),
    CHILD("طفل", "🧒", 1.5f),
    CHIPMUNK("سنجاب", "🐿️", 1.7f),
    DEEP("صوت عميق", "🔊", 0.65f, lowPass = true),
    MONSTER("وحش", "👹", 0.55f, lowPass = true, echo = true),
    ROBOT("روبوت", "🤖", 1.0f, ringMod = true, ringFreq = 90f),
    ALIEN("فضائي", "👽", 1.2f, ringMod = true, ringFreq = 55f, tremolo = true),
    CAVE("كهف / صدى", "🕳️", 1.0f, echo = true),
    DEMON("شيطان", "😈", 0.5f, ringMod = true, ringFreq = 40f, echo = true, lowPass = true);

    companion object {
        fun byNameOr(name: String?, default: VoiceEffect = NONE): VoiceEffect =
            entries.firstOrNull { it.name == name } ?: default
    }
}
