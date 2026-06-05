package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 9.1.2 — the `capabilities` metadata field, when present, MUST be a
 * JSON array of (URN) strings. Absent entirely is fine; non-conforming shapes
 * are flagged so the caller can decide severity.
 */
public class EnsureMetadataCapabilitiesValid extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		if (!server.has("capabilities")) {
			logSuccess("Metadata `capabilities` not present (optional)");
			return env;
		}
		JsonElement capsElem = server.get("capabilities");
		if (!capsElem.isJsonArray()) {
			throw error("Metadata `capabilities` is present but is not a JSON array",
				args("capabilities", capsElem));
		}
		JsonArray caps = capsElem.getAsJsonArray();
		for (int i = 0; i < caps.size(); i++) {
			JsonElement entry = caps.get(i);
			if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
				throw error("Metadata `capabilities` array contains a non-string entry",
					args("index", i, "entry", entry));
			}
		}
		logSuccess("Metadata `capabilities` is a valid array of strings", args("capabilities", caps));
		return env;
	}
}
