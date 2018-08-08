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

package io.fintechlabs.testframework.info;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import io.fintechlabs.testframework.logging.DBEventLog;
import io.fintechlabs.testframework.runner.TestRunnerSupport;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.TestModule.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

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

	@Autowired
	private TestInfoService testInfoService;

	@Autowired
	private TestRunnerSupport testRunnerSupport;

	// Create a Criteria with or without the security constraints as needed
	private Criteria createCriteria(Criteria findTestId, Criteria additionalConstraints) {
		Criteria criteria = new Criteria();
		if (!authenticationFacade.isAdmin()) {
			criteria = criteria.andOperator(
				findTestId,
				additionalConstraints,
				Criteria.where("testOwner").is(authenticationFacade.getPrincipal())
			);
		} else {
			criteria = criteria.andOperator(
				findTestId,
				additionalConstraints
			);
		}
		return criteria;
	}

	@Override
	public DBObject fillPlaceholder(String testId, String placeholder, Update update) {
		Criteria findTestId = Criteria.where("testId").is(testId);

		// add the placeholder condition
		Criteria placeholderExists = Criteria.where("upload").is(placeholder);

		// if we're not admin, make sure we also own the log
		Criteria criteria = createCriteria(findTestId, placeholderExists);

		Query query = Query.query(criteria);

		update.unset("upload");

		return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), DBObject.class, DBEventLog.COLLECTION);
	}

	@Override
	public List<DBObject> getRemainingPlaceholders(String testId) {
		Criteria findTestId = Criteria.where("testId").is(testId);

		// check to see if all placeholders are set by searching for any remaining ones on this test
		Criteria noMorePlaceholders = Criteria.where("upload").exists(true);

		Criteria postSearch = createCriteria(findTestId, noMorePlaceholders);
		Query search = Query.query(postSearch);
		return mongoTemplate.getCollection(DBEventLog.COLLECTION).find(search.getQueryObject()).toArray();
	}

	// call if there aren't any placeholders left on the test, to update the status to FINISHED
	@Override
	public void lastPlaceholderFilled(String testId) {
		// FIXME: only move to finished if we're currently in waiting

		// first, see if it's currently running; if so we update the running object
		TestModule test = testRunnerSupport.getRunningTestById(testId);
		if (test != null) {
			test.fireTestFinished();		// set our current status to finished
			// and stop the running test
		} else {
			// otherwise we need to do it directly in the database
			testInfoService.updateTestStatus(testId, Status.FINISHED);
		}
	}

	@Override
	public List<DBObject> getAllImagesForTestId(String testId) {
		Criteria findTestId = Criteria.where("testId").is(testId);

		Criteria anyImages =
			new Criteria().orOperator(
				Criteria.where("img").exists(true),
				Criteria.where("upload").exists(true)
			);

		// add in the security parameters
		Criteria criteria = createCriteria(findTestId, anyImages);

		Query search = Query.query(criteria);

		return mongoTemplate.getCollection(DBEventLog.COLLECTION).find(search.getQueryObject())
			.sort(BasicDBObjectBuilder.start()
				.add("time", 1)
				.get())
			.toArray();
	}

}
