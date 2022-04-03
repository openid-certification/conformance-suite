package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.InvalidateDpopProofSignature;
import net.openid.conformance.condition.client.AddQueryAndFragmentToDpopHtu;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200or201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.condition.client.RemoveHtuFromDpopProof;
import net.openid.conformance.condition.client.RemoveIatFromDpopProof;
import net.openid.conformance.condition.client.SetDpopAccessTokenHash;
import net.openid.conformance.condition.client.SetDpopAccessTokenHashToIncorrectValue;
import net.openid.conformance.condition.client.SetDpopHeaderJwkToPrivateKey;
import net.openid.conformance.condition.client.SetDpopHeaderTypToInvalidValue;
import net.openid.conformance.condition.client.SetDpopHtmHtuForResourceEndpoint;
import net.openid.conformance.condition.client.SetDpopHtmToPut;
import net.openid.conformance.condition.client.SetDpopHtuToDifferentUrl;
import net.openid.conformance.condition.client.SetDpopIatToOneHourInFuture;
import net.openid.conformance.condition.client.SetDpopIatToOneHourInPast;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-baseline-id2-dpop-negative-tests",
	displayName = "FAPI2-Baseline-ID2: DPoP negative tests",
	summary = "Obtain an access token as normal, check it works, then various negative tests to check the server is implementing the checks required by the DPoP specification.",
	profile = "FAPI2-Baseline-ID2",
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
public class FAPI2BaselineID2DpopNegativeTests extends AbstractFAPI2BaselineID2ServerTestModule {

	class CallResourceEndpointSteps extends AbstractConditionSequence {
		boolean expectSuccess;

		public CallResourceEndpointSteps(boolean expectSuccess) {
			this.expectSuccess = expectSuccess;
		}

