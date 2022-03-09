package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

public class SetUtf8JsonAcceptHeadersForResourceEndpointRequest extends AbstractCondition {

	// The FAPI1-R implementers draft 2 specs explicitly require the charset UTF8 header, but Spring have correctly
	// deprecated this in their http module, so we have our own definition to ensure we follow the specs.
	// https://github.com/spring-projects/spring-framework/issues/22788
	// https://bitbucket.org/openid/fapi/issues/236/charset-not-needed-for-application-json
	MediaType MEDIATYPE_APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

	@Override
	@PreEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		requestHeaders.addProperty(HttpHeaders.ACCEPT, MEDIATYPE_APPLICATION_JSON_UTF8.toString());
		requestHeaders.addProperty(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.toString());

		logSuccess("Set Accept header", args("Accept", requestHeaders.get("Accept")));

		return env;
	}

}
