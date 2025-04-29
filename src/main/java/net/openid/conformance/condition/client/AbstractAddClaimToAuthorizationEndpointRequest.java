package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public abstract class AbstractAddClaimToAuthorizationEndpointRequest extends AbstractCondition {
	private enum ClaimRequestType {
		// We test (if we have enough supported claims!) all the ways a claim can be requested as per
		// https://openid.net/specs/openid-connect-core-1_0.html#IndividualClaimsRequests
		// We don't test value / values as we don't know what values the server may return
		AsNull, AsEmpty, EssentialTrue, Random, EssentialFalse;

		public static final ClaimRequestType[] values = values();

		public ClaimRequestType next() {
			return values[(ordinal() + 1) % values.length];
		}
	}

	/**
	 * Add claims to given claims object, using different forms of request
	 * @param claimsObject Claims object - i.e. id_token or userinfo entry inside 'claims' in request
	 * @param claimsToAdd Names of the claims to request
	 */
	protected void addRequestsForClaims(JsonObject claimsObject, List<String> claimsToAdd) {
		ClaimRequestType requestType = ClaimRequestType.values[0];
		for (String claimName : claimsToAdd) {
			if (requestType == ClaimRequestType.AsNull) {
				claimsObject.add(claimName, JsonNull.INSTANCE);
			} else {
				JsonObject claimBody = new JsonObject();
				switch (requestType) {
					case AsNull:
						// already handled
						break;
					case AsEmpty:
						break;
					case EssentialTrue:
						claimBody.addProperty("essential", true);
						break;
					case Random:
						// "Other members MAY be defined to provide additional information about the requested Claims. Any members used that are not understood MUST be ignored."
						claimBody.addProperty(RandomStringUtils.secure().nextAlphanumeric(10), RandomStringUtils.secure().nextAlphanumeric(10));
						break;
					case EssentialFalse:
						claimBody.addProperty("essential", false);
						break;
				}
				claimsObject.add(claimName, claimBody);
			}
			requestType = requestType.next();
		}
	}

	public enum LocationToRequestClaim {
		ID_TOKEN,
		USERINFO
	}

	public Environment addClaim(Environment env, LocationToRequestClaim locationToRequestClaim, String claim, String value, boolean essential) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		String locationStr;
		switch (locationToRequestClaim) {
			case ID_TOKEN:
				locationStr = "id_token";
				break;
			case USERINFO:
				locationStr = "userinfo";
				break;
			default:
				throw error("Unknown locationToRequestClaim value, this is a bug in the test condition");
		}

		JsonObject claimsIdToken = getClaimsForLocation(authorizationEndpointRequest, locationStr);

		JsonObject claimBody = new JsonObject();
		if (value != null) {
			claimBody.addProperty("value", value);
		}
		claimBody.addProperty("essential", essential);
		claimsIdToken.add(claim, claimBody);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added "+claim+" claim to authorization_endpoint_request", args("authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}

	protected JsonObject getClaimsForLocation(JsonObject authorizationEndpointRequest, String locationStr) {
		JsonObject claims;
		if (authorizationEndpointRequest.has("claims")) {
			JsonElement claimsElement = authorizationEndpointRequest.get("claims");
			if (claimsElement.isJsonObject()) {
				claims = claimsElement.getAsJsonObject();
			} else {
				throw error("Invalid claims entry in authorization_endpoint_request", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
		} else {
			claims = new JsonObject();
			authorizationEndpointRequest.add("claims", claims);
		}

		JsonObject identityClaimsForRequestedLocation;
		if (claims.has(locationStr)) {
			JsonElement idTokenElement = claims.get(locationStr);
			if (idTokenElement.isJsonObject()) {
				identityClaimsForRequestedLocation = idTokenElement.getAsJsonObject();
			} else {
				throw error("Invalid "+ locationStr +" entry in authorization_endpoint_request", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
		} else {
			identityClaimsForRequestedLocation = new JsonObject();
			claims.add(locationStr, identityClaimsForRequestedLocation);
		}
		return identityClaimsForRequestedLocation;
	}

}
