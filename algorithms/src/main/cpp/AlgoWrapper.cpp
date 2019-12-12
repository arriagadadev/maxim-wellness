/*
 * AlgoIntegration.cpp
 *
 *  Created on: Nov 25, 2019
 *      Author: Yagmur.Gok
 */


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include "../../../libs/algorithms/include/mxm_hrv_public.h"
#include "../../../libs/algorithms/include/mxm_respiration_rate_manager.h"
#include "../../../libs/algorithms/include/mxm_stress_monitoring.h"
#include "../../../libs/algorithms/include/mxm_stress_monitoring.h"


#include "../../../libs/algorithms/include/SSauthentication.h"
#include "../../../libs/algorithms/include/AlgoWrapper.h"
#include "../../../libs/algorithms/include/AlgoWrapperInternal.h"

#define MXM_ALGOSUITE_ENABLE_HRV_    ( 1 << 0)
#define MXM_ALGOSUITE_ENABLE_RESP_   ( 1 << 1)
#define MXM_ALGOSUITE_ENABLE_SLEEP_  ( 1 << 2)
#define MXM_ALGOSUITE_ENABLE_STRESS_ ( 1 << 3)
#define MXM_ALGOSUITE_ENABLE_SPORTS_ ( 1 << 4)


//#define ASYNCHRONOUS_IOMEM_ACCESS

extern DH_PARAMS sessionAuthParams;

/*
uint8_t  __attribute__((visibility("hidden"))) isAuthenticationCompleted = 0;
*/
static unsigned char gAlgoStatusTracker = 0;


uint8_t *get_authentication_status(void) {
    static uint8_t isAuthenticationCompleted = 0;
    return &isAuthenticationCompleted;
}

void enable_authentication(void) {
    DEBUGPRINT("\r\n Authenticated to sensorhub \r\n");
    uint8_t *ptr = get_authentication_status();
    *ptr = 1;
    return;
}

void disable_authentication(void) {
    uint8_t *ptr = get_authentication_status();
    *ptr = 0;
    return;
}


static int calculate_local_response(const uint8_t *challange, const uint8_t challange_sz,
                                    uint8_t *local_response, const uint8_t local_response_sz) {


    int ret;

    uint8_t local_digest_buf[SHA256_BLOCK_SIZE];
    uint8_t sha256_nonce_key_input[SHA256_BLOCK_SIZE];
    static const uint8_t sha256_secret_key[20] = {0xba, 0x78, 0x16, 0xbf, 0x8f, 0x01, 0xcf, 0xea,
                                                  0x41, 0x41,
                                                  0x40, 0xde, 0x5d, 0xae, 0x22, 0x23, 0xb0, 0x03,
                                                  0x61, 0xa3};

    memcpy(&sha256_nonce_key_input[0], &challange[0], SHA256_BLOCK_SIZE - SSAUTHENT_KEY_SIZE);
    memcpy(&sha256_nonce_key_input[SHA256_BLOCK_SIZE - SSAUTHENT_KEY_SIZE], &sha256_secret_key[0],
           SSAUTHENT_KEY_SIZE);

    MXM_SHA256_CTX ctx;
    mxm_sha256_init(&ctx);
    mxm_sha256_update(&ctx, &sha256_nonce_key_input[0], SHA256_BLOCK_SIZE);
    mxm_sha256_final(&ctx, local_digest_buf);

    if (local_response_sz >= SHA256_BLOCK_SIZE) {
        memcpy(local_response, &local_digest_buf[0], SHA256_BLOCK_SIZE);
        ret = 0;
    } else
        ret = -1;

    return ret;
}


