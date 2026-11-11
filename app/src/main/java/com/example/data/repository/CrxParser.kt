package com.example.data.repository

import com.example.data.model.KopiExtension

object CrxParser {
    fun parseCrx(bytes: ByteArray, defaultName: String = "extension.kd"): KopiExtension {
        // Delegate to unified KD Parser
        return KdParser.parseKd(bytes, defaultName)
    }
}
