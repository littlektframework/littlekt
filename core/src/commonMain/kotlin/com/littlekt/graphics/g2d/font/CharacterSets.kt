package com.littlekt.graphics.g2d.font

/**
 * @author Colton Daily
 * @date 12/2/2021
 */
object CharacterSets {
    const val SPACE = " "
    const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    const val NUMBERS = "0123456789"
    const val PUNCTUATION = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}"
    const val LATIN_BASIC =
        "çÇ ñÑ åÅ æÆ ÿ ¢£¥Pª°¿¬½¼¡«»ßµø±÷°·.² áéíóúäëïöüàèìòùâêîôû ÁÉÍÓÚÄËÏÖÜÀÈÌÒÙÂÊÎÔÛ"
    const val CYRILLIC = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
    val LATIN_ALL = SPACE + UPPERCASE + LOWERCASE + NUMBERS + PUNCTUATION + LATIN_BASIC
}
