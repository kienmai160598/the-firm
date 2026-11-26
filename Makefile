SHELL := /bin/bash

JAVA_HOME ?= /opt/homebrew/opt/openjdk@17
ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
export JAVA_HOME ANDROID_HOME

ANDROID_TV := apps/android-tv
APPLE_TV := apps/apple-tv
BOX_APK := $(ANDROID_TV)/app/build/outputs/apk/box/androidTvApp-box.apk
BOX_PACKAGE := com.kien.movies.next
# Core's build demands a distribution for assemble tasks; the bridge uses playstore sources.
GRADLE := cd shared && ./gradlew -Pnuvio.android.distribution=full
KOTLIN_SOURCES := "apps/android-tv/app/src/**/*.kt" "apps/android-tv/app/*.kts" \
	"shared/android-tv-bridge/src/**/*.kt" "shared/android-tv-bridge/*.kts" \
	"shared/torrent/**/*.kt" "shared/torrent/*.kts"
# Inside the checkout, so every build of one checkout reuses it and it goes with its worktree.
APPLE_TV_DERIVED := $(CURDIR)/$(APPLE_TV)/build/DerivedData
APPLE_TV_APP := $(APPLE_TV_DERIVED)/Build/Products/Release-appletvos/NuvioTv.app
APPLE_TV_SIM := 'platform=tvOS Simulator,name=Apple TV 4K (4th generation)'

.PHONY: help check lint format tv-runtime-init-check android-tv android-tv-test box-install \
	box-roundtrip apple-tv-project apple-tv apple-tv-sim apple-tv-test apple-tv-install shared-test \
	engine-next engine-test engine-android

help:
	@echo "check             shared, engine and Android TV tests, the Android engine build, lint and the TvRuntime init check"
	@echo "lint              ktlint on the Android TV app, its bridge and shared/torrent"
	@echo "format            ktlint --format on the same sources"
	@echo "android-tv        box APK ($(BOX_APK))"
	@echo "android-tv-test   unit tests of the Android TV app and bridge"
	@echo "box-install       install the box APK with its profile (ANDROID_SERIAL=<ip>:5555)"
	@echo "box-roundtrip     persistence round trip on the box (test addon on 127.0.0.1:8765)"
	@echo "apple-tv          Release build for a device (APPLE_TEAM=<team id>)"
	@echo "apple-tv-sim      Debug build for the tvOS simulator"
	@echo "apple-tv-test     Apple TV unit and UI tests on the simulator (ONLY=<target>[/<class>[/<test>]])"
	@echo "apple-tv-install  install the Release build (APPLE_TV_DEVICE=<devicectl id>)"
	@echo "shared-test       Apple TV bridge and torrent module tests on the tvOS simulator"
	@echo "engine-next       build the torrent engine xcframework (engine/build)"
	@echo "engine-test       torrent engine unit tests on this Mac"
	@echo "engine-android    build the torrent engine for Android (engine/build/android/jniLibs)"

check: shared-test engine-test engine-android android-tv-test lint tv-runtime-init-check

lint:
	scripts/ktlint --relative $(KOTLIN_SOURCES)

format:
	scripts/ktlint --format --relative $(KOTLIN_SOURCES)

tv-runtime-init-check:
	scripts/check-tv-runtime-init

android-tv: engine-android
	$(GRADLE) :androidTvApp:assembleBox

android-tv-test:
	$(GRADLE) :androidTvApp:testDebugUnitTest :androidTvShared:testAndroidHostTest :torrent:testAndroidHostTest

box-install: android-tv
	BOX_PACKAGE=$(BOX_PACKAGE) $(ANDROID_TV)/box/install.sh $(BOX_APK)

box-roundtrip: engine-android
	$(GRADLE) :androidTvApp:assembleDebug :androidTvApp:assembleDebugAndroidTest
	$(ANDROID_TV)/box/roundtrip.sh

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

apple-tv-test: apple-tv-project
	xcodebuild -project $(APPLE_TV)/NuvioTv.xcodeproj -scheme NuvioTv -configuration Debug \
		-destination $(APPLE_TV_SIM) -derivedDataPath $(APPLE_TV_DERIVED)-sim \
		$(if $(ONLY),-only-testing:$(ONLY)) test

apple-tv-install: apple-tv
	xcrun devicectl device install app --device "$${APPLE_TV_DEVICE:?set APPLE_TV_DEVICE}" \
		$(APPLE_TV_APP)

shared-test: engine-next
	cd shared && ./gradlew :tvosShared:tvosSimulatorArm64Test :torrent:tvosSimulatorArm64Test

engine-next:
	engine/scripts/build-xcframework.sh

engine-test:
	engine/scripts/test.sh

engine-android:
	engine/scripts/build-android.sh
