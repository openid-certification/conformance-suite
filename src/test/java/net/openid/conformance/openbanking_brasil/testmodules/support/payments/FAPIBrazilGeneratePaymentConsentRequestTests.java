package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JsonObjectBuilder;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class FAPIBrazilGeneratePaymentConsentRequestTests {

	@Test
	public void constructConsentFromMinimalConfig() throws IOException, JSONException {

		JsonObject config = new JsonObjectBuilder()
			.addField("brazilPaymentConsent.data.loggedUser.document.identification", "76109277673")
			.addField("brazilPaymentConsent.data.loggedUser.document.rel", "CPF")
			.addField("brazilPaymentConsent.data.debtorAccount.ispb", "12345678")
			.addField("brazilPaymentConsent.data.debtorAccount.issuer", "6272")
			.addField("brazilPaymentConsent.data.debtorAccount.number", "94088392")
			.addField("brazilPaymentConsent.data.debtorAccount.accountType", "CACC")
			.addField( "brazilPaymentConsent.data.payment.amount", "100.00")
			.build();
		Environment environment = new Environment();
		environment.putObject("resource", config);

		FAPIBrazilGeneratePaymentConsentRequest condition = new FAPIBrazilGeneratePaymentConsentRequest();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		condition.evaluate(environment);

		JsonObject consent = environment.getObject("consent_endpoint_request");

		String expectedJson = IOUtils.resourceToString("payment_consent_request.json", Charset.defaultCharset(), getClass().getClassLoader());

		JSONAssert.assertEquals(expectedJson, consent.toString(), new CustomComparator(
			JSONCompareMode.LENIENT,
			new Customization("data.payment.date", (o1, o2) -> true)
		));

	}

	@Test
	public void constructConsentWithExtraneousConfig() throws IOException, JSONException {

		JsonObject config = new JsonObjectBuilder()
			.addField("brazilPaymentConsent.data.loggedUser.document.identification", "76109277673")
			.addField("brazilPaymentConsent.data.loggedUser.document.rel", "CPF")
			.addField("brazilPaymentConsent.data.debtorAccount.ispb", "12345678")
			.addField("brazilPaymentConsent.data.debtorAccount.issuer", "6272")
			.addField("brazilPaymentConsent.data.debtorAccount.number", "94088392")
			.addField("brazilPaymentConsent.data.debtorAccount.accountType", "CACC")
			.addField( "brazilPaymentConsent.data.payment.amount", "100.00")
			.addField( "brazilPaymentConsent.data.payment.fridge", "running")
			.build();
		Environment environment = new Environment();
		environment.putObject("resource", config);

		FAPIBrazilGeneratePaymentConsentRequest condition = new FAPIBrazilGeneratePaymentConsentRequest();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		condition.evaluate(environment);

		JsonObject consent = environment.getObject("consent_endpoint_request");

		String expectedJson = IOUtils.resourceToString("payment_consent_request.json", Charset.defaultCharset(), getClass().getClassLoader());

		JSONAssert.assertEquals(expectedJson, consent.toString(), new CustomComparator(
			JSONCompareMode.LENIENT,
			new Customization("data.payment.date", (o1, o2) -> true)
		));

	}

	@Test
	public void failsIfMandatoryConfigMissing() throws IOException, JSONException {

		JsonObject config = new JsonObjectBuilder()
			.addField("brazilPaymentConsent.data.loggedUser.document.rel", "CPF")
			.addField("brazilPaymentConsent.data.debtorAccount.ispb", "12345678")
			.addField("brazilPaymentConsent.data.debtorAccount.issuer", "6272")
			.addField("brazilPaymentConsent.data.debtorAccount.number", "94088392")
			.addField("brazilPaymentConsent.data.debtorAccount.accountType", "CACC")
			.addField( "brazilPaymentConsent.data.payment.amount", "100.00")
			.build();
		Environment environment = new Environment();
		environment.putObject("resource", config);

		FAPIBrazilGeneratePaymentConsentRequest condition = new FAPIBrazilGeneratePaymentConsentRequest();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.evaluate(environment);
			fail("Should not pass");
		} catch (ConditionError ce) {
			assertEquals("FAPIBrazilGeneratePaymentConsentRequest: Unable to find element resource in config at brazilPaymentConsent.data.loggedUser.document.identification", ce.getMessage());
		}

	}

}
