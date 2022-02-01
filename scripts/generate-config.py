#!/usr/bin/env python3

import json

def load_override_template():
    with open('./scripts/configs/override-template.json') as json_file:
        return json.load(json_file)

def add_overrides(config, overrides):

    overrides_element = {}
    config['override'] = overrides_element
    for module_name in overrides:
        override_info = overrides[module_name]
        override_data = load_override_template()
        if 'pixPaymentAmount' in override_info:
            payment_amount = override_info['pixPaymentAmount']
            print('Overriding pix payment amount in module {0} to {1}'.format(module_name, payment_amount))
            override_data['resource']['brazilPaymentConsent']['data']['payment']['amount'] = payment_amount
            override_data['resource']['brazilPixPayment']['data']['payment']['amount'] = payment_amount
        if 'qrdnAmount' in override_info:
            payment_amount = override_info['qrdnAmount']
            print('Overriding qrdn payment amount in module {0} to {1}'.format(module_name, payment_amount))
            override_data['resource']['brazilQrdnRemittance'] = "$$EDITPAYMENT$$:" + payment_amount
        overrides_element[module_name] = override_data


if __name__ == '__main__':

    data = {}
    overrides = {}

    with open('./scripts/configs/base-payment-config.json') as json_file:
        data = json.load(json_file)

    with open('./scripts/configs/overrides.json') as json_file:
        overrides = json.load(json_file)

    add_overrides(data, overrides)

    with open('./scripts/configs/generated-payment-config.json', 'w+', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=4)
