# controller-demo

Demonstrates the pattern used across HMCTS services where a **separate API spec jar** defines the
controller interfaces and model classes, and the service simply implements them.

## The pattern

```
api-hmcts-crime-template (jar)          controller-demo (this service)
─────────────────────────────           ──────────────────────────────
ExamplesApi  (interface)        ──▶     ExampleController implements ExamplesApi
RootApi      (interface)        ──▶     RootController    implements RootApi
ExampleResponse (model)         ──▶     used as return type in ExampleController
ErrorResponse   (model)         ──▶     used by the default error handling in the interface
```

The API jar is generated from an OpenAPI spec using the OpenAPI Generator Gradle plugin
(`interfaceOnly: true`). It contains only interfaces and model POJOs — no business logic.

## Why this approach?

- **Contract first** — the OpenAPI spec is the single source of truth for paths, verbs, request/response shapes
- **No annotation scatter** — `@RequestMapping`, `@Operation`, `@Schema` etc. live in the jar, not in the service
- **Enforced at compile time** — if the jar changes a method signature the service won't compile

## Structure

```
controller-demo/
├── build.gradle
└── src/
    ├── main/java/uk/gov/hmcts/marketplace/
    │   ├── Application.java
    │   ├── controllers/
    │   │   ├── ExampleController.java   ← @RestController implements ExamplesApi
    │   │   └── RootController.java      ← @RestController implements RootApi
    │   └── services/
    │       └── ExampleService.java      ← business logic, builds ExampleResponse
    └── test/java/.../integration/
        └── SpringBootHappyIntegrationTest.java
```

## Key dependency

```groovy
configurations {
    apiSpec
    implementation.extendsFrom apiSpec
}

dependencies {
    apiSpec files("../../api-hmcts-crime-template/build/libs/api-hmcts-crime-template-0.0.999.jar")
}
```

The `apiSpec` configuration keeps the API jar visible as a distinct dependency.
In a CI/CD pipeline this would be resolved from the Azure DevOps artifact feed:

```groovy
apiSpec "uk.gov.hmcts.cp:api-hmcts-crime-template:2.0.2"
```

## Implementing a controller

```java
@RestController
@RequiredArgsConstructor
@Slf4j
public class ExampleController implements ExamplesApi {

    private final ExampleService exampleService;

    @Override
    public ResponseEntity<ExampleResponse> getExampleByExampleId(final Long exampleId) {
        return ResponseEntity.ok(exampleService.getExampleById(exampleId));
    }
}
```

- No `@RequestMapping` needed — the path is declared in the interface
- No model imports from the service layer — `ExampleResponse` comes straight from the jar
- `@RestController` is the only Spring annotation required on the class

## Running locally

```bash
./gradlew bootRun
```

```bash
curl http://localhost:8080/
curl http://localhost:8080/examples/1
```

## Running tests

```bash
./gradlew test
```
