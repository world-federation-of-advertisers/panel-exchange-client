package org.wfanet.panelmatch.client.storage

import com.google.protobuf.ByteString

/** Interface for DataWriter adapter. */
interface DataWriter {

  /**
   * Writes output data into given path.
   *
   * @param path String location of data to write to.
   * @throws IOException
   */
  suspend fun write(path: String, data: ByteString)
}
