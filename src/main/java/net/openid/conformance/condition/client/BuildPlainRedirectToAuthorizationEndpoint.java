package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.web.util.UriComponentsBuilder;

public class BuildPlainRedirectToAuthorizationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "server" })
	@PostEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("authorization_endpoint_request")) {
			throw error("Couldn't find authorization endpoint request");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String authorizationEndpoint = env.getString("authorization_endpoint") != null ? env.getString("authorization_endpoint") : env.getString("server", "authorization_endpoint");

		if (Strings.isNullOrEmpty(authorizationEndpoint)) {
			throw error("Couldn't find authorization endpoint");
		}

		// send a front channel request to start things off
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authorizationEndpoint);

		for (String key : authorizationEndpointRequest.keySet()) {
			JsonElement element = authorizationEndpointRequest.get(key);

			// for nonce, state, client_id, redirect_uri, etc.
			if (element.isJsonPrimitive()) {
				if (key.equals("max_age")) {
					builder.queryParam(key, OIDFJSON.getNumber(element));
				} else {
					builder.queryParam(key, OIDFJSON.getString(element));
				}
			}
			// for claims
			else {

				builder.queryParam(key, element.toString());
			}

		}
		// FIXME hack walt.id requires this
		String claims = "{" +
			"  \"vp_token\": {" +
			"    \"presentation_definition\": {" +
			"      \"format\": null," +
			"      \"id\": \"1\"," +
			"      \"input_descriptors\": [" +
			"        {" +
			"          \"constraints\": null," +
			"          \"format\": null," +
			"          \"group\": null," +
			"          \"id\": \"1\"," +
			"          \"name\": null," +
			"          \"purpose\": null," +
			"          \"schema\": {" +
			"            \"uri\": \"https://api.preprod.ebsi.eu/trusted-schemas-registry/v1/schemas/0xb77f8516a965631b4f197ad54c65a9e2f9936ebfb76bae4906d33744dbcc60ba\"" +
			"          }" +
			"        }" +
			"      ]," +
			"      \"name\": null," +
			"      \"purpose\": null," +
			"      \"submission_requirements\": null" +
			"    }" +
			"  }" +
			"}";
		builder.queryParam("claims", claims);

		String redirectTo = builder.toUriString();

//		redirectTo = "https://wallet.walt.id/api/wallet/siopv2/initPresentation/?"+
//			"response_type=id_token&" +
//			"response_mode=form_post&" +
//			"client_id=https%3A%2F%2Flocalhost.emobix.co.uk%3A8443%2Ftest%2Fa%2Foidf-siop%2Fcallback&" +
//			"redirect_uri=https%3A%2F%2Flocalhost.emobix.co.uk%3A8443%2Ftest%2Fa%2Foidf-siop%2Fcallback&" +
//			"scope=openid&" +
//			"nonce=d6226617-5103-4721-a5a1-f31174bf8685&" +
//			"claims=%7B%22vp_token%22+%3A+%7B%22presentation_definition%22+%3A+%7B%22format%22+%3A+null%2C+%22id%22+%3A+%221%22%2C+%22input_descriptors%22+%3A+%5B%7B%22constraints%22+%3A+null%2C+%22format%22+%3A+null%2C+%22group%22+%3A+null%2C+%22id%22+%3A+%221%22%2C+%22name%22+%3A+null%2C+%22purpose%22+%3A+null%2C+%22schema%22+%3A+%7B%22uri%22+%3A+%22https%3A%2F%2Fapi.preprod.ebsi.eu%2Ftrusted-schemas-registry%2Fv1%2Fschemas%2F0xb77f8516a965631b4f197ad54c65a9e2f9936ebfb76bae4906d33744dbcc60ba%22%7D%7D%5D%2C+%22name%22+%3A+null%2C+%22purpose%22+%3A+null%2C+%22submission_requirements%22+%3A+null%7D%7D%7D&" +
//			"state=d6226617-5103-4721-a5a1-f31174bf8685";
//		redirectTo = "https://wallet.walt.id/api/wallet/siopv2/initPresentation/?"+
//			"response_type=id_token&" +
//			"response_mode=form_post&" +
//			"client_id=https%3A%2F%2Fverifier.walt.id%2Fverifier-api%2Fverify&" +
//			"redirect_uri=https%3A%2F%2Fverifier.walt.id%2Fverifier-api%2Fverify&" +
//			"scope=openid&" +
//			"nonce=d6226617-5103-4721-a5a1-f31174bf8685&" +
//			"claims=%7B%22vp_token%22+%3A+%7B%22presentation_definition%22+%3A+%7B%22format%22+%3A+null%2C+%22id%22+%3A+%221%22%2C+%22input_descriptors%22+%3A+%5B%7B%22constraints%22+%3A+null%2C+%22format%22+%3A+null%2C+%22group%22+%3A+null%2C+%22id%22+%3A+%221%22%2C+%22name%22+%3A+null%2C+%22purpose%22+%3A+null%2C+%22schema%22+%3A+%7B%22uri%22+%3A+%22https%3A%2F%2Fapi.preprod.ebsi.eu%2Ftrusted-schemas-registry%2Fv1%2Fschemas%2F0xb77f8516a965631b4f197ad54c65a9e2f9936ebfb76bae4906d33744dbcc60ba%22%7D%7D%5D%2C+%22name%22+%3A+null%2C+%22purpose%22+%3A+null%2C+%22submission_requirements%22+%3A+null%7D%7D%7D&" +
//			"state=d6226617-5103-4721-a5a1-f31174bf8685";

		logSuccess("Sending to authorization endpoint", args("redirect_to_authorization_endpoint", redirectTo, "auth_request", authorizationEndpointRequest));

		env.putString("redirect_to_authorization_endpoint", redirectTo);

		return env;
	}

}
