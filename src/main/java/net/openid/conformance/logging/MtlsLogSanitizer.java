package net.openid.conformance.logging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Set;

/** Removes TLS private keys from log copies, never from operational configuration or state. */
public final class MtlsLogSanitizer {

	private static final Set<String> CREDENTIAL_OBJECTS = Set.of(
		"mtls", "mtls2", "mutual_tls_authentication", "mutual_tls_authentication2", "request_mutual_tls");

	private MtlsLogSanitizer() {
	}

	public static JsonObject redact(JsonObject object) {
		JsonObject copy = object.deepCopy();
		redactKeys(copy, false);
		return copy;
	}

	private static void redactKeys(JsonElement element, boolean credentials) {
		if (element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			// The named objects also cover invalid configuration in which the certificate is absent.
			// cert/key objects cover generated credentials and mapped copies in the final environment.
			if (credentials || object.has("cert")) {
				object.remove("key");
			}
			for (var entry : object.entrySet()) {
				redactKeys(entry.getValue(), CREDENTIAL_OBJECTS.contains(entry.getKey()));
			}
		} else if (element.isJsonArray()) {
			for (JsonElement child : element.getAsJsonArray()) {
				redactKeys(child, credentials);
			}
		}
	}
}
