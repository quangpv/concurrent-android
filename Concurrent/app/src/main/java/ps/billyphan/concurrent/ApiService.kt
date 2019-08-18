package ps.billyphan.concurrent

import android.util.Log
import ps.billyphan.concurrent.async.concurrent
import ps.billyphan.concurrent.async.continuation
import kotlin.random.Random

class ApiService {
    fun getUser(i: Int): User {
        Log.e("User", "Start $i")

        Thread.sleep(1000)

        if (Random.nextBoolean())
            throw UserException(i)

        Log.e("User", "Stop $i")
        return User(i)
    }

    fun getNumOfUser(): Int = continuation { cont ->
        pretendGetNumOfUser({
            cont.resume(it)
        }, {
            cont.resumeWithException(it)
        })
    }

    fun getUserId() = concurrent {
        Log.e("User", "Start ID")
        val userId1 = async { getUser(3) }
        val userId2 = async { getUser(4) }

        Log.e("User", "Stop ID")
        "${userId1.await()} - ${userId2.await()}"
    }
}

private fun pretendGetNumOfUser(success: (Int) -> Unit, error: (Throwable) -> Unit) {
    if (Random.nextBoolean()) {
        success(Random.nextInt())
    } else error(Throwable("Not found num of user"))
}

