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

#include "wfa/panelmatch/common/compression/brotli_wrapper.h"

#include <string>

#include "absl/status/statusor.h"
#include "absl/strings/string_view.h"
#include "common_cpp/jni/jni_wrap.h"
#include "wfa/panelmatch/common/compression/brotli.h"

namespace wfa::panelmatch {

absl::StatusOr<std::string> BrotliCompressWrapper(
    const std::string& serialized_request) {
  return JniWrap(serialized_request, BrotliCompress);
}

absl::StatusOr<std::string> BrotliDecompressWrapper(
    const std::string& serialized_request) {
  return JniWrap(serialized_request, BrotliDecompress);
}

}  // namespace wfa::panelmatch
