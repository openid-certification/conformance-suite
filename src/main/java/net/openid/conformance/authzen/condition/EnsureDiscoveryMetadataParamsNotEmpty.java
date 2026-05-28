package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Spec section 9.2.2-5: parameters with no value MUST be omitted rather than
 * sent as `null` or as an empty string. This condition walks the top-level
 * discovery metadata and throws on any param whose value is JSON null or an
 * empty string, so the caller can surface it as a WARNING (SHOULD-grade).
 */
public class EnsureDiscoveryMetadataParamsNotEmpty extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject metadata = env.getObject("server");
		List<String> offending = new ArrayList<>();
		for (Map.Entry<String, JsonElement> entry : metadata.entrySet()) {
			JsonElement value = entry.getValue();
			if (value == null || value.isJsonNull()) {
				offending.add(entry.getKey() + " (null)");
				continue;
			}
			if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
				&& OIDFJSON.getString(value).isEmpty()) {
				offending.add(entry.getKey() + " (empty string)");
			}
		}
		if (!offending.isEmpty()) {
			throw error("Discovery metadata contains parameters with null or empty-string values; spec 9.2.2-5 requires absent parameters to be omitted",
				args("offending_parameters", offending, "metadata", metadata));
		}
		logSuccess("All discovery metadata parameters have non-empty values");
		return env;
	}
}
