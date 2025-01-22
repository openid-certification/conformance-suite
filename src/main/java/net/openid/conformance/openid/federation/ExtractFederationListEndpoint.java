package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractFederationListEndpoint extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "federation_endpoint_url")
	public Environment evaluate(Environment env) {

		String listEndpoint = OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "claims.metadata.federation_entity.federation_list_endpoint"));

		env.putString("federation_endpoint_url", listEndpoint);

		logSuccess("Extracted federation list endpoint", args("federation_endpoint_url", listEndpoint));

		return env;
	}

}
