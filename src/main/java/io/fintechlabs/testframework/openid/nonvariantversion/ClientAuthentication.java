package io.fintechlabs.testframework.openid.nonvariantversion;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.client.AddBasicAuthClientSecretAuthenticationParameters;
import io.fintechlabs.testframework.condition.client.AddFormBasedClientSecretAuthenticationParameters;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * authentication configuration for test clients
 */
public class ClientAuthentication {
	private Logger logger = LoggerFactory.getLogger(ClientAuthentication.class);

	public enum ClientAuthenticationType {
		none,
		client_secret_basic,
		client_secret_post,
		client_secret_jwt,
		private_key_jwt,
		tls_client_auth,
		self_signed_tls_client_auth
	};

	private ClientAuthenticationType authenticationType;
	private String clientSecret;

	/**
	 *
	 * @param clientConfig same fields as a dynamic client registration response
	 */
	public ClientAuthentication(JsonObject clientConfig) {
		String clientAuthnTypeStr = OIDFJSON.getString(clientConfig.get("token_endpoint_auth_method"));
		try {
			this.authenticationType = ClientAuthenticationType.valueOf(clientAuthnTypeStr);
		} catch (IllegalArgumentException ex) {
			logger.error("Invalid client authentication type '"+clientAuthnTypeStr+"', using none", ex);
			this.authenticationType = ClientAuthenticationType.none;
		}

		switch (this.authenticationType) {
			case client_secret_basic:
			case client_secret_post:
			case client_secret_jwt:
				this.clientSecret = OIDFJSON.getString(clientConfig.get("client_secret"));
				break;
			default:
				//FIXME this is just a PoC. handle other options
				break;
		}
	}


	public static class AddBasicAuthClientSecretAuthenticationToTokenRequest extends AbstractConditionSequence
	{
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddBasicAuthClientSecretAuthenticationParameters.class);
		}
	}

	public static class AddFormBasedClientSecretAuthenticationToTokenRequest extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddFormBasedClientSecretAuthenticationParameters.class);
		}
	}

	/**
	 * return the appropriate authentication sequence for authenticationType
	 * @param env
	 * @return
	 */
	public Class<? extends ConditionSequence> getClientAuthenticationSequenceClass(Environment env) {
		switch (this.authenticationType)
		{
			case none:
				return null;
			case client_secret_basic:
				env.getObject("client").addProperty("client_secret", this.clientSecret);
				return AddBasicAuthClientSecretAuthenticationToTokenRequest.class;
			case client_secret_post:
				env.getObject("client").addProperty("client_secret", this.clientSecret);
				return AddFormBasedClientSecretAuthenticationToTokenRequest.class;
			default:
				//FIXME add other client authentication types
				return null;
		}
	}
}
