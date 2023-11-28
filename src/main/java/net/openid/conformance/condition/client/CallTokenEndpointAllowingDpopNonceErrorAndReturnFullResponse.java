package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse extends CallTokenEndpointAndReturnFullResponse {

	// WARNING optional token_endpoint_dpop_nonce_error returned with required nonce value
	@Override
	@PreEnvironment(required = { "server", "token_endpoint_request_form_parameters" })
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		env.removeNativeValue("token_endpoint_dpop_nonce_error");
		return callTokenEndpoint(env, new DefaultResponseErrorHandler(){
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				if(response.getRawStatusCode() == 400) {
					String body = new String(this.getResponseBody(response));
					// Look for "error": "use_dpop_nonce" in body
					Pattern pattern = Pattern.compile(".*\"error\"\\s*:\\s*\"use_dpop_nonce\".*");
					Matcher matcher = pattern.matcher(body);
					if(matcher.matches()) {
						return true;
					}
				}
				return false;
			}
		});
	}

	@Override
	protected Environment handleRestClientResponseException(Environment env, RestClientResponseException e) {
		if(e.getRawStatusCode() == 400) {
			String jsonString = e.getResponseBodyAsString();
			if (!Strings.isNullOrEmpty(e.getResponseBodyAsString())) {
				try {
					JsonElement jsonRoot = JsonParser.parseString(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						throw error("Token Endpoint did not return a JSON object");
					}
					JsonObject jsonObject = jsonRoot.getAsJsonObject();

					log("Parsed token endpoint eror response", jsonObject);
					if(jsonObject.has("error")) {
						if("use_dpop_nonce".equals(OIDFJSON.getString(jsonObject.get("error")))) {
							List<String> nonceList = e.getResponseHeaders().get("DPoP-Nonce");
							if(null != nonceList) {
								if(nonceList.size() == 1) {
									env.putString("authorization_server_dpop_nonce", nonceList.get(0));
									env.putString("token_endpoint_dpop_nonce_error", nonceList.get(0));
									log("Got DPoP-Nonce header", args("DPoP-Nonce", nonceList.get(0)));
									env.putObject("token_endpoint_response", jsonObject);
									return env;
								} else {
									return super.handleRestClientResponseException(env, e);
								}
							} else {
								return super.handleRestClientResponseException(env, e);
							}
						}
					}
				} catch (JsonParseException parseException) {
//					return handleJsonParseException(env, parseException);
				}
			}
		}
		// Can't find what we needed, so put full response into Env instead of throwing error in super.handleRestClientResponseException
		ResponseEntity<String> responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getResponseHeaders(), e.getRawStatusCode());
		addFullResponse(env, responseEntity);

		if (Strings.isNullOrEmpty(e.getResponseBodyAsString())) {
			throw error("Missing or empty response from the token endpoint");
		}
		if(jsonParseError && (null != jsonParseException)) {
			return handleJsonParseException(env, jsonParseException);
		}
		if(jsonObjectError) {
			throw error("Token Endpoint did not return a JSON object", args("response", e.getResponseBodyAsString()));
		}
		env.putObject("token_endpoint_response", env.getElementFromObject("token_endpoint_response_full", "body_json").getAsJsonObject());
		logSuccess("Parsed token endpoint response", env.getObject("token_endpoint_response"));
		return env;
	}

}
