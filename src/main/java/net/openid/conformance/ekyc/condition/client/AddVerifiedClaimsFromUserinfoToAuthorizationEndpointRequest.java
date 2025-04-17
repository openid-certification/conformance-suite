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

import java.util.Arrays;
import java.util.List;

public class AddVerifiedClaimsFromUserinfoToAuthorizationEndpointRequest extends AbstractCondition {


	@Override
	@PreEnvironment(required = {"config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement userinfo = env.getElementFromObject("config", "ekyc.userinfo");
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
			JsonObject assuranceProcessInUserinfo = verificationInUserinfo.get("assurance_process").getAsJsonObject();
			JsonObject assuranceProcess = getJsonObjectSubElementsWithConstrainableElementValue(assuranceProcessInUserinfo, "policy", "procedure");

			// assurance_details is an array
			if(assuranceProcessInUserinfo.has("assurance_details")) {
				JsonElement assuranceDetailsInAssuranceProcess = assuranceProcessInUserinfo.get("assurance_details");
				if(assuranceDetailsInAssuranceProcess.isJsonArray()) {
					JsonArray assuranceDetailsArrayInAssuranceProcess = assuranceDetailsInAssuranceProcess.getAsJsonArray();
					JsonArray assuranceDetailsArray = new JsonArray();
					for(JsonElement assuranceDetailsElementInAssuranceProcess : assuranceDetailsArrayInAssuranceProcess) {
						if(assuranceDetailsElementInAssuranceProcess.isJsonObject()) {
							JsonObject assuranceDetailsElementInAssuranceProcessAsJsonObject = assuranceDetailsElementInAssuranceProcess.getAsJsonObject();
							JsonObject assuranceDetailsObject = getJsonObjectSubElementsWithConstrainableElementValue(assuranceDetailsElementInAssuranceProcessAsJsonObject, "assurance_type", "assurance_classification");

							// evidence_ref is an array
							if(assuranceDetailsElementInAssuranceProcessAsJsonObject.has("evidence_ref")) {
								JsonElement evidenceRefInAssuranceDetailsElement = assuranceDetailsElementInAssuranceProcessAsJsonObject.get("evidence_ref");
								if(evidenceRefInAssuranceDetailsElement.isJsonArray()) {
									JsonArray evidenceRefInAssuranceDetailsArray = evidenceRefInAssuranceDetailsElement.getAsJsonArray();
									JsonArray evidenceRefArray = new JsonArray();
									for(JsonElement evidenceRefInAssuranceDetailsArrayItem : evidenceRefInAssuranceDetailsArray) {
										if(evidenceRefInAssuranceDetailsArrayItem.isJsonObject()) {
											JsonObject evidenceRefInAssuranceDetailsArrayItemAsObject = evidenceRefInAssuranceDetailsArrayItem.getAsJsonObject();
											JsonObject evidenceRefItemObject = getJsonObjectSubElementsWithConstrainableElementValue(evidenceRefInAssuranceDetailsArrayItemAsObject, "check_id");
											if(!evidenceRefItemObject.has("check_id")) {
												throw error("evidence_ref items require check_id", args("evidence_ref", evidenceRefInAssuranceDetailsArrayItemAsObject));
											}
											// evidence_metadata
											if(evidenceRefInAssuranceDetailsArrayItemAsObject.has("evidence_metadata")) {
												JsonElement evidenceMetadataInEvidenceRefInAssuranceDetailsArrayItem = evidenceRefInAssuranceDetailsArrayItemAsObject.get("evidence_metadata");
												if(!evidenceMetadataInEvidenceRefInAssuranceDetailsArrayItem.isJsonObject()) {
													throw error("evidence_metadat must be a JSON object", args("evidence_metadata", evidenceMetadataInEvidenceRefInAssuranceDetailsArrayItem));
												} else {
													JsonObject evidenceMetadata = getJsonObjectSubElementsWithConstrainableElementValue(evidenceMetadataInEvidenceRefInAssuranceDetailsArrayItem.getAsJsonObject(), "evidence_classification");
													evidenceRefItemObject.add("evidence_metadata", evidenceMetadata);
												}
											}
											evidenceRefArray.add(evidenceRefItemObject);
										} else {
											throw error("evidence_ref items must be objects", args("evidence_ref", evidenceRefInAssuranceDetailsArrayItem));
										}
									}
									assuranceDetailsObject.add("evidence_ref", evidenceRefArray);

								} else {
									throw error("evidence_ref must be an array", args("evidence_ref", evidenceRefInAssuranceDetailsElement));
								}
							}
							assuranceDetailsArray.add(assuranceDetailsObject);
						} else {
							throw error("Items in assurance_details must be objects", args("assurance_details", assuranceDetailsInAssuranceProcess));
						}

					}
					assuranceProcess.add("assurance_details", assuranceDetailsArray);
				} else {
					throw error("assurance_details must be an array of objects", args("assurance_details", assuranceDetailsInAssuranceProcess));
				}
			}
			verification.add("assurance_process", assuranceProcess);
		}
		//TODO add time? e.g calulate max_age from time in userinfo and request values that satisfy or does not satisfy the max_age


		if(verificationInUserinfo.has("verification_process")) {
			verification.add("verification_process", getConstrainableElementWithValue(verificationInUserinfo.get("verification_process")));
		}

		if (verificationInUserinfo.has("evidence")) {
			JsonArray evidencesInUserinfo = verificationInUserinfo.get("evidence").getAsJsonArray();
			JsonArray evidenceRequest = new JsonArray();
			for(JsonElement evidenceElementFromUserinfo : evidencesInUserinfo) {
				JsonObject evidence = new JsonObject();
				JsonObject evidenceInUserinfo = evidenceElementFromUserinfo.getAsJsonObject();

				//type
				JsonObject evidenceType = new JsonObject();
				evidenceType.addProperty("value", OIDFJSON.getString(evidenceInUserinfo.get("type")));
				evidence.add("type", evidenceType);
				//TODO eKYC add evidence type specific items (document, electronic_record, vouch, electronic_signature)
				if(OIDFJSON.getString(evidenceInUserinfo.get("type")).equals("document")) {
					if(evidenceInUserinfo.has("document_details")) {
						JsonObject documentDetailsInEvidence = evidenceInUserinfo.getAsJsonObject("document_details");
						JsonObject documentDetails = new JsonObject();
						documentDetails.add("type", getConstrainableElementWithValue(documentDetailsInEvidence.get("type")));

						List<String> documentDetailsElements = Arrays.asList("document_number", "serial_number", "date_of_issuance", "date_of_expiry" );
						for(String documentDetailsElement : documentDetailsElements ) {
							if(documentDetailsInEvidence.has(documentDetailsElement)) {
								documentDetails.add(documentDetailsElement, JsonNull.INSTANCE);
							}
						}

						if(documentDetailsInEvidence.has("issuer")) {
							JsonObject issuer = new JsonObject();
							JsonObject issuerInDocumentDetails = documentDetailsInEvidence.getAsJsonObject("issuer");
							List<String> issuerElements = Arrays.asList("name", "country_code", "jurisdiction", "date_of_issuance", "date_of_expiry" /* "address */);
							for(String issuerElement : issuerElements) {
								if(issuerInDocumentDetails.has(issuerElement)) {
									issuer.add(issuerElement, JsonNull.INSTANCE);
								}
							}
							documentDetails.add("issuer", issuer);
						}

						// TODO add derived claims object

						evidence.add("document_details", documentDetails);
					}
				}

				//attachments
				if(evidenceInUserinfo.has("attachments")) {
					evidence.add("attachments", JsonNull.INSTANCE);
				}

				evidenceRequest.add(evidence);
			}
			verification.add("evidence", evidenceRequest);
		}
		rv.add("verification", verification);
		return rv;
	}



	private JsonObject getJsonObjectSubElementsWithConstrainableElementValue(JsonObject refJsonObject, String ... elementsList) {
		JsonObject retJsonObject = new JsonObject();
		for(String element : elementsList) {
			if(refJsonObject.has(element)) {
				retJsonObject.add(element, getConstrainableElementWithValue(refJsonObject.get(element)));
			}
		}
		return retJsonObject;
	}

	protected JsonObject getConstrainableElementWithValue(JsonElement valueInUserinfo) {
		JsonObject rv = new JsonObject();
		rv.addProperty("value", OIDFJSON.getString(valueInUserinfo));
		return rv;
	}
}
