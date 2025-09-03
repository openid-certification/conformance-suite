package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFStreamUtils {

	enum StreamStatusValue {
		enabled,
		paused,
		disabled {
			@Override
			public boolean isStatusChangeAllowed(StreamStatusValue newStatus) {
				return false;
			}
		};

		public boolean isStatusChangeAllowed(StreamStatusValue newStatus) {
			return true;
		}
	}

	public static void updateStreamStatus(JsonObject streamConfig, StreamStatusValue newStatusValue, String reason) {

		JsonObject streamStatus = streamConfig.getAsJsonObject("_status");
		if (streamStatus == null) {
			streamStatus = new JsonObject();
			streamStatus.addProperty("stream_id", OIDFJSON.tryGetString(streamConfig.get("stream_id")));
			streamStatus.addProperty("status", StreamStatusValue.enabled.name());
			streamConfig.add("_status", streamStatus);
		}

		StreamStatusValue currentStatus = StreamStatusValue.valueOf(OIDFJSON.getString(streamStatus.get("status")));
		if (!currentStatus.isStatusChangeAllowed(newStatusValue)) {
			throw new IllegalArgumentException("Invalid stream status change: cannot transition from " + currentStatus + " to " + newStatusValue);
		}

		streamStatus.addProperty("status", newStatusValue.name());
		if (reason != null) {
			streamStatus.addProperty("reason", reason);
		} else {
			streamStatus.remove("reason");
		}
	}
}
