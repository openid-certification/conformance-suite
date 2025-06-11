package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCITryToExtractIssuerStateFromCredentialOffer extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject credentialOffer = env.getElementFromObject("vci", "credential_offer").getAsJsonObject();

		JsonElement issuerStateEl = env.getElementFromObject("vci", "credential_offer.grants.authorization_code.issuer_state");
		if (issuerStateEl == null) {
			log("Couldn't find issuer_state in credential offer", args("credential_offer", credentialOffer));
		} else {
			String issuerState = OIDFJSON.getString(issuerStateEl);
			env.putString("vci", "issuer_state", issuerState);
			log("Found issuer state", args("issuer_state", issuerState, "credential_offer", credentialOffer));
		}

		return env;
	}
}
