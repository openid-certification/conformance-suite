package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddDpopHeaderForTokenEndpointRequest;
import net.openid.conformance.condition.client.AddScopeToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateDpopClaims;
import net.openid.conformance.condition.client.CreateDpopHeader;
import net.openid.conformance.condition.client.CreateRefreshTokenRequest;
import net.openid.conformance.condition.client.SetDpopHtmHtuForTokenEndpoint;
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
			callAndStopOnFailure(SignDpopProof.class);
			callAndStopOnFailure(AddDpopHeaderForTokenEndpointRequest.class);
		}

		callAndContinueOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class);
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, ConditionResult.FAILURE, "RFC6749-5.2");
	}
}
