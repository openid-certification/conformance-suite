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
import java.util.List;

public abstract class AbstractValidateVerifiedClaimsAgainstRequest extends AbstractCondition {

	protected boolean checkExpectedValuesConfiguration = false;


	/**
	 *
	 * @param requestedVerifiedClaimsElement json object or array
	 * @param returnedVerifiedClaimsElement json object or array
	 */
	protected boolean validateResponseAgainstRequestedVerifiedClaims(JsonElement requestedVerifiedClaimsElement, JsonElement returnedVerifiedClaimsElement, JsonElement expectedValuesConfig) {
		if(requestedVerifiedClaimsElement.isJsonObject()) {
			return validateResponseAgainstSingleRequestedVerifiedClaims(requestedVerifiedClaimsElement.getAsJsonObject(), returnedVerifiedClaimsElement, expectedValuesConfig);
		} else if(requestedVerifiedClaimsElement.isJsonArray()) {
			boolean foundValidResponse = false;
			for(JsonElement element : requestedVerifiedClaimsElement.getAsJsonArray()) {
				if(element.isJsonObject()) {
					JsonObject jsonObject = element.getAsJsonObject();
					// request arrays act as OR conditions, return after first valid response???
					foundValidResponse = foundValidResponse || validateResponseAgainstSingleRequestedVerifiedClaims(jsonObject, returnedVerifiedClaimsElement, expectedValuesConfig);
				} else {
					throw error("Unexpected element in verified_claims array in request", args("element", element));
				}
			}
			return foundValidResponse;
		} else {
			throw error("Unexpected verified_claims element in request. Must be either an array or object",
						args("element", requestedVerifiedClaimsElement));
		}

	}


	protected boolean validateResponseAgainstSingleRequestedVerifiedClaims(JsonObject requestedVerifiedClaims, JsonElement returnedVerifiedClaimsElement, JsonElement expectedValuesConfig) {
		if(returnedVerifiedClaimsElement.isJsonArray()) {
			boolean foundValidResponse = false;
			// check response array for matches against the request
			for(JsonElement element : returnedVerifiedClaimsElement.getAsJsonArray()) {
				if(element.isJsonObject()) {
					// arrays act as OR conditions, return after first valid response???
					boolean isValidResponseElement = validateOneOnOne(requestedVerifiedClaims, element.getAsJsonObject(), null); // check if response matches request first
					if(isValidResponseElement && checkExpectedValuesConfiguration) {
						JsonArray expectedValuesArray = OIDFJSON.packJsonElementIntoJsonArray(expectedValuesConfig);
						for(JsonElement expectedElement : expectedValuesArray) {
							isValidResponseElement = validateOneOnOne(requestedVerifiedClaims, element.getAsJsonObject(), expectedElement.getAsJsonObject());
							if(isValidResponseElement) {
								break;  // found one matching expected value
							}
						}
					}
					foundValidResponse = foundValidResponse || isValidResponseElement;
				} else {
					throw error("Unexpected element in verified_claims array in request", args("element", element));
				}
			}
			return foundValidResponse;
		} else if(returnedVerifiedClaimsElement.isJsonObject()) {

			boolean foundValidResponse = validateOneOnOne(requestedVerifiedClaims, returnedVerifiedClaimsElement.getAsJsonObject(), null);
			if(foundValidResponse && checkExpectedValuesConfiguration && (null != expectedValuesConfig)) {
				for(JsonElement expectedElement : OIDFJSON.packJsonElementIntoJsonArray(expectedValuesConfig)) {
					foundValidResponse = validateOneOnOne(requestedVerifiedClaims, returnedVerifiedClaimsElement.getAsJsonObject(), expectedElement.getAsJsonObject());
					if(foundValidResponse) {
						break;  // found one matching expected value
					}
				}
			}
			return foundValidResponse;
		} else {
			throw error("Returned verified_claims element is neither an array or object",
						args("element", returnedVerifiedClaimsElement));
		}
	}

	protected JsonObject safeGetElementAsJsonObject(JsonElement jsonElement) {
		if(null != jsonElement) {
			return jsonElement.getAsJsonObject();
		} else {
			return null;
		}
	}

