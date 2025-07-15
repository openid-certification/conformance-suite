package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCILogGeneratedCredentialIssuerMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "credential_issuer_metadata")
	public Environment evaluate(Environment env) {

		JsonObject credentialIssuerMetadata = env.getObject("credential_issuer_metadata");
		log("Created credential issuer metadata", args("credential_issuer", credentialIssuerMetadata));

		return env;

	}
}
