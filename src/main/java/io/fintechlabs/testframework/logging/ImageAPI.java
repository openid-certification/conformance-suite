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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mongodb.BasicDBObjectBuilder;

/**
 * @author jricher
 *
 */
@Controller
public class ImageAPI {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	TestInfoService testInfoService;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@PostMapping(path = "/log/{id}/imgfile")
	public ResponseEntity<Object> uploadImageToNewLogEntry(@RequestBody String encoded,
			@PathVariable(name="id") String testId) throws IOException {
		ImmutableMap<String,String> testOwner = testInfoService.getTestOwner(testId);

		// Should this be checked? I.E. does a non-user facing client ever call this?
		if(authenticationFacade.isAdmin() ||
				authenticationFacade.getPrincipal().equals(testOwner)) {
			// create a new entry in the database
			BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
					.add("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32))
					.add("testId", testId)
					.add("testOwner", testOwner)
					.add("src", "_image-api")
					.add("time", new Date().getTime())
					.add("img", encoded);

			mongoTemplate.insert(documentBuilder.get(), DBEventLog.COLLECTION);
		}
		
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
	
	@PostMapping(path = "/log/{id}/imgfile/{placeholder}")
	public ResponseEntity<Object> uploadImageToExitingLogEntry(@RequestBody String encoded,
			@PathVariable(name="id") String testId,
			@PathVariable(name="placeholder") String placeholder) throws IOException {

		List<Criteria> criterias = new ArrayList<Criteria>();

		// add the placeholder condition
		criterias.add(Criteria.where("upload").is(placeholder));

		// if we're not admin, make sure we also own the log
		if (authenticationFacade.getAuthenticationToken() != null &&
				!authenticationFacade.isAdmin()) {
			criterias.add(Criteria.where("testOwner").is(authenticationFacade.getPrincipal()));
		}

		Criteria criteria = Criteria.where("testId").is(testId).andOperator(
				criterias.toArray(new Criteria[criterias.size()]));

		// find the existing entity
		//Criteria criteria = Criteria.where("testId").is(testId).andOperator(
		//		Criteria.where("upload").is(placeholder));

		//if (authenticationFacade.getAuthenticationToken() != null &&
		//		!authenticationFacade.isAdmin()) {
		//	criteria.andOperator(Criteria.where("testOwner").is(authenticationFacade.getPrincipal()));
		//}

		Query query = Query.query(criteria);
		
		Update update = new Update();
		update.unset("upload");
		update.set("img", encoded);

		mongoTemplate.updateFirst(query, update, DBEventLog.COLLECTION);
		
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
	
	
}
