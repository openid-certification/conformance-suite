package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

public class ExtractFederationFetchEndpoint extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "federation_fetch_endpoint")
	public Environment evaluate(Environment env) {

		String fetchEndpoint = OIDFJSON.getString(env.getElementFromObject("entity_statement_body", "metadata.federation_entity.federation_fetch_endpoint"));
		String primaryIss = OIDFJSON.getString(env.getElementFromObject("primary_entity_statement_body", "iss"));
		String fetchEndpointUrlWithSubParam = UriComponentsBuilder.fromHttpUrl(fetchEndpoint).queryParam("sub", primaryIss).toUriString();
		env.putString("federation_fetch_endpoint", fetchEndpointUrlWithSubParam);

		logSuccess("Extracted federation fetch endpoint and appended the sub parameter", args("federation_fetch_endpoint", fetchEndpointUrlWithSubParam));

		return env;
	}

}
