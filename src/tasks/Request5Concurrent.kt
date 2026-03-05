package tasks

import contributors.*
import kotlinx.coroutines.*

suspend fun loadContributorsConcurrent(service: GitHubService, req: RequestData): List<User> = coroutineScope {
    val repos = async {
        service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .body() ?: emptyList()
    }
        .await()

    val differedUsersList: List<Deferred<List<User>>> = repos.map { repo ->
        async(Dispatchers.Default) {
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }

    differedUsersList.awaitAll().flatten().aggregate()
}