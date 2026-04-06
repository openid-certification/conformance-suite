import au.com.auspayplus.digitalid.config.SdkConfig;
import au.com.auspayplus.digitalid.config.CustomConfigBuilder;
import au.com.auspayplus.digitalid.incubating.RelyingPartyCibaClientSdk;
import au.com.auspayplus.digitalid.incubating.ciba.CibaAuthenticationResponse;
import au.com.auspayplus.digitalid.incubating.ciba.CibaTokenResponse;
import au.com.auspayplus.digitalid.incubating.ciba.PollResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class ConnectIdCibaRpClient {
    public static void main(String[] args) throws Exception {
        String issuer = System.getenv("ISSUER");
        String configFilePath = System.getenv("CONFIG_FILE");

        if (issuer == null || configFilePath == null) {
            System.err.println("ISSUER and CONFIG_FILE environment variables are required.");
            System.exit(1);
        }

        String configJson = Files.readString(Paths.get(configFilePath));
        JsonObject config = JsonParser.parseString(configJson).getAsJsonObject();

        JsonObject client = config.getAsJsonObject("client");
        String clientId = client.get("client_id").getAsString();

        JsonObject jwks = client.getAsJsonObject("jwks");
        JsonObject jwk = jwks.getAsJsonArray("keys").get(0).getAsJsonObject();
        String signingKid = jwk.has("kid") ? jwk.get("kid").getAsString() : "fapi-jwt-assertion-20180817-1";

        RSAKey rsaKey = RSAKey.parse(jwk.toString());
        PrivateKey privateKey = rsaKey.toPrivateKey();
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString(privateKey.getEncoded()).replaceAll("(.{64})", "$1\n") +
                "\n-----END PRIVATE KEY-----\n";

        JsonObject mtls = config.getAsJsonObject("mtls");
        String transportPem = mtls.get("cert").getAsString();
        String transportKey = mtls.get("key").getAsString();
        String caPem = transportPem; // Self-signed dev environment

        HttpServer server = HttpServer.create(new InetSocketAddress(18080), 0);
        server.createContext("/participants", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "[\n" +
                        "  {\n" +
                        "    \"OrganisationId\": \"test-org\",\n" +
                        "    \"Status\": \"Active\",\n" +
                        "    \"AuthorisationServers\": [\n" +
                        "      {\n" +
                        "        \"AuthorisationServerId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                        "        \"Issuer\": \"" + issuer + "\",\n" +
                        "        \"OpenIDDiscoveryDocument\": \"" + issuer + ".well-known/openid-configuration\",\n" +
                        "        \"PayloadSigningCertLocationUri\": \"" + issuer + "jwks\",\n" +
                        "        \"CustomerFriendlyName\": \"Test OP\",\n" +
                        "        \"AuthorisationServerCertifications\": [\n" +
                        "          {\n" +
                        "            \"ProfileVariant\": \"FAPI2 Adv. OP w/Private Key, PAR\",\n" +
                        "            \"ProfileType\": \"Redirect\",\n" +
                        "            \"Status\": \"Certified\",\n" +
                        "            \"CertificationStartDate\": \"01/01/2020\",\n" +
                        "            \"CertificationExpirationDate\": \"01/01/2050\"\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "]";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });
        server.start();

        System.out.println("Started mock participants server on port 18080");

        try {
            SdkConfig sdkConfig = new SdkConfig(
                    signingKid,
                    transportKey,
                    transportPem,
                    privateKeyPem,
                    transportPem,
                    caPem,
                    URI.create("https://tpp.localhost/cb"),
                    URI.create("http://localhost:18080/participants"),
                    URI.create(clientId),
                    new CustomConfigBuilder().includeUncertifiedParticipants(true).build()
            );

            RelyingPartyCibaClientSdk rpClient = new RelyingPartyCibaClientSdk(sdkConfig);

            UUID authServerId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            String loginHint = "test-user";
            String purpose = "verifying your identity";

            System.out.println("Sending CIBA authentication request...");
            CibaAuthenticationResponse authResponse = rpClient.sendCibaAuthenticationRequest(
                    authServerId,
                    loginHint,
                    purpose,
                    Set.of("given_name", "txn"),
                    Collections.emptySet(),
                    Collections.emptyList(),
                    300
            );

            System.out.println("CIBA Authentication successful. AuthReqId: " + authResponse.getAuthReqId());
            System.out.println("Polling for tokens...");

            int interval = authResponse.getInterval() > 0 ? authResponse.getInterval() : 5;
            long deadline = System.currentTimeMillis() + 60000; // 60 seconds timeout

            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(interval * 1000L);
                PollResult<CibaTokenResponse> result = rpClient.retrieveCibaTokens(authServerId, authResponse.getAuthReqId(), interval);

                if (result instanceof PollResult.Success) {
                    System.out.println("Tokens retrieved successfully!");
                    System.exit(0);
                } else if (result instanceof PollResult.SlowDown) {
                    interval = ((PollResult.SlowDown<CibaTokenResponse>) result).newInterval();
                    System.out.println("Slow down received. New interval: " + interval);
                } else if (result instanceof PollResult.InProgress) {
                    System.out.println("Authorization in progress...");
                } else if (result instanceof PollResult.Declined) {
                    System.err.println("Authorization declined.");
                    System.exit(1);
                } else {
                    System.out.println("Unhandled poll result: " + result.getClass());
                }
            }

            System.err.println("Timeout waiting for tokens.");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            server.stop(0);
        }
    }
}