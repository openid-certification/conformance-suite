package net.openid.conformance.fapiciba;

import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.testmodule.TestSkippedException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class FAPICIBARegisteredAuthenticationApplicability_UnitTest {

	@ParameterizedTest
	@MethodSource("assertionModules")
	public void skipsAssertionTestWhenRegisteredClientUsesMtls(AbstractFAPICIBAID1 module) {
		configure(module, "tls_client_auth");
		assertThatThrownBy(module::onConfigure).isInstanceOf(TestSkippedException.class)
			.hasMessageContaining("registered client authentication method");
		assertThat(module.getResult()).isEqualTo(TestModule.Result.SKIPPED);
	}

	@ParameterizedTest
	@MethodSource("assertionModules")
	public void retainsAssertionTestForRegisteredPrivateKeyJwt(AbstractFAPICIBAID1 module) {
		configure(module, "private_key_jwt");
		assertThatCode(module::onConfigure).doesNotThrowAnyException();
	}

	private static Stream<AbstractFAPICIBAID1> assertionModules() {
		return Stream.of(
			new FAPICIBAID1EnsureClientAssertionWithIssAudToTokenEndpointSucceeds(),
			new FAPICIBAID1EnsureClientAssertionSignatureAlgorithmInTokenEndpointRequestIsRS256Fails(),
			new FAPICIBAID1EnsureClientAssertionSignatureAlgorithmInBackchannelAuthorizationRequestIsRS256Fails(),
			new FAPICIBAID1EnsureWithoutClientAssertionInTokenEndpointFails(),
			new FAPICIBAID1EnsureWithoutClientAssertionInBackchannelAuthorizationRequestFails());
	}

	private static void configure(AbstractFAPICIBAID1 module, String method) {
		module.setProperties("UNIT-TEST", Map.of(), BsonEncoding.testInstanceEventLog(), null,
			mock(TestInfoService.class), null, null);
		module.setupOpenBankingBrazil();
		module.setupPrivateKeyJwt();
		module.getEnv().putObjectFromJsonString("client", "{}");
		module.getEnv().putString("client", "token_endpoint_auth_method", method);
		module.getEnv().putObjectFromJsonString("client_jwks", "{\"keys\":[{\"alg\":\"PS256\"}]}");
	}
}
