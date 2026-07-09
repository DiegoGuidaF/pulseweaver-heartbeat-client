.PHONY: help test verify-all check screenshots build-android build-desktop run run-debug clean \
       package-deb package-dmg package-msi package-all \
       release release-patch release-minor release-major _release \
       generate-keystore setup-secrets lint \
       install-hooks version

VERSION ?= $(shell (git describe --tags --abbrev=0 2>/dev/null || echo v1.0.0) | sed 's/^v//')
NEXT_PATCH = $(shell echo $(VERSION) | awk -F. '{printf "%d.%d.%d", $$1, $$2, $$3+1}')
NEXT_MINOR = $(shell echo $(VERSION) | awk -F. '{printf "%d.%d.0", $$1, $$2+1}')
NEXT_MAJOR = $(shell echo $(VERSION) | awk -F. '{printf "%d.0.0", $$1+1}')
SKIP_RELEASE_CHECK ?= 0

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ---------------------------------------------------------------------------
# Gradle invocation (verification targets)
# ---------------------------------------------------------------------------
# The macOS Kotlin compiler daemon writes its `*.alive` flag file to the system
# temp dir (java.io.tmpdir), which the Claude Code sandbox denies with
# "Operation not permitted". Route Kotlin's temp to a project-local dir and
# compile in-process so the verify targets run cleanly under the sandbox. The
# trade-off — no persistent compiler daemon, so slightly slower warm builds — is
# why only the verification targets use this; interactive/packaging targets keep
# plain ./gradlew and its daemon. --no-configuration-cache avoids caching a failed
# sandboxed run (org.gradle.configuration-cache=true is on by default).
KOTLIN_TMP := $(CURDIR)/build/tmp-kotlin
GRADLEW_VERIFY = TMPDIR=$(KOTLIN_TMP) ./gradlew --no-configuration-cache \
	-Dkotlin.compiler.execution.strategy=in-process -Djava.io.tmpdir=$(KOTLIN_TMP)

# Per-source-set ktlint checks. The top-level `ktlintCheck` also scans generated
# Compose resource collectors and is unreliable — see
# docs/patterns/kotlin-multiplatform/testing.md in the workspace.
KTLINT_CHECK_TASKS = shared:runKtlintCheckOverCommonMainSourceSet \
	shared:runKtlintCheckOverAndroidMainSourceSet \
	shared:runKtlintCheckOverJvmMainSourceSet \
	shared:runKtlintCheckOverCommonTestSourceSet \
	shared:runKtlintCheckOverJvmTestSourceSet

$(KOTLIN_TMP):
	@mkdir -p $(KOTLIN_TMP)

# ---------------------------------------------------------------------------
# Development
# ---------------------------------------------------------------------------

test: | $(KOTLIN_TMP) ## Run all tests (JVM)
	$(GRADLEW_VERIFY) shared:jvmTest --stacktrace

verify-all: | $(KOTLIN_TMP) ## Compile every target (jvm+android+ios) and run JVM tests
	$(GRADLEW_VERIFY) \
		shared:compileKotlinJvm \
		shared:compileAndroidMain \
		shared:compileKotlinIosArm64 \
		shared:jvmTest --stacktrace

screenshots: ## Regenerate documentation screenshots into screenshots/companionapp
	./gradlew shared:jvmTest --tests "com.pulseweaver.heartbeat.ui.DocScreenshotsTest" \
		-Dpw.screenshots=true --rerun-tasks

lint: | $(KOTLIN_TMP) ## Run ktlint (per-source-set; top-level ktlintCheck is unreliable)
	$(GRADLEW_VERIFY) $(KTLINT_CHECK_TASKS)

run: ## Run desktop app
	./gradlew :shared:run

# Debug interval in seconds for `run-debug` (override: make run-debug PW_INTERVAL=10)
PW_INTERVAL ?= 5
run-debug: ## Run desktop app in debug mode (verbose logs + PW_INTERVAL beat, default 5s)
	PW_DEBUG=1 PW_INTERVAL=$(PW_INTERVAL) ./gradlew :shared:run

