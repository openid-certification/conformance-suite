package net.openid.conformance.openid.ssf.support;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamAdminClient {

	private final String configurationEndpoint;
	private final String transmitterAccessToken;

	public StreamAdminClient(String configurationEndpoint, String transmitterAccessToken) {
		this.configurationEndpoint = configurationEndpoint;
		this.transmitterAccessToken = transmitterAccessToken;
	}

	public ResponseEntity<QueryStreamOutput> queryStreamConfig() {
		return queryStreamConfig(null);
	}

	public ResponseEntity<QueryStreamOutput> queryStreamConfig(String streamId) {
		var restClient = createRestClient();
		String queryStreamUri = configurationEndpoint;
		if (streamId != null) {
			queryStreamUri = queryStreamUri + "?stream_id=" + streamId;
		}
		var response = restClient.get()
			.uri(queryStreamUri)
			.headers(headers -> headers.setBearerAuth(transmitterAccessToken))
			.retrieve().onStatus(new NoOpResponseErrorHandler())
			.toEntity(QueryStreamOutput.class);

		return response;
	}

	@NotNull
	private static RestClient createRestClient() {
		return RestClient.builder()
			.messageConverters(converters -> converters.add(new MappingJackson2HttpMessageConverter() {
				{
					// workaround wrong content-type returned by caep.dev
					setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
				}
			}))
			.build();
	}

	public ResponseEntity<CreateStreamOutput> createStream(CreateStreamInput input) {

		var restClient = createRestClient();
		var response = restClient.post()
			.uri(configurationEndpoint)
			.headers(headers -> headers.setBearerAuth(transmitterAccessToken))
			.body(input)
			.retrieve()
			.toEntity(CreateStreamOutput.class);

		return response;
	}

	public ResponseEntity<?> deleteStream(String streamId) {

		String deleteStreamUri = configurationEndpoint;
		if (streamId != null) {
			deleteStreamUri = deleteStreamUri + "?stream_id=" + streamId;
		}

		var restClient = createRestClient();
		var response = restClient.delete()
			.uri(deleteStreamUri)
			.headers(headers -> headers.setBearerAuth(transmitterAccessToken))
			.retrieve()
			.toBodilessEntity();

		return response;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class QueryStreamOutput extends StreamConfiguration {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class CreateStreamInput {

		@JsonProperty("delivery")
		public Object delivery;

		@JsonProperty("events_requested")
		public Set<String> eventsRequested;

		@JsonProperty("description")
		public String description;

		@JsonProperty("aud")
		public Object audience;

	}

	public static class CreateStreamOutput extends StreamConfiguration {
	}

	public static class StreamConfiguration {

		@JsonProperty("stream_id")
		public String streamId;

		@JsonProperty("iss")
		public String issuer;

		@JsonProperty("aud")
		public String audience;

		@JsonProperty("description")
		public String description;

		@JsonProperty("delivery")
		public Map<String, Object> delivery;

		@JsonProperty("events_supported")
		public Set<String> eventsSupported;

		@JsonProperty("events_delivered")
		public Set<String> eventsDelivered;

		@JsonProperty("events_requested")
		public Set<String> eventsRequested;

		public Map<String, Object> properties = new HashMap<>();

		@JsonAnySetter
		public void setProperty(String key, Object value) {
			properties.put(key, value);
		}
	}
}
