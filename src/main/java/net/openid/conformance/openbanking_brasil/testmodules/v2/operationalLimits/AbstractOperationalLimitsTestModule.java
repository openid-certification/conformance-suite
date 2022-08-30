package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.logging.TestInstanceEventLogIgnoreSuccess;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseHasLinksForConsents;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateResponseMetaData;
import net.openid.conformance.openbanking_brasil.testmodules.v2.GenerateRefreshAccessTokenSteps;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.variant.ClientAuthType;

import java.util.Map;

public abstract class AbstractOperationalLimitsTestModule extends AbstractOBBrasilFunctionalTestModule {

	private TestInstanceEventLog originalLogger;
	private TestInstanceEventLog ignoreSuccessLogger;
	protected ClientAuthType clientAuthType;

	@Override
	public void setProperties(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager, ImageService imageService) {
		this.ignoreSuccessLogger = new TestInstanceEventLogIgnoreSuccess(eventLog);
		this.originalLogger = eventLog;
		super.setProperties(id, owner, eventLog, browser, testInfo, executionManager, imageService);
	}

	@Override
	protected void configureClient() {
		// Everything below is taken from super
		validateFirstClient();
		validateSecondClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		clientAuthType = getVariant(ClientAuthType.class);
		super.onConfigure(config, baseUrl);
	}

	protected void disableLogging() {
		if(!eventLog.equals(ignoreSuccessLogger)){
			eventLog.log(getName(), "Logging is reduced. Only errors and warnings will be displayed");
			eventLog = ignoreSuccessLogger;
		}
	}

	protected void enableLogging() {
		if(!eventLog.equals(originalLogger)){
			eventLog = originalLogger;
			eventLog.log(getName(), "Full logging is enabled");
		}
	}

	protected void runInLoggingBlock(Runnable runnable){
		enableLogging();
		runnable.run();
		disableLogging();
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		return false;
	}

	protected void validateSecondClient() {
		eventLog.startBlock("Verify configuration of Operational Limits client");

		// extract second client
		switchToSecondClient();
		callAndStopOnFailure(GetStaticClient2Configuration.class);
		callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, Condition.ConditionResult.FAILURE);

		validateClientConfiguration();

		unmapClient();

		callAndContinueOnFailure(ValidateClientPrivateKeysAreDifferent.class, Condition.ConditionResult.FAILURE);

		eventLog.endBlock();
	}

	protected void validateFirstClient() {
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		// Test won't pass without MATLS, but we'll try anyway (for now)
		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);

		validateClientConfiguration();
	}

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddOpenIdScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void performPreAuthorizationSteps() {
		super.performPreAuthorizationSteps();

		call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING);

		if (getResult() == Result.WARNING) {
			fireTestFinished();
		} else {
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
			call(exec().unmapKey("endpoint_response"));
			validatePermissions();

			if (getResult() == Result.WARNING) {
				fireTestFinished();
			} else {
				callAndContinueOnFailure(EnsureResponseHasLinksForConsents.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
				callAndStopOnFailure(ExtractConsentIdFromConsentEndpointResponse.class);
				callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11", "FAPI1-BASE-6.2.1-11");
				callAndStopOnFailure(FAPIBrazilAddConsentIdToClientScope.class);
			}
		}
	}

	protected void validatePermissions() {
		callAndContinueOnFailure(FAPIBrazilConsentEndpointResponseValidatePermissions.class, Condition.ConditionResult.WARNING);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		return new OpenBankingBrazilPreAuthorizationSteps(isSecondClient(), false, addTokenEndpointClientAuthentication, brazilPayments.isTrue(), true);
	}

	protected void refreshAccessToken() {
		runInLoggingBlock(() -> {
			GenerateRefreshAccessTokenSteps refreshAccessTokenSteps = new GenerateRefreshAccessTokenSteps(clientAuthType);
			call(refreshAccessTokenSteps);
		});
	}

	@Override
	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
		eventLog.log(getName(), "Switched to second client");
		JsonObject client = env.getObject("client");
		if (client != null) {
			eventLog.log(getName(), client);

		}
	}

	@Override
	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("mutual_tls_authentication");
		eventLog.log(getName(), "Switched to first client");
		JsonObject client = env.getObject("client");
		if (client != null) {
			eventLog.log(getName(), client);

		}
	}


}
