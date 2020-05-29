package com.maximintegrated.maximsensorsapp.sleep.utils

import android.app.Application
import com.maximintegrated.maximsensorsapp.sleep.database.entity.Sleep
import com.maximintegrated.maximsensorsapp.sleep.database.entity.Source
import com.maximintegrated.maximsensorsapp.sleep.database.repository.SleepRepository
import com.maximintegrated.maximsensorsapp.sleep.database.repository.SourceRepository
import timber.log.Timber
import java.io.*
import java.lang.Exception
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.ArrayList

class CsvUtil {

    companion object {

        suspend fun importFromCsv(application: Application, file: File) {

            val md5 = calculateMD5(file)
            Timber.d("CsvUtil: File = ${file.name}")

            val source = Source(0, file.name, md5!!)

            val sourceRepo = SourceRepository(application)
            val sleepRepo = SleepRepository(application)
            sourceRepo.deleteByFileName(file.name)

            val sourceId = sourceRepo.insert(source)

            val lines = file.readLines()

            val sleepList = ArrayList<Sleep>()

            for (line in lines) {
                val arr = line.split(",")
                val dateArr = arr[0].replace("'", "").split("/")
                val dateCal: Calendar = Calendar.getInstance()

                dateCal.set(
                    dateArr[0].toInt(),
                    dateArr[1].toInt() - 1,
                    dateArr[2].toInt(),
                    dateArr[3].toInt(),
                    dateArr[4].toInt(),
                    dateArr[5].toInt()
                )

                val date = Date(dateCal.timeInMillis)

                try{
                    val sleepPhasesOutputProcessed = if(arr.size > 10){
                        arr[10].trim().toIntOrNull() ?: arr[5].trim().toInt()
                    }else{
                        arr[5].trim().toInt()
                    }

                    sleepList.add(
                        Sleep(
                            id = 0,
                            sourceId = sourceId,
                            date = date,
                            isSleep = arr[1].trim().toInt(),
                            latency = arr[2].trim().toInt(),
                            sleepWakeOutput = arr[3].trim().toInt(),
                            sleepPhasesReady = arr[4].trim().toInt(),
                            sleepPhasesOutput = arr[5].trim().toInt(),
                            hr = arr[6].trim().toDouble(),
                            ibi = arr[7].trim().toDouble(),
                            spo2 = arr[8].trim().toInt(),
                            accMag = arr[9].trim().toDouble(),
                            sleepPhasesOutputProcessed = sleepPhasesOutputProcessed
                        )
                    )
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

            sleepRepo.insertAll(sleepList)
        }

        private fun calculateMD5(updateFile: File): String? {
            val digest: MessageDigest
            try {
                digest = MessageDigest.getInstance("MD5")
            } catch (e: NoSuchAlgorithmException) {
                Timber.e(e, "Exception while getting digest")
                return null
            }

            val inputStream: InputStream
            try {
                inputStream = FileInputStream(updateFile)
            } catch (e: FileNotFoundException) {
                Timber.e(e, "Exception while getting FileInputStream")
                return null
            }

            val buffer = ByteArray(8192)
            var read = 0
            try {

                while ({ read = inputStream.read(buffer); read }() > 0) {
                    digest.update(buffer, 0, read)
                }

                val md5sum = digest.digest()
                val bigInt = BigInteger(1, md5sum)
                var output = bigInt.toString(16)

                output = String.format("%32s", output).replace(' ', '0')
                return output
            } catch (e: IOException) {
                throw RuntimeException("Unable to process file for MD5", e)
            } finally {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    Timber.e(e, "Exception on closing MD5 input stream")
                }
            }
        }

        fun listCalculateMD5(list: List<File>): List<String> {
            val resultList = ArrayList<String>()
            for (i in list) {
                resultList.add(calculateMD5(i).toString())
            }

            return resultList
        }
    }
}