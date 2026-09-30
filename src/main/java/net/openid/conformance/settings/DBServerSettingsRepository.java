package net.openid.conformance.settings;

import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * The only class that reads or writes the server settings collection. Documents are stored as
 * given, secrets included, in plaintext.
 */
@Service
public class DBServerSettingsRepository implements ServerSettingsRepository {

	public static final String COLLECTION = "SERVER_SETTINGS";

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public Optional<Document> find(String sectionId) {
		return Optional.ofNullable(mongoTemplate.getCollection(COLLECTION)
			.find(new Document("_id", sectionId))
			.first());
	}

	@Override
	public void save(String sectionId, Document document) {
		Document stored = new Document(document).append("_id", sectionId);
		mongoTemplate.getCollection(COLLECTION)
			.replaceOne(new Document("_id", sectionId), stored, new ReplaceOptions().upsert(true));
	}
}
