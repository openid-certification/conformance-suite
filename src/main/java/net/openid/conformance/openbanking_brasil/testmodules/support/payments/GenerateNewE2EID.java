package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class GenerateNewE2EID extends AbstractCondition {

	static private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

	@Override
	public Environment evaluate(Environment env) {
		log("Generating new endToEndId for the payment request object");
		OffsetDateTime currentDateTime = OffsetDateTime.now(ZoneOffset.UTC);
		String formattedCurrentDateTime = currentDateTime.format(FORMATTER);
		String randomString = RandomStringUtils.randomAlphanumeric(11);
		String endToEndId = String.format(
			"E%s%s%s",
			DictHomologKeys.PROXY_E2EID_ISPB,
			formattedCurrentDateTime,
			randomString
		);

		JsonObject resource = env.getObject("resource");
		JsonObject paymentRequest = resource.getAsJsonObject("brazilPixPayment");
		paymentRequest.getAsJsonObject("data").addProperty("endToEndId", endToEndId);

		env.putString("endToEndId", endToEndId);

		logSuccess("Successfully generated a new endToEndId", paymentRequest);

		return env;
	}
}
