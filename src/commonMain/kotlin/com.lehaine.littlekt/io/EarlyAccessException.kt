package com.lehaine.littlekt.io

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class EarlyAccessException(val filename: String, val property: String) :
    RuntimeException("Content of file '$filename' was accessed before being loaded by the property '$property'!")