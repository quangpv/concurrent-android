package ps.billyphan.concurrent

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.fragment.app.Fragment
import android.util.Log
import ps.billyphan.concurrent.async.ConcurrentScope
import java.io.InterruptedIOException

abstract class BaseViewModel : ViewModel() {
    val error = MutableLiveData<Throwable>()
    val loading = MutableLiveData<Boolean>()
    private val scope = ConcurrentScope()

    public override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }

    fun launch(
            loading: MutableLiveData<Boolean>? = this.loading,
            error: MutableLiveData<out Throwable>? = this.error,
            function: ConcurrentScope.() -> Unit
    ) = scope.launch {
        loading?.postValue(true)
        try {
            function(this)
        } catch (e: InterruptedException) {
            Log.e("Interrupt", "Normal")
        } catch (e: InterruptedIOException) {
            Log.e("Interrupt", "IO")
        } catch (e: Throwable) {
            @Suppress("unchecked_cast")
            (error as? MutableLiveData<Throwable>)?.postValue(e)
            e.printStackTrace()
            throw e
        } finally {
            loading?.postValue(false)
        }
    }
}

class ViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    companion object {
        val sInstance = ViewModelFactory()
    }
}

inline fun <reified T : BaseViewModel> ViewModelStoreOwner.getOrCreate(): T {
    return ViewModelProvider(this, ViewModelFactory.sInstance)[T::class.java]
}

inline fun <reified T : BaseViewModel> ViewModelStoreOwner.viewModel() = lazy { getOrCreate<T>() }

inline fun <reified T : BaseViewModel> androidx.fragment.app.Fragment.shareViewModel() = lazy {
    activity!!.getOrCreate<T>()
}

fun <T> MutableLiveData<T>.post(t: T) = postValue(t)

infix fun <A, B, C> Pair<A, B>.to(c: C): Triple<A, B, C> {
    return Triple(first, second, c)
}

