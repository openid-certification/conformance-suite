package net.openid.conformance.oauth.statuslists;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class JwtStatusListTokenClaimsBuilder_UnitTest {

	private static final String URI = "https://issuer.example.com/statuslists/1";

	private static final Instant IAT = Instant.ofEpochSecond(1_700_000_000);

	private static final Instant EXP = IAT.plusSeconds(600);

	@Test
	public void testBuild_producesTheClaimsOfAStatusListToken() {
		String encoded = EvenOddStatusListContents.create().encodeStatusList();

		JsonObject claims = JwtStatusListTokenClaimsBuilder.build(URI, IAT, EXP, 300,
			EvenOddStatusListContents.BITS, encoded, null);

		assertEquals(URI, OIDFJSON.getString(claims.get("sub")));
		assertEquals(IAT.getEpochSecond(), OIDFJSON.getLong(claims.get("iat")));
		assertEquals(EXP.getEpochSecond(), OIDFJSON.getLong(claims.get("exp")));
		assertEquals(300, OIDFJSON.getLong(claims.get("ttl")));

		JsonObject statusList = claims.getAsJsonObject("status_list");
		assertEquals(EvenOddStatusListContents.BITS, OIDFJSON.getInt(statusList.get("bits")));
		assertEquals(encoded, OIDFJSON.getString(statusList.get("lst")));
		assertFalse(statusList.has("aggregation_uri"));
	}

	@Test
	public void testBuild_includesTheAggregationUriWhenGiven() {
		JsonObject claims = JwtStatusListTokenClaimsBuilder.build(URI, IAT, EXP, 300,
			EvenOddStatusListContents.BITS, "lst", "https://issuer.example.com/statuslists");

		assertEquals("https://issuer.example.com/statuslists",
			OIDFJSON.getString(claims.getAsJsonObject("status_list").get("aggregation_uri")));
	}
}
