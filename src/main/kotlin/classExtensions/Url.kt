package org.whatever.classExtensions

import io.ktor.http.*

fun Url.getPxlsStyleParams(): Map<String, String> {
    val paramsString =
        this
            .toString()
            .split("#")
            .drop(1)
            .joinToString("#")
    val paramPairStrings = paramsString.split("&")
    return buildMap {
        paramPairStrings.forEach {
            put(it.split("=").first(), it.split("=").drop(1).joinToString("="))
        }
    }
}
