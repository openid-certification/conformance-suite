package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractFederationFetchEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "entity_statement_body" })
	@PostEnvironment(strings = "entity_statement_url")
	public Environment evaluate(Environment env) {

		String fetchEndpoint = OIDFJSON.getString(env.getElementFromObject("entity_statement_body", "metadata.federation_entity.federation_fetch_endpoint"));
		env.putString("entity_statement_url", fetchEndpoint);

		logSuccess("Extracted federation fetch endpoint", args("entity_statement_url", fetchEndpoint));

		return env;
	}

}
