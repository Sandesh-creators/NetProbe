package com.netprobe.diagnostics.data.model

data class OuiResult(
    val prefix: String,
    val vendor: String,
    val isPrivate: Boolean = false
)
