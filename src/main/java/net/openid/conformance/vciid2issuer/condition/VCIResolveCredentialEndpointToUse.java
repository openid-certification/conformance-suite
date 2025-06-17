package net.openid.conformance.vciid2issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIResolveCredentialEndpointToUse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialEndpointUrl = env.getString("config", "resource.resourceUrl");
		if (credentialEndpointUrl != null) {
			log("Use credential endpoint (Resource resourceUrl) from configuration", args("credential_endpoint", credentialEndpointUrl));
		} else {
			credentialEndpointUrl = env.getString("vci", "credential_issuer_metadata.credential_endpoint");
			log("Use credential endpoint from credential issuer metadata", args("credential_endpoint", credentialEndpointUrl));
		}
		env.putString("resource","resourceUrl", credentialEndpointUrl);

		return env;
	}
}
