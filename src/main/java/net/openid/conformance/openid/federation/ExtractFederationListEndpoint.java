package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractFederationListEndpoint extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "federation_endpoint_url")
	public Environment evaluate(Environment env) {

		JsonElement listEndpointElement = env.getElementFromObject("federation_response_jwt", "claims.metadata.federation_entity.federation_list_endpoint");
		if (listEndpointElement == null) {
			throw error("Federation entity metadata does not contain a federation_list_endpoint",
                    args("federation_list_endpoint", listEndpointElement));
		}

		String listEndpoint = OIDFJSON.getString(listEndpointElement);

		env.putString("federation_endpoint_url", listEndpoint);

		logSuccess("Extracted federation list endpoint", args("federation_endpoint_url", listEndpoint));

		return env;
	}

}
