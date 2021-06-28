#include <string>
#include "wfanet/panelmatch/client/eventpreprocessing/preprocess_events_wrapper.h"
#include "absl/status/statusor.h"
#include "wfanet/panelmatch/client/eventpreprocessing/preprocess_events.pb.h"
#inclue "src/main/cc/wfanet/panelmatch/client/eventpreprocessing/preprocess_events.h"

namespace wfanet::panelmatch::client::PreprocessEvents {
  absl::StatusOr<PreprocessEventsResponse> Convert(const PreprocessEventsRequest& request) {
    // TODO: call Erin's library
    return absl::UnimplementedError("Not implemented");
  }
}