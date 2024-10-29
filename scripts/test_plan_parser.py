import pyparsing as pp

# BNF:
#
# name :: alphanum + '-_'
# value :: name + ' '
# config_path :: name + './'
# variant = '[' name '=' value ']'
# module_list :: ':' [name] [',' [name]]*
# env_param :: '(' name '=' value ')'
# test_with_var_and_modules :: name variant* module_list? env_param*
# complete_test :: test_with_var_and_modules ['{' test_with_var_and_modules '}' config_path]?


name_chars = pp.alphanums + '-_'
value_chars = name_chars + ' \\=,./'
path_chars = name_chars + './'

name = pp.Word(name_chars)
value = pp.Word(value_chars)
path = pp.Word(path_chars)
left_brace = pp.Literal('[').suppress()
right_brace = pp.Literal(']').suppress()
left_paren = pp.Literal('(').suppress()
right_paren = pp.Literal(')').suppress()
left_curly = pp.Literal('{').suppress()
right_curly = pp.Literal('}').suppress()
equals = pp.Literal('=').suppress()
colon = pp.Literal(':').suppress()
comma = pp.Literal(',').suppress()

debug_enabled = False

variant = pp.Group(left_brace + name + equals + value + right_brace)
variant_list = pp.Group(pp.OneOrMore(variant)).set_results_name("variants")
variant_list.setParseAction(lambda t: {item[0]: item[1] for item in t.as_list()[0]})
env_param = left_paren + pp.Group(name + equals + path) + right_paren
env_list = pp.Group(pp.OneOrMore(env_param)).set_results_name("environment")
env_list.setParseAction(lambda t: {item[0]: item[1] for item in t.as_list()[0]})
module_list = pp.Group(colon + pp.delimitedList(name, comma)).set_results_name('modules')
test_with_var_and_modules = name.set_results_name("test_name") + pp.Opt(variant_list) + pp.Opt(module_list) + pp.Opt(
    env_list) + pp.Opt(module_list)
test_with_var_and_modules.setParseAction(lambda t: t.as_dict())
op_test = left_curly + test_with_var_and_modules.setDebug(debug_enabled)("test_plan") + right_curly + pp.Opt(
    path).setDebug(debug_enabled).set_results_name("config_file")


# op_test.setParseAction(lambda t: t[0])

def fix_op_test(t):
    aux = t.as_dict()
    op_config = aux["config_file"] if "config_file" in aux else None
    to_return = aux["test_plan"]
    to_return["config_file"] = op_config
    return to_return


op_test.setParseAction(fix_op_test)

test_unit = test_with_var_and_modules.set_results_name("test") + pp.Opt(op_test).set_results_name("op_test")


def fix_test_unit(t):
    to_return = t.as_dict()
    to_return["op_test"] = to_return["op_test"][0] if "op_test" in to_return.keys() else None
    return to_return


test_unit.setParseAction(fix_test_unit)

test_plan = pp.Group(
    test_unit.set_results_name("test_unit").setDebug(debug_enabled) + path.set_results_name("config_file"))


def fix_test_plan(t):
    to_return = t[0].as_dict()
    to_return["test_unit"]["test"]["config_file"] = to_return["config_file"]
    return to_return["test_unit"]


test_plan.setParseAction(fix_test_plan)

command_line_syntax = pp.OneOrMore(test_plan)


def parse_test(test_string):
    if isinstance(test_string, dict):
        # Used to mock the OP and the RP test
        # If the OP is handled specifically as OP test, this wouldn't be used.
        test_unit_obj = test_string
        test_unit_obj['test'] = test_unit_obj['test_plan']
        return test_unit_obj
    test_unit.debug = debug_enabled

    test_unit_parsed = test_unit.parse_string(test_string)

    return test_unit_parsed[0]
