package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetRequestObjectLoginHintToMalformedCardPrimaryAccountNumber extends AbstractCondition {

	public static final String MALFORMED_CARD_PRIMARY_ACCOUNT_NUMBER = "not-a-card-primary-account-number";

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.addProperty("login_hint", MALFORMED_CARD_PRIMARY_ACCOUNT_NUMBER);
		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Set login_hint in request object claims to a malformed card primary account number",
			args("login_hint", MALFORMED_CARD_PRIMARY_ACCOUNT_NUMBER, "request_object_claims", requestObjectClaims));

		return env;
	}
}
