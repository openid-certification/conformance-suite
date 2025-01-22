package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateEntityMetadataClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateEntityStatementMetadata validateEntityStatementMetadataCond;
	private ValidateFederationEntityMetadata validateFederationEntityMetadataCond;

	@BeforeEach
	public void setUp() throws Exception {
		validateEntityStatementMetadataCond = new ValidateEntityStatementMetadata();
		validateEntityStatementMetadataCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		validateFederationEntityMetadataCond = new ValidateFederationEntityMetadata();
		validateFederationEntityMetadataCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void test_evaluate_happy_path() throws Exception {
		String entityStatementBody = IOUtils.resourceToString("federation/entity-statement-authlete-1.json", StandardCharsets.UTF_8, getClass().getClassLoader());
		JsonObject federation_response_jwt = new JsonObject();
		federation_response_jwt.add("claims", JsonParser.parseString(entityStatementBody));
		env.putObject("federation_response_jwt", federation_response_jwt);
		validateEntityStatementMetadataCond.execute(env);
	}

	@Test
	public void test_with_invalid_entry_in_metadata_claim() throws Exception {
		String entityStatementBody = IOUtils.resourceToString("federation/entity-statement-invalid-entity-1.json", StandardCharsets.UTF_8, getClass().getClassLoader());
		JsonObject federation_response_jwt = new JsonObject();
		federation_response_jwt.add("claims", JsonParser.parseString(entityStatementBody));
		env.putObject("federation_response_jwt", federation_response_jwt);

		ConditionError exception = assertThrows(ConditionError.class, () -> validateEntityStatementMetadataCond.execute(env));

		assertEquals("ValidateEntityStatementMetadata: The metadata claim contains non-standard entity types", exception.getMessage());
	}

	@Test
	public void test_with_invalid_entry_in_federation_entity_claim() throws Exception {
		String entityStatementBody = IOUtils.resourceToString("federation/entity-statement-invalid-federation-entity-1.json", StandardCharsets.UTF_8, getClass().getClassLoader());
		JsonObject federation_response_jwt = new JsonObject();
		federation_response_jwt.add("claims", JsonParser.parseString(entityStatementBody));
		env.putObject("federation_response_jwt", federation_response_jwt);

		ConditionError exception = assertThrows(ConditionError.class, () -> validateFederationEntityMetadataCond.execute(env));

		assertEquals("ValidateFederationEntityMetadata: This URL MUST use the https scheme and MAY contain port, path, and query parameter components encoded in application/x-www-form-urlencoded format; it MUST NOT contain a fragment component.", exception.getMessage());
	}

}
