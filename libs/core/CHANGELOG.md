## [v1.8.0]

## Changelog

## 🛠  Build
- [5146f79](https://github.com/antistereov/singularity-core/commits/5146f79) bump version to 1.8.0
- [1122c27](https://github.com/antistereov/singularity-core/commits/1122c27) fix release and deploy script


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.7.0]

## Changelog

## 🚀 Features
- [fd3df82](https://github.com/antistereov/singularity-content/commits/fd3df82) **core**: rename env variable name for path access style in s3
- [fb0c312](https://github.com/antistereov/singularity-content/commits/fb0c312) **core**: update s3 properties to allow path-style-access
- [2506bad](https://github.com/antistereov/singularity-content/commits/2506bad) **core**: implement group controller with pagination and sorting
- [0f834e1](https://github.com/antistereov/singularity-content/commits/0f834e1) **core**: create interface for translatable crud service
- [0779ab0](https://github.com/antistereov/singularity-content/commits/0779ab0) **core**: create interface for crud service
- [43bd776](https://github.com/antistereov/singularity-content/commits/43bd776) **core**: implement group controller with full crud funcitonality
- [94fcac0](https://github.com/antistereov/singularity-content/commits/94fcac0) **core**: SuccessResponse is now true by default
- [e4e1366](https://github.com/antistereov/singularity-content/commits/e4e1366) **core**: implement base mail template and use it
- [d33e9b9](https://github.com/antistereov/singularity-content/commits/d33e9b9) **core**: implement group controller
- [4699dab](https://github.com/antistereov/singularity-content/commits/4699dab) **core**: refactor config for user
- [e71717b](https://github.com/antistereov/singularity-content/commits/e71717b) **core**: hash service use hmacsha256 for SearchableHash
- [4f2504a](https://github.com/antistereov/singularity-content/commits/4f2504a) **content**: improve access details
- [47e340e](https://github.com/antistereov/singularity-content/commits/47e340e) **core**: improve email template and fix translation
- [8b75ed9](https://github.com/antistereov/singularity-content/commits/8b75ed9) **core**: implement invitation, email templates, translations, ...
- [621ced6](https://github.com/antistereov/singularity-content/commits/621ced6) **content**: update article management methods and add image upload
- [02e84a5](https://github.com/antistereov/singularity-content/commits/02e84a5) **content**: admin are also allowed to edit
- [57d160b](https://github.com/antistereov/singularity-content/commits/57d160b) **content**: implement content and article updates
- [464cd05](https://github.com/antistereov/singularity-content/commits/464cd05) **content**: make summary and content optional in CreateArticleRequest
- [a859037](https://github.com/antistereov/singularity-content/commits/a859037) **core**: fix primary language of group to english
- [99cce28](https://github.com/antistereov/singularity-content/commits/99cce28) **content**: fix primary language of tag to english
- [4e1497a](https://github.com/antistereov/singularity-content/commits/4e1497a) **content**: add german translations for initial tags
- [f9b6dd6](https://github.com/antistereov/singularity-content/commits/f9b6dd6) **content**: create editor group as initial grousp
- [91476bb](https://github.com/antistereov/singularity-content/commits/91476bb) **content**: update content permissions to use group key instead of group id
- [fe16752](https://github.com/antistereov/singularity-content/commits/fe16752) **core**: create translations for groups and add initial groups to application properties
- [630a29d](https://github.com/antistereov/singularity-content/commits/630a29d) **core**: add fallback options for translate method in Translatable interface
- [fee34c5](https://github.com/antistereov/singularity-content/commits/fee34c5) **core**: add fallback options for translate method in Translatable interface
- [0119182](https://github.com/antistereov/singularity-content/commits/0119182) **core**: rename UserDto to UserResponse
- [134f857](https://github.com/antistereov/singularity-content/commits/134f857) **core**: remove application info from user document - this will be saved in a separate collection in the future
- [f97d013](https://github.com/antistereov/singularity-content/commits/f97d013) **core**: remove device data from UserDto
- [e82f07f](https://github.com/antistereov/singularity-content/commits/e82f07f) **core**: add translations
- [cc56711](https://github.com/antistereov/singularity-content/commits/cc56711) **core**: simplify base exception handler and update usages
- [7c91660](https://github.com/antistereov/singularity-content/commits/7c91660) **content**: use tag key for identification instead of id
- [6e7d2bc](https://github.com/antistereov/singularity-content/commits/6e7d2bc) **stereov-io**: add default tags that will be created on startup
- [1d595ce](https://github.com/antistereov/singularity-content/commits/1d595ce) **content**: create findById endpoint in TagController
- [ccdf273](https://github.com/antistereov/singularity-content/commits/ccdf273) **content**: improve getArticles endpoint with filtering based on tags
- [97e89ff](https://github.com/antistereov/singularity-content/commits/97e89ff) **content**: create TagResponse as DTO for TagDocument
- [a2b2594](https://github.com/antistereov/singularity-content/commits/a2b2594) **content**: add properties and allow creation of predefined tags on startup
- [6e18fbb](https://github.com/antistereov/singularity-content/commits/6e18fbb) **content**: add tag features
- [f4b5d3a](https://github.com/antistereov/singularity-content/commits/f4b5d3a) **core**: create criteria creator for field contains substring
- [a97d250](https://github.com/antistereov/singularity-content/commits/a97d250) **content**: use jackson instead of kotlinx.serialization and ObjectId instead of String for ids, add missing features in article api
- [66287fc](https://github.com/antistereov/singularity-content/commits/66287fc) **core**: use Jackson instead of kotlinx.serialization and use ObjectId class for ids
- [d3fd854](https://github.com/antistereov/singularity-content/commits/d3fd854) **content**: create scroll and get requests for articles
- [f4a7928](https://github.com/antistereov/singularity-content/commits/f4a7928) **content**: update ContentAutoConfiguration after adding common package
- [cd150fb](https://github.com/antistereov/singularity-content/commits/cd150fb) **content**: update Article implementation to use all functions and fields from common package
- [202f01e](https://github.com/antistereov/singularity-content/commits/202f01e) **content**: implement common package to unify fields and functions used by all content document
- [282673c](https://github.com/antistereov/singularity-content/commits/282673c) **core**: update configuration to include groups
- [1061844](https://github.com/antistereov/singularity-content/commits/1061844) **core**: change AuthenticationService to include null-safe getCurrentUserOrNull method instead for userId
- [82c8ef3](https://github.com/antistereov/singularity-content/commits/82c8ef3) **core**: create ExistsResponse
- [ccec586](https://github.com/antistereov/singularity-content/commits/ccec586) **core**: remove unused null-checks for _id
- [4d32a1d](https://github.com/antistereov/singularity-content/commits/4d32a1d) **core**: make _id private in UserDocument
- [4b950d8](https://github.com/antistereov/singularity-content/commits/4b950d8) **core**: add groups to UserDocument
- [4015863](https://github.com/antistereov/singularity-content/commits/4015863) **core**: create group collection and functions
- [5cf542e](https://github.com/antistereov/singularity-content/commits/5cf542e) **content**: create more advanced queries for article access based on rights
- [88d339b](https://github.com/antistereov/singularity-content/commits/88d339b) **content**: save now requires authentication
- [3ef0b38](https://github.com/antistereov/singularity-content/commits/3ef0b38) **content**: image in Article is now optional
- [ccfc701](https://github.com/antistereov/singularity-content/commits/ccfc701) **core**: add validateVerification to AuthenticationService
- [2149634](https://github.com/antistereov/singularity-content/commits/2149634) **core**: roles in UserDocument is now a set
- [64e78f1](https://github.com/antistereov/singularity-content/commits/64e78f1) **content**: move article specific classes to new content package and implement some new functions
- [ced1ad9](https://github.com/antistereov/singularity-content/commits/ced1ad9) **core**: change base path to /api
- [405f371](https://github.com/antistereov/singularity-content/commits/405f371) **stereov-io**: add endpoint to get all users to admin
- [80ccba5](https://github.com/antistereov/singularity-content/commits/80ccba5) **stereov-io**: articles now save the creator
- [b39c340](https://github.com/antistereov/singularity-content/commits/b39c340) **core**: name field is now required in UserDocument
- [8ff7385](https://github.com/antistereov/singularity-content/commits/8ff7385) **stereov-io**: create more concise DTOs for overview and full
- [ad06d05](https://github.com/antistereov/singularity-content/commits/ad06d05) **stereov-io**: enhance ArticleService and add new classes for Article
- [6f7dc17](https://github.com/antistereov/singularity-content/commits/6f7dc17) **stereov-io**: add stereov-io application
- [7da68e8](https://github.com/antistereov/singularity-content/commits/7da68e8) **core**: add application slug to secret key names

## 🐛 Fixes
- [a796336](https://github.com/antistereov/singularity-content/commits/a796336) **content**: findByKey does not require permissions anymore - if authorization is required, use findAuthorizedByKey
- [c4d5f76](https://github.com/antistereov/singularity-content/commits/c4d5f76) **core**: javaMailSender now uses MimeMessage instead of SimpleMessage and test this
- [02d9755](https://github.com/antistereov/singularity-content/commits/02d9755) **core**: fix configurations to make tests run
- [119c7de](https://github.com/antistereov/singularity-content/commits/119c7de) **core**: fix mail template creation
- [f0bee20](https://github.com/antistereov/singularity-content/commits/f0bee20) **content**: uniqueKey will now only add UUID if the article ids are not the same
- [d500967](https://github.com/antistereov/singularity-content/commits/d500967) **content**: fix change image endpoint
- [b14c64f](https://github.com/antistereov/singularity-content/commits/b14c64f) **content**: fix auto configuration
- [db3ec3e](https://github.com/antistereov/singularity-content/commits/db3ec3e) **content**: add ArticleManagementController bean to content auto configuration
- [48358b3](https://github.com/antistereov/singularity-content/commits/48358b3) **content**: fix methods after removing unused function parameters
- [6065755](https://github.com/antistereov/singularity-content/commits/6065755) **content**: update auto configuration after changes in ArticleService
- [3d66750](https://github.com/antistereov/singularity-content/commits/3d66750) **content**: tags is now a set of ObjectId instead of strings
- [77e2e93](https://github.com/antistereov/singularity-content/commits/77e2e93) **content**: fix collection name for TagDocument to tags
- [72f3aaa](https://github.com/antistereov/singularity-content/commits/72f3aaa) **stereov-io**: add content lib to Dockerfile
- [83c4ce4](https://github.com/antistereov/singularity-content/commits/83c4ce4) **content**: disable bootRun task in build script
- [bbf2dbd](https://github.com/antistereov/singularity-content/commits/bbf2dbd) **core**: fix code after changing base path
- [0153592](https://github.com/antistereov/singularity-content/commits/0153592) **article**: remove unused imports and enable reactive MongoDB repositories
- [2fc3760](https://github.com/antistereov/singularity-content/commits/2fc3760) **demo**: add application name to .env.sample and add reference to application.yml

## 🔄️ Changes
- [1ed8e74](https://github.com/antistereov/singularity-content/commits/1ed8e74) create build.gradle.kts in root directory and specify build and release configurations
- [72e1f09](https://github.com/antistereov/singularity-content/commits/72e1f09) **core**: prepare core lib for mirroring to public repo
- [50b5fe6](https://github.com/antistereov/singularity-content/commits/50b5fe6) **core**: move AccessTokenCache to user package
- [1225dc4](https://github.com/antistereov/singularity-content/commits/1225dc4) **core**: create rotateKeys method in secret manager that allows to fix secrets when configured
- [e55e510](https://github.com/antistereov/singularity-content/commits/e55e510) **core**: move implementations of SecretService to packages they belong to
- [c7d6424](https://github.com/antistereov/singularity-content/commits/c7d6424) **core**: refactor core to make it more modular
- [4b88534](https://github.com/antistereov/singularity-content/commits/4b88534) **core**: refactor database package
- [57cc0e4](https://github.com/antistereov/singularity-content/commits/57cc0e4) **core**: move various packages
- [1a69c4e](https://github.com/antistereov/singularity-content/commits/1a69c4e) **core**: rename base package from io.stereov.singularity.core to io.stereov.singularity
- [040faf2](https://github.com/antistereov/singularity-content/commits/040faf2) **content, core**: improve function naming
- [7c49830](https://github.com/antistereov/singularity-content/commits/7c49830) **content**: rename ArticleOverviewDto to ArticleOverviewResponse
- [5a050d3](https://github.com/antistereov/singularity-content/commits/5a050d3) **integration-tests**: demo package is now called integration-tests
- [7a8fa9c](https://github.com/antistereov/singularity-content/commits/7a8fa9c) change base package for core to io.stereov.singularity.core
- [6bf953b](https://github.com/antistereov/singularity-content/commits/6bf953b) change base package to io.stereov.singularity
- [4e43f42](https://github.com/antistereov/singularity-content/commits/4e43f42) **core**: move InvalidDocumentException to global exceptions

## 🧪 Tests
- [daef01b](https://github.com/antistereov/singularity-content/commits/daef01b) **content**: fix tests after change in groups
- [1d18fe6](https://github.com/antistereov/singularity-content/commits/1d18fe6) **core**: fix tests after change in groups
- [f92d3c4](https://github.com/antistereov/singularity-content/commits/f92d3c4) **content**: add test for tag filtering
- [d8ed09b](https://github.com/antistereov/singularity-content/commits/d8ed09b) **content**: all tags will be deleted after each test
- [cb94458](https://github.com/antistereov/singularity-content/commits/cb94458) **content**: update tests after changes
- [cb25f75](https://github.com/antistereov/singularity-content/commits/cb25f75) **core**: fix tests after changes in core
- [4c161fa](https://github.com/antistereov/singularity-content/commits/4c161fa) **content**: create tests for getArticles
- [ae58bcd](https://github.com/antistereov/singularity-content/commits/ae58bcd) **content**: update and add missing tests for ArticleController and ArticleManagementController
- [357fe0b](https://github.com/antistereov/singularity-content/commits/357fe0b) **content**: update save function after changes in article and content API
- [deacd3e](https://github.com/antistereov/singularity-content/commits/deacd3e) **core**: make basePath final in BaseIntegrationTest
- [e957755](https://github.com/antistereov/singularity-content/commits/e957755) **core**: add groups to register function in BaseSpringBootTest
- [c7b5a19](https://github.com/antistereov/singularity-content/commits/c7b5a19) **core**: change all _id calls in UserDocument to id

## 🧰 Tasks
- [cc2bbf4](https://github.com/antistereov/singularity-content/commits/cc2bbf4) **core**: remove unused methods
- [e8b4629](https://github.com/antistereov/singularity-content/commits/e8b4629) **core**: remove unused imports
- [6557317](https://github.com/antistereov/singularity-content/commits/6557317) **content**: remove unused methods
- [d686527](https://github.com/antistereov/singularity-content/commits/d686527) **core**: remove unused imports
- [a4321c2](https://github.com/antistereov/singularity-content/commits/a4321c2) **core**: remove unused imports and methods
- [3857496](https://github.com/antistereov/singularity-content/commits/3857496) **content**: remove unused Dtos
- [0ffe227](https://github.com/antistereov/singularity-content/commits/0ffe227) **content**: code cleanup
- [3ca8980](https://github.com/antistereov/singularity-content/commits/3ca8980) **content**: remove unused imports
- [5fe014d](https://github.com/antistereov/singularity-content/commits/5fe014d) **code**: code cleanup
- [9ba5a30](https://github.com/antistereov/singularity-content/commits/9ba5a30) remove unused methods, classes and imports
- [7b209e0](https://github.com/antistereov/singularity-content/commits/7b209e0) **content**: set default value for description in CreateTagRequest
- [9c73a5e](https://github.com/antistereov/singularity-content/commits/9c73a5e) **content**: remove unused methods
- [49a3148](https://github.com/antistereov/singularity-content/commits/49a3148) **content**: code cleanup
- [26abd1e](https://github.com/antistereov/singularity-content/commits/26abd1e) **core**: code cleanup
- [1fbfab4](https://github.com/antistereov/singularity-content/commits/1fbfab4) **core**: code cleanup
- [0984279](https://github.com/antistereov/singularity-content/commits/0984279) **content**: remove unused method in ArticleService
- [9031451](https://github.com/antistereov/singularity-content/commits/9031451) **content**: remove unused ArticleContent model
- [4369a38](https://github.com/antistereov/singularity-content/commits/4369a38) **content**: remove unused methods and imports in ArticleRepository
- [357b7cb](https://github.com/antistereov/singularity-content/commits/357b7cb) **content**: remove unused imports
- [234ce08](https://github.com/antistereov/singularity-content/commits/234ce08) **core**: add more information in logging filter
- [465066c](https://github.com/antistereov/singularity-content/commits/465066c) **core**: LoggingFilter will now also show origin
- [15693d8](https://github.com/antistereov/singularity-content/commits/15693d8) **stereov-io**: log ip address of host in logging filter
- [2422e17](https://github.com/antistereov/singularity-content/commits/2422e17) **stereov-io**: remove unused imports

## 🛠  Build
- [7a8b87b](https://github.com/antistereov/singularity-content/commits/7a8b87b) fix location of sync workflows
- [285ba74](https://github.com/antistereov/singularity-content/commits/285ba74) update email config for github-actions bot
- [ac9f5c1](https://github.com/antistereov/singularity-content/commits/ac9f5c1) fix path in sync jobs
- [334b599](https://github.com/antistereov/singularity-content/commits/334b599) create sync job for content
- [612bf85](https://github.com/antistereov/singularity-content/commits/612bf85) remove redundant clean step
- [dea66f2](https://github.com/antistereov/singularity-content/commits/dea66f2) update release workflow to explicitly release selected libs
- [6a5db3b](https://github.com/antistereov/singularity-content/commits/6a5db3b) update workflows after creating new structure
- [349ee75](https://github.com/antistereov/singularity-content/commits/349ee75) **core**: bump version to 1.7.1-SNAPSHOT
- [aadc6b3](https://github.com/antistereov/singularity-content/commits/aadc6b3) **core**: bump version to 1.7.0
- [8d04d44](https://github.com/antistereov/singularity-content/commits/8d04d44) **core**: update release config
- [309199b](https://github.com/antistereov/singularity-content/commits/309199b) fix sync token issue
- [6beef76](https://github.com/antistereov/singularity-content/commits/6beef76) fix sync token issue
- [291f774](https://github.com/antistereov/singularity-content/commits/291f774) fix sync token issue
- [f1e7f27](https://github.com/antistereov/singularity-content/commits/f1e7f27) fix sync token issue
- [417f62f](https://github.com/antistereov/singularity-content/commits/417f62f) fix sync token issue
- [dfaa266](https://github.com/antistereov/singularity-content/commits/dfaa266) fix sync token issue
- [818df35](https://github.com/antistereov/singularity-content/commits/818df35) add and update workflows for sync to public repo
- [9876c6b](https://github.com/antistereov/singularity-content/commits/9876c6b) **stereov-io**: remove k8s infrastructure since it will be moved to infrastructure repo
- [3641a4c](https://github.com/antistereov/singularity-content/commits/3641a4c) **stereov-io**: update secret name and ingress annotations
- [6ba8f1c](https://github.com/antistereov/singularity-content/commits/6ba8f1c) **stereov-io**: add cert
- [e91e57c](https://github.com/antistereov/singularity-content/commits/e91e57c) **stereov-io**: change db to stereov
- [559c67b](https://github.com/antistereov/singularity-content/commits/559c67b) **stereov-io**: add deployment scripts for Kubernetes
- [f877ca0](https://github.com/antistereov/singularity-content/commits/f877ca0) **stereov-io**: use eclipse-temurin image as in includes glib in Dockerfile
- [0e5a876](https://github.com/antistereov/singularity-content/commits/0e5a876) **stereov-io**: add healthcheck and env variables to Dockerfile
- [e5cccfb](https://github.com/antistereov/singularity-content/commits/e5cccfb) **stereov-io**: create Dockerfile and automate image creation
- [c617288](https://github.com/antistereov/singularity-content/commits/c617288) bump version to 1.6.5-SNAPSHOT

## 📝 Documentation
- [3de8bba](https://github.com/antistereov/singularity-content/commits/3de8bba) **readme**: update file references in README.md
- [1907ad7](https://github.com/antistereov/singularity-content/commits/1907ad7) **readme**: remove dividers in README.md
- [e16a7ba](https://github.com/antistereov/singularity-content/commits/e16a7ba) **readme**: fix maven central batch
- [38954a9](https://github.com/antistereov/singularity-content/commits/38954a9) update import in development setup in README.md
- [5b7bf54](https://github.com/antistereov/singularity-content/commits/5b7bf54) update maven-central batch in README.md


## Contributors
We'd like to thank the following people for their contributions:
André Antimonov, GitHub, antistereov

## [v1.6.2]

## Changelog

## 🐛 Fixes
- [f64c2e6](https://github.com/antistereov/web-kotlin-spring-baseline/commits/f64c2e6) **mail**: several minor fixes in mail services

## 🧰 Tasks
- [1ec9d66](https://github.com/antistereov/web-kotlin-spring-baseline/commits/1ec9d66) update version to 1.6.2-SNAPSHOT

## 🛠  Build
- [b6b4db6](https://github.com/antistereov/web-kotlin-spring-baseline/commits/b6b4db6) bump version to 1.6.2
- [37d7a0b](https://github.com/antistereov/web-kotlin-spring-baseline/commits/37d7a0b) add changelog task to automate release process

## 📝 Documentation
- [3ad521e](https://github.com/antistereov/web-kotlin-spring-baseline/commits/3ad521e) **changelog**: add changelog entries for version 1.6.1


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.6.1]

## Changelog

## 🐛 Fixes
- [f9f228a](https://github.com/antistereov/web-kotlin-spring-baseline/commits/f9f228a) add expiration when caching secrets and make KeyManager async by using suspend in IO operations

## 🧰 Tasks
- [dba2fe3](https://github.com/antistereov/web-kotlin-spring-baseline/commits/dba2fe3) update version to 1.6.1
- [d7db761](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d7db761) update version to 1.6.1-SNAPSHOT

## 📝 Documentation
- [75e128c](https://github.com/antistereov/web-kotlin-spring-baseline/commits/75e128c) **changelog**: update CHANGELOG.md for version 1.6.0


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.6.0]

## Changelog

## 🚀 Features
- [be49273](https://github.com/antistereov/web-kotlin-spring-baseline/commits/be49273) create abstract SecretService to split responsibilities between key manager and secret manager
- [3398917](https://github.com/antistereov/web-kotlin-spring-baseline/commits/3398917) create admin user on startup
- [93961b3](https://github.com/antistereov/web-kotlin-spring-baseline/commits/93961b3) implement endpoints for admin and move schedule for key rotation to AdminService

## 🐛 Fixes
- [8aad1e8](https://github.com/antistereov/web-kotlin-spring-baseline/commits/8aad1e8) **secret-service**: fix loading of current secret

## 🧪 Tests
- [a55d88d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/a55d88d) **jwt**: simplify test
- [0bc90e3](https://github.com/antistereov/web-kotlin-spring-baseline/commits/0bc90e3) **jwt-service**: create tests for jwt decoding and encoding

## 🧰 Tasks
- [d0413ae](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d0413ae) update version to 1.6.0
- [60fb426](https://github.com/antistereov/web-kotlin-spring-baseline/commits/60fb426) use Instant serializer for all Instant fields
- [64b7d89](https://github.com/antistereov/web-kotlin-spring-baseline/commits/64b7d89) remove unused imports
- [b073ccb](https://github.com/antistereov/web-kotlin-spring-baseline/commits/b073ccb) remove unused imports
- [2a634f2](https://github.com/antistereov/web-kotlin-spring-baseline/commits/2a634f2) **token-service**: improve naming
- [4dfc163](https://github.com/antistereov/web-kotlin-spring-baseline/commits/4dfc163) **token-service**: add todo
- [87db6e2](https://github.com/antistereov/web-kotlin-spring-baseline/commits/87db6e2) **admin-service**: key rotation is now async and does not block thread
- [d8e75d0](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d8e75d0) **user-session-service**: sending verification email is now async and does not block response

## 🛠  Build
- [2364cc1](https://github.com/antistereov/web-kotlin-spring-baseline/commits/2364cc1) bump version to 1.5.1-SNAPSHOT

## 📝 Documentation
- [f193a10](https://github.com/antistereov/web-kotlin-spring-baseline/commits/f193a10) **changelog**: update CHANGELOG.md for 1.5.0


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.5.0]

## Changelog

## 🚀 Features
- [e42130e](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e42130e) enable key rotation for jwt
- [d2f8475](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d2f8475) create abstract classes to simplify handling encryption
- [9ec8ac9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/9ec8ac9) **store**: implement a KeyManager interface to enable future implementations of key rotation
- [4c423ad](https://github.com/antistereov/web-kotlin-spring-baseline/commits/4c423ad) various improvements
- [8c7307f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/8c7307f) implement S3 compatibility

## 🐛 Fixes
- [7d9129c](https://github.com/antistereov/web-kotlin-spring-baseline/commits/7d9129c) fix encryption secret not being set after initialization of BitwardenKeyManager
- [8080347](https://github.com/antistereov/web-kotlin-spring-baseline/commits/8080347) fix some bugs that were introduced by the new encryption strategy
- [c0b7b78](https://github.com/antistereov/web-kotlin-spring-baseline/commits/c0b7b78) **s3-file-storage**: fix calculation of file size
- [055d6aa](https://github.com/antistereov/web-kotlin-spring-baseline/commits/055d6aa) **file**: fix file upload and download and add static resources

## 🧪 Tests
- [8f7e20e](https://github.com/antistereov/web-kotlin-spring-baseline/commits/8f7e20e) explicitly set application property for domain
- [df1a76c](https://github.com/antistereov/web-kotlin-spring-baseline/commits/df1a76c) fix tests after configuration update

## 🧰 Tasks
- [060b2d0](https://github.com/antistereov/web-kotlin-spring-baseline/commits/060b2d0) remove unused EnvKeyManager
- [e419d9f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e419d9f) enable scheduled key rotation and update properties
- [38cae45](https://github.com/antistereov/web-kotlin-spring-baseline/commits/38cae45) remove done todos
- [83d22ad](https://github.com/antistereov/web-kotlin-spring-baseline/commits/83d22ad) **encryption**: make encryption ready for key-rotation
- [de38ef6](https://github.com/antistereov/web-kotlin-spring-baseline/commits/de38ef6) **file**: add information about upload date
- [63d78f9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/63d78f9) add slug getter to AppProperties
- [7fead50](https://github.com/antistereov/web-kotlin-spring-baseline/commits/7fead50) **file-storage**: create interface for FileStorage and remove necessity for temp file in upload
- [38b2a70](https://github.com/antistereov/web-kotlin-spring-baseline/commits/38b2a70) remove unused classes
- [2e8bc32](https://github.com/antistereov/web-kotlin-spring-baseline/commits/2e8bc32) **s3**: make some changes to file handling
- [dab933f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/dab933f) **two-factor**: recovery now sets step-up token automatically, so no more 2FA is needed to deactivate 2FA

## 🛠  Build
- [f8c7ee9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/f8c7ee9) bump version to 1.5.0
- [c466f22](https://github.com/antistereov/web-kotlin-spring-baseline/commits/c466f22) bump version to 1.4.1-SNAPSHOT

## 📝 Documentation
- [64012ba](https://github.com/antistereov/web-kotlin-spring-baseline/commits/64012ba) **changelog**: update CHANGELOG.md for 1.4.0


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.4.0]

## Changelog

## 🚀 Features
- [425686d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/425686d) **file**: implement FileService

## 🐛 Fixes
- [72cfbcc](https://github.com/antistereov/web-kotlin-spring-baseline/commits/72cfbcc) **two-factor**: disabling 2FA requires password now

## 🛠  Build
- [d721e9d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d721e9d) bump version to 1.4.0
- [b4b3429](https://github.com/antistereov/web-kotlin-spring-baseline/commits/b4b3429) bump version to 1.3.2-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.3.1]

## Changelog

## 🧰 Tasks
- [04d4fde](https://github.com/antistereov/web-kotlin-spring-baseline/commits/04d4fde) **two-factor**: step-up endpoints now also return TwoFactorStatusResponse

## 🛠  Build
- [116f464](https://github.com/antistereov/web-kotlin-spring-baseline/commits/116f464) bump version to 1.3.1
- [c04b08d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/c04b08d) bump version to 1.3.1-SNAPSHOT

## 📝 Documentation
- [0722b44](https://github.com/antistereov/web-kotlin-spring-baseline/commits/0722b44) **changelog**: update CHANGELOG.md for v1.3.0


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.3.0]

## Changelog

## 🚀 Features
- [4289713](https://github.com/antistereov/web-kotlin-spring-baseline/commits/4289713) **two-factor**: change 2FA recovery design

## 🧰 Tasks
- [a43cadd](https://github.com/antistereov/web-kotlin-spring-baseline/commits/a43cadd) remove unused imports and update docstrings

## 🛠  Build
- [295a604](https://github.com/antistereov/web-kotlin-spring-baseline/commits/295a604) bump version to 1.3.0
- [2f1e020](https://github.com/antistereov/web-kotlin-spring-baseline/commits/2f1e020) bump version to 1.2.1-SNAPSHOT

## 📝 Documentation
- [e217ed0](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e217ed0) **changelog**: update CHANGELOG.md for v1.2.0


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.2.0]

## Changelog

## 🚀 Features
- [6ecabfd](https://github.com/antistereov/web-kotlin-spring-baseline/commits/6ecabfd) **two-factor-auth**: all access tokens are cleared when 2FA gets enabled

## 🐛 Fixes
- [37c33b2](https://github.com/antistereov/web-kotlin-spring-baseline/commits/37c33b2) **two-factor-auth**: setup does not automatically enable 2FA - verification is needed

## 🧪 Tests
- [a73510c](https://github.com/antistereov/web-kotlin-spring-baseline/commits/a73510c) **two-factor-auth**: fix tests for new 2fa setup

## 🛠  Build
- [e2a6bbb](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e2a6bbb) bump version to 1.2.0
- [46560d1](https://github.com/antistereov/web-kotlin-spring-baseline/commits/46560d1) bump version to 1.1.4-SNAPSHOT

## 📝 Documentation
- [08ca557](https://github.com/antistereov/web-kotlin-spring-baseline/commits/08ca557) **changelog**: update changelog for v1.1.3


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.1.3]

## Changelog

## 🚀 Features
- [dd220ff](https://github.com/antistereov/web-kotlin-spring-baseline/commits/dd220ff) **two-factor-auth**: setup different endpoints for login and step up

## 🛠  Build
- [1e5e556](https://github.com/antistereov/web-kotlin-spring-baseline/commits/1e5e556) bump version to 1.1.3
- [d9d195f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d9d195f) bump version to 1.1.3-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.1.2]

## Changelog

## 🚀 Features
- [7448c50](https://github.com/antistereov/web-kotlin-spring-baseline/commits/7448c50) add token for step-up which is required for critical changes to the account, e.g., changing the password

## 🧰 Tasks
- [f1c5c45](https://github.com/antistereov/web-kotlin-spring-baseline/commits/f1c5c45) remove unused methods
- [0b7f74b](https://github.com/antistereov/web-kotlin-spring-baseline/commits/0b7f74b) remove unused variables in test

## 🛠  Build
- [adad77e](https://github.com/antistereov/web-kotlin-spring-baseline/commits/adad77e) remove automated update of changelog
- [b68de56](https://github.com/antistereov/web-kotlin-spring-baseline/commits/b68de56) bump version to 1.1.2
- [b3fdf79](https://github.com/antistereov/web-kotlin-spring-baseline/commits/b3fdf79) remove an unused id in verification step of release workflow
- [79d3d8d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/79d3d8d) fix release workflow
- [d20e1cf](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d20e1cf) add automated update of changelog after release
- [96ab591](https://github.com/antistereov/web-kotlin-spring-baseline/commits/96ab591) update jrealeaser config
- [d80e74a](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d80e74a) bump version to 1.1.2-SNAPSHOT

## 📝 Documentation
- [7f68893](https://github.com/antistereov/web-kotlin-spring-baseline/commits/7f68893) **readme**: update README.md
- [90abb1d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/90abb1d) update CHANGELOG.md


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.1.1]

## Changelog

## 🛠  Build
- [5f9d874](https://github.com/antistereov/web-kotlin-spring-baseline/commits/5f9d874) bump version to 1.1.1
- [16891f6](https://github.com/antistereov/web-kotlin-spring-baseline/commits/16891f6) fix release script
- [3a6f9e0](https://github.com/antistereov/web-kotlin-spring-baseline/commits/3a6f9e0) bump version to 1.1.1-SNAPSHOT

## 📝 Documentation
- [732dbed](https://github.com/antistereov/web-kotlin-spring-baseline/commits/732dbed) update CHANGELOG.md


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.1.0]

## Changelog

## 🛠  Build
- [38633e2](https://github.com/antistereov/web-kotlin-spring-baseline/commits/38633e2) bump version to 1.1.0
- [327a2f0](https://github.com/antistereov/web-kotlin-spring-baseline/commits/327a2f0) fix release workflow
- [d13aed1](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d13aed1) bump version to 1.0.7-SNAPSHOT

## 📝 Documentation
- [0d4e07b](https://github.com/antistereov/web-kotlin-spring-baseline/commits/0d4e07b) update CHANGELOG.md


## Contributors
We'd like to thank the following people for their contributions:
GitHub, Stereov, antistereov

## [v1.0.6]

## Changelog

## 🛠  Build
- [87dfc1b](https://github.com/antistereov/web-kotlin-spring-baseline/commits/87dfc1b) bump version 1.0.6
- [3991351](https://github.com/antistereov/web-kotlin-spring-baseline/commits/3991351) update jreleaser config
- [0ec5e79](https://github.com/antistereov/web-kotlin-spring-baseline/commits/0ec5e79) update release and deploy script
- [0a55940](https://github.com/antistereov/web-kotlin-spring-baseline/commits/0a55940) bump version to 1.0.6-SNAPSHOT

## 📝 Documentation
- [9bc9d3b](https://github.com/antistereov/web-kotlin-spring-baseline/commits/9bc9d3b) update CHANGELOG.md


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.0.5]

## Changelog

## 🐛 Fixes
- 9632796 fix dependencies and logger configuration for tests

## 🧪 Tests
- 22da44b update configs in tests

## 🛠  Build
- eff13ce bump version to 1.0.5
- a4efeac increase max number of retries for deployment to maven central
- 16c8dc9 update ci workflows
- c4c51aa move test dependencies to demo project
- 0820f3d fix build and test workflow
- ec2e8ab fix build and test workflow
- 75b6d3c fix build step
- c712e0b bump version to 1.0.5-SNAPSHOT

## 📝 Documentation
- cccaaee update changelog and changelog config
- 62b8774 **readme**: update README.md
- c1bfaae **changelog**: update CHANGELOG.md


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.0.4]

## Changelog

## 🐛 Fixes
- a3f6eb4 fix dependencies and add log4j2 logging

## 🔄️ Changes
- 955f347 refactor .env.sample and set default application properties


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [1.0.3]

## Changelog

## 🚀 Features
- [3d2310e](https://github.com/antistereov/web-kotlin-spring-baseline/commits/3d2310e) update configuration properties and add default values

## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [1.0.2]

## Changelog

## 🐛 Fixes
- [35f7e5d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/35f7e5d) add bean for TwoFactorAuthTokenService

## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [1.0.1]

## Changelog

## 🐛 Fixes
- [669f7d6](https://github.com/antistereov/web-kotlin-spring-baseline/commits/669f7d6) add missing bean creations


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [1.0.0]

## Changelog

## 🚀 Features
- [6a4513d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/6a4513d) add login attempt limits and refactor rate limits
- [aac778f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/aac778f) add caching of valid access tokens
- [1c28312](https://github.com/antistereov/web-kotlin-spring-baseline/commits/1c28312) implement password reset
- [558ca91](https://github.com/antistereov/web-kotlin-spring-baseline/commits/558ca91) simplify paths in mail controller
- [abefd41](https://github.com/antistereov/web-kotlin-spring-baseline/commits/abefd41) mail is now optional
- [1e9c1dd](https://github.com/antistereov/web-kotlin-spring-baseline/commits/1e9c1dd) create dto for email verification cooldown
- [93430f9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/93430f9) **two-factor-auth**: implement recovery
- [7645206](https://github.com/antistereov/web-kotlin-spring-baseline/commits/7645206) implement two-factor authentication
- [e1ec3c2](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e1ec3c2) user model is now more robust and can be used for multiple different projects
- [421ca19](https://github.com/antistereov/web-kotlin-spring-baseline/commits/421ca19) initialize library
- [81c99cf](https://github.com/antistereov/web-kotlin-spring-baseline/commits/81c99cf) initialize repository

## 🐛 Fixes
- [9395f4d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/9395f4d) change frontend email verification path
- [47e09d4](https://github.com/antistereov/web-kotlin-spring-baseline/commits/47e09d4) remove default values for backend and mail properties since there is an issue with config generation otherwise
- [58c78de](https://github.com/antistereov/web-kotlin-spring-baseline/commits/58c78de) try to resolve annotation processing

## 🔄️ Changes
- [2fa6f29](https://github.com/antistereov/web-kotlin-spring-baseline/commits/2fa6f29) create RateLimitService and remove login attempt check based on body
- [710b16f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/710b16f) make GoogleAuthenticator a bean instead of singleton inside TwoFactorAuthService
- [b673fe9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/b673fe9) make mail verification truly optional and edit configuration constraints
- [e50551f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e50551f) change project structure to make it more readable
- [e87be17](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e87be17) make naming of dtos consistent
- [864f084](https://github.com/antistereov/web-kotlin-spring-baseline/commits/864f084) simplify code and add tests
- [f768861](https://github.com/antistereov/web-kotlin-spring-baseline/commits/f768861) create separate configuration classes for easier maintainability
- [4dc1840](https://github.com/antistereov/web-kotlin-spring-baseline/commits/4dc1840) remove idea config from repo
- [c212780](https://github.com/antistereov/web-kotlin-spring-baseline/commits/c212780) update gitignore
- [532c3e3](https://github.com/antistereov/web-kotlin-spring-baseline/commits/532c3e3) remove unused tests and set version to snapshot
- [30c2759](https://github.com/antistereov/web-kotlin-spring-baseline/commits/30c2759) create module for autoconfiguration and for starter


## Contributors
We'd like to thank the following people for their contributions:
GitHub, Stereov, antistereov

## [0.2.0]

## Changelog

## 🚀 Features
- [558ca91](https://github.com/antistereov/web-kotlin-spring-baseline/commits/558ca91) simplify paths in mail controller
- [abefd41](https://github.com/antistereov/web-kotlin-spring-baseline/commits/abefd41) mail is now optional
- [1e9c1dd](https://github.com/antistereov/web-kotlin-spring-baseline/commits/1e9c1dd) create dto for email verification cooldown
- [93430f9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/93430f9) **two-factor-auth**: implement recovery
- [7645206](https://github.com/antistereov/web-kotlin-spring-baseline/commits/7645206) implement two-factor authentication
- [e1ec3c2](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e1ec3c2) user model is now more robust and can be used for multiple different projects
- [421ca19](https://github.com/antistereov/web-kotlin-spring-baseline/commits/421ca19) initialize library
- [81c99cf](https://github.com/antistereov/web-kotlin-spring-baseline/commits/81c99cf) initialize repository

## 🐛 Fixes
- [9395f4d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/9395f4d) change frontend email verification path
- [47e09d4](https://github.com/antistereov/web-kotlin-spring-baseline/commits/47e09d4) remove default values for backend and mail properties since there is an issue with config generation otherwise
- [58c78de](https://github.com/antistereov/web-kotlin-spring-baseline/commits/58c78de) try to resolve annotation processing

## 🔄️ Changes
- [b673fe9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/b673fe9) make mail verification truly optional and edit configuration constraints
- [e50551f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e50551f) change project structure to make it more readable
- [e87be17](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e87be17) make naming of dtos consistent
- [864f084](https://github.com/antistereov/web-kotlin-spring-baseline/commits/864f084) simplify code and add tests
- [f768861](https://github.com/antistereov/web-kotlin-spring-baseline/commits/f768861) create separate configuration classes for easier maintainability
- [4dc1840](https://github.com/antistereov/web-kotlin-spring-baseline/commits/4dc1840) remove idea config from repo
- [c212780](https://github.com/antistereov/web-kotlin-spring-baseline/commits/c212780) update gitignore
- [532c3e3](https://github.com/antistereov/web-kotlin-spring-baseline/commits/532c3e3) remove unused tests and set version to snapshot
- [30c2759](https://github.com/antistereov/web-kotlin-spring-baseline/commits/30c2759) create module for autoconfiguration and for starter


## Contributors
We'd like to thank the following people for their contributions:
GitHub, Stereov, antistereov

## [0.1.8]

## Changelog

## 🚀 Features
- [1e9c1dd](https://github.com/antistereov/web-kotlin-spring-baseline/commits/1e9c1dd) create dto for email verification cooldown
- [93430f9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/93430f9) **two-factor-auth**: implement recovery
- [7645206](https://github.com/antistereov/web-kotlin-spring-baseline/commits/7645206) implement two-factor authentication
- [e1ec3c2](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e1ec3c2) user model is now more robust and can be used for multiple different projects
- [421ca19](https://github.com/antistereov/web-kotlin-spring-baseline/commits/421ca19) initialize library
- [81c99cf](https://github.com/antistereov/web-kotlin-spring-baseline/commits/81c99cf) initialize repository

## 🐛 Fixes
- [9395f4d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/9395f4d) change frontend email verification path
- [47e09d4](https://github.com/antistereov/web-kotlin-spring-baseline/commits/47e09d4) remove default values for backend and mail properties since there is an issue with config generation otherwise
- [58c78de](https://github.com/antistereov/web-kotlin-spring-baseline/commits/58c78de) try to resolve annotation processing

## 🔄️ Changes
- [e50551f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e50551f) change project structure to make it more readable
- [e87be17](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e87be17) make naming of dtos consistent
- [864f084](https://github.com/antistereov/web-kotlin-spring-baseline/commits/864f084) simplify code and add tests
- [f768861](https://github.com/antistereov/web-kotlin-spring-baseline/commits/f768861) create separate configuration classes for easier maintainability
- [4dc1840](https://github.com/antistereov/web-kotlin-spring-baseline/commits/4dc1840) remove idea config from repo
- [c212780](https://github.com/antistereov/web-kotlin-spring-baseline/commits/c212780) update gitignore
- [532c3e3](https://github.com/antistereov/web-kotlin-spring-baseline/commits/532c3e3) remove unused tests and set version to snapshot
- [30c2759](https://github.com/antistereov/web-kotlin-spring-baseline/commits/30c2759) create module for autoconfiguration and for starter


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [0.1.7]

## Changelog

## 🚀 Features
- [1e9c1dd](https://github.com/antistereov/web-kotlin-spring-baseline/commits/1e9c1dd) create dto for email verification cooldown
- [93430f9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/93430f9) **two-factor-auth**: implement recovery
- [7645206](https://github.com/antistereov/web-kotlin-spring-baseline/commits/7645206) implement two-factor authentication
- [e1ec3c2](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e1ec3c2) user model is now more robust and can be used for multiple different projects
- [421ca19](https://github.com/antistereov/web-kotlin-spring-baseline/commits/421ca19) initialize library
- [81c99cf](https://github.com/antistereov/web-kotlin-spring-baseline/commits/81c99cf) initialize repository

## 🐛 Fixes
- [9395f4d](https://github.com/antistereov/web-kotlin-spring-baseline/commits/9395f4d) change frontend email verification path
- [47e09d4](https://github.com/antistereov/web-kotlin-spring-baseline/commits/47e09d4) remove default values for backend and mail properties since there is an issue with config generation otherwise
- [58c78de](https://github.com/antistereov/web-kotlin-spring-baseline/commits/58c78de) try to resolve annotation processing

## 🔄️ Changes
- [e50551f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e50551f) change project structure to make it more readable
- [e87be17](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e87be17) make naming of dtos consistent
- [864f084](https://github.com/antistereov/web-kotlin-spring-baseline/commits/864f084) simplify code and add tests
- [f768861](https://github.com/antistereov/web-kotlin-spring-baseline/commits/f768861) create separate configuration classes for easier maintainability
- [4dc1840](https://github.com/antistereov/web-kotlin-spring-baseline/commits/4dc1840) remove idea config from repo
- [c212780](https://github.com/antistereov/web-kotlin-spring-baseline/commits/c212780) update gitignore
- [532c3e3](https://github.com/antistereov/web-kotlin-spring-baseline/commits/532c3e3) remove unused tests and set version to snapshot
- [30c2759](https://github.com/antistereov/web-kotlin-spring-baseline/commits/30c2759) create module for autoconfiguration and for starter


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [0.1.6]

## Changelog

## 🔄️ Changes
- [e50551f](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e50551f) change project structure to make it more readable


## Contributors
We'd like to thank the following people for their contributions:
antistereov
