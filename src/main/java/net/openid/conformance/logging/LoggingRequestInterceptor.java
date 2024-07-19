package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.DataUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor, DataUtils {

	private final String source;
	private final TestInstanceEventLog log;
	private JsonObject mutualTls;

	public LoggingRequestInterceptor(String source, TestInstanceEventLog log, JsonObject mutualTls) {
		this.source = source;
		this.log = log;
		this.mutualTls = mutualTls; // if we get in the mutual TLS parameters from the environment, save them for logging purposes
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		logRequest(request, body);
		WrappedClientHttpResponse response = new WrappedClientHttpResponse(execution.execute(request, body));
		logResponse(response);
		return response;
	}

	private void logRequest(HttpRequest request, byte[] body) throws IOException {
		JsonObject o = new JsonObject();
		o.addProperty("request_uri", request.getURI().toString());
		o.addProperty("request_method", request.getMethod().toString());
		o.add("request_headers", mapToJsonObject(request.getHeaders(), false));
		if (body != null) {
			o.addProperty("request_body", new String(body, StandardCharsets.UTF_8));
		}
		o.addProperty("msg", "HTTP request");
		o.addProperty("http", "request");
		if (mutualTls != null) {
			o.add("request_mutual_tls", mutualTls);
		}
		log.log(source, o);
	}

	private void logResponse(WrappedClientHttpResponse response) throws IOException {
		JsonObject o = new JsonObject();
		o.addProperty("response_status_code", response.getStatusCode().toString());
		o.addProperty("response_status_text", response.getStatusText());
		o.add("response_headers", mapToJsonObject(response.getHeaders(), true));
		if (response.body != null) {
			o.addProperty("response_body", new String(response.body, StandardCharsets.UTF_8));
		}
		o.addProperty("msg", "HTTP response");
		o.addProperty("http", "response");
		if (response.bodyException != null) {
			o.addProperty("exception_reading_body", response.bodyException.getMessage());
		}
		log.log(source, o);
	}

	private static final class WrappedClientHttpResponse implements ClientHttpResponse {

		private final ClientHttpResponse response;
		private byte[] body;
		private IOException bodyException;

		public WrappedClientHttpResponse(ClientHttpResponse response) {
			this.response = response;
			try {
				this.body = StreamUtils.copyToByteArray(response.getBody());
				this.bodyException = null;
			} catch (IOException e) {
				this.body = null;
				this.bodyException = e;
			}
		}

		@Override
		public InputStream getBody() throws IOException {
			if (body != null) {
				return new ByteArrayInputStream(body);
			} else {
				throw bodyException;
			}
		}

		@Override
		public HttpHeaders getHeaders() {
			return response.getHeaders();
		}

		@Override
		public HttpStatusCode getStatusCode() throws IOException {
			return response.getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return response.getStatusText();
		}

		@Override
		public void close() {
			response.close();
		}

	}

}
