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
