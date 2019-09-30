package io.fintechlabs.testframework.openid.nonvariantversion;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddAudToRequestObject;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddIssToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddScopeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallProtectedResourceWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForSubjectInIdToken;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters;
import io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.client.ConfigurationRequestsTestIsSkipped;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.RejectErrorInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseModeToQuery;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenACRClaimAgainstRequest;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignatureUsingKid;
import io.fintechlabs.testframework.condition.client.ValidateServerJWKs;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInServerJWKs;
import io.fintechlabs.testframework.fapi.AbstractRedirectServerTestModule;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * INCOMPLETE. PoC.
 * Base class for OIDC core tests.
 * Does not use variants.
 * Contains many small, easily overridable methods
 *
 */
public abstract class AbstractOIDCCoreServerTestNonVariant extends AbstractRedirectServerTestModule
{
	private Logger logger = LoggerFactory.getLogger(AbstractOIDCCoreServerTestNonVariant.class);
	/**
	 * have two clients,
	 * client1 is not null but client2 may be null if it's not configured or if not needed
	 */
	protected Client client1;
	protected Client client2;
	/**
	 * use defaults if not configured
	 */
	protected ResponseMode responseMode;
	/**
	 * use defaults if not configured
	 */
	protected ResponseType responseType;
	/**
	 * static or dynamic OP configuration
	 * tests must be able to get various configuration options from server configuration at runtime
	 * e.g auto-select a signing algorithm supported by the server
	 */
	protected ServerConfiguration serverConfiguration;
	/**
	 * when true tls checks will be skipped
	 * this will also be added to env and all relevant Conditions needs to be updated to
	 * take it into account
	 * This is similar to the insecure option in the old suite
	 */
	protected boolean ignoreCertificateErrors;

	public enum AuthnRequestType {plain, signed, encrypted};

	protected AuthnRequestType authnRequestType;

	protected Client currentClient;


	/**
	 * These are additional tests involving the second client,
	 * performed after the usual post-authorization flow.
	 */
	protected void performSecondClientTests() {

	}

	/**
	 * TODO add various onBefore onAfter methods that can be overidden
	 */
	protected void onBeforeRequestAuthorizationCode() {

	}

	protected void onAfterRequestAuthorizationCode() {

	}

	protected void onBeforeCreateAuthorizationRequest() {

	}

	protected void onBeforeCreateAuthorizationRedirect() {

	}

	protected void onBeforeSwitchingToSecondClient()
	{

	}