void
mxm_algosuite_manager_getauthinitials(const uint8_t *const auth_inits, uint8_t *out_auth_initials) {

    //isAuthenticationCompleted = 0;

    uint8_t localPublicKey[12];
    //uint8_t shufflocalPublicKey[12];

    sessionAuthParams.P =
            (auth_inits[0] << 24) + (auth_inits[1] << 16) + (auth_inits[2] << 8) + auth_inits[3];
    sessionAuthParams.G = auth_inits[4];
    srand(sessionAuthParams.P);
    sessionAuthParams.K = rand() % 63;

    DEBUGPRINT("\r\n P= %d  G= %d  K= %d  RK=  %d \r\n", sessionAuthParams.P, sessionAuthParams.G,
               sessionAuthParams.K, (auth_inits[5]));

    sessionAuthParams.localPublicKey = dh_compute(sessionAuthParams.G, sessionAuthParams.K,
                                                  sessionAuthParams.P);

    DEBUGPRINT(" LOCAL PUBLIC = %d \r\n", sessionAuthParams.localPublicKey);

    localPublicKey[0] = localPublicKey[4] = localPublicKey[8] = (uint8_t) (
            (sessionAuthParams.localPublicKey >> 24) & 0x000000FF);
    localPublicKey[1] = localPublicKey[5] = localPublicKey[9] = (uint8_t) (
            (sessionAuthParams.localPublicKey >> 16) & 0x000000FF);
    localPublicKey[2] = localPublicKey[6] = localPublicKey[10] = (uint8_t) (
            (sessionAuthParams.localPublicKey >> 8) & 0x000000FF);
    localPublicKey[3] = localPublicKey[7] = localPublicKey[11] = (uint8_t) (
            (sessionAuthParams.localPublicKey >> 0) & 0x000000FF);

    // Shuffled output
    shuffleDeshuffle(localPublicKey, out_auth_initials, 12);

}

void
mxm_algosuite_manager_authenticate(const uint8_t *const auth_str1, const uint8_t *const auth_str2) {

    uint8_t LOCAL_DIGEST[32];
    uint8_t repRcvdPubKey[12];
    uint8_t NONCE[12];

    // Shuffled input
    shuffleDeshuffle(auth_str1, repRcvdPubKey, 12);

    uint32_t remotePublic =
            (repRcvdPubKey[0] << 24) + (repRcvdPubKey[1] << 16) + (repRcvdPubKey[2] << 8) +
            (repRcvdPubKey[3] << 0);
    uint32_t secretNonce = dh_compute(remotePublic, sessionAuthParams.K, sessionAuthParams.P);

    NONCE[0] = NONCE[4] = NONCE[8] = (uint8_t) ((secretNonce >> 24) & 0x000000FF);
    NONCE[1] = NONCE[5] = NONCE[9] = (uint8_t) ((secretNonce >> 16) & 0x000000FF);
    NONCE[2] = NONCE[6] = NONCE[10] = (uint8_t) ((secretNonce >> 8) & 0x000000FF);
    NONCE[3] = NONCE[7] = NONCE[11] = (uint8_t) ((secretNonce >> 0) & 0x000000FF);


    calculate_local_response(&NONCE[0], sizeof(NONCE), &LOCAL_DIGEST[0], sizeof(LOCAL_DIGEST));

#if defined(PRINT_DEBUG_MSG)
    DEBUGPRINT("LOCAL DIGEST = ");
    for(int i = 0; i< 32 ; i++)
        DEBUGPRINT("%02X", LOCAL_DIGEST[i]);
    DEBUGPRINT("\r\n");
#endif

    void (*authenticateCallBack)(void) = (memcmp(&LOCAL_DIGEST[0], auth_str2, 32) == 0)
                                         ? enable_authentication : disable_authentication;
    authenticateCallBack();

    return;

}

