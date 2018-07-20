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
    def __init__(self, api_url_base, requests_session):
        self.api_url_base = api_url_base
        self.requests_session = requests_session

    def authorise(self):
        self.auth_server = "http://localhost:9001/"
        api_url = '{0}token'.format(self.auth_server)
        payload = {'grant_type': 'client_credentials'}
        response = self.requests_session.post(api_url, data=payload, auth=('oauth-client-1', 'oauth-client-secret-1'))
        if response.status_code != 200:
            raise Exception(" {} authorisation failed - HTTP {:d} {}".format(self.auth_server, response.status_code, response.content))
        token_response = json.loads(response.content.decode('utf-8'))
        print(token_response)
        api_token = token_response['access_token']
        headers = {'Content-Type': 'application/json',
                   'Authorization': 'Bearer {0}'.format(api_token)}
        self.requests_session.headers = headers

    def create_test_plan(self, name, configuration):
        api_url = '{0}plan'.format(self.api_url_base)
        payload = {'planName': name}
        response = self.requests_session.post(api_url, params=payload, data=configuration)

        if response.status_code != 201:
            raise Exception("create_test_plan failed - HTTP {:d} {}".format(response.status_code, response.content))
        return json.loads(response.content.decode('utf-8'))

    def create_test(self, plan_id, test_name, configuration):
        api_url = '{0}runner'.format(self.api_url_base)
        payload = {'test': test_name, 'plan': plan_id}
        response = self.requests_session.post(api_url, params=payload, data=configuration)

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

    def wait_for_state(self, module_id, required_state):
        timeout_at = time.time() + 30
        while True:
            if time.time() > timeout_at:
                raise Exception("Timed out waiting for test module {} to move to state {}".
                                format(module_id, required_state))

            info = self.get_module_info(module_id)

            status = info['status']
            print("module id {} status is {}", module_id, status)
            if status == required_state:
                return
            if status == 'INTERRUPTED':
                raise Exception("Test module {} has moved to INTERRUPTED".format(module_id, required_state))

            time.sleep(2)
