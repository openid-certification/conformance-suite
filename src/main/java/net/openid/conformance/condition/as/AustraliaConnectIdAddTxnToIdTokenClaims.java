package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfo;

public class AustraliaConnectIdAddTxnToIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject idTokenClaims = env.getObject("id_token_claims");

		if (!idTokenClaims.has("txn")) {
			String txn = null;
			if (env.containsObject("backchannel_request_object")) {
				// For CIBA, we use auth_req_id as txn if possible, or just generate one
				txn = env.getString("auth_req_id");
			}

			if (txn == null) {
				JsonObject userinfo = OIDCCLoadUserInfo.getUserInfoClaimsValues("txn");
				txn = userinfo.get("txn").getAsString();
			}

			idTokenClaims.addProperty("txn", txn);
			env.putObject("id_token_claims", idTokenClaims);
			logSuccess("Added mandatory txn claim to ID Token", args("txn", txn));
		} else {
			log("txn claim already present in ID Token");
		}

		return env;
	}
}
