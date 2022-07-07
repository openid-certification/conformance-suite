package net.openid.conformance.openbanking_brasil.testmodules.v2.accounts;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.v1.AccountBalancesResponseValidator;
import net.openid.conformance.openbanking_brasil.account.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "accounts-api-operational-limits",
	displayName = "Test will make sure that the server has not implemented any type of operational limits for the Accounts API.",
	summary = "",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.client_id_operational_limits",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.brazilCnpj",
		"resource.brazilCpfOperational",
		"resource.brazilCnpjOperational",
		"consent.productType"
	}
)
public class AccountsApiOperationalLimitsTestModule extends AbstractOBBrasilFunctionalTestModule {

	private int numberOfExecutions = 1;


	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl){
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(EnsureClientIdForOperationalLimitsIsPresent.class);
		callAndStopOnFailure(SwitchToOperationalLimitsClientId.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
		super.onConfigure(config, baseUrl);
	}


//	@Override
//	protected void performPreAuthorizationSteps(){
//		if (numberOfExecutions >= 2){
//			fireTestFinished();
//		}
//		super.performPreAuthorizationSteps();
//	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		expose("consent_id " + numberOfExecutions, env.getString("consent_id"));

		if(numberOfExecutions == 1){
			callAndStopOnFailure(SwitchToOriginalClientId.class);
			callAndStopOnFailure(RemoveOperationalLimitsFromConsentRequest.class);
			callAndContinueOnFailure(RemoveConsentIdFromClientScopes.class);
			callAndStopOnFailure(PrepareUrlForFetchingAccounts.class);
			validationStarted = false;
			numberOfExecutions++;
			performAuthorizationFlow();
		} else {
			fireTestFinished();
		}
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(AccountListValidatorV2.class);
		callAndStopOnFailure(AccountsSelectTwoResources.class);
		if (numberOfExecutions == 1){
			callAndStopOnFailure(AccountsOperationLimitsSelectResourceOne.class);
			accountsOperationalLimitCalls();
			callAndStopOnFailure(AccountOperationalLimitsSelectResourceTwo.class);
			accountsOperationalLimitCalls();
		} else if (numberOfExecutions == 2){
			callAndStopOnFailure(AccountsOperationLimitsSelectResourceOne.class);
			accountsOperationalLimitCalls();
		}


	}

	private ConditionSequence callAccountsBalancesSequence(){

		return new OperationalLimitsCallResource()
			.replace(PrepareUrlForFetchingAccountResource.class,condition(PrepareUrlForFetchingAccountBalances.class))
			.replace(AccountIdentificationResponseValidatorV2.class, condition(AccountBalancesResponseValidator.class));
	}

	private ConditionSequence callAccountsTransactionsCurrent(){
		return new OperationalLimitsCallResource()
			.replace(PrepareUrlForFetchingAccountResource.class,condition(PrepareUrlForFetchingAccountTransactionsCurrent.class))
			.replace(AccountIdentificationResponseValidatorV2.class, condition(AccountTransactionsCurrentValidatorV2.class));
	}

	private void accountsOperationalLimitCalls(){
		preCallProtectedResource("Fetching First Account");
		//Call Account resource
		ConditionSequence callAccountResource = new OperationalLimitsCallResource();
		call(callAccountResource);
		eventLog.startBlock("Fetching transactions with booking date parameters");
		//Account transactions using booking date parameter
		ConditionSequence callTransactionWithBookingParams = new OperationalLimitsCallResource()
			.replace(PrepareUrlForFetchingAccountResource.class,condition(PrepareUrlForFetchingAccountTransactions.class))
			.insertAfter(PrepareUrlForFetchingAccountResource.class, condition(AddBookingDateSixDaysBefore.class))
			.replace(AccountIdentificationResponseValidatorV2.class,condition(AccountTransactionsValidatorV2.class));
		call(callTransactionWithBookingParams);

		eventLog.startBlock("Fetching balances multiple times");
		//Account Balances x14
		repeatSequence(this::callAccountsBalancesSequence).times(14).trailingPause(1).run();

		eventLog.startBlock("Fetching transaction current multiple times");
		//Transaction Date x7
		repeatSequence(this::callAccountsTransactionsCurrent).times(7).trailingPause(1).run();

		eventLog.startBlock("Fetching transaction current with booking date parameters");
		//Transaction Current with booking date params
		ConditionSequence transactionsCurrentSingleSequence = callAccountsTransactionsCurrent();
		transactionsCurrentSingleSequence
			.insertBefore(CreateEmptyResourceEndpointRequestHeaders.class,condition(AddBookingDateSixDaysBeforeTransactionsCurrent.class));
		call(transactionsCurrentSingleSequence);

		//Set to resource response next url
		callAndStopOnFailure(SetProtectedResourceUrlToNextEndpoint.class);

		eventLog.startBlock("Fetching transaction current next link multiple times");
		//Call next url x9 and validate page size is 1
		ConditionSequence transactionsCurrentNextSequence = callAccountsTransactionsCurrent();
		transactionsCurrentNextSequence
			.skip(PrepareUrlForFetchingAccountResource.class,"Resource Url is set to Next")
			.replace(AccountIdentificationResponseValidatorV2.class, condition(AccountTransactionsCurrentValidatorV2.class))
			.insertAfter(AccountIdentificationResponseValidatorV2.class,condition(ValidatePageSizeIsOne.class))
			.insertAfter(AccountIdentificationResponseValidatorV2.class,condition(SetProtectedResourceUrlToNextEndpoint.class));
		call(transactionsCurrentNextSequence);

		repeatSequence(() -> transactionsCurrentNextSequence).times(9).trailingPause(1).run();
	}
}
