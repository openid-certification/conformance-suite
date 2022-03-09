package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.ekyc.condition.client.ValidateElectronicRecordsSupportedInServerConfiguration;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValidateElectronicRecordsSupportedInServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateElectronicRecordsSupportedInServerConfiguration cond;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateElectronicRecordsSupportedInServerConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"electronic_record\""
			+ "],"
			+ "\"electronic_records_supported\": [ \"foo\" ]"
			+ "}");
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_empty () {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"electronic_record\""
			+ "],"
			+ "\"electronic_records_supported\": []"
			+ "}");
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missing () {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"electronic_record\""
			+ "]}");
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notAnArray() {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"electronic_record\""
			+ "],"
			+ "\"electronic_records_supported\": true"
			+ "}");
		cond.execute(env);
	}
	@Test(expected = ConditionError.class)
	public void testEvaluate_notString() {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"electronic_record\""
			+ "],"
			+ "\"electronic_records_supported\": [ false ]"
			+ "}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_notInEvidenceSupported() {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"foo\""
			+ "],"
			+ "\"electronic_records_supported\": [ \"foo\" ]"
			+ "}");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_notInEvidenceSupportedAndMissing() {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"foo\""
			+ "]"
			+ "}");
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notInEvidenceSupportedNotAString() {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"foo\""
			+ "],"
			+ "\"electronic_records_supported\": [ false ]"
			+ "}");
		cond.execute(env);
	}
	@Test(expected = ConditionError.class)
	public void testEvaluate_notInEvidenceSupportedNotAnArray() {
		env.putObjectFromJsonString("server", "{"
			+ "\"evidence_supported\": ["
			+ "\"foo\""
			+ "],"
			+ "\"electronic_records_supported\": true"
			+ "}");
		cond.execute(env);
	}

}
