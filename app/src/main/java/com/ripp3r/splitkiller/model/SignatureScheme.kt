package com.ripp3r.splitkiller.model

enum class SignatureScheme(val displayName: String) {
    V1("v1 (JAR Signature)"),
    V2("v2 (APK Signature)"),
    V3("v3 (APK Signature v3)"),
    V1_V2("v1 + v2"),
    V1_V2_V3("v1 + v2 + v3"),
    UNSIGNED("Unsigned");
    
    val useV1: Boolean
        get() = this == V1 || this == V1_V2 || this == V1_V2_V3
    
    val useV2: Boolean
        get() = this == V2 || this == V1_V2 || this == V1_V2_V3
    
    val useV3: Boolean
        get() = this == V3 || this == V1_V2_V3
    
    val isSigned: Boolean
        get() = this != UNSIGNED
}
