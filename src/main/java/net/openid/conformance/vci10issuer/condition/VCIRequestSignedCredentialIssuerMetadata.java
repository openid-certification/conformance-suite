package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIRequestSignedCredentialIssuerMetadata extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject headers = new JsonObject();
		headers.addProperty("Accept", "application/jwt");
		env.putObject("credential_issuer_metadata_endpoint_request", "headers", headers);
		log("Set Accept header to request signed credential issuer metadata", args("headers", headers));
		return env;
	}
}
