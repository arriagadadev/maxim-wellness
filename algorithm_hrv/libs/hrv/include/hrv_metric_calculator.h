/*******************************************************************************
* Copyright (C) Maxim Integrated Products, Inc., All rights Reserved.
*
* This software is protected by copyright laws of the United States and
* of foreign countries. This material may also be protected by patent laws
* and technology transfer regulations of the United States and of foreign
* countries. This software is furnished under a license agreement and/or a
* nondisclosure agreement and may only be used or reproduced in accordance
* with the terms of those agreements. Dissemination of this information to
* any party or parties not specified in the license agreement and/or
* nondisclosure agreement is expressly prohibited.
*
* The above copyright notice and this permission notice shall be included
* in all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL MAXIM INTEGRATED BE LIABLE FOR ANY CLAIM, DAMAGES
* OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
* ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
* OTHER DEALINGS IN THE SOFTWARE.
*
* Except as contained in this notice, the name of Maxim Integrated
* Products, Inc. shall not be used except as stated in the Maxim Integrated
* Products, Inc. Branding Policy.
*
* The mere transfer of this software does not imply any licenses
* of trade secrets, proprietary technology, copyrights, patents,
* trademarks, maskwork rights, or any other form of intellectual
* property whatsoever. Maxim Integrated Products, Inc. retains all
* ownership rights.
*******************************************************************************
*/

/**
* @file hrv_metric_calculator.h
* @date August 2019
* @brief HRV Metric Calculator Module header file
*/

/**
* @defgroup hrv_metric_calculator Maxim HRV Metric Calculator Module
* @brief    Module for calculating HRV metrics
*/

#ifndef __HRV_METRIC_CALCULATOR_H__
#define __HRV_METRIC_CALCULATOR_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>
#include "ibi_container.h"
#include "lombPeriodogram.h"

/**
* @ingroup hrv_metric_calculator
*
* This part is very critical to determine RAM consumption of the module.
* These should be tuned to fit the module in embedded platforms if needed.
* Please especially pay attention to POW2_CEIL function for effective
* parameter tuning.
*/
/**@{*/

#define MAX_HRV_METRIC_CALC_WIN_SIZE_SEC 120    /**< Max Value for runtime configurable HRV metric calculation window in sec (@ref _HrvMetricCalcConfig.windowSizeInSec) */
#define MAX_HRV_METRIC_CALC_WIN_SHIFT_SEC 120    /**< Max Value for runtime configurable HRV metric calculation window shift size in sec (@ref _HrvMetricCalcConfig.windowShiftSizeInSec)*/

#define VALID_WINDOW_PERCENT_POPULATED 70    /**< At least this percentage of the window should be populated temporally for HRV calculation */
#define OVERSAMPLING_FACTOR 2    /**< Spectrum oversampling factor (samples per Rayleigh width) for Lomb Periodogram method */
#define UPPER_FREQ_FACTOR 1    /**< Calculated frequency upper limit in terms of a multiple of the Nyquist frequency */
#define MINIMUM_ELEMENTS_TO_CALCULATE_HRV 4    /**< Minimum number of IBI samples for HRV calculation */

#define MAX_N_IBIS_IN_WINDOW ((int)(MAX_HRV_METRIC_CALC_WIN_SIZE_SEC * 3))    /**< Maximum allowed IBI samples in an HRV metric calculation window (window size is runtime configurable but for the static array can be defined as 3 * @ref MAX_HRV_METRIC_CALC_WIN_SIZE_SEC) */

#define MAX_EXTIRPOLATED_SIGNAL_SIZE_INTERMEDIATE ((int)(MAX_N_IBIS_IN_WINDOW * OVERSAMPLING_FACTOR * UPPER_FREQ_FACTOR * EXTIRP_FACTOR * 2))
    /**< Intermediate value for calculating maximum signal size after extirpolation for Lomb Periodogram */
#define MAX_EXTIRPOLATED_SIGNAL_SIZE POW2_CEIL_LP(MAX_EXTIRPOLATED_SIGNAL_SIZE_INTERMEDIATE)    /**< Maximum signal size after extirpolation for Lomb Periodogram */
#define MAX_LOMB_PERIODOGRAM_OUTPUT_SIZE ((int)(MAX_N_IBIS_IN_WINDOW * OVERSAMPLING_FACTOR * UPPER_FREQ_FACTOR / 2))
    /**< Maximum output array size for Lomb Periodogram */

#define ULF_UPPER_LIMIT_HZ 0.0033    /**< Ultra low frequency band upper limit in Hz */
#define VLF_UPPER_LIMIT_HZ 0.04    /**< Very low frequency band upper limit in Hz */
#define LF_UPPER_LIMIT_HZ 0.15    /**< Low frequency band upper limit in Hz */
#define HF_UPPER_LIMIT_HZ 0.4    /**< High frequency band upper limit in Hz */
/**@}*/

