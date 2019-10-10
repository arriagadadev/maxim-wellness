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
* @file mxm_hrv.h
* @date August 2019
* @brief Maxim HRV Module top level header file
*/

/**
* @defgroup mxm_hrv Maxim HRV Module
* @brief    Top level of the heart rate variability module
*
* This module encapsulates IBI preprocessing and HRV metric
* calculation functionalities.
*/

#ifndef __MXM_HRV_H__
#define __MXM_HRV_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>
#include "ibi_preprocessor.h"
#include "hrv_metric_calculator.h"

/**
* @ingroup mxm_hrv
* @brief   HRV top level module return codes
*/
typedef enum _MxmHrvRet {
    MXM_HRV_SUCCESS,
    MXM_HRV_NULL_PTR_ERROR,
    MXM_HRV_INVALID_CONFIG_ERROR,
    MXM_HRV_NON_POSITIVE_SAMPLING_PERIOD_ERROR,
    MXM_HRV_IBI_PREP_ERROR,
    MXM_HRV_METRIC_CALC_ERROR
} MxmHrvRet;

/**
* @ingroup mxm_hrv
* @brief   HRV Module configuration package
*/
typedef struct _MxmHrvConfig {
    float samplingPeriod;         /**< Sampling (clock) period in ms          */
    int16_t windowSizeInSec;      /**< HRV metric calculation window in sec   */
    int16_t windowShiftSizeInSec; /**< HRV metric calculation window shift size
                                       in sec                                 */
} MxmHrvConfig;

/**
* @ingroup mxm_hrv
* @brief   HRV Module instance definition
*/
typedef struct _MxmHrvModule {
    IbiPreprocessor preprocessor;         /**< IBI preprocessor instance      */
    HrvMetricCalculator metricCalculator; /**< HRV metric calculator instance */

    float currentTime;      /**< Module's internal clock in ms                */
    MxmHrvConfig config;    /**< Used configuration                           */
} MxmHrvModule;

/**
* @ingroup mxm_hrv
* @brief   Input data structure for running the HRV Module
*/
typedef struct _MxmHrvInData {
    float ibi;    /**< Inter Beat Interval, in ms */
    int ibiConfidence; /**< IBI quality in percentage score */
    bool isIbiValid;    /**< Flag that indicates whether the provided IBI value is valid */
} MxmHrvInData;

/**
* @ingroup mxm_hrv
* @brief   Default configuration package object (read-only)
*/
extern const MxmHrvConfig defaultHrvConfig;

/**
* @ingroup mxm_hrv
* @brief   Initialization function
*
* Initializes the HRV Module according to the provided configuration. This function should
* be called only once at the beginning of a typical flow.
*
* @param     [in] instance    Pointer to an HRV Module instance
* @param     [in] config    Pointer to a configuration package
*
* @return    Return code as defined in @ref MxmHrvRet
*/
MxmHrvRet initMxmHrv(MxmHrvModule *instance, MxmHrvConfig *config);

/**
* @ingroup mxm_hrv
* @brief   Run function
*
* Runs the HRV module with the input data and fills the output data structure
* with HRV metrics as they are calculated. <br>
* A typical flow starts with an initialization (@ref initMxmHrv). After that,
* run function is called for every clock period as defined during initialization.
* This should be done regardless of whether the user has a valid IBI value to
* provide to the algorithm, so that the algorithm can manage its internal timing
* information. The termination function (@ref endMxmHrv) should be called only once
* at the end. <br>
* Provided IBI values can have a finer resolution than the clock period.
*
* @param     [in] instance    Pointer to an HRV Module instance
* @param     [in] inData    Pointer to a structure that contains input data to run the HRV Module
* @param     [out] outData    Pointer to a structure that contains output data filled by the HRV Module
*
* @return    Return code as defined in @ref MxmHrvRet
*/
MxmHrvRet runMxmHrv(MxmHrvModule *instance, MxmHrvInData *inData, MxmHrvOutData *outData);

/**
* @ingroup mxm_hrv
* @brief   Termination function
*
* Terminates the HRV Module. This function should be called only once at the end of
* a typical flow.
*
* @param     [in] instance    Pointer to an HRV Module instance
*
* @return    Return code as defined in @ref MxmHrvRet
*/
MxmHrvRet endMxmHrv(MxmHrvModule *instance);

#ifdef __cplusplus    /* If this is a C++ compiler, use C linkage */
}
#endif

#endif    /* __MXM_HRV_H__ */

