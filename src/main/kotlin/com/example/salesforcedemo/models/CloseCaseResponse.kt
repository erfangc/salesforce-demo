package com.example.salesforcedemo.models

data class CloseCaseResponse(
    val success: Boolean,
    val message: String,
    val caseId: String,
)