void mxm_algosuite_manager_init(const mxm_algosuite_init_data *const init_str,
                                mxm_algosuite_return_code *const status) {

    //printf("\r\n ALGO INIT AUTHENTICATION STATUS: %d = \r\n ", isAuthenticationCompleted);

    MxmHrvRet hrv_init_status = MXM_HRV_NOT_INITIALIZED_ERR;
    mxm_respiration_rate_manager_return_code resp_init_status = MXM_RESPIRATION_RATE_MANAGER_INIT_ERROR;
    mxm_stress_monitoring_return_code stress_init_status = MXM_STRESS_MONITORING_INVALID_CONFIG_ERR;
    mxm_sleep_manager_return sleep_init_status = MXM_SLEEP_MANAGER_ALGO_INIT_ERROR;

    //uint8_t* isAuthenticationCompleted =  get_authentication_status();

    if (*(get_authentication_status())) {

        if (init_str->enabledAlgorithms & MXM_ALGOSUITE_ENABLE_HRV) {
            hrv_init_status = initMxmHrvPublic((MxmHrvConfig *) &init_str->hrvConfig);
            if (hrv_init_status == MXM_HRV_SUCCESS)
                gAlgoStatusTracker |= MXM_ALGOSUITE_ENABLE_HRV_;

            if (init_str->enabledAlgorithms & MXM_ALGOSUITE_ENABLE_STRESS) {

                stress_init_status = mxm_stress_monitoring_init(&init_str->stressConfig);
                if (stress_init_status == MXM_STRESS_MONITORING_SUCCESS)
                    gAlgoStatusTracker |= MXM_ALGOSUITE_ENABLE_STRESS_;
            }

        }

        if (init_str->enabledAlgorithms & MXM_ALGOSUITE_ENABLE_RESP) {
            resp_init_status = mxm_respiration_rate_manager_init(&init_str->respConfig);
            if (resp_init_status == MXM_RESPIRATION_RATE_MANAGER_SUCCESS)
                gAlgoStatusTracker |= MXM_ALGOSUITE_ENABLE_RESP_;
        }

        if (init_str->enabledAlgorithms & MXM_ALGOSUITE_ENABLE_SLEEP_) {
            sleep_init_status = mxm_sleep_manager_init(
                    (mxm_sleep_manager_config *) &init_str->sleepConfig);
            if (sleep_init_status == MXM_SLEEP_MANAGER_SUCCESS)
                gAlgoStatusTracker |= MXM_ALGOSUITE_ENABLE_SLEEP_;
        }


    }

    status->hrv_status = hrv_init_status;
    status->resp_status = resp_init_status;
    status->stress_status = stress_init_status;
    status->sleep_status = sleep_init_status;


    //disable_authentication();  //isAuthenticationCompleted = 0;

    return;
}


static float get_accel_input(uint32_t accX, uint32_t accY, uint32_t accZ);

