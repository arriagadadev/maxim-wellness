
#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

#include "mxm_hrv_public.h"
#include "mxm_respiration_rate_manager.h"
#include "mxm_stress_monitoring.h"
#include "mxm_sleep_manager.h"

#include "AlgoWrapper.h"
#include "SSauthentication.h"

//#define PRINT_DEBUG_MSG

#ifdef PRINT_DEBUG_MSG
  #define ERRGPRINT(...)  fprintf (stderr, __VA_ARGS__);
  #define DEBUGPRINT(...) printf(__VA_ARGS__);
#else
  #define ERRGPRINT(...)
  #define DEBUGPRINT(args...)
#endif


#define MXM_ALGOSUITE_ENABLE_HRV_    ( 1 << 0)
#define MXM_ALGOSUITE_ENABLE_RESP_   ( 1 << 1)
#define MXM_ALGOSUITE_ENABLE_SLEEP_  ( 1 << 2)
#define MXM_ALGOSUITE_ENABLE_STRESS_ ( 1 << 3)
#define MXM_ALGOSUITE_ENABLE_SPORTS_ ( 1 << 4)


#define MXM_HRV_API  MXM_ALGOSUITE_API
#define MXM_HRV_CALL MXM_ALGOSUITE_CALL
#define MXM_RRM_API  MXM_ALGOSUITE_API
#define MXM_RRM_CALL MXM_ALGOSUITE_CALL
#define SLEEP_MANAGER_API MXM_ALGOSUITE_API
#define SLEEP_MANAGER_CALL MXM_ALGOSUITE_CALL
#define LIBMXMSTRESS_EXPORTED MXM_ALGOSUITE_API

const mxm_algosuite_version_str ALGO_SUITE_VERSION = {"1.0.0",1,0,0};

/***************************************************************************************************************
 **************************************** HRV ALGORITHM DEFINITIONS ********************************************
 ***************************************************************************************************************/


typedef struct _MxmHrvInData {
    float ibi;    /**< Inter Beat Interval, in ms */
    int ibiConfidence; /**< IBI quality in percentage score */
    bool isIbiValid;    /**< Flag that indicates whether the provided IBI value is valid */
} MxmHrvInData;

extern const MXM_HRV_API MxmHrvConfig defaultHrvConfig;

MXM_HRV_API MxmHrvRet MXM_HRV_CALL initMxmHrvPublic(MxmHrvConfig *config);
MXM_HRV_API MxmHrvRet MXM_HRV_CALL runMxmHrvPublic(MxmHrvInData *inData, MxmHrvOutData *outData);
MXM_HRV_API MxmHrvRet MXM_HRV_CALL endMxmHrvPublic();


/***************************************************************************************************************
 *************************************** RESPIRATION ALGORITHM DEFINITIONS *************************************
 ***************************************************************************************************************/


typedef bool boolean_t;

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
* @brief   Input data structure for running respiration rate estimator
*/
typedef struct _mxm_respiration_rate_manager_in_data_str {
    float ppg; /**< PPG value*/
    float ibi; /**< Inter-beat-inteval value*/
    float ibi_confidence; /**< Confidence of IBI information*/
    boolean_t ppg_update_flag; /**< If PPG value updated in this sample*/
    boolean_t ibi_update_flag; /**< If IBI value updated in this sample*/
} mxm_respiration_rate_manager_in_data_str;

MXM_RRM_API mxm_respiration_rate_manager_return_code MXM_RRM_CALL mxm_respiration_rate_manager_init( const mxm_respiration_rate_manager_init_str *const init_str );
MXM_RRM_API mxm_respiration_rate_manager_return_code MXM_RRM_CALL mxm_respiration_rate_manager_run( const mxm_respiration_rate_manager_in_data_str *const data_in_str,
																								    mxm_respiration_rate_manager_out_data_str *const data_out_str );
MXM_RRM_API mxm_respiration_rate_manager_return_code MXM_RRM_CALL mxm_respiration_rate_manager_end();
MXM_RRM_API mxm_respiration_rate_manager_return_code MXM_RRM_CALL mxm_respiration_rate_manager_get_version( mxm_respiration_rate_manager_version_str *const version_str );



/***************************************************************************************************************
 ****************************************** STRESS ALGORITHM DEFINITIONS ***************************************
 ***************************************************************************************************************/


extern const LIBMXMSTRESS_EXPORTED mxm_stress_monitoring_config
    mxm_stress_monitoring_default_config;


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


extern mxm_stress_monitoring_return_code LIBMXMSTRESS_EXPORTED mxm_stress_monitoring_init(mxm_stress_monitoring_config const* const config_ptr);
extern mxm_stress_monitoring_return_code LIBMXMSTRESS_EXPORTED mxm_stress_monitoring_run( mxm_mxm_stress_monitoring_run_input const* const input_ptr,mxm_mxm_stress_monitoring_run_output* const output_ptr);
extern mxm_stress_monitoring_return_code LIBMXMSTRESS_EXPORTED mxm_stress_monitoring_end(void);
extern void LIBMXMSTRESS_EXPORTED mxm_stress_monitoring_get_version(mxm_stress_monitoring_version* const version_ptr);