		@Override
		public void evaluate() {
			call(makeUpdateResourceRequestSteps());
			callAndStopOnFailure(CallProtectedResource.class, "RFC7231-5.3.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			if (expectSuccess) {
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200or201.class, Condition.ConditionResult.FAILURE);
			} else {
				callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "DPOP-7.1");
			}
			call(exec().unmapKey("endpoint_response"));
		}
	}

	@Override
	protected void requestProtectedResource() {
		super.requestProtectedResource();

		// as per https://www.ietf.org/archive/id/draft-ietf-oauth-dpop-07.html#section-4.3:

		// 1. that there is not more than one DPoP header in the request,
		// FIXME

		// 2. the string value of the header field is a well-formed JWT,
		// FIXME (not entirely sure how to test this - maybe strip the part after the final dot (i.e. the signature)

		// 3. all required claims per Section 4.2 are contained in the JWT,
		// FIXME missing typ
		// FIXME missing alg
		// FIXME missing jwk
		// FIXME missing jti
		// FIXME missing htm
		eventLog.startBlock("Try DPoP proof where 'htu' is missing");
		call(new CallResourceEndpointSteps(false).insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(RemoveHtuFromDpopProof.class)));
		eventLog.startBlock("Try DPoP proof where 'iat' is missing");
		call(new CallResourceEndpointSteps(false).insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(RemoveIatFromDpopProof.class)));
		eventLog.startBlock("Try DPoP proof where 'ath' is missing");
		call(new CallResourceEndpointSteps(false).skip(SetDpopAccessTokenHash.class, "Skipping adding DPoP ATH"));

		// 4. the typ field in the header has the value dpop+jwt,
		eventLog.startBlock("Try DPoP proof with invalid 'typ' in header");
		call(new CallResourceEndpointSteps(false).insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(SetDpopHeaderTypToInvalidValue.class)));

		// 5. the algorithm in the header of the JWT indicates an asymmetric digital signature algorithm, is not none, is supported by the application, and is deemed secure,
		// I think mostly this is one that can only be tested at the token endpoint, but there are a few things we can try:
		// FIXME use RS256 instead of PS256 (should be rejected as FAPI doesn't allow RS256)
		// FIXME try with alg: none

		// 6. the JWT signature verifies with the public key contained in the jwk header of the JWT,
		eventLog.startBlock("Try DPoP proof with invalid signature");
		call(new CallResourceEndpointSteps(false).insertAfter(SignDpopProof.class, condition(InvalidateDpopProofSignature.class)));

		// 7. the jwk header of the JWT does not contain a private key,
		eventLog.startBlock("Try DPoP proof with jwk header incorrectly containing private key");
		call(new CallResourceEndpointSteps(false).insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(SetDpopHeaderJwkToPrivateKey.class)));

		// 8. the htm claim matches the HTTP method value of the HTTP request in which the JWT was received,
		eventLog.startBlock("Try DPoP proof with incorrect 'htm'");
		call(new CallResourceEndpointSteps(false).insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(SetDpopHtmToPut.class)));

		// 9. the htu claim matches the HTTPS URI value for the HTTP request in which the JWT was received, ignoring any query and fragment parts,
		eventLog.startBlock("Try DPoP proof where 'htu' has a query/fragment (which must be ignored in match as per 4.3-9 in DPoP spec)");
		call(new CallResourceEndpointSteps(true).insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(AddQueryAndFragmentToDpopHtu.class)));

		eventLog.startBlock("Try DPoP proof where 'htu' is a different url");
		call(new CallResourceEndpointSteps(false).insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(SetDpopHtuToDifferentUrl.class)));


		// 10. if the server provided a nonce value to the client, the nonce claim matches the server-provided nonce value,
		// We're not doing nonces yet

		// 11. the iat claim value is within an acceptable timeframe and, within a reasonable consideration of accuracy and resource utilization, a proof JWT with the same jti value has not previously been received at the same resource during that time period (see Section 11.1),
		// iat is expected to be within seconds/minutes of current time as per https://mailarchive.ietf.org/arch/msg/oauth/CC0ZlExBdZFOjO2ltgkJ3w6VioI/
		// This might need to be changed when we support nonces if the server is using nonce; there's been discussion on the IETF OAuth list about the nonce check replacing the iat check
		// https://mailarchive.ietf.org/arch/msg/oauth/T4stTh9mQRExvZTdEC30OF541p0/
		eventLog.startBlock("Try DPoP proof where 'iat' is one hour in the future");
		call(new CallResourceEndpointSteps(false).insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(SetDpopIatToOneHourInFuture.class)));

		eventLog.startBlock("Try DPoP proof where 'iat' is one hour in the past");
		call(new CallResourceEndpointSteps(false).insertAfter(SetDpopHtmHtuForResourceEndpoint.class, condition(SetDpopIatToOneHourInPast.class)));

		// FIXME try reusing a proof (i.e. same jti value) - if this fails it should only be a warning, due to the "within a reasonable consideration of accuracy and resource utilization" getout clause in the spec

		// 12 if presented to a protected resource in conjunction with an access token,
		// 12.1 ensure that the value of the ath claim equals the hash of that access token,

		eventLog.startBlock("Try DPoP proof where 'ath' is incorrect");
		call(new CallResourceEndpointSteps(false).replace(SetDpopAccessTokenHash.class, condition(SetDpopAccessTokenHashToIncorrectValue.class)));

		// 12.2 confirm that the public key to which the access token is bound matches the public key from the DPoP proof.
		// FIXME

		// try proof with unknown values in header/body
		// FIXME

		// Servers SHOULD employ Syntax-Based Normalization and Scheme-Based Normalization in accordance with Section 6.2.2. and Section 6.2.3. of [RFC3986] before comparing the htu claim.¶
		// FIXME (only a 'should' so try de-normalizing url, maybe add port number to the real url (https://foo:443/...) and only warn if it fails)

		// This is a final sanity check to make sure that all the above tests failed because of the invalid dpop proofs,
		// and not because the access token had stopped working for some reason etc.
		eventLog.startBlock("Check a correct DPoP proof still works");
		call(new CallResourceEndpointSteps(true));
		if (brazilPayments) {
			validateBrazilPaymentInitiationSignedResponse();
		}


	}
}
