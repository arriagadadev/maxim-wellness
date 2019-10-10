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

#ifndef LOMB_PERIODOGRAM_H
#define LOMB_PERIODOGRAM_H

#ifdef __cplusplus
extern "C" {
#endif

typedef float floatlp_t;    // General floating point precision
typedef float trprec_t;    // Floating point precision for trigonometric recurrences

#define EXTIRP_FACTOR 3

/* this macro computes the nearest upper power of 2 for an 32-bit unsigned value. Ex: 17->32, 64->64 */
#define POW2_CEIL_LP(v) (1 + \
(((((((((v) - 1) | (((v) - 1) >> 0x10) | \
      (((v) - 1) | (((v) - 1) >> 0x10) >> 0x08)) | \
     ((((v) - 1) | (((v) - 1) >> 0x10) | \
      (((v) - 1) | (((v) - 1) >> 0x10) >> 0x08)) >> 0x04))) | \
   ((((((v) - 1) | (((v) - 1) >> 0x10) | \
      (((v) - 1) | (((v) - 1) >> 0x10) >> 0x08)) | \
     ((((v) - 1) | (((v) - 1) >> 0x10) | \
      (((v) - 1) | (((v) - 1) >> 0x10) >> 0x08)) >> 0x04))) >> 0x02))) | \
 ((((((((v) - 1) | (((v) - 1) >> 0x10) | \
      (((v) - 1) | (((v) - 1) >> 0x10) >> 0x08)) | \
     ((((v) - 1) | (((v) - 1) >> 0x10) | \
      (((v) - 1) | (((v) - 1) >> 0x10) >> 0x08)) >> 0x04))) | \
   ((((((v) - 1) | (((v) - 1) >> 0x10) | \
      (((v) - 1) | (((v) - 1) >> 0x10) >> 0x08)) | \
     ((((v) - 1) | (((v) - 1) >> 0x10) | \
      (((v) - 1) | (((v) - 1) >> 0x10) >> 0x08)) >> 0x04))) >> 0x02))) >> 0x01))))

typedef enum _LPReturn {
    LP_SUCCESS,
    LP_NOT_POWER_OF_2_ERR,
    LP_FACTORIAL_TABLE_OOB_ERROR,
    LP_OUTPUT_ARRAY_SIZE_ERR
} LPReturn;

LPReturn fft_1d(floatlp_t *complexIn, const int size, const int signOfJ);

LPReturn fftOfRealSig(floatlp_t *realIn, const int size, const int signOfJ);

LPReturn extirp(const floatlp_t yIn, floatlp_t *yOut, const int size, const floatlp_t xIn, const int extirpFactor);

LPReturn lombPeriodogramCore(floatlp_t *xIn, floatlp_t *yIn, const int sizeIn,
    floatlp_t *extirp1, floatlp_t *extirp2, const int sizeExtirped,
    const floatlp_t oversamplingFactor, const floatlp_t upperFreqFactor,
    floatlp_t *powX, floatlp_t *powY, const int sizePowArr, int *sizeOut);

void freqSizes(const int sizeIn, const floatlp_t oversamplingFactor,
    const floatlp_t upperFreqFactor, int *sizeExtirped);

LPReturn lombPeriodogram(floatlp_t *xIn, floatlp_t *yIn, const int sizeIn,
    const floatlp_t oversamplingFactor, const floatlp_t upperFreqFactor,
    floatlp_t *powX, floatlp_t *powY, const int sizePowArr, int *sizeOut);

#ifdef __cplusplus // If this is a C++ compiler, use C linkage
}
#endif

#endif