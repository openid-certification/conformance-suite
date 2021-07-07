package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-test-no-widening-permissions",
	displayName = "Validate that consent API only returns the requested permissions",
	summary = "Validate that consent API only returns the requested permissions",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class ConsentsApiWideningPermissionsTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void preConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		callAndStopOnFailure(RequestAccountReadOnly.class);
	}

	@Override
	protected void runTests() {
		// first one needs to be explicit as the resource is called before we get to here
		runInBlock("Validate ACCOUNTS_READ request only gives ACCOUNTS_READ back", () -> {

			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(ValidateRequestedPermissionsAreNotWidened.class, Condition.ConditionResult.FAILURE);

		});

		validatePermissions("ACCOUNTS_BALANCES_READ", RequestAccountBalancesReadOnly.class);
		validatePermissions("CREDIT_CARDS_ACCOUNTS_READ", RequestCreditCardsAccountsReadOnly.class);
		validatePermissions("FINANCINGS_READ FINANCINGS_PAYMENTS_READ", RequestFinancingsReadAndPaymentsReadOnly.class);

	}


	private void validatePermissions(String permissions, Class<? extends Condition> setupClass) {
		String logMessage = String.format("Validate %s request only gives %s back", permissions, permissions);
		runInBlock(logMessage, () -> {

			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(setupClass);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(ValidateRequestedPermissionsAreNotWidened.class, Condition.ConditionResult.FAILURE);

		});
	}

}
