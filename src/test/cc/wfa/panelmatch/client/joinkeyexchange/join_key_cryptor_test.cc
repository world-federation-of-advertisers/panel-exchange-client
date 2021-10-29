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

#include "wfa/panelmatch/client/joinkeyexchange/join_key_cryptor.h"

#include <google/protobuf/repeated_field.h>

#include <string>

#include "absl/base/port.h"
#include "absl/memory/memory.h"
#include "absl/status/status.h"
#include "absl/status/statusor.h"
#include "absl/types/span.h"
#include "common_cpp/testing/common_matchers.h"
#include "common_cpp/testing/status_macros.h"
#include "common_cpp/testing/status_matchers.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "wfa/panelmatch/client/joinkeyexchange/join_key_cryptor.pb.h"

namespace wfa::panelmatch::client::joinkeyexchange {
namespace {
using ::google::protobuf::RepeatedPtrField;
using ::testing::ContainerEq;
using ::testing::Eq;
using ::testing::Ne;
using ::testing::Not;
using ::testing::Pointwise;
using ::wfa::EqualsProto;
using ::wfa::IsOkAndHolds;

// TODO(stevenwarejones) Move to common-cpp
MATCHER_P(NotEqualsProto, expected, "") {
  ::google::protobuf::util::MessageDifferencer differencer;
  return !differencer.Compare(arg, expected);
}

TEST(PanelMatchTest, JoinKeyEncryptionUtility) {
  JoinKeyCryptorGenerateCipherKeyRequest generate_key_request1;
  ASSERT_OK_AND_ASSIGN(
      JoinKeyCryptorGenerateCipherKeyResponse generate_key_response1,
      JoinKeyGenerateCipherKey(generate_key_request1));
  std::string random_key_1 = generate_key_response1.key();

  JoinKeyCryptorGenerateCipherKeyRequest generate_key_request2;
  ASSERT_OK_AND_ASSIGN(
      JoinKeyCryptorGenerateCipherKeyResponse generate_key_response2,
      JoinKeyGenerateCipherKey(generate_key_request2));
  std::string random_key_2 = generate_key_response2.key();

  JoinKeyCryptorEncryptRequest encrypt_request1;
  encrypt_request1.set_encryption_key(random_key_1);
  JoinKeyAndId plaintext_join_key_and_id_1;
  plaintext_join_key_and_id_1.mutable_join_key()->set_key("some joinkey0");
  plaintext_join_key_and_id_1.mutable_join_key_identifier()->set_id(
      "some identifier0");
  *encrypt_request1.add_plaintext_join_key_and_ids() =
      plaintext_join_key_and_id_1;
  JoinKeyAndId plaintext_join_key_and_id_2;
  plaintext_join_key_and_id_2.mutable_join_key()->set_key("some joinkey1");
  plaintext_join_key_and_id_2.mutable_join_key_identifier()->set_id(
      "some identifier1");
  *encrypt_request1.add_plaintext_join_key_and_ids() =
      plaintext_join_key_and_id_2;
  JoinKeyAndId plaintext_join_key_and_id_3;
  plaintext_join_key_and_id_3.mutable_join_key()->set_key("some joinkey2");
  plaintext_join_key_and_id_3.mutable_join_key_identifier()->set_id(
      "some identifier2");
  *encrypt_request1.add_plaintext_join_key_and_ids() =
      plaintext_join_key_and_id_3;
  ASSERT_OK_AND_ASSIGN(JoinKeyCryptorEncryptResponse encrypted_response1,
                       JoinKeyEncrypt(encrypt_request1));
  const auto& encrypted_texts1 =
      encrypted_response1.encrypted_join_key_and_ids();

  JoinKeyCryptorEncryptRequest encrypt_request2;
  encrypt_request2.set_encryption_key(random_key_2);
  encrypt_request2.mutable_plaintext_join_key_and_ids()->CopyFrom(
      encrypt_request1.plaintext_join_key_and_ids());
  ASSERT_OK_AND_ASSIGN(JoinKeyCryptorEncryptResponse encrypted_response2,
                       JoinKeyEncrypt(encrypt_request2));
  const auto& encrypted_texts2 =
      encrypted_response2.encrypted_join_key_and_ids();
  JoinKeyCryptorEncryptResponse expected_encrypted_response2;
  expected_encrypted_response2.mutable_encrypted_join_key_and_ids()->CopyFrom(
      encrypted_texts1);
  ASSERT_THAT(encrypted_response2,
              NotEqualsProto(expected_encrypted_response2));

  JoinKeyCryptorReEncryptRequest reencrypt_request1;
  reencrypt_request1.set_encryption_key(random_key_1);
  reencrypt_request1.mutable_encrypted_join_key_and_ids()->CopyFrom(
      encrypted_texts2);
  ASSERT_OK_AND_ASSIGN(
      JoinKeyCryptorReEncryptResponse double_encrypted_response1,
      JoinKeyReEncrypt(reencrypt_request1));
  const auto& double_encrypted_texts1 =
      double_encrypted_response1.encrypted_join_key_and_ids();
  JoinKeyCryptorReEncryptResponse expected_double_encrypted_response1;
  expected_double_encrypted_response1.mutable_encrypted_join_key_and_ids()
      ->CopyFrom(encrypted_texts2);
  ASSERT_THAT(double_encrypted_response1,
              NotEqualsProto(expected_double_encrypted_response1));

  JoinKeyCryptorReEncryptRequest reencrypt_request2;
  reencrypt_request2.set_encryption_key(random_key_2);
  reencrypt_request2.mutable_encrypted_join_key_and_ids()->CopyFrom(
      encrypted_texts1);
  ASSERT_OK_AND_ASSIGN(
      JoinKeyCryptorReEncryptResponse double_encrypted_response2,
      JoinKeyReEncrypt(reencrypt_request2));
  const auto& double_encrypted_texts2 =
      double_encrypted_response2.encrypted_join_key_and_ids();
  JoinKeyCryptorReEncryptResponse expected_double_encrypted_response2;
  expected_double_encrypted_response2.mutable_encrypted_join_key_and_ids()
      ->CopyFrom(encrypted_texts1);
  ASSERT_THAT(double_encrypted_response2,
              NotEqualsProto(expected_double_encrypted_response2));

  JoinKeyCryptorDecryptRequest decrypt_request1;
  decrypt_request1.set_encryption_key(random_key_1);
  decrypt_request1.mutable_encrypted_join_key_and_ids()->CopyFrom(
      double_encrypted_texts1);
  ASSERT_OK_AND_ASSIGN(JoinKeyCryptorDecryptResponse decrypted_response1,
                       JoinKeyDecrypt(decrypt_request1));
  JoinKeyCryptorDecryptResponse expected_decrypted_response1;
  expected_decrypted_response1.mutable_decrypted_join_key_and_ids()->CopyFrom(
      encrypted_texts2);
  ASSERT_THAT(decrypted_response1, EqualsProto(expected_decrypted_response1));

  JoinKeyCryptorDecryptRequest decrypt_request2;
  decrypt_request2.set_encryption_key(random_key_1);
  decrypt_request2.mutable_encrypted_join_key_and_ids()->CopyFrom(
      double_encrypted_texts2);
  ASSERT_OK_AND_ASSIGN(JoinKeyCryptorDecryptResponse decrypted_response2,
                       JoinKeyDecrypt(decrypt_request2));
  JoinKeyCryptorDecryptResponse expected_decrypted_response2;
  expected_decrypted_response2.mutable_decrypted_join_key_and_ids()->CopyFrom(
      encrypted_texts2);
  ASSERT_THAT(decrypted_response2, EqualsProto(expected_decrypted_response2));

  JoinKeyCryptorDecryptRequest decrypt_request3;
  decrypt_request3.set_encryption_key(random_key_2);
  decrypt_request3.mutable_encrypted_join_key_and_ids()->CopyFrom(
      double_encrypted_texts1);
  ASSERT_OK_AND_ASSIGN(JoinKeyCryptorDecryptResponse decrypted_response3,
                       JoinKeyDecrypt(decrypt_request3));
  JoinKeyCryptorDecryptResponse expected_decrypted_response3;
  expected_decrypted_response3.mutable_decrypted_join_key_and_ids()->CopyFrom(
      encrypted_texts1);
  ASSERT_THAT(decrypted_response3, EqualsProto(expected_decrypted_response3));

  JoinKeyCryptorDecryptRequest decrypt_request4;
  decrypt_request4.set_encryption_key(random_key_2);
  decrypt_request4.mutable_encrypted_join_key_and_ids()->CopyFrom(
      double_encrypted_texts2);
  ASSERT_OK_AND_ASSIGN(JoinKeyCryptorDecryptResponse decrypted_response4,
                       JoinKeyDecrypt(decrypt_request4));
  JoinKeyCryptorDecryptResponse expected_decrypted_response4;
  expected_decrypted_response4.mutable_decrypted_join_key_and_ids()->CopyFrom(
      encrypted_texts1);
  ASSERT_THAT(decrypted_response4, EqualsProto(expected_decrypted_response4));
}

}  // namespace
}  // namespace wfa::panelmatch::client::joinkeyexchange
