package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.client.CreatePaymentRequestEntityClaims;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.OpenBankingBrazilPreAuthorizationErrorAgnosticSteps;
import net.openid.conformance.openbanking_brasil.testmodules.support.OptionallyAllow201Or422;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;



@PublishTestModule(
	testName = "payments-api-qrdn-good-proxy-test-deprecated",
	displayName = "Payments Consents API test module for QRDN local instrument which expects an ACCC status",
	summary = "[THIS TEST HAS BEEN REPLACED BY payments-api-qrdn-good-proxy-test AND SHALL BE REMOVED. RUNNING THIS TEST WILL CONSUME THE QRDN BRINGING A FAILURE TO THE OTHER MODULE].  ",
	profile = "hide",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.resourceUrl",
		"resource.brazilCpf",
		"resource.brazilPaymentConsent",
		"resource.brazilPixPayment",
		"resource.brazilQrdnPaymentConsent",
		"resource.brazilQrdnCnpj",
		"resource.brazilQrdnRemittance"
	}
)
public class PaymentsConsentsApiQRDNHappyLegacyTestModule extends AbstractOBBrasilQrCodePaymentFunctionalTestModule {

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		eventLog.log(getName(), "Payments scope present - protected resource assumed to be a payments endpoint");
		ConditionSequence steps = new OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(addTokenEndpointClientAuthentication)
			.replace(FAPIBrazilCreatePaymentConsentRequest.class, condition(SelectPaymentConsentWithQrdnCode.class))
			.replace(OptionallyAllow201Or422.class, condition(EnsureConsentResponseCodeWas201.class));
		return steps;
	}

	@Override
	protected void postProcessResourceSequence(ConditionSequence pixSequence) {
		pixSequence.replace(CreatePaymentRequestEntityClaims.class, condition(CreatePaymentRequestEntityClaimsFromQrdnConfig.class));
	}

	@Override
	protected void configureDictInfo() {
		callAndStopOnFailure(SelectQRDNCodeLocalInstrumentWithQrdnConfig.class);
		callAndStopOnFailure(SelectQRDNCodePixLocalInstrument.class);
		callAndStopOnFailure(ValidateQrdnConfig.class);
	}
}
