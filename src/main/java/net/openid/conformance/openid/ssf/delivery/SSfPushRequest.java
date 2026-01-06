package net.openid.conformance.openid.ssf.delivery;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;

public record SSfPushRequest(
	String id,
	String path,
	Instant receivedAt,
	HttpServletRequest req,
	HttpServletResponse res,
	JsonObject requestParts
) {
}
