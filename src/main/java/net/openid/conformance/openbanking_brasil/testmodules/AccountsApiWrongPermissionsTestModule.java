package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildRequestObjectPostToPAREndpoint;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@PublishTestModule(
	testName = "account-api-wrong-permissions-test",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions",
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
		"resource.resourceUrl"
	}
)
public class AccountsApiWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void preFetchResources() {

		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);

		preCallProtectedResource("Fetch Account");
		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		preCallProtectedResource("Fetch Account balance");
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
		preCallProtectedResource("Fetch Account transactions");
		callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
		preCallProtectedResource("Fetch Account limits");

	}

	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForAccountsApi.class);
	}

	protected void requestResourcesWithIncorrectPermissions() {

		runInBlock("Ensure we cannot call the accounts root API", () -> {
			callAndStopOnFailure(PrepareUrlForAccountsRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account resource API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account balance API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account transactions API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account limits API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

	}

}
