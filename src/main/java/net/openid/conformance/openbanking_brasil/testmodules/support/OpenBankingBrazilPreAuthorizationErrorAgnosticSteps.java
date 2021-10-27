package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

public class OpenBankingBrazilPreAuthorizationErrorAgnosticSteps extends AbstractConditionSequence {

	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
	}

	@Override
	public void evaluate() {

		call(exec().startBlock("Use client_credentials grant to obtain Brazil consent"));

		/* create client credentials request */

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

		callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);

		call(sequence(addClientAuthenticationToTokenEndpointRequest));

		/* get access token */

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, "RFC6749-4.4.3", "RFC6749-5.1");

		call(condition(ValidateExpiresIn.class)
			.skipIfObjectMissing("expires_in")
			.onSkip(Condition.ConditionResult.INFO)
			.requirements("RFC6749-5.1")
			.onFail(Condition.ConditionResult.FAILURE)
			.dontStopOnFailure());

		/* create consent request */

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);

		// as per https://github.com/OpenBanking-Brasil/areadesenvolvedor/blob/master/documentation/source/swagger/swagger_payments_apis.yaml
		callAndStopOnFailure(CreateIdempotencyKey.class);
		callAndStopOnFailure(AddIdempotencyKeyHeader.class);

		callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

		// to retrieve the organisation id
		callAndStopOnFailure(FAPIBrazilExtractClientMTLSCertificateSubject.class);

		// we reuse the request object conditions to add various jwt claims; it would perhaps make sense to make
		// these more generic.
		call(exec().mapKey("request_object_claims", "consent_endpoint_request"));

		// aud (in the JWT request): the Resource Provider (eg the institution holding the account) must validate if the value of the aud field matches the endpoint being triggered;
		callAndStopOnFailure(AddAudAsPaymentConsentUriToRequestObject.class, "BrazilOB-6.1");

		//iss (in the JWT request and in the JWT response): the receiver of the message shall validate if the value of the iss field matches the organisationId of the sender;
		callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");

		//jti (in the JWT request and in the JWT response): the value of the jti field shall be filled with the UUID defined by the institution according to [RFC4122] version 4;
		callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");

		//iat (in the JWT request and in the JWT response): the iat field shall be filled with the message generation time and according to the standard established in [RFC7519](https:// datatracker.ietf.org/doc/html/rfc7519#section-2) to the NumericDate format.
		callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

		call(exec().unmapKey("request_object_claims"));

		callAndStopOnFailure(FAPIBrazilSignPaymentConsentRequest.class);

		callAndStopOnFailure(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class);

		call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));
		call(exec().mapKey("endpoint_response_jwt", "consent_endpoint_response_jwt"));

		callAndStopOnFailure(OptionallyAllow201Or422.class, Condition.ConditionResult.SUCCESS);

		call(condition(EnsureContentTypeApplicationJwt.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(condition(EnsureHttpStatusCodeIs201.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.skipIfStringMissing("proceed_with_test"));

		call(condition(ExtractSignedJwtFromResourceResponse.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(condition(ExtractSignedJwtFromResourceResponse.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.skipIfStringMissing("proceed_with_test"));

		call(condition(FAPIBrazilValidateResourceResponseTyp.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		// signature needs to be validated against the organisation jwks
		call(condition(FAPIBrazilGetKeystoreJwksUri.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.skipIfStringMissing("proceed_with_test"));


		call(exec().mapKey("server", "org_server"));
		call(exec().mapKey("server_jwks", "org_server_jwks"));

		call(condition(FetchServerKeys.class)
			.skipIfStringMissing("proceed_with_test"));

		call(exec().unmapKey("server"));
		call(exec().unmapKey("server_jwks"));

		call(condition(ValidateResourceResponseSignature.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(condition(ValidateResourceResponseJwtClaims.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("BrazilOB-6.1")
			.skipIfStringMissing("proceed_with_test"));

		call(exec().unmapKey("endpoint_response"));
		call(exec().unmapKey("endpoint_response_jwt"));

		call(condition(ExtractConsentIdFromConsentEndpointResponse.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.skipIfStringMissing("proceed_with_test"));

		call(condition(CheckForFAPIInteractionIdInResourceResponse.class)
			.dontStopOnFailure()
			.onFail(Condition.ConditionResult.FAILURE)
			.requirements("FAPI-R-6.2.1-11", "FAPI1-BASE-6.2.1-11")
			.skipIfStringMissing("proceed_with_test"));

		call(condition(FAPIBrazilAddConsentIdToClientScope.class)
			.onFail(Condition.ConditionResult.FAILURE)
			.skipIfStringMissing("proceed_with_test"));

		call(exec().endBlock());
	}
}
