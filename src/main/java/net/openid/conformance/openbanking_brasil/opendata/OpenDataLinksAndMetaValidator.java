package net.openid.conformance.openbanking_brasil.opendata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

public class OpenDataLinksAndMetaValidator {

    private final AbstractJsonAssertingCondition validator;

    public OpenDataLinksAndMetaValidator(AbstractJsonAssertingCondition validator) {
        this.validator = validator;
    }

    public void assertMetaAndLinks(JsonElement body) {
        validator.assertField(body,
                new ObjectField
                        .Builder("links")
                        .setValidator(this::assertLinks)
                        .build());

        validator.assertField(body,
                new ObjectField
                        .Builder("meta")
                        .setValidator(this::assertMeta)
                        .build());
    }

    private void assertLinks(JsonObject links) {
        validator.assertField(links,
                new StringField
                        .Builder("self")
                        .setMaxLength(2000)
                        .setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
                        .build());

        validator.assertField(links,
                new StringField
                        .Builder("first")
                        .setOptional()
                        .setMaxLength(2000)
                        .setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
                        .build());

        validator.assertField(links,
                new StringField
                        .Builder("prev")
                        .setOptional()
                        .setMaxLength(2000)
                        .setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
                        .build());

        validator.assertField(links,
                new StringField
                        .Builder("next")
                        .setOptional()
                        .setMaxLength(2000)
                        .setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
                        .build());

        validator.assertField(links,
                new StringField
                        .Builder("last")
                        .setOptional()
                        .setMaxLength(2000)
                        .setPattern("^(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$")
                        .build());
    }

    private void assertMeta(JsonObject meta) {
        validator.assertField(meta,
                new IntField
                        .Builder("totalRecords")
                        .build());

        validator.assertField(meta,
                new IntField
                        .Builder("totalPages")
                        .build());
    }
}
