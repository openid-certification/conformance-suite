package net.openid.conformance.condition.client;

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
public class EnsureIncomingRequestContentTypeIsFormUrlEncoded_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureIncomingRequestContentTypeIsFormUrlEncoded cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureIncomingRequestContentTypeIsFormUrlEncoded();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		env.putString("incoming_request", "headers.content-type","application/x-www-form-urlencoded");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noErrorWithCharset() {
		// I'm not sure we actually want to tolerate the charset header, but Spring seems to be automatically including it
		// which causes failures in the OID4VP RP-against-OP tests if we don't accept it
		env.putString("incoming_request", "headers.content-type","application/x-www-form-urlencodED; charset=UTF-8");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalidCharset() {
		assertThrows(ConditionError.class, () -> {
			env.putString("incoming_request", "headers.content-type","application/json; charset=UTF-8");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_noContentType() {
		assertThrows(ConditionError.class, () -> {
			env.putString("incoming_request", "headers.foo","application/json; charset=UTF-8");
			cond.execute(env);
		});
	}

}
