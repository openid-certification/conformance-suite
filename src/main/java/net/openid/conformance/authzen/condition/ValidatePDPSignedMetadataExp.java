package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;


public class ValidatePDPSignedMetadataExp extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		JsonElement expEl = env.getElementFromObject("pdp_signed_metadata", "claims.exp");

		if (expEl == null) {
			log("PDP signed_metadata has no exp claim (optional), skipping check");
			return env;
		}

		long exp = OIDFJSON.getLong(expEl);

		try {
			JWTUtil.validateExpClaim(exp);
		} catch (IllegalArgumentException e) {
			throw error("PDP signed_metadata exp claim is invalid: " + e.getMessage(), args("exp", exp));
		}

		logSuccess("PDP signed_metadata exp claim is valid", args("exp", exp));

		return env;	}
}
