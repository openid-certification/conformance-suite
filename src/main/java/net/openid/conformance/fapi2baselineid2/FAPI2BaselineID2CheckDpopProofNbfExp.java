package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.client.AddDpopHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.AddDpopHeaderForTokenEndpointRequest;
import net.openid.conformance.condition.client.CreateDpopClaims;
import net.openid.conformance.condition.client.CreateDpopHeader;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.condition.client.SetDpopAccessTokenHash;
import net.openid.conformance.condition.client.SetDpopExpToFiveMinutesInFuture;
import net.openid.conformance.condition.client.SetDpopHtmHtuForResourceEndpoint;
import net.openid.conformance.condition.client.SetDpopHtmHtuForTokenEndpoint;
import net.openid.conformance.condition.client.SetDpopNbfToNow;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
		testName = "fapi2-baseline-id2-Check-DPoP-Proof-Nbf-Exp",
		displayName = "FAPI2-Baseline-ID2: checks DPoP Proof nbf and exp claim validation",
		summary = "Tests whether the AS can handle the nbf and exp claim in the DPoP Proof",
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
public class FAPI2BaselineID2CheckDpopProofNbfExp extends AbstractFAPI2BaselineID2ServerTestModule {


	@Override
	@VariantSetup(parameter = FAPI2SenderConstrainMethod.class, value = "dpop")
	public void setupCreateDpopForResourceEndpointSteps() {
		createDpopForResourceEndpointSteps = CreateDpopWithNbfExpForResourceEndpointSteps.class;
	}

	public static class CreateDpopWithNbfExpForResourceEndpointSteps extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateDpopHeader.class);
			callAndStopOnFailure(CreateDpopClaims.class);
			callAndStopOnFailure(SetDpopHtmHtuForResourceEndpoint.class);
			callAndStopOnFailure(SetDpopAccessTokenHash.class);
			callAndStopOnFailure(SetDpopNbfToNow.class);
			callAndStopOnFailure(SetDpopExpToFiveMinutesInFuture.class);
			callAndStopOnFailure(SignDpopProof.class);
			callAndStopOnFailure(AddDpopHeaderForResourceEndpointRequest.class);
		}
	}


	@Override
	protected void createDpopForTokenEndpoint(boolean createKey) {
		if (createKey) {
			callAndStopOnFailure(GenerateDpopKey.class);
		}
		callAndStopOnFailure(CreateDpopHeader.class);
		callAndStopOnFailure(CreateDpopClaims.class);
		callAndStopOnFailure(SetDpopHtmHtuForTokenEndpoint.class);
		callAndStopOnFailure(SetDpopNbfToNow.class);
		callAndStopOnFailure(SetDpopExpToFiveMinutesInFuture.class);
		callAndStopOnFailure(SignDpopProof.class);
		callAndStopOnFailure(AddDpopHeaderForTokenEndpointRequest.class);
	}
}
