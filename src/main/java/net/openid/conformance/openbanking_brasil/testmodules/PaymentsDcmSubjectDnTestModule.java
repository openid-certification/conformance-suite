package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsurePaymentDateIsToday;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideCNPJ;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideClientWithDCMSubjectDnTestClient1;
import net.openid.conformance.openbanking_brasil.testmodules.support.OverrideScopeWithOpenIdPayments;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetDirectoryInfo;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetProtectedResourceUrlToPaymentsEndpoint;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = PaymentsDcmSubjectDnTestModule.testName,
	displayName = "Brazil DCM: payments: check that subjectdn can be updated using the dynamic client management endpoint",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client), register a new client on the target authorization server, perform a successful client credentials grant. Verify that the client credentials grant fails when using a certificate with a different subjectdn. Then use the DCM endpoint to change subjectdn for the client to the subjectdn for the other certificate, and verify the client credentials grant succeeds with that certificate but fails with the original certificate.\n\nNote that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.resourceUrl",
		"resource.brazilOrganizationId",
		"resource.brazilPaymentConsent",
		"resource.brazilPixPayment"
	}
)

public class PaymentsDcmSubjectDnTestModule extends AbstractDcmSubjectDnTestModule {
	public static final String testName = "payments-dcm-subject-dn-test";

	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideClientWithDCMSubjectDnTestClient1.class);
		callAndStopOnFailure(OverrideScopeWithOpenIdPayments.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		callAndStopOnFailure(OverrideCNPJ.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(SanitiseQrCodeConfig.class);

		super.onConfigure(config, baseUrl);
	}

}
