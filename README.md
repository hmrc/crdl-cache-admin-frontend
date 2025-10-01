
# crdl-cache-admin-frontend

This service provides a view into the data held be the [crdl-cache service](https://github.com/hmrc/crdl-cache/)

## Usage

### Running the service

1. Make sure you run all the dependant services through the service manager:
```shell
sm2 --start CRDL_CACHE_ADMIN
```

2. Stop the admin frontend itself from the service manager and it can be run locally:
```shell
sm2 --stop CRDL_CACHE_ADMIN_FRONTEND
```
```shell
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes 
```
\* some test only endpoints are required to mimic endpoints and redirect requests to services that would otherwise be avaialble on cloud environments but are on different hosts when run through service manager, it is recommended to always enable these with running locally

The service runs on port 7255 by default

### Set up local auth

You will need to set up a dummy internal-auth token by invoking the test-only token endpoint of **internal-auth**:

```shell
curl -i -X POST -H 'Content-Type: application/json' -d '{
  "token": "crdl-cache-token",
  "principal": "emcs-tfe-crdl-reference-data",
  "permissions": [{
    "resourceType": "crdl-cache",
    "resourceLocation": "*",
    "actions": ["READ"]
  }]
}' 'http://localhost:8470/test-only/token'
```
Note that this is the same token used by the crdl-cache service so if you have already setup that service locally, this will be the same and this step can be skipped.

## Accessing the admin frontend

To access the admin frontend, navigate to http://localhost:7255/crdl-cache

> If you are met with a `This page can’t be found` at the url `/internal-auth-frontend/sign-in?continue_url=%2Fcrdl-cache` then it is likely that the required test only endpoints are not enable to handle redirecting this request. Ensure the service is running with test only endpoints enabled.

If not already "logged in" or your session has expired, you will be redirected with the internal-auth-frontend stub where you can set the necessary grant details.

| Field | Value |
| ------|-------|
| Principal | Any value so long as one is provided |
| Redirect url | Should be provided by the request itself: http://localhost:7255/crdl-cache |
| Resource Type | crdl-cache |
| Resource Locations | * |
| Action | READ |

![Example of filled in internal auth test config](.reference-images/Local%20grants.png)

### Viewing values

To see value from crdl-cache, use the service Navigation at the top of each page to jump between data types:

![Service navigation at hte tome of writing](.reference-images/Service%20navigation.png)

#### Available Data

| List | Info |
| ---- | ---- |
| Code Lists | CRDL Cache code lists. Select to see a summary of all available code lists and select Details to see the values of an individual entries and their properties. |
| Customs Offices | Customs offices lists. Select to see a summary of each office and select Details to see all of the individual fields for that office. |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").