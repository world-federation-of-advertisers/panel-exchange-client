package org.wfanet.panelmatch.client.exchangetasks

import org.wfanet.panelmatch.client.common.ExchangeContext

interface GenerateKeysTasks {

  fun getGenerateSymmetricKeyTask(): ExchangeTask

  fun getGenerateLookupKeysTask(): ExchangeTask

  fun ExchangeContext.getGenerateSerializedRlweKeysStepTask(): ExchangeTask

  fun ExchangeContext.getGenerateExchangeCertificateTask(): ExchangeTask
}
