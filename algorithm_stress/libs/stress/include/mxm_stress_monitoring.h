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
********************************************************************************
*/

#ifndef __MXM_STRESS_MONITORING_H__
#define __MXM_STRESS_MONITORING_H__

#ifdef __cplusplus /* If this is a C++ compiler, use C linkage */
extern "C" {
#endif

/**
  * @file mxm_stress_monitoring.h
  * @public
  * @copyright Copyright/licensing notice (see Legal and Copyright Notices)
  */

/**
  * @defgroup mxm_stress_monitoring Maxim Stress Monitoring Module
  * @brief    Stress Monitoring Module
  *
  * This module presents the Maxim Stress Monitoring Module, API related
  * function definitions and declarations.
  */

#if defined _WIN32 || defined __CYGWIN__ || __GNUC__ < 4
#define LIBMXMSTRESS_EXPORTED
#else
#define LIBMXMSTRESS_EXPORTED __attribute__ ((visibility ("default")))
#endif

#include <stdbool.h>
#include <stdint.h>

typedef bool boolean_t;     /**< Boolean quantity        */
typedef float float32_t;    /**< Signed 32 bits quantity */

/**
  * @public
  * @ingroup mxm_stress_monitoring
  * @brief Maxim Stress Monitoring Module return codes
  */
typedef enum _mxm_stress_monitoring_return_code {

    MXM_STRESS_MONITORING_SUCCESS = 0,        /**< SUCCESS code               */
    MXM_STRESS_MONITORING_NULL_PTR_ERR,       /**< NULL pointer error         */
    MXM_STRESS_MONITORING_INVALID_CONFIG_ERR, /**< Configuration error        */
    MXM_STRESS_MONITORING_RUN_ERR,            /**< RUN error                  */

} mxm_stress_monitoring_return_code;

/**
  * @public
  * @ingroup mxm_stress_monitoring
  * @brief Maxim Stress Monitoring Module configuration structure
  */
typedef struct _mxm_stress_monitoring_config {

    uint8_t dummy_config_for_compilation;
} mxm_stress_monitoring_config;

/**
  * @public
  * @ingroup mxm_stress_monitoring
  * @brief Maxim Stress Monitoring Module default configuration variable
  *
  * Default configuration variable that can be used in
  * ::mxm_stress_monitoring_init function to initialize the algorithm with
  * default configuration values. <br>
  * Default configuration structure can also be copied by the client into a
  * mutable variable that is later to be used for altering the members of
  * interest only.
  */
extern const LIBMXMSTRESS_EXPORTED mxm_stress_monitoring_config
    mxm_stress_monitoring_default_config;

/**
  * @public
  * @ingroup mxm_stress_monitoring
  * @brief   Initializes stress monitoring algorithm
  *
  * @param [in]   config_ptr  Configuration structure pointer
  *
  * Throughout lifetime of algorithm (before calling
  * ::mxm_stress_monitoring_end) this initialization function should be called
  * exactly once also before processing any sample by
  * ::mxm_stress_monitoring_run function.
  *
  * @return Return code as defined in @ref mxm_stress_monitoring_return_code
  */
extern mxm_stress_monitoring_return_code LIBMXMSTRESS_EXPORTED
    mxm_stress_monitoring_init(
        mxm_stress_monitoring_config const* const config_ptr);

/**
  * @ingroup mxm_stress_monitoring
  * @brief   Time domain HRV metrics structure
  */
typedef struct _mxm_stress_monitoring_time_domain_hrv_metrics {

    float32_t avnn;     /**< Average of NN intervals in ms                    */
    float32_t sdnn;     /**< Standard deviation of NN intervals in ms         */
    float32_t rmssd;    /**< RMS value of successive differences in ms        */
    float32_t pnn50;    /**< Percentage of successive differences greater than
                                                                        50 ms */
} mxm_stress_monitoring_time_domain_hrv_metrics;

/**
  * @ingroup mxm_stress_monitoring
  * @brief   Frequency domain HRV metrics structure
  */
typedef struct _mxm_stress_monitoring_freq_domain_hrv_metrics {

    float32_t ulf;      /**< Power in ULF band: [0 0.0033] (ms^2)             */
    float32_t vlf;      /**< Power in VLF band: [0.0033 0.04] (ms^2)          */
    float32_t lf;       /**< Power in LF band: [0.04 0.15] (ms^2)             */
    float32_t hf;       /**< Power in HF band: [0.15 0.4] (ms^2)              */
    float32_t totPwr;   /**< Total power (ms^2)                               */

} mxm_stress_monitoring_freq_domain_hrv_metrics;

/**
  * @ingroup mxm_stress_monitoring
  * @brief   Stress Monitoring Module features structure
  */
typedef struct _mxm_stress_monitoring_features {

    mxm_stress_monitoring_time_domain_hrv_metrics time_domain_hrv_metrics; /**<
       Time domain HRV metrics                                                */
    mxm_stress_monitoring_freq_domain_hrv_metrics freq_domain_hrv_metrics; /**<
       Frequency domain HRV metrics                                           */

} mxm_stress_monitoring_features;

/**
  * @public
  * @ingroup mxm_stress_monitoring
  * @brief   Specifies the input structure for ::mxm_stress_monitoring_run
  *          function
  */
typedef struct _mxm_stress_monitoring_run_input {

    mxm_stress_monitoring_features features; /**< Features used in stress score
                                                  estimation                  */
} mxm_mxm_stress_monitoring_run_input;

/**
  * @public
  * @ingroup mxm_stress_monitoring
  * @brief   Specifies the output structure for ::mxm_stress_monitoring_run
  *          function
  */
typedef struct _mxm_stress_monitoring_run_output {

    boolean_t stress_class; /**< Binary stress/non-stress output */
    uint8_t stress_score;   /**< Integer stress score output in interval [0 18]:
                                 <br>
                                 Scores [0 8]: Represent stressful scores from
                                 highest to lowest levels where parasympathetic
                                 system is dominant                         <br>
                                 Scores [9 18]: Represent non-stressful scores
                                 from highest to lowest levels where sympathetic
                                 system is dominant                           */
    float32_t stress_score_prc;   /**< Stress score output as percentage      */

} mxm_mxm_stress_monitoring_run_output;

/**
  * @public
  * @ingroup mxm_stress_monitoring
  * @brief   Runs stress monitoring algorithm for given input
  *
  * @param [in]   input_ptr   input data structure pointer
  * @param [out]  output_ptr  output data structure pointer
  *
  * This function can be called anytime when the input structure fields are
  * meaningful (whenever related features can be calculated).
  *
  * @return Return code as defined in @ref mxm_stress_monitoring_return_code
  */
extern mxm_stress_monitoring_return_code LIBMXMSTRESS_EXPORTED
    mxm_stress_monitoring_run(
        mxm_mxm_stress_monitoring_run_input const* const input_ptr,
        mxm_mxm_stress_monitoring_run_output* const output_ptr);

/**
  * @public
  * @ingroup mxm_stress_monitoring
  * @brief   Ends stress monitoring algorithm
  *
  * @return Return code as defined in @ref mxm_stress_monitoring_return_code
  */
extern mxm_stress_monitoring_return_code LIBMXMSTRESS_EXPORTED
    mxm_stress_monitoring_end(void);


/**
* @public
* @ingroup mxm_stress_monitoring
* @brief   Maxim Stress Monitoring Module version structure
*
*/
typedef struct _mxm_stress_monitoring_version {
    uint8_t version;         /**< Version main number component               */
    uint8_t sub_version;     /**< Version sub-level number component          */
    uint8_t sub_sub_version; /**< Version sub-sub-level number component      */
} mxm_stress_monitoring_version;

/**
*
* @ingroup mxm_stress_monitoring
* @brief    This function returns the version of the algorithm as a three-level
*           structure: version, sub-version and sub-sub-version (XX.XX.XX) by
*           filling given reference to input @ref mxm_stress_monitoring_version
*           structure
*
* @b Note: If the input parameter @p version_ptr is NULL, function performs
*          no operation and @p version_ptr would still be NULL.
*
* @param [in, out] version_ptr Version structure instance pointer to be altered.
*/
extern void LIBMXMSTRESS_EXPORTED mxm_stress_monitoring_get_version(
    mxm_stress_monitoring_version* const version_ptr);

#ifdef __cplusplus /* If this is a C++ compiler, use C linkage */
}
#endif

#endif    /* #define __MXM_STRESS_MONITORING_H__ */
