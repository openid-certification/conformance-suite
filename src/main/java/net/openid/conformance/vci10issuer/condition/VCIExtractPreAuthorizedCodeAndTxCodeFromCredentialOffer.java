package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIExtractPreAuthorizedCodeAndTxCodeFromCredentialOffer extends AbstractCondition {

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

		String txCodeValue = "123456"; // FIXME obtain proper tx_code value
		env.putString("vci", "pre-authorized_code_tx_code_value", txCodeValue);

		logSuccess("Found pre-authorized_code in Pre-authorized code grant", args("pre-authorized_code", preAuthorizedCode, "tx_code_value", txCodeValue, "credential_offer", credentialOfferObject));
		return env;
	}
}
