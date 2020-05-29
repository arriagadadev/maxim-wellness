package com.maximintegrated.maximsensorsapp.sleep.database.entity

import androidx.room.*
import com.maximintegrated.maximsensorsapp.sleep.database.DateConverter
import java.util.*

@Entity(
    foreignKeys = [ForeignKey(
        entity = Source::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("source_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
@TypeConverters(DateConverter::class)
class Sleep(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    @ColumnInfo(name = "source_id")
    var sourceId: Long,
    @ColumnInfo(name = "date")
    var date: Date,
    @ColumnInfo(name = "is_sleep")
    var isSleep: Int,
    @ColumnInfo(name = "latency")
    var latency: Int,
    @ColumnInfo(name = "sleep_wake_output")
    var sleepWakeOutput: Int,
    @ColumnInfo(name = "sleep_phases_ready")
    var sleepPhasesReady: Int,
    @ColumnInfo(name = "sleep_phases_output")
    var sleepPhasesOutput: Int,
    @ColumnInfo(name = "hr")
    var hr: Double,
    @ColumnInfo(name = "ibi")
    var ibi: Double,
    @ColumnInfo(name = "spo2")
    var spo2: Int,
    @ColumnInfo(name = "acc_mag")
    var accMag: Double,
    @ColumnInfo(name = "sleep_phases_output_processed")
    var sleepPhasesOutputProcessed: Int
)