void mxm_algosuite_manager_run(const mxm_algosuite_input_data *const data_in_str,
                               mxm_algosuite_output_data *const data_out_str,
                               mxm_algosuite_return_code *const status) {

    static uint32_t sampleCnt = 0;


    MxmHrvRet hrv_run_status;
    mxm_respiration_rate_manager_return_code resp_run_status;
    mxm_stress_monitoring_return_code stress_run_status;
    mxm_sleep_manager_return sleep_run_status;

    MxmHrvOutData hrvOutput;
#ifndef ASYNCHRONOUS_IOMEM_ACCESS
    //MxmHrvOutData 							  hrvOutput;
    mxm_mxm_stress_monitoring_run_output stressOutput;
    mxm_respiration_rate_manager_out_data_str respOutput;
    mxm_sleep_manager_output_data_str sleepOutput;
#endif

    if (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_HRV_) {

        DEBUGPRINT("hrvrun ");
        MxmHrvInData hrvInput;
        hrvInput.ibi = (float) data_in_str->rr_interbeat_interval / 10.0;
        hrvInput.ibiConfidence = data_in_str->rr_confidence;
        hrvInput.isIbiValid = (bool) (data_in_str->rr_interbeat_interval > 0);
#ifndef ASYNCHRONOUS_IOMEM_ACCESS
        hrv_run_status = runMxmHrvPublic(&hrvInput, &hrvOutput);
#else
        hrv_run_status = runMxmHrvPublic(&hrvInput, &data_out_str->hrv_out_sample);
        status->hrv_status  = hrv_run_status;
#endif
        if (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_STRESS_ && hrvOutput.isHrvCalculated) {

            DEBUGPRINT("stressrun ");

            mxm_mxm_stress_monitoring_run_input stressInput;
            stressInput.features.freq_domain_hrv_metrics.hf = hrvOutput.freqDomainMetrics.hf;
            stressInput.features.freq_domain_hrv_metrics.lf = hrvOutput.freqDomainMetrics.lf;
            stressInput.features.freq_domain_hrv_metrics.ulf = hrvOutput.freqDomainMetrics.ulf;
            stressInput.features.freq_domain_hrv_metrics.vlf = hrvOutput.freqDomainMetrics.vlf;
            stressInput.features.freq_domain_hrv_metrics.totPwr = hrvOutput.freqDomainMetrics.totPwr;

            stressInput.features.time_domain_hrv_metrics.avnn = hrvOutput.timeDomainMetrics.avnn;
            stressInput.features.time_domain_hrv_metrics.pnn50 = hrvOutput.timeDomainMetrics.pnn50;
            stressInput.features.time_domain_hrv_metrics.rmssd = hrvOutput.timeDomainMetrics.rmssd;
            stressInput.features.time_domain_hrv_metrics.sdnn = hrvOutput.timeDomainMetrics.sdnn;
#ifndef ASYNCHRONOUS_IOMEM_ACCESS
            stress_run_status = mxm_stress_monitoring_run(&stressInput, &stressOutput);
#else
            //mxm_mxm_stress_monitoring_run_input* stressInputPtr = (mxm_mxm_stress_monitoring_run_input*) &data_out_str->hrv_out_sample;
            //stress_run_status = mxm_stress_monitoring_run( stressInputPtr, &data_out_str->stress_out_sample);
            stress_run_status = mxm_stress_monitoring_run( &stressInput, &data_out_str->stress_out_sample);
            status->stress_status = stress_run_status;
#endif
        }

    }

    if (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_RESP_) {

        DEBUGPRINT("resprun ");
        mxm_respiration_rate_manager_in_data_str respInput;

        respInput.ibi = (float) data_in_str->rr_interbeat_interval / 10.0;
        respInput.ibi_confidence = (float) data_in_str->rr_confidence;
        respInput.ibi_update_flag = (bool) (data_in_str->rr_interbeat_interval > 0);
        respInput.ppg = (float) data_in_str->grn_count;
        respInput.ppg_update_flag = true;

#ifndef ASYNCHRONOUS_IOMEM_ACCESS
        resp_run_status = mxm_respiration_rate_manager_run(&respInput, &respOutput);
#else
        resp_run_status = mxm_respiration_rate_manager_run( &respInput, &data_out_str->resp_out_sample);
        status->resp_status  = resp_run_status;
#endif
    }

    if (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_SLEEP) {


        DEBUGPRINT("sleeprun ");
        mxm_sleep_manager_input_data_str sleepInput;


        sleepInput.hr = (float) data_in_str->hearth_rate_estim;
        sleepInput.hr_conf_level = (float) data_in_str->hr_confidence;
        sleepInput.is_hr_updated = true;
        sleepInput.interbeat_interval = (float) data_in_str->rr_interbeat_interval;
        sleepInput.interbeat_interval_conf_level = (float) data_in_str->rr_confidence;
        sleepInput.is_interbeat_interval_updated = (data_in_str->rr_interbeat_interval > 0) ? true
                                                                                            : false;
        sleepInput.activity_type = (mxm_sleep_manager_activity_type) data_in_str->activity_class;
        sleepInput.mean_accelerometer_magnitude = get_accel_input(data_in_str->accelx,
                                                                  data_in_str->accely,
                                                                  data_in_str->accelz);
        sleepInput.is_accelerometer_updated = true;


#ifndef ASYNCHRONOUS_IOMEM_ACCESS
        if (sleepInput.mean_accelerometer_magnitude)
            sleep_run_status = mxm_sleep_manager_run(&sleepInput, &sleepOutput);
#else
        sleep_run_status =  MXM_SLEEP_MANAGER_SUCCESS; /* mxm_sleep_manager_run( &sleepInput, &data_out_str->sleep_out_Sample); */
        status->sleep_status  = sleep_run_status;
#endif
    }

#ifndef ASYNCHRONOUS_IOMEM_ACCESS
    data_out_str->hrv_out_sample = hrvOutput;
    data_out_str->stress_out_sample = stressOutput;
    data_out_str->resp_out_sample = respOutput;
    data_out_str->sleep_out_Sample = sleepOutput;

    status->hrv_status = hrv_run_status;
    status->resp_status = resp_run_status;
    status->stress_status = stress_run_status;
    status->sleep_status = sleep_run_status;
#endif


    return;
}

