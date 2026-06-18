# Changelog

All notable changes to this project will be documented in this file.

## [1.2.2] - 2026-06-18

### Bug Fixes

- *(ci)* Manually set github repo for artifact upload since the folder where this action is run is not part of the repo ([`bee98b3`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/bee98b33aec509196eb36faf5fb4e1c1103a7a66))

### Features

- *(android)* Scan QR pairing code on setup ([`1ae6224`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/1ae6224fd831848d423e06edc3cfea00b1e02961))
- *(android)*: Improve android background process handling the heartbeat. Now it should properly run each heartbeat even when on background and after restart ([`645a91e4`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/645a91e4202d4f7b4f27debbbd196f153d577359))

## [1.2.1] - 2026-06-12

### Bug Fixes

- Replace alpha version of material3 with one from compose dependency ([`b65dffd`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/b65dffd6fe2537d330a895f7bc125f9e8a2019db))
- *(android)* Use white adaptive icon background ([`0321b20`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/0321b20c2884d8f1512a958eefa931778f89d291))
- *(android)* Switch adaptive icon to dark-variant mark for white background ([`c9d4709`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/c9d47099d96a1fa56fa2f13400a7c62c55971d4d))

### Features

- *(android)* Improvements to background process handling periodic heartbeat
- *(ui)* HexBolt BrandMark, AppColors, monochrome icon, themed system bars ([`ee41501`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/ee41501bed564516711edb43a8ca1adcdc308e73))
- Limit heartbeat minimum to 15minutes since less than that is too quick, keep it simple.

### Miscellaneous

- Apply formatting via KTLint ([`db23a3d`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/db23a3d9894702a6eea7acd2d37ac27ad0b6a44c))
- Remove deprecated option from gradle options and place it on appropiate android gradle ([`d8769e0`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/d8769e01d474decec2de197031174fc9d7d70a5d))
- Remove unneeded documentation from repo ([`b4a7d64`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/b4a7d646896a8354c8d859fdd91271289bce22d6))

## [1.2.0] - 2026-06-02

### Features
- Add new branding ([`b153e61`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/b153e61712cf6cdb926b6cb5c3c9e6ed193022aa))

### Miscellaneous

- *(ci)* On release ensure first thing is creating the actual release page, then create artifacts. Only tests must pass, added to the make release command. ([`edf0e6a`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/edf0e6ac53adc46c9bc3a82ee12e86211bd435c2))
- *(ci)* Release - Improve process and properly relegate changelog to manual review and then pipeline for image generation ([`42dd7d1`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/42dd7d10b6e22d7e767a3db76a58894505e359a1))
- Improve background process handling by ensuring it is started on boot as well as on app update ([`6b64858`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/6b64858e97f7b3ef20e59dda91461e10934fe217))
- Update commit msg script ([`912282d`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/912282df5a7554bde14d9b5ddfc828dc74f5c322))
- Update device pairing endpoint to the new "device-pair" ([`c0ce080`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/c0ce080d4379c1f659768ea8343fbcb87b9881d8))

## [1.1.1] - 2026-04-17

### Bug Fixes

- *(ci)* CHANGELOG generation happens locally so that release pipeline doesn't run into issues when generating the CHANGELOG and committing it. Pros: Now changelog can be reviewed and editted locally. Tag not has updated changelog already there. Pipeline is simpler ([`0af6bc3`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/0af6bc3188dc28e099af4b502e824c6efa27a281))

## [1.1.0] - 2026-04-16

### Features

- Add docker image as a simple alternative to the application ([`75c4ca6`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/75c4ca6d2e79ccdadf5d41dde454614ca59f7482))
- Allow registering the application via a registration code shared by the admin ([`29c202a`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/29c202a8871a2f68e8927baf52bc0deae63124f9))

### Miscellaneous

- *(ci)* Update CHANGELOG.md for v1.0.3 ([`2bd6677`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/2bd6677d0a9135b0e1dac155079f97fc3be6145a))
- Improve documentation and split ita ([`e9d282b`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/e9d282b48e15728910e7407729fccf09be0e063a))
- Improve documentation by splitting into patterns with an index ([`5c5de91`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/5c5de91fda3b5619e76961a8798f9664e801ac2e))
- *(deps)* Bump softprops/action-gh-release from 2 to 3 ([`4d485b6`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/4d485b6a208a3123cb4a48f690da339ca1f20c1c))
- *(deps)* Bump actions/checkout from 5 to 6 ([`ca6d5fc`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/ca6d5fc3fcc44f23a368120b3918ebc457bfda41))
- *(deps)* Bump gradle/actions from 4 to 6 ([`8661aa0`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/8661aa0fa7018ed253962e27548c259a7d1f2870))
- *(deps)* Bump actions/download-artifact from 5 to 8 ([`4e41ff9`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/4e41ff9e0b71b04a30f06aaa83d545d57ac20c3f))
- Improve documentation ([`71a7ca7`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/71a7ca7592fa95d3c981783d1246e70766b21820))
- *(ci)* Auto-merge dependabot. Pending enable automerge at github repo level, needs public repo. ([`30c4cdb`](https://github.com/DiegoGuidaF/PulseWeaver-Heartbeat/commit/30c4cdb77c64ed3d00ecbb60fcbeb94681d00b74))

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


