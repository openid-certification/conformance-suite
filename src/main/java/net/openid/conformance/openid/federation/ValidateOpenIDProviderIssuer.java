package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateOpenIDProviderIssuer extends AbstractCondition {

	@Override
	@PreEnvironment(required = "primary_entity_statement_jwt")
	public Environment evaluate(Environment env) {
		JsonElement issuerElement = env.getElementFromObject("primary_entity_statement_jwt", "claims.metadata.openid_provider.issuer");
		if (!OIDFJSON.isString(issuerElement)) {
			throw error("OpenID Provider metadata issuer is missing or is not a string", args("issuer", issuerElement));
		}

		String issuer = OIDFJSON.getString(issuerElement);
		String entityIdentifier = env.getString("primary_entity_statement_jwt", "claims.iss");
		if (!issuer.equals(entityIdentifier)) {
			throw error("OpenID Provider metadata issuer does not match the Entity Identifier",
				args("issuer", issuer, "entity_identifier", entityIdentifier));
		}

		logSuccess("OpenID Provider metadata issuer matches the Entity Identifier", args("issuer", issuer));
		return env;
	}

}
