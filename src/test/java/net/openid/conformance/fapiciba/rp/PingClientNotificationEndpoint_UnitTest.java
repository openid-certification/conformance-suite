package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PingClientNotificationEndpoint_UnitTest {

	@Mock
	private RestTemplate restTemplate;

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private TestablePingClientNotificationEndpoint cond;

	@BeforeEach
	public void setUp() {
		cond = new TestablePingClientNotificationEndpoint(restTemplate);
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

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
}
