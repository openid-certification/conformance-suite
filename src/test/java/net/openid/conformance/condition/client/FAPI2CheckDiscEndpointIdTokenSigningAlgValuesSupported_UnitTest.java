package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addServerValues(String... algs) {
		JsonArray array = new JsonArray();
		for (String alg : algs) {
			array.add(alg);
		}
		JsonObject server = new JsonObject();
		server.add("id_token_signing_alg_values_supported", array);
		env.putObject("server", server);
	}

	@Test
	public void allowedAlgsContainsExactlyTheFourFapi2Identifiers() {
		assertEquals(Set.of("PS256", "ES256", "EdDSA", "Ed25519"),
			Set.copyOf(List.of(FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported.FAPI2_ALLOWED_ALGS)));
	}

	@Test
	public void onlyEd25519AdvertisedPasses() {
		addServerValues("RS256", "Ed25519");
		cond.execute(env);
	}

	@Test
	public void onlyEdDSAAdvertisedStillPasses() {
		addServerValues("RS256", "EdDSA");
		cond.execute(env);
	}

	@Test
	public void neitherEdDSANorEd25519NorOtherAllowedAlgFails() {
		assertThrows(ConditionError.class, () -> {
			addServerValues("RS256", "HS256");
			cond.execute(env);
		});
	}
}
