package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilEnsureRedirectUrisMatchSoftwareRedirectUris extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "software_statement"})
	public Environment evaluate(Environment env) {

		JsonArray redirectUris = env.getElementFromObject("dynamic_registration_request", "redirect_uris").getAsJsonArray();
		JsonArray softwareRedirectUris = env.getElementFromObject("software_statement", "claims.software_redirect_uris").getAsJsonArray();

		if(redirectUris==null) {
			throw error("Registration request does not contain a redirect_uris element");
		}
		if(redirectUris.size()<1) {
			throw error("Registration request does not contain any redirect_uris");
		}
		if(softwareRedirectUris==null) {
			throw error("Software statement request does not contain a software_redirect_uris element");
		}
		if(softwareRedirectUris.size()<1) {
			throw error("Software statement does not contain any software_redirect_uris");
		}
		for(JsonElement redirUriElement : redirectUris){
			if(!softwareRedirectUris.contains(redirUriElement)) {
				throw error("Unexpected redirect_uri value", args("offending_value", redirUriElement, "expected", softwareRedirectUris));
			}
		}
		logSuccess("redirect_uris match or contain a sub set of software_redirect_uris");
		return env;
	}
}
