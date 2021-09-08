package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

public class CreatePolicyUri extends AbstractCondition {

	// As per https://openid.net/specs/openid-connect-registration-1_0.html#Impersonation
	// "The Authorization Server SHOULD check to see if the logo_uri and policy_uri have the same host as the hosts defined in the array of redirect_uris."
	// so we generate a url that exists on the conformance server, and the login page is one of the few unauthenticated
	// pages we currently have
	private static final String LOGO_PATH = "/login.html";

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "policy_uri")
	public Environment evaluate(Environment env) {

		URI policyUri;

		try {
			URI baseUri = new URI(env.getString("base_url"));
			policyUri = new URI(
				baseUri.getScheme(),
				null,
				baseUri.getHost(),
				baseUri.getPort(),
				LOGO_PATH,
				null,
				null);
		} catch (URISyntaxException e) {
			throw error("Failed to generate logo URI", e);
		}

		env.putString("policy_uri", policyUri.toString());
		log("Generated policy URI", args("policy_uri", policyUri));

		return env;
	}

}
