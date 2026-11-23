SHELL := /bin/bash

JAVA_HOME ?= /opt/homebrew/opt/openjdk@17
ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
# Torrent engine the Apple TV app links: legacy (nuvio-engine) or next (engine/). See D2 in
# docs/engine-rebuild.md. Gradle reads it from the environment, also inside the Xcode build.
NUVIO_TVOS_ENGINE ?= legacy
export JAVA_HOME ANDROID_HOME NUVIO_TVOS_ENGINE

ANDROID_TV := apps/android-tv
APPLE_TV := apps/apple-tv
BOX_APK := $(ANDROID_TV)/app/build/outputs/apk/full/box/app-full-armeabi-v7a-box.apk
APPLE_TV_DERIVED := /tmp/the-firm-apple-tv
APPLE_TV_APP := $(APPLE_TV_DERIVED)/Build/Products/Release-appletvos/NuvioTv.app
APPLE_TV_SIM := 'platform=tvOS Simulator,name=Apple TV 4K (4th generation)'
KOTLIN_SOURCES := "app/src/**/*.kt" "baselineprofile/src/**/*.kt" "*.kts" "app/*.kts" \
	"baselineprofile/*.kts"
SHARED_KOTLIN_SOURCES := "shared/torrent/**/*.kt" "shared/torrent/*.kts"
KTLINT_CONFIG := --editorconfig=$(ANDROID_TV)/.editorconfig

.PHONY: help check toolchain-check layering-check lint format android-tv android-tv-test \
	box-install apple-tv-project apple-tv apple-tv-sim apple-tv-install shared-test engine \
	engine-next engine-test

help:
	@echo "check             toolchain check, layering check, lint, engine and Android TV unit tests"
	@echo "layering-check    fail when data, core or domain use ui, or domain uses data"
	@echo "toolchain-check   fail when the two Gradle builds use different toolchain versions"
	@echo "lint              ktlint on the Android TV and shared/torrent Kotlin sources"
	@echo "format            ktlint --format on the Android TV and shared/torrent Kotlin sources"
	@echo "android-tv        box APK ($(BOX_APK))"
	@echo "android-tv-test   Android TV unit tests"
	@echo "box-install       install the box APK with its profile (ANDROID_SERIAL=<ip>:5555)"
	@echo "apple-tv          Release build for a device (APPLE_TEAM=<team id>)"
	@echo "                  any apple-tv target: NUVIO_TVOS_ENGINE=next links the new torrent engine"
	@echo "apple-tv-sim      Debug build for the tvOS simulator"
	@echo "apple-tv-install  install the Release build (APPLE_TV_DEVICE=<devicectl id>)"
	@echo "shared-test       Apple TV bridge (both engines) and torrent module tests on the tvOS simulator"
	@echo "engine            build the old torrent engine xcframework (about 16 minutes)"
	@echo "engine-next       build the new torrent engine xcframework (engine/build)"
	@echo "engine-test       new torrent engine unit tests on this Mac"

check: toolchain-check layering-check lint engine-test android-tv-test

toolchain-check:
	scripts/toolchain-check

layering-check:
	scripts/check-layering

lint:
	cd $(ANDROID_TV) && ../../scripts/ktlint --relative $(KOTLIN_SOURCES)
	scripts/ktlint $(KTLINT_CONFIG) --relative $(SHARED_KOTLIN_SOURCES)

format:
	cd $(ANDROID_TV) && ../../scripts/ktlint --format --relative $(KOTLIN_SOURCES)
	scripts/ktlint --format $(KTLINT_CONFIG) --relative $(SHARED_KOTLIN_SOURCES)

android-tv:
	cd $(ANDROID_TV) && ./gradlew :app:assembleFullBox

android-tv-test:
	cd $(ANDROID_TV) && ./gradlew :app:testFullDebugUnitTest

box-install: android-tv
	$(ANDROID_TV)/box/install.sh $(BOX_APK)

apple-tv-project: engine-next
	cd $(APPLE_TV) && xcodegen generate

apple-tv: apple-tv-project
	xcodebuild -project $(APPLE_TV)/NuvioTv.xcodeproj -scheme NuvioTv -configuration Release \
		-destination 'generic/platform=tvOS' -derivedDataPath $(APPLE_TV_DERIVED) \
		-allowProvisioningUpdates CODE_SIGN_STYLE=Automatic \
		$(if $(APPLE_TEAM),DEVELOPMENT_TEAM=$(APPLE_TEAM)) build

apple-tv-sim: apple-tv-project
	xcodebuild -project $(APPLE_TV)/NuvioTv.xcodeproj -scheme NuvioTv -configuration Debug \
		-destination $(APPLE_TV_SIM) -derivedDataPath $(APPLE_TV_DERIVED)-sim build

apple-tv-install: apple-tv
	xcrun devicectl device install app --device "$${APPLE_TV_DEVICE:?set APPLE_TV_DEVICE}" \
		$(APPLE_TV_APP)

shared-test: engine-next
	cd shared && ./gradlew :tvosShared:tvosSimulatorArm64Test :torrent:tvosSimulatorArm64Test \
		-Pnuvio.tvos.engine=legacy
	cd shared && ./gradlew :tvosShared:tvosSimulatorArm64Test -Pnuvio.tvos.engine=next

engine:
	engine/build.sh

engine-next:
	engine/scripts/build-xcframework.sh

engine-test:
	engine/scripts/test.sh
