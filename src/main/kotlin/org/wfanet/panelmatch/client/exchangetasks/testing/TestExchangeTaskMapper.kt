package org.wfanet.panelmatch.client.exchangetasks.testing

import org.wfanet.panelmatch.client.exchangetasks.ExchangeTaskMapper

fun makeExchangeTaskMapper(): ExchangeTaskMapper {
  return ExchangeTaskMapper(
    validationTasks = FakeValidationTasks(),
    commutativeEncryptionTasks = FakeCommutativeEncryptionTasks(),
    mapReduceTasks = FakeMapReduceTasks(),
    generateKeysTasks = FakeGenerateKeyTasks(),
    privateStorageTasks = FakePrivateStorageTasks(),
    sharedStorageTasks = FakeSharedStorageTasks()
  )
}
