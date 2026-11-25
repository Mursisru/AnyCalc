package com.mursis.anycalc.data

import androidx.compose.ui.graphics.Color

data class Palette(
    val id: String,
    val name: String,
    val group: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val danger: Color,
)

private fun c(hex: String) = Color(android.graphics.Color.parseColor(hex))

val DARK_PALETTES = listOf(
    Palette("dark_obsidian", "Обсидиан", "Бизнес", c("#FF050505"), c("#FF111214"), c("#FF1C1C1E"), c("#14FFFFFF"), c("#FFF5F5F7"), c("#FFB0B2B8"), c("#FFE5E5EA"), c("#FFC8A96A"), c("#FF30D158"), c("#FFFF453A")),
    Palette("dark_graphite", "Графит", "Бизнес", c("#FF0A0A0C"), c("#FF16181B"), c("#FF222428"), c("#16FFFFFF"), c("#FFF2F3F5"), c("#FFB3B6BD"), c("#FFDDE0E6"), c("#FFA8AAB0"), c("#FF34C77B"), c("#FFF0544D")),
    Palette("dark_slate", "Сланец", "Бизнес", c("#FF080B0F"), c("#FF121820"), c("#FF1B242E"), c("#14FFFFFF"), c("#FFEFF3F7"), c("#FFAFBAC6"), c("#FFDCE6F0"), c("#FF7FB3D5"), c("#FF3ECB8F"), c("#FFFF5B5B")),
    Palette("dark_charcoal", "Уголь", "Бизнес", c("#FF101010"), c("#FF1B1B1B"), c("#FF262626"), c("#18FFFFFF"), c("#FFF7F6F3"), c("#FFBCB9B1"), c("#FFF0EDE6"), c("#FFCBB995"), c("#FF4ACF86"), c("#FFFF6B6B")),
    Palette("dark_lavender", "Лаванда", "Пастель", c("#FF100E16"), c("#FF1B1826"), c("#FF272134"), c("#18FFFFFF"), c("#FFF4F0FA"), c("#FFC4BCD3"), c("#FFDCCBFF"), c("#FFB9A2E8"), c("#FF6BD4A8"), c("#FFFF8FA0")),
    Palette("dark_teal", "Глубокий тил", "Пастель", c("#FF08110F"), c("#FF101D1A"), c("#FF182A25"), c("#14FFFFFF"), c("#FFEEF7F3"), c("#FFB2CCC3"), c("#FFC8EBDC"), c("#FF85C7AE"), c("#FF55D6A0"), c("#FFFF8C8C")),
    Palette("dark_rose", "Розовый дым", "Пастель", c("#FF120D10"), c("#FF20161B"), c("#FF2D2027"), c("#16FFFFFF"), c("#FFF8F0F3"), c("#FFCFBAC4"), c("#FFF3D3DE"), c("#FFE0A6BC"), c("#FF63D3A6"), c("#FFFF8A9B")),
    Palette("dark_sand", "Песок", "Пастель", c("#FF12100C"), c("#FF1F1A14"), c("#FF2C251C"), c("#16FFFFFF"), c("#FFF8F4EC"), c("#FFCFC2AE"), c("#FFEFDFC4"), c("#FFD9BC8E"), c("#FF77CE9C"), c("#FFFF9A87")),
)

val LIGHT_PALETTES = listOf(
    Palette("light_paper", "Бумага", "Бизнес", c("#FFF7F7F8"), c("#FFFFFFFF"), c("#FFEFF0F2"), c("#FFE2E3E7"), c("#FF111217"), c("#FF6C7079"), c("#FF1C1D22"), c("#FF9A7B3F"), c("#FF1FA971"), c("#FFD93A3A")),
    Palette("light_pearl", "Жемчуг", "Бизнес", c("#FFF2F3F5"), c("#FFFFFFFF"), c("#FFE7E9ED"), c("#FFDCDFE4"), c("#FF191B20"), c("#FF71757E"), c("#FF23262C"), c("#FF8E8E96"), c("#FF15936A"), c("#FFCE3535")),
    Palette("light_frost", "Иней", "Бизнес", c("#FFEDF2F6"), c("#FFFFFFFF"), c("#FFE0E8EF"), c("#FFD3DFE8"), c("#FF16202A"), c("#FF687686"), c("#FF1B2B3A"), c("#FF5C8FB3"), c("#FF0E9A72"), c("#FFD23535")),
    Palette("light_linen", "Лён", "Бизнес", c("#FFF5F2EC"), c("#FFFFFFFC"), c("#FFEAE4D9"), c("#FFDED6C8"), c("#FF211E18"), c("#FF7B7366"), c("#FF2C261D"), c("#FFA88A55"), c("#FF159A70"), c("#FFCF3A3A")),
    Palette("light_blush", "Румянец", "Пастель", c("#FFFBEFF1"), c("#FFFFFFFF"), c("#FFF4E5EA"), c("#FFEFD9E0"), c("#FF2A1B21"), c("#FF8B737D"), c("#FF35242B"), c("#FFD98FA5"), c("#FF14966F"), c("#FFD63752")),
    Palette("light_mint", "Мята", "Пастель", c("#FFEDF7F2"), c("#FFFFFFFF"), c("#FFE1F0E9"), c("#FFD3E7DD"), c("#FF17261F"), c("#FF6C8477"), c("#FF1E332A"), c("#FF72B79A"), c("#FF0F9B71"), c("#FFD43C4E")),
    Palette("light_lilac", "Лилиак", "Пастель", c("#FFF2EEFA"), c("#FFFFFFFE"), c("#FFE9E0F5"), c("#FFDDD0EE"), c("#FF231B2E"), c("#FF7E7390"), c("#FF31253F"), c("#FFA78ACB"), c("#FF149B76"), c("#FFD33B58")),
    Palette("light_butter", "Масло", "Пастель", c("#FFFCF6E9"), c("#FFFFFFFC"), c("#FFF4EBD8"), c("#FFEDE0C7"), c("#FF282214"), c("#FF857B64"), c("#FF382E1B"), c("#FFD0AF6C"), c("#FF129B72"), c("#FFD33C3C")),
)

val ALL_PALETTES = DARK_PALETTES + LIGHT_PALETTES

fun paletteById(id: String, dark: Boolean): Palette {
    val list = if (dark) DARK_PALETTES else LIGHT_PALETTES
    return list.firstOrNull { it.id == id } ?: list.first()
}
