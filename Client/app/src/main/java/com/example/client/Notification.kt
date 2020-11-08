package com.example.client

data class Notification (
    var imageUrl: String = "",
    var soundUrl: String = "",
    var time: String = "",
    var decibels: Double = 0.0
)