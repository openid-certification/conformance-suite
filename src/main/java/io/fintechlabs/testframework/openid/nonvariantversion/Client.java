package io.fintechlabs.testframework.openid.nonvariantversion;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fintechlabs.testframework.condition.client.AddRedirectUriToDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint;
import io.fintechlabs.testframework.condition.client.CreateDynamicRegistrationRequest;
import io.fintechlabs.testframework.condition.client.GetDynamicClientConfiguration;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

/**
 * Used to represent a client
 * Incomplete, just a PoC
 */
public class Client
{
	public enum ClientType {PUBLIC_CLIENT, CONFIDENTIAL_CLIENT};
	public enum ClientConfigType {STATIC, DYNAMIC};

	private ClientAuthentication authentication;

	private ClientType clientType;
	private String clientId;
	private String scope;
	private ClientConfigType clientConfigType;
	private JsonObject clientDetails;

	/**
	 * TODO clientConfig may be coming from test config or might be a dynamic client registration response
	 * they are not consistent at the moment.
	 * Normally the code should be able to get any configurable property from clientConfig
	 *
	 * clientConfig contains the following. Only client_config_type is required, the rest is optional
	 "client_config_type",	//static or dynamic
	 "client_type",	//public or confidential
	 "client_id",
	 "scope",
	 "authn_type",
	 "client_secret",
	 "jwks",
	 * @param clientConfig
	 */
	public Client(JsonObject clientConfig) {
		this.clientDetails = clientConfig;
		String clientConfigType = OIDFJSON.getString(clientConfig.get("client_config_type"));
		if("static".equals(clientConfigType)) {
			this.clientConfigType = ClientConfigType.STATIC;
			this.authentication = new ClientAuthentication(clientConfig);
			this.scope = OIDFJSON.getString(clientConfig.get("scope"));
			this.clientId = OIDFJSON.getString(clientConfig.get("client_id"));
		} else {
			this.clientConfigType = ClientConfigType.DYNAMIC;
		}
	}

	public boolean isDynamicRegistrationNeeded() {
		return this.clientConfigType == ClientConfigType.DYNAMIC;
	}

	public String getClientId()
	{
		return this.clientId;
	}

	/**
	 * adds necessary info about this client to environment
	 * @param environment
	 */
	public void addClientToEnvironment(Environment environment) {
		environment.putObject("client", clientDetails);
		environment.putObject("client_jwks", clientDetails.getAsJsonObject("jwks"));
	}

	/**
	 * There is a "client" in the environment and we initialize a client instance from environment
	 * @param environment
	 */
	public void mapFromClientInEnv(Environment environment)
	{
		this.clientDetails = environment.getObject("client");
		this.clientId = OIDFJSON.getString(this.clientDetails.get("client_id"));
		if(this.clientDetails.get("scope")!=null) {
			this.scope = OIDFJSON.getString(this.clientDetails.get("scope"));
		}
		this.authentication = new ClientAuthentication(this.clientDetails);
	}

	public Class<? extends ConditionSequence> getClientAuthenticationSequence(Environment env) {
		return this.authentication.getClientAuthenticationSequenceClass(env);
	}

	public Class<? extends ConditionSequence> getDynamicRegistrationSequence() {
		return DynamicRegistrationSequence.class;
	}

	/**
	 * dynamic client registration implementation
	 */
	public static class DynamicRegistrationSequence extends AbstractConditionSequence {
		@Override
		public void evaluate()
		{
			callAndStopOnFailure(GetDynamicClientConfiguration.class);
			//callAndStopOnFailure(ExtractJWKsFromDynamicClientConfiguration.class);

			// create basic dynamic registration request
			callAndStopOnFailure(CreateDynamicRegistrationRequest.class);
			//expose("client_name", env.getString("dynamic_registration_request", "client_name"));

			//callAndStopOnFailure(AddPublicJwksToDynamicRegistrationRequest.class, "RFC7591-2");
			callAndStopOnFailure(AddRedirectUriToDynamicRegistrationRequest.class);

			callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		}
	}
}
