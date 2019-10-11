//
// Created by Tonguc.Catakli on 10/10/2019.
//
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "mxm_respiration_rate_manager.h"

#define TAG "RESPIRATORY_RATE_ALGO"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__)

static mxm_respiration_rate_manager_init_str initData;
static mxm_respiration_rate_manager_in_data_str inputData;
static mxm_respiration_rate_manager_out_data_str outputData;

static jmethodID updateOutputMethodId;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1respiratory_1rate_RespiratoryRateAlgorithm_init(JNIEnv *env, jclass type, jint sourceOptions, jint ledCodes, jint samplingRateOption) {

    initData.signal_source_option = (mxm_respiration_rate_manager_ppg_source_options) sourceOptions;
    initData.led_code = (mxm_respiration_rate_manager_led_codes) ledCodes;
    initData.sampling_rate = (mxm_respiration_rate_manager_sampling_rate_option) samplingRateOption;

    mxm_respiration_rate_manager_return_code returnCode = mxm_respiration_rate_manager_init(&initData);

    if (returnCode != MXM_RESPIRATION_RATE_MANAGER_SUCCESS) {
        LOGD("initRespiratoryRateAlgo -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    LOGD("initRespiratoryRateAlgo -> SUCCESS");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1respiratory_1rate_RespiratoryRateAlgorithm_run(JNIEnv *env, jclass type, jfloat ppg, jfloat ibi, jfloat ibiConfidence,
                                                                                   jboolean ppgUpdateFlag, jboolean ibiUpdateFlag, jobject output) {

    inputData.ppg = ppg;
    inputData.ibi = ibi;
    inputData.ibi_confidence = ibiConfidence;
    inputData.ppg_update_flag = ppgUpdateFlag;
    inputData.ibi_update_flag = ibiUpdateFlag;

    mxm_respiration_rate_manager_return_code returnCode = mxm_respiration_rate_manager_run(&inputData, &outputData);

    env->CallVoidMethod(output, updateOutputMethodId,
                        outputData.respiration_rate, outputData.confidence_level);

    if (returnCode != MXM_RESPIRATION_RATE_MANAGER_SUCCESS) {
        LOGD("runRespiratoryRateAlgo -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    return JNI_TRUE;

}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1respiratory_1rate_RespiratoryRateAlgorithm_end(JNIEnv *env,
                                                                                   jclass type) {
    mxm_respiration_rate_manager_return_code returnCode = mxm_respiration_rate_manager_end();

    if (returnCode != MXM_RESPIRATION_RATE_MANAGER_SUCCESS) {
        LOGD("endRespiratoryRateAlgo -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    LOGD("endRespiratoryRateAlgo -> SUCCESS");
    return JNI_TRUE;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass outputClass = env->FindClass(
            "com/maximintegrated/algorithm_respiratory_rate/RespiratoryRateAlgorithmOutput");

    updateOutputMethodId = env->GetMethodID(outputClass, "update",
                                            "(FF)V");

    return JNI_VERSION_1_6;
}