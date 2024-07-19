package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddDpopHeaderForParEndpointRequest;
import net.openid.conformance.condition.client.AddDpopHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.AddDpopHeaderForTokenEndpointRequest;
import net.openid.conformance.condition.client.CreateDpopClaims;
import net.openid.conformance.condition.client.CreateDpopHeader;
import net.openid.conformance.condition.client.EnsureDpopNonceContainsAllowedCharactersOnly;
import net.openid.conformance.condition.client.SetDpopAccessTokenHash;
import net.openid.conformance.condition.client.SetDpopHtmHtuForParEndpoint;
import net.openid.conformance.condition.client.SetDpopHtmHtuForResourceEndpoint;
import net.openid.conformance.condition.client.SetDpopHtmHtuForTokenEndpoint;
import net.openid.conformance.condition.client.SetDpopProofNonceForResourceEndpoint;
import net.openid.conformance.condition.client.SetDpopProofNonceForAuthorizationServer;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;


public class CreateDpopProofSteps extends AbstractConditionSequence {
	public enum DPOP_PROOF_TYPE { TOKEN, PAR, RESOURCE }
	private DPOP_PROOF_TYPE proofType  = DPOP_PROOF_TYPE.TOKEN;


	public CreateDpopProofSteps(DPOP_PROOF_TYPE proofType) {
		this.proofType = proofType;
	}

	public static ConditionSequence createParEndpointDpopSteps() {
		return new CreateDpopProofSteps(DPOP_PROOF_TYPE.PAR);
	}

	public static ConditionSequence createTokenEndpointDpopSteps() {
		return new CreateDpopProofSteps(DPOP_PROOF_TYPE.TOKEN);
	}

	public static ConditionSequence createResourceEndpointDpopSteps() {
		return new CreateDpopProofSteps(DPOP_PROOF_TYPE.RESOURCE);
	}

	@Override
	public void evaluate() {

		callAndStopOnFailure(CreateDpopHeader.class);
		callAndStopOnFailure(CreateDpopClaims.class);

		// Add endpoint specific DPOP params
		switch (proofType) {
			case TOKEN:
				callAndStopOnFailure(SetDpopHtmHtuForTokenEndpoint.class);
				callAndContinueOnFailure(SetDpopProofNonceForAuthorizationServer.class, ConditionResult.INFO);
				break;

			case PAR:
				callAndStopOnFailure(SetDpopHtmHtuForParEndpoint.class);
				callAndContinueOnFailure(SetDpopProofNonceForAuthorizationServer.class, ConditionResult.INFO);
				break;

			case RESOURCE:
				callAndStopOnFailure(SetDpopHtmHtuForResourceEndpoint.class);
				callAndStopOnFailure(SetDpopAccessTokenHash.class);
				callAndContinueOnFailure(SetDpopProofNonceForResourceEndpoint.class, ConditionResult.INFO);
				break;
		}

		callAndContinueOnFailure(EnsureDpopNonceContainsAllowedCharactersOnly.class, ConditionResult.FAILURE, "DPOP-8.1");
		callAndStopOnFailure(SignDpopProof.class);

		// Add DPOP header to request
		switch (proofType) {
			case TOKEN:
				callAndStopOnFailure(AddDpopHeaderForTokenEndpointRequest.class);
				break;

			case PAR:
				callAndStopOnFailure(AddDpopHeaderForParEndpointRequest.class);
				break;

			case RESOURCE:
				callAndStopOnFailure(AddDpopHeaderForResourceEndpointRequest.class);
				break;
		}
	}
}
