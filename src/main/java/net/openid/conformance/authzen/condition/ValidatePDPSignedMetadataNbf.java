package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;


public class ValidatePDPSignedMetadataNbf extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		JsonElement nbfEl = env.getElementFromObject("pdp_signed_metadata", "claims.nbf");

		if (nbfEl == null) {
			log("PDP signed_metadata has no nbf claim (optional), skipping check");
			return env;
		}

		long nbf = OIDFJSON.getLong(nbfEl);

		try {
			JWTUtil.validateNbfClaim(nbf);
		} catch (IllegalArgumentException e) {
			throw error("PDP signed_metadata nbf claim is invalid: " + e.getMessage(), args("nbf", nbf));
		}

		logSuccess("PDP signed_metadata nbf claim is valid", args("nbf", nbf));

		return env;	}
}
