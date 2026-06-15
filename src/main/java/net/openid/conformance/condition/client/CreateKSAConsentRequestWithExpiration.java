package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class CreateKSAConsentRequestWithExpiration extends AbstractCreateKSAConsentRequest {

	@Override
	protected void customizeMessageData(JsonObject data) {
		Instant baseDateRough = Instant.now();
		Instant baseDate = baseDateRough.minusNanos(baseDateRough.getNano());

		DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC"));
		data.addProperty("ExpirationDateTime", fmt.format(baseDate.plus(2, ChronoUnit.HOURS)));
		data.addProperty("TransactionFromDateTime", fmt.format(baseDate.minus(30, ChronoUnit.DAYS)));
		data.addProperty("TransactionToDateTime", fmt.format(baseDate));
	}
}
