package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.CreatePaymentRequestEntityClaimsFromQrdnConfig;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class CreatePaymentRequestEntityClaimsFromQrdnConfigTests {

	@Test
	public void worksCorrectly() throws IOException {

		String rawJson = IOUtils.resourceToString("qrdn_consent_config.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject qrdnConsentConfig = new JsonParser().parse(rawJson).getAsJsonObject();
		JsonObject resourceConfig = new JsonObject();
		resourceConfig.add("brazilQrdnPaymentConsent", qrdnConsentConfig);
		resourceConfig.addProperty("brazilQrdnCnpj", "60545350000165");
		resourceConfig.addProperty("brazilQrdnRemittance", "Pagamento da nota RSTO035-002.");
		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);

		Environment environment = new Environment();
		environment.putObject("resource", resourceConfig);
		environment.putObject("config", config);

		CreatePaymentRequestEntityClaimsFromQrdnConfig condition = new CreatePaymentRequestEntityClaimsFromQrdnConfig();

		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.execute(environment);

		JsonObject paymentClaims = environment.getObject("resource_request_entity_claims");

		String rawPaymentJson = IOUtils.resourceToString("qrdn_payment.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject expectedPayment = new JsonParser().parse(rawPaymentJson).getAsJsonObject();

		assertEquals(expectedPayment, paymentClaims);

		JsonObject obj = paymentClaims.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");

		assertNull(obj.get("ibgeTownCode"));

	}

	@Test
	public void ibgeTownIsCopiedIfPresent() throws IOException {

		String rawJson = IOUtils.resourceToString("qrdn_consent_config_ibge.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject qrdnConsentConfig = new JsonParser().parse(rawJson).getAsJsonObject();

		JsonObject resourceConfig = new JsonObject();
		resourceConfig.add("brazilQrdnPaymentConsent", qrdnConsentConfig);
		resourceConfig.addProperty("brazilQrdnCnpj", "60545350000165");
		resourceConfig.addProperty("brazilQrdnRemittance", "Pagamento da nota RSTO035-002.");
		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);

		Environment environment = new Environment();
		environment.putObject("resource", resourceConfig);
		environment.putObject("config", config);

		CreatePaymentRequestEntityClaimsFromQrdnConfig condition = new CreatePaymentRequestEntityClaimsFromQrdnConfig();

		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.execute(environment);

		JsonObject paymentClaims = environment.getObject("resource_request_entity_claims");
        JsonObject obj = paymentClaims.getAsJsonObject("data");

        String igbeTown = OIDFJSON.getString(obj.get("ibgeTownCode"));

        assertEquals("5300108", igbeTown);

	}

	@Test
	public void failsGracefully() throws IOException {

		String rawJson = IOUtils.resourceToString("qrdn_consent_config_incomplete.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject qrdnConsentConfig = new JsonParser().parse(rawJson).getAsJsonObject();

		JsonObject resourceConfig = new JsonObject();
		resourceConfig.add("brazilQrdnPaymentConsent", qrdnConsentConfig);
		resourceConfig.addProperty("brazilQrdnCnpj", "60545350000165");
		resourceConfig.addProperty("brazilQrdnRemittance", "Pagamento da nota RSTO035-002.");
		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);

		Environment environment = new Environment();
		environment.putObject("resource", resourceConfig);
		environment.putObject("config", config);

		CreatePaymentRequestEntityClaimsFromQrdnConfig condition = new CreatePaymentRequestEntityClaimsFromQrdnConfig();

		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.execute(environment);
			fail("Should have thrown an error");
		} catch(ConditionError e) {
			assertEquals(e.getMessage(), "CreatePaymentRequestEntityClaimsFromQrdnConfig: Unable to find object in JSON");
		}

	}

	@Test
	public void failsGracefullyIfConfigMissing() throws IOException {

		JsonObject qrdnConsentConfig = new JsonObject();

		JsonObject resourceConfig = new JsonObject();
		resourceConfig.add("brazilQrdnPaymentConsent", qrdnConsentConfig);
		resourceConfig.addProperty("brazilQrdnCnpj", "60545350000165");
		resourceConfig.addProperty("brazilQrdnRemittance", "Pagamento da nota RSTO035-002.");
		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);

		Environment environment = new Environment();
		environment.putObject("resource", resourceConfig);
		environment.putObject("config", config);

		CreatePaymentRequestEntityClaimsFromQrdnConfig condition = new CreatePaymentRequestEntityClaimsFromQrdnConfig();

		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.execute(environment);
			fail("Should have thrown an error");
		} catch(ConditionError e) {
			assertEquals(e.getMessage(), "CreatePaymentRequestEntityClaimsFromQrdnConfig: Unable to find object in JSON");
		}

	}

}
