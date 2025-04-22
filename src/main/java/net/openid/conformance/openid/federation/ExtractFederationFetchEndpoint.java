package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractFederationFetchEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" })
	@PostEnvironment(strings = "federation_endpoint_url")
	public Environment evaluate(Environment env) {

		JsonElement fetchEndpointElement = env.getElementFromObject("federation_response_jwt", "claims.metadata.federation_entity.federation_fetch_endpoint");
		if (fetchEndpointElement == null) {
			throw error("Federation entity metadata does not contain a federation_fetch_endpoint",
				args("federation_list_endpoint", fetchEndpointElement));
		}

		String fetchEndpoint = OIDFJSON.getString(fetchEndpointElement);

		env.putString("federation_endpoint_url", fetchEndpoint);

		logSuccess("Extracted federation fetch endpoint", args("federation_endpoint_url", fetchEndpoint));

		return env;
	}

}
