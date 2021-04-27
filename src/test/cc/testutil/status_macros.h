// Copyright 2020 The Cross-Media Measurement Authors
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

#ifndef SRC_TEST_CC_TESTUTIL_STATUS_MACROS_H_
#define SRC_TEST_CC_TESTUTIL_STATUS_MACROS_H_

#include <utility>
#define ASSERT_OK_AND_ASSIGN(lhs, rexpr) \
  ASSERT_OK_AND_ASSIGN_IMPL(             \
      STATUS_MACROS_CONCAT_NAME(_status_or_value, __COUNTER__), lhs, rexpr)

#define ASSERT_OK_AND_ASSIGN_IMPL(statusor, lhs, rexpr) \
  auto statusor = (rexpr);                              \
  ASSERT_TRUE(statusor.ok());                           \
  lhs = std::move(statusor).value()

#define STATUS_MACROS_CONCAT_NAME(x, y) STATUS_MACROS_CONCAT_IMPL(x, y)

#define STATUS_MACROS_CONCAT_IMPL(x, y) x##y

#endif  // SRC_TEST_CC_TESTUTIL_STATUS_MACROS_H_
