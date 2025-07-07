package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.SetDpopExpToFiveMinutesInFuture;
import net.openid.conformance.condition.client.SetDpopNbfToNow;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.sequence.client.CreateDpopProofSteps;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
		testName = "fapi2-security-profile-id2-check-dpop-proof-nbf-exp",
		displayName = "FAPI2-Security-Profile-ID2: checks DPoP Proof nbf and exp claim validation",
		summary = "Tests whether the AS can handle the nbf and exp claim in the DPoP Proof",
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
public class FAPI2SPID2CheckDpopProofNbfExp extends AbstractFAPI2SPID2ServerTestModule {


	@Override
	@VariantSetup(parameter = FAPI2SenderConstrainMethod.class, value = "dpop")
	public void setupCreateDpopForEndpointSteps() {
		super.setupCreateDpopForEndpointSteps();

		createDpopForTokenEndpointSteps = () -> CreateDpopProofSteps.createTokenEndpointDpopSteps()
			.insertBefore(SignDpopProof.class, sequenceOf(condition(SetDpopNbfToNow.class), condition(SetDpopExpToFiveMinutesInFuture.class)));

		createDpopForResourceEndpointSteps = ()-> CreateDpopProofSteps.createResourceEndpointDpopSteps()
			.insertBefore(SignDpopProof.class, sequenceOf(condition(SetDpopNbfToNow.class), condition(SetDpopExpToFiveMinutesInFuture.class)));
	}
}
