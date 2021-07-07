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
public class AccountsApiWrongPermissionsTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(RememberOriginalScopes.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
//		callAndStopOnFailure(ProvideIncorrectPermissionsForAccountsApi.class);
	}


	@Override
	protected void validateResponse() {

		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);

		preCallProtectedResource("Fetch Account");
		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		preCallProtectedResource("Fetch Account balance");
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
		preCallProtectedResource("Fetch Account transactions");
		callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
		preCallProtectedResource("Fetch Account limits");

		callAndStopOnFailure(ResetScopesToConfigured.class);
		callAndStopOnFailure(ProvideIncorrectPermissionsForAccountsApi.class);

		performAuthorizationFlow();

	}

	private void waitForBrowserToReturn() {

		// this happens in the background so that we can check the state of the browser controller
		getTestExecutionManager().runFinalisationTaskInBackground(() -> {

			// wait for web runners to wrap up first

			Instant timeout = Instant.now().plusSeconds(60); // wait at most 60 seconds
			while (browser.getWebRunners().size() > 0
				&& Instant.now().isBefore(timeout)) {
				Thread.sleep(100); // sleep before we check again
			}

			return "Done";
		});
	}

	boolean i = false;

	@Override // To allow multiple auth code flows
	protected void onPostAuthorizationFlowComplete() {

		if(!i) {
			i = true;
			return;
		}

		eventLog.log(getName(), "WAIT FOR BROWSWSERR - start");
		waitForBrowserToReturn();

		eventLog.log(getName(), "WAIT FOR BROWSWSERR - end");
		call(sequence(CallProtectedResourceExpectingFailureSequence.class));
		forceReleaseLock();
		setResult(Result.PASSED);
		setStatus(Status.FINISHED);

	}

}
