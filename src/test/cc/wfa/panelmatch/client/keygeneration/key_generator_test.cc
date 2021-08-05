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
