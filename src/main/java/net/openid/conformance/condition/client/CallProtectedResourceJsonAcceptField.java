package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Collections;

public class CallProtectedResourceJsonAcceptField extends CallProtectedResource{
	@Override
	protected HttpHeaders getHeaders(Environment env) {
		HttpHeaders headers = super.getHeaders(env);

		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		return headers;
	}
}
