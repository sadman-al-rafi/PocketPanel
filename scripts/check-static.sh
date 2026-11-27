#!/usr/bin/env bash
#
# Local static checks for PocketPanel.
#
# Runs without a JDK, Android SDK, Gradle or network access. It verifies
# structure and cross-references only; it cannot prove that the code
# compiles or that the UI renders. Build and unit-test verification
# requires an authorized manual CI run (AGENTS.md section 13, D031).
#
# Usage: scripts/check-static.sh
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

failures=0
checks=0

fail() {
    printf 'FAIL  %s\n' "$1"
    failures=$((failures + 1))
}

ok() {
    checks=$((checks + 1))
}

section() {
    printf '\n== %s ==\n' "$1"
}

# --- Required files ---------------------------------------------------------

section "Required files"

required_files=(
    "gradlew"
    "gradle/wrapper/gradle-wrapper.properties"
    "gradle/wrapper/gradle-wrapper.jar"
    "settings.gradle.kts"
    "build.gradle.kts"
    "gradle/libs.versions.toml"
    "app/build.gradle.kts"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/dev/pocketpanel/MainActivity.kt"
    "app/src/main/java/dev/pocketpanel/PocketPanelApplication.kt"
    "app/src/main/java/dev/pocketpanel/ui/theme/Theme.kt"
    "app/src/main/java/dev/pocketpanel/ui/PocketPanelApp.kt"
    "app/src/main/java/dev/pocketpanel/ui/PocketPanelViewModel.kt"
    "app/src/main/java/dev/pocketpanel/ui/main/MainScreen.kt"
    "app/src/main/java/dev/pocketpanel/ui/settings/SettingsScreen.kt"
    "app/src/main/java/dev/pocketpanel/settings/SettingsRepository.kt"
    "app/src/main/java/dev/pocketpanel/settings/DataStoreSettingsRepository.kt"
    "app/src/main/java/dev/pocketpanel/vpn/VpnRepository.kt"
    "app/src/main/java/dev/pocketpanel/ui/main/VpnCard.kt"
    "app/src/test/java/dev/pocketpanel/vpn/WireGuardConfigParserTest.kt"
    "app/src/main/java/dev/pocketpanel/settings/HandleEdge.kt"
    "app/src/main/java/dev/pocketpanel/overlay/OverlayContent.kt"
    "app/src/main/java/dev/pocketpanel/overlay/OverlayGeometry.kt"
    "app/src/main/java/dev/pocketpanel/overlay/OverlayLifecycleOwner.kt"
    "app/src/main/java/dev/pocketpanel/overlay/OverlayPermission.kt"
    "app/src/main/java/dev/pocketpanel/overlay/OverlayService.kt"
    "app/src/main/java/dev/pocketpanel/overlay/OverlayWindowController.kt"
    "app/src/test/java/dev/pocketpanel/overlay/OverlayGeometryTest.kt"
    "app/src/test/java/dev/pocketpanel/settings/HandleEdgeTest.kt"
    "app/src/main/java/dev/pocketpanel/settings/HandleSize.kt"
    "app/src/main/java/dev/pocketpanel/settings/MainCardLayout.kt"
    "app/src/test/java/dev/pocketpanel/settings/SettingsEnumsTest.kt"
    "app/src/main/java/dev/pocketpanel/VpnToggleActivity.kt"
    "app/src/main/java/dev/pocketpanel/system/CpuUsage.kt"
    "app/src/main/java/dev/pocketpanel/system/CpuUsageMath.kt"
    "app/src/test/java/dev/pocketpanel/system/CpuUsageMathTest.kt"
    "app/src/main/java/dev/pocketpanel/system/GpuInfo.kt"
    "app/src/main/java/dev/pocketpanel/system/GpuInfoProvider.kt"
    "app/src/main/java/dev/pocketpanel/system/HardwareSnapshot.kt"
    "app/src/main/java/dev/pocketpanel/system/MemoryUsage.kt"
    "app/src/test/java/dev/pocketpanel/system/MemoryUsageTest.kt"
    "app/src/main/java/dev/pocketpanel/overlay/OverlayMonitoring.kt"
    "app/src/main/java/dev/pocketpanel/ui/main/HardwareCard.kt"
    "app/src/main/res/drawable/ic_notification.xml"
    "app/src/main/res/values/strings.xml"
    "app/src/main/res/values/themes.xml"
    "app/src/main/res/values-night/themes.xml"
    "app/src/main/res/drawable/ic_launcher_foreground.xml"
    "app/src/main/res/mipmap-anydpi/ic_launcher.xml"
    "app/src/main/res/xml/data_extraction_rules.xml"
    "scripts/check-static.sh"
    ".github/workflows/verify-toolchain.yml"
    ".github/workflows/release.yml"
    "release/version.properties"
    "docs/RELEASING.md"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        ok
    else
        fail "missing required file: $file"
    fi
done

# --- XML well-formedness ----------------------------------------------------

# No XML parser is installed, so this checks tag nesting and balance while
# ignoring comments, declarations and self-closing tags. The whole file is
# buffered so that tags split over several lines are handled.
check_xml() {
    local file=$1
    if awk '
        { buf = buf $0 "\n" }
        END {
            ok = 1
            while ((i = index(buf, "<!--")) > 0) {
                rest = substr(buf, i + 4)
                j = index(rest, "-->")
                if (j == 0) {
                    print "unterminated comment"
                    ok = 0
                    buf = substr(buf, 1, i - 1)
                    break
                }
                buf = substr(buf, 1, i - 1) substr(rest, j + 3)
            }
            while (match(buf, /<[^>]*>/)) {
                tag = substr(buf, RSTART, RLENGTH)
                buf = substr(buf, RSTART + RLENGTH)
                if (tag ~ /^<\?/) continue
                if (tag ~ /^<!/) continue
                if (tag ~ /\/>$/) continue
                if (tag ~ /^<\//) {
                    name = tag
                    sub(/^<\//, "", name)
                    sub(/[ \t\r\n>].*$/, "", name)
                    if (depth == 0) {
                        printf "unexpected closing tag </%s>\n", name
                        ok = 0
                    } else if (stack[depth] != name) {
                        printf "closing </%s> does not match <%s>\n", name, stack[depth]
                        ok = 0
                    } else {
                        depth--
                    }
                } else {
                    name = tag
                    sub(/^</, "", name)
                    sub(/[ \t\r\n\/>].*$/, "", name)
                    depth++
                    stack[depth] = name
                }
            }
            if (depth != 0) { printf "unclosed <%s>\n", stack[depth]; ok = 0 }
            exit (ok ? 0 : 1)
        }
    ' "$file"; then
        ok
    else
        fail "malformed XML: $file"
    fi
}

section "XML well-formedness"

while IFS= read -r file; do
    check_xml "$file"
done < <(find app/src/main/res app/src/main/AndroidManifest.xml -name '*.xml' -type f | sort)

# --- Resource declarations and references -----------------------------------

section "Resource references"

declared_strings=$(grep -o '<string name="[^"]*"' app/src/main/res/values/strings.xml \
    | sed -E 's/.*name="([^"]*)"/\1/' | sort -u)
declared_drawables=$(find app/src/main/res -type f \
    \( -path '*/drawable*/*' -o -path '*/mipmap*/*' \) \
    | sed -E 's#.*/##; s/\.(xml|png|webp|jpg)$//' | sort -u)
declared_styles=$(grep -ho '<style name="[^"]*"' app/src/main/res/values*/themes.xml \
    | sed -E 's/.*name="([^"]*)"/\1/' | sort -u)
declared_colors=$(grep -ho '<color name="[^"]*"' app/src/main/res/values*/colors.xml \
    | sed -E 's/.*name="([^"]*)"/\1/' | sort -u)
declared_xml=$(find app/src/main/res/xml -type f -name '*.xml' 2>/dev/null \
    | sed -E 's#.*/##; s/\.xml$//' | sort -u)

verify_refs() {
    local kind=$1 pattern=$2 strip=$3 declared=$4
    local name
    while IFS= read -r name; do
        [ -n "$name" ] || continue
        if ! grep -qxF -- "$name" <<< "$declared"; then
            fail "$kind '$name' is referenced but not declared"
        fi
    done < <(grep -rhoE "$pattern" app/src 2>/dev/null | sed -E "$strip" | sort -u)
}

verify_refs "string" 'R\.string\.[A-Za-z0-9_]+' 's/^R\.string\.//' "$declared_strings"
verify_refs "string" '@string/[A-Za-z0-9_.]+' 's#^@string/##' "$declared_strings"
verify_refs "drawable" 'R\.drawable\.[A-Za-z0-9_]+' 's/^R\.drawable\.//' "$declared_drawables"
verify_refs "drawable" '@drawable/[A-Za-z0-9_.]+' 's#^@drawable/##' "$declared_drawables"
verify_refs "mipmap" 'R\.mipmap\.[A-Za-z0-9_]+' 's/^R\.mipmap\.//' "$declared_drawables"
verify_refs "mipmap" '@mipmap/[A-Za-z0-9_.]+' 's#^@mipmap/##' "$declared_drawables"
verify_refs "style" '@style/[A-Za-z0-9_.]+' 's#^@style/##' "$declared_styles"
verify_refs "color" '@color/[A-Za-z0-9_.]+' 's#^@color/##' "$declared_colors"
verify_refs "xml" '@xml/[A-Za-z0-9_.]+' 's#^@xml/##' "$declared_xml"
ok

duplicates=$(grep -ho '<string name="[^"]*"' app/src/main/res/values/strings.xml \
    | sed -E 's/.*name="([^"]*)"/\1/' | sort | uniq -d)
if [ -n "$duplicates" ]; then
    fail "duplicate string names: $(tr '\n' ' ' <<< "$duplicates")"
else
    ok
fi

# --- Manifest wiring --------------------------------------------------------

section "Manifest wiring"

manifest=app/src/main/AndroidManifest.xml

if grep -q 'android:name=".PocketPanelApplication"' "$manifest" \
    && [ -f app/src/main/java/dev/pocketpanel/PocketPanelApplication.kt ]; then
    ok
else
    fail "manifest application class is missing or has no source file"
fi

if grep -q 'android:name=".MainActivity"' "$manifest" \
    && [ -f app/src/main/java/dev/pocketpanel/MainActivity.kt ]; then
    ok
else
    fail "manifest activity class is missing or has no source file"
fi

# The manifest may only declare permissions required by an implemented
# milestone (docs/ARCHITECTURE.md section 8). Extend this list together with
# the feature that uses the permission.
allowed_permissions="
android.permission.SYSTEM_ALERT_WINDOW
android.permission.FOREGROUND_SERVICE
android.permission.FOREGROUND_SERVICE_SPECIAL_USE
android.permission.POST_NOTIFICATIONS
android.permission.ACCESS_NETWORK_STATE
android.permission.INTERNET
"
declared_permissions=$(grep -oE 'android\.permission\.[A-Z_]+' "$manifest" | sort -u)
for permission in $declared_permissions; do
    if grep -qwF -- "$permission" <<< "$allowed_permissions"; then
        ok
    else
        fail "undeclared-by-policy permission in manifest: $permission"
    fi
done

service_source=app/src/main/java/dev/pocketpanel/overlay/OverlayService.kt
controller_source=app/src/main/java/dev/pocketpanel/overlay/OverlayWindowController.kt
permission_source=app/src/main/java/dev/pocketpanel/overlay/OverlayPermission.kt

if grep -q 'android:name=".overlay.OverlayService"' "$manifest" \
    && [ -f "$service_source" ]; then
    ok
else
    fail "manifest overlay service is missing or has no source file"
fi

if grep -q 'android:exported="false"' "$manifest"; then
    ok
else
    fail "overlay service must not be exported"
fi

if grep -q 'android:foregroundServiceType="specialUse"' "$manifest"; then
    ok
else
    fail "overlay service must declare the specialUse foreground type"
fi

if grep -q 'android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE' "$manifest"; then
    ok
else
    fail "overlay service is missing the special-use subtype property"
fi

if grep -q 'TYPE_APPLICATION_OVERLAY' "$controller_source"; then
    ok
else
    fail "overlay window does not use TYPE_APPLICATION_OVERLAY"
fi

if grep -q 'FLAG_NOT_FOCUSABLE' "$controller_source"; then
    ok
else
    fail "overlay window must be non-focusable"
fi

if grep -q 'START_NOT_STICKY' "$service_source"; then
    ok
else
    fail "overlay service must use START_NOT_STICKY"
fi

if grep -q 'ServiceCompat.startForeground' "$service_source"; then
    ok
else
    fail "overlay service must call startForeground"
fi

if grep -q 'canDrawOverlays' "$permission_source"; then
    ok
else
    fail "overlay permission helper does not check Settings.canDrawOverlays"
fi

if grep -rq 'BOOT_COMPLETED' app/src/main; then
    fail "overlay must not start from a boot receiver"
else
    ok
fi

if grep -rq 'TYPE_PHONE\|TYPE_SYSTEM_ALERT' app/src/main/java; then
    fail "legacy overlay window types are not allowed"
else
    ok
fi

# --- CPU monitoring ---------------------------------------------------------

section "CPU monitoring"

cpu_sampler=app/src/main/java/dev/pocketpanel/system/CpuUsageSampler.kt
main_screen=app/src/main/java/dev/pocketpanel/ui/main/MainScreen.kt
strings=app/src/main/res/values/strings.xml

if grep -q 'CpuUsageMath.fromFrequencies' "$cpu_sampler"; then
    ok
else
    fail "CPU sampler has no cpufreq fallback"
fi

if grep -q 'cpuUsageIsEstimate' "$main_screen" \
    && grep -q 'monitoring_cpu_usage_estimated' "$strings"; then
    ok
else
    fail "estimated CPU values are not labelled in the UI"
fi

if grep -q 'val cpuUsageIsEstimate' \
    app/src/main/java/dev/pocketpanel/system/DeviceInfo.kt; then
    ok
else
    fail "DeviceInfo does not expose the CPU estimate flag"
fi

# --- M10 customization ------------------------------------------------------

section "M10 customization"

settings_store=app/src/main/java/dev/pocketpanel/settings/DataStoreSettingsRepository.kt
settings_screen=app/src/main/java/dev/pocketpanel/ui/settings/SettingsScreen.kt
overlay_controller=app/src/main/java/dev/pocketpanel/overlay/OverlayWindowController.kt
overlay_content=app/src/main/java/dev/pocketpanel/overlay/OverlayContent.kt

if grep -q 'handle_size' "$settings_store" \
    && grep -q 'main_card_layout' "$settings_store" \
    && grep -q 'setHandleSize' "$settings_store"; then
    ok
else
    fail "M10 customization settings are not persisted"
fi

if grep -q 'MainCardLayout.entries' "$settings_screen" \
    && grep -q 'HandleSize.entries' "$settings_screen"; then
    ok
else
    fail "settings screen is missing the customization options"
fi

if grep -q 'android:name=".VpnToggleActivity"' "$manifest" \
    && grep -q 'android:taskAffinity=""' "$manifest" \
    && grep -q 'FLAG_ACTIVITY_NEW_TASK' \
    app/src/main/java/dev/pocketpanel/overlay/OverlayService.kt; then
    ok
else
    fail "overlay VPN toggle cannot request system consent invisibly"
fi

if grep -q 'TestTags.OVERLAY_VPN_BUTTON' "$overlay_content" \
    && grep -q 'vpnRepository.state' \
    app/src/main/java/dev/pocketpanel/overlay/OverlayService.kt; then
    ok
else
    fail "overlay VPN control does not use the shared repository state"
fi

# --- Advanced metrics -------------------------------------------------------

section "Advanced metrics"

gpu_provider=app/src/main/java/dev/pocketpanel/system/GpuInfoProvider.kt
hardware_card=app/src/main/java/dev/pocketpanel/ui/main/HardwareCard.kt
overlay_content=app/src/main/java/dev/pocketpanel/overlay/OverlayContent.kt

if grep -q 'GLES20.glGetString' "$gpu_provider" \
    && grep -q 'eglDestroyContext' "$gpu_provider"; then
    ok
else
    fail "GPU identity provider is missing or does not release its EGL context"
fi

if grep -q 'hardware_gpu_note' "$hardware_card" \
    && ! grep -rq 'ThermalLevel\|getThermalHeadroom' app/src/main/java; then
    ok
else
    fail "hardware card or thermal removal is incomplete"
fi

if grep -q 'overlay_metric_cpu' "$overlay_content" \
    && grep -q 'overlay_metric_ping' "$overlay_content" \
    && grep -q 'MemoryUsage.usedPercent' "$overlay_content"; then
    ok
else
    fail "overlay 2x2 monitoring grid is incomplete"
fi

overlay_service=app/src/main/java/dev/pocketpanel/overlay/OverlayService.kt
if grep -q 'restartMonitoring' "$overlay_service" \
    && grep -q 'if (!preferences.monitoringEnabled)' "$overlay_service"; then
    ok
else
    fail "overlay monitoring is not gated by the monitoring setting"
fi

# --- VPN wiring -------------------------------------------------------------

section "VPN wiring"

vpn_repository=app/src/main/java/dev/pocketpanel/vpn/VpnRepository.kt

if grep -q 'module = "com.wireguard.android:tunnel"' gradle/libs.versions.toml \
    && grep -q 'libs.wireguard.tunnel' app/build.gradle.kts; then
    ok
else
    fail "WireGuard tunnel library is not declared in the catalog and app"
fi

if grep -q 'isCoreLibraryDesugaringEnabled = true' app/build.gradle.kts \
    && grep -q 'libs.desugar.jdk.libs' app/build.gradle.kts; then
    ok
else
    fail "core library desugaring for the WireGuard library is missing"
fi

if grep -q 'VpnService.prepare' "$vpn_repository"; then
    ok
else
    fail "VPN connect path does not request system consent"
fi

if grep -q 'BIND_VPN_SERVICE' "$manifest"; then
    fail "VPN service comes from the library AAR; do not redeclare it"
else
    ok
fi

if grep -rq 'startForeground' app/src/main/java/dev/pocketpanel/vpn; then
    fail "VPN must not run as an app foreground service"
else
    ok
fi

# --- Version catalog --------------------------------------------------------

section "Version catalog"

catalog=gradle/libs.versions.toml

catalog_entries=$(awk '
    /^\[/ { section = $0; gsub(/[\[\]]/, "", section); next }
    /^[A-Za-z0-9_.-]+[ \t]*=/ {
        name = $0
        sub(/[ \t]*=.*/, "", name)
        print section "|" name
    }
' "$catalog")

while IFS= read -r accessor; do
    [ -n "$accessor" ] || continue
    path=${accessor#libs.}
    section_name="libraries"
    if [[ $path == plugins.* ]]; then
        section_name="plugins"
        path=${path#plugins.}
    elif [[ $path == versions.* ]]; then
        section_name="versions"
        path=${path#versions.}
    fi
    alias=${path//./-}
    if ! grep -qxF -- "$section_name|$alias" <<< "$catalog_entries"; then
        fail "version catalog alias missing: [$section_name] $alias (from $accessor)"
    fi
done < <(grep -rhoE 'libs\.[A-Za-z0-9_.]+' \
    app/build.gradle.kts build.gradle.kts settings.gradle.kts 2>/dev/null | sort -u)
ok

while IFS= read -r ref; do
    [ -n "$ref" ] || continue
    if ! grep -qxF -- "versions|$ref" <<< "$catalog_entries"; then
        fail "version reference '$ref' has no [versions] entry"
    fi
done < <(grep -hoE 'version\.ref[ \t]*=[ \t]*"[^"]+"' "$catalog" \
    | sed -E 's/.*"([^"]+)"/\1/' | sort -u)
ok

# --- Theme and UI rules -----------------------------------------------------

section "Theme and UI rules"

hardcoded=$(grep -rnE 'Color\(0x' app/src/main/java | grep -v '/ui/theme/' || true)
if [ -n "$hardcoded" ]; then
    fail "hardcoded color outside ui/theme:"
    printf '%s\n' "$hardcoded" | sed 's/^/      /'
else
    ok
fi

test_tags_file=app/src/main/java/dev/pocketpanel/ui/TestTags.kt
tag_names=$(grep -oE 'const val [A-Z_]+' "$test_tags_file" | awk '{print $3}' | sort -u)

for name in $tag_names; do
    if ! grep -rqF "TestTags.$name" app/src --include='*.kt' --exclude=TestTags.kt; then
        fail "TestTags.$name is declared but never used"
    fi
done
for name in $(grep -rhoE 'TestTags\.[A-Z_]+' app/src --include='*.kt' | sed 's/TestTags\.//' | sort -u); do
    if ! grep -qxF -- "$name" <<< "$tag_names"; then
        fail "unknown test tag constant: TestTags.$name"
    fi
done
while IFS= read -r line; do
    if ! grep -qE 'testTag\((TestTags\.[A-Z_]+|"[^"]+"|[A-Za-z_][A-Za-z0-9_]*\.testTag)\)' <<< "$line"; then
        fail "testTag() argument is not a stable tag: $line"
    fi
done < <(grep -rn 'testTag(' app/src/main/java --include='*.kt')
if grep -rqE 'val [A-Za-z_][A-Za-z0-9_]*\.testTag' app/src/main/java --include='*.kt'; then
    ok
else
    fail "testTag() is called through a property that is not defined"
fi

# --- Workflow policy --------------------------------------------------------

section "Workflow policy"

workflow=.github/workflows/verify-toolchain.yml

if grep -q 'workflow_dispatch:' "$workflow"; then
    ok
else
    fail "workflow has no workflow_dispatch trigger"
fi

if grep -qE '^  push:' "$workflow"; then
    fail "workflow must not run automatically (D031)"
else
    ok
fi

if grep -qE '^  (pull_request|schedule):' "$workflow"; then
    fail "workflow must not run on pull requests or schedules"
else
    ok
fi

if grep -qi 'emulator' "$workflow"; then
    fail "workflow must not install or boot an emulator (D016)"
else
    ok
fi

if grep -qE '^\s+- name: Run unit tests' "$workflow" \
    && grep -qE ':app:testDebugUnitTest' "$workflow"; then
    ok
else
    fail "workflow no longer runs the JVM unit tests"
fi

# --- Release infrastructure -------------------------------------------------

section "Release infrastructure"

app_build=app/build.gradle.kts
version_file=release/version.properties
release_workflow=.github/workflows/release.yml

if grep -q 'applicationId = "dev.pocketpanel"' "$app_build"; then
    ok
else
    fail "release application ID changed from dev.pocketpanel"
fi

if grep -qE '^versionCode=[0-9]+$' "$version_file" \
    && grep -qE '^versionName=.+$' "$version_file" \
    && grep -q 'release/version.properties' "$app_build"; then
    ok
else
    fail "version file is missing, malformed, or not read by Gradle"
fi

if grep -q 'System.getenv("POCKETPANEL_RELEASE_' "$app_build" \
    && grep -q 'keystore.properties' "$app_build" \
    && grep -q 'signingConfig = signingConfigs.getByName("release")' "$app_build"; then
    ok
else
    fail "release signing is not wired to secure inputs"
fi

tracked_secrets=$(git ls-files | grep -E '(^|/)(keystore\.properties|.*\.(jks|keystore))$' || true)
if [ -z "$tracked_secrets" ]; then
    ok
else
    fail "signing material is tracked by Git:"
    printf '%s\n' "$tracked_secrets" | sed 's/^/      /'
fi

on_block=$(awk '/^on:/{flag=1;next} /^[a-zA-Z]/{flag=0} flag' "$release_workflow")
if grep -q 'workflow_dispatch:' <<< "$on_block" \
    && ! grep -qE '^[[:space:]]+(push|pull_request|schedule|workflow_run|release):' <<< "$on_block"; then
    ok
else
    fail "release workflow is not manual-only"
fi

if grep -q 'environment: release' "$release_workflow" \
    && grep -q "inputs.confirm == 'PUBLISH'" "$release_workflow" \
    && grep -q 'inputs.dry_run' "$release_workflow" \
    && grep -q 'RELEASE_NOTES' "$release_workflow" \
    && grep -q -- '--notes-file' "$release_workflow" \
    && grep -q 'verify --print-certs' "$release_workflow" \
    && grep -q "tr -d ':'" "$release_workflow" \
    && grep -q 'git archive' "$release_workflow" \
    && grep -q 'gh release create' "$release_workflow"; then
    ok
else
    fail "release workflow is missing approval, verification or publishing steps"
fi

# --- Source hygiene ---------------------------------------------------------

section "Source hygiene"

markers=$(grep -rnE 'TODO|FIXME' app/src/main/java app/src/main/res 2>/dev/null || true)
if [ -n "$markers" ]; then
    fail "unresolved markers in application sources:"
    printf '%s\n' "$markers" | sed 's/^/      /'
else
    ok
fi

# --- Result -----------------------------------------------------------------

printf '\n----------------------------------------\n'
if [ "$failures" -eq 0 ]; then
    printf 'Static checks passed (%d checks).\n' "$checks"
    printf 'This does not prove compilation or runtime behavior.\n'
    exit 0
fi

printf 'Static checks failed: %d failure(s), %d passed check(s).\n' \
    "$failures" "$checks"
exit 1
