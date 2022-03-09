package net.openid.conformance.ekyc.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Use only to validate a single verification element in a discovery document
 */
public class ValidateVerificationElementInServerConfiguration extends AbstractCondition {


	@Override
	@PreEnvironment(required = { "verification_element", "server" } )
	public Environment evaluate(Environment env) {

		JsonObject verificationObject = env.getObject("verification_element");
		if(verificationObject==null) {
			throw error("Missing 'verification' element");
		}
		//trust_framework: REQUIRED. String determining the trust framework governing the identity verification process and the identity assurance level of the OP.
		String trustFramework = env.getString("verification_element", "trust_framework");
		if (Strings.isNullOrEmpty(trustFramework)) {
			throw error("Missing 'trust_framework' in verification element");
		}

		//TODO the following is not necessary in discovery document?
		//time: Time stamp in ISO 8601:2004 [ISO8601-2004] YYYY-MM-DDThh:mm[:ss]TZD format representing the date and time when the identity verification process took place.
		//verification_process: Unique reference to the identity verification process as performed by the OP.
		//evidence: JSON array containing information about the evidence the OP used to verify the user's identity as separate JSON objects.
		// Every object contains the property type which determines the type of the evidence. The RP uses this information to process the evidence property appropriately.

		logSuccess("verification element is valid", args("verification", verificationObject));
		return env;

	}

}
