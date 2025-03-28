package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;

public class VCIValidateCredentialIssuerUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		String iss = OIDFJSON.getString(metadata.get("credential_issuer"));

		URI issUri = URI.create(iss);

		if (!"https".equals(issUri.getScheme())) {
			throw error("credential_issuer must use https", args("credential_issuer", iss));
		}

		if (issUri.getFragment() != null) {
			throw error("credential_issuer must not contain a fragment part", args("credential_issuer", iss));
		}

		if (issUri.getQuery() != null) {
			throw error("credential_issuer must not contain a query part", args("credential_issuer", iss));
		}

		logSuccess("credential_issuer URI is valid", args("credential_issuer", iss));

		return env;
	}
}
