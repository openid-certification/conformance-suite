package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddDpopHeaderForTokenEndpointRequest;
import net.openid.conformance.condition.client.AddScopeToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateDpopClaims;
import net.openid.conformance.condition.client.CreateDpopHeader;
import net.openid.conformance.condition.client.CreateRefreshTokenRequest;
import net.openid.conformance.condition.client.EnsureDpopNonceContainsAllowedCharactersOnly;
import net.openid.conformance.condition.client.SetDpopHtmHtuForTokenEndpoint;
import net.openid.conformance.condition.client.SetDpopProofNonceForTokenEndpoint;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

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
			callAndStopOnFailure(CreateDpopHeader.class);
			callAndStopOnFailure(CreateDpopClaims.class);
			callAndStopOnFailure(SetDpopHtmHtuForTokenEndpoint.class);
			callAndContinueOnFailure(SetDpopProofNonceForTokenEndpoint.class, ConditionResult.INFO);
			callAndContinueOnFailure(EnsureDpopNonceContainsAllowedCharactersOnly.class, ConditionResult.FAILURE, "DPOP-8.1");
			callAndStopOnFailure(SignDpopProof.class);
			callAndStopOnFailure(AddDpopHeaderForTokenEndpointRequest.class);
			callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class);

			// retry request if token_endpoint_dpop_nonce_error is found
			exec().startBlock("Token endpoint DPoP nonce retry");
			call(condition(CreateDpopHeader.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO));
			call(condition(CreateDpopClaims.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO));
			call(condition(SetDpopHtmHtuForTokenEndpoint.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO));
			call(condition(SetDpopProofNonceForTokenEndpoint.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO)
				.dontStopOnFailure());
			call(condition(EnsureDpopNonceContainsAllowedCharactersOnly.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO)
				.requirement("DPOP-8.1")
				.onFail(ConditionResult.WARNING)
				.dontStopOnFailure());
			call(condition(SignDpopProof.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO));
			call(condition(AddDpopHeaderForTokenEndpointRequest.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO));
			call(condition(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO));
			exec().endBlock();
		} else {
			callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		}

		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class);
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, ConditionResult.FAILURE, "RFC6749-5.2");
	}
}
