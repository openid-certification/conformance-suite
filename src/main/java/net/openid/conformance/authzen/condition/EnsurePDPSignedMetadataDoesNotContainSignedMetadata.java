package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;


public class EnsurePDPSignedMetadataDoesNotContainSignedMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		JsonElement signedMetadataElement = env.getElementFromObject("pdp_signed_metadata", "claims.signed_metadata");
		if(signedMetadataElement!=null) {
			throw error("PDP signed_metadata contains 'signed_metadata' claim");
		}
		logSuccess("PDP signed_metadata does not contain 'signed_metadata' claim");
		return env;
	}
}
