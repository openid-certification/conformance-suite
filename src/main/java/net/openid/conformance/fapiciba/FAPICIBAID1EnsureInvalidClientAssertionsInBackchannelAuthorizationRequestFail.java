package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientAssertionToRequest;
import net.openid.conformance.condition.client.AddClientIdToRequest;
import net.openid.conformance.condition.client.AddExpIs5MinutesInPastToClientAssertionClaims;
import net.openid.conformance.condition.client.AddWrongAudToClientAssertionClaims;
import net.openid.conformance.condition.client.AddWrongIssToClientAssertionClaims;
import net.openid.conformance.condition.client.ChangeClientJwksAlgToRS256;
import net.openid.conformance.condition.client.CheckBackchannelAuthenticationEndpointErrorHttpStatus;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromBackchannelAuthenticationEndpointContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointError;
import net.openid.conformance.condition.client.CreateUnsecuredClientAuthenticationAssertion;
import net.openid.conformance.condition.client.InvalidateClientAssertionSignature;
import net.openid.conformance.condition.client.RemoveAudFromClientAssertionClaims;
import net.openid.conformance.condition.client.RemoveClientAssertionTypeFromRequest;
import net.openid.conformance.condition.client.RemoveIssFromClientAssertionClaims;
import net.openid.conformance.condition.client.RemoveSubFromClientAssertionClaims;
import net.openid.conformance.condition.client.SetClientAssertionTypeToWrongValue;
import net.openid.conformance.condition.client.SetClientAuthenticationAudToBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.SetSubToWrongValueInClientAssertionClaims;
import net.openid.conformance.condition.client.SignClientAuthenticationAssertion;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.ValidateErrorResponseFromBackchannelAuthenticationEndpoint;
import net.openid.conformance.condition.client.ValidateErrorUriFromBackchannelAuthenticationEndpoint;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddPrivateKeyJWTClientAuthenticationToBackchannelRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-invalid-client-assertions-in-backchannel-authorization-request-fail",
	displayName = "FAPI-CIBA-ID1: Ensure invalid private_key_jwt client assertions in backchannel authorization requests are rejected",
	summary = "This test sends a series of requests to the backchannel authentication endpoint, each with a client assertion that is invalid in one way." +
		" Each must be rejected with an access_denied, invalid_request or invalid_client error." +
		" The invalid cases are: no client assertion; client_assertion_type missing; client_assertion_type wrong; sub missing; sub wrong; iss missing; iss wrong; aud missing; aud an unrelated URL; exp in the past; signed with RS256 (only if the client key is an RSA key); alg 'none'; invalid signature." +
		" A normal flow with a valid client assertion is then completed, to show that the earlier requests were rejected because of the client assertion." +
		" In ping mode, if the server accepts an invalid client assertion the remaining cases are not sent, as the authentication request that was wrongly started has to be completed.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "mtls" })
public class FAPICIBAID1EnsureInvalidClientAssertionsInBackchannelAuthorizationRequestFail extends AbstractFAPICIBAID1 {

	/**
	 * @param mutation applied to the standard client authentication sequence; null means no client assertion is sent
	 */
	private record AssertionCase(String label, boolean needsRsaKey, UnaryOperator<ConditionSequence> mutation) {
	}

	// claims are altered after the sequence has set aud, so that the aud cases are not overwritten
	private static final Class<? extends Condition> CLAIMS = SetClientAuthenticationAudToBackchannelAuthenticationEndpoint.class;
	private static final Class<? extends Condition> SIGN = SignClientAuthenticationAssertion.class;
	private static final Class<? extends Condition> ADD = AddClientAssertionToRequest.class;

	private AssertionCase currentCase;

	private boolean preAuthorizationStepsDone;

	private AssertionCase claimsCase(String label, Class<? extends Condition> claimsMutator) {
		return new AssertionCase(label, false, s -> s.insertAfter(CLAIMS, condition(claimsMutator).requirements("RFC7523-3")));
	}

