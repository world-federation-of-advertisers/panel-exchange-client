// Copyright 2021 The Cross-Media Measurement Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wfanet.panelmatch.common.beam

import com.google.protobuf.ByteString
import com.google.protobuf.CodedOutputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel
import java.security.PrivateKey
import java.security.Signature
import org.apache.beam.sdk.io.FileIO
import org.apache.beam.sdk.io.WriteFilesResult
import org.apache.beam.sdk.transforms.PTransform
import org.apache.beam.sdk.values.PCollection
import org.wfanet.measurement.common.crypto.jceProvider
import org.wfanet.panelmatch.common.toByteString

class WriteSignedFiles(
  private val directoryUri: String,
  private val prefix: String,
  private val numShards: Int
) : PTransform<PCollection<ByteString>, WriteFilesResult<Void>>() {
  override fun expand(input: PCollection<ByteString>): WriteFilesResult<Void> {
    val fileSpec = "$directoryUri/$prefix-*-of-%05d".format(numShards)

    return input.apply(
      FileIO.write<ByteString>()
        .to(directoryUri)
        .withNumShards(numShards)
        .withPrefix(prefix)
        .via(SignedFileSink(fileSpec))
    )
  }
}

private class SignedFileSink(private val fileSpec: String, privateKey: PrivateKey) :
  FileIO.Sink<ByteString> {
  private lateinit var outputStream: OutputStream
  private lateinit var codedOutput: CodedOutputStream
  private val signer: Signature =
    Signature.getInstance(privateKey.algorithm, jceProvider).apply { initSign(privateKey) }

  override fun open(channel: WritableByteChannel) {
    outputStream = Channels.newOutputStream(channel)
    codedOutput = CodedOutputStream.newInstance(outputStream)
  }

  override fun write(element: ByteString) {
    codedOutput.writeBoolNoTag(true)
    codedOutput.writeBytesNoTag(element)
    signer.update(element.asReadOnlyByteBuffer())
  }

  override fun flush() {
    codedOutput.writeBoolNoTag(false)

    val fileSpecBytes = fileSpec.toByteString()
    codedOutput.writeBytesNoTag(fileSpecBytes)
    signer.update(fileSpecBytes.asReadOnlyByteBuffer())

    codedOutput.writeByteArrayNoTag(signer.sign())
  }
}
