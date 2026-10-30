package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TestKeysAndCerts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Raises a finding when the suite is about to sign with a built-in certificate whose CA is an
 * ephemeral stand-in: the deployment has no key for the published CA, so nothing outside this
 * suite instance trusts the certificate, and an implementation that trusts the published CA or a
 * list of trusted entities naming it will reject what the suite signs. The subclasses know which
 * built-in certificates a module falls back to and which test configuration fields replace them.
 * Meant to be called at WARNING.
 */
public abstract class AbstractWarnIfBuiltInCertificateAuthorityIsEphemeral extends AbstractCondition {

	/**
	 * The CAs whose built-in leaves the module signs with under the test configuration in
	 * {@code env}, each with what the leaf signs and the test configuration field that replaces it;
	 * empty when the configuration supplies every key.
	 */
	protected abstract Map<TestKeysAndCerts.SuiteCa, String> builtInSignersInUse(Environment env);

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		Map<TestKeysAndCerts.SuiteCa, String> inUse = builtInSignersInUse(env);
		if (inUse.isEmpty()) {
			logSuccess("The test configuration supplies the signing keys; none of the suite's built-in certificates is used here");
			return env;
		}
		List<String> ephemeral = new ArrayList<>();
		List<String> variables = new ArrayList<>();
		for (Map.Entry<TestKeysAndCerts.SuiteCa, String> entry : inUse.entrySet()) {
			if (entry.getKey().getIdentity().getEphemeral()) {
				ephemeral.add(entry.getValue() + " (CA " + entry.getKey().getSlug() + ")");
				variables.add(TestKeysAndCerts.keyEnvironmentVariable(entry.getKey().getSlug()));
			}
		}
		if (ephemeral.isEmpty()) {
			logSuccess("The suite signs with built-in certificates issued by its published CAs",
				args("built_in_signers", inUse.values(), "trust_anchors_url", TestKeysAndCerts.getTrustAnchorsUrl()));
			return env;
		}
		throw error("The suite signs with built-in certificates issued by ephemeral stand-ins for its published CAs, because"
				+ " this deployment does not have the published CAs' keys: nothing outside this suite instance trusts them, so an"
				+ " implementation that trusts the published CA, or a list of trusted entities naming it, will reject what the suite"
				+ " signs. The certificates: " + String.join("; ", ephemeral) + ". Either fill in those fields of the test"
				+ " configuration or deploy the suite with the keys in the environment variables " + String.join(", ", variables) + ".",
			args("ephemeral_signers", ephemeral, "environment_variables", variables, "trust_anchors_url", TestKeysAndCerts.getTrustAnchorsUrl()));
	}

	/** Whether the test configuration sets any of {@code keys} (a cleared field is kept as JSON null or ""). */
	protected static boolean isConfigured(Environment env, String... keys) {
		for (String key : keys) {
			JsonElement value = env.getElementFromObject("config", key);
			if (value != null && !value.isJsonNull() && !(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
				&& OIDFJSON.getString(value).isBlank())) {
				return true;
			}
		}
		return false;
	}
}
