package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddArrayContainingIssuerAndAnotherValueAsAudToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.AddClientAssertionToRequest;
import net.openid.conformance.condition.client.AddClientIdToRequest;
import net.openid.conformance.condition.client.AddExpIs5MinutesInPastToClientAssertionClaims;
import net.openid.conformance.condition.client.AddIatNbf8SecondsInTheFutureToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.AddIatNbfExpOver60SecondsInTheFutureToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.AddPAREndpointAsAudToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.AddTokenEndpointAsAudToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.AddWrongAudToClientAssertionClaims;
import net.openid.conformance.condition.client.AddWrongIssToClientAssertionClaims;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.ChangeClientJwksAlgToRS256;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CheckPAREndpointResponse201WithNoError;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaimsWithIssAudience;
import net.openid.conformance.condition.client.CreateUnsecuredClientAuthenticationAssertion;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.condition.client.InvalidateClientAssertionSignature;
import net.openid.conformance.condition.client.RemoveAudFromClientAssertionClaims;
import net.openid.conformance.condition.client.RemoveClientAssertionFromRequest;
import net.openid.conformance.condition.client.RemoveClientAssertionTypeFromRequest;
import net.openid.conformance.condition.client.RemoveIssFromClientAssertionClaims;
import net.openid.conformance.condition.client.RemoveSubFromClientAssertionClaims;
import net.openid.conformance.condition.client.SetClientAssertionTypeToWrongValue;
import net.openid.conformance.condition.client.SetSubToWrongValueInClientAssertionClaims;
import net.openid.conformance.condition.client.SignClientAuthenticationAssertion;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-invalid-client-assertions-fail",
	displayName = "FAPI2-Security-Profile-Final: ensure invalid private_key_jwt client assertions are rejected",
	summary = "This test sends a series of requests to the first endpoint that requires client authentication - the PAR endpoint, or the token endpoint when the client credentials grant is being tested - each with a client assertion that is invalid in one way." +
		" Each must be rejected with an HTTP status of 400 or 401 and an error of invalid_client or invalid_request." +
		" The invalid cases are: no client assertion; client_assertion_type missing; client_assertion_type wrong; sub missing; sub wrong; iss missing; iss wrong; aud missing; aud an unrelated URL; aud the PAR endpoint URL (PAR endpoint only); aud the token endpoint URL; aud an array containing the issuer; exp in the past; iat/nbf more than 60 seconds in the future; signed with RS256 (only if the client key is an RSA key); alg 'none'; invalid signature." +
		" An assertion with iat/nbf 8 seconds in the future is then sent, which should be accepted (a warning is raised if it is not), and finally a valid assertion is sent, which must be accepted, to show that the earlier requests were rejected because of the client assertion.",
	profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "mtls", "client_attestation" })
public class FAPI2SPFinalEnsureInvalidClientAssertionsFail extends AbstractFAPI2SPFinalServerTestModule {

	private enum Expect { REJECTED, SHOULD_BE_ACCEPTED, MUST_BE_ACCEPTED }

	/**
	 * @param mutation applied to the standard client authentication sequence; null means no client assertion is sent
	 */
	private record AssertionCase(String label, Expect expect, boolean needsRsaKey, UnaryOperator<ConditionSequence> mutation) {
	}

	private static final Class<? extends Condition> CLAIMS = CreateClientAuthenticationAssertionClaimsWithIssAudience.class;
	private static final Class<? extends Condition> SIGN = SignClientAuthenticationAssertion.class;
	private static final Class<? extends Condition> ADD = AddClientAssertionToRequest.class;

	private AssertionCase currentCase;

	private AssertionCase rejected(String label, UnaryOperator<ConditionSequence> mutation) {
		return new AssertionCase(label, Expect.REJECTED, false, mutation);
	}

	private AssertionCase rejectedClaims(String label, Class<? extends Condition> claimsMutator, String... requirements) {
		return rejected(label, s -> s.insertAfter(CLAIMS, condition(claimsMutator).requirements(requirements)));
	}

