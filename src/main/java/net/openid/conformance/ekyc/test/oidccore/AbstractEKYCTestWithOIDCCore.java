package net.openid.conformance.ekyc.test.oidccore;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.ekyc.condition.client.AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequestUsingJsonNull;
import net.openid.conformance.ekyc.condition.client.AddUnverifiedClaimsToAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.CreateUnverifiedClaimsToRequestInAuthorizationEndpointRequest;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromIdToken;
import net.openid.conformance.ekyc.condition.client.ExtractVerifiedClaimsFromUserinfoResponse;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInIdTokenAgainstOPMetadata;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInIdTokenAgainstRequest;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoAgainstOPMetadata;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoResponseAgainstRequest;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsResponseAgainstSchema;
import net.openid.conformance.openid.AbstractOIDCCServerTest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;

public abstract class AbstractEKYCTestWithOIDCCore extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(new SetupPkceAndAddToAuthorizationRequest());
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		super.createAuthorizationCodeRequest();

		callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class, "RFC7636-4.5");
	}

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		// add claims
		// authorization_endpoint_request
		addUnverifiedClaimsToAuthorizationRequest();
		addVerifiedClaimsToAuthorizationRequest();
	}

	protected void addUnverifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(CreateUnverifiedClaimsToRequestInAuthorizationEndpointRequest.class);
		callAndContinueOnFailure(AddUnverifiedClaimsToAuthorizationEndpointRequest.class, "IA-6");
	}

	protected void addVerifiedClaimsToAuthorizationRequest() {
		callAndContinueOnFailure(AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequestUsingJsonNull.class, "IA-6");
	}

	@Override
	protected void performIdTokenValidation() {
		super.performIdTokenValidation();
		processVerifiedClaimsInIdToken();
	}

	protected void processVerifiedClaimsInIdToken() {
		callAndStopOnFailure(ExtractVerifiedClaimsFromIdToken.class, Condition.ConditionResult.FAILURE, "IA-5");
		validateVerifiedClaimsResponseSchema();
		ensureReturnedVerifiedClaimsMatchOPMetadata(false);
		validateIdTokenVerifiedClaimsAgainstRequested();
	}

	protected void validateIdTokenVerifiedClaimsAgainstRequested() {
		callAndContinueOnFailure(ValidateVerifiedClaimsInIdTokenAgainstRequest.class, Condition.ConditionResult.FAILURE, "IA-6");
	}

	protected void ensureReturnedVerifiedClaimsMatchOPMetadata(boolean isUserinfo) {
		if(isUserinfo){
			callAndContinueOnFailure(ValidateVerifiedClaimsInUserinfoAgainstOPMetadata.class, Condition.ConditionResult.FAILURE, "IA-9");
		} else {
			callAndContinueOnFailure(ValidateVerifiedClaimsInIdTokenAgainstOPMetadata.class, Condition.ConditionResult.FAILURE, "IA-9");
		}
	}

	protected void validateVerifiedClaimsResponseSchema() {
		callAndContinueOnFailure(ValidateVerifiedClaimsResponseAgainstSchema.class, Condition.ConditionResult.FAILURE, "IA-6");
	}

	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Userinfo endpoint tests");
		callAndStopOnFailure(CallProtectedResource.class);
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		processVerifiedClaimsInUserinfo();
		eventLog.endBlock();
	}

	protected void processVerifiedClaimsInUserinfo() {
		callAndContinueOnFailure(ExtractVerifiedClaimsFromUserinfoResponse.class, Condition.ConditionResult.FAILURE, "IA-5");
		validateVerifiedClaimsResponseSchema();
		ensureReturnedVerifiedClaimsMatchOPMetadata(true);
		validateUserinfoVerifiedClaimsAgainstRequested();
	}

	protected void validateUserinfoVerifiedClaimsAgainstRequested() {
		callAndContinueOnFailure(ValidateVerifiedClaimsInUserinfoResponseAgainstRequest.class, Condition.ConditionResult.FAILURE, "IA-6");
	}
	protected void validateIdDocument() {
		//5.1.1.1. id_document
		//type: REQUIRED. Value MUST be set to "id_document".
		//method: The method used to verify the ID document. For information on predefined verification method values see Section 12.
		//verifier: JSON object denoting the legal entity that performed the identity verification on behalf of the OP. This object SHOULD only be included if the OP did not perform the identity verification itself. This object consists of the following properties:
		//
		//organization: String denoting the organization which performed the verification on behalf of the OP.
		//
		//txn: Identifier referring to the identity verification transaction. This transaction identifier can be resolved into transaction details during an audit.
		//time: Time stamp in ISO 8601:2004 [ISO8601-2004] YYYY-MM-DDThh:mm[:ss]TZD format representing the date when this ID document was verified.
		//document: JSON object representing the ID document used to perform the identity verification. It consists of the following properties:
			//type: REQUIRED. String denoting the type of the ID document. For information on predefined identity document values see Section 12. The OP MAY use other than the predefined values in which case the RPs will either be unable to process the assertion, just store this value for audit purposes, or apply bespoken business logic to it.
			//number: String representing the number of the identity document.
			//issuer: JSON object containing information about the issuer of this identity document. This object consists of the following properties:
			//name: Designation of the issuer of the identity document.
			//country: String denoting the country or organization that issued the document as ICAO 2-letter code [ICAO-Doc9303], e.g. "JP". ICAO 3-letter codes MAY be used when there is no corresponding ISO 2-letter code, such as "UNO".
			//date_of_issuance: The date the document was issued as ISO 8601:2004 YYYY-MM-DD format.
			//date_of_expiry: The date the document will expire as ISO 8601:2004 YYYY-MM-DD format.
	}

	protected void validateUtilityBill() {
		//5.1.1.2. utility_bill
		//The following elements are contained in a utility_bill evidence sub-element.
		//	type: REQUIRED. Value MUST be set to "utility_bill".
		//	provider: JSON object identifying the respective provider that issued the bill. The object consists of the following properties:
		//	name: String designating the provider.
		//	All elements of the OpenID Connect address Claim ([OpenID])
		//	date: String in ISO 8601:2004 YYYY-MM-DD format containing the date when this bill was issued.
	}

	protected void validateQes() {
		//The following elements are contained in a qes evidence sub-element.
		//	type: REQUIRED. Value MUST be set to "qes".
		//	issuer: String denoting the certification authority that issued the signer's certificate.
		//	serial_number: String containing the serial number of the certificate used to sign.
		//	created_at: The time the signature was created as ISO 8601:2004 YYYY-MM-DDThh:mm[:ss]TZD format.
	}
}
