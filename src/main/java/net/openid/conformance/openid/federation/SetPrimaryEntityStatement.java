package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetPrimaryEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "entity_statement_endpoint_response", "entity_statement_body", "entity_statement_header" } )
	@PostEnvironment(required = { "primary_entity_statement_endpoint_response", "primary_entity_statement_body", "primary_entity_statement_header" } )
	public Environment evaluate(Environment env) {

		env.putObject("primary_entity_statement_endpoint_response", env.getObject("entity_statement_endpoint_response"));
		env.putObject("primary_entity_statement_body", env.getObject("entity_statement_body"));
		env.putObject("primary_entity_statement_header", env.getObject("entity_statement_header"));

		logSuccess("Primary entity statement set");

		return env;
	}

}
