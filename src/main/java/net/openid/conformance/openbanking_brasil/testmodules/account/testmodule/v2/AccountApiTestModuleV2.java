package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-test-v2",
	displayName = "Validate structure of all accounts API resources V2",
	summary = "Validates the structure of all account API resources V2\n" +
		"\u2022 Cria Consentimento apenas com as Permissions necessárias para acessar os recursos da API de Accounts\n" +
		"\u2022 Valida todos os campos enviados na API de consentimento\n" +
		"\u2022 Chama todos os recursos da API de Accounts V2\n" +
		"\u2022 Valida todos os campos dos recursos da API de Accounts",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
	}
)

public class AccountApiTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {
	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(AccountListValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch Account V2");
		callAndContinueOnFailure(AccountIdentificationResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		preCallProtectedResource("Fetch Account balance V2");
		callAndContinueOnFailure(AccountBalancesResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
		callAndStopOnFailure(LogKnownIssue.class, "BCLOG-F02-172");
		callAndStopOnFailure(AddBookingDateOneYearBefore.class);
		preCallProtectedResource("Fetch Account transactions V2");
		callAndContinueOnFailure(AccountTransactionsValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateMetaOnlyRequestDateTime.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));
		callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
		preCallProtectedResource("Fetch Account limits V2");
		callAndContinueOnFailure(AccountLimitsValidatorV2.class, Condition.ConditionResult.FAILURE);

	}

}
