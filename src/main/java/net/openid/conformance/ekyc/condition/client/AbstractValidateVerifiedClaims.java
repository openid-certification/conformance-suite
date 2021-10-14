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

public abstract class AbstractValidateVerifiedClaims extends AbstractCondition {
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
			throw error("Unexpected verified_claims element in request", args("element", requestedVerifiedClaimsElement));
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
			throw error("Returned verified_claims element is neither an array or object");
		}
	}

	/**
	 * compare a single verified_claims in request with a single verified_claims in response
	 * skip if requested claims not found in returned
	 * as OPs may not return all requested claims, don't error if not found at all
	 * TODO error if something not requested was returned
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
			JsonObject vp = requestedVerificationProcess.getAsJsonObject();
			if(vp.has("essential") && OIDFJSON.getBoolean(vp.get("essential"))){
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
			throw error("Unexpected verification_process in request, this is an bug in the suite",
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

	//TODO check other props recursively, for now only checking type
	protected boolean validateSingleEvidence(JsonObject requestedEvidence, JsonArray returnedEvidence) {
		int typeMatchCount = 0;
		for(JsonElement returnEvidenceElement : returnedEvidence) {
			JsonObject returnedObject = returnEvidenceElement.getAsJsonObject();
			if(requestedEvidence.get("type").getAsJsonObject().get("value").equals(returnedObject.get("type"))){
				typeMatchCount++;
			}
		}
		if(typeMatchCount<1) {
			return false;
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
	"constrainable_element": {
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "object",
          "properties": {
            "value": {
              "type": "string"
            },
            "values": {
              "type": "array",
              "items": {
                "type": "string"
              },
              "minItems": 1
            },
            "essential": {
              "type": "boolean"
            },
            "purpose": {
              "type": "string" ,
              "minLength": 3 ,
              "maxLength": 300
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
			JsonObject tf = requestedTrustFramework.getAsJsonObject();
			if(tf.has("value")) {
				if(tf.get("value").equals(returnedTrustFramework)) {
					logSuccess("Requested trust_framework matches the returned value",
						args("trust_framework", returnedTrustFramework));
				}
			} else if(tf.has("values")) {
				if(tf.get("values").getAsJsonArray().contains(returnedTrustFramework)){
					logSuccess("Returned trust_framework is one of the expected values",
						args("trust_framework", returnedTrustFramework));
				} else {
					throw error("Returned trust_framework value is not one of the requested ones",
						args("requested", tf.get("values"), "returned", returnedTrustFramework));
				}
			} else {
				throw error("Requested trust_framework must contain either value or values, " +
					"this is a bug in the suite", args("requestedTrustFramework", requestedTrustFramework));
			}
			return;
		}
		throw error("Unexpected requested trust_framework type, this is a bug in the suite",
			args("requestedTrustFramework", requestedTrustFramework));
	}
}
