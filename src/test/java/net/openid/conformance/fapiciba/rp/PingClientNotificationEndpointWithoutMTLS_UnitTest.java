package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

public class PingClientNotificationEndpointWithoutMTLS_UnitTest {

	@Test
	public void doesNotLoadMtlsCredentials() {
		Environment env = new Environment();
		env.putObjectFromJsonString("mutual_tls_authentication", "{\"cert\":\"invalid\",\"key\":\"invalid\"}");
		TestableCondition condition = new TestableCondition();
		condition.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), ConditionResult.INFO);
		assertThatCode(() -> condition.createRestTemplate(env, true)).doesNotThrowAnyException();
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
