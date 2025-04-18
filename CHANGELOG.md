## [v1.5.0]

## Changelog

## 🚀 Features
- [e42130e](https://github.com/antistereov/web-kotlin-spring-baseline/commits/e42130e) enable key rotation for jwt
- [d2f8475](https://github.com/antistereov/web-kotlin-spring-baseline/commits/d2f8475) create abstract classes to simplify handling encryption
- [9ec8ac9](https://github.com/antistereov/web-kotlin-spring-baseline/commits/9ec8ac9) **key-manager**: implement a KeyManager interface to enable future implementations of key rotation
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
