package com.maximintegrated.maximsensorsapp.bpt

import android.app.Application
import android.os.Environment
import android.os.Handler
import android.os.SystemClock
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.maximintegrated.maximsensorsapp.R
import kotlinx.coroutines.*
import java.io.File
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

enum class BptStatus {
    NO_SIGNAL,
    PROGRESS,
    SUCCESS,
    BAD_SIGNAL,
    MOTION,
    FAILURE,
    CAL_SEGMENT_DONE,
    INIT_SUBJECT_FAILURE,
    INIT_SUCCESS,
    INIT_CAL_REF_BP_TRENDING_ERROR,
    INIT_CAL_REF_BP_INCONSISTENCY_ERROR_1,
    INIT_CAL_REF_BP_INCONSISTENCY_ERROR_2,
    INIT_CAL_REF_BP_INCONSISTENCY_ERROR_3,
    INIT_MIN_PULSE_PRESSURE_ERROR,
    INIT_CAL_REF_BP_ERRONEOUS_REF,
    HR_TOO_HIGH_IN_CAL,
    HR_TOO_HIGH_IN_EST,
    PI_OUT_OF_RANGE,
    ESTIMATION_ERROR,
    TOO_MANY_CAL_POINT_ERROR;

    companion object {
        private val map = values().associateBy(BptStatus::ordinal)
        fun fromInt(index: Int) = map[index]
    }
}

private const val CALIBRATION_TIMEOUT_IN_SEC = 10

class BptViewModel(private val app: Application) : AndroidViewModel(app) {

    companion object {
        val OUTPUT_DIRECTORY = File(
            Environment.getExternalStorageDirectory(),
            "MaximSensorsApp${File.separator}BP Trending"
        )
    }

    private val _userList = MutableLiveData<MutableList<String>>(arrayListOf())
    val userList: LiveData<MutableList<String>>
        get() = _userList

    private var startElapsedTime = 0L
    private var handler = Handler()
    private var timerStarted = false

    private val _elapsedTime = MutableLiveData<Long>(0)
    val elapsedTime: LiveData<Long>
        get() = _elapsedTime

    var startTime = ""
        private set

    private val _calibrationStates = MutableLiveData<Pair<Int, CalibrationStatus>>()
    val calibrationStates: LiveData<Pair<Int, CalibrationStatus>?>
        get() = _calibrationStates

    private val _isMonitoring = MutableLiveData<Boolean>(false)
    val isMonitoring: LiveData<Boolean>
        get() = _isMonitoring

    val spO2Coefficients =
        floatArrayOf(1.5958422407923467f, -34.6596622470280020f, 112.6898759138307500f)

    private var calibrationTimePassed = AtomicInteger(-1)

    private var _historyDataList = MutableLiveData<List<BptHistoryData>?>(emptyList())
    val historyDataList: LiveData<List<BptHistoryData>?>
        get() = _historyDataList

    private val job = Job()

    private val uiScope = CoroutineScope(job + Dispatchers.Main)

    init {
        prepareUserList()
        readSpO2ConfigFile()
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    private fun readSpO2ConfigFile() {
        val file = File(OUTPUT_DIRECTORY, "${File.separator}SPO2.conf")
        if (!file.exists()) {
            OUTPUT_DIRECTORY.mkdirs()
            file.createNewFile()
            file.bufferedWriter()
                .use { out -> out.write("${spO2Coefficients[0]},${spO2Coefficients[1]},${spO2Coefficients[2]}") }
        } else {
            val str = file.bufferedReader().readText()
            val coeffs = str.split(",")
            for (i in spO2Coefficients.indices)
                spO2Coefficients[i] = coeffs[i].toFloatOrNull() ?: spO2Coefficients[i]
        }
    }

    fun addNewUser(name: String) {
        if (BptSettings.users.contains(name)) {
            Toast.makeText(
                app,
                app.getString(R.string.username_already_exists),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            BptSettings.users.add(name)
        }
        prepareUserList()
    }

    private fun prepareUserList() {
        _userList.value?.clear()
        _userList.value?.add(app.getString(R.string.select_user))
        if (BptSettings.users.isNotEmpty()) {
            _userList.value?.addAll(BptSettings.users)
            _userList.value = _userList.value
        }
    }

    fun startTimer() {
        if (!timerStarted) {
            startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            startElapsedTime = SystemClock.elapsedRealtime()
            _elapsedTime.value = SystemClock.elapsedRealtime() - startElapsedTime
            handler.postDelayed(tickRunnable, 1000)
            timerStarted = true
        }
    }

    fun restartTimer() {
        handler.removeCallbacks(tickRunnable)
        startTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        startElapsedTime = SystemClock.elapsedRealtime()
        _elapsedTime.value = SystemClock.elapsedRealtime() - startElapsedTime
        handler.postDelayed(tickRunnable, 1000)
        timerStarted = true
    }

    fun stopTimer() {
        handler.removeCallbacks(tickRunnable)
        timerStarted = false
        startTime = ""
        startElapsedTime = 0
        _elapsedTime.value = 0
        calibrationTimePassed.set(-1)
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (timerStarted) {
                _elapsedTime.value = SystemClock.elapsedRealtime() - startElapsedTime
                handler.postDelayed(this, 1000)
                if (calibrationTimePassed.get() >= 0) {

                    if (calibrationTimePassed.incrementAndGet() == CALIBRATION_TIMEOUT_IN_SEC) {
                        onCalibrationTimeout()
                    }
                }
            } else {
                handler.removeCallbacks(this)
            }
        }
    }

    fun startDataCollection(index: Int) {
        if (!_isMonitoring.value!!) {
            val state = Pair(index, CalibrationStatus.STARTED)
            _calibrationStates.value = state
            _isMonitoring.value = true
            startTimer()
        }
    }

    fun stopDataCollection(index: Int = 0) {
        if (_calibrationStates.value?.second != CalibrationStatus.SUCCESS && _calibrationStates.value?.second != CalibrationStatus.FAIL) {
            _calibrationStates.value = Pair(index, CalibrationStatus.IDLE)
        }
        _isMonitoring.value = false
        stopTimer()
    }

    fun onCalibrationResultsRequested() {
        _calibrationStates.value?.let {
            val state = Pair(_calibrationStates.value!!.first, CalibrationStatus.PROCESSING)
            _calibrationStates.value = state
        }
        calibrationTimePassed.set(0)
    }

    fun onCalibrationReceived() {
        _calibrationStates.value?.let {
            val state = Pair(_calibrationStates.value!!.first, CalibrationStatus.SUCCESS)
            _calibrationStates.value = state
        }
        calibrationTimePassed.set(-1)
    }

    fun onCalibrationTimeout() {
        _calibrationStates.value?.let {
            val state = Pair(_calibrationStates.value!!.first, CalibrationStatus.FAIL)
            _calibrationStates.value = state
        }
        calibrationTimePassed.set(-1)
    }

    fun startMeasurement() {
        _isMonitoring.value = true
        startTimer()
    }

    fun stopMeasurement() {
        _isMonitoring.value = false
        stopTimer()
    }

    fun isWaitingForCalibrationResults(): Boolean {
        return calibrationStates.value?.second == CalibrationStatus.PROCESSING
    }

    fun refreshHistoryData(){
        uiScope.launch {
            _historyDataList.value = getHistoryData()
        }
    }

    private suspend fun getHistoryData(): List<BptHistoryData> {
        return withContext(Dispatchers.IO){
            readHistoryData()
        }
    }
}