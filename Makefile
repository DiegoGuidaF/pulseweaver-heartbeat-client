.PHONY: help test build-android build-desktop run clean \
       package-deb package-dmg package-msi package-all \
       release release-patch release-minor release-major \
       generate-keystore setup-secrets lint

VERSION ?= $(shell (git describe --tags --abbrev=0 2>/dev/null || echo v1.0.0) | sed 's/^v//')
NEXT_PATCH = $(shell echo $(VERSION) | awk -F. '{printf "%d.%d.%d", $$1, $$2, $$3+1}')
NEXT_MINOR = $(shell echo $(VERSION) | awk -F. '{printf "%d.%d.0", $$1, $$2+1}')
NEXT_MAJOR = $(shell echo $(VERSION) | awk -F. '{printf "%d.0.0", $$1+1}')

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

release-patch: ## Tag and push a patch release (x.y.Z+1)
	@echo "Current: v$(VERSION) → Next: v$(NEXT_PATCH)"
	@read -p "Confirm? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	git tag -a "v$(NEXT_PATCH)" -m "Release v$(NEXT_PATCH)"
	git push origin main --tags

release-minor: ## Tag and push a minor release (x.Y+1.0)
	@echo "Current: v$(VERSION) → Next: v$(NEXT_MINOR)"
	@read -p "Confirm? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	git tag -a "v$(NEXT_MINOR)" -m "Release v$(NEXT_MINOR)"
	git push origin main --tags

release-major: ## Tag and push a major release (X+1.0.0)
	@echo "Current: v$(VERSION) → Next: v$(NEXT_MAJOR)"
	@read -p "Confirm? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	git tag -a "v$(NEXT_MAJOR)" -m "Release v$(NEXT_MAJOR)"
	git push origin main --tags

# ---------------------------------------------------------------------------
# Setup
# ---------------------------------------------------------------------------

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

version: ## Show current version
	@echo "$(VERSION)"
