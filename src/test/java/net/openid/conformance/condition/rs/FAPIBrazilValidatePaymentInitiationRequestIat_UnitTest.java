package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FAPIBrazilValidatePaymentInitiationRequestIat_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIBrazilValidatePaymentInitiationRequestIat cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPIBrazilValidatePaymentInitiationRequestIat();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addPaymentRequest(long iat) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iat", iat);
		JsonObject request = new JsonObject();
		request.add("claims", claims);
		env.putObject("payment_initiation_request", request);
	}

	@Test
	public void testIatRecent() {
		addPaymentRequest(Instant.now().getEpochSecond() - 10);
		cond.execute(env);
	}

	@Test
	public void testIatTooOld() {
		assertThrows(ConditionError.class, () -> {
			addPaymentRequest(Instant.now().getEpochSecond() - 2 * 86400);
			cond.execute(env);
		});
	}

	@Test
	public void testIatInFuture() {
		assertThrows(ConditionError.class, () -> {
			addPaymentRequest(Instant.now().getEpochSecond() + 3600);
			cond.execute(env);
		});
	}
}
