package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddDpopHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallProtectedResourceForceBearer;
import net.openid.conformance.condition.client.CreateDpopClaims;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.condition.client.Fapi2DPoPNegativeConditions;
import net.openid.conformance.condition.client.SetDpopAccessTokenHash;
import net.openid.conformance.condition.client.SetDpopAccessTokenHashToIncorrectValue;
import net.openid.conformance.condition.client.SetDpopHeaderJwkToPrivateKey;
import net.openid.conformance.condition.client.SetDpopHtmHtuForResourceEndpoint;
import net.openid.conformance.condition.client.SetDpopIatToOneHourInFuture;
import net.openid.conformance.condition.client.SetDpopIatToOneHourInPast;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-dpop-negative-tests",
	displayName = "FAPI2-Security-Profile-ID2: DPoP negative tests",
	summary = "Obtain an access token as normal, check it works, then various negative tests to check the server is implementing the checks required by the DPoP specification.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPI2SenderConstrainMethod.class, values={"mtls"})
public class FAPI2SPID2DpopNegativeTests extends AbstractFAPI2SPID2ServerTestModule {

	class CallResourceEndpointSteps extends AbstractConditionSequence {
		boolean expectSuccess;
		boolean shouldFail;
		boolean forceBearer;

		String[] requirements;

		public CallResourceEndpointSteps(boolean expectSuccess, boolean shouldFail, String... requirements) {
			this(expectSuccess, shouldFail, false, requirements);
		}

		public CallResourceEndpointSteps(boolean expectSuccess, boolean shouldFail, boolean forceBearer, String... requirements) {
			this.expectSuccess = expectSuccess;
			this.shouldFail = shouldFail;
			this.forceBearer = forceBearer;
			this.requirements = requirements;
		}

