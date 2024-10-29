import unittest
from test_plan_parser import parse_test, test_plan, command_line_syntax


class ParserTestCase(unittest.TestCase):
    def test_simple(self):
        test_config = parse_test("fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")

    def test_simple_mod_list(self):
        test_config = parse_test("fapi1-advanced-final-client-test-plan:mod1,mod2")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["modules"], ["mod1", "mod2"])

    def test_with_one_variant(self):
        test_config = parse_test("fapi1-advanced-final-client-test-plan[variant1=value1]")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["variants"]["variant1"], "value1")

    def test_with_two_variants(self):
        test_config = parse_test("fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2]")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["variants"]["variant1"], "value1")
        self.assertEqual(test_config["test"]["variants"]["variant2"], "value2")

    def test_with_more_variants(self):
        test_config = parse_test(
            "fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4]")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["variants"]["variant1"], "value1")
        self.assertEqual(test_config["test"]["variants"]["variant2"], "value2")

    def test_variants_and_mod_list(self):
        test_config = parse_test(
            "fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4]:mod1,mod2")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["modules"], ["mod1", "mod2"])
        self.assertEqual(test_config["test"]["variants"]["variant1"], "value1")
        self.assertEqual(test_config["test"]["variants"]["variant2"], "value2")

    def test_complex_variant_value(self):
        test_config = parse_test("fapi1-advanced-final-client-test-plan[variant1=this is a value]")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["variants"]["variant1"], "this is a value")

    def test_complex_variant_value_2(self):
        test_config = parse_test("fapi1-advanced-final-client-test-plan[variant1=../../path/to/file.txt]")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["variants"]["variant1"], "../../path/to/file.txt")

    def test_with_env_variable(self):
        test_config = parse_test("fapi1-advanced-final-client-test-plan(ENV1=value1)")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["environment"]["ENV1"], "value1")

    def test_with_2_env_variable(self):
        test_config = parse_test("fapi1-advanced-final-client-test-plan(ENV1=value1)(env2=../../path/to/file.txt)")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["environment"]["ENV1"], "value1")
        self.assertEqual(test_config["test"]["environment"]["env2"], "../../path/to/file.txt")

    def test_with_variant_and_env_variable(self):
        test_config = parse_test(
            "fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3](ENV1=value1)(env2=../../path/to/file.txt)")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["environment"]["ENV1"], "value1")
        self.assertEqual(test_config["test"]["environment"]["env2"], "../../path/to/file.txt")
        self.assertEqual(test_config["test"]["variants"]["variant1"], "value1")
        self.assertEqual(test_config["test"]["variants"]["variant2"], "value2")

    def test_with_variant_env_variable_and_mod_list(self):
        test_config = parse_test(
            "fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4]:mod1,mod2(ENV1=value1)(env2=../../path/to/file.txt)")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["modules"], ["mod1", "mod2"])
        self.assertEqual(test_config["test"]["variants"]["variant1"], "value1")
        self.assertEqual(test_config["test"]["variants"]["variant2"], "value2")
        self.assertEqual(test_config["test"]["environment"]["ENV1"], "value1")
        self.assertEqual(test_config["test"]["environment"]["env2"], "../../path/to/file.txt")

    def test_with_variant_env_variable_and_mod_list(self):
        test_config = parse_test(
            "fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4](ENV1=value1)(env2=../../path/to/file.txt):mod1,mod2")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["modules"], ["mod1", "mod2"])
        self.assertEqual(test_config["test"]["variants"]["variant1"], "value1")
        self.assertEqual(test_config["test"]["variants"]["variant2"], "value2")
        self.assertEqual(test_config["test"]["environment"]["ENV1"], "value1")
        self.assertEqual(test_config["test"]["environment"]["env2"], "../../path/to/file.txt")

    def test_simple_op(self):
        test_config = parse_test(
            "fapi1-advanced-final-client-test-plan{fapi1-advanced-final-op-test-plan}op_test_config.json")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["op_test"]["test_name"], "fapi1-advanced-final-op-test-plan")
        self.assertEqual(test_config["op_test"]["config_file"], "op_test_config.json")

    def test_rp_op_complete(self):
        test_config = parse_test(
            "fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4](ENV1=value1)(env2=../../path/to/file.txt):mod1,mod2{fapi1-advanced-final-op-test-plan[var1=val1][var2=val2](env1=val1):tst1,tst2}op_test_config.json")
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["modules"], ["mod1", "mod2"])
        self.assertEqual(test_config["test"]["variants"]["variant1"], "value1")
        self.assertEqual(test_config["test"]["variants"]["variant2"], "value2")
        self.assertEqual(test_config["test"]["environment"]["ENV1"], "value1")
        self.assertEqual(test_config["test"]["environment"]["env2"], "../../path/to/file.txt")
        self.assertEqual(test_config["op_test"]["test_name"], "fapi1-advanced-final-op-test-plan")
        self.assertEqual(test_config["op_test"]["modules"], ["tst1", "tst2"])
        self.assertEqual(test_config["op_test"]["environment"]["env1"], "val1")
        self.assertEqual(test_config["op_test"]["variants"]["var1"], "val1")
        self.assertEqual(test_config["op_test"]["config_file"], "op_test_config.json")

    def test_plan_simple(self):
        test_config = test_plan.parse_string("fapi1-advanced-final-client-test-plan config_file.json")[0]
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["config_file"], "config_file.json")


    def test_plan_with_params(self):
        test_config = test_plan.parse_string("fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4](ENV1=value1)(env2=../../path/to/file.txt):mod1,mod2 config_file.json")[0]
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["config_file"], "config_file.json")

    def test_plan_with_op(self):
        test_config = test_plan.parse_string("fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4](ENV1=value1)(env2=../../path/to/file.txt):mod1,mod2{fapi1-advanced-final-op-test-plan[var1=val1][var2=val2](env1=val1):tst1,tst2}op_test_config.json config_file.json")[0]
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["config_file"], "config_file.json")


    def test_plan_with_op_without_config_file(self):
        test_config = test_plan.parse_string("fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4](ENV1=value1)(env2=../../path/to/file.txt):mod1,mod2{fapi1-advanced-final-op-test-plan[var1=val1][var2=val2](env1=val1):tst1,tst2}_ config_file.json")[0]
        self.assertEqual(test_config["test"]["test_name"], "fapi1-advanced-final-client-test-plan")
        self.assertEqual(test_config["test"]["config_file"], "config_file.json")


    def test_command_line(self):
        cmd_line = "fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4](ENV1=value1)(env2=../../path/to/file.txt):mod1,mod2{fapi1-advanced-final-op-test-plan[var1=val1][var2=val2](env1=val1):tst1,tst2}_ config_file.json"
        cmd_line += " fapi1-advanced-final-client-test-plan[variant1=value1][variant2=value2][variant3=value3][variant4=value4](ENV1=value1)(env2=../../path/to/file.txt):mod1,mod2{fapi1-advanced-final-op-test-plan[var1=val1][var2=val2](env1=val1):tst1,tst2}op_test_config.json config_file.json"
        cmd_line += " fapi1-advanced-final-client-test-plan config_file.json"
        test_config = command_line_syntax.parse_string(cmd_line)
        self.assertEqual(len(test_config), 3)
        self.assertEqual(test_config["test"]["config_file"], "config_file.json")



if __name__ == '__main__':
    unittest.main()