/**
* @ingroup hrv_metric_calculator
* @brief   HRV Metric Calculator return codes
*/
typedef enum _HrvMetricCalcRet {
    HRV_METRIC_CALC_SUCCESS,
    HRV_METRIC_CALC_NULL_PTR_ERROR,
    HRV_METRIC_CALC_INVALID_CONFIG_ERROR,
    HRV_METRIC_CALC_IBI_CONTAINER_ERROR,
    HRV_METRIC_CALC_IBI_SEQUENCE_CROPPING_ERROR,
    HRV_METRIC_CALC_LOMB_PERIODOGRAM_ERROR,
    HRV_METRIC_CALC_BUFFER_ALLOCATION_ERROR
} HrvMetricCalcRet;

/**
* @ingroup hrv_metric_calculator
* @brief   HRV metric calculator configuration structure
*/
typedef struct _HrvMetricCalcConfig {

    int16_t windowSizeInSec;      /**< HRV metric calculation window in sec   */
    int16_t windowShiftSizeInSec; /**< HRV metric calculation window shift size
                                       in sec                                 */
} HrvMetricCalcConfig;

/**
* @ingroup hrv_metric_calculator
* @brief   Default configuration package object (read-only)
*/
extern const HrvMetricCalcConfig defaultHrvMetricCalcConfig;


/**
* @ingroup hrv_metric_calculator
* @brief   HRV Metric Calculator instance definition
*/
typedef struct _HrvMetricCalculator {
    IbiContainer ibiWindow;    /**< IBI window instance */
    float ibis[MAX_N_IBIS_IN_WINDOW];    /**< External IBI buffer for the IBI window instance (in ms) */
    float times[MAX_N_IBIS_IN_WINDOW];    /**< External time buffer for the IBI window instance (in sec) */
    float windowEndTime;    /**< Planned ending time of the current windowed IBI accumulation in sec */
    int progress;

    HrvMetricCalcConfig config; /**< Used configuration (set by
                                     ::initHrvMetricCalc method)              */
} HrvMetricCalculator;

/**
* @ingroup hrv_metric_calculator
* @brief   HRV metric calculator input data structure
*/
typedef struct _HrvMetricCalcInData {
    float ibi;    /**< New IBI sample in ms  */
    float time;    /**< Timing of the new IBI sample in sec */
} HrvMetricCalcInData;

/**
* @ingroup hrv_metric_calculator
* @brief   Time domain HRV metrics structure
*/
typedef struct _TimeDomainHrvMetrics {
    float avnn;    /**< Average of NN intervals in ms */
    float sdnn;    /**< Standard deviation of NN intervals in ms */
    float rmssd;    /**< RMS value of successive differences in ms */
    float pnn50;    /**< Percentage of successive differences greater than 50 ms */
} TimeDomainHrvMetrics;

/**
* @ingroup hrv_metric_calculator
* @brief   Frequency domain HRV metrics structure
*/
typedef struct _FreqDomainHrvMetrics {
    float ulf;    /**< Power in ULF band (ms^2) */
    float vlf;    /**< Power in VLF band (ms^2) */
    float lf;    /**< Power in LF band (ms^2) */
    float hf;    /**< Power in HF band (ms^2) */
    float lfOverHf;    /**< LF/HF ratio */
    float totPwr;    /**< Total power (ms^2) */
} FreqDomainHrvMetrics;

/**
* @ingroup hrv_metric_calculator
* @brief   HRV metric calculator output data structure
*/
typedef struct _MxmHrvOutData {
    TimeDomainHrvMetrics timeDomainMetrics;    /**< Time domain HRV metrics */
    FreqDomainHrvMetrics freqDomainMetrics;    /**< Frequency domain HRV metrics */
    int percentCompleted;    /**< 0 to 100 Progress indicator  */
    bool isHrvCalculated;    /**< Flag that indicates if the content of the output is valid */
} MxmHrvOutData;


/**
* @ingroup hrv_metric_calculator
* @brief   Initialization function for HRV Metric Calculator
*
* This function must be called once, at the beginning of the flow.
*
* @param     [in] instance    Pointer to an HRV Metric Calculator instance
* @param     [in] config    Pointer to a configuration structure
*
* @return    Return code as defined in @ref HrvMetricCalcRet
*/
HrvMetricCalcRet initHrvMetricCalc(HrvMetricCalculator *instance, HrvMetricCalcConfig *config);

/**
* @ingroup hrv_metric_calculator
* @brief   Top level run function for HRV Metric Calculator Module
*
* This function must be called for providing every preprocessed IBI sample to the metric calculator.
*
* @param     [in] instance    Pointer to an HRV Metric Calculator instance
* @param     [in] inData    Pointer to an input data structure
* @param     [out] outData    Pointer to an output data structure to be filled
*
* @return    Return code as defined in @ref HrvMetricCalcRet
*/
HrvMetricCalcRet runHrvMetricCalc(HrvMetricCalculator *instance, HrvMetricCalcInData *inData, MxmHrvOutData *outData);

