package net.openid.conformance.openbanking_brasil.paymentInitiation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesHeader;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilEnsureBadPaymentSignatureFails;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-bad-signature-test",
	displayName = "Payments API test using bad signature on request",
	summary = "This test makes a request to the payment consent endpoint request with a bad signature, which must fail.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca"
	}
)
public class PaymentsApiBadPaymentSignatureFails extends FAPI1AdvancedFinalBrazilEnsureBadPaymentSignatureFails {

	@Override
	protected void setupResourceEndpoint() {
		callAndStopOnFailure(AddResourceUrlToConfig.class);
		super.setupResourceEndpoint();
	}

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddPaymentScope.class);
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);

		super.validateClientConfiguration();
	}

	@Override
	protected void configureClient() {
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		// Test won't pass without MATLS, but we'll try anyway (for now)
		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);

		validateClientConfiguration();
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
		callAndStopOnFailure(ConsentErrorMetaValidator.class);
		fireTestFinished();
	}

}
