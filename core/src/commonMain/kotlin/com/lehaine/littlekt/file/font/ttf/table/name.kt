package com.lehaine.littlekt.file.font.ttf.table

import com.lehaine.littlekt.file.ByteBuffer

/**
 * The `name` naming table.
 * https://www.microsoft.com/typography/OTSPEC/name.htm
 * @author Colton Daily
 * @date 12/1/2021
 */
internal class NameParser(val buffer: ByteBuffer, val start: Int, val ltag: List<String>) {
  init {
      TODO("Determine if name table is actually needed?")
  }
}