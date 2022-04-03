package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAudAsPaymentConsentUriToRequestObject;
import net.openid.conformance.condition.client.AddDpopHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.AddDpopHeaderForTokenEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIdempotencyKeyHeader;
import net.openid.conformance.condition.client.AddIssAsCertificateOuToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.CallConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CreateDpopClaims;
import net.openid.conformance.condition.client.CreateDpopHeader;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateIdempotencyKey;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractConsentIdFromConsentEndpointResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractSignedJwtFromResourceResponse;
import net.openid.conformance.condition.client.FAPIBrazilAddConsentIdToClientScope;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCallPaymentConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilConsentEndpointResponseValidatePermissions;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilExtractClientMTLSCertificateSubject;
import net.openid.conformance.condition.client.FAPIBrazilGetKeystoreJwksUri;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilValidateResourceResponseSigningAlg;
import net.openid.conformance.condition.client.FAPIBrazilValidateResourceResponseTyp;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetDpopAccessTokenHash;
import net.openid.conformance.condition.client.SetDpopHtmHtuForConsentEndpoint;
import net.openid.conformance.condition.client.SetDpopHtmHtuForTokenEndpoint;
import net.openid.conformance.condition.client.SetPaymentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateOrganizationJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateResourceResponseJwtClaims;
import net.openid.conformance.condition.client.ValidateResourceResponseSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

public class OpenBankingBrazilPreAuthorizationSteps extends AbstractConditionSequence {

	private boolean payments;
	private boolean dpop;
	private boolean stopAfterConsentEndpointCall;
	private String currentClient;
	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public OpenBankingBrazilPreAuthorizationSteps(boolean secondClient, boolean dpop, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest, boolean payments, boolean stopAfterConsentEndpointCall) {
		this.currentClient = secondClient ? "Second client: " : "";
		this.dpop = dpop;
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
		this.payments = payments;
		this.stopAfterConsentEndpointCall = stopAfterConsentEndpointCall;
	}

		@Override
	public void evaluate() {
		call(exec().startBlock(currentClient + "Use client_credentials grant to obtain Brazil consent"));

		/* create client credentials request */

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

		if (payments) {
			callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);
		} else {
			callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
		}

		call(sequence(addClientAuthenticationToTokenEndpointRequest));

		if (dpop) {
			callAndStopOnFailure(GenerateDpopKey.class);
			callAndStopOnFailure(CreateDpopHeader.class);
			callAndStopOnFailure(CreateDpopClaims.class);
			callAndStopOnFailure(SetDpopHtmHtuForTokenEndpoint.class);
			callAndStopOnFailure(SignDpopProof.class);
			callAndStopOnFailure(AddDpopHeaderForTokenEndpointRequest.class);
		}

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

		if (payments) {
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

			callAndStopOnFailure(ValidateOrganizationJWKsPrivatePart.class);

			if (dpop) {
				callAndStopOnFailure(CreateDpopHeader.class);
				callAndStopOnFailure(CreateDpopClaims.class);
				callAndStopOnFailure(SetDpopHtmHtuForConsentEndpoint.class);
				callAndStopOnFailure(SetDpopAccessTokenHash.class);
				callAndStopOnFailure(SignDpopProof.class);
				callAndStopOnFailure(AddDpopHeaderForResourceEndpointRequest.class);
			}

			callAndStopOnFailure(FAPIBrazilSignPaymentConsentRequest.class);

			callAndStopOnFailure(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class);

			if (stopAfterConsentEndpointCall) {
				return;
			}

			call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));
			call(exec().mapKey("endpoint_response_jwt", "consent_endpoint_response_jwt"));

			callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE);

			callAndStopOnFailure(ExtractSignedJwtFromResourceResponse.class, "BrazilOB-6.1");

			callAndContinueOnFailure(FAPIBrazilValidateResourceResponseSigningAlg.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

			callAndContinueOnFailure(FAPIBrazilValidateResourceResponseTyp.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

			// signature needs to be validated against the organisation jwks
			callAndStopOnFailure(FAPIBrazilGetKeystoreJwksUri.class, Condition.ConditionResult.FAILURE);

			call(exec().mapKey("server", "org_server"));
			call(exec().mapKey("server_jwks", "org_server_jwks"));
			callAndStopOnFailure(FetchServerKeys.class);
			call(exec().unmapKey("server"));
			call(exec().unmapKey("server_jwks"));

			callAndContinueOnFailure(ValidateResourceResponseSignature.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

			callAndContinueOnFailure(ValidateResourceResponseJwtClaims.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

			call(exec().unmapKey("endpoint_response"));
			call(exec().unmapKey("endpoint_response_jwt"));
		} else {
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);

			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);

			if (dpop) {
				callAndStopOnFailure(CreateDpopHeader.class);
				callAndStopOnFailure(CreateDpopClaims.class);
				callAndStopOnFailure(SetDpopHtmHtuForConsentEndpoint.class);
				callAndStopOnFailure(SetDpopAccessTokenHash.class);
				callAndStopOnFailure(SignDpopProof.class);
				callAndStopOnFailure(AddDpopHeaderForResourceEndpointRequest.class);
			}

			callAndStopOnFailure(CallConsentEndpointWithBearerToken.class);
			if (stopAfterConsentEndpointCall) {
				return;
			}
			call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
			call(exec().unmapKey("endpoint_response"));

			callAndContinueOnFailure(FAPIBrazilConsentEndpointResponseValidatePermissions.class, Condition.ConditionResult.FAILURE);
		}

		callAndStopOnFailure(ExtractConsentIdFromConsentEndpointResponse.class);

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11", "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(FAPIBrazilAddConsentIdToClientScope.class);

		call(exec().endBlock());
	}
}