	private List<AssertionCase> cases() {
		List<AssertionCase> cases = new ArrayList<>();

		cases.add(new AssertionCase("No client assertion", false, null));
		cases.add(new AssertionCase("client_assertion_type missing", false,
			s -> s.insertAfter(ADD, condition(RemoveClientAssertionTypeFromRequest.class).requirements("RFC7521-4.2"))));
		cases.add(new AssertionCase("client_assertion_type wrong", false,
			s -> s.insertAfter(ADD, condition(SetClientAssertionTypeToWrongValue.class).requirements("RFC7521-4.2"))));

		cases.add(claimsCase("Client assertion without sub", RemoveSubFromClientAssertionClaims.class));
		cases.add(claimsCase("Client assertion with wrong sub", SetSubToWrongValueInClientAssertionClaims.class));
		cases.add(claimsCase("Client assertion without iss", RemoveIssFromClientAssertionClaims.class));
		cases.add(claimsCase("Client assertion with wrong iss", AddWrongIssToClientAssertionClaims.class));
		cases.add(claimsCase("Client assertion without aud", RemoveAudFromClientAssertionClaims.class));
		cases.add(claimsCase("Client assertion with unrelated aud", AddWrongAudToClientAssertionClaims.class));
		cases.add(claimsCase("Client assertion with exp in the past", AddExpIs5MinutesInPastToClientAssertionClaims.class));

		cases.add(new AssertionCase("Client assertion signed with RS256", true,
			s -> s.insertBefore(SIGN, condition(ChangeClientJwksAlgToRS256.class).requirements("FAPI-CIBA-7.10"))));
		cases.add(new AssertionCase("Client assertion with alg none", false,
			s -> s.replace(SIGN, condition(CreateUnsecuredClientAuthenticationAssertion.class).requirements("RFC7523-3", "FAPI-CIBA-7.10"))));
		cases.add(new AssertionCase("Client assertion with invalid signature", false,
			s -> s.insertAfter(SIGN, condition(InvalidateClientAssertionSignature.class).requirements("RFC7523-3"))));

		return cases;
	}

	@Override
	protected void performPreAuthorizationSteps() {
		if (!preAuthorizationStepsDone) {
			super.performPreAuthorizationSteps();
			preAuthorizationStepsDone = true;
		}
	}

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

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
			createAuthorizationRequest();
			performAuthorizationRequest();
			validateErrorFromBackchannelAuthorizationRequestResponse();

			env.putObject("client_jwks", clientJwks);
			eventLog.endBlock();

			Integer httpStatus = env.getInteger("backchannel_authentication_endpoint_response_http_status");
			if (testType == CIBAMode.PING && httpStatus != null && httpStatus == org.apache.hc.core5.http.HttpStatus.SC_OK) {
				// the wrongly started authentication must be completed, or its ping notification would
				// arrive during a later test
				cleanupAfterBackchannelRequestShouldHaveFailed();
				return;
			}
		}

		currentCase = null;
		super.performAuthorizationFlow();
	}

	@Override
	protected void addClientAuthenticationToBackchannelRequest() {
		if (currentCase == null) {
			super.addClientAuthenticationToBackchannelRequest();
			return;
		}

		mapClientAuthKeys("backchannel_authentication_endpoint_request_form_parameters", "backchannel_authentication_endpoint_request_headers");
		if (currentCase.mutation() == null) {
			callAndStopOnFailure(AddClientIdToRequest.class);
		} else {
			call(currentCase.mutation().apply(new AddPrivateKeyJWTClientAuthenticationToBackchannelRequest(isSecondClient(), true)));
		}
		unmapClientAuthKeys();
	}

	@Override
	protected void validateErrorFromBackchannelAuthorizationRequestResponse() {
		callAndContinueOnFailure(ValidateErrorResponseFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");
		callAndContinueOnFailure(ValidateErrorUriFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");
		callAndContinueOnFailure(CheckErrorDescriptionFromBackchannelAuthenticationEndpointContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointError.class, Condition.ConditionResult.FAILURE, "CIBA-13");
		callAndContinueOnFailure(CheckBackchannelAuthenticationEndpointErrorHttpStatus.class, Condition.ConditionResult.FAILURE, "CIBA-13");
	}
}
