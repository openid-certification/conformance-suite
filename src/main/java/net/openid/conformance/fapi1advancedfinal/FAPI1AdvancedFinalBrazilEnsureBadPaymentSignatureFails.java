package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentConsentRequest;
import net.openid.conformance.condition.client.InvalidateConsentEndpointRequestSignature;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-ensure-bad-payment-signature-fails",
	displayName = "FAPI1-Advanced-Final: ensure bad payment signature fails",
	summary = "This test makes a request to the payment consent endpoint request with a bad signature, which must fail.",
	profile = "FAPI1-Advanced-Final",
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
@VariantNotApplicable(parameter = FAPI1FinalOPProfile.class, values = {
		"plain_fapi",
		"openbanking_uk",
		"consumerdataright_au",
		"openinsurance_brazil",
		"openbanking_ksa"
})
public class FAPI1AdvancedFinalBrazilEnsureBadPaymentSignatureFails extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		if (brazilPayments.isTrue()) {
			eventLog.log(getName(), "Payments scope present - protected resource assumed to be a payments endpoint");
		}
		OpenBankingBrazilPreAuthorizationSteps steps = new OpenBankingBrazilPreAuthorizationSteps(isSecondClient(),
			false,
			addTokenEndpointClientAuthentication,
			brazilPayments.isTrue(),
			profile == FAPI1FinalOPProfile.OPENINSURANCE_BRAZIL,
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
