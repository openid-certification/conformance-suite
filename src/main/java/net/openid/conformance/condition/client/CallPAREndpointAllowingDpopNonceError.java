package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
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

/**
 * This class makes a http post to PAR endpoint and examines the response for DPoP nonce errors and stores
 * the required nonce for retry
 */
public class CallPAREndpointAllowingDpopNonceError extends CallPAREndpoint {
	@Override
	public Environment evaluate(Environment env) {
		env.removeNativeValue("par_endpoint_dpop_nonce_error");
		return callParEndpoint(env, new DefaultResponseErrorHandler(){
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
						throw error("PAR Endpoint did not return a JSON object");
					}
					JsonObject jsonObject = jsonRoot.getAsJsonObject();
					log("Parsed PAR endpoint error response", jsonObject);
					if(jsonObject.has("error")) {
						if("use_dpop_nonce".equals(OIDFJSON.getString(jsonObject.get("error")))) {
							List<String> nonceList = e.getResponseHeaders().get("DPoP-Nonce");
							if(null != nonceList) {
								if(nonceList.size() == 1) {
									env.putString("authorization_server_dpop_nonce", nonceList.get(0));
									env.putString("par_endpoint_dpop_nonce_error", nonceList.get(0));
									env.putObject("par_endpoint_response", jsonObject);
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
					// response will be reparsed and handled in convertJsonResponseForEnvironment
				}
			}
		}
		// Can't find what we needed, so put full response into Env instead of throwing in super.handleRestClientResponseException

		ResponseEntity<String> responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getResponseHeaders(), e.getRawStatusCode());
		JsonObject fullResponse = convertJsonResponseForEnvironment("pushed authorization request", responseEntity, true);
		env.putObject(RESPONSE_KEY, fullResponse);
		return env;
	}


	protected Environment handleJsonParseException(Environment env, JsonParseException e) {
		throw error("Error parsing par endpoint response body as JSON", e);
	}

}
