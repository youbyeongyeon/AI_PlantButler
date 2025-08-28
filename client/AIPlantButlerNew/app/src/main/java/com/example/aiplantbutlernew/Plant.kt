package com.example.aiplantbutlernew

data class Plant(
    val name: String,
    val imageUriString: String,
    val tasks: MutableList<Task> = mutableListOf()
)