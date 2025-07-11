package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractCreateVerifiedClaimsRequestFromResponseObject extends AbstractCondition {

	protected JsonObject createVerificationClaims(JsonObject verificationObject) {
		if(!verificationObject.has("trust_framework")) {
			throw error("verification requires trust_framework", args("verification", verificationObject));
		}
		JsonObject verificationClaims = getJsonObjectSubElementsWithConstrainableElementValue(verificationObject, "trust_framework", "assurance_level", "verification_process", "time");

		// TODO TODO add time? e.g calulate max_age from time in userinfo and request values that satisfy or does not satisfy the max_age

		// assurance_process
		if(verificationObject.has("assurance_process")) {
			JsonObject assuranceProcessClaims = createAssuranceProcessClaims(verificationObject.getAsJsonObject("assurance_process"));
			verificationClaims.add("assurance_process", assuranceProcessClaims);
		}

		// evidence
		if(verificationObject.has("evidence")) {
			verificationClaims.add("evidence", createEvidenceClaimsArray(verificationObject.get("evidence")));
		}

		return verificationClaims;
	}

	protected JsonObject createAssuranceProcessClaims(JsonObject assuranceProcessObject) {
		JsonObject assuranceProcess = getJsonObjectSubElementsWithConstrainableElementValue(assuranceProcessObject, "policy", "procedure");

		// assurance_details is an array
		if (assuranceProcessObject.has("assurance_details")) {
			JsonElement assuranceDetailsInAssuranceProcess = assuranceProcessObject.get("assurance_details");
			if (!assuranceDetailsInAssuranceProcess.isJsonArray()) {
				throw error("assurance_details must be an array of objects", args("assurance_details", assuranceDetailsInAssuranceProcess));
			}
			JsonArray assuranceDetailsArrayInAssuranceProcess = assuranceDetailsInAssuranceProcess.getAsJsonArray();
			JsonArray assuranceDetailsArray = new JsonArray();
			for (JsonElement assuranceDetailsElementInAssuranceProcess : assuranceDetailsArrayInAssuranceProcess) {
				if (!assuranceDetailsElementInAssuranceProcess.isJsonObject()) {
					throw error("Items in assurance_details must be objects", args("assurance_details", assuranceDetailsInAssuranceProcess));
				}
				JsonObject assuranceDetailsElementInAssuranceProcessAsJsonObject = assuranceDetailsElementInAssuranceProcess.getAsJsonObject();
				JsonObject assuranceDetailsObject = getJsonObjectSubElementsWithConstrainableElementValue(assuranceDetailsElementInAssuranceProcessAsJsonObject, "assurance_type", "assurance_classification");

				// evidence_ref is an array
				if (assuranceDetailsElementInAssuranceProcessAsJsonObject.has("evidence_ref")) {
					JsonElement evidenceRefInAssuranceDetailsElement = assuranceDetailsElementInAssuranceProcessAsJsonObject.get("evidence_ref");
					if (!evidenceRefInAssuranceDetailsElement.isJsonArray()) {
						throw error("evidence_ref must be an array", args("evidence_ref", evidenceRefInAssuranceDetailsElement));
					}
					JsonArray evidenceRefInAssuranceDetailsArray = evidenceRefInAssuranceDetailsElement.getAsJsonArray();
					JsonArray evidenceRefArray = new JsonArray();
					for (JsonElement evidenceRefInAssuranceDetailsArrayItem : evidenceRefInAssuranceDetailsArray) {
						if (!evidenceRefInAssuranceDetailsArrayItem.isJsonObject()) {
							throw error("evidence_ref items must be objects", args("evidence_ref", evidenceRefInAssuranceDetailsArrayItem));
						}
						JsonObject evidenceRefInAssuranceDetailsArrayItemAsObject = evidenceRefInAssuranceDetailsArrayItem.getAsJsonObject();
						JsonObject evidenceRefItemObject = getJsonObjectSubElementsWithConstrainableElementValue(evidenceRefInAssuranceDetailsArrayItemAsObject, "check_id");
						if (!evidenceRefItemObject.has("check_id")) {
							throw error("evidence_ref items require check_id", args("evidence_ref", evidenceRefInAssuranceDetailsArrayItemAsObject));
						}
						// evidence_metadata
						if (evidenceRefInAssuranceDetailsArrayItemAsObject.has("evidence_metadata")) {
							JsonElement evidenceMetadataInEvidenceRefInAssuranceDetailsArrayItem = evidenceRefInAssuranceDetailsArrayItemAsObject.get("evidence_metadata");
							if (!evidenceMetadataInEvidenceRefInAssuranceDetailsArrayItem.isJsonObject()) {
								throw error("evidence_metadata must be a JSON object", args("evidence_metadata", evidenceMetadataInEvidenceRefInAssuranceDetailsArrayItem));
							}
							JsonObject evidenceMetadata = getJsonObjectSubElementsWithConstrainableElementValue(evidenceMetadataInEvidenceRefInAssuranceDetailsArrayItem.getAsJsonObject(), "evidence_classification");
							evidenceRefItemObject.add("evidence_metadata", evidenceMetadata);
						}
						evidenceRefArray.add(evidenceRefItemObject);
					}
					assuranceDetailsObject.add("evidence_ref", evidenceRefArray);
				}
				assuranceDetailsArray.add(assuranceDetailsObject);
			}
			assuranceProcess.add("assurance_details", assuranceDetailsArray);
		}
		return assuranceProcess;
	}

	protected JsonArray createEvidenceClaimsArray(JsonElement refEvidencesElement) {
		if(!refEvidencesElement.isJsonArray()) {
			throw error("evidence must be an array", args("evidence", refEvidencesElement));
		}
		JsonArray evidenceClaimsArray = new JsonArray();
		JsonArray refEvidencesArray = refEvidencesElement.getAsJsonArray();
		for(JsonElement refEvidenceElement : refEvidencesArray) {
			evidenceClaimsArray.add(creatEvidenceClaimsObject(refEvidenceElement.getAsJsonObject()));
		}
		return evidenceClaimsArray;
	}

	protected JsonObject creatEvidenceClaimsObject(JsonObject refEvidenceObject) {

		if(!refEvidenceObject.has("type")) {
			throw error("evidence object must contain type", args("evidence", refEvidenceObject));
		}
		JsonObject evidenceObject = null;
		switch (OIDFJSON.getString(refEvidenceObject.get("type"))) {
			case "document" :
				evidenceObject = createDocumentEvidenceClaimsObject(refEvidenceObject);
				break;

			case "electronic_record":
				evidenceObject = createElectronicRecordEvidenceClaimsObject(refEvidenceObject);
				break;

			case "vouch":
				evidenceObject = createVouchEvidenceClaimsObject(refEvidenceObject);
				break;

			case "electronic_signature":
				evidenceObject = createElectronicSignatureEvdenceClaimsObject(refEvidenceObject);
				break;

			default:
				throw error("invalid evidence type", args("evidence", refEvidenceObject));
		}

		//TODO handle attachments

		return evidenceObject;
	}


	protected JsonObject createDocumentEvidenceClaimsObject(JsonObject refEvidenceObject) {
		JsonObject documentEvidenceClaims = getJsonObjectSubElementsWithConstrainableElementValue(refEvidenceObject, "type");
		if(refEvidenceObject.has("check_details")) {
			documentEvidenceClaims.add("check_details", createEvidenceCheckDetailsClaimsArray(refEvidenceObject.getAsJsonArray("check_details")));
		}

		if(refEvidenceObject.has("document_details")) {
			documentEvidenceClaims.add("document_details", createEvidenceDocumentDetailsClaimsObject(refEvidenceObject.getAsJsonObject("document_details")));
		}
		return documentEvidenceClaims;
	}

	protected JsonArray createEvidenceCheckDetailsClaimsArray(JsonArray refCheckDetailsArray) {
		JsonArray checkDetailsArray = new JsonArray();
		for(JsonElement refCheckDetailsElement : refCheckDetailsArray) {
			if(!refCheckDetailsElement.getAsJsonObject().has("check_method")) {
				throw error("check_details must contain check_method", args("check_details", refCheckDetailsElement));
			}
			checkDetailsArray.add(getJsonObjectSubElementsWithConstrainableElementValue(refCheckDetailsElement.getAsJsonObject(), "check_method", "organization", "check_id", "time"));
		}
		return checkDetailsArray;
	}

	protected JsonObject createEvidenceDocumentDetailsClaimsObject(JsonObject refDocumentDetailsObject) {
		if(!refDocumentDetailsObject.has("type")) {
			throw error("document_details must contain type", args("document_details", refDocumentDetailsObject));
		}
		JsonObject documentDetailsClaims = getJsonObjectSubElementsWithConstrainableElementValue(refDocumentDetailsObject, "type", "document_number", "serial_number", "date_of_issuance", "date_of_expiry");
		if(refDocumentDetailsObject.has("issuer")) {
			JsonObject issuerObject = createIssuerClaimsObject(refDocumentDetailsObject.getAsJsonObject("issuer"));
			documentDetailsClaims.add("issuer", issuerObject);
		}

		// TODO add derived_claims???
		return documentDetailsClaims;
	}

	protected  JsonObject createIssuerClaimsObject(JsonObject refIssuerObject) {
		String[] issuerClaimsList = {"name", "country_code", "jurisdiction",
			// address claims
			"formatted", "street_address", "locality", "region", "postal_code", "country"
		};
		return getJsonObjectSubElementsWithConstrainableElementValue(refIssuerObject, issuerClaimsList);
	}

	protected JsonObject createElectronicRecordEvidenceClaimsObject(JsonObject refEvidenceObject) {
		JsonObject electronicRecordClaims = getJsonObjectSubElementsWithConstrainableElementValue(refEvidenceObject, "type");
		if(refEvidenceObject.has("check_details")) {
			electronicRecordClaims.add("check_details", createEvidenceCheckDetailsClaimsArray(refEvidenceObject.getAsJsonArray("check_details")));
		}
		if(refEvidenceObject.has("record")) {
			electronicRecordClaims.add("record", createElectronicRecordRecordClaimsObject(refEvidenceObject.getAsJsonObject("record")));
		}
		return electronicRecordClaims;
	}

	protected  JsonObject createElectronicRecordRecordClaimsObject(JsonObject refRecordObject) {
		if(!refRecordObject.has("type")) {
			throw error("record object must contain type", args("record", refRecordObject));
		}
		JsonObject electronicRecordClaims = getJsonObjectSubElementsWithConstrainableElementValue(refRecordObject,
			"type", "created_at", "date_of_expiry");
		if(refRecordObject.has("source")) {
			electronicRecordClaims.add("source", createElectronicRecordSourceClaimsObject(refRecordObject.getAsJsonObject("source")));
		}

		// TODO add derived_claims???

		return electronicRecordClaims;
	}

	protected  JsonObject createElectronicRecordSourceClaimsObject(JsonObject refSourceObject) {
		String[] sourceClaimsList = {"name", "country_code", "jurisdiction",
			// address claims
			"formatted", "street_address", "locality", "region", "postal_code", "country"
		};
		return getJsonObjectSubElementsWithConstrainableElementValue(refSourceObject, sourceClaimsList);
	}

	protected JsonObject createVouchEvidenceClaimsObject(JsonObject refEvidenceObject) {
		JsonObject vouchEvidenceClaims = getJsonObjectSubElementsWithConstrainableElementValue(refEvidenceObject, "type");
		if(refEvidenceObject.has("check_details")) {
			vouchEvidenceClaims.add("check_details", createEvidenceCheckDetailsClaimsArray(refEvidenceObject.getAsJsonArray("check_details")));
		}
		if(refEvidenceObject.has("attestation")) {
			vouchEvidenceClaims.add("attestation", createVouchAttestationClaimsObject(refEvidenceObject.getAsJsonObject("attestation")));
		}

		return vouchEvidenceClaims;
	}

	protected JsonObject createVouchAttestationClaimsObject(JsonObject refVouchAttestationObject) {
		if(!refVouchAttestationObject.has("type")) {
			throw error("vouch attestation must contain type", args("vouch attestation", refVouchAttestationObject));
		}
		JsonObject attestationClaims = getJsonObjectSubElementsWithConstrainableElementValue(refVouchAttestationObject,
			"type", "reference_number", "date_of_issuance", "date_of_expiry");
		if(refVouchAttestationObject.has("voucher")) {
			attestationClaims.add("voucher", createVouchAttestationVoucherClaimsObject(refVouchAttestationObject.getAsJsonObject("voucher")));
		}

		// TODO add derived_claims???

		return attestationClaims;
	}

	protected JsonObject createVouchAttestationVoucherClaimsObject(JsonObject refVoucherObject) {
		String[] voucherClaimsList = {"name", "birthdate", "country_code", "occupation", "organization",
			// address claims
			"formatted", "street_address", "locality", "region", "postal_code", "country"
		};
		return getJsonObjectSubElementsWithConstrainableElementValue(refVoucherObject, voucherClaimsList);
	}

	private JsonObject createElectronicSignatureEvdenceClaimsObject(JsonObject refEvidenceObject) {
		String[] requireClaims = { "type", "signature_type", "issuer", "serial_number"};
		for(String claim : requireClaims) {
			if(!refEvidenceObject.has(claim)) {
				throw error("electronic_signature requires " + claim, args("electronic_signature", refEvidenceObject));
			}
		}
		JsonObject electronicSignatureClaims = getJsonObjectSubElementsWithConstrainableElementValue(refEvidenceObject, requireClaims);
		if(refEvidenceObject.has("created_at")) {
			electronicSignatureClaims.add("created_at", getConstrainableElementWithValue(refEvidenceObject.get("created_at")));
		}

		// TODO add derived_claims???

		return electronicSignatureClaims;
	}



	protected JsonObject getJsonObjectSubElementsWithConstrainableElementValue(JsonObject refJsonObject, String ... elementsList) {
		JsonObject retJsonObject = new JsonObject();
		if(refJsonObject != null) {
			for(String element : elementsList) {
				if(refJsonObject.has(element)) {
					retJsonObject.add(element, getConstrainableElementWithValue(refJsonObject.get(element)));
				}
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
