package org.wfanet.panelmatch.common.certificates.utilities

import com.google.protobuf.Duration

class Utilities {

  private val SECONDS_IN_DAY = 86400

  fun getDurationProto(numberOfDays: Int): Duration {
    return Duration.newBuilder().setSeconds(numberOfDays.toLong() * SECONDS_IN_DAY).build()
  }

}
