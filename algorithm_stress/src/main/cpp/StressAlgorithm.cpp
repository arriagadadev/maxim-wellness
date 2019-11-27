//
// Created by Meliksah.Cakir on 11/26/2019.
//


#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <malloc.h>
#include "mxm_stress_monitoring.h"

#define TAG "STRESS_ALGO"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__)

static jmethodID updateOutputMethodId;
static mxm_mxm_stress_monitoring_run_input inputData;
static mxm_mxm_stress_monitoring_run_output outputData;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1stress_StressAlgorithm_stress_1init(JNIEnv *env, jclass clazz) {
    mxm_stress_monitoring_return_code returnCode =
            mxm_stress_monitoring_init(&mxm_stress_monitoring_default_config);

    if (returnCode != MXM_STRESS_MONITORING_SUCCESS) {
        LOGD("initStressAlgo -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    LOGD("initStressAlgo -> SUCCESS");
    return JNI_TRUE;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1stress_StressAlgorithm_stress_1run(JNIEnv *env, jclass clazz,
                                                                       jfloatArray input,
                                                                       jobject output) {
    float *runinput = env->GetFloatArrayElements(input, 0);

    // Clear used variables:
    memset(&inputData, 0, sizeof(mxm_mxm_stress_monitoring_run_input));
    memset(&outputData, 0, sizeof(mxm_mxm_stress_monitoring_run_output));

    //TODO : Fill run_input structure with runinput float array
    inputData.features.time_domain_hrv_metrics.avnn = runinput[0];
    inputData.features.time_domain_hrv_metrics.sdnn = runinput[1];
    inputData.features.time_domain_hrv_metrics.rmssd = runinput[2];
    inputData.features.time_domain_hrv_metrics.pnn50 = runinput[3];
    inputData.features.freq_domain_hrv_metrics.ulf = runinput[4];
    inputData.features.freq_domain_hrv_metrics.vlf = runinput[5];
    inputData.features.freq_domain_hrv_metrics.lf = runinput[6];
    inputData.features.freq_domain_hrv_metrics.hf = runinput[7];
    inputData.features.freq_domain_hrv_metrics.totPwr = runinput[8];

    mxm_stress_monitoring_return_code returnCode = mxm_stress_monitoring_run(&inputData,
                                                                             &outputData);

    if (returnCode != MXM_STRESS_MONITORING_SUCCESS) {
        LOGD("StressAlgo -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    // this is a routine to calculate lfhf based stress score and combining with tree results.
    // can be removed later on when this portion is implemented to stress algo

    float lfhfval = runinput[6] / runinput[7];
    int lfhfindex = 18;
    if (lfhfval > 0.01)
        lfhfindex = 17;
    if (lfhfval > 0.0763)
        lfhfindex = 16;
    if (lfhfval > 0.1994)
        lfhfindex = 15;
    if (lfhfval > 0.3226)
        lfhfindex = 14;
    if (lfhfval > 0.4458)
        lfhfindex = 13;
    if (lfhfval > 0.5689)
        lfhfindex = 12;
    if (lfhfval > 0.6921)
        lfhfindex = 11;
    if (lfhfval > 0.8153)
        lfhfindex = 10;
    if (lfhfval >= 0.9384)
        lfhfindex = 9;
    if (lfhfval > 1.3167)
        lfhfindex = 8;
    if (lfhfval > 1.9500)
        lfhfindex = 7;
    if (lfhfval > 2.5833)
        lfhfindex = 6;
    if (lfhfval > 3.2167)
        lfhfindex = 5;
    if (lfhfval > 3.8500)
        lfhfindex = 4;
    if (lfhfval > 4.4833)
        lfhfindex = 3;
    if (lfhfval > 5.1167)
        lfhfindex = 2;
    if (lfhfval > 5.7500)
        lfhfindex = 1;
    if (lfhfval > 6.3833)
        lfhfindex = 0;

    env->CallVoidMethod(output, updateOutputMethodId,
                        outputData.stress_class,
                        (int) (outputData.stress_score / 2.0 + lfhfindex / 2.0),
                        outputData.stress_score_prc);
    env->ReleaseFloatArrayElements(input, runinput, 0);

    return JNI_TRUE;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1stress_StressAlgorithm_stress_1end(JNIEnv *env, jclass clazz) {

    mxm_stress_monitoring_return_code returnCode = mxm_stress_monitoring_end();

    if (returnCode != MXM_STRESS_MONITORING_SUCCESS) {
        LOGD("endStressAlgo -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    LOGD("endStressAlgo -> SUCCESS");
    return JNI_TRUE;
}


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass outputClass = env->FindClass(
            "com/maximintegrated/algorithm_stress/StressAlgorithmOutput");

    updateOutputMethodId = env->GetMethodID(outputClass, "update",
                                            "(ZIF)V");

    return JNI_VERSION_1_6;
}