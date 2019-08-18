package ps.billyphan.concurrent

import androidx.lifecycle.MutableLiveData


class MainViewModel : BaseViewModel() {
    private val apiService = ApiService()
    val result = MutableLiveData<Triple<User, User, String>>()

    fun loadUsers() = launch {
        val user1 = async { apiService.getUser(1) }
        val user2 = async { apiService.getUser(2) }
        val userId = apiService.getUserId().await()
        val numOfUser = apiService.getNumOfUser()
        result.post(user1.await() to user2.await() to "$userId with num of user $numOfUser")
    }
}