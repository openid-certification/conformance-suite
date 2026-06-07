package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.IssuerUrlValidation;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class VCIValidateCredentialIssuerUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		JsonElement credentialIssuerEl = metadata.get("credential_issuer");
		if (!OIDFJSON.isString(credentialIssuerEl)) {
			throw error("credential_issuer is missing or not a string in credential issuer metadata",
				args("credential_issuer", credentialIssuerEl));
		}
		String iss = OIDFJSON.getString(credentialIssuerEl);

		List<String> issues = new ArrayList<>();
		IssuerUrlValidation.validate(iss, "credential_issuer", issues);
		if (!issues.isEmpty()) {
			throw error("credential_issuer is not a valid Credential Issuer Identifier URL (OID4VCI 1.0 Final §12.2.1)",
				args("credential_issuer", iss, "issues", issues));
		}

		String configuredIssuer = env.getString("config", "vci.credential_issuer_url");
		if (configuredIssuer == null || configuredIssuer.isBlank()) {
			log("No configured Credential Issuer Identifier (vci.credential_issuer_url) in test config; skipping the byte-identical equality check",
				args("credential_issuer", iss));
			logSuccess("credential_issuer URI is valid",
				args("credential_issuer", iss));
			return env;
		}

		if (!configuredIssuer.equals(iss)) {
			throw error("credential_issuer in the returned metadata is not identical to the Credential Issuer Identifier used to retrieve it — OID4VCI 1.0 Final §12.2.3 requires byte-identical equality",
				args("credential_issuer", iss, "configured_issuer", configuredIssuer));
		}

		logSuccess("credential_issuer URI is valid and matches the configured Credential Issuer Identifier",
			args("credential_issuer", iss));

		return env;
	}
}
