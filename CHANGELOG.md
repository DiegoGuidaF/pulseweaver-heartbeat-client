# Changelog

All notable changes to this project will be documented in this file.

## [1.0.3] - 2026-04-02

### Bug Fixes

- Remember last heartbeat data even on background and show it on focus ([`78ebe61`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/78ebe614818b040c3c9536206f9faa9ebfed8563))

### Features

- Collapse the connection info (server and api) so its not so prominent and more difficult to change by mistake ([`4ea888e`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/4ea888e6b0ab9c725a1d3e39e947ff59b04aa541))

### Miscellaneous

- Do not send heartbeat on app foreground focus. ([`6362f98`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/6362f987feb68e2571cf4606d2d32990189d254e))
- *(android)* Do not fire heartbeat on app init via networkmonitor listener. ([`9afa072`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/9afa07214d76c48493fbcbda4acc017fbb7deec4))
- Improve theme colors light/dark ([`af325ec`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/af325ec8b0c3bfffa03b5bedf47fafdfa4b25900))
- *(ai)* Add basic app documentation ([`49686b5`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/49686b5613b612d017ebba64cec2ee80081ff9e0))
- *(ci)* Improve and better segregate workflows ([`45fbdaa`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/45fbdaa6c075467ef5954e8f84f379d6fd4b097f))

## [1.0.2] - 2026-04-01

### Miscellaneous

- *(deps)* Bump actions/upload-artifact from 5.0.0 to 7.0.0 ([`25b235d`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/25b235d39ca9e1f95140c93842ff01dfd03af8cf))
- *(deps)* Bump org.jlleitschuh.gradle.ktlint from 12.2.0 to 14.2.0 ([`e99bca7`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/e99bca72f8166abd37b75688d12d2349c1e99073))
- *(ci)* Dependabot group minor/patch updates for gh actions ([`9925fb2`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/9925fb274d1c1fbb7c1810d3851df44699f470bd))
- *(deps)* Bump the gradle-minor-patch group with 2 updates ([`e7b2f32`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/e7b2f32f6f26d9e52ac5eaf28392f0c5e777aa48))
- *(ci)* Set gh actions tags to latest version (not pin to sha) to reduce PR noise and always use latest. Keep dependabot PR for major changes on them ([`ee524ed`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/ee524edf871cf082acadb4ae04f79fa0030a3210))
- *(ci)* Try to improve action usage of gradle setup and cache. Should solve windows issues trying to use filesystem while gradle still has the lock on it ([`8a95f36`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/8a95f36e541abfa7bbee3427aa85a5051755ab41))

## [1.0.1] - 2026-03-31

### Miscellaneous

- Add macos heartbeat with system scheduler (launchd) ([`f2eab73`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/f2eab73a77e5da17f089d692c0f1235b12d9583d))
- Update gh action versions to latest and pin sha ([`7a19021`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/7a19021798fcc14e8ed73f1e0bffd879dfaf4b2d))
- *(ci)* Add git-cliff changelog generation and conventional commit hook ([`8a0024d`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/8a0024d5b6746087251471b5d1cf91af895525bb))
- *(ci)* Add Dependabot configuration ([`7824531`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/7824531cc84539b93549fe3a3b81c94310b05df2))
- Improve documentation and link to main server repo ([`10cdca6`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/10cdca68b62adb1264fc6d66724bb8d3601c2ce7))

## [1.0.0] - 2026-03-31

### Features

- Implement PulseWeaver heartbeat client ([`c0b88de`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/c0b88de7c6b5b265af12c84a7afa242dc880c734))

### Init

- KMP App Template base ([`e573848`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/e573848bae6b2d2b3e12df6bb537fe089dfbc827))

### Miscellaneous

- Add readme ([`7283c3b`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/7283c3b2627039d2e78b6c1cf0f2401b903e165e))
- *(ci)* Add CICD with target builders ([`04a5dbd`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/04a5dbd1cc0a71593ec84ebf353df41f3671ddf7))


