package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIResolveCredentialEndpointToUse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		String credentialEndpointUrl = env.getString("vci", "credential_issuer_metadata.credential_endpoint");
		log("Use credential endpoint from credential issuer metadata", args("credential_endpoint", credentialEndpointUrl));
		env.putString("resource", "resourceUrl", credentialEndpointUrl);

		return env;
	}
}
