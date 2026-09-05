package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class PingClientNotificationEndpointWithoutMTLS_UnitTest {

	@Test
	public void buildsUnauthenticatedClientWithoutReadingOrRemovingSharedCredentials() throws Exception {
		Environment env = new Environment();
		// These cannot be loaded into a KeyManager. Building the negative client must not try.
		env.putObjectFromJsonString("mutual_tls_authentication", "{\"cert\":\"invalid\",\"key\":\"invalid\"}");
		var credentials = env.getObject("mutual_tls_authentication");
		TestableCondition condition = new TestableCondition();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), ConditionResult.INFO);
		RestTemplate client = condition.createRestTemplate(env, true);
		MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
		server.expect(request -> assertThat(env.getObject("mutual_tls_authentication")).isSameAs(credentials))
			.andRespond(withStatus(HttpStatus.NO_CONTENT));

		client.postForEntity("https://client.example/notify", "{}", String.class);

		server.verify();
		assertThat(env.getObject("mutual_tls_authentication")).isSameAs(credentials);
	}

	private static class TestableCondition extends PingClientNotificationEndpointWithoutMTLS {
		@Override
		public RestTemplate createRestTemplate(Environment env, boolean restrictAllowedTLSVersions)
			throws java.security.UnrecoverableKeyException, java.security.KeyManagementException,
			java.security.cert.CertificateException, java.security.spec.InvalidKeySpecException,
			java.security.NoSuchAlgorithmException, java.security.KeyStoreException, java.io.IOException {
			return super.createRestTemplate(env, restrictAllowedTLSVersions);
		}
	}
}
