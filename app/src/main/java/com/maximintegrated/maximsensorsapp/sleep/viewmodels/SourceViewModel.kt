package com.maximintegrated.maximsensorsapp.sleep.viewmodels

import android.app.Application
import android.os.Environment
import androidx.lifecycle.*
import com.maximintegrated.maximsensorsapp.sleep.database.SearchFile
import com.maximintegrated.maximsensorsapp.sleep.database.repository.SourceRepository
import com.maximintegrated.maximsensorsapp.sleep.utils.CsvUtil
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File

class SourceViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val SLEEP_OUTPUT_DIRECTORY = File(Environment.getExternalStorageDirectory(), "MaximSleepQa")
    }

    private val sourceRepository: SourceRepository =
        SourceRepository(application)

    private val listParameter: MutableLiveData<SearchListParameter> = MutableLiveData()

    private val listFilePresent: LiveData<List<SearchFile>> =
        Transformations.switchMap(listParameter) {
            sourceRepository.listFileExists(it.nameList, it.md5List)
        }

    private val job = Job()

    private val uiScope = CoroutineScope(job + Dispatchers.Main)

    private var files: Array<File>? = null

    private val listFilePresentObserver = Observer<List<SearchFile>> {list ->
        val nameList = list?.map { it.fileName }?.toMutableList()
        val fileNameList = files?.map { it.name }?.toMutableList()
        if (nameList != null) {
            fileNameList?.removeAll(nameList)
        }
        if (fileNameList != null) {
            importCsvFiles(fileNameList.toTypedArray())
        }
    }

    //private val _busy = MutableLiveData<Boolean>(true)
    //val busy: LiveData<Boolean>
    //    get() = _busy

    init {
        listFilePresent.observeForever(listFilePresentObserver)
    }

    fun getSleepData(){
        if(files.isNullOrEmpty()){
            files = SLEEP_OUTPUT_DIRECTORY.listFiles()
            if(files != null){
                uiScope.launch {
                    listParameter.value = getSleepData(files!!)
                }
            }
        }
    }

    private suspend fun getSleepData(files: Array<File>): SearchListParameter{
        return withContext(Dispatchers.IO){
            SearchListParameter(files.map { it.name }, CsvUtil.listCalculateMD5(files.toList()))
        }
    }

    private fun importCsvFiles(fileNames: Array<String>){
        uiScope.launch {
            for (fileName in fileNames) {
                CsvUtil.importFromCsv(getApplication(), File(SLEEP_OUTPUT_DIRECTORY, fileName))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listFilePresent.removeObserver(listFilePresentObserver)
        job.cancel()
    }

    inner class SearchListParameter(
        var nameList: List<String>,
        var md5List: List<String>
    )
}