/**
* @ingroup hrv_metric_calculator
* @brief   Termination function for HRV Metric Calculator Module
*
* This function must be called once, at the end of the flow.
*
* @param     [in] instance    Pointer to an HRV Metric Calculator instance
*
* @return    Return code as defined in @ref HrvMetricCalcRet
*/
HrvMetricCalcRet endHrvMetricCalc(HrvMetricCalculator *instance);


/* static functions */
#ifdef TEST_HRV
    /**
    * @ingroup hrv_metric_calculator
    * @brief   Checks if a window is sufficiently populated with IBIs
    *
    * @param     [in] instance    Pointer to an IBI container instance
    * @param     [in] hrvMetricCalcWindowSizeInSec Window size used in HRV
    *                                              metric calculation (in sec.)
    *
    * @return    True for sufficiently populated window, false otherwise
    */
    bool isIbiPopulationSufficient(IbiContainer* instance,
                                   const uint16_t hrvMetricCalcWindowSizeInSec);

    /**
    * @ingroup hrv_metric_calculator
    * @brief   Checks what percentage of buffer is populated so far
    *
    * @param     [in] instance    Pointer to an IBI container instance
    * @param     [in] hrvMetricCalcWindowSizeInSec Window size used in HRV
    *                                              metric calculation (in sec.)
    *
    * @return    Completion percentage for the buffers, going from 0 to 100 when suffiently populated.
    */
float checkBufferOccupancyRate(IbiContainer* instance,
                               const uint16_t hrvMetricCalcWindowSizeInSec);

    /**
    * @ingroup hrv_metric_calculator
    * @brief   Checks what percentage measurement is completed compared to @p hrvInstance->config.windowSizeInSec
    *
    * @param     [in] hrvInstance Pointer to an HRV calculator instance
    * @param     [in] lastTime    time index of last IBI input
    *
    * @return    Completion percentage for the measurement time
    */
    float checkMeasurementTimeProgress(HrvMetricCalculator *hrvInstance, float lastTime);

    /**
    * @ingroup hrv_metric_calculator
    * @brief   Manages if a window is ready for HRV calculation
    *
    * @param     [in] instance    Pointer to an HRV Metric Calculator instance
    * @param     [in] lastTime    Time of the final sample in sec
    * @param     [out] ready    Output flag to indicate whether the window is ready for HRV calculation
    *
    * @return    Return code as defined in @ref HrvMetricCalcRet
    */
    HrvMetricCalcRet isIbiWindowReady(HrvMetricCalculator *instance, float lastTime, bool *ready);

    /**
    * @ingroup hrv_metric_calculator
    * @brief   Manages window shifting after HRV calculation
    *
    * @param     [in] instance    Pointer to an HRV Metric Calculator instance
    *
    * @return    Return code as defined in @ref HrvMetricCalcRet
    */
    HrvMetricCalcRet  shiftWindow(HrvMetricCalculator *instance);

    /**
    * @ingroup hrv_metric_calculator
    * @brief   Calculates time domain HRV metrics
    *
    * @param     [in] ibis    IBI array
    * @param     [in] size    Size of the array
    * @param     [out] out    Pointer to an output data structure
    *
    * @return    This is a void function.
    */
    void calcTimeDomainMetrics(float *ibis, int16_t size, TimeDomainHrvMetrics *out);

    /**
    * @ingroup hrv_metric_calculator
    * @brief   Calculates frequency domain HRV metrics
    *
    * @param     [in] ibis    IBI array
    * @param     [in] times    Time array
    * @param     [in] size    Size of the arrays
    * @param     [out] out    Pointer to an output data structure
    *
    * @return    Return code as defined in @ref HrvMetricCalcRet
    */
    HrvMetricCalcRet calcFreqDomainMetrics(float *ibis,
        float *times, int16_t size, FreqDomainHrvMetrics *out);

    /**
    * @ingroup hrv_metric_calculator
    * @brief   Converts the periodogram to frequency domain HRV metrics
    *
    * @param     [in] powX    Abscissa array of the periodogram
    * @param     [in] powY    Ordinate array of the periodogram
    * @param     [in] size    Size of the arrays
    * @param     [out] out    Pointer to an output data structure
    *
    * @return    This is a void function.
    */
    void spectrum2Hrv(float *powX, float *powY, int size, FreqDomainHrvMetrics *out);

    /**
    * @ingroup hrv_metric_calculator
    * @brief   Calculates all HRV metrics
    *
    * @param     [in] instance    Pointer to an HRV Metric Calculator instance
    * @param     [in] outData    Pointer to an output data structure to be filled
    *
    * @return    Return code as defined in @ref HrvMetricCalcRet
    */
    HrvMetricCalcRet calculateHrvMetrics(HrvMetricCalculator *instance, MxmHrvOutData *outData);

    #define HRV_TESTABLE_STATIC
#else
    #define HRV_TESTABLE_STATIC static
#endif


#ifdef __cplusplus    /* If this is a C++ compiler, use C linkage */
}
#endif

#endif    /* __HRV_METRIC_CALCULATOR_H__ */

