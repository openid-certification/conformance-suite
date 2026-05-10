package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.SetDpopExpToFiveMinutesInFuture;
import net.openid.conformance.condition.client.SetDpopNbfToNow;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.sequence.client.CreateDpopProofSteps;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
		testName = "fapi2-security-profile-final-check-dpop-proof-nbf-exp",
		displayName = "FAPI2-Security-Profile-Final: checks DPoP Proof nbf and exp claim validation",
		summary = "Tests whether the AS can handle the nbf and exp claim in the DPoP Proof",
		profile = "FAPI2-Security-Profile-Final"
	)

@VariantNotApplicable(parameter = FAPI2SenderConstrainMethod.class, values={"mtls"})
public class FAPI2SPFinalCheckDpopProofNbfExp extends AbstractFAPI2SPFinalServerTestModule {


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
