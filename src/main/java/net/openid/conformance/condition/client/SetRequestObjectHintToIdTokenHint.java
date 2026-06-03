package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetRequestObjectHintToIdTokenHint extends AbstractCondition {

	public static final String UNSUPPORTED_ID_TOKEN_HINT =
		"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InVuc3VwcG9ydGVkLWhpbnQtdGVzdCJ9"
			+ ".eyJpc3MiOiJodHRwczovL2lzc3Vlci5leGFtcGxlLmNvbSIsInN1YiI6InN1YmplY3QiLCJhdWQiOiJjbGllbnQiLCJleHAiOjQxMDI0NDQ4MDAsImlhdCI6MTc2MDAwMDAwMCwibm9uY2UiOiJub25jZSJ9"
			+ ".c2lnbmF0dXJl";

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.remove("login_hint");
		requestObjectClaims.addProperty("id_token_hint", UNSUPPORTED_ID_TOKEN_HINT);
		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Replaced login_hint in request object claims with id_token_hint",
			args("id_token_hint", UNSUPPORTED_ID_TOKEN_HINT, "request_object_claims", requestObjectClaims));

		return env;
	}
}
