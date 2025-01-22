package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateAbsenceOfAuthorityHints extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" } )
	public Environment evaluate(Environment env) {

		JsonElement authorityHintsElement = env.getElementFromObject("federation_response_jwt", "claims.authority_hints");

		if (authorityHintsElement != null && !authorityHintsElement.isJsonNull()) {
			throw error("authority_hints must not be present in Subordinate Statements", args("authority_hints", authorityHintsElement));
		}

		logSuccess("Entity statement does not contain authority_hints");
		return env;
	}
}
