/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.logging;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import io.fintechlabs.testframework.info.DBTestInfoService;
import io.fintechlabs.testframework.security.AuthenticationFacade;

/**
 * @author jricher
 *
 */
@Controller
public class LogApi {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@GetMapping(value = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DBObject>> getAllTests() {

		DBObject queryFilter;
		if (authenticationFacade.isAdmin()) {
			queryFilter = BasicDBObjectBuilder.start().get();
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			queryFilter = BasicDBObjectBuilder.start().add("testOwner", owner).get();
		}

		@SuppressWarnings("unchecked")
		List<String> testIds = mongoTemplate.getCollection(DBEventLog.COLLECTION).distinct("testId", queryFilter);

		List<DBObject> results = new ArrayList<>(testIds.size());

		for (String testId : testIds) {
			// fetch the test object from the info log if available
			DBObject testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(testId);

			if (testInfo == null) {
				// make a fake document with just the ID
				results.add(BasicDBObjectBuilder.start("_id", testId).get());
			} else {
				// otherwise, add everything
				results.add(testInfo);
			}
		}

		return new ResponseEntity<>(results, HttpStatus.OK);

	}

	@GetMapping(value = "/log/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DBObject>> getTestInfo(@PathVariable("id") String id, @RequestParam(name = "dl", defaultValue = "false") boolean dl) {
		BasicDBObjectBuilder queryBuilder = BasicDBObjectBuilder.start().add("testId", id);
		if (!authenticationFacade.isAdmin()) {
			queryBuilder = queryBuilder.add("testOwner", authenticationFacade.getPrincipal());
		}

		List<DBObject> results = mongoTemplate.getCollection(DBEventLog.COLLECTION).find(queryBuilder.get())
			.sort(BasicDBObjectBuilder.start()
				.add("time", 1)
				.get())
			.toArray();

		HttpHeaders headers = new HttpHeaders();

		if (dl) {
			// TODO: come up with a better filename
			headers.add("Content-Disposition", "attachment; filename=\"test-log-" + id + ".json\"");
		}

		return ResponseEntity.ok().headers(headers).body(results);

	}

}
