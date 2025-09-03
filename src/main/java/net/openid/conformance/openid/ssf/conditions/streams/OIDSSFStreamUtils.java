package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Iterator;

public class OIDSSFStreamUtils {

	public enum StreamSubjectOperation {
		add,
		remove,
	}

	public enum StreamStatusValue {
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

	public static void addStreamSubject(JsonObject streamConfig, JsonObject newSubject, Boolean verified) {

		JsonObject streamSubjects = streamConfig.getAsJsonObject("_subjects");
		if (streamSubjects == null) {
			streamSubjects = new JsonObject();
			streamSubjects.add("subjects", new JsonArray());
			streamConfig.add("_subjects", streamSubjects);
		}

		JsonArray subjects = streamSubjects.getAsJsonArray("subjects");
		boolean found = false;
		for (JsonElement subject : subjects) {
			JsonObject subjectObj = subject.getAsJsonObject();

			// we do a very simple comparison here for now
			// real implementations will probably do subject format specific checks here.
			if (subjectObj.equals(newSubject)) {
				found = true;
				break;
			}
		}

		if (verified == null) {
			newSubject.addProperty("_verified", true);
		} else {
			newSubject.addProperty("_verified", verified);
		}

		if (!found) {
			subjects.add(newSubject);
		}
	}

	public static boolean removeStreamSubject(JsonObject streamConfig, JsonObject subjectToRemove) {

		JsonObject streamSubjects = streamConfig.getAsJsonObject("_subjects");
		if (streamSubjects == null) {
			streamSubjects = new JsonObject();
			streamSubjects.add("subjects", new JsonArray());
			streamConfig.add("_subjects", streamSubjects);
		}

		JsonArray subjects = streamSubjects.getAsJsonArray("subjects");

		int removed = 0;
		for (Iterator<JsonElement> iter = subjects.iterator(); iter.hasNext(); ) {
			JsonElement subject = iter.next();
			JsonObject subjectObj = subject.getAsJsonObject();

			JsonObject subjectObjWithoutVerified = subjectObj.deepCopy();
			subjectObjWithoutVerified.remove("_verified");

			JsonObject subjectToRemoveWithoutVerified = subjectToRemove.deepCopy();
			subjectToRemoveWithoutVerified.remove("_verified");

			// we do a very simple comparison here for now excluding the verified field
			// real implementations will probably do subject format specific checks here.
			if (subjectObjWithoutVerified.equals(subjectToRemoveWithoutVerified)) {
				iter.remove();
				removed++;
			}
		}

		return removed > 0;
	}

	public static JsonObject getInvalidSubjectExample() {
		JsonObject invalidSubject = new JsonObject();
		invalidSubject.addProperty("format", "opaque");
		invalidSubject.addProperty("id", "invalid");
		return invalidSubject;
	}
}
