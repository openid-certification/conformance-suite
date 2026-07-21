package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;


public class ValidatePDPSignedMetadataIat extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		JsonElement iatEl = env.getElementFromObject("pdp_signed_metadata", "claims.iat");

		if (iatEl == null) {
			log("PDP signed_metadata has no iat claim (optional), skipping check");
			return env;
		}

		long iat = OIDFJSON.getLong(iatEl);

		try {
			JWTUtil.validateIatClaim(iat);
		} catch (IllegalArgumentException e) {
			throw error("PDP signed_metadata iat claim is invalid: " + e.getMessage(), args("iat", iat));
		}

		logSuccess("PDP signed_metadata iat claim is valid", args("iat", iat));

		return env;	}
}