void mxm_algosuite_manager_end(const unsigned char tobeDisabledAlgorithms,
                               mxm_algosuite_return_code *const status) {

    MxmHrvRet hrv_end_status;
    mxm_respiration_rate_manager_return_code resp_end_status;
    mxm_stress_monitoring_return_code stress_end_status;
    mxm_sleep_manager_return sleep_end_status;

    if ((tobeDisabledAlgorithms & MXM_ALGOSUITE_ENABLE_HRV_) &&
        (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_HRV_)) {
        hrv_end_status = endMxmHrvPublic();
        if (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_STRESS_)
            stress_end_status = mxm_stress_monitoring_end();
    }
    if ((tobeDisabledAlgorithms & MXM_ALGOSUITE_ENABLE_HRV_) &&
        (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_STRESS_))
        stress_end_status = mxm_stress_monitoring_end();

    if ((tobeDisabledAlgorithms & MXM_ALGOSUITE_ENABLE_RESP_) &&
        (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_RESP_))
        resp_end_status = mxm_respiration_rate_manager_end();

    if ((tobeDisabledAlgorithms & MXM_ALGOSUITE_ENABLE_STRESS_) &&
        (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_STRESS_))
        stress_end_status = mxm_stress_monitoring_end();

    if ((tobeDisabledAlgorithms & MXM_ALGOSUITE_ENABLE_SLEEP_) &&
        (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_SLEEP_))
        sleep_end_status = mxm_sleep_manager_end();

    status->hrv_status = hrv_end_status;
    status->resp_status = resp_end_status;
    status->stress_status = stress_end_status;
    status->sleep_status = sleep_end_status;

    return;

}


void mxm_algosuite_manager_get_versions(mxm_algosuite_version_str *const version_str) {

    *version_str = ALGO_SUITE_VERSION;
    return;
}


void mxm_algosuite_manager_calculate_SQI(float deep_time_in_sec,
                                         float rem_time_in_sec,
                                         float in_sleep_wake_time_in_sec,
                                         int number_of_wake_in_sleep,
                                         float *output_sleep_quality_index,
                                         mxm_algosuite_return_code *const status) {

    mxm_sleep_manager_return sleep_sqi_calc_status = MXM_SLEEP_MANAGER_RUN_ALGO_ERROR;
    if (gAlgoStatusTracker & MXM_ALGOSUITE_ENABLE_SLEEP_)
        sleep_sqi_calc_status = mxm_sleep_manager_calculate_SQI(deep_time_in_sec, rem_time_in_sec,
                                                                in_sleep_wake_time_in_sec,
                                                                number_of_wake_in_sleep,
                                                                output_sleep_quality_index);

    status->sleep_status = sleep_sqi_calc_status;

    return;

}

static float get_accel_input(uint32_t accX, uint32_t accY, uint32_t accZ) {

    const float fsample_rate_inHz = 25.0;
    const uint32_t uisample_rate_inHz = 25;

    float ret = 0.0;
    static uint32_t cnt = 0;
    static float sum = 0;

    sum += sqrtf((float) (accX * accX + accY * accY + accZ * accZ)) / 1000.0;
    cnt += 1;
    if (cnt == uisample_rate_inHz) {
        cnt = 0;
        ret = sum / 25.0;
        sum = 0;
    }

    return ret;
}


