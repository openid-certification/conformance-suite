/**
 * Mock data matching the proposed GET /api/plan/schema/{planName} response.
 * This is the Phase 1 API contract -- the backend must produce this shape.
 */
export const MOCK_SCHEMA = {
  planName: "oidcc-basic-certification-test-plan",
  schema: {
    type: "object",
    properties: {
      server: {
        type: "object",
        properties: {
          issuer: {
            type: "string",
            format: "uri",
            title: "Issuer URL",
            description: "The OpenID Provider's issuer identifier",
          },
          jwks_uri: {
            type: "string",
            format: "uri",
            title: "JWKS URI",
            description: "URL of the OP's JWK Set document",
          },
          authorization_endpoint: {
            type: "string",
            format: "uri",
            title: "Authorization Endpoint",
            description: "URL of the OP's authorization endpoint (auto-discovered from issuer if not set)",
          },
        },
        required: ["issuer"],
      },
      client: {
        type: "object",
        properties: {
          client_id: {
            type: "string",
            title: "Client ID",
            description: "OAuth 2.0 client identifier registered with the OP",
          },
          client_secret: {
            type: "string",
            format: "password",
            title: "Client Secret",
            description: "OAuth 2.0 client secret",
          },
          jwks: {
            type: "object",
            format: "json",
            title: "Client JWKS",
            description: "JSON Web Key Set for the client (for private_key_jwt authentication)",
          },
        },
        required: ["client_id"],
      },
    },
  },
  uiSchema: {
    sections: [
      { key: "server", title: "Server Configuration" },
      { key: "client", title: "Client Configuration" },
    ],
  },
};

/**
 * Mock validation response matching POST /api/plan/validate.
 */
export const MOCK_VALIDATION_SUCCESS = {
  valid: true,
  errors: [],
};

export const MOCK_VALIDATION_ERRORS = {
  valid: false,
  errors: [
    { field: "server.issuer", message: "Required field", type: "required" },
    { field: "client.jwks", message: "Invalid JSON: unexpected token", type: "format" },
  ],
};
