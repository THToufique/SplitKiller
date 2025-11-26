package com.ripp3r.splitkiller.model

data class SigningKey(
    val id: String,
    val alias: String,
    val createdDate: Long,
    val validityYears: Int,
    val organization: String,
    val isDefaultKey: Boolean = false
)
