/*
 * SSauthentication.h
 *
 *  Created on: Oct 31, 2019
 *      Author: Yagmur.Gok
 */

#ifndef INTERFACES_SSAUTHENTICATION_SSAUTHENTICATION_H_
#define INTERFACES_SSAUTHENTICATION_SSAUTHENTICATION_H_

#include <stddef.h>

#define SHA256_BLOCK_SIZE  32            // SHA256 outputs a 32 byte digest
#define SSAUTHENT_KEY_SIZE 20

/**************************** DATA TYPES ****************************/
typedef unsigned char BYTE;             // 8-bit byte
typedef unsigned int  WORD32;             // 32-bit word, change to "long" for 16-bit machines
typedef volatile unsigned int REGTYPE;
typedef unsigned long long    LONGWORD;

typedef struct {
	BYTE data[64];
	WORD32 datalen;
	unsigned long long bitlen;
	WORD32 state[8];
} MXM_SHA256_CTX;

typedef struct {
	WORD32 P;
	WORD32 G;
	WORD32 K;
	WORD32  localPublicKey;
	WORD32  producedNonce;
	WORD32  remotePublicKey;
} DH_PARAMS;


void mxm_sha256_init  (MXM_SHA256_CTX *ctx);
void mxm_sha256_update(MXM_SHA256_CTX *ctx, const BYTE data[], int len);
void mxm_sha256_final (MXM_SHA256_CTX *ctx, BYTE hash[]);


LONGWORD dh_compute( LONGWORD a,  WORD32 m,  WORD32 n);
void shuffleDeshuffle( const BYTE* const input , BYTE* output , const int iolen );

extern DH_PARAMS sessionAuthParams;
extern BYTE isAuthenticationCompleted;

#endif /* INTERFACES_SSAUTHENTICATION_SSAUTHENTICATION_H_ */
