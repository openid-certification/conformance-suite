/*******************************************************************************
 * Copyright 2016 The MITRE Corporation
 *   and the MIT Internet Trust Consortium
 *
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

package io.bspk.testframework.strawman.runner;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bspk.testframework.strawman.frontChannel.BrowserControl;

/**
 * @author jricher
 *
 */
public class CollectingBrowserControl implements BrowserControl {

	private static Logger logger = LoggerFactory.getLogger(CollectingBrowserControl.class);
	
	private List<String> urls = new ArrayList<>();
	private List<String> visited = new ArrayList<>();
	
	@Override
	public void goToUrl(String url) {
		logger.info("Browser going to: " + url);
		
		urls.add(url);
	}

	public List<String> getUrls() {
		return urls;
	}

	/* (non-Javadoc)
	 * @see io.bspk.testframework.strawman.frontChannel.BrowserControl#urlVisited(java.lang.String)
	 */
	@Override
	public void urlVisited(String url) {
		logger.info("Browser went to: " + url);
		
		urls.remove(url);
		visited.add(url);
		
	}

	/**
	 * @return the visited
	 */
	public List<String> getVisited() {
		return visited;
	}

}
