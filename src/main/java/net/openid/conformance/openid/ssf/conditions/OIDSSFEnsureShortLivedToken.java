package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.concurrent.TimeUnit;

public class OIDSSFEnsureShortLivedToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "expires_in")
	public Environment evaluate(Environment env) {

		JsonObject expiresIn = env.getObject("expires_in");
		if (expiresIn == null || !expiresIn.has("expires_in")) {
			throw error("Could not find expires_in in access token response");
		}

		int expiresInValue = OIDFJSON.getInt(expiresIn.get("expires_in"));

		if (expiresInValue > TimeUnit.MINUTES.toSeconds(60)) {
			throw error("Found long-lived token with expires_in > 60 minutes", args("expires_in", expiresInValue));
		}

		logSuccess("Found short-lived token with expires_in <= 60 minutes", args("expires_in", expiresInValue));

		return env;
	}
}
