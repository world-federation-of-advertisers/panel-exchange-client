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

package org.wfanet.panelmatch.client.privatemembership

import com.google.protobuf.ByteString

/** Constructs a [ShardId]. */
fun shardIdOf(id: Int): ShardId = ShardId.newBuilder().setId(id).build()

/** Constructs a [BucketId]. */
fun bucketIdOf(id: Int): BucketId = BucketId.newBuilder().setId(id).build()

/** Constructs a [QueryId]. */
fun queryIdOf(id: Int): QueryId = QueryId.newBuilder().setId(id).build()

fun unencryptedQueryOf(shard: Int, query: Int, bucket: Int): UnencryptedQuery =
  unencryptedQueryOf(shardIdOf(shard), bucketIdOf(bucket), queryIdOf(query))

/** Constructs a [UnencryptedQuery]. */
fun unencryptedQueryOf(shardId: ShardId, bucketId: BucketId, queryId: QueryId): UnencryptedQuery =
  UnencryptedQuery.newBuilder()
    .setShardId(shardId)
    .setBucketId(bucketId)
    .setQueryId(queryId)
    .build()

/** Constructs a [DatabaseShard]. */
fun databaseShardOf(shardId: ShardId, buckets: Iterable<Bucket>): DatabaseShard =
  DatabaseShard.newBuilder().setShardId(shardId).addAllBuckets(buckets).build()

/** Constructs a [Bucket]. */
fun bucketOf(bucketId: BucketId, payload: ByteString): Bucket =
  Bucket.newBuilder().setBucketId(bucketId).setPayload(payload).build()

/** Constructs a [Result]. */
fun resultOf(queryMetadata: QueryMetadata, payload: ByteString): Result =
  Result.newBuilder().setQueryMetadata(queryMetadata).setPayload(payload).build()

/** Constructs a [QueryBundle]. */
fun queryBundleOf(
  shardId: ShardId,
  queryMetadata: Iterable<QueryMetadata>,
  payload: ByteString
): QueryBundle =
  QueryBundle.newBuilder()
    .setShardId(shardId)
    .addAllQueryMetadata(queryMetadata)
    .setPayload(payload)
    .build()

/** Constructs a [QueryMetadata]. */
fun queryMetadataOf(queryId: QueryId, metadata: ByteString): QueryMetadata =
  QueryMetadata.newBuilder().setQueryId(queryId).setMetadata(metadata).build()

/** Constructs a [DatabaseKey]. */
fun databaseKeyOf(id: Long): DatabaseKey = DatabaseKey.newBuilder().setId(id).build()

/** Constructs a [Plaintext]. */
fun plaintextOf(payload: ByteString): Plaintext = Plaintext.newBuilder().setPayload(payload).build()

/** Constructs a [PanelistKey]. */
fun panelistKeyOf(id: Long): PanelistKey = PanelistKey.newBuilder().setId(id).build()

/** Constructs a [JoinKey]. */
fun joinKeyOf(key: ByteString): JoinKey = JoinKey.newBuilder().setKey(key).build()