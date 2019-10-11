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
* @file mxm_respiration_rate_manager.h
* @date JUN 2019
* @brief Maxim Respiration Rate Measurement public API header file
*/

/**
* @defgroup respiration_rate_manager Respiration Rate Measurement API Module
* @brief    Defines Respiration Rate Measurement Public API.
*
* This module is in charge of handling Respiration Rate Measurement API
* related tasks and definitions.
*/

#ifndef _MXM_RESPIRATION_RATE_MANAGER_H_
#define _MXM_RESPIRATION_RATE_MANAGER_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

    /* Macros for library import/export */
#if defined(WIN32) || defined(_WIN32)
#ifdef MXM_RRM_EXPORTS
#define MXM_RRM_API __declspec(dllexport)
#elif defined MXM_RRM_IMPORTS
#define MXM_RRM_API __declspec(dllimport)
#else
#define MXM_RRM_API
#endif
#define MXM_RRM_CALL __cdecl
#else    /* Android */
#define MXM_RRM_API __attribute__((__visibility__("default")))
#define MXM_RRM_CALL
#endif


/*
* Type Definitions
*/
    typedef bool boolean_t;

    /**
    * @public
    * @ingroup respiration_rate_manager
    * @brief   Respiration rate return options
    */
    typedef enum _mxm_repiration_rate_manager_return_code {
        MXM_RESPIRATION_RATE_MANAGER_SUCCESS,    /**< Success return code */
        MXM_RESPIRATION_RATE_MANAGER_INIT_NULL_PTR_ERROR,    /**< NULL pointer error (during initialization) return code */
        MXM_RESPIRATION_RATE_MANAGER_INIT_ERROR,    /**< error (during initialization) return code */
        MXM_RESPIRATION_RATE_MANAGER_RUN_NULL_PTR_ERROR,    /**< NULL pointer error (during execution) return code */
        MXM_RESPIRATION_RATE_MANAGER_RUN_ERROR,    /**< NULL pointer error (during execution) return code */
        MXM_RESPIRATION_RATE_MANAGER_END_NULL_PTR_ERROR,    /**< NULL pointer error (during termination) return code */
        MXM_RESPIRATION_RATE_MANAGER_END_ERROR,   /**< NULL pointer error (during termination) return code */
        MXM_RESPIRATION_RATE_MANAGER_GET_VERSION_NULL_PTR_ERROR,    /**< NULL pointer error (during termination) return code */
    } mxm_respiration_rate_manager_return_code;

    /**
    * @public
    * @ingroup respiration_rate_manager
    * @brief   Respiration rate LED Codes
    *
    * This enumeration type is useful for identifying correct array indices for multiple LED channels.
    */
    typedef enum _mxm_respiration_rate_manager_led_codes {
        MXM_RESPIRATION_RATE_MANAGER_GREEN_LED,    /**< GREEN channel */
        MXM_RESPIRATION_RATE_MANAGER_IR_LED,       /**< IR channel */
        MXM_RESPIRATION_RATE_MANAGER_RED_LED,      /**< RED channel */
    } mxm_respiration_rate_manager_led_codes;

    /**
    * @public
    * @ingroup respiration_rate_manager
    * @brief   Respiration rate ppg source options
    */
    typedef enum _mxm_respiration_rate_manager_ppg_source_options {
        MXM_RESPIRATION_RATE_MANAGER_PPG_SOURCE_WRIST, /**< The algorithm will work with the PPG gathered from wrist. */
        MXM_RESPIRATION_RATE_MANAGER_PPG_SOURCE_FINGER /**< The algorithm will work with the PPG gathered from finger. */
    } mxm_respiration_rate_manager_ppg_source_options;

    /**
    * @public
    * @ingroup respiration_rate_manager
    * @brief   Respiration rate available sampling rate options
    */
    typedef enum _mxm_respiration_rate_manager_sampling_rate_option {
        MXM_RESPIRATION_RATE_MANAGER_SAMPLING_RATE_25_HZ = 25,   /**< 25 Hz sampling rate */
        MXM_RESPIRATION_RATE_MANAGER_SAMPLING_RATE_100_HZ = 100   /**< 100 Hz sampling rate */
    } mxm_respiration_rate_manager_sampling_rate_option;

    /**
    * @public
    * @ingroup respiration_rate_manager
    * @brief   Respiration rate software initialization structure
    */
    typedef struct _mxm_respiration_rate_manager_init_str {
        mxm_respiration_rate_manager_ppg_source_options signal_source_option; /**< The location of PPG signal source*/
        mxm_respiration_rate_manager_led_codes led_code; /**< The color/wavelength of PPG signal source*/
        mxm_respiration_rate_manager_sampling_rate_option sampling_rate; /**< Input PPG sampling rate*/
    } mxm_respiration_rate_manager_init_str;

    /**
    * @public
    * @ingroup respiration_rate_manager
    * @brief   Input data structure for running respiration rate estimator
    */
    typedef struct _mxm_respiration_rate_manager_in_data_str {
        float ppg; /**< PPG value*/
        float ibi; /**< Inter-beat-inteval value*/
        float ibi_confidence; /**< Confidence of IBI information*/
        boolean_t ppg_update_flag; /**< If PPG value updated in this sample*/
        boolean_t ibi_update_flag; /**< If IBI value updated in this sample*/
    } mxm_respiration_rate_manager_in_data_str;

    /**
    * @public
    * @ingroup respiration_rate_manager
    * @brief   Output data structure for running respiration rate estimator
    */
    typedef struct _mxm_respiration_rate_manager_out_data_str {
        float respiration_rate; /**< Output respiration rate with unit 'Breaths/Minute'*/
        float confidence_level; /**< Confidence level of the output in range of 0-100*/
    } mxm_respiration_rate_manager_out_data_str;

    /**
    * @public
    * @ingroup respiration_rate_manager
    * @brief   Version data structure for respiration rate software
    */
    typedef struct _mxm_respiration_rate_manager_version_str {
        char version_string[20]; /**< version in vXX.XX.XX format */
        unsigned short int version;  /**< The first number in vXX.XX.XX format */
        unsigned short int sub_version;  /**< The second number in vXX.XX.XX format */
        unsigned short int sub_sub_version;  /**< The third number in vXX.XX.XX format */
    } mxm_respiration_rate_manager_version_str;


    /**
    * @public
    * @ingroup respiration_rate_manager
    *
    * @brief     Initializes the respiration rate estimator
    *
    * @param     [in] init_str    Pointer to a structure that contains configuration of respiration rate software
    *
    * @return    Return code as defined in @ref mxm_respiration_rate_manager_return_code
    */
    MXM_RRM_API mxm_respiration_rate_manager_return_code MXM_RRM_CALL mxm_respiration_rate_manager_init(const mxm_respiration_rate_manager_init_str *const init_str);

    /**
    * @public
    * @ingroup respiration_rate_manager
    *
    * @brief     Runs the respiration rate estimator
    *
    * Typical algorithm flow should be as follows <br>
    * - Call ::mxm_respiration_rate_manager_init once for initialization
    * - Call ::mxm_respiration_rate_manager_run for every sample
    * - Call ::mxm_respiration_rate_manager_end once for termination
    *
    * @param     [in] data_in_str    Pointer to a structure that contains input data to run the respiration rate estimator
    * @param     [out] data_out_str    Pointer to a structure that contains output data filled by the respiration rate estimator
    * @return    Return code as defined in @ref mxm_respiration_rate_manager_return_code
    */
    MXM_RRM_API mxm_respiration_rate_manager_return_code MXM_RRM_CALL mxm_respiration_rate_manager_run(const mxm_respiration_rate_manager_in_data_str *const data_in_str, mxm_respiration_rate_manager_out_data_str *const data_out_str);

    /**
    * @public
    * @ingroup respiration_rate_manager
    *
    * @brief     Terminates the respiration rate estimator
    *
    * @return    Return code as defined in @ref mxm_respiration_rate_manager_return_code
    */
    MXM_RRM_API mxm_respiration_rate_manager_return_code MXM_RRM_CALL mxm_respiration_rate_manager_end();

    /**
    * @public
    * @ingroup respiration_rate_manager
    *
    * @brief     Returns version of the respiration rate measurement software
    *
    * @param     [out] version_str    Pointer to a structure that contains version data filled by the respiration rate estimator
    *
    * @return    Return code as defined in @ref mxm_respiration_rate_manager_return_code
    */
    MXM_RRM_API mxm_respiration_rate_manager_return_code MXM_RRM_CALL mxm_respiration_rate_manager_get_version(mxm_respiration_rate_manager_version_str *const version_str);

#ifdef __cplusplus
}
#endif

#endif /* _MXM_RESPIRATION_RATE_MANAGER_H_ */
