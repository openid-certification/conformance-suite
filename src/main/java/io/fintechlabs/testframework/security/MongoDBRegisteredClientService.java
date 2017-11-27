package io.fintechlabs.testframework.security;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.ClientDetailsEntityJsonProcessor;
import org.mitre.openid.connect.client.service.RegisteredClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.reflect.Type;
import java.util.Date;

public class MongoDBRegisteredClientService implements RegisteredClientService{

    public static final String COLLECTION = "OIDC_REGISTERED_CLIENTS";

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
    * Logger for this class
    */
    private static final Logger logger = LoggerFactory.getLogger(MongoDBRegisteredClientService.class);

    // This is from the JsonFileRegisteredClientService
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(RegisteredClient.class, new JsonSerializer<RegisteredClient>() {
                @Override
                public JsonElement serialize(RegisteredClient src, Type typeOfSrc, JsonSerializationContext context) {
                    return ClientDetailsEntityJsonProcessor.serialize(src);
                }
            })
            .registerTypeAdapter(RegisteredClient.class, new JsonDeserializer<RegisteredClient>() {
                @Override
                public RegisteredClient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    return ClientDetailsEntityJsonProcessor.parseRegistered(json);
                }
            })
            .setPrettyPrinting()
            .create();

    @Override
    public RegisteredClient getByIssuer(String issuer) {
        logger.info("Looking up client for issuer: " + issuer);
        BasicDBObject dbObject = mongoTemplate.findById(issuer, BasicDBObject.class, COLLECTION);
        if (dbObject != null && dbObject.containsField("client_json")) {
            logger.info("Found client, attempting to deserialize");
            RegisteredClient client = gson.fromJson((String) dbObject.get("client_json"), new TypeToken<RegisteredClient>(){}.getType());
            logger.info("Returning client: " + client.toString());
            return client;
        } else {
            logger.info("No client found");
            return null;
        }
    }

    @Override
    public void save(String issuer, RegisteredClient client) {
        BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
                .add("_id", issuer)
                .add("client_json",gson.toJson(client))
                .add("time", new Date().getTime());

        mongoTemplate.insert(documentBuilder.get(), COLLECTION);
    }
}
