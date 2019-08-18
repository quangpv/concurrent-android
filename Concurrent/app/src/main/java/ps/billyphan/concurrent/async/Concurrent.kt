package ps.billyphan.concurrent.async

import android.util.Log
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

private const val STATE_INIT = -1
private const val STATE_ERROR = 1
private const val STATE_SUCCESS = 0

open class ConcurrentScope {

    private val mChildScopes = arrayListOf<ConcurrentScope>()
    private val mJobs = hashSetOf<Job>()

    fun cancel() {
        mChildScopes.forEach { it.cancel() }
        mJobs.asSequence().filter { !it.isDone() }
                .forEach { it.cancel() }
    }

    fun <T : Any> async(function: ConcurrentScope.() -> T): Deferred<T> =
            Deferred(Dispatchers.AsyncIO, newChildScope(), function)

    fun launch(executor: JobExecutor = Dispatchers.Default, function: ConcurrentScope.() -> Unit): Job {
        val child = newChildScope()
        return executor.submit {
            try {
                function(child)
            } catch (t: Throwable) {
                cancel()
                Log.e("ConcurrentScope", "Cancel all")
            }
        }.apply { addJob(this) }
    }

    private fun newChildScope(): ConcurrentScope = ConcurrentScope().apply { mChildScopes.add(this) }

    fun addJob(job: Job) = mJobs.add(job)

    fun <T : Any> Concurrent<T>.async(): Deferred<T> = Deferred(Dispatchers.AsyncIO, newChildScope(), function)

    fun <T : Any> Concurrent<T>.await(): T = Deferred(Dispatchers.BlockIO, newChildScope(), function).await()

    fun <T : Any> Concurrent<T>.tryAwait(): Pair<T, Throwable?> {
        return try {
            await() to null
        } catch (t: Throwable) {
            await() to t
        }
    }

    fun <T : Any, V : Throwable> Concurrent<T>.awaitOrThrow(catch: KClass<V>, function: (V) -> Throwable): T {
        return try {
            await()
        } catch (t: Throwable) {
            if (catch.java.isInstance(t)) throw function(t as V)
            else throw t
        }
    }
}

object GlobalScope : ConcurrentScope()

class Concurrent<T : Any> internal constructor(val function: ConcurrentScope.() -> T)

interface Continuation<T : Any> {
    fun resume(result: T)
    fun resumeWithException(error: Throwable)
}

class AwaitContinuation<T : Any>(function: (Continuation<T>) -> Unit) : ReentrantLock(), Continuation<T> {
    private lateinit var mError: Throwable
    private lateinit var mResult: T
    private var mState = STATE_INIT
    private val mLock = newCondition()

    init {
        function(this)
    }

    fun await(): T = withLock {
        if (mState == STATE_INIT) mLock.await()
        if (mState == STATE_ERROR) throw mError
        if (!this@AwaitContinuation::mResult.isInitialized) throw InterruptedException()
        mResult
    }

    override fun resume(result: T) = withLock {
        mResult = result
        mState = STATE_SUCCESS
        mLock.signal()
    }

    override fun resumeWithException(error: Throwable) = withLock {
        mError = error
        mState = STATE_ERROR
        mLock.signal()
    }
}

class Deferred<T : Any>(
        executor: JobExecutor,
        private val scope: ConcurrentScope,
        private val function: ConcurrentScope.() -> T
) : Job {

    private var mJob: WaitableJob
    private lateinit var mError: Throwable
    private lateinit var mResult: T

    private var mStatus: Int = STATE_INIT

    init {
        mJob = executor.submit {
            try {
                mResult = function(scope)
                mStatus = STATE_SUCCESS

            } catch (e: Throwable) {
                mError = e
                mStatus = STATE_ERROR
            }
        }
        scope.addJob(mJob)
    }

    fun await(): T {
        if (mStatus == STATE_INIT) mJob.waitSignal()
        if (mStatus == STATE_ERROR) throw mError
        if (!::mResult.isInitialized) throw InterruptedException()
        return mResult
    }

    override fun cancel() = mJob.cancel()

    override fun isDone(): Boolean = mStatus != STATE_INIT
}

fun <T : Any> concurrent(function: ConcurrentScope.() -> T): Concurrent<T> = Concurrent(function)
fun <T : Any> continuation(function: (Continuation<T>) -> Unit): T = AwaitContinuation(function).await()
