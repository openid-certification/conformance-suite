package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIExtractPreAuthorizedCodeFromCredentialOffer extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject credentialOfferObject = env.getElementFromObject("vci", "credential_offer").getAsJsonObject();
		JsonObject credentialOfferGrantsObject = env.getElementFromObject("vci", "credential_offer.grants").getAsJsonObject();

		String preAuthorizedCodeGrant = "urn:ietf:params:oauth:grant-type:pre-authorized_code";
		JsonObject preAuthorizedCodeGrantObject = credentialOfferGrantsObject.getAsJsonObject(preAuthorizedCodeGrant);
		if (preAuthorizedCodeGrantObject == null) {
			throw error("Pre-authorized code grant not found in credential offer grants", args("credential_offer", credentialOfferObject));
		}

		if (!preAuthorizedCodeGrantObject.has("pre-authorized_code")) {
			throw error("pre-authorized_code missing in Pre-authorized code grant", args("credential_offer", credentialOfferObject));
		}

		String preAuthorizedCode  = OIDFJSON.getString(preAuthorizedCodeGrantObject.get("pre-authorized_code"));
		env.putString("vci", "pre-authorized_code", preAuthorizedCode);

		logSuccess("Found pre-authorized_code in Pre-authorized code grant", args("pre-authorized_code", preAuthorizedCode, "credential_offer", credentialOfferObject));
		return env;
	}
}
