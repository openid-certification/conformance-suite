package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
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
	 * A single verified_claims object
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

		JsonArray evidenceSupported = opMetadata.get("evidence_supported").getAsJsonArray();
		JsonElement evidenceArrayElement = verification.get("evidence");
		if(evidenceArrayElement!=null)
		{
			if(!evidenceArrayElement.isJsonArray()) {
				throw error("evidence must be an array", args("actual", evidenceArrayElement));
			}
			JsonArray evidences = evidenceArrayElement.getAsJsonArray();
			for (JsonElement evidenceElement : evidences)
			{
				JsonObject evidence = evidenceElement.getAsJsonObject();
				if (evidenceSupported.contains(evidence.get("type")))
				{
					logSuccess("Evidence type is one of the supported values advertised in OP metadata",
						args("evidence_type", evidence.get("type"), "evidence_supported", evidenceSupported));
				}
				else
				{
					throw error("Evidence type is not one of the supported values advertised in OP metadata",
						args("evidence_type", evidence.get("type"), "evidence_supported", evidenceSupported));
				}
			}

			//documents_supported: REQUIRED when evidence_supported contains "document" or "id_document".
			// JSON array containing all identity document types utilized by the OP for identity verification.
			if (evidenceSupported.contains(new JsonPrimitive("document")) || evidenceSupported.contains(new JsonPrimitive("id_document")))
			{
				JsonElement documentsSupportedElement = opMetadata.get("documents_supported");
				if(documentsSupportedElement==null) {
					throw error("documents_supported is REQUIRED when evidence_supported contains document or id_document " +
						"but documents_supported could not be found in OP metadata");
				}
				JsonArray documentsSupported = documentsSupportedElement.getAsJsonArray();
				for (JsonElement evidenceElement : evidences)
				{
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if (evidence.get("type").equals(new JsonPrimitive("id_document")) ||
						evidence.get("type").equals(new JsonPrimitive("document")))
					{
						JsonObject documentObject = evidence.get("document").getAsJsonObject();
						JsonElement documentType = documentObject.get("type");
						if (documentsSupported.contains(documentType))
						{
							logSuccess("Document type is one of the supported values advertised in OP metadata",
								args("document_type", documentType, "documents_supported", documentsSupported));
						}
						else
						{
							throw error("Document type is not one of the supported values advertised in OP metadata",
								args("document_type", documentType, "documents_supported", documentsSupported));
						}
					}
				}
			}
			//documents_methods_supported: OPTIONAL. JSON array containing the validation &
			// verification process the OP supports (see @!predefined_values)
			if (opMetadata.has("documents_methods_supported"))
			{
				JsonArray docMethodsSupported = opMetadata.get("documents_methods_supported").getAsJsonArray();
				for (JsonElement evidenceElement : evidences)
				{
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if (evidence.get("type").equals(new JsonPrimitive("id_document")))
					{
						JsonElement method = evidence.get("method");
						if (docMethodsSupported.contains(method))
						{
							logSuccess("method is one of the supported values advertised in OP metadata",
								args("method", method, "documents_methods_supported", docMethodsSupported));
						}
						else
						{
							throw error("method is not one of the supported values advertised in OP metadata",
								args("method", method, "documents_methods_supported", docMethodsSupported));
						}
					}
				}
			}
			//documents_validation_methods_supported: OPTIONAL. JSON array containing the document
			// validation methods the OP supports (see @!predefined_values).
			if (opMetadata.has("documents_validation_methods_supported"))
			{
				JsonArray validationMethodsSupported = opMetadata.get("documents_validation_methods_supported").getAsJsonArray();
				for (JsonElement evidenceElement : evidences)
				{
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if (evidence.has("validation_method"))
					{
						JsonObject validationMethod = evidence.get("validation_method").getAsJsonObject();
						JsonElement validationType = validationMethod.get("type");
						if (validationMethodsSupported.contains(validationType))
						{
							logSuccess("validation_method type is one of the supported values advertised in OP metadata",
								args("validation_method_type", validationType,
									"documents_validation_methods_supported", validationMethodsSupported));
						}
						else
						{
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

			//attachments_supported: REQUIRED when OP supports external attachments. JSON array containing all
			// attachment types supported by the OP. Possible values are external and embedded.
			// If the list is empty, the OP does not support attachments.
			if(opMetadata.has("attachments_supported")) {
				JsonArray attachmentsSupported = opMetadata.get("attachments_supported").getAsJsonArray();
				for (JsonElement evidenceElement : evidences) {
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if(evidence.has("attachments")) {
						for(JsonElement attachmentElement : evidence.get("attachments").getAsJsonArray()){
							JsonObject attachmentObject = attachmentElement.getAsJsonObject();
							if(attachmentObject.has("digest")){
								//this is an external_attachment
								if(attachmentsSupported.contains(new JsonPrimitive("external"))){
									logSuccess("Server supports external attachments");
								} else {
									throw error("Evidence contains an external attachment but server does not advertise support for " +
											"external attachments",
										args("evidence", evidence, "attachments_supported", attachmentsSupported));
								}
							} else if(attachmentObject.has("content")) {
								//embedded_attachment
								if(attachmentsSupported.contains(new JsonPrimitive("embedded"))){
									logSuccess("Server supports embedded attachments");
								} else {
									throw error("Evidence contains an embedded attachment but server does not advertise support for " +
											"embedded attachments",
										args("evidence", evidence, "attachments_supported", attachmentsSupported));
								}
							}
						}
					}
				}

			}

			//digest_algorithms_supported: REQUIRED when OP supports external attachments. JSON array containing all
			// supported digest algorithms which can be used as alg property within the digest object of
			// external attachments. If the OP supports external attachments, at least the algorithm sha-256
			// MUST be supported by the OP as well. The list of possible digest/hash algorithm names is maintained
			// by IANA in [hash_name_registry] (established by [RFC6920]).
			if(opMetadata.has("digest_algorithms_supported")) {
				JsonArray digestAlgorithmsSupported = opMetadata.get("digest_algorithms_supported").getAsJsonArray();
				for (JsonElement evidenceElement : evidences) {
					JsonObject evidence = evidenceElement.getAsJsonObject();
					if(evidence.has("attachments")) {
						for(JsonElement attachmentElement : evidence.get("attachments").getAsJsonArray()){
							JsonObject attachmentObject = attachmentElement.getAsJsonObject();
							if(attachmentObject.has("digest")){
								//this is an external_attachment
								JsonObject digest = attachmentObject.get("digest").getAsJsonObject();
								JsonElement alg = digest.get("alg");
								if(digestAlgorithmsSupported.contains(alg)) {
									logSuccess("Evidence digest algorithm is one of the supported values advertised in OP metadata",
										args("alg", alg, "digest_algorithms_supported", digestAlgorithmsSupported));
								} else {
									throw error("Evidence digest algorithm is not one of the supported values advertised in OP metadata",
										args("alg", alg, "digest_algorithms_supported", digestAlgorithmsSupported));
								}
							}
						}
					}
				}
			} else {
				JsonArray attachmentsSupported = opMetadata.get("attachments_supported").getAsJsonArray();
				if(attachmentsSupported.contains(new JsonPrimitive("external"))) {
					throw error("External attachments are supported but OP metadata does not contain digest_algorithms_supported");
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
