package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;

import java.time.Instant;

public abstract class AbstractPermissionsCheckingFunctionalTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(RememberOriginalScopes.class);
		prepareCorrectConsents();
	}

	private boolean preFetchResources = false;

	@Override
	protected void requestProtectedResource() {
		if(!preFetchResources) {
			preFetchResources = true;
			super.requestProtectedResource();
			eventLog.startBlock(currentClientString() + "Validate response");
			preFetchResources();
			callAndStopOnFailure(ResetScopesToConfigured.class);
			prepareIncorrectPermissions();
			performAuthorizationFlow();
			eventLog.endBlock();
		}
	}

	boolean preFetched = false;

	@Override
	protected void onPostAuthorizationFlowComplete() {

		if(!preFetched) {
			preFetched = true;
			return;
		}

		waitForBrowserToReturn();

		requestResourcesWithIncorrectPermissions();
		forceReleaseLock();
		setResult(Result.PASSED);
		setStatus(Status.FINISHED);

	}



	protected void waitForBrowserToReturn() {

		getTestExecutionManager().runFinalisationTaskInBackground(() -> {

			Instant timeout = Instant.now().plusSeconds(60); // wait at most 60 seconds
			while (browser.getWebRunners().size() > 0
				&& Instant.now().isBefore(timeout)) {
				Thread.sleep(100); // sleep before we check again
			}

			return "Done";
		});
	}


	protected abstract void preFetchResources();
	protected abstract void prepareCorrectConsents();
	protected abstract void prepareIncorrectPermissions();

	protected abstract void requestResourcesWithIncorrectPermissions();

	@Override
	protected final void validateResponse() {

	}

}
