package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-dcr-happyflow",
	displayName = "Payments API Use payments after registering client via DCR",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client that has the PAGTO role), register a new client on the target authorization server and perform an authorization flow. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.resourceUrl"
	}
)
public class PaymentsApiDcrHappyFlowTestModule extends AbstractApiDcrTestModule {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideClientWithPagtoClient.class);
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
