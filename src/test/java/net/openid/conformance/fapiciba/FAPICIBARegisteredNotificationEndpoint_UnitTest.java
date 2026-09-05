package net.openid.conformance.fapiciba;

import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.testmodule.TestSkippedException;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class FAPICIBARegisteredNotificationEndpoint_UnitTest {

	@Test
	public void skipsUnusableRegistrationWithoutCallingItAProtocolViolation() {
		AbstractFAPICIBAID1 module = module("https://other.example/notify");
		assertThatThrownBy(module::onConfigure).isInstanceOf(TestSkippedException.class)
			.hasMessageContaining("https://other.example/notify")
			.hasMessageContaining("https://suite.example/test-mtls/example/ciba-notification-endpoint");
		assertThat(module.getResult()).isEqualTo(TestModule.Result.SKIPPED);
	}

	@Test
	public void retainsRegistrationUsingTheSuiteNotificationEndpoint() {
		AbstractFAPICIBAID1 module = module("https://suite.example/test-mtls/example/ciba-notification-endpoint");
		assertThatCode(module::onConfigure).doesNotThrowAnyException();
	}

	@Test
	public void staticRegistrationDoesNotRequireNotificationMetadataInSavedConfig() {
		AbstractFAPICIBAID1 module = module(null);
		module.setVariant(Map.of(ClientRegistration.class, ClientRegistration.STATIC_CLIENT));
		assertThatCode(module::onConfigure).doesNotThrowAnyException();
	}

	private static AbstractFAPICIBAID1 module(String registeredEndpoint) {
		AbstractFAPICIBAID1 module = new FAPICIBAID1EnsureOtherScopeOrderSucceeds();
		module.setProperties("UNIT-TEST", Map.of(), BsonEncoding.testInstanceEventLog(), null,
			mock(TestInfoService.class), null, null);
		module.setupOpenBankingBrazil();
		module.setupPrivateKeyJwt();
		module.testType = CIBAMode.PING;
		module.setVariant(Map.of(ClientRegistration.class, ClientRegistration.DYNAMIC_CLIENT,
			ClientAuthType.class, ClientAuthType.PRIVATE_KEY_JWT));
		module.getEnv().putString("notification_uri", "https://suite.example/test-mtls/example/ciba-notification-endpoint");
		module.getEnv().putObjectFromJsonString("client", "{\"token_endpoint_auth_method\":\"private_key_jwt\"}");
		if (registeredEndpoint != null) {
			module.getEnv().putString("client", "backchannel_client_notification_endpoint", registeredEndpoint);
		}
		return module;
	}
}
