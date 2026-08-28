package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class KsaValidateRequestObjectExp_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private KsaValidateRequestObjectExp cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new KsaValidateRequestObjectExp();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putRequestObject(Long nbf, Long exp) {
		JsonObject claims = new JsonObject();
		if (nbf != null) {
			claims.addProperty("nbf", nbf);
		}
		if (exp != null) {
			claims.addProperty("exp", exp);
		}
		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);
	}

	@Test
	public void testEvaluate_isGood() {
		long now = Instant.now().getEpochSecond();
		putRequestObject(now, now + 300);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_exactlyTenMinutesIsAccepted() {
		long now = Instant.now().getEpochSecond();
		putRequestObject(now, now + 600);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_moreThanTenMinutesAfterNbf() {
		assertThrows(ConditionError.class, () -> {
			long now = Instant.now().getEpochSecond();
			putRequestObject(now, now + 601);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_expired() {
		assertThrows(ConditionError.class, () -> {
			long now = Instant.now().getEpochSecond();
			putRequestObject(now - 900, now - 400);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingExp() {
		assertThrows(ConditionError.class, () -> {
			long now = Instant.now().getEpochSecond();
			putRequestObject(now, null);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingNbf() {
		assertThrows(ConditionError.class, () -> {
			long now = Instant.now().getEpochSecond();
			putRequestObject(null, now + 300);

			cond.execute(env);
		});
	}
}
