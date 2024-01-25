package otus.homework.coroutines

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException

class MainViewModel(
    private val catsService: CatsService,
    private val imageService: CatImageService,
    private val resManager: Resources
) : ViewModel() {

    private val _uiState = MutableLiveData<Result?>(null)
    val uiState: LiveData<Result?> get() = _uiState

    private var job: Job? = null

    fun updateFact() {
        job?.cancel()
        job = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val fact = async {
                        catsService.getCatFact()
                    }
                    val image = async {
                        imageService.getCatImages().firstOrNull()
                    }
                    FactState(
                        fact = fact.await(),
                        image = image.await()
                    )
                }
                _uiState.value = Result.Success(result)
            } catch (e: Exception) {
                if (e is SocketTimeoutException) {
                    _uiState.value =
                        Result.Error(resManager.getString(R.string.socket_timeout_error))
                } else {
                    CrashMonitor.trackWarning()
                    _uiState.value = Result.Error(resManager.getString(R.string.unknown_error))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
        job = null
    }

    class Factory(
        private val catsService: CatsService,
        private val imageService: CatImageService,
        private val resManager: Resources
    ) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(
                    catsService = catsService,
                    imageService = imageService,
                    resManager = resManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    sealed interface Result {
        data class Success(val state: FactState) : Result

        data class Error(val error: String) : Result
    }
}