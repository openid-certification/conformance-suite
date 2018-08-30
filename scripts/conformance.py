#!/usr/bin/env python
#
# python wrapper for conformance suite API
# Author: Joseph Heenan

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import json
import time


class Conformance(object):
    def __init__(self, api_url_base, token_endpoint, requests_session):
        self.api_url_base = api_url_base
        self.requests_session = requests_session
        self.token_endpoint = token_endpoint

    def authorise(self, client_id, client_secret):
        payload = {'grant_type': 'client_credentials'}
        response = self.requests_session.post(self.token_endpoint, data=payload, auth=(client_id, client_secret))
        if response.status_code != 200:
            raise Exception(" {} authorisation failed - HTTP {:d} {}".format(
                self.token_endpoint, response.status_code, response.content))
        token_response = json.loads(response.content.decode('utf-8'))
        print(token_response)
        api_token = token_response['access_token']
        headers = {'Content-Type': 'application/json',
                   'Authorization': 'Bearer {0}'.format(api_token)}
        self.requests_session.headers = headers

    def get_all_test_modules(self):
        """ Returns an array containing a dictionary per test module """
        api_url = '{0}runner/available'.format(self.api_url_base)
        response = self.requests_session.get(api_url)

        if response.status_code != 200:
            raise Exception("get_all_test_modules failed - HTTP {:d} {}".format(response.status_code, response.content))
        return json.loads(response.content.decode('utf-8'))

    def create_test_plan(self, name, configuration):
        api_url = '{0}plan'.format(self.api_url_base)
        payload = {'planName': name}
        response = self.requests_session.post(api_url, params=payload, data=configuration)

        if response.status_code != 201:
            raise Exception("create_test_plan failed - HTTP {:d} {}".format(response.status_code, response.content))
        return json.loads(response.content.decode('utf-8'))

    def create_test(self, test_name, configuration):
        api_url = '{0}runner'.format(self.api_url_base)
        payload = {'test': test_name}
        response = self.requests_session.post(api_url, params=payload, data=configuration)

        if response.status_code != 201:
            raise Exception("create_test failed - HTTP {:d} {}".format(response.status_code, response.content))
        return json.loads(response.content.decode('utf-8'))

    def create_test_from_plan(self, plan_id, test_name):
        api_url = '{0}runner'.format(self.api_url_base)
        payload = {'test': test_name, 'plan': plan_id}
        response = self.requests_session.post(api_url, params=payload)

        if response.status_code != 201:
            raise Exception("create_test failed - HTTP {:d} {}".format(response.status_code, response.content))
        return json.loads(response.content.decode('utf-8'))

    def get_module_info(self, module_id):
        api_url = '{0}info/{1}'.format(self.api_url_base, module_id)
        response = self.requests_session.get(api_url)

        if response.status_code != 200:
            raise Exception("get_module_info failed - HTTP {:d} {}".format(response.status_code, response.content))
        return json.loads(response.content.decode('utf-8'))

    def get_test_log(self, module_id):
        api_url = '{0}log/{1}'.format(self.api_url_base, module_id)
        response = self.requests_session.get(api_url)

        if response.status_code != 200:
            raise Exception("get_test_log failed - HTTP {:d} {}".format(response.status_code, response.content))
        return json.loads(response.content.decode('utf-8'))

    def start_test(self, module_id):
        api_url = '{0}runner/{1}'.format(self.api_url_base, module_id)
        response = self.requests_session.post(api_url)

        if response.status_code != 200:
            raise Exception("start_test failed - HTTP {:d} {}".format(response.status_code, response.content))
        return json.loads(response.content.decode('utf-8'))

    def wait_for_state(self, module_id, required_states, timeout=240):
        timeout_at = time.time() + timeout
        while True:
            if time.time() > timeout_at:
                raise Exception("Timed out waiting for test module {} to be in one of states: {}".
                                format(module_id, required_states))

            info = self.get_module_info(module_id)

            status = info['status']
            print("module id {} status is {}".format(module_id, status))
            if status in required_states:
                return status
            if status == 'INTERRUPTED':
                raise Exception("Test module {} has moved to INTERRUPTED".format(module_id))

            time.sleep(2)
