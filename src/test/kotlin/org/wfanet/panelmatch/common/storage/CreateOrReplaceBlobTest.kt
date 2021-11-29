package org.wfanet.panelmatch.common.storage

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.kotlin.toByteStringUtf8
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.measurement.storage.filesystem.FileSystemStorageClient
import org.wfanet.panelmatch.common.testing.runBlockingTest

private const val BLOB_KEY = "some-blob-key"
private val BLOB = "some-blob-contents".toByteStringUtf8()

@RunWith(JUnit4::class)
class CreateOrReplaceBlobTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  private val storage by lazy { FileSystemStorageClient(temporaryFolder.root) }

  @Test
  fun create() = runBlockingTest {
    val blob = storage.createOrReplaceBlob(BLOB_KEY, BLOB)
    assertThat(blob.toByteString()).isEqualTo(BLOB)
    assertThat(storage.getBlob(BLOB_KEY)?.toByteString()).isEqualTo(BLOB)
  }

  @Test
  fun replace() = runBlockingTest {
    storage.createBlob(BLOB_KEY, BLOB.concat(BLOB))

    val blob = storage.createOrReplaceBlob(BLOB_KEY, BLOB)
    assertThat(blob.toByteString()).isEqualTo(BLOB)
    assertThat(storage.getBlob(BLOB_KEY)?.toByteString()).isEqualTo(BLOB)
  }

  @Test
  fun replaceManyTimes() = runBlockingTest {
    storage.createBlob(BLOB_KEY, BLOB.concat(BLOB))

    // This is to detect bugs around filesystem flushing.
    repeat(10) { storage.createOrReplaceBlob(BLOB_KEY, BLOB) }

    assertThat(storage.getBlob(BLOB_KEY)?.toByteString()).isEqualTo(BLOB)
  }
}