	private List<AssertionCase> cases() {
		List<AssertionCase> cases = new ArrayList<>();

		cases.add(rejected("No client assertion", null));
		cases.add(rejected("client_assertion_type missing",
			s -> s.insertAfter(ADD, condition(RemoveClientAssertionTypeFromRequest.class).requirements("RFC7521-4.2"))));
		cases.add(rejected("client_assertion_type wrong",
			s -> s.insertAfter(ADD, condition(SetClientAssertionTypeToWrongValue.class).requirements("RFC7521-4.2"))));

		cases.add(rejectedClaims("Client assertion without sub", RemoveSubFromClientAssertionClaims.class, "RFC7523-3"));
		cases.add(rejectedClaims("Client assertion with wrong sub", SetSubToWrongValueInClientAssertionClaims.class, "RFC7523-3"));
		cases.add(rejectedClaims("Client assertion without iss", RemoveIssFromClientAssertionClaims.class, "RFC7523-3"));
		cases.add(rejectedClaims("Client assertion with wrong iss", AddWrongIssToClientAssertionClaims.class, "RFC7523-3"));

		cases.add(rejectedClaims("Client assertion without aud", RemoveAudFromClientAssertionClaims.class, "RFC7523-3"));
		cases.add(rejectedClaims("Client assertion with unrelated aud", AddWrongAudToClientAssertionClaims.class, "RFC7523-3", "FAPI2-SP-FINAL-5.3.2.1-8"));
		if (!clientCredentialsGrant) {
			cases.add(rejectedClaims("Client assertion with PAR endpoint as aud", AddPAREndpointAsAudToClientAuthenticationAssertionClaims.class, "FAPI2-SP-FINAL-5.3.2.1-8"));
		}
		cases.add(rejectedClaims("Client assertion with token endpoint as aud", AddTokenEndpointAsAudToClientAuthenticationAssertionClaims.class, "FAPI2-SP-FINAL-5.3.2.1-8"));
		cases.add(rejectedClaims("Client assertion with array as aud", AddArrayContainingIssuerAndAnotherValueAsAudToClientAuthenticationAssertionClaims.class, "FAPI2-SP-FINAL-5.3.2.1-8"));

		cases.add(rejectedClaims("Client assertion with exp in the past", AddExpIs5MinutesInPastToClientAssertionClaims.class, "RFC7523-3"));
		cases.add(rejectedClaims("Client assertion with iat and nbf over 60 seconds in the future",
			AddIatNbfExpOver60SecondsInTheFutureToClientAuthenticationAssertionClaims.class, "RFC7519-4.1.5", "RFC7519-4.1.6", "FAPI2-SP-FINAL-5.3.2.1"));

		cases.add(new AssertionCase("Client assertion signed with RS256", Expect.REJECTED, true,
			s -> s.insertBefore(SIGN, condition(ChangeClientJwksAlgToRS256.class).requirements("FAPI2-SP-FINAL-5.4"))));
		cases.add(rejected("Client assertion with alg none",
			s -> s.replace(SIGN, condition(CreateUnsecuredClientAuthenticationAssertion.class).requirements("RFC7523-3", "FAPI2-SP-FINAL-5.4"))));
		cases.add(rejected("Client assertion with invalid signature",
			s -> s.insertAfter(SIGN, condition(InvalidateClientAssertionSignature.class).requirements("RFC7523-3"))));

		cases.add(new AssertionCase("Client assertion with iat and nbf 8 seconds in the future", Expect.SHOULD_BE_ACCEPTED, false,
			s -> s.insertAfter(CLAIMS, condition(AddIatNbf8SecondsInTheFutureToClientAuthenticationAssertionClaims.class)
				.requirements("RFC7519-4.1.5", "RFC7519-4.1.6", "FAPI2-SP-FINAL-5.3.2.1"))));
		cases.add(new AssertionCase("Valid client assertion", Expect.MUST_BE_ACCEPTED, false, UnaryOperator.identity()));

		return cases;
	}

