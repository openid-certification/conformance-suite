package net.openid.conformance.ekyc.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractValidateVerifiedClaimsAgainstRequest extends AbstractCondition {
	/**
	 *
	 * @param requestedVerifiedClaimsElement json object or array
	 * @param returnedVerifiedClaimsElement json object or array
	 */
	protected void validateResponseAgainstRequestedVerifiedClaims(JsonElement requestedVerifiedClaimsElement, JsonElement returnedVerifiedClaimsElement) {
		if(requestedVerifiedClaimsElement.isJsonObject()) {
			validateResponseAgainstSingleRequestedVerifiedClaims(requestedVerifiedClaimsElement.getAsJsonObject(), returnedVerifiedClaimsElement);
		} else if(requestedVerifiedClaimsElement.isJsonArray()) {
			for(JsonElement element : requestedVerifiedClaimsElement.getAsJsonArray()) {
				if(element.isJsonObject()) {
					JsonObject jsonObject = element.getAsJsonObject();
					validateResponseAgainstSingleRequestedVerifiedClaims(jsonObject, returnedVerifiedClaimsElement);
				} else {
					throw error("Unexpected element in verified_claims array in request", args("element", element));
				}
			}
		} else {
			throw error("Unexpected verified_claims element in request. Must be either an array or object",
						args("element", requestedVerifiedClaimsElement));
		}
	}

	protected void validateResponseAgainstSingleRequestedVerifiedClaims(JsonObject requestedVerifiedClaims, JsonElement returnedVerifiedClaimsElement) {
		if(returnedVerifiedClaimsElement.isJsonArray()) {
			for(JsonElement element : returnedVerifiedClaimsElement.getAsJsonArray()) {
				if(element.isJsonObject()) {
					JsonObject jsonObject = element.getAsJsonObject();
					validateOneOnOne(requestedVerifiedClaims, jsonObject);
				} else {
					throw error("Unexpected element in verified_claims array in request", args("element", element));
				}
			}
		} else if(returnedVerifiedClaimsElement.isJsonObject()) {
			validateOneOnOne(requestedVerifiedClaims, returnedVerifiedClaimsElement.getAsJsonObject());
		} else {
			throw error("Returned verified_claims element is neither an array or object",
						args("element", returnedVerifiedClaimsElement));
		}
	}

	/**
	 * compare a single verified_claims in request with a single verified_claims in response
	 * skip if requested claims not found in returned
	 * as OPs may not return all requested claims, don't error if not found at all
	 *
	 * 6. Requesting Verified Claims
	 * ...
	 * Note: The OP MUST NOT provide the RP with any data it did not request. However, the OP MAY at
	 * its discretion omit Claims from the response.
	 * @param requestedVerifiedClaims
	 * @param returnedVerifiedClaims
	 */
	protected void validateOneOnOne(JsonObject requestedVerifiedClaims, JsonObject returnedVerifiedClaims){
		JsonObject requestedClaims = requestedVerifiedClaims.get("claims").getAsJsonObject();
		JsonObject returnedClaims = returnedVerifiedClaims.get("claims").getAsJsonObject();
		boolean foundAtLeastOneMatch = false;
		for(String key : requestedClaims.keySet()) {
			if(returnedClaims.has(key)) {
				foundAtLeastOneMatch = true;
				break;
			}
		}
		if(!foundAtLeastOneMatch) {
			//no need to compare these. requested claims are not included in this verified_claims in response
			return;
		}
		JsonObject requestedVerification = requestedVerifiedClaims.get("verification").getAsJsonObject();
		JsonObject returnedVerification = returnedVerifiedClaims.get("verification").getAsJsonObject();

		//trust_framework
		compareTrustFrameworks(requestedVerification.get("trust_framework"), returnedVerification.get("trust_framework"));

		if(requestedVerification.has("time")) {
			checkTime(requestedVerification, returnedVerification);
		}

		if(requestedVerification.has("verification_process")) {
			validateVerificationProcess(requestedVerification.get("verification_process"), returnedVerification.get("verification_process"));
		}

		if(requestedVerification.has("evidence")) {
			validateEvidence(requestedVerification.get("evidence"), returnedVerification.get("evidence"));
		}
	}

	/**
	 *
	 * @param requestedVerificationProcess null or simple element with "essential" and "purpose" only
	 * @param returnedVerificationProcess string
	 */
	protected void validateVerificationProcess(JsonElement requestedVerificationProcess, JsonElement returnedVerificationProcess) {
		if(requestedVerificationProcess.isJsonNull()){
			logSuccess("verification_process was null in request, any value will be accepted",
				args("returned_verification_process", returnedVerificationProcess));
			return;
		}
		if(requestedVerificationProcess.isJsonObject()){
			JsonObject requestedVP = requestedVerificationProcess.getAsJsonObject();
			if(requestedVP.has("essential") && OIDFJSON.getBoolean(requestedVP.get("essential"))){
				if(returnedVerificationProcess==null || returnedVerificationProcess.isJsonNull()) {
					throw error("verification_process was requested as essential but it is not included in the response");
				} else {
					String returnedVP = OIDFJSON.getString(returnedVerificationProcess);
					if(Strings.isNullOrEmpty(returnedVP)) {
						//TODO does this require a failure?
						throw error("verification_process was requested as essential but it is empty in the response");
					} else {
						logSuccess("verification_process, requested as essential, is included in the response",
							args("verification_process", returnedVerificationProcess));
					}
				}
			}
		} else {
			throw error("Unexpected verification_process in request",
				args("verification_process", requestedVerificationProcess));
		}
	}

	/**
	 * A single entry in the evidence array represents a filter over elements of a certain evidence type.
	 * The RP therefore MUST specify this type by including the type field including a suitable value
	 * sub-element value. The values sub-element MUST NOT be used for the evidence/type field.
	 *
	 * If multiple entries are present in evidence, these filters are linked by a logical OR.
	 * @param requestedEvidenceElement
	 * @param returnedEvidenceElement
	 */
	protected void validateEvidence(JsonElement requestedEvidenceElement, JsonElement returnedEvidenceElement) {
		JsonArray requestedEvidence = requestedEvidenceElement.getAsJsonArray();
		JsonArray returnedEvidence = returnedEvidenceElement.getAsJsonArray();
		if(requestedEvidence.size()==1) {
			JsonObject evidenceObject = requestedEvidence.get(0).getAsJsonObject();
			if(validateSingleEvidence(evidenceObject, returnedEvidence)){
				logSuccess("Returned evidence match requested");
			} else {
				throw error("Response does not contain an evidence with the requested type",
					args("requested_type", evidenceObject.get("type"), "returned_evidence", returnedEvidence));
			}
		} else {
			//OR results
			boolean atLeastOneMatched = false;
			for(JsonElement element : requestedEvidence){
				JsonObject evidenceObject = element.getAsJsonObject();
				atLeastOneMatched = (atLeastOneMatched || validateSingleEvidence(evidenceObject, returnedEvidence));
			}
			if(atLeastOneMatched) {
				logSuccess("Returned evidence match one of the requested evidence types");
			} else {
				throw error("Response does not contain an evidence matching one of the requested evidence types",
					args("requested", requestedEvidence, "returned", returnedEvidence));
			}
		}
	}

	protected boolean validateSingleEvidence(JsonObject requestedEvidence, JsonArray returnedEvidence) {
		int typeMatchCount = 0;
		for(JsonElement returnEvidenceElement : returnedEvidence) {
			JsonObject returnedObject = returnEvidenceElement.getAsJsonObject();
			if(requestedEvidence.get("type").getAsJsonObject().get("value").equals(returnedObject.get("type"))){
				//attachments
				if(requestedEvidence.has("attachments")) {
					if(requestedEvidence.get("attachments").isJsonObject()) {
						JsonObject requestedAttachments = requestedEvidence.get("attachments").getAsJsonObject();
						if(requestedAttachments.has("essential") && OIDFJSON.getBoolean(requestedAttachments.get("essential"))) {
							if(!returnedObject.has("attachments")){
								throw error("attachments was requested as essential but the returned evidence does not have attachments",
										args("requested", requestedEvidence, "returned", returnedObject));
							}
						}
					} else if(requestedEvidence.get("attachments").isJsonNull()) {
						//TODO when attachments is json null, must the response contain attachments or is it optional? Likewise for "essential" case above
					}
				}
				//"type" specific checks
				String evidenceType = OIDFJSON.getString(requestedEvidence.get("type").getAsJsonObject().get("value"));
				switch (evidenceType) {
					case "document":
						compareDocumentType(requestedEvidence, returnedObject);
						break;
					//TODO add methods for the following
					case "electronic_record":
						break;
					case "vouch":
						break;
					case "utility_bill":
						break;
					case "electronic_signature":
						break;
				}
				typeMatchCount++;
			}
		}
		if(typeMatchCount<1) {
			return false;
		}
		return true;
	}

	protected void compareDocumentType(JsonObject requestedEvidence, JsonObject returnedEvidenceObject) {
		//validation_method
		if(requestedEvidence.has("validation_method")) {
			if(requestedEvidence.get("validation_method").isJsonNull()) {
				if(!returnedEvidenceObject.has("validation_method")) {
					throw error("validation_method was requested but not returned in evidence",
						args("requested", requestedEvidence, "returned", returnedEvidenceObject));
				}
			} else if (requestedEvidence.get("validation_method").isJsonObject()) {
				JsonObject requestedValidationMethod = requestedEvidence.get("validation_method").getAsJsonObject();
				JsonObject returnedValidationMethod = returnedEvidenceObject.get("validation_method").getAsJsonObject();
				if(requestedValidationMethod.has("type")) {
					compareConstrainableElement(requestedValidationMethod.get("type"), returnedValidationMethod.get("type"));
				}
				if(requestedValidationMethod.has("policy")) {
					compareConstrainableElement(requestedValidationMethod.get("policy"), returnedValidationMethod.get("policy"));
				}
				if(requestedValidationMethod.has("procedure")) {
					compareConstrainableElement(requestedValidationMethod.get("procedure"), returnedValidationMethod.get("procedure"));
				}
				if(requestedValidationMethod.has("status")) {
					compareConstrainableElement(requestedValidationMethod.get("status"), returnedValidationMethod.get("status"));
				}

			}
		}
		//verification_method
		if(requestedEvidence.has("verification_method")) {
			if(requestedEvidence.get("verification_method").isJsonNull()) {
				if(!returnedEvidenceObject.has("verification_method")) {
					throw error("verification_method was requested but not returned in evidence",
						args("requested", requestedEvidence, "returned", returnedEvidenceObject));
				}
			} else if (requestedEvidence.get("verification_method").isJsonObject()) {
				JsonObject requestedVerificationMethod = requestedEvidence.get("verification_method").getAsJsonObject();
				JsonObject returnedVerificationMethod = returnedEvidenceObject.get("verification_method").getAsJsonObject();
				if(requestedVerificationMethod.has("type")) {
					compareConstrainableElement(requestedVerificationMethod.get("type"), returnedVerificationMethod.get("type"));
				}
				if(requestedVerificationMethod.has("policy")) {
					compareConstrainableElement(requestedVerificationMethod.get("policy"), returnedVerificationMethod.get("policy"));
				}
				if(requestedVerificationMethod.has("procedure")) {
					compareConstrainableElement(requestedVerificationMethod.get("procedure"), returnedVerificationMethod.get("procedure"));
				}
				if(requestedVerificationMethod.has("status")) {
					compareConstrainableElement(requestedVerificationMethod.get("status"), returnedVerificationMethod.get("status"));
				}

			}
		}
		//method
		if(requestedEvidence.has("method")) {
			if(requestedEvidence.get("method").isJsonNull()) {
				if(!returnedEvidenceObject.has("method")) {
					throw error("method was requested but not returned in evidence",
						args("requested", requestedEvidence, "returned", returnedEvidenceObject));
				}
			} else if (requestedEvidence.get("method").isJsonObject()) {
				JsonObject requestedMethod = requestedEvidence.get("method").getAsJsonObject();
				JsonElement returnedMethod = returnedEvidenceObject.get("method");
				compareConstrainableElement(requestedMethod, returnedMethod);
			}
		}
		//verifier
			//organization
			//txn
		if(requestedEvidence.has("verifier")) {
			if(requestedEvidence.get("verifier").isJsonNull()) {
				if(!returnedEvidenceObject.has("verifier")) {
					throw error("verifier was requested but not returned in evidence",
						args("requested", requestedEvidence, "returned", returnedEvidenceObject));
				}
			} else if (requestedEvidence.get("verifier").isJsonObject()) {
				JsonObject requestedVerifier = requestedEvidence.get("verifier").getAsJsonObject();
				JsonElement returnedVerifier = returnedEvidenceObject.get("verifier");
				if(requestedVerifier.has("organization")) {
					if(requestedVerifier.get("organization").isJsonObject()) {
						if(requestedVerifier.get("organization").getAsJsonObject().has("essential")
						&& OIDFJSON.getBoolean(requestedVerifier.get("organization").getAsJsonObject().get("essential"))) {
							throw error("verifier organization was requested as essential in evidence but not returned",
								args("requested", requestedEvidence, "returned", returnedEvidenceObject));
						}
					} else {
						if(!returnedVerifier.getAsJsonObject().has("organization")) {
							throw error("verifier organization was requested in evidence but not returned",
										args("requested", requestedEvidence, "returned", returnedEvidenceObject));
						}
					}
				}
				if(requestedVerifier.has("txn")) {
					if(requestedVerifier.get("txn").isJsonObject()) {
						if(requestedVerifier.get("txn").getAsJsonObject().has("essential")
							&& OIDFJSON.getBoolean(requestedVerifier.get("txn").getAsJsonObject().get("essential"))) {
							throw error("verifier txn was requested as essential in evidence but not returned",
								args("requested", requestedEvidence, "returned", returnedEvidenceObject));
						}
					} else {
						if(!returnedVerifier.getAsJsonObject().has("txn")) {
							throw error("verifier txn was requested in evidence but not returned",
								args("requested", requestedEvidence, "returned", returnedEvidenceObject));
						}
					}
				}
			}
		}
		//time
		if(requestedEvidence.has("time")) {
			if(requestedEvidence.get("time").isJsonObject()) {
				JsonObject requestedTimeObject = requestedEvidence.get("time").getAsJsonObject();
				if(requestedTimeObject.has("max_age")) {
					Long maxAge = OIDFJSON.getLong(requestedTimeObject.get("max_age"));
					String returnedTimeInISOFormat = OIDFJSON.getString(returnedEvidenceObject.get("time"));
					ZonedDateTime verificationTime = ZonedDateTime.parse(returnedTimeInISOFormat, DateTimeFormatter.ISO_INSTANT);
					Instant now = Instant.now();
					if(verificationTime.isBefore(now.minusSeconds(maxAge).atZone(ZoneOffset.UTC))) {
						throw error("Document verification time is before the requested max_age",
							args("max_age", maxAge, "now", now.toString(), "verificationTime", returnedTimeInISOFormat));
					}
				}
			}
		}
		//TODO add the following
		//document_details
		//document
	}



	//TODO how does "essential" affect this behavior? e.g if a "value" was provided but not "essential" can the OP ignore the "value"?
	protected boolean compareConstrainableElement(JsonElement requested, JsonElement returned) {
		if(requested.isJsonObject()) {
			JsonObject requestedObject = requested.getAsJsonObject();
			if(requestedObject.has("value")) {
				if(requestedObject.get("value").equals(returned)) {
					return true;
				} else {
					throw error("Returned value does not match requested value", args("requested", requested, "returned", returned));
				}
			} else if(requestedObject.has("values")) {
				JsonArray values = requestedObject.get("values").getAsJsonArray();
				if(values.contains(returned)) {
					return true;
				} else {
					throw error("Returned value is not one of the requested values", args("requested", requested, "returned", returned));
				}
			}
		}
		return true;
	}

	protected void checkTime(JsonObject requestedVerification, JsonObject returnedVerification) {
		JsonElement requestedTime = requestedVerification.get("time");
		if(requestedTime.isJsonNull()) {
			//time must be present
			if(returnedVerification.has("time")) {
				logSuccess("As requested, verification.time element is present in returned response",
					args("time", returnedVerification.get("time")));
			}
		} else if(requestedTime.isJsonObject()) {
			//has max_age
			//TODO is this just a SHOULD?. if so then what's the point? I'm assuming that this should fail if max_age cannot be met
			// The OP SHOULD try to fulfill this requirement. If the verification data of the End-User is older
			// than the requested max_age, the OP MAY attempt to refresh the End-User's verification by sending them
			// through an online identity verification process, e.g., by utilizing an electronic ID card or
			// a video identification approach.
			JsonObject requestedTimeObject = requestedTime.getAsJsonObject();
			if(requestedTimeObject.has("max_age")) {
				Long maxAge = OIDFJSON.getLong(requestedTimeObject.get("max_age"));
				String returnedTimeInISOFormat = OIDFJSON.getString(returnedVerification.get("time"));
				ZonedDateTime verificationTime = ZonedDateTime.parse(returnedTimeInISOFormat, DateTimeFormatter.ISO_INSTANT);
				Instant now = Instant.now();
				if(verificationTime.isBefore(now.minusSeconds(maxAge).atZone(ZoneOffset.UTC))) {
					throw error("Verification time is before the requested max_age",
						args("max_age", maxAge, "now", now.toString(), "verificationTime", returnedTimeInISOFormat));
				} else {
					logSuccess("Verification time is within allowed limits",
						args("max_age", maxAge, "now", now.toString(), "verificationTime", returnedTimeInISOFormat));
				}
			}
		}
	}

	/* IN REQUEST:
	"constrainable_element":{
		"oneOf":[
			{
				"type":"null"
			},
			{
				"type":"object",
				"properties":{
					"value":{
						"type":"string"
					},
					"values":{
						"type":"array",
						"items":{
							"type":"string"
						},
						"minItems":1
					},
					"essential":{
						"type":"boolean"
					},
					"purpose":{
						"type":"string",
						"minLength":3,
						"maxLength":300
					}
				}
			}
		]
	}

	IN RESPONSE: string
	 */
	protected void compareTrustFrameworks(JsonElement requestedTrustFramework, JsonElement returnedTrustFramework) {
		if(requestedTrustFramework.isJsonNull()) {
			//anything works
			logSuccess("Requested trust_framework was null so any value will be accepted",
				args("returned", returnedTrustFramework));
			return;
		}
		if(requestedTrustFramework.isJsonObject()) {
			JsonObject requestedTF = requestedTrustFramework.getAsJsonObject();
			if(requestedTF.has("value")) {
				if(requestedTF.get("value").equals(returnedTrustFramework)) {
					logSuccess("Requested trust_framework matches the returned value",
						args("trust_framework", returnedTrustFramework));
				}
			} else if(requestedTF.has("values")) {
				if (requestedTF.get("values").getAsJsonArray().contains(returnedTrustFramework)) {
					logSuccess("Returned trust_framework is one of the expected values",
						args("trust_framework", returnedTrustFramework));
				} else {
					throw error("Returned trust_framework value is not one of the requested ones",
						args("requested", requestedTF.get("values"), "returned", returnedTrustFramework));
				}
			} else if(requestedTF.size() == 0) {
				logSuccess("Requested trust_framework was empty object so any value will be accepted",
					args("returned", returnedTrustFramework));
			} else if(requestedTF.size() == 1 && requestedTF.getAsJsonPrimitive("essential") != null) {
				logSuccess("Requested trust_framework was object with only essential:false so any value will be accepted",
					args("returned", returnedTrustFramework));
			} else {
				// I'm not entirely clear what this error means; it doesn't appear to be a requirement in the spec
				// I think it might be a requirement when configuring the test rather than a requirement from the spec
				throw error("Requested trust_framework must contain either value or values",
							args("requestedTrustFramework", requestedTrustFramework));
			}
			return;
		}
		throw error("Unexpected requested trust_framework type in request",
					args("requestedTrustFramework", requestedTrustFramework));
	}
}
