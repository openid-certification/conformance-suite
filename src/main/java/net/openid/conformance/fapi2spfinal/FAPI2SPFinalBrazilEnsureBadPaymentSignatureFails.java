package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentConsentRequest;
import net.openid.conformance.condition.client.InvalidateConsentEndpointRequestSignature;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi2-security-profile-final-brazil-ensure-bad-payment-signature-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure bad payment signature fails",
	summary = "This test makes a request to the payment consent endpoint request with a bad signature, which must fail.",
	profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {
	"plain_fapi",
	"openbanking_uk",
	"consumerdataright_au",
	"connectid_au",
	"cbuae",
	"fapi_client_credentials_grant"
})
public class FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	@VariantSetup(parameter = FAPI2FinalOPProfile.class, value = "openbanking_brazil")
	public void setupOpenBankingBrazil() {
		initProfileBehavior(new BadPaymentSignatureBrazilProfileBehavior());
	}

	@Override
	protected void performAuthorizationFlow() {
		if (!scopeContains("payments")) {
			fireTestFinished();
			return;
		}
		performPreAuthorizationSteps();

		call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		fireTestFinished();
	}

	/**
	 * Custom profile behavior that creates pre-auth steps with an invalidated payment signature.
	 */
	static class BadPaymentSignatureBrazilProfileBehavior extends OpenBankingBrazilProfileBehavior {

		@Override
		protected OpenBankingBrazilPreAuthorizationSteps createOpenBankingBrazilPreAuthorizationSteps(boolean payments, boolean stopAfterConsentEndpointCall) {
			//  stopAfterConsentEndpointCall is true so that the consent endpoint is not called
			return super.createOpenBankingBrazilPreAuthorizationSteps(payments, true);
		}

		@Override
		protected ConditionSequence createOBBPreauthSteps() {
			ConditionSequence steps = super.createOBBPreauthSteps();

			steps.insertAfter(FAPIBrazilSignPaymentConsentRequest.class,
				new ConditionCallBuilder(InvalidateConsentEndpointRequestSignature.class));

			return steps;
		}
	}
}
