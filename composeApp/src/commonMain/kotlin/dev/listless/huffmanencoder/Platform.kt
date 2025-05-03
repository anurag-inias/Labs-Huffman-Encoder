package dev.listless.huffmanencoder

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform