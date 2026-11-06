package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class PoppyTestData {

	private PoppyTestData() {
	}

	static final String POPPY_JSON = """
		{
		  "protocol_version": "0.4",
		  "organization": {"name": "Example Company", "domain": "example.com", "logo_uri": "https://example.com/logo.png"},
		  "auth": {
		    "scopes": {"orders:read": "See your orders"},
		    "scope_hints": {"orders:read": {"changes": "none", "personal_data": true}}
		  },
		  "agent": {"protocols": [{"type": "poppy", "endpoint": "https://api.example.com/poppy/conversations"}]},
		  "web": {"browser_session_endpoint": "https://example.com/poppy/browser-session"},
		  "feedback": {"endpoint": "https://api.example.com/poppy/feedback"},
		  "excluded_tasks": {"url": "https://example.com/poppy/excluded-tasks.json"},
		  "apis": [
		    {"type": "openapi", "url": "https://api.example.com/openapi.json", "description": "Orders"},
		    {"type": "mcp", "url": "https://mcp.example.com/mcp", "description": "Search"}
		  ]
		}
		""";

	static final String AS_METADATA = """
		{
		  "issuer": "https://auth.example.com",
		  "token_endpoint": "https://auth.example.com/oauth/token",
		  "revocation_endpoint": "https://auth.example.com/oauth/revoke",
		  "authorization_endpoint": "https://auth.example.com/oauth/authorize",
		  "device_authorization_endpoint": "https://auth.example.com/oauth/device",
		  "grant_types_supported": ["refresh_token", "authorization_code", "urn:ietf:params:oauth:grant-type:device_code"],
		  "scopes_supported": ["orders:read", "poppy:browser-sign-in"],
		  "protected_resources": ["https://example.com", "https://example.co.uk", "https://mcp.example.com/mcp"]
		}
		""";

	static final String CLIENT_METADATA = """
		{
		  "client_id": "https://agent.example/agent.json",
		  "client_name": "Example Agent",
		  "logo_uri": "https://agent.example/logo.png",
		  "jwks_uri": "https://agent.example/jwks.json",
		  "redirect_uris": ["https://agent.example/oauth/callback"],
		  "token_endpoint_auth_method": "private_key_jwt"
		}
		""";

	static final String PUBLIC_JWKS = """
		{"keys": [{"kty": "EC", "use": "sig", "crv": "P-256", "kid": "k1",
		  "x": "W7GYo8l5peUH8trIGV3JoJFe_5xoJ9j37oYNLPR0GGw", "y": "qO8C9AFSbtdIoKoFuOQ_pBBcPv05iL_ys1JiYf1GT-c", "alg": "ES256"}]}
		""";

	static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}
}
