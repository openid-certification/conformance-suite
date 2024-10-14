import re
import json

class TestConfigParser:
    def __init__(self, client_certs, baseurl, baseurlmtls,):
        self._client_certs = client_certs
        self._baseurl = baseurl
        self._baseurlmtls = baseurlmtls

    def parse(self, plan_config_file_name):
        with open(plan_config_file_name) as f:
            json_config = f.read()
        json_config = json_config.replace('{BASEURL}', self._baseurl)
        json_config = json_config.replace('{BASEURLMTLS}', self._baseurlmtls)

        for k,v in self._client_certs.items():
            json_config = json_config.replace('{'+ k + '}', v)
        return (json_config, json.loads(json_config))

# syntax is:
# test_plan_name[variant=value][variant2=value2]:optional-run-only-module-named-test{optestplan}optestconfig
# optestplan/config are only used when running the rp tests against the op tests
def split_name_and_variant(test_plan):
    modules = None
    op_test = None
    op_config = None
    test_plan = test_plan.replace('+', ' ')
    if '{' in test_plan:
        (test_plan, op_test) = test_plan.split("{", 1)
        (op_test, op_config) = op_test.split("}", 1)
    if ':' in test_plan:
        (test_plan, module) = test_plan.split(":", 1)
        modules = module.split(",")
    if '[' in test_plan:
        name = re.match(r'^[^\[]*', test_plan).group(0)
        vs = re.finditer(r'\[([^=\]]*)=([^\]]*)\]', test_plan)
        variant = {v.group(1): v.group(2) for v in vs}
        return (name, variant, modules, op_test, op_config)
    elif '(' in test_plan:
        #only for oidcc RP tests
        matches = re.match(r'(.*)\((.*)\)$', test_plan)
        name = matches.group(1)
        oidcc_configfile = matches.group(2)
        return (name, oidcc_configfile, modules, op_test, op_config)
    else:
        return (test_plan, None, modules, op_test, op_config)


# If testmodule has variants, return a string form like used on our command line
# e.g.
# oidcc-server[client_auth_type=client_secret_basic][response_mode=default][response_type=code]
# This is useful for using as a dictionary key for storing results
def get_string_name_for_module_with_variant(moduledict):
    name = moduledict['testModule']
    variants = moduledict.get('variant')
    if variants != None:
        for v in sorted(variants.keys()):
            name += "[{}={}]".format(v, variants[v])
    return name
