package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.client.CreatePaymentRequestEntityClaims;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.OpenBankingBrazilPreAuthorizationErrorAgnosticSteps;
import net.openid.conformance.openbanking_brasil.testmodules.support.OptionallyAllow201Or422;
import net.openid.conformance.openbanking_brasil.testmodules.support.SelectQRDNCodeLocalInstrument;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;



@PublishTestModule(
	testName = "payments-api-qrdn-good-email-proxy-test",
	displayName = "Payments Consents API test module for QRDN local instrument with email address",
	summary = "Payments Consents API test module using a qr code with an email address in it",
	profile = OBBProfile.OBB_PROFILE,
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
public class PaymentsConsentsApiQRDNHappyTestModule extends AbstractOBBrasilQrCodePaymentFunctionalTestModule {

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
		callAndStopOnFailure(SelectQRDNCodeLocalInstrument.class);
		callAndStopOnFailure(SelectQRDNCodePixLocalInstrument.class);
		callAndStopOnFailure(ValidateQrdnConfig.class);
	}
}
