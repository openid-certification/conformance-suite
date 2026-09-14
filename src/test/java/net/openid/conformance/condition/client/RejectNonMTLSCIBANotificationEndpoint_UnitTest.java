package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RejectNonMTLSCIBANotificationEndpoint_UnitTest {

	@Test
	public void rejectionIncludesTheUsableNotificationAddress() {
		var condition = new RejectNonMTLSCIBANotificationEndpoint();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.FAILURE);
		Environment env = new Environment();
		env.putString("notification_uri", "https://mtls.example/test-mtls/id/notify");
		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("https://mtls.example/test-mtls/id/notify");
	}

	@Test
	public void missingNotificationAddressIsASetupError() {
		var condition = new RejectNonMTLSCIBANotificationEndpoint();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.FAILURE);
		assertThatThrownBy(() -> condition.execute(new Environment())).isInstanceOf(ConditionError.class)
			.hasMessageContaining("[pre]").hasMessageContaining("notification_uri");
	}
}
