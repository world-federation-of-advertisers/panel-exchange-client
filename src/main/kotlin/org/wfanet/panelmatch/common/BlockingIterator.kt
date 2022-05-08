package org.wfanet.panelmatch.common

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.channels.ChannelIterator
import kotlinx.coroutines.runBlocking

/** [Iterator] that wraps [channelIterator] using [runBlocking]. */
class BlockingIterator<T>(
  private val channelIterator: ChannelIterator<T>,
  private val context: CoroutineContext = EmptyCoroutineContext
) : Iterator<T> {
  override fun hasNext(): Boolean = runBlocking(context) { channelIterator.hasNext() }

  override fun next(): T =
    runBlocking(context) {
      // Note that ChannelIterator throws IllegalStateException when next() is called without a
      // preceding call to hasNext(). Thus we call hasNext() to avoid that exception and better
      // conform to Iterator's contract by throwing NoSuchElementException when the iterator is
      // empty.
      if (hasNext()) {
        channelIterator.next()
      } else {
        throw NoSuchElementException("No remaining elements")
      }
    }
}
