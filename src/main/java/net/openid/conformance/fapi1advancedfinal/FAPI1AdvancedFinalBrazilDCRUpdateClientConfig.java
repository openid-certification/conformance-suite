package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRedirectUriQuerySuffix;
import net.openid.conformance.condition.client.AddRedirectUriToClientConfigurationRequest;
import net.openid.conformance.condition.client.AddScopeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToClientConfigurationRequest;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientConfigurationCredentialsFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRedirectUrisFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.CheckScopesFromDynamicRegistrationEndpointDoNotExceedRequestedScopes;
import net.openid.conformance.condition.client.CreateClientConfigurationRequestFromDynamicClientRegistrationResponse;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.FapiBrazilVerifyRedirectUriContainedInSoftwareStatement;
import net.openid.conformance.condition.client.ReverseScopeOrderInDynamicRegistrationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-update-client-config",
	displayName = "FAPI1-Advanced-Final: Brazil DCR update client config",
	summary = "Obtain a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration), register a new client on the target authorization server and perform an authorization flow. After the registration, a PUT will be made to the RFC7592 client to change the redirect uri (both redirect uris must be present in the software on the Brazil directory), which must succeed and an authorization flow will then be run using the new redirect uri.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI1AdvancedFinalBrazilDCRUpdateClientConfig extends AbstractFAPI1AdvancedFinalBrazilDCR {

	@Override
	protected void callRegistrationEndpoint() {
		super.callRegistrationEndpoint();

		eventLog.startBlock("Make PUT request to client configuration endpoint to change redirect uri");
		callAndStopOnFailure(CreateClientConfigurationRequestFromDynamicClientRegistrationResponse.class);
		// get a new SSA (technically there should be one in the DCR response, but they may be single use?)
		callAndStopOnFailure(FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		callAndStopOnFailure(AddSoftwareStatementToClientConfigurationRequest.class);
		callAndStopOnFailure(AddRedirectUriQuerySuffix.class, "RFC6749-3.1.2");
		callAndStopOnFailure(CreateRedirectUri.class, "RFC6749-3.1.2");
		callAndContinueOnFailure(FapiBrazilVerifyRedirectUriContainedInSoftwareStatement.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(AddRedirectUriToClientConfigurationRequest.class);

		callAndStopOnFailure(CallClientConfigurationEndpoint.class);
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckRedirectUrisFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");
		callAndContinueOnFailure(CheckClientConfigurationCredentialsFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7592-3");

	}
}
