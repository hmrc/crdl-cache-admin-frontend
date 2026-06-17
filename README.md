
# crdl-cache-admin-frontend

This service provides a view into the data held be the [crdl-cache service](https://github.com/hmrc/crdl-cache/)

## Usage

[API Documentation (1.0)](https://redocly.github.io/redoc/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhmrc%2Fcrdl-cache%2Frefs%2Fheads%2Fmain%2Fpublic%2Fapi%2F1.0%2Fopenapi.yaml)

### Running the service

1. Make sure you run all the dependant services through the service manager:
```shell
sm2 --start CRDL_CACHE_ADMIN
```

2. Stop both the admin frontend and the cache from the service manager so they can be run locally:
```shell
sm2 --stop CRDL_CACHE_ADMIN_FRONTEND
```
```shell
sm2 --stop CRDL_CACHE
```
```shell
sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes 
```
\* some test only endpoints are required to mimic endpoints and redirect requests to services that would otherwise be available on cloud environments but are on different hosts when run through service manager, it is recommended to always enable these with running locally

The service runs on port 7255 by default

### Test Only Endpoints

* **Customs office import** can be triggered via curl using the below test-only endpoint:
    ```shell
    curl -X POST http://localhost:7252/crdl-cache/test-only/customs-office-lists
    ```

* Similarly, for **codelists**:
    ```shell
    curl -X POST http://localhost:7252/crdl-cache/test-only/codelists
    ```

* For **correspondence lists**:
    ```shell
    curl -X POST http://localhost:7252/crdl-cache/test-only/correspondence-lists
    ```

* For **Phase & Domain list**:
    ```shell
    curl -X POST http://localhost:7252/crdl-cache/test-only/pd-lists
    ```
  
* In cases when you need a fresh import you can use the **DELETE** test-only endpoints to clear the data that has been previously imported. Here are the delete endpoints for customs office, codelists, correspondence lists and last updated respectively.

  Please note that the last updated needs to be deleted along with codelists and correspondence lists. This is because the last updated contains the data when the codelists and correspondence lists were last updated.
    ```shell
    curl -X DELETE http://localhost:7252/crdl-cache/test-only/customs-office-lists
    curl -X DELETE http://localhost:7252/crdl-cache/test-only/codelists
    curl -X DELETE http://localhost:7252/crdl-cache/test-only/correspondence-lists
    curl -X DELETE http://localhost:7252/crdl-cache/test-only/last-updated
    curl -X DELETE http://localhost:7252/crdl-cache/test-only/pd-lists
    ```

* To check the **status** of an import you can use the following endpoints for customs offices, codelists and correspondence lists respectively.
    ```shell
    curl -X GET http://localhost:7252/crdl-cache/test-only/codelists
    curl -X GET http://localhost:7252/crdl-cache/test-only/customs-office-lists
    curl -X GET http://localhost:7252/crdl-cache/test-only/correspondence-lists
    curl -X GET http://localhost:7252/crdl-cache/test-only/pd-lists
    ```
  Depending on the job status it would either return IDLE or RUNNING status.

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

To access the admin frontend, navigate to http://localhost:7255/crdl-cache-admin-frontend

> If you are met with a `This page can’t be found` at the url `/internal-auth-frontend/sign-in?continue_url=%2Fcrdl-cache` then it is likely that the required test only endpoints are not enable to handle redirecting this request. Ensure the service is running with test only endpoints enabled.

If not already "logged in" or your session has expired, you will be redirected with the internal-auth-frontend stub where you can set the necessary grant details.

| Field | Value                                                                                     |
| ------|-------------------------------------------------------------------------------------------|
| Principal | Any value so long as one is provided (e.g. my-service, or jo.bloggs)                      |
| Redirect url | Should be provided by the request itself: http://localhost:7255/crdl-cache-admin-frontend |
| Resource Type | crdl-cache                                                                                |
| Resource Locations | *                                                                                         |
| Action | READ                                                                                      |

### Viewing values

To see value from crdl-cache, use the service Navigation at the top of each page to jump between data types.

#### Available Data

| List | Info                                                                                                                                                        |
| ---- |-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Code Lists | CRDL Cache code lists. Select to see a summary of all available code lists and select Details to see the values of individual entries and their properties. |
| Customs Offices | Customs offices lists. Select to see a summary of each office and select Details to see all of the individual fields for that office.                       |

---

### Scalafmt

Check all project files are formatted as expected as follows:

> `sbt scalafmtCheckAll`

Format `*.sbt` and `project/*.scala` files as follows:

> `sbt scalafmtSbt`

Format all project files as follows:

> `sbt scalafmtAll`

### Tests

Run all unit tests with command:

> `sbt test`

Run all integration tests command:

> `sbt it/test`

### All tests and checks
This is an sbt command alias specific to this project. It will run a scala format
check, run unit tests, run integration tests and produce a coverage report:

> `sbt runAllChecks`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").