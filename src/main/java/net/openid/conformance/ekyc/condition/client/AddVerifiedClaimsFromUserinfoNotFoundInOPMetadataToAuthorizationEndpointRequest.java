package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

// FIXME rename to AddVerifiedClaimsRequestToRequestSuppliedUserDataToAuthorizationEndpointRequest ?
public class AddVerifiedClaimsFromUserinfoNotFoundInOPMetadataToAuthorizationEndpointRequest extends AbstractCondition {


	@Override
	@PreEnvironment(required = {"server", "config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request", strings = "userinfo_contains_data_notfoundin_opmetadata")
	public Environment evaluate(Environment env) {
		JsonElement userinfo = env.getElementFromObject("config", "ekyc_userinfo");
		if(userinfo==null){
			throw error("User provided userinfo data is not set in configuration");
		}
		JsonElement verifiedClaims = createVerifiedClaimsRequestFromUserinfo(userinfo.getAsJsonObject(), env);
		if(verifiedClaims==null) {
			log("All claims and/or verification data in userinfo match OP metadata.");
			env.putString("userinfo_contains_data_notfoundin_opmetadata", "no");
			return env;
		}
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
		logSuccess("Added verified_claims based on provided userinfo to authorization request, " +
				"specifically requesting user claims and/or verification data not found in OP metadata",
			args("authorization_endpoint_request",
				env.getObject("authorization_endpoint_request")));
		env.putString("userinfo_contains_data_notfoundin_opmetadata", "yes");
		return env;
	}

	protected JsonElement createVerifiedClaimsRequestFromUserinfo(JsonObject userinfo, Environment env) {
		JsonElement verifiedClaimsElementInUserinfo = userinfo.get("verified_claims");
		if(verifiedClaimsElementInUserinfo==null){
			throw error("userinfo must contain a verified_claims element");
		}
		if(verifiedClaimsElementInUserinfo.isJsonObject()) {
			JsonObject newRequestElement = createVerifiedClaimsFromVerifiedClaimsObjectIfNotFoundInOPMetadata(verifiedClaimsElementInUserinfo.getAsJsonObject(), env);
			return newRequestElement;
		} else if(verifiedClaimsElementInUserinfo.isJsonArray()) {
			JsonArray array = verifiedClaimsElementInUserinfo.getAsJsonArray();
			JsonArray requestClaims = new JsonArray();
			for(JsonElement element : array) {
				JsonObject newRequestElement = createVerifiedClaimsFromVerifiedClaimsObjectIfNotFoundInOPMetadata(element.getAsJsonObject(), env);
				if(newRequestElement!=null) {
					requestClaims.add(newRequestElement);
				}
			}
			if(requestClaims.size()<1) {
				return null;
			}
			return requestClaims;
		} else {
			throw error("Unexpected verified_claims in userinfo, must be either an array or object",
						args("userinfo", userinfo));
		}
	}

	protected JsonObject createVerifiedClaimsFromVerifiedClaimsObjectIfNotFoundInOPMetadata(JsonObject verifiedClaimsObjectFromUserinfo, Environment env) {
		JsonObject rv = new JsonObject();
		JsonObject claims = new JsonObject();
		JsonObject opMetadata = env.getObject("server");
		JsonObject claimsInUserinfo = verifiedClaimsObjectFromUserinfo.get("claims").getAsJsonObject();
		int claimsNotFoundInClaimsInVerifiedClaimsSupported = 0;
		int verificationElementMismatchCount = 0;
		JsonArray claimsInVerifiedClaimsSupported = opMetadata.get("claims_in_verified_claims_supported").getAsJsonArray();
		for(String claimName : claimsInUserinfo.keySet()) {
			claims.add(claimName, JsonNull.INSTANCE);
			if(!claimsInVerifiedClaimsSupported.contains(new JsonPrimitive(claimName))) {
				claimsNotFoundInClaimsInVerifiedClaimsSupported++;
			}
		}
		rv.add("claims", claims);
		JsonObject verification = new JsonObject();
		JsonArray trustFrameworksSupported = opMetadata.get("trust_frameworks_supported").getAsJsonArray();

		JsonObject verificationInUserinfo = verifiedClaimsObjectFromUserinfo.get("verification").getAsJsonObject();

		if(!trustFrameworksSupported.contains(verificationInUserinfo.get("trust_framework"))){
			verificationElementMismatchCount++;
		}
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

		JsonArray evidenceSupported = null;
		if(opMetadata.has("evidence_supported")){
			evidenceSupported = opMetadata.get("evidence_supported").getAsJsonArray();
		} else {
			evidenceSupported = new JsonArray();
		}
		JsonArray attachmentsSupported = null;
		if(opMetadata.has("attachments_supported")){
			attachmentsSupported = opMetadata.get("attachments_supported").getAsJsonArray();
		} else {
			attachmentsSupported = new JsonArray();
		}
		JsonArray evidencesInUserinfo = verificationInUserinfo.get("evidence").getAsJsonArray();
		JsonArray evidenceRequest = new JsonArray();
		for(JsonElement evidenceElementFromUserinfo : evidencesInUserinfo) {
			JsonObject evidenceInUserinfo = evidenceElementFromUserinfo.getAsJsonObject();

			if(!evidenceSupported.contains(evidenceInUserinfo.get("type"))) {
				verificationElementMismatchCount++;
				JsonObject evidence = new JsonObject();
				//type
				JsonObject evidenceType = new JsonObject();
				evidenceType.addProperty("value", OIDFJSON.getString(evidenceInUserinfo.get("type")));
				evidence.add("type", evidenceType);
				//TODO add evidence type specific items

				//attachments
				if (evidenceInUserinfo.has("attachments")) {
					evidence.add("attachments", JsonNull.INSTANCE);
					JsonArray attachmentsInUserinfo = evidenceInUserinfo.getAsJsonArray();
					for(JsonElement attachmentElement : attachmentsInUserinfo) {
						JsonObject attachmentObject = attachmentElement.getAsJsonObject();
						if(attachmentObject.has("url")) {
							//external attachment
							if(!attachmentsSupported.contains(new JsonPrimitive("external"))) {
								verificationElementMismatchCount++;
							}
						} else if(attachmentObject.has("content")) {
							//embedded
							if(!attachmentsSupported.contains(new JsonPrimitive("embedded"))) {
								verificationElementMismatchCount++;
							}
						} else {
							throw error("Unexpected attachment in userinfo", args("attachment", attachmentElement));
						}
					}
				}

				evidenceRequest.add(evidence);
			}
		}
		if(evidenceRequest.size()>0) {
			verification.add("evidence", evidenceRequest);
		}
		rv.add("verification", verification);
		if(verificationElementMismatchCount==0 && claimsNotFoundInClaimsInVerifiedClaimsSupported==0) {
			//nothing to do return null
			return null;
		}
		return rv;
	}

	protected JsonObject getConstrainableElementWithValue(JsonElement valueInUserinfo) {
		JsonObject rv = new JsonObject();
		rv.addProperty("value", OIDFJSON.getString(valueInUserinfo));
		return rv;
	}
}