	protected JsonElement safeGetJsonObjectElement(JsonObject jsonObject, String element) {
		if(null != jsonObject) {
			return jsonObject.get(element);
		} else {
			return null;
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
	 * @param expectedVerifiedClaimsConfig JsonArray or JsonObject of expected verified_claims
	 */
	protected boolean validateOneOnOne(JsonObject requestedVerifiedClaims, JsonObject returnedVerifiedClaims, JsonObject expectedVerifiedClaimsConfig){
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
			return false;

		}
		JsonObject requestedVerification = requestedVerifiedClaims.get("verification").getAsJsonObject();
		JsonObject returnedVerification = returnedVerifiedClaims.get("verification").getAsJsonObject();
		JsonObject expectedVerification = safeGetElementAsJsonObject(safeGetJsonObjectElement(expectedVerifiedClaimsConfig, "verification"));

		//trust_framework
		boolean isValid = compareTrustFrameworks(requestedVerification.get("trust_framework"), returnedVerification.get("trust_framework"), safeGetJsonObjectElement(expectedVerification, "trust_framework"));

		if(isValid && requestedVerification.has("time")) {
			isValid = checkTime(requestedVerification, returnedVerification, true, true);
		}

		// assurance_process
		if(isValid && requestedVerification.has("assurance_process")) {
			isValid = validateAssuranceProcess(requestedVerification.get("assurance_process"), returnedVerification.get("assurance_process"), safeGetJsonObjectElement(expectedVerification, "assurance_process"));
		}
		if(isValid) {
			isValid = compareConstrainableElementList(returnedVerification, returnedVerification, expectedVerification,
				"assurance_level", "verification_process");
		}
		if(isValid && requestedVerification.has("evidence")) {
			isValid = validateEvidence(requestedVerification.get("evidence"), returnedVerification.get("evidence"), safeGetJsonObjectElement(expectedVerification, "evidence"));
		}
		return isValid;
	}

	protected boolean validateAssuranceProcess(JsonElement requestedAssuranceProcess, JsonElement returnedAssuranceProcess, JsonElement expectedAssuranceProcess) {
		if(requestedAssuranceProcess.isJsonObject()) {
			JsonObject requestedAssuranceProcessObject = requestedAssuranceProcess.getAsJsonObject();
			JsonObject returnedAssuranceProcessObject = returnedAssuranceProcess.getAsJsonObject();
			JsonObject expectedAssuranceProcessObject = safeGetElementAsJsonObject(expectedAssuranceProcess);

			boolean isValid = compareConstrainableElementList(requestedAssuranceProcessObject, returnedAssuranceProcessObject, expectedAssuranceProcessObject, "policy", "procedure");

			if(isValid && requestedAssuranceProcessObject.has("assurance_details")) {
				isValid = validateAssuranceProcessAssuranceDetails(requestedAssuranceProcessObject.get("assurance_details"), returnedAssuranceProcessObject.get("assurance_details"), safeGetJsonObjectElement(expectedAssuranceProcessObject, "assurance_details"));
			}
			return isValid;
		} else {
			throw error("Unexpected assurance_process in request",
				args("assurance_process", requestedAssuranceProcess));
		}
	}


	// Validate the assurance_details for assurance_type and assurance_classification only
	// evidence_ref is an array and may contain check_id which RP may not know
	// TODO possibly check expected values
	protected boolean validateAssuranceProcessAssuranceDetails(JsonElement requestedAssuranceDetails, JsonElement returnedAssuranceDetails, JsonElement expectedAssuranceDetails) {
		if(requestedAssuranceDetails.isJsonArray()) {
			if(returnedAssuranceDetails == null) {
				log("assurance_details was requested but none was returned", args("assurance_details", requestedAssuranceDetails));
				return false;
			}
			JsonArray requestedAssuranceDetailsArray = requestedAssuranceDetails.getAsJsonArray();
			JsonArray returnedAssuranceDetailsArray = returnedAssuranceDetails.getAsJsonArray();
			JsonArray expectedAssuranceDetailsArray = OIDFJSON.packJsonElementIntoJsonArray(expectedAssuranceDetails);

			short numMatches = 0;
			for(JsonElement requestedElement : requestedAssuranceDetailsArray) {
				boolean foundMatch = false;
				for(JsonElement responseElement : returnedAssuranceDetailsArray) {
					foundMatch = false;
					if(checkExpectedValuesConfiguration && !expectedAssuranceDetailsArray.isEmpty()) {
						for(JsonElement expectedElement : expectedAssuranceDetailsArray) {
							foundMatch = compareConstrainableElementList(requestedElement.getAsJsonObject(), responseElement.getAsJsonObject(), expectedElement.getAsJsonObject(), "assurance_type", "assurance_classification");
							if(foundMatch) {
								break;
							}
						}
					} else {
						foundMatch = compareConstrainableElementList(requestedElement.getAsJsonObject(), responseElement.getAsJsonObject(), null, "assurance_type", "assurance_classification");
					}
					if(foundMatch) {
						++numMatches;
						break; // no need to check other responses
					}
				}
				if(!foundMatch){ // stop if no matching response for the current request
					log("The response does not match the request", args("request", requestedAssuranceDetails, "response", returnedAssuranceDetails, "expected", expectedAssuranceDetails));
					break;
				}
			}
			return (numMatches == requestedAssuranceDetailsArray.size());
		} else {
			throw error("Unexpected assurance_details in request",
				args("assurance_details", requestedAssuranceDetails));

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
	protected boolean validateEvidence(JsonElement requestedEvidenceElement, JsonElement returnedEvidenceElement, JsonElement expectedEvidenceElement) {
		boolean isValid = false;
		JsonArray requestedEvidence = requestedEvidenceElement.getAsJsonArray();
		JsonArray returnedEvidence = returnedEvidenceElement.getAsJsonArray();
		JsonArray expectedEvidence = OIDFJSON.packJsonElementIntoJsonArray(expectedEvidenceElement);
		if(requestedEvidence.size()==1) {
			JsonObject evidenceObject = requestedEvidence.get(0).getAsJsonObject();
			if(validateSingleEvidence(evidenceObject, returnedEvidence, expectedEvidence) > 0){
				log("Returned evidence match requested");
				isValid = true;
			} else {
				log("Response does not contain an evidence with the requested type",
					args("requested_type", evidenceObject.get("type"), "returned_evidence", returnedEvidence));
			}
		} else {
			//OR results
			boolean atLeastOneMatched = false;
			for(JsonElement element : requestedEvidence){
				JsonObject evidenceObject = element.getAsJsonObject();
				atLeastOneMatched = (atLeastOneMatched || (validateSingleEvidence(evidenceObject, returnedEvidence, expectedEvidence) > 0));
			}
			if(atLeastOneMatched) {
				log("Returned evidence match one of the requested evidence types");
			} else {
				log("Response does not contain an evidence matching one of the requested evidence types",
					args("requested", requestedEvidence, "returned", returnedEvidence));
			}
		}
		return isValid;
	}

	JsonArray getEvidenceTypes(JsonArray evidenceIn, String evidenceType) {
		JsonArray result = new JsonArray();
		if(null != evidenceIn && null != evidenceType) {
			for(JsonElement elem : evidenceIn) {
				if(elem.isJsonObject()) {
					JsonObject elemObj = elem.getAsJsonObject();
					if(elemObj.has("type") && evidenceType.equals(OIDFJSON.getString(elemObj.get("type")))) {
						result.add(elem);
					}
				}
			}
		}
		return result;
	}

	protected String getRequestedValue(JsonObject obj, String key) {
		if(obj.has(key) && obj.getAsJsonObject(key).has("value")) {
			return OIDFJSON.getString(obj.getAsJsonObject(key).get("value"));
		}
		throw error("request parameter has no value", args("request", obj, "key", key));
	}
	protected int validateSingleEvidence(JsonObject requestedEvidence, JsonArray returnedEvidence, JsonArray expectedEvidence) {
		if(requestedEvidence.has("type")) {
			String requestedEvidenceType = getRequestedValue(requestedEvidence, "type");  // value should be available since request schema requires 'type' value
			List<String> evidenceList = List.of("document", "electronic_record", "vouch", "electronic_signature");
			if(evidenceList.contains(requestedEvidenceType)) {
				final JsonArray requestedEvidenceResponse = getEvidenceTypes(returnedEvidence, requestedEvidenceType);
				final JsonArray expectedEvidenceResponse = getEvidenceTypes(expectedEvidence, requestedEvidenceType);
				if(requestedEvidenceResponse.isEmpty()) {
					return 0;
				}
				int typeMatchCount = 0;
				for(JsonElement evidenceElement : requestedEvidenceResponse) {
					JsonObject evidenceObject = evidenceElement.getAsJsonObject();
					switch (requestedEvidenceType) {
						case "document":
							if(checkExpectedValuesConfiguration && !expectedEvidenceResponse.isEmpty()) {
								for(JsonElement expectedEvidenceObject : expectedEvidenceResponse) {
									if( compareDocumentType(requestedEvidence, evidenceObject, expectedEvidenceObject.getAsJsonObject()) ) {
										++typeMatchCount;
										break;
									}
								}
							} else if( compareDocumentType(requestedEvidence, evidenceObject, null) ) {
								++typeMatchCount;
							}
							break;
						case "electronic_record":
							if(checkExpectedValuesConfiguration && !expectedEvidenceResponse.isEmpty()) {
								for(JsonElement expectedEvidenceObject : expectedEvidenceResponse) {
									if( compareElectronicRecordType(requestedEvidence, evidenceObject, expectedEvidenceObject.getAsJsonObject()) ) {
										++typeMatchCount;
										break;
									}
								}
							} else if(compareElectronicRecordType(requestedEvidence, evidenceObject, null) ) {
								++typeMatchCount;
							}
							break;
						case "vouch":
							if(checkExpectedValuesConfiguration && !expectedEvidenceResponse.isEmpty()) {
								for(JsonElement expectedEvidenceObject : expectedEvidenceResponse) {
									if( compareVouchType(requestedEvidence, evidenceObject, expectedEvidenceObject.getAsJsonObject()) ) {
										++typeMatchCount;
										break;
									}
								}
							} else if(compareVouchType(requestedEvidence, evidenceObject, null)) {
								++typeMatchCount;
							}
							break;
						case "electronic_signature":
							if(checkExpectedValuesConfiguration && !expectedEvidenceResponse.isEmpty()) {
								for(JsonElement expectedEvidenceObject : expectedEvidenceResponse) {
									if(compareElectronicSignatureType(requestedEvidence, evidenceObject, expectedEvidenceObject.getAsJsonObject())) {
										++typeMatchCount;
										break;
									}
								}
							} else if(compareElectronicSignatureType(requestedEvidence, evidenceObject, null)) {
								++typeMatchCount;
							}
							break;
						default:
							throw error("Invalid requested evidence type", args("evidence type", requestedEvidenceType));
					}
				}
				return typeMatchCount;
			} else {
				throw error("Requested evidence type is not valid", args("requested evidence", requestedEvidence));
			}
		} else {
			throw error("Requested evidence type is not present", args("requested evidence", requestedEvidence));
		}
	}

	protected boolean compareDocumentType(JsonObject requestedEvidence, JsonObject returnedEvidenceObject, JsonObject expectedEvidenceObject) {

		// use the response value to compare with the request, as this simplifies the logic of
		// figuring out what 'type' is requested. The request value can be null which makes it
		// difficult to determine how to process the type
		if(returnedEvidenceObject.has("type")) {
			if(OIDFJSON.getString(returnedEvidenceObject.get("type")).equals("document")) {
				boolean isValid = compareConstrainableElementList(requestedEvidence, returnedEvidenceObject, expectedEvidenceObject,"type");
				if(isValid && requestedEvidence.has("check_details")) {
					isValid = validateEvidenceCheckDetails(requestedEvidence.get("check_details"), safeGetJsonObjectElement(returnedEvidenceObject, "check_details"), safeGetJsonObjectElement(expectedEvidenceObject, "check_details"));
				}
				if(isValid && requestedEvidence.has("document_details")) {
					isValid = validateEvidenceDocumentDetails(requestedEvidence.get("document_details"), safeGetJsonObjectElement(returnedEvidenceObject, "document_details"), safeGetJsonObjectElement(expectedEvidenceObject, "document_details"));
				}
				return isValid;

				// TDOO derived_claims
			} else {
				throw error("Evidence type is not document", args("evidence type", OIDFJSON.getString(returnedEvidenceObject.get("type"))));
			}
		} else {
			throw error("Evidence missing required type", args("evidence", returnedEvidenceObject));
		}
	}

	protected boolean validateEvidenceCheckDetails(JsonElement requestedCheckDetails, JsonElement returnedCheckDetails, JsonElement expectedCheckDetails) {
		int matchCount = 0;
		if(requestedCheckDetails == null) {
			// check_details not requested, no need to check
			return true;
		}
		if(requestedCheckDetails.isJsonArray() ) {
			JsonArray requestedCheckDetailsArray = requestedCheckDetails.getAsJsonArray();
			if(returnedCheckDetails == null) {
				// no response returned
				return false;
			}
			if(!returnedCheckDetails.isJsonArray()) {
				throw error("Unexpected check_details element in response. Must be an array.",
					args("check_details response", returnedCheckDetails));
			}
			JsonArray returnedCheckDetailsArray = returnedCheckDetails.getAsJsonArray();
			for(JsonElement requestedCheckDetailsElement : requestedCheckDetailsArray) {
				for(JsonElement returnedCheckDetailsElement: returnedCheckDetailsArray) {
					validateElementsAreObjects("evidence check_details", requestedCheckDetailsElement, returnedCheckDetailsElement);
					JsonObject requestedCheckDetailsObject = requestedCheckDetailsElement.getAsJsonObject();
					JsonObject returnedCheckDetailsObject = returnedCheckDetailsElement.getAsJsonObject();
					validateObjectsContainRequiredElements("evidence check_details", requestedCheckDetailsObject,returnedCheckDetailsObject, "check_method");
					boolean isValid = compareConstrainableElementList(requestedCheckDetailsObject, returnedCheckDetailsObject, null,"check_method", "organization", "check_id");
					if(isValid && checkExpectedValuesConfiguration && (null != expectedCheckDetails)) {
						for(JsonElement expectedCheckDetailsElement : expectedCheckDetails.getAsJsonArray()) {
							isValid = compareConstrainableElementList(requestedCheckDetailsObject, returnedCheckDetailsObject, expectedCheckDetailsElement.getAsJsonObject(),"check_method", "organization", "check_id");
							if(isValid) {
								break;
							}
						}
					}
					if (isValid && requestedCheckDetailsObject.has("time")) {
						isValid = checkTime(requestedCheckDetailsObject, returnedCheckDetailsObject, false, false);
					}
					if( isValid) {
						log("check_details matched", args("result", ConditionResult.INFO.toString(),"req", requestedCheckDetailsObject, "res", returnedCheckDetailsObject));
						++matchCount;
					} else {
						log("check_details mismatched", args("result", ConditionResult.INFO.toString(),"req", requestedCheckDetailsObject, "res", returnedCheckDetailsObject));
					}
				}
			}
			boolean matched = matchCount >= requestedCheckDetailsArray.size();
			log("validateEvidenceCheckDetails result", args("result", ConditionResult.INFO.toString(),"res", matched, "count", matchCount));
			return matchCount >= requestedCheckDetailsArray.size();
		} else {
			throw error("Unexpected check_details element in request. Must be an array.",
				args("check_details request", requestedCheckDetails));
		}
	}

	protected boolean validateEvidenceDocumentDetails(JsonElement requestedDocDetails, JsonElement returnedDocDetails, JsonElement expectedDocDetails) {
		boolean isValid = true;
		if(requestedDocDetails != null) {
			if(returnedDocDetails != null) {
				validateElementsAreObjects("evidence document_details", requestedDocDetails, returnedDocDetails);
				JsonObject requestedDocDetailsObject = requestedDocDetails.getAsJsonObject();
				JsonObject returnedDocDetailsObject = returnedDocDetails.getAsJsonObject();
				validateObjectsContainRequiredElements("evidence document_details", requestedDocDetailsObject,returnedDocDetailsObject, "type");
				isValid = compareConstrainableElementList(requestedDocDetailsObject, returnedDocDetailsObject, safeGetElementAsJsonObject(expectedDocDetails), "type", null, "document_number", "serial_number", "date_of_issuance", "date_of_expiry");
				if(isValid && requestedDocDetailsObject.has("issuer")) {
					isValid = validateEvidenceDocumentDetailsIssuer(requestedDocDetailsObject.get("issuer"), safeGetJsonObjectElement(returnedDocDetailsObject, "issuer"), safeGetJsonObjectElement(safeGetElementAsJsonObject(expectedDocDetails), "issuer"));
				}
			} else {
				isValid = false;
			}
		}
		return isValid;
	}

	protected boolean validateEvidenceDocumentDetailsIssuer(JsonElement requestedIssuer, JsonElement returnedIssuer, JsonElement expectedIssuer) {
		boolean isValid = true;
		if(requestedIssuer != null) {
			if(returnedIssuer != null) {
				validateElementsAreObjects("document_details issuer", requestedIssuer, returnedIssuer);
				JsonObject requestedIssuerObject = requestedIssuer.getAsJsonObject();
				JsonObject returnedIssuerObject = returnedIssuer.getAsJsonObject();
				final String[] documentIssuerClaims = {"name", "country_code", "jurisdiction",
					// OIDC address claims
					"formatted", "street_address", "locality", "region", "postal_code", "country"};
				isValid = compareConstrainableElementList(requestedIssuerObject, returnedIssuerObject, safeGetElementAsJsonObject(expectedIssuer), documentIssuerClaims);
			} else {
				isValid = false;
			}
		}
		return isValid;
	}

	protected boolean compareElectronicRecordType(JsonObject requestedEvidence, JsonObject returnedEvidenceObject, JsonObject expectedEvidenceObject) {

		if(returnedEvidenceObject.has("type")) {
			if(OIDFJSON.getString(returnedEvidenceObject.get("type")).equals("electronic_record")) {
				boolean isValid = compareConstrainableElementList(requestedEvidence, returnedEvidenceObject, null, "type");
				if(isValid && requestedEvidence.has("check_details")) {
					isValid = validateEvidenceCheckDetails(requestedEvidence.get("check_details"), safeGetJsonObjectElement(returnedEvidenceObject, "check_details"), safeGetJsonObjectElement(expectedEvidenceObject, "check_details"));
				}
				if(isValid && requestedEvidence.has("record")) {
					isValid = validateEvidenceRecord(requestedEvidence.get("record"), safeGetJsonObjectElement(returnedEvidenceObject, "record"), safeGetJsonObjectElement(expectedEvidenceObject, "record"));
				}
				return isValid;
			} else {
				throw error("Evidence type is not electronic_record", args("evidence type", OIDFJSON.getString(returnedEvidenceObject.get("type"))));
			}
		} else {
			throw error("Evidence missing required type", args("evidence", returnedEvidenceObject));
		}
	}

	protected boolean validateEvidenceRecord(JsonElement requestedRecord, JsonElement returnedRecord, JsonElement expectedRecord) {
		boolean isValid = true;
		if(requestedRecord != null) {
			if(returnedRecord != null) {
				validateElementsAreObjects("evidence record", requestedRecord, returnedRecord);
				JsonObject requestedRecordObject = requestedRecord.getAsJsonObject();
				JsonObject returnedRecordObject = returnedRecord.getAsJsonObject();
				validateObjectsContainRequiredElements("evidence record", requestedRecordObject, returnedRecordObject, "type");
				isValid = compareConstrainableElementList(requestedRecordObject, returnedRecordObject, safeGetElementAsJsonObject(expectedRecord), "type", "created_at", "date_of_expiry");
				if(isValid && requestedRecordObject.has("source")) {
					isValid = validateEvidenceRecordSource(requestedRecordObject.get("source"), returnedRecordObject.get("source"), safeGetJsonObjectElement(safeGetElementAsJsonObject(expectedRecord), "source"));
				}
				// TDOO derived_claims

			} else {
				isValid = false;
			}
		}
		return isValid;
	}

	protected boolean validateEvidenceRecordSource(JsonElement requestedSource, JsonElement returnedSource, JsonElement expectedSource) {
		boolean isValid = true;
		if(requestedSource != null) {
			if(returnedSource != null) {
				validateElementsAreObjects("record source", requestedSource, returnedSource);
				JsonObject requestedSourceObject = requestedSource.getAsJsonObject();
				JsonObject returnedSourceObject = returnedSource.getAsJsonObject();
				final String[] recordSourceClaims = {"name", "country_code", "jurisdiction",
					// OIDC address claims
					"formatted", "street_address", "locality", "region", "postal_code", "country"};
				isValid = compareConstrainableElementList(requestedSourceObject, returnedSourceObject, safeGetElementAsJsonObject(expectedSource), recordSourceClaims);
			} else {
				isValid = false;
			}
		}
		return isValid;
	}

	protected boolean compareVouchType(JsonObject requestedEvidence, JsonObject returnedEvidenceObject, JsonObject expectedEvidenceObject) {

		if(returnedEvidenceObject.has("type")) {
			if(OIDFJSON.getString(returnedEvidenceObject.get("type")).equals("vouch")) {
				boolean isValid = compareConstrainableElementList(requestedEvidence, returnedEvidenceObject, null, "type");
				if(isValid && requestedEvidence.has("check_details")) {
					isValid = validateEvidenceCheckDetails(requestedEvidence.get("check_details"), safeGetJsonObjectElement(returnedEvidenceObject, "check_details"), safeGetJsonObjectElement(safeGetElementAsJsonObject(expectedEvidenceObject), "check_details"));
				}
				if(isValid && requestedEvidence.has("attestation")) {
					isValid = validateEvidenceAttestation(requestedEvidence.get("attestation"), safeGetJsonObjectElement(returnedEvidenceObject, "attestation"), safeGetJsonObjectElement(safeGetElementAsJsonObject(expectedEvidenceObject), "attestation"));
				}
				return isValid;
			} else {
				throw error("Evidence type is not vouch", args("evidence type", OIDFJSON.getString(returnedEvidenceObject.get("type"))));
			}
		} else {
			throw error("Evidence missing required type", args("evidence", returnedEvidenceObject));
		}
	}

	protected boolean validateEvidenceAttestation(JsonElement requestedAttestation, JsonElement returnedAttestation, JsonElement expectedAttestation) {
		boolean isValid = true;
		if(requestedAttestation != null) {
			if(returnedAttestation != null) {
				validateElementsAreObjects("evidence attestation", requestedAttestation, returnedAttestation);
				JsonObject requestedAttestationObject = requestedAttestation.getAsJsonObject();
				JsonObject returnedAttestationObject = returnedAttestation.getAsJsonObject();
				validateObjectsContainRequiredElements("evidence attestation", requestedAttestationObject,returnedAttestationObject, "type");
				isValid = compareConstrainableElementList(requestedAttestationObject, returnedAttestationObject, safeGetElementAsJsonObject(expectedAttestation), "type", "reference_number", "date_of_issuance", "date_of_expiry");
				if(isValid && requestedAttestationObject.has("voucher")) {
					isValid = validateEvidenceAttestationVoucher(requestedAttestationObject.get("voucher"), safeGetJsonObjectElement(returnedAttestationObject, "voucher"), safeGetJsonObjectElement(safeGetElementAsJsonObject(expectedAttestation), "voucher"));
				}
				// TDOO derived_claims
			} else {
				isValid = false;
			}
		}
		return isValid;
	}

	protected boolean validateEvidenceAttestationVoucher(JsonElement requestedVoucher, JsonElement returnedVoucher, JsonElement expectedVoucher) {
		boolean isValid = true;
		if(requestedVoucher != null) {
			if(returnedVoucher != null) {
				validateElementsAreObjects("attestation voucher", requestedVoucher, returnedVoucher);
				JsonObject requestedVoucherObject = requestedVoucher.getAsJsonObject();
				JsonObject returnedVoucherObject = returnedVoucher.getAsJsonObject();
				final String[] attestationVoucherClaims = {"name", "birthdate", "country_code", "occupation", "organization",
					// OIDC address claims
					"formatted", "street_address", "locality", "region", "postal_code", "country"};
				isValid = compareConstrainableElementList(requestedVoucherObject, returnedVoucherObject, safeGetElementAsJsonObject(expectedVoucher), attestationVoucherClaims);
			} else {
				isValid = false;
			}
		}
		return isValid;
	}

	protected boolean compareElectronicSignatureType(JsonObject requestedElectronicSignature, JsonObject returnedElectronicSignature, JsonObject expectedElectronicSignature) {

		if(returnedElectronicSignature.has("type")) {
			if(OIDFJSON.getString(returnedElectronicSignature.get("type")).equals("electronic_signature")) {
				return compareConstrainableElementList(requestedElectronicSignature, returnedElectronicSignature, expectedElectronicSignature, "type", "signature_type", "issuer", "serial_number", "created_at");
				// TDOO derived_claims
			} else {
				throw error("Evidence type is not electronic_signature", args("evidence type", OIDFJSON.getString(returnedElectronicSignature.get("type"))));
			}
		} else {
			throw error("Evidence missing required type", args("evidence", returnedElectronicSignature));
		}
	}

	protected boolean compareConstrainableElementList(JsonObject requested, JsonObject returned, JsonObject expected, String ... elementsList) {
		for(String element : elementsList) {
			if(requested.has(element)) {
				if(!compareConstrainableElement(element, requested.get(element),  safeGetJsonObjectElement(returned, element), safeGetJsonObjectElement(expected, element))) {
					return false;
				}
			}
		}
		return true;
	}

		//TODO how does "essential" affect this behavior? e.g if a "value" was provided but not "essential" can the OP ignore the "value"?
	protected boolean compareConstrainableElement(String claimName, JsonElement requested, JsonElement returned, JsonElement expected) {
		return compareConstrainableElementImpl(claimName, requested, returned, expected,false);
	}

	protected boolean compareConstrainableElementImpl(String claimName, JsonElement requested, JsonElement returned, JsonElement expected, boolean throwOnFalse) {
		if(requested.isJsonObject()) {
			JsonObject requestedObject = requested.getAsJsonObject();
			if(requestedObject.has("value")) {
				if(requestedObject.get("value").equals(returned)) {
					return true;
				} else {
					if(throwOnFalse) {
						throw error("Returned value for " + claimName + " does not match requested value", args("requested", requested, "returned", returned));
					} else {
						log("Returned value for " + claimName + " does not match requested value", args("requested", requested, "returned", returned));
						return false;
					}
				}
			} else if(requestedObject.has("values")) {
				JsonArray values = requestedObject.get("values").getAsJsonArray();
				if(values.contains(returned)) {
					return true;
				} else {
					if(throwOnFalse) {
						throw error("Returned value " + claimName + "is not one of the requested values", args("requested", requested, "returned", returned));
					} else {
						log("Returned value " + claimName + "is not one of the requested values", args("requested", requested, "returned", returned));
						return false;
					}
				}
			} else if(requestedObject.has("essential")) { // essential, no value requested
				if(OIDFJSON.getBoolean(requestedObject.get("essential"))) {
					if(returned == null || returned.isJsonNull()) {
						if(throwOnFalse) {
							throw error(claimName + " was requested as essential but it is not included in the response", args("requested", requested, "returned", returned));
						} else {
							log(claimName + " was requested as essential but it is not included in the response", args("requested", requested, "returned", returned));
							return false;
						}
					} else {
						String returnedValue = OIDFJSON.getString(returned);
						if(Strings.isNullOrEmpty(returnedValue)) {
							//TODO does this require a failure?
							if(throwOnFalse) {
								throw error(claimName + " was requested as essential but it is empty in the response", args("requested", requested, "returned", returned));
							} else {
								log(claimName + " was requested as essential but it is empty in the response", args("requested", requested, "returned", returned));
								return false;
							}
						} else {
							// Check expected value since request did not explicitly request a specific value
							if(checkExpectedValuesConfiguration && (null != expected)) {
								if(!returned.equals(expected)) {
									if(throwOnFalse) {
										throw error(claimName + " was requested as essential but response value did not matched expected value", args("requested", requested, "returned", returned, "expected", expected));
									} else {
										log(claimName + " was requested as essential but response value did not matched expected value", args("requested", requested, "returned", returned, "expected", expected));
										return false;
									}
								} else {
									log(claimName + " was requested as essential, is included in the response which matched expected value", args("requested",
										requested, "returned", returned, "expected", expected));
								}
							} else {
								log(claimName + " was requested as essential, is included in the response", args("requested",
									requested, "returned", returned));
							}
						}
					}
				} else {  // non-essential, no value requested
					if(returned == null || returned.isJsonNull()) {  // no value returned
						log(claimName + " was requested as not essential and no value was returned", args("requested",
							requested, "returned", returned, "expected", expected));
					} else {
						String returnedValue = OIDFJSON.getString(returned);
						if(Strings.isNullOrEmpty(returnedValue)) {
							//TODO does this require a failure?
							log(claimName + " was requested as not essential and empty value was returned", args("requested",
								requested, "returned", returned, "expected", expected));
						} else {
							// Check expected value since request did not explicitly request a specific value
							if(checkExpectedValuesConfiguration && (null != expected)) {
								if(!returned.equals(expected)) {
									if(throwOnFalse) {
										throw error(claimName + " was requested as not essential but response value did not matched expected value", args("requested", requested, "returned", returned, "expected", expected));
									} else {
										log(claimName + " was requested as not essential but response value did not matched expected value", args("requested", requested, "returned", returned, "expected", expected));
										return false;
									}
								} else {
									log(claimName + " was requested as not essential, is included in the response which matched expected value", args("requested",
										requested, "returned", returned, "expected", expected));
								}
							} else {
								log(claimName + " was requested as not essential, is included in the response", args("requested",
									requested, "returned", returned));
							}
						}
					}
				}
			}
		} else if(requested.isJsonNull()) {
			// Check expected value since request did not explicitly request a specific value
			if(checkExpectedValuesConfiguration && (null != returned && !returned.isJsonNull()) && (null != expected)) {
				if(!returned.equals(expected)) {
					if(throwOnFalse) {
						throw error(claimName + " was requested but response value did not matched expected value", args("requested", requested, "returned", returned, "expected", expected));
					} else {
						log(claimName + " was requested but response value did not matched expected value", args("requested", requested, "returned", returned, "expected", expected));
						return false;
					}
				} else {
					log(claimName + " was requested and is included in the response which matched expected value", args("requested",
						requested, "returned", returned, "expected", expected));
				}
			} else {
				log(claimName + " was requested as null, any response is accepted", args("requested",
					requested, "returned", returned));
			}
		}
		return true;
	}

	protected void validateObjectsContainRequiredElements(String objectName, JsonObject requested, JsonObject returned, String ... elementsList) {
		for(String element : elementsList) {
			if(!requested.has(element)) {
				throw error("Request " + objectName + "  is missing required " + element, args("Request", requested));
			}
			if(!returned.has(element)) {
				throw error("Response " + objectName + "  is missing required " + element, args("Response", returned));
			}
		}
	}

	protected void validateElementsAreObjects(String objectName, JsonElement requested, JsonElement returned) {
		if(!requested.isJsonObject()) {
			throw error("Request " + objectName + " is not an object", args("request", requested));
		}
		if(!returned.isJsonObject()) {
			throw error("Response " + objectName + " is not an object", args("response", returned));
		}
	}

	protected boolean checkTime(JsonObject requestedVerification, JsonObject returnedVerification, boolean logSuccess, boolean throwOnFalse) {
		JsonElement requestedTime = requestedVerification.get("time");
		if(requestedTime.isJsonNull()) {
			//time must be present
			if(returnedVerification.has("time")) {
				if(logSuccess) {
					logSuccess("As requested, verification.time element is present in returned response",
						args("time", returnedVerification.get("time")));
				}
				return true;
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
					if(throwOnFalse) {
						throw error("Verification time is before the requested max_age",
							args("max_age", maxAge, "now", now.toString(), "verificationTime", returnedTimeInISOFormat));
					} else {
						return false;
					}
				} else {
					if(logSuccess) {
						logSuccess("Verification time is within allowed limits",
							args("max_age", maxAge, "now", now.toString(), "verificationTime", returnedTimeInISOFormat));
						return true;
					}
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	protected boolean compareTrustFrameworks(JsonElement requestedTrustFramework, JsonElement returnedTrustFramework, JsonElement expectedTrustFamework) {
		return compareConstrainableElementImpl("trust_framework", requestedTrustFramework, returnedTrustFramework, expectedTrustFamework, false);
	}
}
