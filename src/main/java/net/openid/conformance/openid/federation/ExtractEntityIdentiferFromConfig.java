package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import static net.openid.conformance.openid.federation.EntityUtils.appendWellKnown;


public class ExtractEntityIdentiferFromConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(strings = "federation_endpoint_url")
	public Environment evaluate(Environment env) {

		String entityIdentifier = env.getString("config", "federation.entity_identifier");
		String entityStatementUrl = appendWellKnown(entityIdentifier);
		env.putString("federation_endpoint_url", entityStatementUrl);

		logSuccess("Set federation endpoint URL to configured entity identifier", args("federation_endpoint_url", entityStatementUrl));

		return env;
	}
}
