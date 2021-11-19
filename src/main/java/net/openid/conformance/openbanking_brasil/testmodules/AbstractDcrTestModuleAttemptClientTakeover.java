package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddJwksUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToClientConfigurationRequest;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientConfigurationUriFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckNoClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRedirectUrisFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.CreateClientConfigurationRequestFromDynamicClientRegistrationResponse;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilExtractJwksUriFromSoftwareStatement;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;

public abstract class AbstractDcrTestModuleAttemptClientTakeover extends AbstractFAPI1AdvancedFinalBrazilDCR {
    protected abstract void switchToAlternateClient();

	@Override
	protected void callRegistrationEndpoint() {
		super.callRegistrationEndpoint();

		eventLog.startBlock("Make PUT request to client configuration endpoint with no changes expecting success");
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		// get a new SSA (technically there should be one in the DCR response, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);
		callAndStopOnFailure(CallClientConfigurationEndpoint.class);
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationUriFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationAccessTokenFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");

		eventLog.startBlock("Attempt 'GET' on client configuration endpoint using MTLS certificate for different software, which must fail");
		//env.putObjectFromJsonString("altconfig", "{\"client\":{}, \"directory\":{}, \"mtls\":{}}");
		env.putObject("altconfig", env.getObject("config").deepCopy());
		env.mapKey("config", "altconfig");
		env.mapKey("mutual_tls_authentication", "altmtls");
		switchToAlternateClient();
		callAndStopOnFailure(ExtractMTLSCertificatesFromConfiguration.class);

		// https://github.com/OpenBanking-Brasil/specs-seguranca/issues/199

		env.removeObject("registration_client_endpoint_request_body"); // so a 'GET' is made
		callAndStopOnFailure(CallClientConfigurationEndpoint.class, "OIDCD-4.2");
		env.mapKey("endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2", "RFC7592-2.2");
		call(exec().unmapKey("endpoint_response"));

		getSsa();
		env.unmapKey("mutual_tls_authentication");

		eventLog.startBlock("Calling PUT on configuration endpoint with SSA from another client");

		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);
		callAndStopOnFailure(FAPIBrazilExtractJwksUriFromSoftwareStatement.class, "BrazilOBDCR-7.1-5");
		callAndStopOnFailure(AddJwksUriToDynamicRegistrationRequest.class, "RFC7591-2", "BrazilOBDCR-7.1-5");
		callAndStopOnFailure(CallClientConfigurationEndpoint.class);

		env.mapKey("endpoint_response", "registration_client_endpoint_response");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "RFC7592-2.1");
		callAndContinueOnFailure(CheckNoClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));

		deleteClient();
	}

	@Override
	public void start() {
		fireTestFinished();
	}
}
