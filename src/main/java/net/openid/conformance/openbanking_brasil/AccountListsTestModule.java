package net.openid.conformance.openbanking_brasil;

import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.account.*;
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
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl"
	}
)
public class AccountListsTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(AccountListValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareAccountFetch.class);
		preCallProtectedResource("Fetch Account");
		callAndContinueOnFailure(AccountIdentificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareBalanceFetch.class);
		preCallProtectedResource("Fetch Account balance");
		callAndContinueOnFailure(AccountBalancesResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareTransactionsFetch.class);
		preCallProtectedResource("Fetch Account transactions");
		callAndContinueOnFailure(AccountTransactionsValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareLimitsFetch.class);
		preCallProtectedResource("Fetch Account limits");
		callAndContinueOnFailure(AccountLimitsValidator.class, Condition.ConditionResult.FAILURE);

	}

}
