# Versioning of apis


Discussion points
## We need to support multiple api versions to allow breaking changes to be deployed independently and seamlessly
Versioning allows a consumer to be decoupled from the api implementation and switch to newer versions in its own time 

i.e.
Consumer is using v1 of an api
The api may need to publish a new version v2 of the api
The api may be deployed at any time because the new ( breaking ) functionality in v2 does not affect v1
At some point in the future the consumer will switch to v2 
Once there are no more consumers of v1 the api will drop the version v1 and its supporting code
Of course there could be more than 2 current versions

Design issues to consider
* How to publish multiple api versions
* How to inform consumers when an api has been deprecated 
* How to warn consumers of an end date of deprecated version
* Do we need an end date after which an api version will be dropped
* How to get accurate information about what api versions consumers are using


## Versioning options
## ✅ OPTION 1 — Add version to the url
i.e. /example/v1

### ⭐ Pros
- Simple
- Easy and obvious uniqueness of controller methods
- Endpoint logging can easily include the version


### ⚠️ Cons
- Adds complication to the url



## ✅ OPTION 2 — Add version as a query parameter
i.e. /example?version=1

### ⭐ Pros
- Endpoint logging can easily include the version


### ⚠️ Cons
- Adds complication to the url



## ✅ OPTION 3 — Add version in a header
i.e. -H "X-API-Version:1"

### ⭐ Pros
- Endpoint logging can easily include the version


### ⚠️ Cons
- Complicates logging
- Ensuring unique endpoint methods is more complex



## Versioning of the api jarfile is independent of the versioning of the api
The consuming service can only import a single jarfile thus the specs to support multiple api versions must be 
in the same api jarfile

