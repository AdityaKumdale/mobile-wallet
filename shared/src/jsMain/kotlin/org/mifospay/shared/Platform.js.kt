package org.mifospay.shared

fun main() {
    onWasmReady {
        Window("KmpApp") {
            App()
        }
    }
}

actual fun getPlatform(): Platform {
    TODO("Not yet implemented")
}