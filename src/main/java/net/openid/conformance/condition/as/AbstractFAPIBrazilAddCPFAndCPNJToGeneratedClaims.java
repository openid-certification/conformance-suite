package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 //if the cpf Claim is requested as an Essential Claim for the ID Token or UserInfo response with a values parameter
 // requesting a specific cpf value, the Authorization Server MUST return a cpf Claim Value that matches the requested
 // value. If this is an Essential Claim and the requirement cannot be met, then the Authorization Server MUST
 // treat that outcome as a failed authentication attempt.
 // ...
 // If the cnpj Claim is requested as an Essential Claim for the ID Token or UserInfo response with a values parameter
 // requesting a specific cnpj value, the Authorization Server MUST return a cnpj Claim Value that contains a set of
 // CNPJs one of which must match the requested value. If this is an Essential Claim and the requirement cannot be met,
 // then the Authorization Server MUST treat that outcome as a failed authentication attempt
 */
public abstract class AbstractFAPIBrazilAddCPFAndCPNJToGeneratedClaims extends AbstractCondition {

	protected boolean addClaims(Environment env, String environmentEntry, String location) {

		JsonObject idTokenClaimsInEnv = env.getObject(environmentEntry);

		JsonElement claimsElementInRequestObject = env.getElementFromObject("authorization_request_object", "claims.claims." + location);
		if(claimsElementInRequestObject==null) {
			log("Request object does not contain a claims element." + location);
			return false;
		}
		JsonObject requestedClaims = claimsElementInRequestObject.getAsJsonObject();

		if(requestedClaims.has("cpf") && !requestedClaims.get("cpf").isJsonNull()) {
			JsonObject cpf = requestedClaims.get("cpf").getAsJsonObject();
			String cpfFromConsentRequest = env.getString("consent_request_cpf");
			String cpfValue = null;
			if(cpf.has("essential") && OIDFJSON.getBoolean(cpf.get("essential"))) {
				if(cpf.has("value")) {
					cpfValue = OIDFJSON.getString(cpf.get("value"));
				} else if(cpf.has("values") && cpf.get("values").isJsonArray()) {
					JsonArray values = cpf.get("values").getAsJsonArray();
					if(values.contains(new JsonPrimitive(cpfFromConsentRequest))) {
						cpfValue = cpfFromConsentRequest;
					} else {
						throw error("Requested cpf claim values does not contain the cpf value from the consent request",
							args("requested_cpf_values", values, "cpf_in_consent_request", cpfFromConsentRequest));
					}
				} else {
					//use the cpf value from consent request
					cpfValue = cpfFromConsentRequest;
				}
				if(cpfFromConsentRequest!=null && !cpfFromConsentRequest.equals(cpfValue)) {
					throw error("Requested cpf claim value does not match the cpf value from the consent request",
						args("requested_cpf_value", cpfValue, "cpf_in_consent_request", cpfFromConsentRequest));
				} else {
					idTokenClaimsInEnv.addProperty("cpf", cpfValue);
				}
			}
		}

		if(requestedClaims.has("cnpj") && !requestedClaims.get("cnpj").isJsonNull()) {
			JsonObject cnpj = requestedClaims.get("cnpj").getAsJsonObject();
			String cnpjFromConsentRequest = env.getString("consent_request_cnpj");
			String cnpjValue = null;
			if(cnpj.has("essential") && OIDFJSON.getBoolean(cnpj.get("essential"))) {
				if(cnpjFromConsentRequest!=null) {
					if (cnpj.has("value")) {
						cnpjValue = OIDFJSON.getString(cnpj.get("value"));
					} else if (cnpj.has("values") && cnpj.get("values").isJsonArray()) {
						JsonArray values = cnpj.get("values").getAsJsonArray();
						if (values.contains(new JsonPrimitive(cnpjFromConsentRequest))) {
							cnpjValue = cnpjFromConsentRequest;
						} else {
							throw error("Requested cnpj claim values does not contain the cnpj value from the consent request",
								args("requested_cnpj_values", values, "cnpj_in_consent_request", cnpjFromConsentRequest));
						}
					} else {
						//use the value from consent request
						cnpjValue = cnpjFromConsentRequest;
					}
					if(!cnpjFromConsentRequest.equals(cnpjValue)) {
						throw error("Requested cnpj claim value does not match the cnpj value from the consent request",
							args("requested_cnpj_value", cnpjValue, "cnpj_in_consent_request", cnpjFromConsentRequest));
					} else {
						idTokenClaimsInEnv.addProperty("cnpj", cnpjValue);
					}
				}
			}
		}
		return true;
	}

}
