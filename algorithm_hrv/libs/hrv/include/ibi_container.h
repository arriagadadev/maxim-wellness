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
* @file ibi_container.h
* @date August 2019
* @brief IBI Container Module header file
*/

/**
* @defgroup ibi_container Maxim HRV IBI Container Module
* @brief    IBI Container Module for storing IBI sequences
*
* This module is can store IBI sequences and related timing
* information for every element in the sequence. Necessary
* functionality to manage these sequences is also part of this
* module.
*/

#ifndef __IBI_CONTAINER_H__
#define __IBI_CONTAINER_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

/**
* @ingroup ibi_container
* @brief   IBI Container return codes
*/
typedef enum _IbiContRet {
    IBI_CONT_SUCCESS,
    IBI_CONT_NULL_PTR_ERROR,
    IBI_CONT_INVALID_CONFIG_ERROR,
    IBI_CONT_INVALID_OFFSET_WARNING,
    IBI_CONT_EMPTY_CONTAINER_WARNING
} IbiContRet;


/**
* @ingroup ibi_container
* @brief   IBI Container instance definition
*/
typedef struct _IbiContainer {
    float *ibis;    /**< Pointer for the external IBI buffer */
    float *times;    /**< Pointer for the external time buffer */
    int16_t begin;    /**< Beginning index of the sequence */
    int16_t end;    /**< End index of the sequence */
    int16_t size;    /**< Size of the container */
} IbiContainer;


/**
* @ingroup ibi_container
* @brief   Initialization function
*
* @param     [in] instance    Pointer to an IBI Container instance
* @param     [in] ibis    External IBI buffer address
* @param     [in] times    External time buffer address
* @param     [in] size    Size of the container to be initialized
*
* @return    Return code as defined in @ref IbiContRet
*/
IbiContRet initIbiContainer(IbiContainer *instance, float *ibis, float *times, int16_t size);

/**
* @ingroup ibi_container
* @brief   Termination function
*
* @param     [in] instance    Pointer to an IBI Container instance
*
* @return    Return code as defined in @ref IbiContRet
*/
IbiContRet endIbiContainer(IbiContainer *instance);

/**
* @ingroup ibi_container
* @brief   Resets the accumulated data without terminating the instance
*
* @param     [in] instance    Pointer to an IBI Container instance
*
* @return    Return code as defined in @ref IbiContRet
*/
IbiContRet resetIbiContainer(IbiContainer *instance);

/**
* @ingroup ibi_container
* @brief   Adds an IBI-time pair to the container
*
* @param     [in] instance    Pointer to an IBI Container instance
* @param     [in] ibi    IBI value
* @param     [in] time    Time value
*
* @return    Return code as defined in @ref IbiContRet
*/
IbiContRet writeIbiContainer(IbiContainer *instance, float ibi, float time);

/**
* @ingroup ibi_container
* @brief   Reads from the container
*
* @param     [in] instance    Pointer to an IBI Container instance
* @param     [in] offsetFromEnd    Positive offset from the end. 0 corresponds to the end.
* @param     [out] ibiOut    Read ibi value
* @param     [out] timeOut    Read time value
*
* @return    Return code as defined in @ref IbiContRet
*/
IbiContRet readIbiContainer(IbiContainer *instance, int16_t offsetFromEnd, float *ibiOut, float *timeOut);

/**
* @ingroup ibi_container
* @brief   Check if the container is empty
*
* @param     [in] instance    Pointer to an IBI Container instance
* @param     [out] empty    Flag that indicates whether the container is empty
*
* @return    Return code as defined in @ref IbiContRet
*/
IbiContRet isEmptyIbiContainer(IbiContainer *instance, bool *empty);

/**
* @ingroup ibi_container
* @brief   Checks if the container is full
*
* @param     [in] instance    Pointer to an IBI Container instance
* @param     [out] full    Flag that indicates whether the container is full
*
* @return    Return code as defined in @ref IbiContRet
*/
IbiContRet isFullIbiContainer(IbiContainer *instance, bool *full);

/**
* @ingroup ibi_container
* @brief   Checks if there is a single element in the container
*
* @param     [in] instance    Pointer to an IBI Container instance
* @param     [out] single    Flag that indicates whether the container has only one IBI-time pair
*
* @return    Return code as defined in @ref IbiContRet
*/
IbiContRet isSingleElementIbiContainer(IbiContainer *instance, bool *single);

/**
* @ingroup ibi_container
* @brief   Crops the beginning of the sequence up to the provided time
*
* @param     [in] instance    Pointer to an IBI Container instance
* @param     [in] cropUpToTime    Crops up to this time
* @param     [out] newBeginTime    New begin time (valid only if the resulting container is not empty)
* @param     [out] isEmpty    Flag that indicates whether the container is empty after the operation
*
* @return    Return code as defined in @ref IbiContRet
*/
IbiContRet cropBeginningIbiContainer(IbiContainer *instance, float cropUpToTime, float *newBeginTime, bool *isEmpty);

/**
* @ingroup ibi_container
* @brief   Returns number of IBI-time pairs in the sequence
*
* @param     [in] instance    Pointer to an IBI Container instance
*
* @return    Number of elements in the container
*/
int16_t nElementsIbiContainer(IbiContainer *instance);

#ifdef __cplusplus    /* If this is a C++ compiler, use C linkage */
}
#endif

#endif    /* __IBI_CONTAINER_H__ */

