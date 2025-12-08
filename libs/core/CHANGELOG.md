## [v1.10.2]

## Changelog

## 🐛 Fixes
- [b015917](https://github.com/antistereov/singularity/commits/b015917) **auth**: change error code for wrong totp recovery code to WRONG_TOTP_RECOVERY_CODE


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.10.1]

## Changelog

## 📝 Documentation
- [821db44](https://github.com/antistereov/singularity/commits/821db44) **openapi**: add error response


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.10.0]

## Changelog

## 📝 Documentation
- [23aa410](https://github.com/antistereov/singularity/commits/23aa410) **core**: fix broken operation names


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.9.6]

## Changelog

## 🛠  Build
- [dde8ffa](https://github.com/antistereov/singularity/commits/dde8ffa) bump version to 1.9.5-SNAPSHOT


## Contributors
We'd like to thank the following people for their contributions:
André Antimonov, GitHub

## [v1.9.4]

## Changelog


## Contributors
We'd like to thank the following people for their contributions:
André Antimonov, GitHub

## [v1.9.3]

## Changelog


## Contributors
We'd like to thank the following people for their contributions:
André Antimonov, GitHub

## [v1.9.2]

## Changelog

## 📝 Documentation
- [4949d4b](https://github.com/antistereov/singularity-core/commits/4949d4b) **openapi**: update api docs after changes in two factor method serialization


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.9.1]

## Changelog

## 🚀 Features
- [e677c3e](https://github.com/antistereov/singularity-core/commits/e677c3e) **auth**: unify naming of property for enabled 2fa methods in responses


## Contributors
We'd like to thank the following people for their contributions:
antistereov

## [v1.9.0]

## Changelog

## 🚀 Features
- [a3d9be4](https://github.com/antistereov/singularity-core/commits/a3d9be4) remove unnecessary file metadata implementation of content service
- [77eebe0](https://github.com/antistereov/singularity-core/commits/77eebe0) test article management
- [5899010](https://github.com/antistereov/singularity-core/commits/5899010) implement deletion of avatars and article images when deleting and fixing bugs in content management
- [b0d4813](https://github.com/antistereov/singularity-core/commits/b0d4813) **content**: update tags and invitations, fix some logic errors, update docs and test
- [1fae890](https://github.com/antistereov/singularity-core/commits/1fae890) **content**: rename ADMIN role to MAINTAINER to make the difference between server admin and content maintainer clear
- [e0136c4](https://github.com/antistereov/singularity-core/commits/e0136c4) **files**: improve new file handling and update tests
- [1676ba2](https://github.com/antistereov/singularity-core/commits/1676ba2) **files**: implement upload of images and automated conversion of images to multiple sizes
- [5364ad5](https://github.com/antistereov/singularity-core/commits/5364ad5) **content**: remove unused fields, improve tag controller and add openapi documentation
- [3282530](https://github.com/antistereov/singularity-core/commits/3282530) **content**: improve content model, create content controller that collects methods and improve creation of queries
- [75898d5](https://github.com/antistereov/singularity-core/commits/75898d5) **auth**: groups keys are fixed now and cannot be updated to maintain consistency and reduce complexity
- [9ebcc58](https://github.com/antistereov/singularity-core/commits/9ebcc58) **security**: implement security alerts for login and profile changes
- [320f320](https://github.com/antistereov/singularity-core/commits/320f320) **users**: improve code, add documentation and test
- [80ff7d2](https://github.com/antistereov/singularity-core/commits/80ff7d2) **users**: improve code, add documentation and test
- [6daf356](https://github.com/antistereov/singularity-core/commits/6daf356) **database**: improve design and add documentation
- [d0d8866](https://github.com/antistereov/singularity-core/commits/d0d8866) **oauth2**: implement custom failure handler and create base test
- [e5d7cd7](https://github.com/antistereov/singularity-core/commits/e5d7cd7) **oauth2**: implement state token
- [f76dd21](https://github.com/antistereov/singularity-core/commits/f76dd21) **auth**: improve oauth2 code, update openapi and add tests
- [834b12e](https://github.com/antistereov/singularity-core/commits/834b12e) **auth**: improve 2fa, add docs and test
- [df57de8](https://github.com/antistereov/singularity-core/commits/df57de8) **auth**: improve 2fa handling and add documentation and tests
- [6f587b5](https://github.com/antistereov/singularity-core/commits/6f587b5) **auth**: improve 2fa handling and add documentation
- [214bb90](https://github.com/antistereov/singularity-core/commits/214bb90) **auth**: minor fixes for groups and add tests
- [35a8fe8](https://github.com/antistereov/singularity-core/commits/35a8fe8) **auth**: implement, test and document endpoints for roles GUEST and ADMIN
- [359241c](https://github.com/antistereov/singularity-core/commits/359241c) **translate**: improve retrieving of resource keys
- [1e08399](https://github.com/antistereov/singularity-core/commits/1e08399) **auth**: improve handling of 2FA already enabled
- [bc8ebe0](https://github.com/antistereov/singularity-core/commits/bc8ebe0) **user**: rename mail property in user security detail to email
- [2567c29](https://github.com/antistereov/singularity-core/commits/2567c29) **content**: update AccessCriteria to use access token and does not request database
- [6adf86c](https://github.com/antistereov/singularity-core/commits/6adf86c) **auth**: create new geolocation response to transfer less data
- [a17f412](https://github.com/antistereov/singularity-core/commits/a17f412) **auth**: minor fixes in AuthenticationController and update openapi docs
- [69ed45a](https://github.com/antistereov/singularity-core/commits/69ed45a) **auth**: minor fixes in AuthenticationController and update openapi docs
- [5f03f27](https://github.com/antistereov/singularity-core/commits/5f03f27) **auth**: password reset requires an email instead of access tokens now and update openapi docs
- [0054dc4](https://github.com/antistereov/singularity-core/commits/0054dc4) **auth**: remove caching the user document in the AuthorizationService since the service is not bound to a session
- [b8f452c](https://github.com/antistereov/singularity-core/commits/b8f452c) **groups**: adding or removing users from groups invalidates the users' access and refresh tokens
- [acce620](https://github.com/antistereov/singularity-core/commits/acce620) **user**: rename mail property in user security detail to email
- [15446fc](https://github.com/antistereov/singularity-core/commits/15446fc) **openapi**: trim indents for info, summaries and descriptions to make it valid
- [c327c6d](https://github.com/antistereov/singularity-core/commits/c327c6d) **cache**: improve CacheService and make AccessTokenCache use CacheService
- [42c8926](https://github.com/antistereov/singularity-core/commits/42c8926) **translation**: create tag request needs locale instead of language tag
- [92b6deb](https://github.com/antistereov/singularity-core/commits/92b6deb) **translation**: user locale instead of custom language class and implement default locale
- [49a6bd8](https://github.com/antistereov/singularity-core/commits/49a6bd8) **demo**: add OpenAPI description
- [9e5f409](https://github.com/antistereov/singularity-core/commits/9e5f409) **openapi**: add admin scope
- [3898e57](https://github.com/antistereov/singularity-core/commits/3898e57) **openapi**: add customizer to fix security requirement specification with header and cookie and sort tags alphabetically
- [50cfb11](https://github.com/antistereov/singularity-core/commits/50cfb11) **auth**: move password reset and email verification properties to their own property classes
- [0b50cb1](https://github.com/antistereov/singularity-core/commits/0b50cb1) **2fa**: update configuration to make it more readable and add OpenAPI documentation
- [537fa2e](https://github.com/antistereov/singularity-core/commits/537fa2e) **2fa**: implement methods to enable and disable mail as 2fa method and to set preferred 2fa method
- [8a449a9](https://github.com/antistereov/singularity-core/commits/8a449a9) **auth**: refresh tokens will now be accepted in the authorization header
- [7080a2b](https://github.com/antistereov/singularity-core/commits/7080a2b) **cache**: make CacheService take value of any Type instead of just String
- [20786fc](https://github.com/antistereov/singularity-core/commits/20786fc) **auth**: invalidate all AccessTokens related to one session if session is invalidated
- [6c1daa4](https://github.com/antistereov/singularity-core/commits/6c1daa4) **auth**: make AccessToken stateless and remove necessity to use session token
- [e55837c](https://github.com/antistereov/singularity-core/commits/e55837c) **auth**: remove necessity to generate a sessionId on the client and move logic to server fully
- [5e2831b](https://github.com/antistereov/singularity-core/commits/5e2831b) **oauth2**: improve error handling and readability
- [184ea0b](https://github.com/antistereov/singularity-core/commits/184ea0b) **auth**: trust email verification of oauth2 provider when creating
- [5a432b4](https://github.com/antistereov/singularity-core/commits/5a432b4) **auth**: implement reauthentication with same account
- [bdb307e](https://github.com/antistereov/singularity-core/commits/bdb307e) **auth**: implement error handling for oauth2 authentication errors
- [673d045](https://github.com/antistereov/singularity-core/commits/673d045) **auth**: remove unnecessary usages of SessionInfoRequest since it is already saved in the access token
- [dfb5a05](https://github.com/antistereov/singularity-core/commits/dfb5a05) **oauth2**: make oauth2 authentication configurable
- [4ddf06b](https://github.com/antistereov/singularity-core/commits/4ddf06b) **auth**: implement connection and handling of identity providers
- [499cbff](https://github.com/antistereov/singularity-core/commits/499cbff) **oauth2**: set custom state parameter
- [c5fe36a](https://github.com/antistereov/singularity-core/commits/c5fe36a) **oauth2**: implement oauth2
- [4411122](https://github.com/antistereov/singularity-core/commits/4411122) **2fa**: improve status request
- [45a18b2](https://github.com/antistereov/singularity-core/commits/45a18b2) **auth**: increase size of login code in email template
- [b88e7b2](https://github.com/antistereov/singularity-core/commits/b88e7b2) **auth**: update step up flow
- [9d58094](https://github.com/antistereov/singularity-core/commits/9d58094) improve naming and update references in docs
- [0d1a6ff](https://github.com/antistereov/singularity-core/commits/0d1a6ff) update endpoints to match REST standards and update structure
- [dfe5a82](https://github.com/antistereov/singularity-core/commits/dfe5a82) **auth**: failing mmdb download now shows a warning instead of throwing an error
- [ee96bfe](https://github.com/antistereov/singularity-core/commits/ee96bfe) unify token declaration and add new interface SecurityToken
- [a97369c](https://github.com/antistereov/singularity-core/commits/a97369c) **auth**: update token definitions and openapi documentation for security
- [6519997](https://github.com/antistereov/singularity-core/commits/6519997) **two-factor**: move properties from security to two-factor
- [4449be3](https://github.com/antistereov/singularity-core/commits/4449be3) **auth**: improve handling of tokens
- [cd5084b](https://github.com/antistereov/singularity-core/commits/cd5084b) **auth**: implement expiration for refresh token and add tests
- [7cae32c](https://github.com/antistereov/singularity-core/commits/7cae32c) **groups**: add group member controller and update package structure
- [68b7288](https://github.com/antistereov/singularity-core/commits/68b7288) **security**: add security package and create properties to specify allowed origins
- [f930798](https://github.com/antistereov/singularity-core/commits/f930798) **app**: update application properties
- [7756bc6](https://github.com/antistereov/singularity-core/commits/7756bc6) **geolocation**: update name
- [3137c43](https://github.com/antistereov/singularity-core/commits/3137c43) **geolocation**: update name
- [aea7d64](https://github.com/antistereov/singularity-core/commits/aea7d64) **geolocation**: rename property for real ip header
- [3426277](https://github.com/antistereov/singularity-core/commits/3426277) **geolocation**: refine usage of geolocation
- [fc3c9e6](https://github.com/antistereov/singularity-core/commits/fc3c9e6) **geolocation**: fix download of database and setting of information
- [b234c17](https://github.com/antistereov/singularity-core/commits/b234c17) **core**: update client ip fetching
- [9c984f3](https://github.com/antistereov/singularity-core/commits/9c984f3) **global**: update configuration for webclient
- [8fec9e0](https://github.com/antistereov/singularity-core/commits/8fec9e0) **geolocation**: configure usage of geolocation in demo and stereov-io
- [062e82b](https://github.com/antistereov/singularity-core/commits/062e82b) **geolocation**: implement automated download and update of database
- [d493358](https://github.com/antistereov/singularity-core/commits/d493358) **geolocation**: use maxmind geolite2-city database for location information
- [f7ff473](https://github.com/antistereov/singularity-core/commits/f7ff473) **auth**: enable header authorization by default
- [f6212b6](https://github.com/antistereov/singularity-core/commits/f6212b6) **demo**: add custom configuration options using env variables
- [af0d463](https://github.com/antistereov/singularity-core/commits/af0d463) **demo**: use redoc instead of swagger
- [14c32b9](https://github.com/antistereov/singularity-core/commits/14c32b9) **demo**: use openapi docs from endpoint of demo application instead of fixed one to save unnecessary build steps
- [2ab24a4](https://github.com/antistereov/singularity-core/commits/2ab24a4) **user**: email will not be marked as verified when mail is deactivated
- [c94aff2](https://github.com/antistereov/singularity-core/commits/c94aff2) **core**: allow header authentication
- [9c0c049](https://github.com/antistereov/singularity-core/commits/9c0c049) **core**: remove openapi configuration
- [d413dcb](https://github.com/antistereov/singularity-core/commits/d413dcb) **auth**: implement optional bearer authentication
- [42ec943](https://github.com/antistereov/singularity-core/commits/42ec943) **file**: improve naming of keys
- [550e85d](https://github.com/antistereov/singularity-core/commits/550e85d) **file**: urls will now be generated when a response is requested which leads to a better and more consistent implementation with S3
- [b1ac0fa](https://github.com/antistereov/singularity-core/commits/b1ac0fa) **secret-store**: add default value for not when creating a secret

## 🐛 Fixes
- [207738c](https://github.com/antistereov/singularity-core/commits/207738c) **files, content**: fix minor bugs in file storage, image storage and tag management
- [86172ac](https://github.com/antistereov/singularity-core/commits/86172ac) **files**: fix issues with content length header and multi file parts
- [ba0db09](https://github.com/antistereov/singularity-core/commits/ba0db09) fix function call
- [fed6ee1](https://github.com/antistereov/singularity-core/commits/fed6ee1) **geolocation**: fix geolocation exception handling if no authorization is specified
- [a6d20ae](https://github.com/antistereov/singularity-core/commits/a6d20ae) **oauth2**: fix bugs in conversion of guests to users and step up flow, test, and update documentation
- [cccd8c6](https://github.com/antistereov/singularity-core/commits/cccd8c6) **oauth2**: fix bugs in connection flow and update documentation
- [b3240be](https://github.com/antistereov/singularity-core/commits/b3240be) **oauth2**: fix bugs in register und login flow and update documentation
- [6abacd0](https://github.com/antistereov/singularity-core/commits/6abacd0) update tests after changes in authentication
- [74a64cb](https://github.com/antistereov/singularity-core/commits/74a64cb) **auth**: AccessToken is invalid if NO session exists
- [cfcae30](https://github.com/antistereov/singularity-core/commits/cfcae30) **auth**: browser and os claim in SessionToken will only be set if not null
- [bf0f7ed](https://github.com/antistereov/singularity-core/commits/bf0f7ed) **auth**: fix authorization header bugs
- [18cb455](https://github.com/antistereov/singularity-core/commits/18cb455) **auth**: fix cookies and token initialization and setup
- [642a487](https://github.com/antistereov/singularity-core/commits/642a487) **two-factor**: fix setting of cookie
- [b60348d](https://github.com/antistereov/singularity-core/commits/b60348d) **invitation-service**: fix scheduled cleanup
- [334f5a5](https://github.com/antistereov/singularity-core/commits/334f5a5) **invitation**: add Id annotation
- [f6f298e](https://github.com/antistereov/singularity-core/commits/f6f298e) **article**: make image nullable when no metadata found
- [28e1fec](https://github.com/antistereov/singularity-core/commits/28e1fec) **local-file-storage**: add controller bean
- [2f54a03](https://github.com/antistereov/singularity-core/commits/2f54a03) **article**: fix creation of article dtos after change in file handling
- [fcad08f](https://github.com/antistereov/singularity-core/commits/fcad08f) **s3-file-storage**: fix generation of presigned urls
- [68f5dba](https://github.com/antistereov/singularity-core/commits/68f5dba) **local-file-storage**: fix implementation of doGetUrl
- [10929e0](https://github.com/antistereov/singularity-core/commits/10929e0) **secret-store**: add conditional on property annotation for classes of local secret store so only one bean of type secret store will be created on startup
- [96fae04](https://github.com/antistereov/singularity-core/commits/96fae04) **vault-secret-store**: add scheme configuration for vault
- [d8b35c8](https://github.com/antistereov/singularity-core/commits/d8b35c8) **vault-secret-store**: fix various bugs in implementation of vault

## 🔄️ Changes
- [f5c987c](https://github.com/antistereov/singularity-core/commits/f5c987c) **auth**: move geolocation package to auth
- [1d89dd8](https://github.com/antistereov/singularity-core/commits/1d89dd8) **core**: reorganize structure
- [3e52553](https://github.com/antistereov/singularity-core/commits/3e52553) **user**: reorganize user structure
- [eb2e196](https://github.com/antistereov/singularity-core/commits/eb2e196) **core**: reorganize packages
- [dd1c0f4](https://github.com/antistereov/singularity-core/commits/dd1c0f4) **local-file-storage**: add logging in controller

## 🧪 Tests
- [d6fa714](https://github.com/antistereov/singularity-core/commits/d6fa714) **content**: fix article tests after changing the API
- [aa863e1](https://github.com/antistereov/singularity-core/commits/aa863e1) update errors in 2fa email test because of login alert email
- [abf291f](https://github.com/antistereov/singularity-core/commits/abf291f) fix oauth2 test
- [876e74f](https://github.com/antistereov/singularity-core/commits/876e74f) **user-settings**: fix user-settings test and add new tests
- [a6e001a](https://github.com/antistereov/singularity-core/commits/a6e001a) **oauth2**: start implementing test for oauth2 flow
- [e29f9f0](https://github.com/antistereov/singularity-core/commits/e29f9f0) **groups**: add tests for group member controller
- [26a509f](https://github.com/antistereov/singularity-core/commits/26a509f) fix override variable
- [71184ee](https://github.com/antistereov/singularity-core/commits/71184ee) fix tests after updating API
- [3e780f5](https://github.com/antistereov/singularity-core/commits/3e780f5) update tests after api change
- [2c4d000](https://github.com/antistereov/singularity-core/commits/2c4d000) update tests after updating session model
- [7cfb7ca](https://github.com/antistereov/singularity-core/commits/7cfb7ca) fix cookie value that lead to timeouts
- [f31a3c3](https://github.com/antistereov/singularity-core/commits/f31a3c3) fix naming of EmailVerificationControllerTest
- [88caa47](https://github.com/antistereov/singularity-core/commits/88caa47) change port of test server to 8001
- [f92a7d1](https://github.com/antistereov/singularity-core/commits/f92a7d1) refactor tests to match project structure
- [8e2ffe4](https://github.com/antistereov/singularity-core/commits/8e2ffe4) **auth**: add tests for token refresh with header
- [86b4fff](https://github.com/antistereov/singularity-core/commits/86b4fff) **geolocation**: add tests for download and update
- [3846f78](https://github.com/antistereov/singularity-core/commits/3846f78) **geolocation**: create test
- [8cd995b](https://github.com/antistereov/singularity-core/commits/8cd995b) **user**: update test after swapping UserResponse to RegisterResponse in /response
- [b94226e](https://github.com/antistereov/singularity-core/commits/b94226e) **local-file-storage**: add test for saving file in sub dir
- [47146c1](https://github.com/antistereov/singularity-core/commits/47146c1) **local-file-storage**: add test for saving file in sub dir
- [ca8162e](https://github.com/antistereov/singularity-core/commits/ca8162e) **secret-store**: fix little bug where Instant will not be rounded to millis as in db
- [5d51ed0](https://github.com/antistereov/singularity-core/commits/5d51ed0) **secret-store**: fix little bug where Instant will not be rounded to millis as in db
- [1b19b82](https://github.com/antistereov/singularity-core/commits/1b19b82) **local-file-storage**: test initialization
- [e943ae9](https://github.com/antistereov/singularity-core/commits/e943ae9) **local-file-storage**: move util package to top level
- [18ea89b](https://github.com/antistereov/singularity-core/commits/18ea89b) **local-file-storage**: test if correct url is generated
- [999eef1](https://github.com/antistereov/singularity-core/commits/999eef1) **secret-store**: fix little bug where Instant will not be rounded to millis as in db
- [aed2baa](https://github.com/antistereov/singularity-core/commits/aed2baa) **local-secret-store**: add integration tests for LocalSecretStore
- [19e670c](https://github.com/antistereov/singularity-core/commits/19e670c) **vault-secret-store**: add integration tests for VaultSecretStore
- [694c3cc](https://github.com/antistereov/singularity-core/commits/694c3cc) make applicationContext public in BaseSpringBootTest

## 🧰 Tasks
- [810d2fa](https://github.com/antistereov/singularity-core/commits/810d2fa) **file-storage**: improve naming of methods to clarify difference between rendition and file metadata
- [dde2799](https://github.com/antistereov/singularity-core/commits/dde2799) fix code warning
- [71313c0](https://github.com/antistereov/singularity-core/commits/71313c0) **files**: fix code warnings
- [1d82282](https://github.com/antistereov/singularity-core/commits/1d82282) fix code warnings
- [ab42781](https://github.com/antistereov/singularity-core/commits/ab42781) code cleanup
- [069b81e](https://github.com/antistereov/singularity-core/commits/069b81e) fix code errors
- [3da14cf](https://github.com/antistereov/singularity-core/commits/3da14cf) fix code warnings
- [b67696f](https://github.com/antistereov/singularity-core/commits/b67696f) remove unused import
- [82e062f](https://github.com/antistereov/singularity-core/commits/82e062f) remove unused variable
- [150eac8](https://github.com/antistereov/singularity-core/commits/150eac8) **auth**: fix grammar in error message
- [888c3cf](https://github.com/antistereov/singularity-core/commits/888c3cf) fix code warnings
- [e9d1af0](https://github.com/antistereov/singularity-core/commits/e9d1af0) fix code warnings
- [e9ac9a6](https://github.com/antistereov/singularity-core/commits/e9ac9a6) code cleanup
- [6c27eec](https://github.com/antistereov/singularity-core/commits/6c27eec) remove unused imports
- [eb1b52e](https://github.com/antistereov/singularity-core/commits/eb1b52e) remove unused variables
- [bd02d87](https://github.com/antistereov/singularity-core/commits/bd02d87) code cleanup
- [b5be3c2](https://github.com/antistereov/singularity-core/commits/b5be3c2) code cleanup
- [a09319f](https://github.com/antistereov/singularity-core/commits/a09319f) code cleanup
- [465411d](https://github.com/antistereov/singularity-core/commits/465411d) code cleanup
- [9158cc8](https://github.com/antistereov/singularity-core/commits/9158cc8) fix code warnings
- [4509576](https://github.com/antistereov/singularity-core/commits/4509576) fix code warnings
- [5f09a95](https://github.com/antistereov/singularity-core/commits/5f09a95) fix code warnings
- [f537fcf](https://github.com/antistereov/singularity-core/commits/f537fcf) **demo**: update database name
- [a4ce692](https://github.com/antistereov/singularity-core/commits/a4ce692) fix test class name
- [6e909a7](https://github.com/antistereov/singularity-core/commits/6e909a7) **auth**: code cleanup
- [dfe5175](https://github.com/antistereov/singularity-core/commits/dfe5175) **gitignore**: add .run configuration to .gitignore
- [a17f803](https://github.com/antistereov/singularity-core/commits/a17f803) code cleanup
- [3246cc1](https://github.com/antistereov/singularity-core/commits/3246cc1) **gitignore**: add run configuraton to gitignore
- [60e5f66](https://github.com/antistereov/singularity-core/commits/60e5f66) **stereov-io**: add geolocation config to .env.sample
- [04b3f22](https://github.com/antistereov/singularity-core/commits/04b3f22) **geolocation**: remove todo because it is done
- [a32d1b2](https://github.com/antistereov/singularity-core/commits/a32d1b2) **core**: remove unused methods and classes
- [7826184](https://github.com/antistereov/singularity-core/commits/7826184) **core**: remove unused variables and imports
- [6700e94](https://github.com/antistereov/singularity-core/commits/6700e94) **auth**: remove unused variables

## 🛠  Build
- [8e2cf7e](https://github.com/antistereov/singularity-core/commits/8e2cf7e) bump version to 1.9.0
- [af4121d](https://github.com/antistereov/singularity-core/commits/af4121d) add maxmind credentials to release and deploy workflow
- [e6e96db](https://github.com/antistereov/singularity-core/commits/e6e96db) update Spring version
- [548bcba](https://github.com/antistereov/singularity-core/commits/548bcba) **demo**: persist maxmind database file
- [0e4445a](https://github.com/antistereov/singularity-core/commits/0e4445a) **build-and-test**: fix env names
- [1cfec90](https://github.com/antistereov/singularity-core/commits/1cfec90) **deploy-docs**: run workflow when apps/demo/** changes
- [cd1dc6d](https://github.com/antistereov/singularity-core/commits/cd1dc6d) **deploy-docs**: fix command for redis
- [2f715bb](https://github.com/antistereov/singularity-core/commits/2f715bb) **deploy-docs**: fix container name of singularity-demo
- [61b6b39](https://github.com/antistereov/singularity-core/commits/61b6b39) **deploy-docs**: use docker compose again instead of swarm
- [c6aefac](https://github.com/antistereov/singularity-core/commits/c6aefac) **deploy-docs**: make use of swarm again
- [abe2641](https://github.com/antistereov/singularity-core/commits/abe2641) **deploy-docs**: stop making use of docker swarm
- [97b0e62](https://github.com/antistereov/singularity-core/commits/97b0e62) **deploy-docs**: update image tags
- [78d52b6](https://github.com/antistereov/singularity-core/commits/78d52b6) **deploy-docs**: fix build of demo
- [100f7ee](https://github.com/antistereov/singularity-core/commits/100f7ee) **deploy-docs**: build container before swarm is started
- [5ec12e8](https://github.com/antistereov/singularity-core/commits/5ec12e8) **deploy-docs**: use docker swarms for zero downtime
- [33b3da1](https://github.com/antistereov/singularity-core/commits/33b3da1) **deploy-docs**: use .env file for storing secrets of demo project
- [5c64491](https://github.com/antistereov/singularity-core/commits/5c64491) **deploy-docs**: fix path to docker-compose.yaml
- [57c1741](https://github.com/antistereov/singularity-core/commits/57c1741) **build-and-test**: specify paths to trigger this workflow
- [2283e22](https://github.com/antistereov/singularity-core/commits/2283e22) **deploy-docs**: fix setting of envs
- [4d4602d](https://github.com/antistereov/singularity-core/commits/4d4602d) **deploy-docs**: update setting of maxmind credentials
- [26c3ebd](https://github.com/antistereov/singularity-core/commits/26c3ebd) **build-and-test**: ignore changes to all README.md files inside the repo
- [766d321](https://github.com/antistereov/singularity-core/commits/766d321) **build-and-test**: skip build of stereov-io
- [b15293a](https://github.com/antistereov/singularity-core/commits/b15293a) **deploy-docs**: fix git pull
- [60ef5dd](https://github.com/antistereov/singularity-core/commits/60ef5dd) **deploy-docs**: update name
- [d3b4db3](https://github.com/antistereov/singularity-core/commits/d3b4db3) **deploy-docs**: make mongodb and redis host configurable
- [f9c276e](https://github.com/antistereov/singularity-core/commits/f9c276e) **build-and-test**: stop running pipeline for changes in readme
- [fa3aedd](https://github.com/antistereov/singularity-core/commits/fa3aedd) **deploy-docs**: fix working directory
- [541a95e](https://github.com/antistereov/singularity-core/commits/541a95e) **build-and-test**: add more ignored paths
- [e20078d](https://github.com/antistereov/singularity-core/commits/e20078d) **build-and-test**: stop running workflow when docs are changed
- [a652931](https://github.com/antistereov/singularity-core/commits/a652931) **deploy docs**: create demo application and automate deployment of demo application
- [ccafa43](https://github.com/antistereov/singularity-core/commits/ccafa43) **deploy-docs**: use node v22
- [fe464ed](https://github.com/antistereov/singularity-core/commits/fe464ed) **deploy-docs**: remove broken npm step
- [ccb148d](https://github.com/antistereov/singularity-core/commits/ccb148d) **deploy-docs**: use yarn in build pipeline
- [2a4bb96](https://github.com/antistereov/singularity-core/commits/2a4bb96) **deploy-docs**: update dependency install with legacy peer deps
- [5fa0c38](https://github.com/antistereov/singularity-core/commits/5fa0c38) **docs**: add cname to deploy action
- [8492d01](https://github.com/antistereov/singularity-core/commits/8492d01) fix docker build
- [c17e18c](https://github.com/antistereov/singularity-core/commits/c17e18c) improve docker builds
- [1556e56](https://github.com/antistereov/singularity-core/commits/1556e56) bump version to 1.8.0-SNAPSHOT for next development cycle
- [bfc9534](https://github.com/antistereov/singularity-core/commits/bfc9534) bump version to 1.8.0
- [5146f79](https://github.com/antistereov/singularity-core/commits/5146f79) bump version to 1.8.0
- [1122c27](https://github.com/antistereov/singularity-core/commits/1122c27) fix release and deploy script

## 📝 Documentation
- [b6cbe52](https://github.com/antistereov/singularity-core/commits/b6cbe52) add algolia search
- [df00f60](https://github.com/antistereov/singularity-core/commits/df00f60) **content**: update content documentation
- [2659ab5](https://github.com/antistereov/singularity-core/commits/2659ab5) **files**: update file storage documentation
- [19c2fff](https://github.com/antistereov/singularity-core/commits/19c2fff) **content**: prepare documents in guides
- [1380791](https://github.com/antistereov/singularity-core/commits/1380791) **auth**: update openapi
- [3c880bc](https://github.com/antistereov/singularity-core/commits/3c880bc) **auth**: update documentation
- [ecfba79](https://github.com/antistereov/singularity-core/commits/ecfba79) **groups**: update docs for groups
- [dc83386](https://github.com/antistereov/singularity-core/commits/dc83386) **groups**: update group documentation
- [b834142](https://github.com/antistereov/singularity-core/commits/b834142) update documetation
- [39e9692](https://github.com/antistereov/singularity-core/commits/39e9692) update information to demo applications's OpenAPI documentation
- [1f91ad1](https://github.com/antistereov/singularity-core/commits/1f91ad1) add documentation for cache
- [8927f16](https://github.com/antistereov/singularity-core/commits/8927f16) fix broken links
- [0bb3037](https://github.com/antistereov/singularity-core/commits/0bb3037) change order of endpoints in openapi.yaml
- [9837fb2](https://github.com/antistereov/singularity-core/commits/9837fb2) update docs after api change
- [d60bcfd](https://github.com/antistereov/singularity-core/commits/d60bcfd) drastically improve api documentation by using the docusaurus-openapi-plugin
- [12c8dbe](https://github.com/antistereov/singularity-core/commits/12c8dbe) update gitignore
- [1d02b49](https://github.com/antistereov/singularity-core/commits/1d02b49) update gitignore
- [afbafa9](https://github.com/antistereov/singularity-core/commits/afbafa9) add docusaurus-openapi plugin and remove swagger
- [7fc1c25](https://github.com/antistereov/singularity-core/commits/7fc1c25) **auth**: add documentation for 2FA
- [e249baf](https://github.com/antistereov/singularity-core/commits/e249baf) **auth**: update documentation
- [d3f2aba](https://github.com/antistereov/singularity-core/commits/d3f2aba) **auth**: update documentation for auth
- [e8a8115](https://github.com/antistereov/singularity-core/commits/e8a8115) **auth**: add openapi documentation for email verification and password reset
- [dc469a8](https://github.com/antistereov/singularity-core/commits/dc469a8) update documentation
- [b201dec](https://github.com/antistereov/singularity-core/commits/b201dec) **oauth2**: update documentation for oauth2
- [5c00a9f](https://github.com/antistereov/singularity-core/commits/5c00a9f) update authorization guide
- [f350137](https://github.com/antistereov/singularity-core/commits/f350137) **demo**: add openapi docs for security
- [302e90d](https://github.com/antistereov/singularity-core/commits/302e90d) add more docs for authentication
- [633b4b2](https://github.com/antistereov/singularity-core/commits/633b4b2) update gradle dependency guide for Kotlin DSL
- [2a647d1](https://github.com/antistereov/singularity-core/commits/2a647d1) update social card
- [0cea0e9](https://github.com/antistereov/singularity-core/commits/0cea0e9) add disclaimer on every page
- [85d40aa](https://github.com/antistereov/singularity-core/commits/85d40aa) update docs
- [db6e033](https://github.com/antistereov/singularity-core/commits/db6e033) **geolocation**: add geolocation docs
- [6451859](https://github.com/antistereov/singularity-core/commits/6451859) **geolocation**: fix type in description
- [452e7a7](https://github.com/antistereov/singularity-core/commits/452e7a7) **docker**: update configuration with maxmind account id and license key
- [5c38313](https://github.com/antistereov/singularity-core/commits/5c38313) **core**: reorganize docs
- [660e2c6](https://github.com/antistereov/singularity-core/commits/660e2c6) **core**: add spring boot reference in intro
- [89b0de9](https://github.com/antistereov/singularity-core/commits/89b0de9) **core**: update spacing in README.md
- [1042ee0](https://github.com/antistereov/singularity-core/commits/1042ee0) **core**: update description in README and docs
- [f06667e](https://github.com/antistereov/singularity-core/commits/f06667e) **core**: add maven central version batch
- [51a1fc2](https://github.com/antistereov/singularity-core/commits/51a1fc2) **core**: remove unused imports and files
- [6928154](https://github.com/antistereov/singularity-core/commits/6928154) **core**: add intro, quickstart and authentication basics
- [2234e69](https://github.com/antistereov/singularity-core/commits/2234e69) **readme**: update README.md
- [951552b](https://github.com/antistereov/singularity-core/commits/951552b) **auth**: add docs for configuration of authentication
- [3e49651](https://github.com/antistereov/singularity-core/commits/3e49651) **swagger**: update path because /api is reserved for backend
- [5375681](https://github.com/antistereov/singularity-core/commits/5375681) **openapi**: update openapi docs
- [5ea0a01](https://github.com/antistereov/singularity-core/commits/5ea0a01) **openapi**: update openapi docs
- [5f0b6e8](https://github.com/antistereov/singularity-core/commits/5f0b6e8) **openapi**: update openapi
- [dda4d28](https://github.com/antistereov/singularity-core/commits/dda4d28) **openapi**: update controllers and initialize openapi documentation and swagger
- [934e65a](https://github.com/antistereov/singularity-core/commits/934e65a) **init**: initialize docs with custom colors and layout


## Contributors
We'd like to thank the following people for their contributions:
André Antimonov, GitHub, antistereov

## [v1.8.0]

## Changelog

## 🚀 Features
- [21201c6](https://github.com/antistereov/singularity-core/commits/21201c6) **local-file-storage**: update controller with more security checks
- [0e935ff](https://github.com/antistereov/singularity-core/commits/0e935ff) **local-file-storage**: serve both public and private files from controller
- [2e1df19](https://github.com/antistereov/singularity-core/commits/2e1df19) add an option to disable mail services to simplify configuration
- [624873e](https://github.com/antistereov/singularity-core/commits/624873e) **secret-store**: remove bitwarden secret store implementation due to extra dependency on GitHub Package Repository which makes the build process more complex
- [970e100](https://github.com/antistereov/singularity-core/commits/970e100) **core**: update docker-compose.yaml and delete config files for faster and easier setup
- [c387b98](https://github.com/antistereov/singularity-core/commits/c387b98) **secret-store**: make local the default secret store implementation
- [6b92d3a](https://github.com/antistereov/singularity-core/commits/6b92d3a) **secret-store**: remove mock as option for secret store implementation
- [3e85aa8](https://github.com/antistereov/singularity-core/commits/3e85aa8) **stereov-io**: update secret application.yml and .env.sample with new options of local secret store
- [247acc5](https://github.com/antistereov/singularity-core/commits/247acc5) **secret-store**: implement local secret storage solution with H2
- [0c431ae](https://github.com/antistereov/singularity-core/commits/0c431ae) **secret-store**: implement local storage solution
- [26b1a61](https://github.com/antistereov/singularity-core/commits/26b1a61) **content**: remove content lib
- [9c49e26](https://github.com/antistereov/singularity-core/commits/9c49e26) **file-storage**: make LocalFileStorage the primary implementation for FileStorage
- [2f54400](https://github.com/antistereov/singularity-core/commits/2f54400) **app-properties**: add slugify dependency for slug creation
- [b3fdf11](https://github.com/antistereov/singularity-core/commits/b3fdf11) **core**: implement local file storage and merge core and content lib
- [c9890d6](https://github.com/antistereov/singularity-core/commits/c9890d6) **core**: update KeyManager interface and implement HashiCorpKeyManager.kt
- [fd3df82](https://github.com/antistereov/singularity-core/commits/fd3df82) **core**: rename env variable name for path access style in s3
- [fb0c312](https://github.com/antistereov/singularity-core/commits/fb0c312) **core**: update s3 properties to allow path-style-access
- [2506bad](https://github.com/antistereov/singularity-core/commits/2506bad) **core**: implement group controller with pagination and sorting
- [0f834e1](https://github.com/antistereov/singularity-core/commits/0f834e1) **core**: create interface for translatable crud service
- [0779ab0](https://github.com/antistereov/singularity-core/commits/0779ab0) **core**: create interface for crud service
- [43bd776](https://github.com/antistereov/singularity-core/commits/43bd776) **core**: implement group controller with full crud funcitonality
- [94fcac0](https://github.com/antistereov/singularity-core/commits/94fcac0) **core**: SuccessResponse is now true by default
- [e4e1366](https://github.com/antistereov/singularity-core/commits/e4e1366) **core**: implement base mail template and use it
- [d33e9b9](https://github.com/antistereov/singularity-core/commits/d33e9b9) **core**: implement group controller
- [4699dab](https://github.com/antistereov/singularity-core/commits/4699dab) **core**: refactor config for user
- [e71717b](https://github.com/antistereov/singularity-core/commits/e71717b) **core**: hash service use hmacsha256 for SearchableHash
- [4f2504a](https://github.com/antistereov/singularity-core/commits/4f2504a) **content**: improve access details
- [47e340e](https://github.com/antistereov/singularity-core/commits/47e340e) **core**: improve email template and fix translation
- [8b75ed9](https://github.com/antistereov/singularity-core/commits/8b75ed9) **core**: implement invitation, email templates, translations, ...
- [621ced6](https://github.com/antistereov/singularity-core/commits/621ced6) **content**: update article management methods and add image upload
- [02e84a5](https://github.com/antistereov/singularity-core/commits/02e84a5) **content**: admin are also allowed to edit
- [57d160b](https://github.com/antistereov/singularity-core/commits/57d160b) **content**: implement content and article updates
- [464cd05](https://github.com/antistereov/singularity-core/commits/464cd05) **content**: make summary and content optional in CreateArticleRequest
- [a859037](https://github.com/antistereov/singularity-core/commits/a859037) **core**: fix primary language of group to english
- [99cce28](https://github.com/antistereov/singularity-core/commits/99cce28) **content**: fix primary language of tag to english
- [4e1497a](https://github.com/antistereov/singularity-core/commits/4e1497a) **content**: add german translations for initial tags
- [f9b6dd6](https://github.com/antistereov/singularity-core/commits/f9b6dd6) **content**: create editor group as initial grousp
- [91476bb](https://github.com/antistereov/singularity-core/commits/91476bb) **content**: update content permissions to use group key instead of group id
- [fe16752](https://github.com/antistereov/singularity-core/commits/fe16752) **core**: create translations for groups and add initial groups to application properties
- [630a29d](https://github.com/antistereov/singularity-core/commits/630a29d) **core**: add fallback options for translate method in Translatable interface
- [fee34c5](https://github.com/antistereov/singularity-core/commits/fee34c5) **core**: add fallback options for translate method in Translatable interface
- [0119182](https://github.com/antistereov/singularity-core/commits/0119182) **core**: rename UserDto to UserResponse
- [134f857](https://github.com/antistereov/singularity-core/commits/134f857) **core**: remove application info from user document - this will be saved in a separate collection in the future
- [f97d013](https://github.com/antistereov/singularity-core/commits/f97d013) **core**: remove device data from UserDto
- [e82f07f](https://github.com/antistereov/singularity-core/commits/e82f07f) **core**: add translations
- [cc56711](https://github.com/antistereov/singularity-core/commits/cc56711) **core**: simplify base exception handler and update usages
- [7c91660](https://github.com/antistereov/singularity-core/commits/7c91660) **content**: use tag key for identification instead of id
- [6e7d2bc](https://github.com/antistereov/singularity-core/commits/6e7d2bc) **stereov-io**: add default tags that will be created on startup
- [1d595ce](https://github.com/antistereov/singularity-core/commits/1d595ce) **content**: create findById endpoint in TagController
- [ccdf273](https://github.com/antistereov/singularity-core/commits/ccdf273) **content**: improve getArticles endpoint with filtering based on tags
- [97e89ff](https://github.com/antistereov/singularity-core/commits/97e89ff) **content**: create TagResponse as DTO for TagDocument
- [a2b2594](https://github.com/antistereov/singularity-core/commits/a2b2594) **content**: add properties and allow creation of predefined tags on startup
- [6e18fbb](https://github.com/antistereov/singularity-core/commits/6e18fbb) **content**: add tag features
- [f4b5d3a](https://github.com/antistereov/singularity-core/commits/f4b5d3a) **core**: create criteria creator for field contains substring
- [a97d250](https://github.com/antistereov/singularity-core/commits/a97d250) **content**: use jackson instead of kotlinx.serialization and ObjectId instead of String for ids, add missing features in article api
- [66287fc](https://github.com/antistereov/singularity-core/commits/66287fc) **core**: use Jackson instead of kotlinx.serialization and use ObjectId class for ids
- [d3fd854](https://github.com/antistereov/singularity-core/commits/d3fd854) **content**: create scroll and get requests for articles
- [f4a7928](https://github.com/antistereov/singularity-core/commits/f4a7928) **content**: update ContentAutoConfiguration after adding common package
- [cd150fb](https://github.com/antistereov/singularity-core/commits/cd150fb) **content**: update Article implementation to use all functions and fields from common package
- [202f01e](https://github.com/antistereov/singularity-core/commits/202f01e) **content**: implement common package to unify fields and functions used by all content document
- [282673c](https://github.com/antistereov/singularity-core/commits/282673c) **core**: update configuration to include groups
- [1061844](https://github.com/antistereov/singularity-core/commits/1061844) **core**: change AuthenticationService to include null-safe getCurrentUserOrNull method instead for userId
- [82c8ef3](https://github.com/antistereov/singularity-core/commits/82c8ef3) **core**: create ExistsResponse
- [ccec586](https://github.com/antistereov/singularity-core/commits/ccec586) **core**: remove unused null-checks for _id
- [4d32a1d](https://github.com/antistereov/singularity-core/commits/4d32a1d) **core**: make _id private in UserDocument
- [4b950d8](https://github.com/antistereov/singularity-core/commits/4b950d8) **core**: add groups to UserDocument
- [4015863](https://github.com/antistereov/singularity-core/commits/4015863) **core**: create group collection and functions
- [5cf542e](https://github.com/antistereov/singularity-core/commits/5cf542e) **content**: create more advanced queries for article access based on rights
- [88d339b](https://github.com/antistereov/singularity-core/commits/88d339b) **content**: save now requires authentication
- [3ef0b38](https://github.com/antistereov/singularity-core/commits/3ef0b38) **content**: image in Article is now optional
- [ccfc701](https://github.com/antistereov/singularity-core/commits/ccfc701) **core**: add validateVerification to AuthenticationService
- [2149634](https://github.com/antistereov/singularity-core/commits/2149634) **core**: roles in UserDocument is now a set
- [64e78f1](https://github.com/antistereov/singularity-core/commits/64e78f1) **content**: move article specific classes to new content package and implement some new functions
- [ced1ad9](https://github.com/antistereov/singularity-core/commits/ced1ad9) **core**: change base path to /api
- [405f371](https://github.com/antistereov/singularity-core/commits/405f371) **stereov-io**: add endpoint to get all users to admin
- [80ccba5](https://github.com/antistereov/singularity-core/commits/80ccba5) **stereov-io**: articles now save the creator
- [b39c340](https://github.com/antistereov/singularity-core/commits/b39c340) **core**: name field is now required in UserDocument
- [8ff7385](https://github.com/antistereov/singularity-core/commits/8ff7385) **stereov-io**: create more concise DTOs for overview and full
- [ad06d05](https://github.com/antistereov/singularity-core/commits/ad06d05) **stereov-io**: enhance ArticleService and add new classes for Article
- [6f7dc17](https://github.com/antistereov/singularity-core/commits/6f7dc17) **stereov-io**: add stereov-io application
- [7da68e8](https://github.com/antistereov/singularity-core/commits/7da68e8) **core**: add application slug to secret key names

## 🐛 Fixes
- [09496fd](https://github.com/antistereov/singularity-core/commits/09496fd) **file**: add exception handler bean in core configuration
- [42068ad](https://github.com/antistereov/singularity-core/commits/42068ad) **content**: make the hasAccess method return true if the content is public and only wants to be viewed
- [419d947](https://github.com/antistereov/singularity-core/commits/419d947) **core**: fix issues after removing support for bitwarden secret manager
- [199d358](https://github.com/antistereov/singularity-core/commits/199d358) **local-secret-store**: change save function to support upsert
- [8f2c0c3](https://github.com/antistereov/singularity-core/commits/8f2c0c3) **secret-store**: use an absolute file path for h2 connection factory
- [32ea37f](https://github.com/antistereov/singularity-core/commits/32ea37f) **secret-store**: update column names for local secret store
- [acf868d](https://github.com/antistereov/singularity-core/commits/acf868d) **secret-store**: fix property value in LocalSecretStoreProperties
- [7396397](https://github.com/antistereov/singularity-core/commits/7396397) **secret-store**: update metadata configuration for local secret store
- [a47f40d](https://github.com/antistereov/singularity-core/commits/a47f40d) **secret-store**: add option for local secret store in SecretStoreImplementation
- [0a5ff3f](https://github.com/antistereov/singularity-core/commits/0a5ff3f) **secret-store**: create bean for localSecretRepository
- [95e0b86](https://github.com/antistereov/singularity-core/commits/95e0b86) **secret-store**: add bean declaration for vault secret store
- [2ca83ad](https://github.com/antistereov/singularity-core/commits/2ca83ad) **file**: add transient annotations for getters in FileDocument
- [2c1e54e](https://github.com/antistereov/singularity-core/commits/2c1e54e) **local-file-storage**: fix file transfer by making it null-safe
- [5acc9da](https://github.com/antistereov/singularity-core/commits/5acc9da) **core**: fix startup errors for new file storage and improve naming
- [8c62275](https://github.com/antistereov/singularity-core/commits/8c62275) **secrets**: fix type mismatch after removing too much code
- [c6a5890](https://github.com/antistereov/singularity-core/commits/c6a5890) **integration-test**: fix application.yml after changing the property keys
- [8753d29](https://github.com/antistereov/singularity-core/commits/8753d29) **core**: fix usages of secrets
- [9a3359d](https://github.com/antistereov/singularity-core/commits/9a3359d) **core**: fix compilation errors after changing keymanger interface
- [c7618a6](https://github.com/antistereov/singularity-core/commits/c7618a6) **core**: make public url for s3 use path style access when enabled
- [a796336](https://github.com/antistereov/singularity-core/commits/a796336) **content**: findByKey does not require permissions anymore - if authorization is required, use findAuthorizedByKey
- [c4d5f76](https://github.com/antistereov/singularity-core/commits/c4d5f76) **core**: javaMailSender now uses MimeMessage instead of SimpleMessage and test this
- [02d9755](https://github.com/antistereov/singularity-core/commits/02d9755) **core**: fix configurations to make tests run
- [119c7de](https://github.com/antistereov/singularity-core/commits/119c7de) **core**: fix mail template creation
- [f0bee20](https://github.com/antistereov/singularity-core/commits/f0bee20) **content**: uniqueKey will now only add UUID if the article ids are not the same
- [d500967](https://github.com/antistereov/singularity-core/commits/d500967) **content**: fix change image endpoint
- [b14c64f](https://github.com/antistereov/singularity-core/commits/b14c64f) **content**: fix auto configuration
- [db3ec3e](https://github.com/antistereov/singularity-core/commits/db3ec3e) **content**: add ArticleManagementController bean to content auto configuration
- [48358b3](https://github.com/antistereov/singularity-core/commits/48358b3) **content**: fix methods after removing unused function parameters
- [6065755](https://github.com/antistereov/singularity-core/commits/6065755) **content**: update auto configuration after changes in ArticleService
- [3d66750](https://github.com/antistereov/singularity-core/commits/3d66750) **content**: tags is now a set of ObjectId instead of strings
- [77e2e93](https://github.com/antistereov/singularity-core/commits/77e2e93) **content**: fix collection name for TagDocument to tags
- [72f3aaa](https://github.com/antistereov/singularity-core/commits/72f3aaa) **stereov-io**: add content lib to Dockerfile
- [83c4ce4](https://github.com/antistereov/singularity-core/commits/83c4ce4) **content**: disable bootRun task in build script
- [bbf2dbd](https://github.com/antistereov/singularity-core/commits/bbf2dbd) **core**: fix code after changing base path
- [0153592](https://github.com/antistereov/singularity-core/commits/0153592) **article**: remove unused imports and enable reactive MongoDB repositories
- [2fc3760](https://github.com/antistereov/singularity-core/commits/2fc3760) **demo**: add application name to .env.sample and add reference to application.yml

## 🔄️ Changes
- [0081601](https://github.com/antistereov/singularity-core/commits/0081601) **local-file-storage**: remove unused methods and variables after combining both private and public files in one directory
- [b281e47](https://github.com/antistereov/singularity-core/commits/b281e47) **file**: rename exception NoSuchFileException to FileNotFoundException
- [c8b2e2c](https://github.com/antistereov/singularity-core/commits/c8b2e2c) **mail**: update names for mail service implementations with more descriptive terms
- [53dc992](https://github.com/antistereov/singularity-core/commits/53dc992) **file**: rename FileDocument to FileMetadataDocument to better reflect its content
- [e007288](https://github.com/antistereov/singularity-core/commits/e007288) **core**: move tag package from common to core
- [b58fcf9](https://github.com/antistereov/singularity-core/commits/b58fcf9) **core**: refactor file packages so it will be easier to add new implementations
- [79d8f02](https://github.com/antistereov/singularity-core/commits/79d8f02) **core**: rename key manager to secret store
- [1ed8e74](https://github.com/antistereov/singularity-core/commits/1ed8e74) create build.gradle.kts in root directory and specify build and release configurations
- [72e1f09](https://github.com/antistereov/singularity-core/commits/72e1f09) **core**: prepare core lib for mirroring to public repo
- [50b5fe6](https://github.com/antistereov/singularity-core/commits/50b5fe6) **core**: move AccessTokenCache to user package
- [1225dc4](https://github.com/antistereov/singularity-core/commits/1225dc4) **core**: create rotateKeys method in secret manager that allows to fix secrets when configured
- [e55e510](https://github.com/antistereov/singularity-core/commits/e55e510) **core**: move implementations of SecretService to packages they belong to
- [c7d6424](https://github.com/antistereov/singularity-core/commits/c7d6424) **core**: refactor core to make it more modular
- [4b88534](https://github.com/antistereov/singularity-core/commits/4b88534) **core**: refactor database package
- [57cc0e4](https://github.com/antistereov/singularity-core/commits/57cc0e4) **core**: move various packages
- [1a69c4e](https://github.com/antistereov/singularity-core/commits/1a69c4e) **core**: rename base package from io.stereov.singularity.core to io.stereov.singularity
- [040faf2](https://github.com/antistereov/singularity-core/commits/040faf2) **content, core**: improve function naming
- [7c49830](https://github.com/antistereov/singularity-core/commits/7c49830) **content**: rename ArticleOverviewDto to ArticleOverviewResponse
- [5a050d3](https://github.com/antistereov/singularity-core/commits/5a050d3) **integration-tests**: demo package is now called integration-tests
- [7a8fa9c](https://github.com/antistereov/singularity-core/commits/7a8fa9c) change base package for core to io.stereov.singularity.core
- [6bf953b](https://github.com/antistereov/singularity-core/commits/6bf953b) change base package to io.stereov.singularity
- [4e43f42](https://github.com/antistereov/singularity-core/commits/4e43f42) **core**: move InvalidDocumentException to global exceptions

## 🧪 Tests
- [beaf4d2](https://github.com/antistereov/singularity-core/commits/beaf4d2) **local-file-storage**: create integration tests for multiple edge and standard cases
- [8fe5a1d](https://github.com/antistereov/singularity-core/commits/8fe5a1d) add tests for exceptions when mail is disabled
- [457dc89](https://github.com/antistereov/singularity-core/commits/457dc89) update tests to cover cases where mail is needed or not needed
- [8ab7568](https://github.com/antistereov/singularity-core/commits/8ab7568) update and simplify application properties
- [5ced3aa](https://github.com/antistereov/singularity-core/commits/5ced3aa) **local-file-storage**: fix upload methods by creating FileMetadataResponse to specify which fields will be saved in the database in which way
- [45013e9](https://github.com/antistereov/singularity-core/commits/45013e9) **local-file-storage**: create integration test for file storage
- [45f0d8a](https://github.com/antistereov/singularity-core/commits/45f0d8a) **local-secret-store**: update test configuration
- [631bc7c](https://github.com/antistereov/singularity-core/commits/631bc7c) **local-secret-store**: update test configuration to use local secret store
- [6e99002](https://github.com/antistereov/singularity-core/commits/6e99002) **local-file-storage**: add test classes for LocalFileStorage
- [edb1004](https://github.com/antistereov/singularity-core/commits/edb1004) **user-session**: add test for user avatar updates
- [8dc6812](https://github.com/antistereov/singularity-core/commits/8dc6812) **file-storage**: add test-image.jpg as test resource for file upload
- [647d77a](https://github.com/antistereov/singularity-core/commits/647d77a) **core**: fix generation of jwts in test
- [daef01b](https://github.com/antistereov/singularity-core/commits/daef01b) **content**: fix tests after change in groups
- [1d18fe6](https://github.com/antistereov/singularity-core/commits/1d18fe6) **core**: fix tests after change in groups
- [f92d3c4](https://github.com/antistereov/singularity-core/commits/f92d3c4) **content**: add test for tag filtering
- [d8ed09b](https://github.com/antistereov/singularity-core/commits/d8ed09b) **content**: all tags will be deleted after each test
- [cb94458](https://github.com/antistereov/singularity-core/commits/cb94458) **content**: update tests after changes
- [cb25f75](https://github.com/antistereov/singularity-core/commits/cb25f75) **core**: fix tests after changes in core
- [4c161fa](https://github.com/antistereov/singularity-core/commits/4c161fa) **content**: create tests for getArticles
- [ae58bcd](https://github.com/antistereov/singularity-core/commits/ae58bcd) **content**: update and add missing tests for ArticleController and ArticleManagementController
- [357fe0b](https://github.com/antistereov/singularity-core/commits/357fe0b) **content**: update save function after changes in article and content API
- [deacd3e](https://github.com/antistereov/singularity-core/commits/deacd3e) **core**: make basePath final in BaseIntegrationTest
- [e957755](https://github.com/antistereov/singularity-core/commits/e957755) **core**: add groups to register function in BaseSpringBootTest
- [c7b5a19](https://github.com/antistereov/singularity-core/commits/c7b5a19) **core**: change all _id calls in UserDocument to id

## 🧰 Tasks
- [1d14995](https://github.com/antistereov/singularity-core/commits/1d14995) **stereov-io**: update application configuration after changing local file storage implementation
- [1188efe](https://github.com/antistereov/singularity-core/commits/1188efe) **core**: remove unused imports, methods and variables
- [c095f72](https://github.com/antistereov/singularity-core/commits/c095f72) **core**: remove unused imports
- [4612256](https://github.com/antistereov/singularity-core/commits/4612256) **core**: remove references to Bitwarden Secret Manager throughout the code
- [37d75ba](https://github.com/antistereov/singularity-core/commits/37d75ba) **.gitignore**: add logs directory to gitignore
- [369c8a5](https://github.com/antistereov/singularity-core/commits/369c8a5) **secret-store**: rename bitwarden bean creation method
- [6c67fdd](https://github.com/antistereov/singularity-core/commits/6c67fdd) **local-file-storage**: update default storage path to .data/files and add .data to .gitignore
- [47de76a](https://github.com/antistereov/singularity-core/commits/47de76a) **core**: update spring boot version to 3.5.3
- [adb1059](https://github.com/antistereov/singularity-core/commits/adb1059) **stereov-io**: update .env.sample with default file storage paths
- [f7009d3](https://github.com/antistereov/singularity-core/commits/f7009d3) **core**: fix code warnings
- [492f12a](https://github.com/antistereov/singularity-core/commits/492f12a) remove unused import statements
- [19357a1](https://github.com/antistereov/singularity-core/commits/19357a1) fix code errors and warnings
- [cc2bbf4](https://github.com/antistereov/singularity-core/commits/cc2bbf4) **core**: remove unused methods
- [e8b4629](https://github.com/antistereov/singularity-core/commits/e8b4629) **core**: remove unused imports
- [6557317](https://github.com/antistereov/singularity-core/commits/6557317) **content**: remove unused methods
- [d686527](https://github.com/antistereov/singularity-core/commits/d686527) **core**: remove unused imports
- [a4321c2](https://github.com/antistereov/singularity-core/commits/a4321c2) **core**: remove unused imports and methods
- [3857496](https://github.com/antistereov/singularity-core/commits/3857496) **content**: remove unused Dtos
- [0ffe227](https://github.com/antistereov/singularity-core/commits/0ffe227) **content**: code cleanup
- [3ca8980](https://github.com/antistereov/singularity-core/commits/3ca8980) **content**: remove unused imports
- [5fe014d](https://github.com/antistereov/singularity-core/commits/5fe014d) **code**: code cleanup
- [9ba5a30](https://github.com/antistereov/singularity-core/commits/9ba5a30) remove unused methods, classes and imports
- [7b209e0](https://github.com/antistereov/singularity-core/commits/7b209e0) **content**: set default value for description in CreateTagRequest
- [9c73a5e](https://github.com/antistereov/singularity-core/commits/9c73a5e) **content**: remove unused methods
- [49a3148](https://github.com/antistereov/singularity-core/commits/49a3148) **content**: code cleanup
- [26abd1e](https://github.com/antistereov/singularity-core/commits/26abd1e) **core**: code cleanup
- [1fbfab4](https://github.com/antistereov/singularity-core/commits/1fbfab4) **core**: code cleanup
- [0984279](https://github.com/antistereov/singularity-core/commits/0984279) **content**: remove unused method in ArticleService
- [9031451](https://github.com/antistereov/singularity-core/commits/9031451) **content**: remove unused ArticleContent model
- [4369a38](https://github.com/antistereov/singularity-core/commits/4369a38) **content**: remove unused methods and imports in ArticleRepository
- [357b7cb](https://github.com/antistereov/singularity-core/commits/357b7cb) **content**: remove unused imports
- [234ce08](https://github.com/antistereov/singularity-core/commits/234ce08) **core**: add more information in logging filter
- [465066c](https://github.com/antistereov/singularity-core/commits/465066c) **core**: LoggingFilter will now also show origin
- [15693d8](https://github.com/antistereov/singularity-core/commits/15693d8) **stereov-io**: log ip address of host in logging filter
- [2422e17](https://github.com/antistereov/singularity-core/commits/2422e17) **stereov-io**: remove unused imports

## 🛠  Build
- [278f4df](https://github.com/antistereov/singularity-core/commits/278f4df) bump version to 1.8.0
- [b9408b2](https://github.com/antistereov/singularity-core/commits/b9408b2) bump version to 1.7.0-SNAPSHOT
- [50d0355](https://github.com/antistereov/singularity-core/commits/50d0355) **stereov-io**: fix docker build script
- [b4de419](https://github.com/antistereov/singularity-core/commits/b4de419) **stereov-io**: add multiplatform builds
- [6c7bc50](https://github.com/antistereov/singularity-core/commits/6c7bc50) update release and deploy with automated version calculation
- [7daf14f](https://github.com/antistereov/singularity-core/commits/7daf14f) push docker image to registry only on pushes and not on pull_requests
- [cea80bd](https://github.com/antistereov/singularity-core/commits/cea80bd) **stereov-io**: remove content directory from Dockerfile
- [61136ea](https://github.com/antistereov/singularity-core/commits/61136ea) remove build dependencies on GPR after removing support for Bitwarden Secret Manager
- [89837e5](https://github.com/antistereov/singularity-core/commits/89837e5) remove architecture specification for stereov-io build
- [c77cd1d](https://github.com/antistereov/singularity-core/commits/c77cd1d) change stereov-io docker build platform to arm64
- [925b763](https://github.com/antistereov/singularity-core/commits/925b763) remove redundant generation of changelogs
- [fba64e0](https://github.com/antistereov/singularity-core/commits/fba64e0) bump version to 1.8.0-SNAPSHOT for next development cycle
- [3d07ca3](https://github.com/antistereov/singularity-core/commits/3d07ca3) update changelog
- [ed3b6f9](https://github.com/antistereov/singularity-core/commits/ed3b6f9) fix build setup for repository - remove root build.gradle.kts
- [7a8b87b](https://github.com/antistereov/singularity-core/commits/7a8b87b) fix location of sync workflows
- [285ba74](https://github.com/antistereov/singularity-core/commits/285ba74) update email config for github-actions bot
- [ac9f5c1](https://github.com/antistereov/singularity-core/commits/ac9f5c1) fix path in sync jobs
- [334b599](https://github.com/antistereov/singularity-core/commits/334b599) create sync job for content
- [612bf85](https://github.com/antistereov/singularity-core/commits/612bf85) remove redundant clean step
- [dea66f2](https://github.com/antistereov/singularity-core/commits/dea66f2) update release workflow to explicitly release selected libs
- [6a5db3b](https://github.com/antistereov/singularity-core/commits/6a5db3b) update workflows after creating new structure
- [349ee75](https://github.com/antistereov/singularity-core/commits/349ee75) **core**: bump version to 1.7.1-SNAPSHOT
- [aadc6b3](https://github.com/antistereov/singularity-core/commits/aadc6b3) **core**: bump version to 1.7.0
- [8d04d44](https://github.com/antistereov/singularity-core/commits/8d04d44) **core**: update release config
- [309199b](https://github.com/antistereov/singularity-core/commits/309199b) fix sync token issue
- [6beef76](https://github.com/antistereov/singularity-core/commits/6beef76) fix sync token issue
- [291f774](https://github.com/antistereov/singularity-core/commits/291f774) fix sync token issue
- [f1e7f27](https://github.com/antistereov/singularity-core/commits/f1e7f27) fix sync token issue
- [417f62f](https://github.com/antistereov/singularity-core/commits/417f62f) fix sync token issue
- [dfaa266](https://github.com/antistereov/singularity-core/commits/dfaa266) fix sync token issue
- [818df35](https://github.com/antistereov/singularity-core/commits/818df35) add and update workflows for sync to public repo
- [9876c6b](https://github.com/antistereov/singularity-core/commits/9876c6b) **stereov-io**: remove k8s infrastructure since it will be moved to infrastructure repo
- [3641a4c](https://github.com/antistereov/singularity-core/commits/3641a4c) **stereov-io**: update secret name and ingress annotations
- [6ba8f1c](https://github.com/antistereov/singularity-core/commits/6ba8f1c) **stereov-io**: add cert
- [e91e57c](https://github.com/antistereov/singularity-core/commits/e91e57c) **stereov-io**: change db to stereov
- [559c67b](https://github.com/antistereov/singularity-core/commits/559c67b) **stereov-io**: add deployment scripts for Kubernetes
- [f877ca0](https://github.com/antistereov/singularity-core/commits/f877ca0) **stereov-io**: use eclipse-temurin image as in includes glib in Dockerfile
- [0e5a876](https://github.com/antistereov/singularity-core/commits/0e5a876) **stereov-io**: add healthcheck and env variables to Dockerfile
- [e5cccfb](https://github.com/antistereov/singularity-core/commits/e5cccfb) **stereov-io**: create Dockerfile and automate image creation
- [c617288](https://github.com/antistereov/singularity-core/commits/c617288) bump version to 1.6.5-SNAPSHOT

## 📝 Documentation
- [72f8f2c](https://github.com/antistereov/singularity-core/commits/72f8f2c) update base url configuration for docusaurus
- [df305d3](https://github.com/antistereov/singularity-core/commits/df305d3) change docusaurus policy for broken links
- [e36d3fe](https://github.com/antistereov/singularity-core/commits/e36d3fe) initialize docusaurus documentation
- [f6aa3e4](https://github.com/antistereov/singularity-core/commits/f6aa3e4) **core**: fix maven banner
- [d0ce2ef](https://github.com/antistereov/singularity-core/commits/d0ce2ef) **core**: update README.md with getting started guide
- [f51f4ab](https://github.com/antistereov/singularity-core/commits/f51f4ab) update changelog and readmes for libs
- [3de8bba](https://github.com/antistereov/singularity-core/commits/3de8bba) **readme**: update file references in README.md
- [1907ad7](https://github.com/antistereov/singularity-core/commits/1907ad7) **readme**: remove dividers in README.md
- [e16a7ba](https://github.com/antistereov/singularity-core/commits/e16a7ba) **readme**: fix maven central batch
- [38954a9](https://github.com/antistereov/singularity-core/commits/38954a9) update import in development setup in README.md
- [5b7bf54](https://github.com/antistereov/singularity-core/commits/5b7bf54) update maven-central batch in README.md


## Contributors
We'd like to thank the following people for their contributions:
André Antimonov, GitHub, antistereov

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
- [f97d013](https://github.com/antistereov/singularity-content/commits/f97d013) **core**: remove session data from UserDto
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
