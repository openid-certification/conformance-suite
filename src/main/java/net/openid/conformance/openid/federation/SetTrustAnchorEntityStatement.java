package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SetTrustAnchorEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "federation_endpoint_response", "federation_response_jwt" } )
	@PostEnvironment(
		required = { "trust_anchor_entity_statement_jwt" },
		strings = { "trust_anchor_entity_statement_iss", "trust_anchor_entity_statement_sub" })
	public Environment evaluate(Environment env) {

		env.putObject("trust_anchor_entity_statement_jwt", env.getObject("federation_response_jwt"));

		env.putString("trust_anchor_entity_statement_iss", OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "claims.iss")));
		env.putString("trust_anchor_entity_statement_sub", OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "claims.sub")));

		logSuccess("Trust anchor entity statement set");

		return env;
	}

}
