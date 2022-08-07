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
	displayName = "Brazil DCM: payments: check that subjectdn can be updated using the dynamic client management endpoint\n",
	summary = "\u2022 Perform Dynamic Client Registration with first credential set (1) setting subject_dn (1)\n" +
		"\u2022 Use client_credentials grant to obtain Brazil consent\n" +
		"\u2022 Switch to certificate (2) with different subjectdn, verify that client credentials grant fails\n" +
		"\u2022 Make PUT request to client configuration endpoint with subjectdn for second certificate (2) -> This will update tls_subject_dn to (2)\n" +
		"\u2022 Use client_credentials grant to obtain Brazil consent\n" +
		"\u2022 Switch back to original certificate (1), verify that client credentials grant fails\n" +
		"\u2022 Use original certificate (1) to make a GET request to client configuration endpoint expecting a success\n" +
		"\u2022 Use original certificate (1) to make a PUT request on the client configuration endpoint expecting a success" +
		"\u2022 Unregister dynamically registered client with original certificate (1)\n\n" +
		"Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
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
