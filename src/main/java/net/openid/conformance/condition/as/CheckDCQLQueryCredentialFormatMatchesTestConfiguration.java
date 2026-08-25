package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

/**
 * Checks that the credential format in the DCQL query matches the credential format
 * the test is configured for. For example, if the test is configured for SD-JWT VC,
 * the DCQL query should request dc+sd-jwt credentials, not mso_mdoc.
 */
public class CheckDCQLQueryCredentialFormatMatchesTestConfiguration extends AbstractCondition {

	// Keyed on the credential format variant values, which are shared by the verifier and wallet
	// test families (VP1FinalVerifierCredentialFormat / VP1FinalWalletCredentialFormat).
	private static final Map<String, String> VARIANT_TO_DCQL_FORMAT = Map.of(
		"sd_jwt_vc", "dc+sd-jwt",
		"iso_mdl", "mso_mdoc"
	);

	@Override
	@PreEnvironment(required = { "dcql_query" }, strings = { "credential_format" })
	public Environment evaluate(Environment env) {
		String configuredFormat = env.getString("credential_format");
		String expectedDcqlFormat = VARIANT_TO_DCQL_FORMAT.get(configuredFormat);
		if (expectedDcqlFormat == null) {
			throw error("Unknown credential_format in test configuration",
				args("credential_format", configuredFormat));
		}

		JsonObject dcql = env.getObject("dcql_query");
		JsonArray credentials = dcql.getAsJsonArray("credentials");
		if (credentials == null || credentials.isEmpty()) {
			throw error("DCQL query has no credentials array");
		}

		for (JsonElement credEl : credentials) {
			JsonObject cred = credEl.getAsJsonObject();
			JsonElement formatEl = cred.get("format");
			if (formatEl == null) {
				throw error("DCQL query credential is missing 'format' field",
					args("credential", cred, "expected_format", expectedDcqlFormat));
			}
			String format = OIDFJSON.getString(formatEl);
			if (!expectedDcqlFormat.equals(format)) {
				throw error("DCQL query requests credential format '%s' but the test is configured for '%s' (%s)".formatted(
						format, expectedDcqlFormat, configuredFormat),
					args("credential", cred, "expected_format", expectedDcqlFormat, "actual_format", format,
						"configured_credential_format", configuredFormat));
			}
		}

		logSuccess("DCQL query credential format matches test configuration",
			args("expected_format", expectedDcqlFormat, "credential_count", credentials.size()));
		return env;
	}
}
