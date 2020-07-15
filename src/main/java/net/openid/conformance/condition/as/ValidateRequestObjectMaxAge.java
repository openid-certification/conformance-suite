package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateRequestObjectMaxAge extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {
		//Also see CreateEffectiveAuthorizationRequestParameters for max_age processing
		JsonElement maxAgeElement  = env.getElementFromObject("authorization_request_object", "claims.max_age");
		if (maxAgeElement == null) {
			log("Request object does not contain a max_age claim");
			return env;
		} else if(maxAgeElement.isJsonNull()) {
			//EnsureNumericRequestObjectClaimsAreNotNull handles the JsonNull case
			//Additionally, CreateEffectiveAuthorizationRequestParameters completely ignores max_age when it is json null
			log("max_age has a 'json null' value");
			return env;
		} else {
			try {
				Number maxAge = OIDFJSON.getNumber(maxAgeElement);
				logSuccess("max_age is correctly encoded as a number", args("max_age", maxAge));
				return env;
			} catch (OIDFJSON.UnexpectedJsonTypeException ex) {
				throw error("max_age is not encoded as a number", args("max_age", maxAgeElement));
			}
		}
	}

}
