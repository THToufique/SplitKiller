package com.ripp3r.splitkiller.model

enum class SignatureScheme(val displayName: String) {
    V1("v1 (JAR)"),
    V2("v2"),
    V3("v3"),
    V1_V2("v1 + v2"),
    V2_V3("v2 + v3"),
    V1_V2_V3("v1 + v2 + v3"),
    V1_V3("v1 + v3")
}
