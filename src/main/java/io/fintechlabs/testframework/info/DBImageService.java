package io.fintechlabs.testframework.info;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import io.fintechlabs.testframework.logging.DBEventLog;
import io.fintechlabs.testframework.security.AuthenticationFacade;

/**
 * @author jheenan
 *
 */
@Service
public class DBImageService implements ImageService {

	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	// Create a Criteria with or without the security constraints as needed
	private Criteria createCriteria(Criteria findTestId, Criteria additionalConstraints, boolean assumeAdmin) {
		Criteria criteria = new Criteria();
		if (assumeAdmin || authenticationFacade.isAdmin()) {
			criteria = criteria.andOperator(
				findTestId,
				additionalConstraints
			);
		} else {
			criteria = criteria.andOperator(
				findTestId,
				additionalConstraints,
				Criteria.where("testOwner").is(authenticationFacade.getPrincipal())
			);
		}
		return criteria;
	}

	@Override
	public DBObject fillPlaceholder(String testId, String placeholder, Map<String, Object> update, boolean assumeAdmin) {
		Criteria findTestId = Criteria.where("testId").is(testId);

		// add the placeholder condition
		Criteria placeholderExists = Criteria.where("upload").is(placeholder);

		// if we're not admin, make sure we also own the log
		Criteria criteria = createCriteria(findTestId, placeholderExists, assumeAdmin);

		Query query = Query.query(criteria);

		Update updateCommand = new Update();
		for (Entry<String, Object> field : update.entrySet()) {
			updateCommand.set(field.getKey(), field.getValue());
		}

		updateCommand.unset("upload");

		return mongoTemplate.findAndModify(query, updateCommand, FindAndModifyOptions.options().returnNew(true), DBObject.class, DBEventLog.COLLECTION);
	}

	@Override
	public List<String> getRemainingPlaceholders(String testId, boolean assumeAdmin) {
		Criteria findTestId = Criteria.where("testId").is(testId);

		// check to see if all placeholders are set by searching for any remaining ones on this test
		Criteria noMorePlaceholders = Criteria.where("upload").exists(true);

		Criteria postSearch = createCriteria(findTestId, noMorePlaceholders, assumeAdmin);
		Query search = Query.query(postSearch);

		search.fields().include("upload");

		return mongoTemplate
				.getCollection(DBEventLog.COLLECTION)
				.find(search.getQueryObject(), search.getFieldsObject())
				.toArray().stream()
					.map((obj) -> obj.get("upload").toString())
					.collect(Collectors.toList());
	}

	@Override
	public List<DBObject> getAllImagesForTestId(String testId, boolean assumeAdmin) {
		Criteria findTestId = Criteria.where("testId").is(testId);

		Criteria anyImages =
			new Criteria().orOperator(
				Criteria.where("img").exists(true),
				Criteria.where("upload").exists(true)
			);

		// add in the security parameters
		Criteria criteria = createCriteria(findTestId, anyImages, assumeAdmin);

		Query search = Query.query(criteria);

		return mongoTemplate.getCollection(DBEventLog.COLLECTION).find(search.getQueryObject())
			.sort(BasicDBObjectBuilder.start()
				.add("time", 1)
				.get())
			.toArray();
	}

}
