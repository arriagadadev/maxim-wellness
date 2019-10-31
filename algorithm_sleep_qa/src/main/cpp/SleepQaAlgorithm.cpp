//
// Created by Tonguc.Catakli on 10/26/2019.
//
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "mxm_sqa_process.h"

#define TAG "SLEEP_QA_ALGO"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_maximintegrated_algorithm_1sleep_1qa_SleepQaAlgorithm_run(JNIEnv *env, jclass clazz,
                                                                   jstring input_path,
                                                                   jstring output_path, jint age,
                                                                   jint height, jint weight,
                                                                   jint gender, jfloat resting_hr) {

    const char *inputPathStr = env->GetStringUTFChars(input_path, nullptr);
    const char *outputPathStr = env->GetStringUTFChars(output_path, nullptr);

    int returnCode = ProcessMaxim(inputPathStr, outputPathStr, age, height, weight, gender,
                                  resting_hr);

    if (returnCode == 128) {
        LOGD("endSleepAlgo - File cannot be opened  -> FAILURE(%d)", returnCode);
        return JNI_FALSE;
    }

    if (returnCode == 129) {
        LOGD("endSleepAlgo -Algorithm did not found a sleep state in the given data -> FAILURE(%d)",
             returnCode);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

