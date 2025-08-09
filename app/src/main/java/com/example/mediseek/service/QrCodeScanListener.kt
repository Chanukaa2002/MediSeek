package com.example.mediseek.service

fun interface QrCodeScanListener {

    fun onQrCodeScanned(value: String)
}