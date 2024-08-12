package net.openid.conformance.condition.rs;

import de.slub.urn.URN;
import de.slub.urn.URNSyntaxError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ConsentId_UnitTest {

	@Test
	public void test_consentIdStartsWithOidfConformanceUrn() {
		var envIn = new Environment();
		envIn.putString("fapi_interaction_id", "04ddbb1b-19f6-4300-9072-775a9bd704c4");

		var consentResponse = new FAPIBrazilGenerateNewConsentResponse();
		consentResponse.setProperties(null, mock(TestInstanceEventLog.class), null);

		Environment envOut = consentResponse.evaluate(envIn);
		String consentId = envOut.getString("consent_id");

		assertTrue(consentId.startsWith("urn:conformance:oidf:"));
	}

	@Test
	public void test_paymentsConsentIdStartsWithOidfConformanceUrn() {
		var envIn = new Environment();
		envIn.putString("fapi_interaction_id", "04ddbb1b-19f6-4300-9072-775a9bd704c4");

		var consentResponse = new FAPIBrazilGenerateNewPaymentsConsentResponse();
		consentResponse.setProperties(null, mock(TestInstanceEventLog.class), null);

		Environment envOut = consentResponse.evaluate(envIn);
		String consentId = envOut.getString("consent_id");

		assertTrue(consentId.startsWith("urn:conformance:oidf:"));
	}

	@Test
	public void test_consentIdHasExpectedLength() {
		var envIn = new Environment();
		envIn.putString("fapi_interaction_id", "04ddbb1b-19f6-4300-9072-775a9bd704c4");

		var consentResponse = new FAPIBrazilGenerateNewPaymentsConsentResponse();
		consentResponse.setProperties(null, mock(TestInstanceEventLog.class), null);

		Environment envOut = consentResponse.evaluate(envIn);
		String consentId = envOut.getString("consent_id");

		assertEquals(31, consentId.length());
	}

	@Test
	public void test_consentIdCompliesWithRFC8141() throws URNSyntaxError {
		var envIn = new Environment();
		envIn.putString("fapi_interaction_id", "04ddbb1b-19f6-4300-9072-775a9bd704c4");

		var consentResponse = new FAPIBrazilGenerateNewConsentResponse();
		consentResponse.setProperties(null, mock(TestInstanceEventLog.class), null);

		Environment envOut = consentResponse.evaluate(envIn);
		String consentId = envOut.getString("consent_id");

		URN.rfc8141().parse(consentId);
	}

	@Test
	public void test_paymentsConsentIdCompliesWithRFC8141() throws URNSyntaxError {
		var envIn = new Environment();
		envIn.putString("fapi_interaction_id", "04ddbb1b-19f6-4300-9072-775a9bd704c4");

		var consentResponse = new FAPIBrazilGenerateNewPaymentsConsentResponse();
		consentResponse.setProperties(null, mock(TestInstanceEventLog.class), null);

		Environment envOut = consentResponse.evaluate(envIn);
		String consentId = envOut.getString("consent_id");

		URN.rfc8141().parse(consentId);
	}
}
