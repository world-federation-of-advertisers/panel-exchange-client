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

#include "wfa/panelmatch/client/keygeneration/key_generator.h"

#include "common_cpp/testing/status_macros.h"
#include "common_cpp/testing/status_matchers.h"
#include "gtest/gtest.h"

namespace wfa::panelmatch::client::keygeneration {

// Test to ensure an error is not thrown when called properly
TEST(KeyGeneratorTest, properImplementation) {
  KeyGenerator generator = KeyGenerator();
  ASSERT_OK_AND_ASSIGN(GeneratedKeys keys, generator.GenerateKeys());
}

// Test to ensure each call to GenerateKeys returns two unique values
TEST(KeyGeneratorTest, uniqueValues) {
  KeyGenerator generator = KeyGenerator();
  ASSERT_OK_AND_ASSIGN(GeneratedKeys keys1, generator.GenerateKeys());
  ASSERT_OK_AND_ASSIGN(GeneratedKeys keys2, generator.GenerateKeys());
  ASSERT_NE(keys1.pepper, keys2.pepper);
  ASSERT_NE(keys1.cryptokey, keys2.cryptokey);
}
}  // namespace wfa::panelmatch::client::keygeneration
