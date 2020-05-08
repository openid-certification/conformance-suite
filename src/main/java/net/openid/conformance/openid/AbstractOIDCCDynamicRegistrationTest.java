package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckCallbackContentTypeIsFormUrlEncoded;
import net.openid.conformance.condition.client.CheckCallbackHttpMethodIsPost;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.condition.client.GetDynamicClientConfiguration;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.RejectErrorInUrlQuery;
import net.openid.conformance.condition.client.SetScopeInClientConfigurationToOpenId;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.fapi.AbstractRedirectServerTestModule;
import net.openid.conformance.openid.AbstractOIDCCServerTest.ConfigureClientForClientSecretJwt;
import net.openid.conformance.openid.AbstractOIDCCServerTest.ConfigureClientForMtls;
import net.openid.conformance.openid.AbstractOIDCCServerTest.CreateAuthorizationRequestSteps;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OIDCCCreateDynamicClientRegistrationRequest;
import net.openid.conformance.sequence.client.SupportMTLSEndpointAliases;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

import java.util.function.Supplier;

@VariantParameters({
	ServerMetadata.class,
	ClientAuthType.class,
	ResponseType.class,
	ResponseMode.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "mtls", configurationFields = {
	"mtls.key",
	"mtls.cert",
	"mtls.ca"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client.client_name"
})
@VariantNotApplicable(parameter = ClientRegistration.class, values = {"static_client"})
@VariantNotApplicable(parameter = ServerMetadata.class, values = { "static" }) // dcr requires discovery
public abstract class AbstractOIDCCDynamicRegistrationTest extends AbstractRedirectServerTestModule {

	protected ResponseType responseType;
	protected boolean formPost;

	protected Supplier<? extends ConditionSequence> profileCompleteClientConfiguration = null;
	protected Class<? extends ConditionSequence> supportMTLSEndpointAliases = null;

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJwt() {
		profileCompleteClientConfiguration = () -> new ConfigureClientForClientSecretJwt();
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		profileCompleteClientConfiguration = null;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMtls() {
		profileCompleteClientConfiguration = () -> new ConfigureClientForMtls(true, false);
		supportMTLSEndpointAliases = SupportMTLSEndpointAliases.class;
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		formPost = getVariant(ResponseMode.class) == ResponseMode.FORM_POST;

		ClientAuthType clientAuthType = getVariant(ClientAuthType.class);
		env.putString("client_auth_type", clientAuthType.toString());

		responseType = getVariant(ResponseType.class);
		env.putString("response_type", responseType.toString());

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		if (supportMTLSEndpointAliases != null) {
			call(sequence(supportMTLSEndpointAliases));
		}

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(GetDynamicClientConfiguration.class);

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void onConfigure(JsonObject config, String baseUrl) {
		// No custom configuration
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		configureDynamicClient();
		if (profileCompleteClientConfiguration != null) {
			call(sequence(profileCompleteClientConfiguration));
		}
		exposeEnvString("client_id");

		performAuthorizationFlow();
	}

	protected void configureDynamicClient() {

		createDynamicClientRegistrationRequest();

		expose("client_name", env.getString("dynamic_registration_request", "client_name"));

		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		callAndStopOnFailure(SetScopeInClientConfigurationToOpenId.class);
	}

	protected void createDynamicClientRegistrationRequest() {
		// Includes AddPublicJwksToDynamicRegistrationRequest, which corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_jwks
		call(new OIDCCCreateDynamicClientRegistrationRequest(responseType));
	}

	abstract protected void performAuthorizationFlow();

	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps(formPost));
	}

	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void processCallback() {
		// We're not expecting a callback, but we need to handle any potential error response

		if (formPost) {
			env.mapKey("authorization_endpoint_response", "callback_body_form_params");
			callAndContinueOnFailure(CheckCallbackHttpMethodIsPost.class, Condition.ConditionResult.FAILURE, "OAuth2-FP-2");
			callAndContinueOnFailure(CheckCallbackContentTypeIsFormUrlEncoded.class, Condition.ConditionResult.FAILURE, "OAuth2-FP-2");
			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");
			callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");
		} else if (responseType.equals(ResponseType.CODE)) {
			env.mapKey("authorization_endpoint_response", "callback_query_params");
		} else {
			env.mapKey("authorization_endpoint_response", "callback_params");

			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");
			callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");
		}

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		eventLog.log(getName(), "Received a callback from the authorization endpoint. It is not necessary to complete login for this test.");

		// We need to release the test lock, but we're still waiting for the placeholder to be filled.
		setStatus(Status.WAITING);
	}

	@Override
	public void cleanup() {
		unregisterClient();
	}

	public void unregisterClient() {
		eventLog.startBlock("Unregister dynamically registered client");

		skipIfMissing(new String[] {"client"},
			new String[] {"registration_client_uri", "registration_access_token"},
			ConditionResult.INFO,
			UnregisterDynamicallyRegisteredClient.class);

		eventLog.endBlock();
	}

}
