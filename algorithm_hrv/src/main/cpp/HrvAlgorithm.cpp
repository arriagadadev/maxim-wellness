//
// Created by Tonguc.Catakli on 10/9/2019.
//
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "mxm_hrv.h"
#include "hrv_metric_calculator.h"

#define TAG "HRV_ALGO"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__)

static MxmHrvConfig initData;
static MxmHrvInData inputData;
static MxmHrvOutData outputData;
static MxmHrvModule hrvInst;

static jmethodID updateOutputMethodId;

volatile jmethodID gRunMxmHrvRet;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1hrv_HrvAlgorithm_init( JNIEnv* env, jclass type, jfloat samplingPeriod, jshort windowSizeInSec, jshort windowShiftSizeInSec){

    initData.samplingPeriod = samplingPeriod;
    initData.windowSizeInSec = windowSizeInSec;
    initData.windowShiftSizeInSec = windowShiftSizeInSec;

    MxmHrvRet returnCode = initMxmHrv(&hrvInst, &initData);

    if(returnCode != MXM_HRV_SUCCESS){
        LOGD("initHrvAlgo -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    LOGD("initHrvAlgo -> SUCCESS");
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1hrv_HrvAlgorithm_run( JNIEnv* env, jclass type, jfloat ibi, jint ibiConfig, jboolean isIbiValid,
                                                          jobject output){
    inputData.ibi = ibi;
    inputData.ibiConfidence = ibiConfig;
    inputData.isIbiValid = isIbiValid;

    MxmHrvRet returnCode = runMxmHrv(&hrvInst, &inputData, &outputData);

    bool isHrvCalculated = outputData.isHrvCalculated;

    TimeDomainHrvMetrics *timeDomainHrvMetrics = &outputData.timeDomainMetrics;
    FreqDomainHrvMetrics *freqDomainHrvMetrics = &outputData.freqDomainMetrics;

    env->CallVoidMethod(output, updateOutputMethodId, timeDomainHrvMetrics->avnn, timeDomainHrvMetrics->sdnn, timeDomainHrvMetrics->rmssd, timeDomainHrvMetrics->pnn50,
            freqDomainHrvMetrics->ulf, freqDomainHrvMetrics->vlf, freqDomainHrvMetrics->lf,freqDomainHrvMetrics->hf, freqDomainHrvMetrics->lfOverHf, freqDomainHrvMetrics->totPwr,
            outputData.percentCompleted, outputData.isHrvCalculated);


    if (returnCode != MXM_HRV_SUCCESS) {
        LOGD("runHrvAlgo -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1hrv_HrvAlgorithm_end(JNIEnv *env, jclass type){
    MxmHrvRet returnCode = endMxmHrv(&hrvInst);
    if (returnCode != MXM_HRV_SUCCESS) {
        LOGD("endHrvAlgo -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    LOGD("endHrvAlgo -> SUCCESS");
    return JNI_TRUE;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass outputClass = env->FindClass(
            "com/maximintegrated/algorithm_hrv/HrvAlgorithmOutput");

    updateOutputMethodId = env->GetMethodID(outputClass, "update",
                                            "(FFFFFFFFFFIZ)V");


    return JNI_VERSION_1_6;
}




