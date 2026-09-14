package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnsureNotificationEndpointRequestHasClientCertificate_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private EnsureNotificationEndpointRequestHasClientCertificate condition;
	private JsonObject notificationCallback;

	@BeforeEach
	public void setUp() {
		condition = new EnsureNotificationEndpointRequestHasClientCertificate();
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		notificationCallback = new JsonObject();
		notificationCallback.add("headers", new JsonObject());
		env.putObject("notification_callback", notificationCallback);
	}

	@Test
	public void acceptsClientCertificateFromMtlsProxy() {
		notificationCallback.getAsJsonObject("headers")
			.addProperty("x-ssl-cert", "-----BEGIN CERTIFICATE----- certificate -----END CERTIFICATE-----");

		assertThatCode(() -> condition.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void rejectsMissingClientCertificateHeader() {
		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void rejectsEmptyClientCertificateHeader() {
		notificationCallback.getAsJsonObject("headers").addProperty("x-ssl-cert", "  ");

		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void rejectsMtlsProxyNullCertificateMarker() {
		notificationCallback.getAsJsonObject("headers").addProperty("x-ssl-cert", "(null)");

		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}
}
