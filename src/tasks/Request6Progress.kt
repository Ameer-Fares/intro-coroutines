package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import contributors.logUsers
import java.util.concurrent.CountDownLatch

suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    val repos = service
        .getOrgRepos(req.org)
        .bodyList()

    val countDownLatch = CountDownLatch(repos.size)
    val users = mutableListOf<User>()

    //region attempt 1 - 50s
//    coroutineScope {
//        repos.map { repo ->
//            val response = async {
//                countDownLatch.countDown()
//                service.getRepoContributors(req.org, repo.name)
//                    .also { logUsers(repo, it) }
//                    .bodyList()
//            }.await()
//            users.addAll(response)
//            updateResults(users.aggregate(), false)
//        }
//        try {
//            countDownLatch.await()
//            updateResults(users.aggregate(), true)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            updateResults(users.aggregate(), true)
//        }
//    }
    //endregion

    //region attempt 2 - 20s
//    coroutineScope {
//        repos.map { repo ->
//            async {
//                service.getRepoContributors(req.org, repo.name)
//                    .also { logUsers(repo, it) }
//                    .bodyList()
//            }
//        }.onEach { differedList ->
//            users.addAll(differedList.await())
//            countDownLatch.countDown()
//            updateResults(users.aggregate(), countDownLatch.count == 0L)
//        }
//    }
    //endregion

    //region solution - 54s no channels
    var allUsers = emptyList<User>()
    for ((index, repo) in repos.withIndex()) {
        val users = service.getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()

        allUsers = (allUsers + users).aggregate()
        updateResults(allUsers, index == repos.lastIndex)
    }
    //endregion
}
