package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EncryptUserInfoResponse extends AbstractJWEEncryptString
{

	/**
	 * Also requires, either signed_user_info_endpoint_response or user_info_endpoint_response
	 * @param env
	 * @return
	 */
	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(strings = "encrypted_user_info_endpoint_response")
	public Environment evaluate(Environment env) {

		String userinfoResponse = env.getString("signed_user_info_endpoint_response");
		if(userinfoResponse==null) {
			JsonObject unsignedUserinfo = env.getObject("user_info_endpoint_response");
			userinfoResponse = unsignedUserinfo.toString();
		}
		String alg = env.getString("client", "userinfo_encrypted_response_alg");
		String enc = env.getString("client", "userinfo_encrypted_response_enc");
		String clientSecret = env.getString("client", "client_secret");
		//client jwks may be null
		JsonElement clientJwksElement = env.getElementFromObject("client", "jwks");
		JsonObject clientJwks = null;
		if(clientJwksElement!=null) {
			clientJwks = clientJwksElement.getAsJsonObject();
		}

		String encryptedResponse = encrypt("client", userinfoResponse, clientSecret, clientJwks, alg, enc,
			"userinfo_encrypted_response_alg", "userinfo_encrypted_response_enc");

		logSuccess("Encrypted userinfo response", args("userinfo", encryptedResponse,
															"userinfo_encrypted_response_alg", alg,
															"userinfo_encrypted_response_enc", enc));
		env.putString("encrypted_user_info_endpoint_response", encryptedResponse);
		return env;
	}

}
