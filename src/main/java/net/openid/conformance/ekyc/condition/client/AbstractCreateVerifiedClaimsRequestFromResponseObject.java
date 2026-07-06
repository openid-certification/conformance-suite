package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractCreateVerifiedClaimsRequestFromResponseObject extends AbstractCondition {

	protected JsonObject createVerificationClaims(JsonObject verificationObject) {
		if(!verificationObject.has("trust_framework")) {
			throw error("verification requires trust_framework", args("verification", verificationObject));
		}
		// trust_framework and assurance_level are constrainable_element (value permitted);
		// verification_process is a simple_element and time is a datetime_element, neither of
		// which permits a value constraint, so they are requested as null.
		JsonObject verificationClaims = getJsonObjectSubElementsWithConstrainableElementValue(verificationObject, "trust_framework", "assurance_level");
		addNonConstrainableElements(verificationClaims, verificationObject, "verification_process", "time");

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
			JsonObject refCheckDetails = refCheckDetailsElement.getAsJsonObject();
			// check_method, organization and check_id are constrainable_element (value permitted);
			// time is a datetime_element and is requested as null.
			JsonObject checkDetails = getJsonObjectSubElementsWithConstrainableElementValue(refCheckDetails, "check_method", "organization", "check_id");
			addNonConstrainableElements(checkDetails, refCheckDetails, "time");
			checkDetailsArray.add(checkDetails);
		}
		return checkDetailsArray;
	}

	protected JsonObject createEvidenceDocumentDetailsClaimsObject(JsonObject refDocumentDetailsObject) {
		if(!refDocumentDetailsObject.has("type")) {
			throw error("document_details must contain type", args("document_details", refDocumentDetailsObject));
		}
		// type is a constrainable_element (value permitted); document_number and serial_number are
		// simple_element and date_of_issuance/date_of_expiry are datetime_element, none of which
		// permit a value constraint, so they are requested as null.
		JsonObject documentDetailsClaims = getJsonObjectSubElementsWithConstrainableElementValue(refDocumentDetailsObject, "type");
		addNonConstrainableElements(documentDetailsClaims, refDocumentDetailsObject, "document_number", "serial_number", "date_of_issuance", "date_of_expiry");
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
		// All issuer sub-elements are simple_element; they do not permit a value constraint.
		JsonObject issuerClaims = new JsonObject();
		addNonConstrainableElements(issuerClaims, refIssuerObject, issuerClaimsList);
		return issuerClaims;
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
		// type is a constrainable_element (value permitted); created_at and date_of_expiry are
		// datetime_element and are requested as null.
		JsonObject electronicRecordClaims = getJsonObjectSubElementsWithConstrainableElementValue(refRecordObject, "type");
		addNonConstrainableElements(electronicRecordClaims, refRecordObject, "created_at", "date_of_expiry");
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
		// All source sub-elements are simple_element; they do not permit a value constraint.
		JsonObject sourceClaims = new JsonObject();
		addNonConstrainableElements(sourceClaims, refSourceObject, sourceClaimsList);
		return sourceClaims;
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
		// type is a constrainable_element (value permitted); reference_number is a simple_element and
		// date_of_issuance/date_of_expiry are datetime_element, none of which permit a value
		// constraint, so they are requested as null.
		JsonObject attestationClaims = getJsonObjectSubElementsWithConstrainableElementValue(refVouchAttestationObject, "type");
		addNonConstrainableElements(attestationClaims, refVouchAttestationObject, "reference_number", "date_of_issuance", "date_of_expiry");
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
		// All voucher sub-elements are simple_element (birthdate is a datetime_element); none permit
		// a value constraint.
		JsonObject voucherClaims = new JsonObject();
		addNonConstrainableElements(voucherClaims, refVoucherObject, voucherClaimsList);
		return voucherClaims;
	}

	private JsonObject createElectronicSignatureEvdenceClaimsObject(JsonObject refEvidenceObject) {
		String[] requireClaims = { "type", "signature_type", "issuer", "serial_number"};
		for(String claim : requireClaims) {
			if(!refEvidenceObject.has(claim)) {
				throw error("electronic_signature requires " + claim, args("electronic_signature", refEvidenceObject));
			}
		}
		// type is the evidence discriminator (an object carrying value); signature_type, issuer and
		// serial_number are simple_element and created_at is a datetime_element, none of which permit
		// a value constraint, so they are requested as null.
		JsonObject electronicSignatureClaims = getJsonObjectSubElementsWithConstrainableElementValue(refEvidenceObject, "type");
		addNonConstrainableElements(electronicSignatureClaims, refEvidenceObject, "signature_type", "issuer", "serial_number", "created_at");

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

	/**
	 * Adds each present element from {@code refJsonObject} to {@code target} as a non-value
	 * request element. The IDA verified_claims request schema models these as {@code simple_element}
	 * or {@code datetime_element}, which permit only {@code null} or an object with
	 * {@code essential} ({@code datetime_element} additionally allows {@code max_age}) — they MUST NOT
	 * carry {@code value}/{@code values}. They are requested as {@code null} (unconstrained),
	 * consistent with how the individual user claims are requested.
	 */
	protected void addNonConstrainableElements(JsonObject target, JsonObject refJsonObject, String ... elementsList) {
		if(refJsonObject != null) {
			for(String element : elementsList) {
				if(refJsonObject.has(element)) {
					target.add(element, JsonNull.INSTANCE);
				}
			}
		}
	}
}
