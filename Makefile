.PHONY: help test build-android build-desktop run clean \
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
# Development
# ---------------------------------------------------------------------------

test: ## Run all tests (JVM)
	./gradlew shared:jvmTest --stacktrace

lint: ## Run ktlint
	./gradlew ktlintCheck

run: ## Run desktop app
	./gradlew :shared:run

clean: ## Clean build outputs
	./gradlew clean

check: lint test

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
