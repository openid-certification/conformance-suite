package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentConsentRequest;
import net.openid.conformance.condition.client.InvalidateConsentEndpointRequestSignature;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-brazil-ensure-bad-payment-signature-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure bad payment signature fails",
	summary = "This test makes a request to the payment consent endpoint request with a bad signature, which must fail.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {
	"plain_fapi",
	"openbanking_uk",
	"consumerdataright_au",
	"connectid_au",
		"cbuae"
})
public class FAPI2SPFinalBrazilEnsureBadPaymentSignatureFails extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		if (brazilPayments) {
			eventLog.log(getName(), "Payments scope present - protected resource assumed to be a payments endpoint");
		}
		OpenBankingBrazilPreAuthorizationSteps steps = new OpenBankingBrazilPreAuthorizationSteps(
			isSecondClient(),
			isDpop(),
			addTokenEndpointClientAuthentication,
			brazilPayments,
			false, // open insurance not yet supported in fapi2
			true,
			false);

		steps.insertAfter(FAPIBrazilSignPaymentConsentRequest.class,
			condition(InvalidateConsentEndpointRequestSignature.class));

		return steps;
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
}
