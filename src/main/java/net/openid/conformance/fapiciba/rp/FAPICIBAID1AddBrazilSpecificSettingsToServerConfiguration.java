package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPICIBAID1AddBrazilSpecificSettingsToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");
		{
			JsonArray algValues = new JsonArray();
			algValues.add("RSA-OAEP");
			server.add("request_object_encryption_alg_values_supported", algValues);
		}
		{
			JsonArray encValues = new JsonArray();
			encValues.add("A256GCM");
			server.add("request_object_encryption_enc_values_supported", encValues);
		}

		{
			JsonArray claimsSupported = new JsonArray();
			claimsSupported.add("cpf");
			claimsSupported.add("cnpj");
			claimsSupported.add("acr");
			server.add("claims_supported", claimsSupported);
		}

		{
			JsonArray acrValuesSupported = new JsonArray();
			acrValuesSupported.add("urn:brasil:openbanking:loa2");
			acrValuesSupported.add("urn:brasil:openbanking:loa3");
			server.add("acr_values_supported", acrValuesSupported);
		}

		{
			JsonArray idTokenSigAlgs = new JsonArray();
			idTokenSigAlgs.add("PS256");
			server.add("id_token_signing_alg_values_supported", idTokenSigAlgs);
		}

		{
			JsonArray algs = new JsonArray();
			algs.add("PS256");
			server.add("request_object_signing_alg_values_supported", algs);
		}

		{
			JsonArray algs = new JsonArray();
			algs.add("PS256");
			server.add("backchannel_authentication_request_signing_alg_values_supported", algs);
		}
		{
			JsonArray scopes = new JsonArray();
			scopes.add("openid");
			scopes.add("phone");
			scopes.add("profile");
			scopes.add("email");
			scopes.add("address");
			scopes.add("offline_access");
			scopes.add("consents");
			scopes.add("resources");
			scopes.add("payments");
			server.add("scopes_supported", scopes);
		}
		log("Added open banking Brazil specific server settings", args("server", server));
		return env;
	}

}
