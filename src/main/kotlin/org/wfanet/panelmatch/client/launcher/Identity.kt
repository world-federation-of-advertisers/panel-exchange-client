package org.wfanet.panelmatch.client.launcher

import org.wfanet.measurement.api.v2alpha.ExchangeWorkflow.Party

data class Identity(val id: String, val party: Party) {
  init {
    require(party == Party.DATA_PROVIDER || party == Party.MODEL_PROVIDER)
  }
}
