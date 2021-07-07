package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalServerTestModule;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-wrong-permissions-test2",
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
public class AccountApiWrongPermissionsTestModule2 extends AbstractFAPI1AdvancedFinalServerTestModule {

	private boolean testStarted = false;

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(RememberOriginalScopes.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
	}

	public void runTest() {

		preCallProtectedResource("Accunts root");
		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);

		preCallProtectedResource("Accunt resouyrce");
		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		preCallProtectedResource("Accunts balance");
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
		preCallProtectedResource("Accunts transactions");
		callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
		preCallProtectedResource("Accunts limits");

		callAndStopOnFailure(ResetScopesToConfigured.class);
		callAndStopOnFailure(ProvideIncorrectPermissionsForAccountsApi.class);

		eventLog.log("Ask for duff consents", args("status", getStatus()));
		performAuthorizationFlow();

		eventLog.log("Now call broken endpoint", args("status", getStatus()));
		call(sequence(CallProtectedResourceExpectingFailureSequence.class));
	}

	private void doRunTest() {
		runTest();
		eventLog.log("TEST ENDED", args("status", getStatus()));
		clearLockIfHeld();
		setStatus(Status.RUNNING);
		super.fireTestFinished();

	}

	protected void requestProtectedResource() {
		eventLog.log("REQUESTING RESURCE", args());
	}

	@Override
	public void fireTestFinished() {
		System.out.println("Test finished");
		eventLog.log("Auth flow finished - run tst", args());
		doRunTest();
	}

	public void callApi() {
		super.requestProtectedResource();
	}

	public void doAuthFlow() {
		super.performAuthorizationFlow();
	}

	protected void onPostAuthorizationFlowComplete() {
		eventLog.log("POST AUTH CODE", args());

		eventLog.log("LOCK RELEASED", args());
		if(!testStarted) {
			testStarted = true;
			eventLog.log("STARTING TEST", args("status", getStatus()));
			super.acquireLock();
			doRunTest();
		} else {
			clearLockIfHeld();
		}
	}

	protected void preCallProtectedResource(String blockHeader) {

		eventLog.startBlock(currentClientString() + blockHeader);

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3");

		callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(EnsureResourceResponseReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-9", "FAPI1-BASE-6.2.1-10");

		eventLog.endBlock();
	}

	protected void switchToSecondClient() {

	}

}
