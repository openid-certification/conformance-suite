package net.openid.conformance.ekyc.condition.client;

import net.openid.conformance.condition.Condition;
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
public class ValidateElectronicRecordsSupportedInServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateElectronicRecordsSupportedInServerConfiguration cond;

	@BeforeEach
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

	@Test
	public void testEvaluate_empty () {
		assertThrows(ConditionError.class, () -> {
			env.putObjectFromJsonString("server", "{"
				+ "\"evidence_supported\": ["
				+ "\"electronic_record\""
				+ "],"
				+ "\"electronic_records_supported\": []"
				+ "}");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missing () {
		assertThrows(ConditionError.class, () -> {
			env.putObjectFromJsonString("server", "{"
				+ "\"evidence_supported\": ["
				+ "\"electronic_record\""
				+ "]}");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_notAnArray() {
		assertThrows(ConditionError.class, () -> {
			env.putObjectFromJsonString("server", "{"
				+ "\"evidence_supported\": ["
				+ "\"electronic_record\""
				+ "],"
				+ "\"electronic_records_supported\": true"
				+ "}");
			cond.execute(env);
		});
	}
	@Test
	public void testEvaluate_notString() {
		assertThrows(ConditionError.class, () -> {
			env.putObjectFromJsonString("server", "{"
				+ "\"evidence_supported\": ["
				+ "\"electronic_record\""
				+ "],"
				+ "\"electronic_records_supported\": [ false ]"
				+ "}");
			cond.execute(env);
		});
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

	@Test
	public void testEvaluate_notInEvidenceSupportedNotAString() {
		assertThrows(ConditionError.class, () -> {
			env.putObjectFromJsonString("server", "{"
				+ "\"evidence_supported\": ["
				+ "\"foo\""
				+ "],"
				+ "\"electronic_records_supported\": [ false ]"
				+ "}");
			cond.execute(env);
		});
	}
	@Test
	public void testEvaluate_notInEvidenceSupportedNotAnArray() {
		assertThrows(ConditionError.class, () -> {
			env.putObjectFromJsonString("server", "{"
				+ "\"evidence_supported\": ["
				+ "\"foo\""
				+ "],"
				+ "\"electronic_records_supported\": true"
				+ "}");
			cond.execute(env);
		});
	}

}
