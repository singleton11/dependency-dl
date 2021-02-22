package com.github.singleton11.depenencydl.integration.util

import org.apache.maven.model.Repository
import org.apache.maven.model.RepositoryPolicy

class RepositoryHelper {
    companion object {
        fun getInternalMavenRepositories(repositories: List<Pair<String, String>>): List<Repository> =
            repositories.map {
                object : Repository() {
                    init {
                        id = it.first
                        releases = RepositoryPolicy().apply {
                            enabled = "true"
                        }
                        url = it.second
                    }
                }
            }
    }
}