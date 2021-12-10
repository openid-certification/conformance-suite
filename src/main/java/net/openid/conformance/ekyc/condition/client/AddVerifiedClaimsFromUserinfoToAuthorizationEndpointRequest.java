package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest extends AbstractCondition {


	@Override
	@PreEnvironment(required = {"config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement userinfo = env.getElementFromObject("config", "ekyc_userinfo");
		if(userinfo==null){
			throw error("User provided userinfo data is not set in configuration");
		}
		JsonElement verifiedClaims = createVerifiedClaimsRequestFromUserinfo(userinfo.getAsJsonObject());
		JsonObject authzEndpointRequest = env.getObject("authorization_endpoint_request");
		JsonObject claims = null;
		if(authzEndpointRequest.has("claims")) {
			claims = authzEndpointRequest.get("claims").getAsJsonObject();
		} else {
			claims = new JsonObject();
		}
		if(!claims.has("userinfo")) {
			claims.add("userinfo", new JsonObject());
		}
		claims.get("userinfo").getAsJsonObject().add("verified_claims", verifiedClaims);
		authzEndpointRequest.add("claims", claims);
		logSuccess("Added verified_claims based on provided userinfo to authorization request",
			args("authorization_endpoint_request",
				env.getObject("authorization_endpoint_request")));
		return env;
	}

	protected JsonElement createVerifiedClaimsRequestFromUserinfo(JsonObject userinfo) {
		JsonElement verifiedClaimsElementInUserinfo = userinfo.get("verified_claims");
		if(verifiedClaimsElementInUserinfo==null){
			throw error("userinfo must contain a verified_claims element");
		}
		if(verifiedClaimsElementInUserinfo.isJsonObject()) {
			JsonObject newRequestElement = createVerifiedClaimsFromVerifiedClaimsObject(verifiedClaimsElementInUserinfo.getAsJsonObject());
			return newRequestElement;
		} else if(verifiedClaimsElementInUserinfo.isJsonArray()) {
			JsonArray array = verifiedClaimsElementInUserinfo.getAsJsonArray();
			JsonArray requestClaims = new JsonArray();
			for(JsonElement element : array) {
				JsonObject newRequestElement = createVerifiedClaimsFromVerifiedClaimsObject(element.getAsJsonObject());
				requestClaims.add(newRequestElement);
			}
			return requestClaims;
		} else {
			throw error("Unexpected verified_claims in userinfo, must be either an array or object",
						args("userinfo", userinfo));
		}
	}

	protected JsonObject createVerifiedClaimsFromVerifiedClaimsObject(JsonObject verifiedClaimsObjectFromUserinfo) {
		JsonObject rv = new JsonObject();
		JsonObject claims = new JsonObject();
		JsonObject claimsInUserinfo = verifiedClaimsObjectFromUserinfo.get("claims").getAsJsonObject();
		for(String claimName : claimsInUserinfo.keySet()) {
			claims.add(claimName, JsonNull.INSTANCE);
		}
		rv.add("claims", claims);
		JsonObject verification = new JsonObject();
		JsonObject verificationInUserinfo = verifiedClaimsObjectFromUserinfo.get("verification").getAsJsonObject();
		verification.add("trust_framework", getConstrainableElementWithValue(verificationInUserinfo.get("trust_framework")));
		if(verificationInUserinfo.has("assurance_level")) {
			verification.add("assurance_level", getConstrainableElementWithValue(verificationInUserinfo.get("assurance_level")));
		}
		if(verificationInUserinfo.has("assurance_process")) {
			JsonObject assuranceProcess = new JsonObject();
			JsonObject assuranceProcessInUserinfo = verificationInUserinfo.get("assurance_process").getAsJsonObject();
			if(assuranceProcessInUserinfo.has("policy")) {
				assuranceProcess.add("policy", getConstrainableElementWithValue(assuranceProcessInUserinfo.get("policy")));
			}
			if(assuranceProcessInUserinfo.has("procedure")) {
				assuranceProcess.add("procedure", getConstrainableElementWithValue(assuranceProcessInUserinfo.get("procedure")));
			}
			if(assuranceProcessInUserinfo.has("status")) {
				assuranceProcess.add("status", getConstrainableElementWithValue(assuranceProcessInUserinfo.get("status")));
			}
			verification.add("assurance_process", assuranceProcess);
		}
		//TODO add time? e.g calulate max_age from time in userinfo and request values that satisfy or does not satisfy the max_age


		if(verificationInUserinfo.has("verification_process")) {
			verification.add("verification_process", getConstrainableElementWithValue(verificationInUserinfo.get("verification_process")));
		}

		JsonArray evidencesInUserinfo = verificationInUserinfo.get("evidence").getAsJsonArray();
		JsonArray evidenceRequest = new JsonArray();
		for(JsonElement evidenceElementFromUserinfo : evidencesInUserinfo) {
			JsonObject evidence = new JsonObject();
			JsonObject evidenceInUserinfo = evidenceElementFromUserinfo.getAsJsonObject();

			//type
			JsonObject evidenceType = new JsonObject();
			evidenceType.addProperty("value", OIDFJSON.getString(evidenceInUserinfo.get("type")));
			evidence.add("type", evidenceType);
			//TODO add evidence type specific items

////			evidence.add("document_details", JsonNull.INSTANCE);
////			JsonObject doc = new JsonObject();
////			doc.add("type", JsonNull.INSTANCE);
////			evidence.add("document", doc);
//
//			JsonObject doc = new JsonObject();
//			doc.add("type", JsonNull.INSTANCE);
//			evidence.add("document", doc); // FIXME yes.com still use document
////			evidence.add("document_details", doc);
//                 {
//                    "method": "pipp",
//                    "document": {
//                        "number": "L3FY80139",
//                        "date_of_issuance": "2018-01-02",
//                        "date_of_expiry": "2028-01-02",
//                        "type": "idcard",
//                        "issuer": {
//                            "country": "DE",
//                            "name": "BA MITTE BÜA 1"
//                        }
//                    },
//                    "time": "2012-04-23T18:00:43.511+01",
//                    "type": "id_document"
//                }

			//attachments
			if(evidenceInUserinfo.has("attachments")) {
				evidence.add("attachments", JsonNull.INSTANCE);
			}

			evidenceRequest.add(evidence);
		}
		verification.add("evidence", evidenceRequest);
		rv.add("verification", verification);
		return rv;
	}

	protected JsonObject getConstrainableElementWithValue(JsonElement valueInUserinfo) {
		JsonObject rv = new JsonObject();
		rv.addProperty("value", OIDFJSON.getString(valueInUserinfo));
		return rv;
	}
}
