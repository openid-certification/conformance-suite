package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToAccountsEndpoint;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "account-api-test",
	displayName = "Validate structure of all accounts API resources",
	summary = "Validates the structure of all account API resources\n" +
		"\u2022 Cria Consentimento apenas com as Permissions necessárias para acessar os recursos da API de Accounts\n" +
		"\u2022 Valida todos os campos enviados na API de consentimento\n" +
		"\u2022 Chama todos os recursos da API de Accounts\n" +
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

public class AccountApiTestModule extends AbstractOBBrasilFunctionalTestModule {
	@Override
	protected void configureClient(){
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
		callAndContinueOnFailure(AccountListValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch Account");
		callAndContinueOnFailure(AccountIdentificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		preCallProtectedResource("Fetch Account balance");
		callAndContinueOnFailure(AccountBalancesResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
		callAndStopOnFailure(LogKnownIssue.class,"BCLOG-F02-172");
		preCallProtectedResource("Fetch Account transactions");
		callAndContinueOnFailure(AccountTransactionsValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));
		callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
		preCallProtectedResource("Fetch Account limits");
		callAndContinueOnFailure(AccountLimitsValidator.class, Condition.ConditionResult.FAILURE);

	}

}