	private void runCases(Runnable makeRequest) {
		for (AssertionCase c : cases()) {
			eventLog.startBlock(c.label());

			if (c.needsRsaKey() && !"PS256".equals(JWKUtil.getAlgFromClientJwks(env))) {
				eventLog.log(getName(), "The client key in the test configuration is not an RSA key, so an RS256 signed assertion cannot be created; not sending this request.");
				eventLog.endBlock();
				continue;
			}

			// ChangeClientJwksAlgToRS256 alters the client keys in place
			JsonObject clientJwks = env.getObject("client_jwks").deepCopy();

			currentCase = c;
			makeRequest.run();

			env.putObject("client_jwks", clientJwks);
			eventLog.endBlock();
		}

		fireTestFinished();
	}

	@Override
	protected void performParAuthorizationRequestFlow() {
		runCases(super::performParAuthorizationRequestFlow);
	}

	@Override
	protected void performPostAuthorizationFlow() {
		// only reached for the client credentials grant; otherwise the test finishes at the PAR endpoint
		runCases(() -> {
			createClientCredentialsGrantRequest();
			callSenderConstrainedTokenEndpoint();
			processTokenEndpointResponse();
		});
	}

	// The call*Endpoint methods add client authentication inside their DPoP nonce retry loops, so the
	// assertion for the current case is rebuilt, with a fresh jti, on every attempt.
	private void addClientAuthenticationForCurrentCase() {
		callAndStopOnFailure(RemoveClientAssertionFromRequest.class);

		if (currentCase.mutation() == null) {
			callAndStopOnFailure(AddClientIdToRequest.class);
		} else {
			call(currentCase.mutation().apply(new CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest()));
		}
	}

	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
		mapClientAuthKeys("pushed_authorization_request_form_parameters", "pushed_authorization_request_endpoint_request_headers");
		addClientAuthenticationForCurrentCase();
		unmapClientAuthKeys();
	}

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		mapClientAuthKeys("token_endpoint_request_form_parameters", "token_endpoint_request_headers");
		addClientAuthenticationForCurrentCase();
		unmapClientAuthKeys();
	}

	@Override
	protected void processParResponse() {
		switch (currentCase.expect()) {
			case REJECTED -> {
				env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
				callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, ConditionResult.FAILURE, "PAR-2.3", "RFC6749-5.2");
				callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest.class, ConditionResult.FAILURE, "PAR-2.3", "RFC6749-5.2");
				env.unmapKey("endpoint_response");
			}
			case SHOULD_BE_ACCEPTED ->
				callAndContinueOnFailure(CheckPAREndpointResponse201WithNoError.class, ConditionResult.WARNING, "PAR-2.2", "PAR-2.3");
			case MUST_BE_ACCEPTED ->
				callAndStopOnFailure(CheckPAREndpointResponse201WithNoError.class, "PAR-2.2", "PAR-2.3");
		}
	}

	@Override
	protected void processTokenEndpointResponse() {
		switch (currentCase.expect()) {
			case REJECTED -> {
				callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
				callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, ConditionResult.FAILURE, "RFC6749-5.2");
				callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, ConditionResult.WARNING, "RFC6749-5.2");
				callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, ConditionResult.FAILURE, "RFC6749-5.2");
				callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, ConditionResult.FAILURE, "RFC6749-5.2");
				callAndContinueOnFailure(CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError.class, ConditionResult.FAILURE, "RFC6749-5.2");
				callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest.class, ConditionResult.FAILURE, "RFC6749-5.2");
			}
			case SHOULD_BE_ACCEPTED ->
				callAndContinueOnFailure(CheckTokenEndpointHttpStatus200.class, ConditionResult.WARNING, "RFC6749-5.1");
			case MUST_BE_ACCEPTED ->
				callAndStopOnFailure(CheckTokenEndpointHttpStatus200.class, "RFC6749-5.1");
		}
	}
}
