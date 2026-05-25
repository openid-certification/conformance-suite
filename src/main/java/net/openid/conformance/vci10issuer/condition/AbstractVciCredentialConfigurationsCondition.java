package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Base class for conditions that validate each entry of
 * {@code credential_issuer_metadata.credential_configurations_supported}. It centralises the
 * extraction and required-shape guard (which would otherwise be copy-pasted into every such
 * condition) so each subclass only supplies the per-configuration validation logic.
 */
public abstract class AbstractVciCredentialConfigurationsCondition extends AbstractCondition {

	/**
	 * Invokes {@code visitor} with the configuration id and configuration object for each entry of
	 * {@code credential_configurations_supported}, skipping entries whose value is not a JSON
	 * object. Throws a condition error if {@code credential_configurations_supported} is missing or
	 * is not a JSON object.
	 */
	protected void forEachCredentialConfiguration(Environment env, BiConsumer<String, JsonObject> visitor) {
		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		JsonElement credentialConfigurationsEl = metadata.get("credential_configurations_supported");
		if (credentialConfigurationsEl == null) {
			throw error("credential_configurations_supported is missing from credential issuer metadata");
		}
		if (!credentialConfigurationsEl.isJsonObject()) {
			throw error("credential_configurations_supported must be a JSON object",
				args("credential_configurations_supported", credentialConfigurationsEl));
		}
		for (Map.Entry<String, JsonElement> entry : credentialConfigurationsEl.getAsJsonObject().entrySet()) {
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			visitor.accept(entry.getKey(), entry.getValue().getAsJsonObject());
		}
	}
}
