package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfo;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.List;

public class AustraliaConnectIdEnsureAuthorizationRequestContainsNoUserinfoIdentityClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = {CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY})
	public Environment evaluate(Environment env) {
		JsonElement requestedUserinfoClaimsElement = env.getElementFromObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "claims.userinfo");
		if((null != requestedUserinfoClaimsElement) && requestedUserinfoClaimsElement.isJsonObject()) {
			JsonObject requestedUserinfoClaims = requestedUserinfoClaimsElement.getAsJsonObject();
			List<String> userinfoClaims = Arrays.asList(OIDCCLoadUserInfo.SUPPORTED_CLAIMS);
			for (String key : requestedUserinfoClaims.keySet()) {
				if (userinfoClaims.contains(key)) {
					throw error("Authorization request claims request contains identity claims for the UserInfo endpoint", args("claims userinfo request", requestedUserinfoClaims));
				}
			}
		}
		logSuccess("Authorization request claims request do not contain identity claims for the UserInfo endpoint");
		return env;
	}
}