	/**
	 * Use small, overridable methods
	 * @param config
	 *            A JSON object consisting of details that the testRunner
	 *            doesn't need to know about
	 * @param baseUrl
	 *            The base of the URL that will need to be appended to any
	 *            URL construction.
	 * @param externalUrlOverride
	 */
	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride)
	{
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		Boolean skip = env.getBoolean("config", "skip_test");
		if (skip != null && skip) {
			// This is intended for use in our CI where we insist all tests run to completion
			// It would be used as a temporary measure in an 'override' where one of the environments we are testing
			// against is not able to run the test to completion due to an issue in that environments.
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
			return;
		}
		configureIgnoreCertificateErrors();

		configureResponseType();
		configureResponseMode();

		configureAuthnRequestType();

		configureCreateRedirectUri();

		configureServerConfiguration();
		configureServerConfigurationSanityChecks();

		// Set up the client configuration
		configureClients();

		configureResourceEndpoint();

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void configureIgnoreCertificateErrors() {
		Boolean configValue = env.getBoolean("config", "tls.ignore_cert_errors");
		if(configValue!=null && configValue) {
			this.ignoreCertificateErrors = true;
			env.putBoolean("ignoreCertificateErrors", Boolean.TRUE);
		} else {
			env.putBoolean("ignoreCertificateErrors", Boolean.FALSE);
		}
	}

	protected void configureAuthnRequestType() {
		String requestTypeStr = env.getString("config", "authn_request.type");
		if(requestTypeStr==null || requestTypeStr.isEmpty()) {
			this.authnRequestType = AuthnRequestType.plain;
			return;
		}
		try {
			this.authnRequestType = AuthnRequestType.valueOf(requestTypeStr);
		} catch (IllegalArgumentException ex) {
			this.authnRequestType = AuthnRequestType.plain;
			logger.error("Invalid authn_request.type: '"+requestTypeStr+"'", ex);
		}
	}

	protected void configureResponseMode() {
		String responseModeStr = env.getString("config", "response_mode");
		this.responseMode = new ResponseMode(responseModeStr, responseType);
	}

	protected void configureResponseType() {
		String responseTypeStr = env.getString("config", "response_type");
		this.responseType = new ResponseType(responseTypeStr);
	}

	protected void configureResourceEndpoint() {
		/*
		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
		*/
	}

	protected void configureServerConfigurationSanityChecks() {
		/*
		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);
		*/
		callAndStopOnFailure(FetchServerKeys.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
	}

	protected void configureServerConfiguration() {
		String discoveryTypeStr = env.getString("config", "server.discovery_type");
		if("static".equals(discoveryTypeStr))
		{
			JsonObject serverInfoFromConfig = env.getElementFromObject("config", "server.static_server_configuration").getAsJsonObject();
			this.serverConfiguration = new ServerConfiguration(ServerConfiguration.DiscoveryType.STATIC, serverInfoFromConfig);
			env.putObject("server", serverInfoFromConfig);
		} else {
			callAndStopOnFailure(GetDynamicServerConfiguration.class);
			JsonObject serverConfigJsonObj = env.getObject("server");
			this.serverConfiguration = new ServerConfiguration(ServerConfiguration.DiscoveryType.DYNAMIC, serverConfigJsonObj);
		}
	}

	protected void configureCreateRedirectUri() {
		callAndStopOnFailure(CreateRedirectUri.class);
		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");
	}

	protected void configureClient1() {
		if(env.getElementFromObject("config", "client1")!=null) {
			client1 = new Client(env.getElementFromObject("config", "client1").getAsJsonObject());
			if(client1.isDynamicRegistrationNeeded()) {
				call(sequence(client1.getDynamicRegistrationSequence()));
				env.getObject("client").add("scope", env.getElementFromObject("config", "client.scope"));
				client1.mapFromClientInEnv(env);
			}
		}
	}

	protected void configureClient2() {
		if(env.getElementFromObject("config", "client2")!=null) {
			//we have a second client
			client2 = new Client(env.getElementFromObject("config", "client2").getAsJsonObject());
			if(client2.isDynamicRegistrationNeeded()) {
				call(sequence(client2.getDynamicRegistrationSequence()));
				env.getObject("client").add("scope", env.getElementFromObject("config", "client.scope"));
				client2.mapFromClientInEnv(env);
			}
		}
	}

	protected void configureClients()
	{
		configureClient1();

		configureClient2();
	}

	protected String currentClientString() {
		if(currentClient==client2) {
			return "Second client(" + currentClient.getClientId() + ") ";
		}
		else {
			return "First client(" + currentClient.getClientId() + ") ";
		}
	}

	protected boolean isSecondClient() {
		return currentClient == client2;
	}


	/**
	 * step 1
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);
		currentClient = client1;
		currentClient.addClientToEnvironment(env);
		performAuthorizationFlow();
	}


	/**
	 * step 2
	 */
	protected void performAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");

		onBeforeCreateAuthorizationRequest();

		createAuthorizationRequest();

		setResponseModeForAuthorizationRequest();

		onBeforeCreateAuthorizationRedirect();

		createAuthorizationRedirect();

		performRedirect();

		eventLog.endBlock();
	}


	protected void setResponseModeForAuthorizationRequest() {
		if(responseMode.isQuery()) {
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToQuery.class);
		}
		else if(responseMode.isFormPost()) {
			//TODO add form_post
		}
		else if(responseMode.isFragment()) {
			//TODO add fragment
		}

		//TODO throw an error if it reaches here
	}


	protected void createAuthorizationRedirect() {
		switch (this.authnRequestType) {
			case signed:
				createAuthorizationRedirectWithRequestObject();
				break;
			case encrypted:
				//TODO
				break;
			default:
				createAuthorizationRedirectPlain();
				break;
		}
	}


	protected void createAuthorizationRedirectPlain() {
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);
	}

	/**
	 * Incomplete. probably won't work
	 */
	protected void createAuthorizationRedirectWithRequestObject() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(AddExpToRequestObject.class);

		callAndStopOnFailure(AddAudToRequestObject.class);

		callAndStopOnFailure(AddIssToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	protected void createAuthorizationRequestState() {
		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);
	}

	protected void createAuthorizationRequestNonce() {
		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);
	}

	protected void createAuthorizationRequestResponseType() {
		//set response type
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		authorizationEndpointRequest.addProperty("response_type", responseType.getAsString());
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);
	}

	protected void createAuthorizationRequest() {
		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		createAuthorizationRequestState();

		createAuthorizationRequestNonce();

		createAuthorizationRequestResponseType();

		callAndStopOnFailure(AddScopeToAuthorizationEndpointRequest.class);

	}


	/**
	 * step 3
	 */
	protected void processCallback() {
		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");
		//TODO will form_post work like this?
		if (responseMode.isQuery() || responseMode.isFormPost()) {
			env.mapKey("authorization_endpoint_response", "callback_query_params");
		} else if(responseMode.isFragment()) {
			env.mapKey("authorization_endpoint_response", "callback_params");

			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");
			callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");
		}

		onAuthorizationCallbackResponse();
		eventLog.endBlock();
	}

	protected void onAuthorizationCallbackResponse() {
		callAndStopOnFailure(CheckMatchingCallbackParameters.class);
		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);
		callAndStopOnFailure(CheckMatchingStateParameter.class);

		if (responseType.includesCode()) {
			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);
		}
		if (responseType.includesToken()) {
			callAndStopOnFailure(ExtractAccessTokenFromAuthorizationResponse.class);
		}
		handleSuccessfulAuthorizationEndpointResponse();
	}

	protected void handleSuccessfulAuthorizationEndpointResponse() {
		if (responseType.includesIdToken()) {
			callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class);

			// save the id_token returned from the authorisation endpoint
			env.putObject("authorization_endpoint_id_token", env.getObject("id_token"));

			performIdTokenValidation();
		}
		if (responseType.includesCode()) {
			performAuthorizationCodeValidation();
		}
		if (responseType.includesToken()) {
			requestProtectedResource();
		}
		performPostAuthorizationFlow();
	}

	protected void performIdTokenValidation() {
		callAndContinueOnFailure(ValidateIdToken.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIdTokenNonce.class, Condition.ConditionResult.FAILURE, "OIDCC-2");
		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");
		callAndContinueOnFailure(ValidateIdTokenSignature.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckForSubjectInIdToken.class, Condition.ConditionResult.FAILURE);
	}

	protected void performAuthorizationCodeValidation() {
		if (responseType.includesIdToken()) {
			callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			skipIfMissing(new String[] { "c_hash" }, null, Condition.ConditionResult.INFO,
				ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		}
	}

	protected void requestProtectedResource() {
		if(env.containsObject("resource")) {
			eventLog.startBlock(currentClientString() + "Resource server endpoint tests");
			callAndStopOnFailure(CallProtectedResourceWithBearerToken.class);
			eventLog.endBlock();
		}
	}

	/**
	 * step 4
	 */
	protected void performPostAuthorizationFlow() {
		if (responseType.includesCode()) {
			// call the token endpoint and complete the flow
			createAuthorizationCodeRequest();
			onBeforeRequestAuthorizationCode();
			requestAuthorizationCode();
			onAfterRequestAuthorizationCode();
		}
		requestProtectedResource();
		onPostAuthorizationFlowComplete();
	}

	protected void onPostAuthorizationFlowComplete() {
		if(client2!=null) {
			if (currentClient != client2)
			{
				switchToSecondClient();
				performAuthorizationFlow();
			}
			else
			{
				performSecondClientTests();
			}
		}
		else {
			fireTestFinished();
		}

	}

	protected void switchToSecondClient() {
		//switching clients
		onBeforeSwitchingToSecondClient();
		currentClient = client2;
		currentClient.addClientToEnvironment(env);
	}

	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);
		Class<? extends ConditionSequence> clientAuthSeq = currentClient.getClientAuthenticationSequence(env);
		if(clientAuthSeq!=null) {
			call(sequence(clientAuthSeq));
		}
	}

	/**
	 * step 5
	 */
	protected void requestAuthorizationCode() {
		call(exec().startBlock( currentClientString() + "Calling token endpoint with authorization code"));
		callAndStopOnFailure(CallTokenEndpoint.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckForAccessTokenValue.class);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);


		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "OIDCC-3.3.2.5");
		performIdTokenValidation();

		callAndContinueOnFailure(CheckForSubjectInIdToken.class, Condition.ConditionResult.FAILURE);

		call(exec().endBlock());
	}



}
