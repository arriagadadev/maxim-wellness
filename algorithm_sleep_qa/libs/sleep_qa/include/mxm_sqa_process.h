/*******************************************************************************
* Copyright (C) 2018 Maxim Integrated Products, Inc., All Rights Reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a
* copy of this software and associated documentation files (the "Software"),
* to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense,
* and/or sell copies of the Software, and to permit persons to whom the
* Software is furnished to do so, subject to the following conditions:
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

#ifdef __cplusplus
extern "C" {
#endif

#ifndef MXM_SQA_PROCESS_H
#define MXM_SQA_PROCESS_H

#include <stdint.h>
#ifdef _WIN32
#ifdef SLEEP_MANAGER_EXPORTS
#define SLEEP_MANAGER_API __declspec(dllexport)
#elif defined SLEEP_MANAGER_IMPORTS
#define SLEEP_MANAGER_API __declspec(dllimport)
#else
#define SLEEP_MANAGER_API
#endif
#define SLEEP_MANAGER_CALL __cdecl
#else    /* Android */
#define SLEEP_MANAGER_API __attribute__((__visibility__("default")))
#define SLEEP_MANAGER_CALL
#endif

/*Error codes 1-127 are preserved for SQA internal errors*/
typedef enum _mxm_sqa_return {
    MXM_SQA_SUCCESS = 0,                  /**< Success return code */
    MXM_SQA_FILE_OPEN_ERROR = 128,      /**< File cannot be opened */
    MXM_SQA_SLEEP_NOT_FOUND = 129,      /**< Algorithm did not found a sleep state in the given data */
} mxm_sqa_return;

SLEEP_MANAGER_API int ProcessMaxim(const char* input_file_str,
        const char* output_file_str,
        uint16_t age,
        uint16_t height,
        uint16_t weight,
        uint16_t gender_t,
        float resting_hr);


#endif

#ifdef __cplusplus    /* If this is a C++ compiler, use C linkage */
}
#endif
