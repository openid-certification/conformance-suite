package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import static net.openid.conformance.openid.federation.EntityUtils.appendWellKnown;


public class ExtractEntityStatementUrlFromConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(strings = "entity_statement_url")
	public Environment evaluate(Environment env) {

		String entityStatementUrl = env.getString("config", "federation.entity_identifier");
		entityStatementUrl = appendWellKnown(entityStatementUrl);
		env.putString("entity_statement_url", entityStatementUrl);

		logSuccess("Constructed entity statement URL based on configured issuer", args("entity_statement_url", entityStatementUrl));

		return env;
	}

}

