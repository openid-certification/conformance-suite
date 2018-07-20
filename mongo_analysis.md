# MongoDB Usage Analysis

## DB
The application uses only one database, named `test_suite`

### Collections
There are three collections in the `test_suite` database

#### `OIDC_REGISTERED_CLIENTS` Collection
This collection holds the dynamically registered client information when a user logs into the test suite using something other than Google. (I.E. `user@mitreid.org`)

##### Fields
> `_id`:  This is the ID of the client, it is the same as the 'issuer' (e.g. `https://mitreid.org`
>
> `_class`: This is the class used by Spring to represent this information, should be `com.mongodb.BasicDBObject`
>
> `client_json`: This contains the serialized JSON for the client registration.
>
> `time`: NumberLong timestamp for the entry

Since this collection is only searched by the issuer (`_id`) The default index on the `_id` field is sufficent.

#### `TEST_INFO` Collection
This collection contains information about each test instance in the framework.

##### Fields
> `_id`: Id field, random string
>
> `_class`: Java class used by Spring to represent this data (should be `com.mongodb.BasicDBObject`)
>
> `testId`: Same as `_id`?
>
> `testName`: This is the name of the test being run. (What type of test is being run)
>
> `started`: Timestamp of when the test was started
>
> `config`: This contains a structure that is the configuration for the test being run. the contents of this sub document depend on the test.
>
> `alias`: Alias for this test. A non-unique name that can be attached to the particular instance of a test.
>
> `owner`: The owner of the test, it is a sub document with the `sub` and `iss` properties of the user. ('subject' and 'issuer' related to how they logged in.
>
> `status`: Current Status of the test, a String value.
>
> `result`: Final result of the test.

##### Queries from `TestInfoApi.java`
`/info` Request Mapping:
> If the user is an admin, it is just a `.find()` of all objects.
If the user isn't an admin, the `owner` field is used, since this is a majority of the cases, an index on just `owner` could be useful

`/info/{id}` RequestMapping:
> As an admin, the query hits just the `_id` field, so the default index is sufficant.
> As a non-admin, the query hits both `_id` and `owner`, HOWEVER since `_id` is unique and already indexed, MongoDB will use the `_id` index to find the document first, then compair the `owner` to it. This means adding a compound index won't be helpful.

##### Queries from `DBTestInfoService.java`
The only `queries` of note in the `DBTestInfoService` are the update operations which are performed on documents based on `_id` (and sometimes `owner`), thus the default index is fine here.


####`EVENT_LOG` Collection

##### Fields (A subset of the fields that are relevnet)
> `_id`: Id field, made up from the `testId` plus a `-` and a long string of random chars
>
> `_class`: Java class used by Spring to represent this data (should be `com.mongodb.BasicDBObject`)
>
> `testId`: Same as `_id`?
>
> `testName`: This is the name of the test being run. (What type of test is being run)
>
> `started`: Timestamp of when the test was started
>
> `alias`: Alias for this test. A non-unique name that can be attached to the particular instance of a test.
>
> `testOwner`: The owner of the test, it is a sub document with the `sub` and `iss` properties of the user. ('subject' and 'issuer' related to how they logged in.
>
> `src`: Where the log came from.
>

##### Queries from `LogApi.java`
`/log` Request Mapping:
> `testOwner` is used to filter documents if the user is not an admin (so an index on `testOwner` could be helpful
>
> There is a call to get distinct `testId`s using the `testOwner` filter from above. There can perhaps be a better way to deal with this index wise. **DO MORE RESEARCH**

`/log/{id}` Request Mapping:
> The main filter being done here is on `testId` so an index on that could help immensly. Addtionally `testOwner` is used, but that can be fufilled by an index on `testOwner` alone. (from above...)
> Additionally a `.sort()` is done on the `time` field. Again an index will speed this operation.

##### Queries from `ImageAPI.java`
The updates performed here use the `testId` field for the main searching. (Along with placeholders, etc. But since all queries use `testId` an index on that should be the most bennificial.)


# Suggested addtional Indeices
Along with the default `_id` index that is on all collections, the following indices could be added to speed performance.

`OIDC_REGISTERED_CLIENTS`
> None. Only the `_id` field is used.

`TEST_INFO`
> `owner` This is used quite often to limit results, so creating an index on this field would be most helpful

`EVENT_LOG`
> `testOwner` is used when preparing the list of log entries
> `testId` is used extensivly (this is used in the 'distinct' call as well)
> `time` is used to sort fields along with `testId`
