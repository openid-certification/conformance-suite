package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddScopeToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateRefreshTokenRequest;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.List;
import java.util.stream.Collectors;

public class RefreshTokenRequestExpectingErrorSteps extends AbstractConditionSequence {

	private boolean secondClient;
	private boolean isDpop;
	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public RefreshTokenRequestExpectingErrorSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this(secondClient, addClientAuthenticationToTokenEndpointRequest, false);
	}

	public RefreshTokenRequestExpectingErrorSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest, boolean isDpop) {
		this.secondClient = secondClient;
		this.isDpop = isDpop;
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateRefreshTokenRequest.class);
		if (!secondClient) {
			callAndStopOnFailure(AddScopeToTokenEndpointRequest.class, "RFC6749-6");
		}

		call(sequence(addClientAuthenticationToTokenEndpointRequest));

		if (isDpop) {
			call(CreateDpopProofSteps.createTokenEndpointDpopSteps());
			callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class);

			// retry request if token_endpoint_dpop_nonce_error is found
			call(exec().startBlock("Token endpoint DPoP nonce retry"));

			// repeat conditions in CreateDpopProofSteps.createTokenEndpointDpopSteps() only if token_endpoint_dpop_nonce_error is found
			ConditionSequence seq = CreateDpopProofSteps.createTokenEndpointDpopSteps();
			seq.evaluate();
			List<Class<?extends Condition>> condList = seq.getTestExecutionUnits().stream().map(actionToConditionClass).collect(Collectors.toList());
			condList.forEach((Class<?extends Condition> cond) -> {
				call(condition(cond)
					.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
					.onSkip(ConditionResult.INFO));
			});

			call(condition(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO));
			call(exec().endBlock());
		} else {
			callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		}

		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class);
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, ConditionResult.FAILURE, "RFC6749-5.2");
	}
}
