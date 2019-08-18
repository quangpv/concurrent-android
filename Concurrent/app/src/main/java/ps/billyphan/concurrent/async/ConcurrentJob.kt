package ps.billyphan.concurrent.async

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


interface Job {

    fun isDone(): Boolean

    fun cancel()

    companion object {
        const val TIME_OUT = 1L
    }
}

interface WaitableJob : Job {
    fun waitSignal()
}

private class AsyncJob(private val future: Future<*>) : WaitableJob {

    override fun waitSignal() {
        future.get(Job.TIME_OUT, TimeUnit.MINUTES)
    }

    override fun isDone(): Boolean {
        return future.isDone
    }

    override fun cancel() {
        future.cancel(true)
    }

}

private open class SyncJob : WaitableJob {

    override fun cancel() {}

    override fun waitSignal() {}

    override fun isDone() = true
}

interface JobExecutor {
    fun submit(runnable: () -> Unit): WaitableJob
}

internal class JobCachedExecutor : JobExecutor {
    private val executor = Executors.newCachedThreadPool()!!

    override fun submit(runnable: () -> Unit): WaitableJob {
        val future = executor.submit(runnable)!!
        return AsyncJob(future)
    }
}

internal class JobBlockingExecutor : JobExecutor {
    override fun submit(runnable: () -> Unit): WaitableJob {
        runnable()
        return SyncJob()
    }
}

internal class MainExecutor : JobExecutor {
    private val mHandler = Handler(Looper.getMainLooper())

    override fun submit(runnable: () -> Unit): WaitableJob {
        mHandler.post(runnable)
        return SyncJob()
    }
}

object Dispatchers {
    val Default: JobExecutor = JobCachedExecutor()
    val AsyncIO: JobExecutor = JobCachedExecutor()
    val BlockIO: JobExecutor = JobBlockingExecutor()
    val MainIO: JobExecutor = MainExecutor()
}