/***************************************************************************************************************
 ******************************************* SLEEP ALGORITHM DEFINITIONS ***************************************
 ***************************************************************************************************************/

/**
* @public
* @ingroup sleepQAManager
* @brief   Sleep Quality Assessment Manager version structure
*/
typedef struct _mxm_sleep_manager_version_str {
    char versionStrn[20];    /**< version in vXX.XX.XX format */
    unsigned short int version;		    /**< The first number in vXX.XX.XX format */
    unsigned short int subVersion;   	/**< The second number in vXX.XX.XX format */
    unsigned short int subsubVersion;   /**< The third number in vXX.XX.XX format */
} mxm_sleep_manager_version_str;

/**
* @public
* @ingroup sleepQAManager
* @brief   Sleep Quality Assessment Manager Supported Input Activity Types
*
* This enumaration defines the supported activities.
* Algorithm supports only REST and WALKING activities to process. Any other activity types
* should be marked as OTHER ACTIVITY.
*/
typedef enum _mxm_sleep_manager_activity_type {

    MXM_SLEEP_MANAGER_ACTIVITY_NOT_READY,    /**< When activity output is not ready */
    MXM_SLEEP_MANAGER_REST_ACTIVITY,        /**< No or very light activity  */
    MXM_SLEEP_MANAGER_OTHER_ACTIVITY       /**< Other activities           */
} mxm_sleep_manager_activity_type;

/**
* @public
* @ingroup sleepQAManager
* @brief   Sleep Quality Assessment Manager Input Data Structure
*/
typedef struct _mxm_sleep_manager_input_data_str {

    float hr;                            /**< Instant HR value of subject (bpm)*/
    float hr_conf_level;                 /**< Confidence Level of instant HR value of subject. Range:0-100.
                                         Higher values correponds to confident measurements.*/
    bool is_hr_updated;                  /**< Update flag of the HR*/
    float interbeat_interval;            /**< Instant inter-beat interval (IBI) value of subject (ms) */
    float interbeat_interval_conf_level; /**< Confidence Level of instant inter-beat interval (IBI)
                                         value of subject. Range:0-100. Higher values corresponds
                                         confident measurements */
    bool is_interbeat_interval_updated; /**< Update flag of the IBI*/
    float respiration_rate;             /**< Reserved for future use*/
    bool is_respiration_rate_updated;   /**< Reserved for future use*/
    float spo2; 						/**< Reserved for future use*/
    bool is_spo2_updated;				/**< Reserved for future use*/
    mxm_sleep_manager_activity_type activity_type; /**< Activity of subject */
    float mean_accelerometer_magnitude; /**< Mean magnitude of the accelerometer data in milli-g units*/
    bool is_accelerometer_updated; 		/**< Update flag of the accelerometer*/

} mxm_sleep_manager_input_data_str;


SLEEP_MANAGER_API mxm_sleep_manager_return SLEEP_MANAGER_CALL mxm_sleep_manager_init(mxm_sleep_manager_config* sleep_confg);
SLEEP_MANAGER_API mxm_sleep_manager_return SLEEP_MANAGER_CALL mxm_sleep_manager_run(mxm_sleep_manager_input_data_str *input_str_ptr, mxm_sleep_manager_output_data_str *output_str_ptr);
SLEEP_MANAGER_API mxm_sleep_manager_return SLEEP_MANAGER_CALL mxm_sleep_manager_end();

SLEEP_MANAGER_API void SLEEP_MANAGER_CALL ver_mxm_sleep_manager(mxm_sleep_manager_version_str *version_ptr);

SLEEP_MANAGER_API mxm_sleep_manager_return SLEEP_MANAGER_CALL mxm_sleep_manager_calculate_SQI(
                                                                    float deep_time_in_sec,
                                                                    float rem_time_in_sec,
                                                                    float in_sleep_wake_time_in_sec,
                                                                    int number_of_wake_in_sleep,
                                                                    float *output_sleep_quality_index);


#ifdef __cplusplus
}
#endif


#if defined(SINGLE_API_FOR_ALGORITHMS)

typedef struct {

	const char *name;
	uint8_t idx; /* do not modify */
	void *data;
	void *init_config;

    int  (*algo_manager_init)(void *ptr);
	int  (*algo_manager_run)(void *ptr, void *ptr); /* Runs algorithms with data in shared buffer */
	void (*algo_manager_end)(void);
	//int  (*get_version)(version_t *version);

	int (*get_sample_rate)(void *data);
	int (*set_sample_rate)(void *data, int sample_rate);
	int (*set_algo_cfg)(uint8_t *params, int param_sz);
	int (*get_algo_cfg)(uint8_t params, uint8_t *tx_buf, int tx_buf_sz);


} algo_t;

#endif

