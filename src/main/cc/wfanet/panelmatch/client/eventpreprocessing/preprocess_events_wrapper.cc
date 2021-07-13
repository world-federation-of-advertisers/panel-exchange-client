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
#include "wfanet/panelmatch/client/eventpreprocessing/preprocess_events_wrapper.h"

#include <string>

#include "absl/status/statusor.h"
#include "wfanet/panelmatch/client/eventpreprocessing/preprocess_events.h"
#include "wfanet/panelmatch/client/eventpreprocessing/preprocess_events.pb.h"
#include "wfanet/panelmatch/common/jni_wrap.h"

namespace wfanet::panelmatch::client {
absl::StatusOr<std::string> PreprocessEventsWrapper(
    const std::string& serialized_request) {
  return wfanet::panelmatch::common::JniWrap(serialized_request,
                                             PreprocessEvents);
}
}  // namespace wfanet::panelmatch::client