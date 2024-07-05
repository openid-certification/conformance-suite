package net.openid.conformance.openid.federation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsInUserinfoAgainstOPMetadata;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(MockitoJUnitRunner.class)
public class ValidateEntityStatementMetadataClaim_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateEntityStatementMetadataClaim cond;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateEntityStatementMetadataClaim();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void test_evaluate_happy_path() throws Exception {
		String entityStatementBody = IOUtils.resourceToString("federation/entity-statement-1.json", StandardCharsets.UTF_8, getClass().getClassLoader());
		env.putObjectFromJsonString("entity_statement_body", entityStatementBody);
		cond.execute(env);
	}

	@Test
	public void test_with_invalid_entry_in_metadata_claim() throws Exception {
		String entityStatementBody = IOUtils.resourceToString("federation/entity-statement-invalid-1.json", StandardCharsets.UTF_8, getClass().getClassLoader());
		env.putObjectFromJsonString("entity_statement_body", entityStatementBody);

		ConditionError exception = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertEquals("ValidateEntityStatementMetadataClaim: The metadata claim contains invalid entity types", exception.getMessage());
	}
}
