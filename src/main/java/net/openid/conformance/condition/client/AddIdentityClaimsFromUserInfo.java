package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIdentityClaimsFromUserInfo extends AbstractCondition {
	public static final String location = "userinfo";

	@Override
	@PreEnvironment(required = {location, "identity_claims"})
	public Environment evaluate(Environment env) {
		JsonObject identityClaims = env.getObject("identity_claims");

		JsonObject userInfo = env.getObject("userinfo");

		for (String claim: userInfo.keySet()) {
			JsonElement userinfoValue = userInfo.get(claim);
			JsonElement idTokenValue = identityClaims.get(claim);
			if (idTokenValue != null) {
				if (!userinfoValue.equals(idTokenValue)) {
					throw error("Value of "+claim+" differs between id_token and userinfo",
						args("id_token", idTokenValue, "userinfo", userinfoValue));
				}
			}
			identityClaims.add(claim, userinfoValue);
		}

		logSuccess("Merged identity claims from userinfo with those from id_token",
			args("userinfo", userInfo, "identity_claims", identityClaims));
		return env;
	}

}
