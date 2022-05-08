package org.wfanet.panelmatch.common

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.produceIn
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.wfanet.panelmatch.common.testing.runBlockingTest

@RunWith(JUnit4::class)
class BlockingIteratorTest {

  @Test
  fun hasNextReturnsFalseForEmptyIterator() = runBlockingTest {
    val iterator = blockingIteratorOf()

    assertThat(iterator.hasNext()).isFalse()
  }

  @Test
  fun hasNextReturnsTrueForNewNonEmptyIterator() = runBlockingTest {
    val iterator = blockingIteratorOf("cat")

    assertThat(iterator.hasNext()).isTrue()
  }

  @Test
  fun hasNextReturnsTrueForPartiallyConsumedIterator() = runBlockingTest {
    val iterator = blockingIteratorOf("cat", "dog", "fox")
    iterator.next()

    assertThat(iterator.hasNext()).isTrue()
  }

  @Test
  fun hasNextReturnsFalseForConsumedIterator() = runBlockingTest {
    val iterator = blockingIteratorOf("cat", "dog", "fox")
    iterator.next()
    iterator.next()
    iterator.next()

    assertThat(iterator.hasNext()).isFalse()
  }

  @Test
  fun hasNextDoesNotConsumeElements() = runBlockingTest {
    val iterator = blockingIteratorOf("cat", "dog", "fox")

    assertThat(iterator.hasNext()).isTrue()
    assertThat(iterator.next()).isEqualTo("cat")
  }

  @Test
  fun nextReturnsNextItem() = runBlockingTest {
    val iterator = blockingIteratorOf("cat", "dog", "fox")
    iterator.next()
    iterator.next()

    assertThat(iterator.next()).isEqualTo("fox")
  }

  @Test
  fun nextThrowsNoSuchElementExceptionWhenConsumed() = runBlockingTest {
    val iterator = blockingIteratorOf("cat", "dog", "fox")
    iterator.next()
    iterator.next()
    iterator.next()

    assertFailsWith<NoSuchElementException> { iterator.next() }
  }

  private fun CoroutineScope.blockingIteratorOf(vararg items: String): BlockingIterator<String> {
    @OptIn(FlowPreview::class) val channel = flowOf(*items).produceIn(this)
    return BlockingIterator(channel.iterator(), this.coroutineContext)
  }
}
