package net.openid.conformance.openinsurance.testmodule.deprecated.consent;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallConsentApiWithBearerToken;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToPostConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.RequestAccountBalancesReadOnly;
import net.openid.conformance.openbanking_brasil.testmodules.support.RequestAccountReadOnly;
import net.openid.conformance.openbanking_brasil.testmodules.support.RequestCreditCardsAccountsReadOnly;
import net.openid.conformance.openbanking_brasil.testmodules.support.RequestFinancingsReadAndPaymentsReadOnly;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateRequestedPermissionsAreNotWidened;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-test-no-widening-permissions-v1",
	displayName = "Validate that consent API v1 only returns the requested permissions",
	summary = "Validate that consent API v1 only returns the requested permissions",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class OpinConsentsApiWideningPermissionsTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests() {

		validatePermissions("ACCOUNTS_READ", RequestAccountReadOnly.class);
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
