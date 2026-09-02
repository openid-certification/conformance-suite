package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AddInvalidClientIdPrefixToMultiSignedClientIds_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddInvalidClientIdPrefixToMultiSignedClientIds cond;

	@BeforeEach
	public void setUp() {
		cond = new AddInvalidClientIdPrefixToMultiSignedClientIds();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_replacesPrefixOnBothClientIds() {
		env.putString("client_id", "x509_hash:abc123");
		env.putString("client2_id", "x509_hash:def456");

		cond.execute(env);

		assertThat(env.getString("client_id")).isEqualTo("invalid_scheme:abc123");
		assertThat(env.getString("client2_id")).isEqualTo("invalid_scheme:def456");
	}

	@Test
	public void testEvaluate_clientIdWithoutPrefixGetsInvalidPrefix() {
		env.putString("client_id", "plain-client");
		env.putString("client2_id", "pre_registered:second");

		cond.execute(env);

		assertThat(env.getString("client_id")).isEqualTo("invalid_scheme:plain-client");
		assertThat(env.getString("client2_id")).isEqualTo("invalid_scheme:second");
	}

	@Test
	public void testEvaluate_missingClient2Id() {
		env.putString("client_id", "x509_hash:abc123");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
