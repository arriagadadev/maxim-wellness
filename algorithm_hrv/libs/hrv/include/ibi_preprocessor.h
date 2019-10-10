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
* @file ibi_preprocessor.h
* @date August 2019
* @brief IBI Preprocessor Module header file
*/

/**
* @defgroup ibi_preprocessor Maxim HRV IBI Preprocessor Module
* @brief    IBI Preprocessor Module for artifact removal
*
* This module is responsible for preprocessing the IBI signal
* to remove artifacts so that the cleaned signal is suitable for
* HRV metric calculation.
*/

#ifndef __IBI_PREPROCESSOR_H__
#define __IBI_PREPROCESSOR_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>
#include "ibi_container.h"

/**
* @ingroup ibi_preprocessor
*/
/**@{*/
#define IBI_PREP_MED_FILT_SIZE 5    /**< Median filter size used for eliminating spikes */
#define IBI_PERCENT_TOLERANCE 20    /**< Percent difference threshold between median value and window center to declare a spike */
#define IBI_GAP_THRESHOLD 10000    /**< Temporal gap in ms to restart median filter for spike removal */

#define LOWER_IBI_LIMIT 300    /**< Lower limit in ms for the shortest acceptable IBI, corresponds to 200 bpm */
#define UPPER_IBI_LIMIT 2000    /**< Upper limit in ms for the longest acceptable IBI, corresponds to 30 bpm */

#define EARLY_SAMPLE_THRESHOLD 300    /**< Discard an IBI sample if it arrives this much earlier than the estimated time */

#define IBI_CONFIDENCE_DEFAULT_THRESHOLD 75     /**< Signal quality threshold for an IBI to be valid in 0 to 100 range */
/**@}*/

/**
* @ingroup ibi_preprocessor
* @brief   Return codes for IBI Preprocessor Module
*/
typedef enum _IbiPrepRet {
    IBI_PREP_SUCCESS,
    IBI_PREP_NULL_PTR_ERROR,
    IBI_PREP_IBI_CONTAINER_ERROR,
    IBI_PREP_INVALID_CONFIG_ERROR
} IbiPrepRet;


/**
* @ingroup ibi_preprocessor
* @brief   IBI preprocessor instance definition
*/
typedef struct _IbiPreprocessor {
    IbiContainer medianFiltWindow;    /**< Median filter instance */
    float ibis[IBI_PREP_MED_FILT_SIZE];    /**< External IBI buffer for the median filter instance (in ms) */
    float times[IBI_PREP_MED_FILT_SIZE];    /**< External Time buffer for the median filter instance (in ms) */
    float prevTime;    /**< Timing of the previous IBI sample in ms */
    float samplingPeriod;    /**< Sampling (clock) period in ms */
    int confidenceLimit;     /**< IBI Signal quality threshold */
} IbiPreprocessor;

/**
* @ingroup ibi_preprocessor
* @brief   IBI preprocessor configuration structure
*/
typedef struct _IbiPrepConfig {
    float samplingPeriod;    /**< Sampling (clock) period in ms */
    int confidenceLimit;     /**< IBI Signal quality threshold */
} IbiPrepConfig;

/**
* @ingroup ibi_preprocessor
* @brief   Input data structure for IBI preprocessing
*/
typedef struct _IbiPrepInData {
    float ibi;    /**< New inter beat interval in ms */
    float time;    /**< Timing of the sample in ms */
    int confidence;  /**< IBI Signal quality (0 to 100) */
} IbiPrepInData;

/**
* @ingroup ibi_preprocessor
* @brief   Input data structure for IBI preprocessing
*/
typedef struct _IbiPrepOutData {
    float ibi;    /**< Filtered inter beat interval in ms */
    float time;    /**< Modified finer timing of the sample in ms */
    bool isValid;    /**< Flag that indicates if the output sample is valid */
} IbiPrepOutData;


/**
* @ingroup ibi_preprocessor
* @brief   Initialization function for IBI preprocessor
*
* This function must be called once, at the beginning of the flow.
*
* @param     [in] instance    Pointer to the IBI preprocessor instance
* @param     [in] config    Pointer to the configuration structure
*
* @return    Return code as defined in @ref IbiPrepRet
*/
IbiPrepRet initIbiPreprocessor(IbiPreprocessor *instance, IbiPrepConfig *config);

/**
* @ingroup ibi_preprocessor
* @brief   Top level run function for IBI Preprocessor
*
* This function must be called for providing every IBI sample to the preprocessor.
*
* @param     [in] instance    Pointer to the IBI preprocessor instance
* @param     [in] inData    Pointer to the input data structure
* @param     [out] outData    Pointer to the output data structure to be filled
*
* @return    Return code as defined in @ref IbiPrepRet
*/
IbiPrepRet runIbiPreprocessor(IbiPreprocessor *instance, IbiPrepInData *inData, IbiPrepOutData *outData);

/**
* @ingroup ibi_preprocessor
* @brief   Termination function for IBI Preprocessor
*
* This function must be called once, at the end of the flow.
*
* @param     [in] instance    Pointer to the IBI preprocessor instance
*
* @return    Return code as defined in @ref IbiPrepRet
*/
IbiPrepRet endIbiPreprocessor(IbiPreprocessor *instance);


/* static functions */
#ifdef TEST_HRV
    /**
    * @ingroup ibi_preprocessor
    * @brief   Manages finer timing of the IBI sample
    *
    * @param     [in] instance    Pointer to the IBI preprocessor instance
    * @param     [in] inData    Pointer to the input data structure
    *
    * @return    Fine time in ms or -1 for invalid samples
    */
    float manageSampleTiming(IbiPreprocessor *instance, const IbiPrepInData *inData);

    /**
    * @ingroup ibi_preprocessor
    * @brief   Eliminates spikes and manages if a clean IBI is added to the output
    *
    * @param     [in] instance    Pointer to the IBI preprocessor instance
    * @param     [in] ibi    New IBI value in ms
    * @param     [in] time    Timing of the sample in ms
    * @param     [out] outData    Pointer to the output data structure to be filled
    *
    * @return    Return code as defined in @ref IbiPrepRet
    */
    IbiPrepRet eliminateSpikes(IbiPreprocessor *instance, float ibi, float time, IbiPrepOutData *outData);

    /**
    * @ingroup ibi_preprocessor
    * @brief   Manages storing provided IBI values and detecting gaps
    *
    * @param     [in] instance    Pointer to the IBI preprocessor instance
    * @param     [in] ibi    New IBI value in ms
    * @param     [in] time    Timing of the sample in ms
    *
    * @return    Return code as defined in @ref IbiPrepRet
    */
    IbiPrepRet pushToIbiContainer(IbiPreprocessor *instance, float ibi, float time);

    /**
    * @ingroup ibi_preprocessor
    * @brief   Identifies spikes and fills the output structure with clean samples
    *
    * @param     [in] instance    Pointer to the IBI preprocessor instance
    * @param     [out] outData    Pointer to the output data structure to be filled
    *
    * @return    Return code as defined in @ref IbiPrepRet
    */
    IbiPrepRet identifySpikes(IbiPreprocessor *instance, IbiPrepOutData *outData);

    #define HRV_TESTABLE_STATIC
#else
    #define HRV_TESTABLE_STATIC static
#endif


#ifdef __cplusplus    /* If this is a C++ compiler, use C linkage */
}
#endif

#endif    /* __IBI_PREPROCESSOR_H__ */

