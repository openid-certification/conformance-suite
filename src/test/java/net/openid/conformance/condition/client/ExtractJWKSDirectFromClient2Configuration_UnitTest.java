package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExtractJWKSDirectFromClient2Configuration_UnitTest {

	@Test
	public void identifiesTheSecondClientConfigurationFieldWhenJwksIsMissing() {
		ExtractJWKSDirectFromClient2Configuration condition = new ExtractJWKSDirectFromClient2Configuration();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.FAILURE);
		Environment env = new Environment();
		env.putObjectFromJsonString("config", "{\"client2\":{}}");

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'jwks' field is missing from the 'Second client' section in the test configuration");
	}
}
