package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractValidateVerifiedClaimsResponseAgainstOPMetadata extends AbstractCondition {

	protected Environment validate(Environment env, String location) {
		JsonObject verifiedClaimsResponse = env.getObject("verified_claims_response");
		JsonElement claimsElement = null;

		if(verifiedClaimsResponse.has(location)) {
			claimsElement = verifiedClaimsResponse.get(location);
		}
		if(claimsElement==null) {
			throw error("Could not find verified_claims in " + location);
		}
		JsonObject opMetadata = env.getObject("server");

		if(claimsElement.isJsonArray()) {
			for(JsonElement element : claimsElement.getAsJsonArray()) {
				validateVerifiedClaimsObject(element.getAsJsonObject(), opMetadata);
			}
		} else if(claimsElement.isJsonObject()) {
			validateVerifiedClaimsObject(claimsElement.getAsJsonObject(), opMetadata);
		} else {
			throw error("verified_claims is neither an array nor an object",
				args("verified_claims", claimsElement));
		}
		logSuccess("Verified claims are valid", args("location", location, "verified_claims", claimsElement));
		return env;
	}

	/**
	 * Validate a single verified_claims json object
	 * Requires that returned elements are advertised in OP metadata
	 * @param verifiedClaims
	 * @param opMetadata
	 */
	protected void validateVerifiedClaimsObject(JsonObject verifiedClaims, JsonObject opMetadata) {
		JsonArray supportedTrustFrameworks = opMetadata.get("trust_frameworks_supported").getAsJsonArray();
		JsonObject verification = verifiedClaims.get("verification").getAsJsonObject();
		if(supportedTrustFrameworks.contains(verification.get("trust_framework"))){
			logSuccess("trust_framework is one of the trust_frameworks_supported values",
				args("trust_framework", verifiedClaims.get("trust_framework"),
					"trust_frameworks_supported", supportedTrustFrameworks));
		} else {
			throw error("trust_framework is not one of the supported values advertised in OP metadata",
				args("verified_claims", verifiedClaims,
					"trust_frameworks_supported", supportedTrustFrameworks));
		}

		JsonArray evidenceSupported = opMetadata.get("evidence_supported") != null ? opMetadata.get("evidence_supported").getAsJsonArray() : null;
		JsonElement evidenceArrayElement = verification.get("evidence");
		if(evidenceArrayElement!=null) {
			if(!evidenceArrayElement.isJsonArray()) {
				throw error("evidence must be an array", args("actual", evidenceArrayElement));
			}
			if(evidenceSupported == null) {
				throw error("Evidence is returned but evidence_supported could not be found in OP metadata", args("evidence", evidenceArrayElement));
			}
			JsonArray evidences = evidenceArrayElement.getAsJsonArray();
			for (JsonElement evidenceElement : evidences) {
				JsonObject evidence = evidenceElement.getAsJsonObject();
				if (evidenceSupported.contains(evidence.get("type"))) {
					logSuccess("Evidence type is one of the supported values advertised in OP metadata",
						args("evidence_type", evidence.get("type"), "evidence_supported", evidenceSupported));
				} else {
					throw error("Evidence type is not one of the supported values advertised in OP metadata",
						args("evidence_type", evidence.get("type"), "evidence_supported", evidenceSupported));
				}
			}
			validateDocumentsSupported (opMetadata, evidences);
			validateEvidenceCheckDetailsCheckMethodsSupported(opMetadata, evidences);
			validateElectronicRecordsSupported (opMetadata, evidences);
		}

		//claims_in_verified_claims_supported: REQUIRED. JSON array containing all Claims supported within verified_claims.
		JsonObject claims = verifiedClaims.get("claims").getAsJsonObject();
		JsonArray supportedClaims = opMetadata.get("claims_in_verified_claims_supported").getAsJsonArray();
		for(String claimName : claims.keySet()) {
			if(!supportedClaims.contains(new JsonPrimitive(claimName))){
				throw error("Returned claim not found in claims_in_verified_claims_supported",
					args("claim_name", claimName, "claims_in_verified_claims_supported", supportedClaims));
			}
		}
		//if we reached here, it's okay
		logSuccess("Only claims advertised in OP metadata are returned",
			args("claims", claims, "supported_claims", supportedClaims));


		//TODO add distributed and/or aggregated claim checks
		//If the OP supports distributed and/or aggregated Claim types in verified_claims, the OP MUST advertise this in its metadata using the claim_types_supported element.
	}

	protected void validateDocumentsSupported(JsonObject opMetadata, JsonArray evidences){
		//documents_supported: REQUIRED when evidence_supported contains "document".
		// JSON array containing all identity document types utilized by the OP for identity verification.
		JsonElement documentsSupportedElement = opMetadata.get("documents_supported");
		for (JsonElement evidenceElement : evidences) {
			JsonObject evidence = evidenceElement.getAsJsonObject();
			if (evidence.get("type").equals(new JsonPrimitive("document"))) {
				JsonObject documentDetailsObject = null;
				if(evidence.has("document_details")) {
					documentDetailsObject = evidence.get("document_details").getAsJsonObject();
				}
				if(documentDetailsObject==null) {
					throw error("Evidence does not contain document_details", args("evidence", evidenceElement));
				}
				JsonElement documentType = documentDetailsObject.get("type");

				if(documentsSupportedElement==null) {
					throw error("Evidence type is " + evidence.get("type") + " but documents_supported could not be found in OP metadata");
				}

				JsonArray documentsSupported = documentsSupportedElement.getAsJsonArray();
				if (documentsSupported.contains(documentType)) {
					logSuccess("Document type is one of the supported values advertised in OP metadata",
						args("document_type", documentType, "documents_supported", documentsSupported));
				} else {
					throw error("Evidence document type is not one of the supported values advertised in OP metadata",
						args("evidence_document_type", documentType, "documents_supported", documentsSupported));
				}
			}
		}
	}

	protected void validateEvidenceCheckDetailsCheckMethodsSupported(JsonObject opMetadata, JsonArray evidences){
		//documents_check_methods_supported: OPTIONAL. JSON array containing the  check methods the OP
		// supports for evidences types "document", "electronic_record, "vouch"
		// (see @!predefined_values)
		JsonElement docCheckMethodsSupportedElement = opMetadata.get("documents_check_methods_supported");
		for (JsonElement evidenceElement : evidences) {
			JsonObject evidence = evidenceElement.getAsJsonObject();
			// only type (document, electronic_record, vouch) has a check_details, electronic_signature does not have one
			JsonElement evidenceType = evidence.get("type");
			if (evidenceType.equals(new JsonPrimitive("document")) ||
				evidenceType.equals(new JsonPrimitive("electronic_record")) ||
				evidenceType.equals(new JsonPrimitive("vouch"))) {
				JsonObject checkDetails = evidence.getAsJsonObject("check_details");
				if(checkDetails == null) {
					log("evidence does not contain check_details", args("evidence", evidence));
					continue;
				}
				JsonElement checkMethod = checkDetails.get("check_method");
				if (checkMethod == null) {
					log("evidence does not contain check_details.check_method", args("evidence", evidence));
					continue;
				}

				if(docCheckMethodsSupportedElement==null) {
					throw error("Evidence document check method is " + checkMethod + " but documents_check_methods_supported could not be found in OP metadata");
				}

				JsonArray docCheckMethodsSupported = docCheckMethodsSupportedElement.getAsJsonArray();
				if (docCheckMethodsSupported.contains(checkMethod)) {
					logSuccess("check method is one of the supported values advertised in OP metadata",
						args("check method", checkMethod, "documents_check_methods_supported", docCheckMethodsSupported));
				} else {
					throw error("check method is not one of the supported values advertised in OP metadata",
						args("check method", checkMethod, "documents_check_methods_supported", docCheckMethodsSupported));
				}
			}
		}
	}

	protected void validateElectronicRecordsSupported (JsonObject opMetadata, JsonArray evidences) {
		//TODO note it is "electronicrecord" below but I assumed it is "electronic_record" based on the rest of the document
		//electronic_records_supported: REQUIRED when evidence_supported contains "electronicrecord".
		// JSON array containing all electronic record types the OP supports (see @!predefinedvalues).
		JsonElement electronicRecordsSupportedElement = opMetadata.get("electronic_records_supported");
		for (JsonElement evidenceElement : evidences) {
			JsonObject evidence = evidenceElement.getAsJsonObject();
			if (evidence.get("type").equals(new JsonPrimitive("electronic_record"))) {
				JsonObject electronicRecord = evidence.get("electronic_record").getAsJsonObject();
				JsonElement electronicRecordType = electronicRecord.get("type");

				if(electronicRecordsSupportedElement==null) {
					throw error("Evidence type is electronic_record but electronic_records_supported could not be found in OP metadata");
				}

				JsonArray electronicRecordsSupported = electronicRecordsSupportedElement.getAsJsonArray();
				if (electronicRecordsSupported.contains(electronicRecordType)) {
					logSuccess("electronic_record type is one of the supported values advertised in OP metadata",
						args("electronic_record_type", electronicRecordType,
							"electronic_records_supported", electronicRecordsSupported));
				}
				else
				{
					throw error("electronic_record type is not one of the supported values advertised in OP metadata",
						args("electronic_record_type", electronicRecordType,
							"electronic_records_supported", electronicRecordsSupported));
				}
			}
		}
	}

	/**
	 * A single verified_claims object
	 * TODO this is an alternative method that ensures that a returned element is one of the advertised IF ADVERTISED.
	 *  I was not sure which method was more appropriate so leaving this one here just in case.
	 *  Remove if the other method is correct
	 * @param verifiedClaims
	 * @param opMetadata
	 */
	protected void validateVerifiedClaimsObjectALT(JsonObject verifiedClaims, JsonObject opMetadata) {
		JsonArray supportedTrustFrameworks = opMetadata.get("trust_frameworks_supported").getAsJsonArray();
		JsonObject verification = verifiedClaims.get("verification").getAsJsonObject();
		if(supportedTrustFrameworks.contains(verification.get("trust_framework"))){
			logSuccess("trust_framework is one of the trust_frameworks_supported values",
				args("trust_framework", verifiedClaims.get("trust_framework"),
					"trust_frameworks_supported", supportedTrustFrameworks));
		} else {
			throw error("trust_framework is not one of the supported values advertised in OP metadata",
				args("verified_claims", verifiedClaims,
				"trust_frameworks_supported", supportedTrustFrameworks));
		}

		JsonArray evidenceSupported = opMetadata.get("evidence_supported").getAsJsonArray();
		JsonElement evidenceArrayElement = verification.get("evidence");
		if(evidenceArrayElement!=null) {
			if(!evidenceArrayElement.isJsonArray()) {
				throw error("evidence must be an array", args("actual", evidenceArrayElement));
			}
			JsonArray evidences = evidenceArrayElement.getAsJsonArray();
			for (JsonElement evidenceElement : evidences) {
				JsonObject evidence = evidenceElement.getAsJsonObject();
				if (evidenceSupported.contains(evidence.get("type"))) {
					logSuccess("Evidence type is one of the supported values advertised in OP metadata",
						args("evidence_type", evidence.get("type"), "evidence_supported", evidenceSupported));
				} else {
					throw error("Evidence type is not one of the supported values advertised in OP metadata",
						args("evidence_type", evidence.get("type"), "evidence_supported", evidenceSupported));
				}
			}

			//documents_supported: REQUIRED when evidence_supported contains "document"".
			// JSON array containing all identity document types utilized by the OP for identity verification.
			if (evidenceSupported.contains(new JsonPrimitive("document"))) {
				JsonElement documentsSupportedElement = opMetadata.get("documents_supported");
				if(documentsSupportedElement==null) {
					throw error("documents_supported is REQUIRED when evidence_supported contains document" +
						"but documents_supported could not be found in OP metadata");
				}
				JsonArray documentsSupported = documentsSupportedElement.getAsJsonArray();
				for (JsonElement evidenceElement : evidences) {
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if (evidence.get("type").equals(new JsonPrimitive("document"))) {
						JsonObject documentObject = evidence.get("document").getAsJsonObject();
						JsonElement documentType = documentObject.get("type");
						if (documentsSupported.contains(documentType)) {
							logSuccess("Document type is one of the supported values advertised in OP metadata",
								args("document_type", documentType, "documents_supported", documentsSupported));
						} else {
							throw error("Document type is not one of the supported values advertised in OP metadata",
								args("document_type", documentType, "documents_supported", documentsSupported));
						}
					}
				}
			}
			//documents_methods_supported: OPTIONAL. JSON array containing the validation &
			// verification process the OP supports (see @!predefined_values)
			if (opMetadata.has("documents_methods_supported")) {
				JsonArray docMethodsSupported = opMetadata.get("documents_methods_supported").getAsJsonArray();
				for (JsonElement evidenceElement : evidences) {
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if (evidence.get("type").equals(new JsonPrimitive("document"))) {
						JsonElement method = evidence.get("method");
						if (docMethodsSupported.contains(method)) {
							logSuccess("method is one of the supported values advertised in OP metadata",
								args("method", method, "documents_methods_supported", docMethodsSupported));
						} else {
							throw error("method is not one of the supported values advertised in OP metadata",
								args("method", method, "documents_methods_supported", docMethodsSupported));
						}
					}
				}
			}
			//documents_validation_methods_supported: OPTIONAL. JSON array containing the document
			// validation methods the OP supports (see @!predefined_values).
			if (opMetadata.has("documents_validation_methods_supported")) {
				JsonArray validationMethodsSupported = opMetadata.get("documents_validation_methods_supported").getAsJsonArray();
				for (JsonElement evidenceElement : evidences) {
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if (evidence.has("validation_method")) {
						JsonObject validationMethod = evidence.get("validation_method").getAsJsonObject();
						JsonElement validationType = validationMethod.get("type");
						if (validationMethodsSupported.contains(validationType)) {
							logSuccess("validation_method type is one of the supported values advertised in OP metadata",
								args("validation_method_type", validationType,
									"documents_validation_methods_supported", validationMethodsSupported));
						} else {
							throw error("validation_method type is not one of the supported values advertised in OP metadata",
								args("validation_method_type", validationType,
									"documents_validation_methods_supported", validationMethodsSupported));
						}
					}
				}
			}

			//documents_verification_methods_supported: OPTIONAL. JSON array containing the verification
			// methods the OP supports (see @!predefined_values).
			if (opMetadata.has("documents_verification_methods_supported"))
			{
				JsonArray verificationMethodsSupported = opMetadata.get("documents_verification_methods_supported").getAsJsonArray();
				for (JsonElement evidenceElement : evidences)
				{
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if (evidence.has("verification_method"))
					{
						JsonObject verificationMethod = evidence.get("verification_method").getAsJsonObject();
						JsonElement verificationType = verificationMethod.get("type");
						if (verificationMethodsSupported.contains(verificationType))
						{
							logSuccess("verification_method type is one of the supported values advertised in OP metadata",
								args("verification_method_type", verificationType,
									"documents_verification_methods_supported", verificationMethodsSupported));
						}
						else
						{
							throw error("verification_method type is not one of the supported values advertised in OP metadata",
								args("verification_method_type", verificationType,
									"documents_verification_methods_supported", verificationMethodsSupported));
						}
					}
				}
			}

			//TODO note it is "electronicrecord" below but I assumed it is "electronic_record" based on the rest of the document
			//electronic_records_supported: REQUIRED when evidence_supported contains "electronicrecord".
			// JSON array containing all electronic record types the OP supports (see @!predefinedvalues).
			if (evidenceSupported.contains(new JsonPrimitive("electronic_record")))
			{
				JsonArray electronicRecordsSupported = opMetadata.get("electronic_records_supported").getAsJsonArray();
				for (JsonElement evidenceElement : evidences)
				{
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if (evidence.get("type").equals(new JsonPrimitive("electronic_record")))
					{
						JsonObject electronicRecord = evidence.get("electronic_record").getAsJsonObject();
						JsonElement electronicRecordType = electronicRecord.get("type");
						if (electronicRecordsSupported.contains(electronicRecordType))
						{
							logSuccess("electronic_record type is one of the supported values advertised in OP metadata",
								args("electronic_record_type", electronicRecordType,
									"electronic_records_supported", electronicRecordsSupported));
						}
						else
						{
							throw error("electronic_record type is not one of the supported values advertised in OP metadata",
								args("electronic_record_type", electronicRecordType,
									"electronic_records_supported", electronicRecordsSupported));
						}
					}
				}
			}
		}
		//claims_in_verified_claims_supported: REQUIRED. JSON array containing all Claims supported within verified_claims.
		JsonObject claims = verifiedClaims.get("claims").getAsJsonObject();
		JsonArray supportedClaims = opMetadata.get("claims_in_verified_claims_supported").getAsJsonArray();
		for(String claimName : claims.keySet()) {
			if(!supportedClaims.contains(new JsonPrimitive(claimName))){
				throw error("Returned claim not found in claims_in_verified_claims_supported",
					args("claim_name", claimName, "claims_in_verified_claims_supported", supportedClaims));
			}
		}
		//if we reached here, it's okay
		logSuccess("Only claims advertised in OP metadata are returned",
			args("claims", claims, "supported_claims", supportedClaims));


		//TODO add distributed and/or aggregated claim checks
		//If the OP supports distributed and/or aggregated Claim types in verified_claims, the OP MUST advertise this in its metadata using the claim_types_supported element.
	}

}
