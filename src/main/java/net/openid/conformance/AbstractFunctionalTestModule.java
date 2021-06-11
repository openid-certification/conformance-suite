package net.openid.conformance;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalServerTestModule;

public abstract class AbstractFunctionalTestModule extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected void requestProtectedResource() {

		super.requestProtectedResource();
		eventLog.startBlock(currentClientString() + "Validate response");
		validateResponse();
		eventLog.endBlock();

	}

	protected void configureClient() {
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		// Test won't pass without MATLS, but we'll try anyway (for now)
		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);

		validateClientConfiguration();

	}

	protected void switchToSecondClient() {

	}

	protected abstract void validateResponse();

}
