package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PingClientNotificationEndpoint_UnitTest {

	@Mock
	private RestTemplate restTemplate;

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private TestablePingClientNotificationEndpoint cond;
	private TestablePingClientNotificationEndpointWithRetriesForBrazil retryCond;

	@BeforeEach
	public void setUp() {
		cond = new TestablePingClientNotificationEndpoint(restTemplate);
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		retryCond = new TestablePingClientNotificationEndpointWithRetriesForBrazil(restTemplate);
		retryCond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putString("auth_req_id", "auth-req-id");
		env.putString("client_notification_token", "notification-token");
		env.putObjectFromJsonString("client", """
			{
				"backchannel_client_notification_endpoint": "https://rp.example.com/ciba-notification"
			}
			""");
	}

	@Test
	public void recordsPingBeforeCallingClientNotificationEndpoint() {
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenAnswer(invocation -> {
				assertThat(env.getBoolean("client_was_pinged")).isTrue();
				return new ResponseEntity<>("{}", HttpStatus.NO_CONTENT);
			});

		cond.execute(env);

		assertThat(env.getInteger("client_notification_endpoint_response_http_status")).isEqualTo(HttpStatus.NO_CONTENT.value());
		assertThat(env.getBoolean("client_was_pinged")).isTrue();
	}

	@Test
	public void omitsAuthorizationHeaderWhenSendingUnauthenticatedNotification() {
		TestablePingClientNotificationEndpointWithoutBearerToken condition =
			new TestablePingClientNotificationEndpointWithoutBearerToken(restTemplate);
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenReturn(new ResponseEntity<>("", HttpStatus.NO_CONTENT));

		condition.execute(env);

		verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), argThat(request -> {
			assertThat(request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
			assertThat(request.getBody()).isEqualTo("{\"auth_req_id\":\"auth-req-id\"}");
			return true;
		}), eq(String.class));
	}

	@Test
	public void sendsValidBearerTokenWithWrongAuthReqId() {
		TestablePingClientNotificationEndpointWithWrongAuthReqId condition =
			new TestablePingClientNotificationEndpointWithWrongAuthReqId(restTemplate);
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenReturn(new ResponseEntity<>("", HttpStatus.NO_CONTENT));

		condition.execute(env);

		verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), argThat(request -> {
			assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
				.isEqualTo("Bearer notification-token");
			assertThat(request.getBody()).isEqualTo("{\"auth_req_id\":\"auth-req-id-invalid\"}");
			return true;
		}), eq(String.class));
	}

	@Test
	public void recordsExpectedClientErrorForInvalidNotification() {
		TestablePingClientNotificationEndpointWithoutBearerToken condition =
			new TestablePingClientNotificationEndpointWithoutBearerToken(restTemplate);
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

		condition.execute(env);

		assertThat(env.getInteger("client_notification_endpoint_response_http_status"))
			.isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	public void rejectsUnexpectedServerErrorForInvalidNotification() {
		TestablePingClientNotificationEndpointWithoutBearerToken condition =
			new TestablePingClientNotificationEndpointWithoutBearerToken(restTemplate);
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void retriesTransientServerErrorForBrazil() {
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))
			.thenReturn(new ResponseEntity<>("", HttpStatus.NO_CONTENT));

		retryCond.execute(env);

		assertThat(env.getInteger("client_notification_endpoint_response_http_status"))
			.isEqualTo(HttpStatus.NO_CONTENT.value());
		verify(restTemplate, times(2))
			.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {"REQUEST_TIMEOUT", "TOO_MANY_REQUESTS"})
	public void retriesTransientClientErrorForBrazil(HttpStatus transientStatus) {
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenThrow(new HttpClientErrorException(transientStatus))
			.thenReturn(new ResponseEntity<>("", HttpStatus.NO_CONTENT));

		retryCond.execute(env);

		assertThat(env.getInteger("client_notification_endpoint_response_http_status"))
			.isEqualTo(HttpStatus.NO_CONTENT.value());
		verify(restTemplate, times(2))
			.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
	}

	@Test
	public void retriesTemporaryCommunicationFailureForBrazil() {
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenThrow(new ResourceAccessException("Connection reset"))
			.thenReturn(new ResponseEntity<>("", HttpStatus.NO_CONTENT));

		retryCond.execute(env);

		assertThat(env.getInteger("client_notification_endpoint_response_http_status"))
			.isEqualTo(HttpStatus.NO_CONTENT.value());
		verify(restTemplate, times(2))
			.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
	}

	@Test
	public void doesNotRetryPermanentClientErrorForBrazil() {
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(() -> retryCond.execute(env)).isInstanceOf(ConditionError.class);
		verify(restTemplate)
			.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
	}

	@Test
	public void stopsAfterBoundedTransientRetriesForBrazil() {
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

		assertThatThrownBy(() -> retryCond.execute(env)).isInstanceOf(ConditionError.class);
		verify(restTemplate, times(3))
			.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
	}

	@Test
	public void genericProfileDoesNotGainBrazilRetryPolicy() {
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
			.thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
		verify(restTemplate)
			.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
	}

	private static class TestablePingClientNotificationEndpoint extends PingClientNotificationEndpoint {
		private final RestTemplate restTemplate;

		private TestablePingClientNotificationEndpoint(RestTemplate restTemplate) {
			this.restTemplate = restTemplate;
		}

		@Override
		protected RestTemplate createRestTemplate(Environment env, boolean restrictAllowedTLSVersions)
			throws UnrecoverableKeyException, KeyManagementException, CertificateException, InvalidKeySpecException,
			NoSuchAlgorithmException, KeyStoreException, IOException {
			return restTemplate;
		}
	}

	private static class TestablePingClientNotificationEndpointWithRetriesForBrazil
		extends PingClientNotificationEndpointWithRetriesForBrazil {
		private final RestTemplate restTemplate;

		private TestablePingClientNotificationEndpointWithRetriesForBrazil(RestTemplate restTemplate) {
			this.restTemplate = restTemplate;
		}

		@Override
		protected RestTemplate createRestTemplate(Environment env, boolean restrictAllowedTLSVersions)
			throws UnrecoverableKeyException, KeyManagementException, CertificateException, InvalidKeySpecException,
			NoSuchAlgorithmException, KeyStoreException, IOException {
			return restTemplate;
		}
	}

	private static class TestablePingClientNotificationEndpointWithoutBearerToken
		extends PingClientNotificationEndpointWithoutBearerToken {
		private final RestTemplate restTemplate;

		private TestablePingClientNotificationEndpointWithoutBearerToken(RestTemplate restTemplate) {
			this.restTemplate = restTemplate;
		}

		@Override
		protected RestTemplate createRestTemplate(Environment env, boolean restrictAllowedTLSVersions)
			throws UnrecoverableKeyException, KeyManagementException, CertificateException, InvalidKeySpecException,
			NoSuchAlgorithmException, KeyStoreException, IOException {
			return restTemplate;
		}
	}

	private static class TestablePingClientNotificationEndpointWithWrongAuthReqId
		extends PingClientNotificationEndpointWithWrongAuthReqId {
		private final RestTemplate restTemplate;

		private TestablePingClientNotificationEndpointWithWrongAuthReqId(RestTemplate restTemplate) {
			this.restTemplate = restTemplate;
		}

		@Override
		protected RestTemplate createRestTemplate(Environment env, boolean restrictAllowedTLSVersions)
			throws UnrecoverableKeyException, KeyManagementException, CertificateException, InvalidKeySpecException,
			NoSuchAlgorithmException, KeyStoreException, IOException {
			return restTemplate;
		}
	}
}
