package net.openid.conformance.security;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import java.lang.reflect.Type;
import java.util.Date;

public class MongoDBRegisteredClientService implements ClientRegistrationRepository {

	public static final String COLLECTION = "OIDC_REGISTERED_CLIENTS";
	private final String gitlabIss;


	@Autowired
	private MongoTemplate mongoTemplate;

	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory.getLogger(MongoDBRegisteredClientService.class);

	// This is from the JsonFileRegisteredClientService
	private Gson gson = new GsonBuilder()
		.registerTypeAdapter(ClientRegistration.class, new JsonSerializer<ClientRegistration>() {
			@Override
			public JsonElement serialize(ClientRegistration src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject obj = new JsonObject();

				obj.addProperty("client_id", src.getClientId());
				obj.addProperty("client_secret", src.getClientSecret());
				obj.addProperty("registration_id", src.getRegistrationId());

				return obj;
			}
		})
		.registerTypeAdapter(ClientRegistration.class, new JsonDeserializer<ClientRegistration>() {
			@Override
			public ClientRegistration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				JsonObject obj = (JsonObject) json;
				String registrationId = obj.get("registration_id").getAsString();
				return ClientRegistration.withRegistrationId(registrationId)
					.clientId(obj.get("client_id").getAsString())
					.clientSecret(obj.get("client_secret").getAsString())
					.build();
			}
		})
		.setPrettyPrinting()
		.create();

	ClientRegistration googleClientRegistration, gitlabClientRegistration;

	public MongoDBRegisteredClientService(String googleClientId,
										  String googleClientSecret,
										  String redirectURI,
										  String gitlabClientId,
										  String gitlabClientSecret,
										  String gitlabIss,
										  String clientName) {
		this.gitlabIss = gitlabIss;
		googleClientRegistration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
			.clientId(googleClientId)
			.clientSecret(googleClientSecret)
			.scope("openid", "email", "profile")
			.redirectUri(redirectURI)
			.build();

		gitlabClientRegistration =
			ClientRegistrations.fromIssuerLocation(gitlabIss)
				.registrationId(gitlabIss)
				.clientId(gitlabClientId)
				.clientSecret(gitlabClientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(redirectURI)
				.scope("openid", "email", "profile")
				.userNameAttributeName(IdTokenClaimNames.SUB)
				.clientName(clientName)
				.build();
	}

	public void save(String issuer, ClientRegistration client) {
		Document document = new Document()
			.append("_id", issuer)
			.append("client_json", gson.toJson(client))
			.append("time", new Date().getTime());

		mongoTemplate.insert(document, COLLECTION);
	}

	@Override
	public ClientRegistration findByRegistrationId(String issuer) {

		if (gitlabIss.equals(issuer)) {
			return gitlabClientRegistration;
		}
		if ("https://accounts.google.com".equals(issuer)) {
			return googleClientRegistration;
		}

		Document dbObject = mongoTemplate.findById(issuer, Document.class, COLLECTION);

		if (dbObject != null && dbObject.containsKey("client_json")) {
			logger.info("Found client, attempting to deserialize");
			ClientRegistration client = gson.fromJson((String) dbObject.get("client_json"), new TypeToken<ClientRegistration>() {
			}.getType());
			logger.info("Returning client: " + client.toString());
			return client;
		} else {
			logger.info("No client found");
			return null;
		}
	}
}
