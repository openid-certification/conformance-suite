package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractValidateDpopProofNonce extends AbstractCondition {

	protected boolean isValidDpopNonce(Environment env, String expectedNonce) {
		boolean isValid = false;
		// check for incoming nonce
		JsonElement incomingNonce = env.getElementFromObject("incoming_dpop_proof", "claims.nonce");

		if(null == expectedNonce) { // server did not set nonce
			if(null != incomingNonce) {
				log("DPoP Proof nonce supplied where none is expected");
				// Spec is unclear whether supplying a nonce when none is requested is an error
				// Treat this as an error by default
			} else {
				isValid = true;
				logSuccess("DPoP nonce not required");
			}
		} else { // server set nonce
			if(null == incomingNonce) {
				log("DPoP Proof nonce not supplied", args("expected", expectedNonce));
			} else if(!expectedNonce.equals(OIDFJSON.getString(incomingNonce))) {
				log("DPoP Proof nonce is invalid", args("expected", expectedNonce, "actual", incomingNonce));
			} else {
				isValid = true;
			}
		}
		return isValid;
	}

}
