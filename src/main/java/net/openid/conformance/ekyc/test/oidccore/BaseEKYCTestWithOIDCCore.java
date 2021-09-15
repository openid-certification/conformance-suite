package net.openid.conformance.ekyc.test.oidccore;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client._SetRedirectUriToYesComTestClientRedirectUri;
import net.openid.conformance.condition.client.ekyc.AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.ekyc.AddUnverifiedClaimsToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.ekyc.ValidateVerifiedClaimsInIdToken;
import net.openid.conformance.condition.client.ekyc.ValidateVerifiedClaimsResponseAgainstSchema;
import net.openid.conformance.condition.client.ekyc.CreateUnverifiedClaimsToRequestInAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.ekyc.ExtractVerifiedClaimsFromIdToken;
import net.openid.conformance.condition.client.ekyc.ExtractVerifiedClaimsFromUserinfoResponse;
import net.openid.conformance.condition.client.ekyc.ValidateClaimsInVerifiedClaimsSupportedInServerConfiguration;
import net.openid.conformance.condition.client.ekyc.ValidateEvidenceSupportedInServerConfiguration;
import net.openid.conformance.condition.client.ekyc.ValidateIdDocumentsSupportedInServerConfiguration;
import net.openid.conformance.condition.client.ekyc.ValidateIdDocumentsVerificationMethodsSupportedInServerConfiguration;
import net.openid.conformance.condition.client.ekyc.EnsureVerifiedClaimsSupportedParameterIsTrue;
import net.openid.conformance.condition.client.ekyc.ValidateTrustFrameworksSupportedInServerConfiguration;
import net.openid.conformance.condition.client.ekyc.ValidateVerifiedClaimsInUserinfoResponse;
import net.openid.conformance.openid.OIDCCServerTest;

public class BaseEKYCTestWithOIDCCore extends OIDCCServerTest {
	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		callAndStopOnFailure(_SetRedirectUriToYesComTestClientRedirectUri.class);
		validateEKYCSpecificServerConfiguration();
	}
	protected void validateEKYCSpecificServerConfiguration() {
		//IA-8, The OP MUST support the claims parameter and needs to publish this in its openid-configuration using the claims_parameter_supported element.
		callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "IA-8");
		//verified_claims_supported: Boolean value indicating support for verified_claims, i.e. the OpenID Connect for Identity Assurance extension.
		//Boolean value indicating support for verified_claims, i.e. the OpenID Connect for Identity Assurance extension
		//TODO I assumed this must always be true for eKYC tests. Please confirm
		callAndContinueOnFailure(EnsureVerifiedClaimsSupportedParameterIsTrue.class, Condition.ConditionResult.FAILURE, "IA-8");

		//TODO clarify validation requirements for the following. e.g are null values allowed, what are allowed/disallowed values etc
		//trust_frameworks_supported: JSON array containing all supported trust frameworks.
		validateTrustFrameworksSupportedInServerConfiguration();

		//evidence_supported: JSON array containing all types of identity evidence the OP uses.
		validateEvidenceSupportedInServerConfiguration();

		//id_documents_supported: JSON array containing all identity documents utilized by the OP for identity verification.
		validateIdDocumentsSupportedInServerConfiguration();

		//id_documents_verification_methods_supported: JSON array containing the ID document verification methods the OP supports as defined in Section 5.1.
		validateIdDocumentsVerificationMethodsSupportedInServerConfiguration();

		//claims_in_verified_claims_supported: JSON array containing all claims supported within verified_claims.
		validateClaimsInVerifiedClaimsSupported();

	}

	protected void validateTrustFrameworksSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateTrustFrameworksSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-8", "IA-5.1");
	}

	protected void validateEvidenceSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateEvidenceSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-8", "IA-5.1");
	}

	protected void validateIdDocumentsSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateIdDocumentsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-8", "IA-5.1");
	}

	protected void validateIdDocumentsVerificationMethodsSupportedInServerConfiguration() {
		callAndContinueOnFailure(ValidateIdDocumentsVerificationMethodsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-8", "IA-5.1");
	}

	protected void validateClaimsInVerifiedClaimsSupported() {
		callAndContinueOnFailure(ValidateClaimsInVerifiedClaimsSupportedInServerConfiguration.class, Condition.ConditionResult.FAILURE, "IA-8");
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
		callAndContinueOnFailure(AddOnlyOneSimpleVerifiedClaimToAuthorizationEndpointRequest.class, "IA-6");
	}

	@Override
	protected void performIdTokenValidation() {
		super.performIdTokenValidation();
		callAndContinueOnFailure(ExtractVerifiedClaimsFromIdToken.class, Condition.ConditionResult.FAILURE, "IA-5");
		validateVerifiedClaimsResponse();
		callAndContinueOnFailure(ValidateVerifiedClaimsInIdToken.class, Condition.ConditionResult.FAILURE, "IA-6");

	}

	protected void validateVerifiedClaimsResponse() {
		callAndContinueOnFailure(ValidateVerifiedClaimsResponseAgainstSchema.class, Condition.ConditionResult.FAILURE, "IA-6");
	}

	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Userinfo endpoint tests");
		callAndStopOnFailure(CallProtectedResourceWithBearerToken.class);
		callAndContinueOnFailure(ExtractVerifiedClaimsFromUserinfoResponse.class, Condition.ConditionResult.FAILURE, "IA-5");
		validateVerifiedClaimsResponse();
		callAndContinueOnFailure(ValidateVerifiedClaimsInUserinfoResponse.class, Condition.ConditionResult.FAILURE, "IA-6");
		eventLog.endBlock();
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
