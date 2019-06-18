package io.fintechlabs.testframework.info;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/api")
public class TestInfoApi {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private TestInfoService testInfoService;

	@GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Document>> getAllTests() {
		List<Document> testInfo = null;
		if (authenticationFacade.isAdmin()) {
			testInfo = Lists.newArrayList(mongoTemplate.getCollection(DBTestInfoService.COLLECTION).find());
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			if (owner != null) {
				testInfo = Lists.newArrayList(mongoTemplate.getCollection(DBTestInfoService.COLLECTION).find(new Document("owner", owner)));
			}
		}
		return new ResponseEntity<>(testInfo, HttpStatus.OK);

	}

	@GetMapping(value = "/info/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getTestInfo(@PathVariable("id") String id) {
		Document testInfo = null;
		if (authenticationFacade.isAdmin()) {
			testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).find(new Document("_id", id)).first();
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			if (owner != null) {
				testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).find(new Document("_id", id).append("owner", owner)).first();
			}
		}
		if (testInfo == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(testInfo, HttpStatus.OK);
		}

	}

	@PostMapping(value = "/info/{id}/publish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> publishTestInfo(@PathVariable("id") String id, @RequestBody JsonObject config) {

		String publish = null;
		if (config.has("publish") && config.get("publish").isJsonPrimitive()) {
			publish = Strings.emptyToNull(OIDFJSON.getString(config.get("publish")));
		} else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (!testInfoService.publishTest(id, publish)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		map.put("publish", publish);

		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@GetMapping(value = "/public/info/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getPublicTestInfo(@PathVariable("id") String id) {

		Query query = new Query();
		query.fields()
			.include("_id")
			.include("testId")
			.include("testName")
			.include("started")
			.include("description")
			.include("alias")
			.include("owner")
			.include("planId")
			.include("status")
			.include("version")
			.include("summary")
			.include("publish")
			.include("result");

		Document testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION)
				.find(new Document("_id", id))
				.projection(query.getFieldsObject())
				.first();

		if (testInfo == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(testInfo, HttpStatus.OK);
		}

	}

}
