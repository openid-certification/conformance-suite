package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

public class ExtractFederationFetchEndpoint extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "entity_statement_url")
	public Environment evaluate(Environment env) {

		//String iss = OIDFJSON.getString(env.getElementFromObject("entity_statement_body", "iss"));
		//JsonObject jwks = env.getElementFromObject("entity_statement_body", "jwks").getAsJsonObject();
		String fetchEndpoint = OIDFJSON.getString(env.getElementFromObject("entity_statement_body", "metadata.federation_entity.federation_fetch_endpoint"));

		//String primaryIss = env.getString("entity_statement_iss");
		//String fetchEndpointUrlWithSubParam = UriComponentsBuilder.fromHttpUrl(fetchEndpoint).queryParam("sub", primaryIss).toUriString();

		env.putString("entity_statement_url", fetchEndpoint);
		//env.putString("federation_fetch_endpoint_iss", iss);
		//env.putObject("federation_fetch_endpoint_jkws", jwks);

		logSuccess("Extracted federation fetch endpoint", args("entity_statement_url", fetchEndpoint));

		return env;
	}

}
