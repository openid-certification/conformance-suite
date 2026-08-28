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
public class KsaValidateRequestObjectNbf_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private KsaValidateRequestObjectNbf cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new KsaValidateRequestObjectNbf();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("client", new JsonObject());
	}

	private void putNbf(Long nbf) {
		JsonObject claims = new JsonObject();
		if (nbf != null) {
			claims.addProperty("nbf", nbf);
		}
		JsonObject requestObject = new JsonObject();
		requestObject.add("claims", claims);
		env.putObject("authorization_request_object", requestObject);
	}

	@Test
	public void testEvaluate_isGood() {
		putNbf(Instant.now().getEpochSecond() - 60);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_justUnderTenMinutesInThePastIsAccepted() {
		putNbf(Instant.now().getEpochSecond() - 590);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_moreThanTenMinutesInThePast() {
		assertThrows(ConditionError.class, () -> {
			putNbf(Instant.now().getEpochSecond() - 900);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_tooFarInTheFuture() {
		assertThrows(ConditionError.class, () -> {
			putNbf(Instant.now().getEpochSecond() + 900);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingNbf() {
		assertThrows(ConditionError.class, () -> {
			putNbf(null);

			cond.execute(env);
		});
	}
}
