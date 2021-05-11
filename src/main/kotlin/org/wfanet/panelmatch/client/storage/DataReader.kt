package org.wfanet.panelmatch.client.storage

import com.google.protobuf.ByteString

/** Interface for DataReader adapter. */
interface DataReader {

  /**
   * Reads input data from given path.
   *
   * @param path String location of input data to read from.
   * @return Input data.
   * @throws IOException
   */
  suspend fun read(path: String): Map<String, ByteString>
}
