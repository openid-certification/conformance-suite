package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;


public class ValidatePDPSignedMetadataAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		String alg = env.getString("pdp_signed_metadata", "header.alg");
		if(Strings.isNullOrEmpty(alg) || "none".equalsIgnoreCase(alg)) {
			throw error("Invalid PDP signed_metadata alg", args("alg", alg));
		}
		logSuccess("PDP signed_metadata signed with valid alg", args("alg", alg));
		return env;
	}
}
