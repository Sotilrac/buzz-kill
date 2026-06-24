GRADLE := ./gradlew

.PHONY: help bootstrap hooks lint format compile verify test check apk apk-debug install-debug install icon clean printversion

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

bootstrap: ## Install git hooks and warm the Gradle cache
	$(MAKE) hooks
	$(GRADLE) help -q

hooks: ## Install lefthook git hooks
	lefthook install

format: ## Auto-format Kotlin with ktlint
	$(GRADLE) :app:ktlintFormat

lint: ## Run ktlint + detekt + Android lint
	$(GRADLE) :app:ktlintCheck :app:detekt :app:lintDebug

compile: ## Compile debug Kotlin (no tests)
	$(GRADLE) :app:compileDebugKotlin

verify: ## Fast pre-commit loop: compile debug + detekt + unit tests
	$(GRADLE) :app:compileDebugKotlin :app:detekt :app:testDebugUnitTest

test: ## Run the pure-JVM unit tests
	$(GRADLE) :app:testDebugUnitTest

check: ## Full verification (lint + all tests)
	$(GRADLE) check

apk: ## Build the release APK
	$(GRADLE) :app:assembleRelease

apk-debug: ## Build the debug APK (no install)
	$(GRADLE) :app:assembleDebug

install-debug: ## Build + install the debug APK on a connected device
	$(GRADLE) :app:installDebug

install: install-debug ## Alias for install-debug

icon: ## Regenerate launcher mipmaps from tools/icon-1024.png
	$(GRADLE) :app:generateIcons

printversion: ## Print the app versionName (single source of truth)
	@grep -E '^\s*versionName\s*=' app/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/'

clean: ## Remove build outputs
	$(GRADLE) clean
