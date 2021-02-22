package com.github.singleton11.depenencydl.model

data class Repositories(val repositories: List<Repository>) {
    fun listOfPairs() = repositories.map { it.id to it.url }
}