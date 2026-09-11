package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnsureRegisteredCIBANotificationEndpointMatches_UnitTest {
	private static final String NOTIFICATION_URI =
		"https://suite.example/test-mtls/example/ciba-notification-endpoint";
	private final Environment env = new Environment();
	private final EnsureRegisteredCIBANotificationEndpointMatches condition =
		new EnsureRegisteredCIBANotificationEndpointMatches();

	@BeforeEach
	public void setUp() {
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.FAILURE);
		env.putString("notification_uri", NOTIFICATION_URI);
		env.putObject("client", new JsonObject());
	}

	@Test
	public void acceptsTheServedNotificationUri() {
		env.putString("client", "backchannel_client_notification_endpoint", NOTIFICATION_URI);
		assertThatCode(() -> condition.execute(env)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"https://other.example/notify",
		"https://suite.example/test/example/ciba-notification-endpoint",
		"https://suite.example:8444/test-mtls/example/ciba-notification-endpoint",
		"https://suite.example/test-mtls/example/ciba-notification-endpoint/",
		"https://suite.example/test-mtls/example/ciba-notification-endpoint?changed=true",
		"", " "
	})
	public void rejectsAChangedNotificationUri(String registeredUri) {
		env.putString("client", "backchannel_client_notification_endpoint", registeredUri);
		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("notification endpoint");
	}

	@Test
	public void rejectsAMissingNotificationUri() {
		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "null", "true", "17", "{}", "[]" })
	public void rejectsANonStringNotificationUri(String json) {
		env.getObject("client").add("backchannel_client_notification_endpoint", JsonParser.parseString(json));
		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void rejectsAChangedNotificationUriForTheMappedSecondClient() {
		env.putString("client", "backchannel_client_notification_endpoint", NOTIFICATION_URI);
		JsonObject secondClient = new JsonObject();
		secondClient.addProperty("backchannel_client_notification_endpoint", "https://other.example/notify");
		env.putObject("client2", secondClient);
		env.mapKey("client", "client2");
		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void acceptsTheServedNotificationUriForTheMappedSecondClient() {
		env.putString("client", "backchannel_client_notification_endpoint", "https://other.example/notify");
		JsonObject secondClient = new JsonObject();
		secondClient.addProperty("backchannel_client_notification_endpoint", NOTIFICATION_URI);
		env.putObject("client2", secondClient);
		env.mapKey("client", "client2");
		assertThatCode(() -> condition.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void rejectsAnEmptyServedUriEvenIfTheRegisteredValueIsEmpty() {
		env.putString("notification_uri", "");
		env.putString("client", "backchannel_client_notification_endpoint", "");
		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}
}