clean: ## Clean build outputs
	./gradlew clean

check: lint test ## Lint + tests

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------

build-android: ## Build Android debug APK
	./gradlew assembleDebug --stacktrace

build-android-release: ## Build Android release APK (requires keystore env vars)
	./gradlew assembleRelease -PappVersion=$(VERSION) --stacktrace

build-desktop: ## Compile desktop JVM target
	./gradlew :shared:compileKotlinJvm --stacktrace

# ---------------------------------------------------------------------------
# Package native installers
# ---------------------------------------------------------------------------

package-deb: ## Package Linux .deb
	./gradlew :shared:packageDeb -PappVersion=$(VERSION) --stacktrace

package-dmg: ## Package macOS .dmg
	./gradlew :shared:packageDmg -PappVersion=$(VERSION) --stacktrace

package-msi: ## Package Windows .msi
	./gradlew :shared:packageMsi -PappVersion=$(VERSION) --stacktrace

package-all: package-deb package-dmg package-msi build-android-release ## Build all platform packages

# ---------------------------------------------------------------------------
# Release
# ---------------------------------------------------------------------------

release-patch: ## Changelog → commit → tag → push (patch: x.y.Z+1)
	@$(MAKE) _release V=$(NEXT_PATCH)

release-minor: ## Changelog → commit → tag → push (minor: x.Y+1.0)
	@$(MAKE) _release V=$(NEXT_MINOR)

release-major: ## Changelog → commit → tag → push (major: X+1.0.0)
	@$(MAKE) _release V=$(NEXT_MAJOR)

# Internal: run as $(MAKE) _release V=x.y.z — never call directly
_release:
	@git diff --quiet && git diff --staged --quiet || (echo "❌ Dirty working tree — commit or stash changes first" && exit 1)
	@echo "Current: v$(VERSION) → Next: v$(V)"
	@read -p "Confirm? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	@if [ "$(SKIP_RELEASE_CHECK)" != "1" ]; then $(MAKE) check; fi
	git-cliff --unreleased --tag "v$(V)" --prepend CHANGELOG.md
	@echo "Review and edit CHANGELOG.md before continuing"
	@read -p "Continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	git add CHANGELOG.md
	git diff --staged --quiet || git commit -m "chore: release v$(V)"
	git tag -a "v$(V)" -m "Release v$(V)"
	git push origin main --tags

# ---------------------------------------------------------------------------
# Setup
# ---------------------------------------------------------------------------

version: ## Show current version
	@echo "v$(VERSION)"

generate-keystore: ## Generate a new Android release keystore
	@if [ -f release.keystore ]; then echo "release.keystore already exists"; exit 1; fi
	keytool -genkeypair -v -keystore release.keystore -alias pulseweaver \
		-keyalg RSA -keysize 2048 -validity 10000 \
		-dname "CN=PulseWeaver, O=PulseWeaver"
	@echo ""
	@echo "Created release.keystore — keep this file safe and never commit it."
	@echo "To set up GitHub Secrets, run: make setup-secrets"

setup-secrets: ## Print instructions for GitHub Actions secrets
	@echo ""
	@echo "Add these secrets to your GitHub repo (Settings → Secrets → Actions):"
	@echo ""
	@echo "  KEYSTORE_BASE64     base64 < release.keystore | pbcopy  (or base64 -w0 on Linux)"
	@echo "  KEYSTORE_PASSWORD   the password you chose during keytool"
	@echo "  KEY_ALIAS           pulseweaver  (or whatever alias you used)"
	@echo ""

# ---------------------------------------------------------------------------
# Hooks
# ---------------------------------------------------------------------------

install-hooks: ## Install git hooks (run once after cloning)
	@ln -sf "$(PWD)/scripts/commit-msg" "$(PWD)/.git/hooks/commit-msg"
	@chmod +x "$(PWD)/.git/hooks/commit-msg"
	@echo "✅ Git hooks installed."
