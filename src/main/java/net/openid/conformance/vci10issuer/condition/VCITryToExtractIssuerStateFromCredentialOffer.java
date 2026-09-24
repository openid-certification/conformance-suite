package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCITryToExtractIssuerStateFromCredentialOffer extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject credentialOffer = env.getElementFromObject("vci", "credential_offer").getAsJsonObject();

		JsonElement issuerStateEl = env.getElementFromObject("vci", "credential_offer.grants.authorization_code.issuer_state");
		if (issuerStateEl == null) {
			if (env.getString("vci", "issuer_state") != null) {
				// left over from an earlier offer in this test; it belongs to that offer's flow and
				// must not be sent with this one
				env.removeElement("vci", "issuer_state");
				log("Couldn't find issuer_state in credential offer; the issuer_state of the previous offer will not be reused",
					args("credential_offer", credentialOffer));
			} else {
				log("Couldn't find issuer_state in credential offer", args("credential_offer", credentialOffer));
			}
		} else {
			String issuerState = OIDFJSON.getString(issuerStateEl);
			env.putString("vci", "issuer_state", issuerState);
			log("Found issuer state", args("issuer_state", issuerState, "credential_offer", credentialOffer));
		}

		return env;
	}
}
