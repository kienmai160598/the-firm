SHELL := /bin/bash

JAVA_HOME ?= /opt/homebrew/opt/openjdk@17
ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
export JAVA_HOME ANDROID_HOME

ANDROID_TV := apps/android-tv
APPLE_TV := apps/apple-tv
BOX_APK := $(ANDROID_TV)/app/build/outputs/apk/full/box/app-full-armeabi-v7a-box.apk
APPLE_TV_DERIVED := /tmp/the-firm-apple-tv
APPLE_TV_APP := $(APPLE_TV_DERIVED)/Build/Products/Release-appletvos/NuvioTv.app
APPLE_TV_SIM := 'platform=tvOS Simulator,name=Apple TV 4K (4th generation)'

.PHONY: help android-tv android-tv-test box-install apple-tv-project apple-tv apple-tv-sim \
	apple-tv-install shared-test engine

help:
	@echo "android-tv        box APK ($(BOX_APK))"
	@echo "android-tv-test   Android TV unit tests"
	@echo "box-install       install the box APK with its profile (ANDROID_SERIAL=<ip>:5555)"
	@echo "apple-tv          Release build for a device (APPLE_TEAM=<team id>)"
	@echo "apple-tv-sim      Debug build for the tvOS simulator"
	@echo "apple-tv-install  install the Release build (APPLE_TV_DEVICE=<devicectl id>)"
	@echo "shared-test       Apple TV bridge tests on the tvOS simulator"
	@echo "engine            build the torrent engine xcframework (about 16 minutes)"

android-tv:
	cd $(ANDROID_TV) && ./gradlew :app:assembleFullBox

android-tv-test:
	cd $(ANDROID_TV) && ./gradlew :app:testFullDebugUnitTest

box-install: android-tv
	$(ANDROID_TV)/box/install.sh $(BOX_APK)

apple-tv-project:
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

shared-test:
	cd shared && ./gradlew :tvosShared:tvosSimulatorArm64Test

engine:
	engine/build.sh
