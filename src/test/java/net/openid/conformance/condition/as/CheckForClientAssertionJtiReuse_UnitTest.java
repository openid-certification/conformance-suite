package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckForClientAssertionJtiReuse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForClientAssertionJtiReuse cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckForClientAssertionJtiReuse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putClientAssertionWithJti(String jti) {
		JsonObject claims = JsonParser.parseString("{\"jti\":\"" + jti + "\"}").getAsJsonObject();
		JsonObject clientAssertion = new JsonObject();
		clientAssertion.add("claims", claims);
		env.putObject("client_assertion", clientAssertion);
	}

	@Test
	public void firstUsePasses() {
		putClientAssertionWithJti("jti-aaaa");

		cond.execute(env);
	}

	@Test
	public void secondUseOfSameJtiFails() {
		putClientAssertionWithJti("jti-aaaa");
		cond.execute(env);

		// Same jti, second time
		putClientAssertionWithJti("jti-aaaa");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void differentJtisAllPass() {
		putClientAssertionWithJti("jti-aaaa");
		cond.execute(env);

		putClientAssertionWithJti("jti-bbbb");
		cond.execute(env);

		putClientAssertionWithJti("jti-cccc");
		cond.execute(env);
	}

	@Test
	public void interleavedReuseIsCaught() {
		putClientAssertionWithJti("jti-aaaa");
		cond.execute(env);

		putClientAssertionWithJti("jti-bbbb");
		cond.execute(env);

		// jti-aaaa returns — must be caught even though jti-bbbb came in between
		putClientAssertionWithJti("jti-aaaa");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingClientAssertionFails() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingJtiClaimFails() {
		JsonObject clientAssertion = new JsonObject();
		clientAssertion.add("claims", new JsonObject());
		env.putObject("client_assertion", clientAssertion);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
