package com.ripp3r.splitkiller.model

data class SigningKey(
    val name: String,
    val alias: String,
    val keystorePath: String? = null
)
