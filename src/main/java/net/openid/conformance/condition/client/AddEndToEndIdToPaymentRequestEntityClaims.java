package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class AddEndToEndIdToPaymentRequestEntityClaims extends AbstractCondition {
	public static final String PROXY_E2EID_ISPB = "00000000";
	static private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");


	@Override
	@PreEnvironment(required = "resource_request_entity_claims")
	@PostEnvironment(required = "resource_request_entity_claims")
	public Environment evaluate(Environment env) {
		OffsetDateTime currentDateTime = OffsetDateTime.now(ZoneOffset.UTC);
		String formattedCurrentDateTime = currentDateTime.format(formatter);
		String randomString = RandomStringUtils.randomAlphanumeric(11);
		String endToEndId = String.format("E%s%s%s", PROXY_E2EID_ISPB, formattedCurrentDateTime, randomString);

		env.putString("resource_request_entity_claims", "data.endToEndId", endToEndId);

		JsonObject o = env.getObject("resource_request_entity_claims");

		logSuccess("Added endToEndId to payment request", args("resource_request_entity_claims", o));

		return env;
	}

}
