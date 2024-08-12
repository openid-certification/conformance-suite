package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckForClientCertificate_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject certificate;

	private CheckForClientCertificate cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckForClientCertificate();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		certificate = JsonParser.parseString("{\"subject\":{\"dn\":\"CN=example.org\"}}").getAsJsonObject();

	}

	/**
	 * Test method for {@link CheckForClientCertificate#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("client_certificate", certificate);

		cond.execute(env);

	}

	/**
	 * Test method for {@link CheckForClientCertificate#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);

		});

	}
}