		@Override
		public void evaluate() {
			call(makeUpdateResourceRequestSteps());
			if (forceBearer) {
				callAndStopOnFailure(CallProtectedResourceForceBearer.class, "RFC7231-5.3.2");
			}
			else {
				callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
			}
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			Condition.ConditionResult result = Condition.ConditionResult.FAILURE;
			if (!shouldFail) {
				result = Condition.ConditionResult.WARNING;
			}
			if (expectSuccess) {
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, result, requirements);
			} else {
				callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, result, requirements);
			}
			call(exec().unmapKey("endpoint_response"));
		}
	}

	@Override
	protected void requestProtectedResource() {
		super.requestProtectedResource();

		// as per https://www.ietf.org/archive/id/draft-ietf-oauth-dpop-07.html#section-4.3:

		eventLog.startBlock("Try DPoP proof with all upper case header value");
		call(new CallResourceEndpointSteps(true, false, "DPOP-4.1")
			.replace(AddDpopHeaderForResourceEndpointRequest.class,
				condition(Fapi2DPoPNegativeConditions.AddDpopHeaderAllCapital.class)));

		// 1. that there is not more than one DPoP header in the request,
		eventLog.startBlock("Try with more than one DPoP in the header");
		call(new CallResourceEndpointSteps(false, true, "DPOP-7.1")
			.replace(AddDpopHeaderForResourceEndpointRequest.class,
				sequence(Fapi2DPoPNegativeConditions.MultipleProofs.class)));

		// 2. the string value of the header field is a well-formed JWT,
		eventLog.startBlock("Try DPoP proof not well-formed JWT");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.insertAfter(SignDpopProof.class,
				condition(Fapi2DPoPNegativeConditions.NotWellformedDPoP.class)));

		// 3. all required claims per Section 4.2 are contained in the JWT,
		eventLog.startBlock("Try DPoP proof where 'typ' is missing in header");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(Fapi2DPoPNegativeConditions.RemoveTypFromDpopProof.class)));
		eventLog.startBlock("Try DPoP proof where 'alg' is missing");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.insertAfter(SignDpopProof.class, condition(Fapi2DPoPNegativeConditions.SignDpopAndRemoveAlg.class)));
		eventLog.startBlock("Try DPoP proof where 'jwk' is missing");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(Fapi2DPoPNegativeConditions.RemoveJwkFromDpopProof.class)));
		eventLog.startBlock("Try DPoP proof where 'jti' is missing");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(Fapi2DPoPNegativeConditions.RemoveJtiFromDpopProof.class)));
		eventLog.startBlock("Try DPoP proof where 'htm' is missing");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(Fapi2DPoPNegativeConditions.RemoveHtmFromDpopProof.class)));
		eventLog.startBlock("Try DPoP proof where 'htu' is missing");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(Fapi2DPoPNegativeConditions.RemoveHtuFromDpopProof.class)));
		eventLog.startBlock("Try DPoP proof where 'iat' is missing");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(Fapi2DPoPNegativeConditions.RemoveIatFromDpopProof.class)));
		eventLog.startBlock("Try DPoP proof where 'ath' is missing");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.skip(SetDpopAccessTokenHash.class, "Skipping adding DPoP ATH"));

		// 4. the typ field in the header has the value dpop+jwt,
		eventLog.startBlock("Try DPoP proof with invalid 'typ' in header");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.2")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(Fapi2DPoPNegativeConditions.SetDpopHeaderTypToInvalidValue.class)));

		// 5. the algorithm in the header of the JWT indicates an asymmetric digital signature algorithm, is not none, is supported by the application, and is deemed secure,
		// I think mostly this is one that can only be tested at the token endpoint, but there are a few things we can try:
		eventLog.startBlock("Try DPoP proof signed using RS256");
		call(new CallResourceEndpointSteps(false, true, "FAPI2-SP-ID2-5.4")
			.insertBefore(SignDpopProof.class,
				condition(Fapi2DPoPNegativeConditions.ChangeSignAlgorithm.class)));

		eventLog.startBlock("Try DPoP proof with none alg");
		call(new CallResourceEndpointSteps(false, true, "FAPI2-SP-ID2-5.4")
			.replace(SignDpopProof.class,
				condition(Fapi2DPoPNegativeConditions.SignDpopProofWithNone.class)));

		// 6. the JWT signature verifies with the public key contained in the jwk header of the JWT,
		eventLog.startBlock("Try DPoP proof with invalid signature");
		call(new CallResourceEndpointSteps(false, true, "FAPI2-SP-ID2-5.4")
			.insertAfter(SignDpopProof.class,
				condition(Fapi2DPoPNegativeConditions.InvalidateDpopProofSignature.class)));

		// 7. the jwk header of the JWT does not contain a private key,
		eventLog.startBlock("Try DPoP proof with jwk header incorrectly containing private key");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.3")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(SetDpopHeaderJwkToPrivateKey.class)));

		// 8. the htm claim matches the HTTP method value of the HTTP request in which the JWT was received,
		eventLog.startBlock("Try DPoP proof with incorrect 'htm'");
		call(new CallResourceEndpointSteps(false, true, "DPOP-4.3")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(Fapi2DPoPNegativeConditions.SetDpopHtmToPut.class)));

		// 9. the htu claim matches the HTTPS URI value for the HTTP request in which the JWT was received, ignoring any query and fragment parts,
		eventLog.startBlock("Try DPoP proof where 'htu' has a query/fragment (which must be ignored in match as per 4.3-9 in DPoP spec)");
		call(new CallResourceEndpointSteps(true, true, "DPOP-4.3")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(Fapi2DPoPNegativeConditions.AddQueryAndFragmentToDpopHtu.class)));

		eventLog.startBlock("Try DPoP proof where 'htu' does not contain the query/fragment (which must be ignored in match as per 4.3-9 in DPoP spec)");
		call(new CallResourceEndpointSteps(true, true, "DPOP-4.3")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(Fapi2DPoPNegativeConditions.RemoveQueryAndFragmentFromDpopHtu.class)));



		eventLog.startBlock("Try DPoP proof where 'htu' is a different url");
		call(new CallResourceEndpointSteps(false, true, "DPOP-7.1")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(Fapi2DPoPNegativeConditions.SetDpopHtuToDifferentUrl.class)));


		// 10. if the server provided a nonce value to the client, the nonce claim matches the server-provided nonce value,
		// We're not doing nonces yet

		// 11. the iat claim value is within an acceptable timeframe and, within a reasonable consideration of accuracy and resource utilization, a proof JWT with the same jti value has not previously been received at the same resource during that time period (see Section 11.1),
		// iat is expected to be within seconds/minutes of current time as per https://mailarchive.ietf.org/arch/msg/oauth/CC0ZlExBdZFOjO2ltgkJ3w6VioI/
		// This might need to be changed when we support nonces if the server is using nonce; there's been discussion on the IETF OAuth list about the nonce check replacing the iat check
		// https://mailarchive.ietf.org/arch/msg/oauth/T4stTh9mQRExvZTdEC30OF541p0/
		eventLog.startBlock("Try DPoP proof where 'iat' is one hour in the future");
		call(new CallResourceEndpointSteps(false, true, "DPOP-7.1")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(SetDpopIatToOneHourInFuture.class)));

		eventLog.startBlock("Try DPoP proof where 'iat' is one hour in the past");
		call(new CallResourceEndpointSteps(false, true, "DPOP-7.1")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(SetDpopIatToOneHourInPast.class)));

		eventLog.startBlock("DPoP reuse, First use of jti");
		call(new CallResourceEndpointSteps(true, true, "DPOP-7.1")
			.insertAfter(CreateDpopClaims.class,
				condition(Fapi2DPoPNegativeConditions.FixedJtiClaim.class)));
		eventLog.startBlock("DPoP reuse, Second use of the same jti, this 'should' fail");
		call(new CallResourceEndpointSteps(false, false, "DPOP-7.1")
			.insertAfter(CreateDpopClaims.class,
				condition(Fapi2DPoPNegativeConditions.FixedJtiClaim.class)));

		// 12 if presented to a protected resource in conjunction with an access token,
		// 12.1 ensure that the value of the ath claim equals the hash of that access token,
		eventLog.startBlock("Try DPoP proof where 'ath' is incorrect");
		call(new CallResourceEndpointSteps(false, true, "DPOP-7.1")
			.replace(SetDpopAccessTokenHash.class,
				condition(SetDpopAccessTokenHashToIncorrectValue.class)));

		// 12.2 confirm that the public key to which the access token is bound matches the public key from the DPoP proof.
		eventLog.startBlock("Try DPoP signed with a different key");
		call(new CallResourceEndpointSteps(false, true, "DPOP-7.1")
			.insertBefore(SignDpopProof.class,
				condition(Fapi2DPoPNegativeConditions.GenerateNewSignKey.class))
			.insertAfter(SignDpopProof.class,
				condition(Fapi2DPoPNegativeConditions.RecoverSignKey.class)));

		// try proof with unknown values in header/body (should succeed)
		eventLog.startBlock("Try DPoP proof with extra claims on header and claims, as it should be ignored by resource server");
		call(new CallResourceEndpointSteps(true, true, "DPOP-7.2")
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(Fapi2DPoPNegativeConditions.AddExtraClaimsToHeader.class))
			.insertAfter(SetDpopHtmHtuForResourceEndpoint.class,
				condition(Fapi2DPoPNegativeConditions.AddExtraClaimsToClaims.class))
		);

		// Servers SHOULD employ Syntax-Based Normalization and Scheme-Based Normalization in accordance with Section 6.2.2. and Section 6.2.3. of [RFC3986] before comparing the htu claim.
		eventLog.startBlock("Try DPoP proof expecting RS to compare scheme and hostname using case independent mode when validating htu claim");
		call(new CallResourceEndpointSteps(true, false, "RFC3986-6.2.2","RFC3986-6.2.3")
			.replace(SetDpopHtmHtuForResourceEndpoint.class,
				condition(Fapi2DPoPNegativeConditions.DpopHtuUpperCase.class)));

		eventLog.startBlock("Try DPoP proof expecting Scheme based normalization of htu claim, where the port is not considered if that is the default for the scheme.");
		call(new CallResourceEndpointSteps(true, false, "RFC3986-6.2.2","RFC3986-6.2.3")
			.replace(SetDpopHtmHtuForResourceEndpoint.class,
				condition(Fapi2DPoPNegativeConditions.DpopHtuWithPort.class)));

		eventLog.startBlock("Try resource access without DPoP proof");
		call(new CallResourceEndpointSteps(false, true, "FAPI2-BASE-4.3.3")
			.insertAfter(AddDpopHeaderForResourceEndpointRequest.class,
				condition(Fapi2DPoPNegativeConditions.RemoveDpopFromResourceRequest.class)));

		eventLog.startBlock("Try resource access without DPoP proof and authorization type changed to 'Bearer'");
		call(new CallResourceEndpointSteps(false, true, true, "DPOP-7.1")
			.insertAfter(AddDpopHeaderForResourceEndpointRequest.class,
				condition(Fapi2DPoPNegativeConditions.RemoveDpopFromResourceRequest.class)));

		// This is a final sanity check to make sure that all the above tests failed because of the invalid dpop proofs,
		// and not because the access token had stopped working for some reason etc.
		eventLog.startBlock("Check a correct DPoP proof still works");
		call(new CallResourceEndpointSteps(true, true, "DPOP-7.1"));
		if (brazilPayments) {
			validateBrazilPaymentInitiationSignedResponse();
		}


	}
}
