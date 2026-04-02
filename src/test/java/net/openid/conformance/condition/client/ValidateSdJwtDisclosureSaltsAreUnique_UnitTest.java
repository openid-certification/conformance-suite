package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateSdJwtDisclosureSaltsAreUnique_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateSdJwtDisclosureSaltsAreUnique cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateSdJwtDisclosureSaltsAreUnique();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_uniqueSalts() {
		JsonArray disclosures = new JsonArray();
		disclosures.add("[\"salt1\", \"given_name\", \"John\"]");
		disclosures.add("[\"salt2\", \"family_name\", \"Doe\"]");
		disclosures.add("[\"salt3\", \"email\", \"john@example.com\"]");

		JsonObject sdjwt = new JsonObject();
		sdjwt.add("disclosures", disclosures);
		env.putObject("sdjwt", sdjwt);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_singleDisclosure() {
		JsonArray disclosures = new JsonArray();
		disclosures.add("[\"salt1\", \"given_name\", \"John\"]");

		JsonObject sdjwt = new JsonObject();
		sdjwt.add("disclosures", disclosures);
		env.putObject("sdjwt", sdjwt);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noDisclosures() {
		JsonArray disclosures = new JsonArray();

		JsonObject sdjwt = new JsonObject();
		sdjwt.add("disclosures", disclosures);
		env.putObject("sdjwt", sdjwt);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_duplicateSalts() {
		assertThrows(ConditionError.class, () -> {
			JsonArray disclosures = new JsonArray();
			disclosures.add("[\"same_salt\", \"given_name\", \"John\"]");
			disclosures.add("[\"same_salt\", \"family_name\", \"Doe\"]");

			JsonObject sdjwt = new JsonObject();
			sdjwt.add("disclosures", disclosures);
			env.putObject("sdjwt", sdjwt);

			cond.execute(env);
		});
	}
}
