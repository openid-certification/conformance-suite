package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.testmodules.support.ForceToValidateConsentResponse;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

import java.time.Instant;
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
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

		requestResourcesWithIncorrectPermissions();

		fireTestFinished();

	}

	@Override
	protected void performPreAuthorizationSteps() {
		callAndStopOnFailure(ForceToValidateConsentResponse.class);
		super.performPreAuthorizationSteps();
	}


	protected abstract void preFetchResources();
	protected abstract void prepareCorrectConsents();
	protected abstract void prepareIncorrectPermissions();

	protected abstract void requestResourcesWithIncorrectPermissions();

	@Override
	protected final void validateResponse() {

	}

}
