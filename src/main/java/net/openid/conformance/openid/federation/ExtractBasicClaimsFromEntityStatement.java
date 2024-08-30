package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public class ExtractBasicClaimsFromEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "entity_statement_body")
	@PostEnvironment(strings = { "entity_statement_iss", "entity_statement_sub" })
	public Environment evaluate(Environment env) {

		String iss = env.getString("entity_statement_body", "iss");
		String sub = env.getString("entity_statement_body", "sub");
		Long iat = env.getLong("entity_statement_body", "iat");
		Long exp = env.getLong("entity_statement_body", "exp");

		env.putString("entity_statement_iss", iss);
		env.putString("entity_statement_sub", sub);
		env.putLong("entity_statement_iat", iat);
		env.putLong("entity_statement_exp", exp);

		logSuccess("Extracted basic claims from entity statement", args("iss", iss, "sub", sub, "iat", iat, "exp", exp));

		return env;
	}

}
