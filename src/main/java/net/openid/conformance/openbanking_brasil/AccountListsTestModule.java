package net.openid.conformance.openbanking_brasil;

import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.account.AccountListValidator;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.*;

@PublishTestModule(
	testName = "account-list-test",
	displayName = "Validate structure of accounts list resource",
	summary = "Validates the structure of the accounts list API",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.resourceUrl"
	}
)
@VariantParameters({
	ClientAuthType.class,
	FAPIRWOPProfile.class,
	FAPIResponseMode.class,
	FAPIAuthRequestMethod.class
})
public class AccountListsTestModule extends AbstractFunctionalTestModule {


	@Override
	protected void validateResponse() {
		callAndStopOnFailure(AccountListValidator.class);
	}
}
