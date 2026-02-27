package net.openid.conformance.vci10issuer.condition;

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
public class VCICheckExpClaimInCredential_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VCICheckExpClaimInCredential cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new VCICheckExpClaimInCredential();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addCredential(long exp) {
		JsonObject claims = new JsonObject();
		claims.addProperty("exp", exp);
		JsonObject credential = new JsonObject();
		credential.add("claims", claims);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
	}

	@Test
	public void testExpReasonable() {
		addCredential(Instant.now().getEpochSecond() + 3600);
		cond.execute(env);
	}

	@Test
	public void testExpMillisAsSeconds() {
		assertThrows(ConditionError.class, () -> {
			addCredential(Instant.now().getEpochSecond() * 1000);
			cond.execute(env);
		});
	}

	@Test
	public void testNoExp() {
		JsonObject claims = new JsonObject();
		JsonObject credential = new JsonObject();
		credential.add("claims", claims);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
		cond.execute(env);
	}
}
