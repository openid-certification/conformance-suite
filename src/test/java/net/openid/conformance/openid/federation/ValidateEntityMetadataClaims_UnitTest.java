package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(MockitoJUnitRunner.class)
public class ValidateEntityMetadataClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateEntityStatementMetadataClaim validateEntityStatementMetadataClaimCond;
	private ValidateFederationEntityMetadata validateFederationEntityMetadataCond;

	@Before
	public void setUp() throws Exception {
		validateEntityStatementMetadataClaimCond = new ValidateEntityStatementMetadataClaim();
		validateEntityStatementMetadataClaimCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		validateFederationEntityMetadataCond = new ValidateFederationEntityMetadata();
		validateFederationEntityMetadataCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void test_evaluate_happy_path() throws Exception {
		String entityStatementBody = IOUtils.resourceToString("federation/entity-statement-authlete-1.json", StandardCharsets.UTF_8, getClass().getClassLoader());
		env.putObjectFromJsonString("entity_statement_body", entityStatementBody);
		validateEntityStatementMetadataClaimCond.execute(env);
	}

	@Test
	public void test_with_invalid_entry_in_metadata_claim() throws Exception {
		String entityStatementBody = IOUtils.resourceToString("federation/entity-statement-invalid-entity-1.json", StandardCharsets.UTF_8, getClass().getClassLoader());
		env.putObjectFromJsonString("entity_statement_body", entityStatementBody);

		ConditionError exception = assertThrows(ConditionError.class, () -> validateEntityStatementMetadataClaimCond.execute(env));

		assertEquals("ValidateEntityStatementMetadataClaim: The metadata claim contains invalid entity types", exception.getMessage());
	}

	@Test
	public void test_with_invalid_entry_in_federation_entity_claim() throws Exception {
		String entityStatementBody = IOUtils.resourceToString("federation/entity-statement-invalid-federation-entity-1.json", StandardCharsets.UTF_8, getClass().getClassLoader());
		env.putObjectFromJsonString("entity_statement_body", entityStatementBody);

		ConditionError exception = assertThrows(ConditionError.class, () -> validateFederationEntityMetadataCond.execute(env));

		assertEquals("ValidateFederationEntityMetadata: This URL MUST use the https scheme and MAY contain port, path, and query parameter components encoded in application/x-www-form-urlencoded format; it MUST NOT contain a fragment component.", exception.getMessage());
	}

}
