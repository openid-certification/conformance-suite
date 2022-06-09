package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.http.AllowHttpStatusToBe201;
import net.openid.conformance.openbanking_brasil.testmodules.support.http.AllowHttpStatusToBe422;
import net.openid.conformance.openbanking_brasil.testmodules.support.http.EnsureHttpStatusCodeIsAllowed;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Optional;

@PublishTestModule(
	testName = "payments-api-dcr-happyflow",
	displayName = "Payments API Use payments after registering client via DCR",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client that has the PAGTO role), register a new client on the target authorization server and perform an authorization flow. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl"
	}
)
public class PaymentsApiDcrHappyFlowTestModule extends AbstractApiDcrTestModule {

	protected void setupResourceEndpoint() {
		callAndStopOnFailure(AddResourceUrlToConfig.class);
		super.setupResourceEndpoint();
	}

	@Override
	protected void configureClient() {
		callAndStopOnFailure(AddBrazilPixPaymentToTheResource.class);
		callAndStopOnFailure(OverrideClientWithPagtoClient.class);
		callAndStopOnFailure(OverrideScopeWithOpenIdPayments.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		callAndStopOnFailure(OverrideCNPJ.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddBrazilPixPaymentToTheResource.class);
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(SanitiseQrCodeConfig.class);

		super.onConfigure(config, baseUrl);
	}

	@Override
	protected Optional<ConditionSequence> getBrazilPaymentsStatusCodeCheck() {
		return Optional.of(sequenceOf(
			condition(AllowHttpStatusToBe201.class).onFail(Condition.ConditionResult.FAILURE),
			condition(AllowHttpStatusToBe422.class).onFail(Condition.ConditionResult.FAILURE),
			condition(EnsureHttpStatusCodeIsAllowed.class).onFail(Condition.ConditionResult.FAILURE)
		));
	}

}
