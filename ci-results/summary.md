## Build result: `success`

### Resolved Kotlin / Compose entries
```
          +--- androidx.compose.foundation:foundation-layout:1.12.0 -> 1.13.0 (*)
          +--- androidx.compose.foundation:foundation:1.12.0 -> 1.13.0 (*)
          +--- androidx.compose.material:material-ripple:1.12.0 -> 1.13.0
          +--- androidx.compose.runtime:runtime:1.12.0 -> 1.13.0 (*)
          +--- androidx.compose.ui:ui-text:1.12.0 -> 1.13.0 (*)
          +--- androidx.compose.ui:ui:1.12.0 -> 1.13.0 (*)
          \--- org.jetbrains.kotlin:kotlin-stdlib:2.2.20 -> 2.4.21 (*)
          |         +--- androidx.compose.foundation:foundation:1.7.1 -> 1.13.0 (*)
          |         +--- androidx.compose.runtime:runtime:1.13.0 (*)
          |         \--- org.jetbrains.kotlin:kotlin-stdlib:2.2.20 -> 2.4.21 (*)
          |    \--- androidx.compose.material:material-ripple-android:1.13.0
     \--- androidx.compose.material3:material3-android:1.5.0
+--- androidx.activity:activity-compose:1.13.0
+--- androidx.compose.foundation:foundation -> 1.13.0
+--- androidx.compose.ui:ui -> 1.13.0 (*)
+--- androidx.compose:compose-bom:2026.11.01
+--- androidx.lifecycle:lifecycle-runtime-compose:2.11.1 (*)
+--- androidx.lifecycle:lifecycle-viewmodel-compose:2.11.1
+--- org.jetbrains.kotlin:kotlin-stdlib:2.4.21
\--- androidx.compose.material3:material3 -> 1.5.0
|         +--- androidx.compose.animation:animation:1.13.0
|         +--- androidx.compose.runtime:runtime-saveable:1.11.0 -> 1.13.0 (*)
|         +--- androidx.compose.runtime:runtime:1.11.0 -> 1.13.0 (*)
|         +--- androidx.compose.runtime:runtime:1.13.0 (*)
|         +--- androidx.compose.ui:ui:1.10.0 -> 1.13.0 (*)
|         +--- androidx.lifecycle:lifecycle-runtime-compose:2.11.1 (c)
|         +--- org.jetbrains.kotlin:kotlin-stdlib:2.1.20 -> 2.4.21 (*)
|         +--- org.jetbrains.kotlin:kotlin-stdlib:2.2.20 -> 2.4.21 (*)
|         +--- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0 (*)
|         +--- org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3 (*)
|         \--- androidx.compose.foundation:foundation-layout:1.13.0 (c)
|         |         +--- androidx.compose.animation:animation-core:1.13.0
|         |         +--- androidx.compose.foundation:foundation-layout:1.10.0 -> 1.13.0
|         |         +--- androidx.compose.runtime:runtime:1.13.0 (*)
|         |         +--- androidx.compose.ui:ui-geometry:1.10.0 -> 1.13.0 (*)
|         |         +--- org.jetbrains.kotlin:kotlin-stdlib:2.2.20 -> 2.4.21 (*)
|         |         +--- org.jetbrains.kotlinx:atomicfu:0.28.0
|         |         +--- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0 (*)
|         |         \--- androidx.compose.animation:animation-core:1.13.0 (c)
|         |         |         +--- androidx.compose.ui:ui:1.13.0 (*)
```

### Buildscript Kotlin / AGP entries
```
+--- org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21
|    +--- org.jetbrains.kotlin:kotlin-gradle-plugins-bom:2.4.21
|    |    +--- org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.4.21 (c)
|    |    +--- org.jetbrains.kotlin:fus-statistics-gradle-plugin:2.4.21 (c)
|    |    +--- org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21 (c)
|    |    +--- org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.21 (c)
|    |    +--- org.jetbrains.kotlin:kotlin-gradle-plugin-annotations:2.4.21 (c)
|    |    +--- org.jetbrains.kotlin:kotlin-native-utils:2.4.21 (c)
|    |    \--- org.jetbrains.kotlin:kotlin-tooling-core:2.4.21 (c)
|    +--- org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.4.21
|    |    +--- org.jetbrains.kotlin:kotlin-gradle-plugins-bom:2.4.21 (*)
|    |    +--- org.jetbrains.kotlin:kotlin-gradle-plugin-annotations:2.4.21
|    |    +--- org.jetbrains.kotlin:kotlin-native-utils:2.4.21
|    |    |    +--- org.jetbrains.kotlin:kotlin-util-io:2.4.21
|    |    |    \--- org.jetbrains.kotlin:kotlin-util-klib:2.4.21
|    |    |         \--- org.jetbrains.kotlin:kotlin-util-io:2.4.21
|    |    +--- org.jetbrains.kotlin:kotlin-tooling-core:2.4.21
|    |    \--- org.jetbrains.kotlin:kotlin-build-tools-api:2.4.21
|    +--- org.jetbrains.kotlin:fus-statistics-gradle-plugin:2.4.21
|    |    \--- org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.4.21 (*)
|    +--- org.jetbrains.kotlin:kotlin-gradle-plugin-idea:2.4.21
|    |    +--- org.jetbrains.kotlin:kotlin-tooling-core:2.4.21
|    |    \--- org.jetbrains.kotlin:kotlin-gradle-plugin-annotations:2.4.21
|    +--- org.jetbrains.kotlin:kotlin-gradle-plugin-idea-proto:2.4.21
|    |    +--- org.jetbrains.kotlin:kotlin-tooling-core:2.4.21
|    |    \--- org.jetbrains.kotlin:kotlin-gradle-plugin-idea:2.4.21 (*)
|    +--- org.jetbrains.kotlin:kotlin-klib-commonizer-api:2.4.21
|    |    \--- org.jetbrains.kotlin:kotlin-native-utils:2.4.21 (*)
|    +--- org.jetbrains.kotlin:kotlin-build-statistics:2.4.21
|    |    +--- org.jetbrains.kotlin:kotlin-build-tools-api:2.4.21
|    +--- org.jetbrains.kotlin:kotlin-util-klib-metadata:2.4.21
|    |    +--- org.jetbrains.kotlin:kotlin-util-io:2.4.21
|    |    \--- org.jetbrains.kotlin:kotlin-util-klib:2.4.21 (*)
|    \--- org.jetbrains.kotlin:kotlin-compiler-runner:2.4.21
|         +--- org.jetbrains.kotlin:kotlin-daemon-client:2.4.21
|         \--- org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.0 -> 1.9.0
|              +--- org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.9.0
|              |    +--- org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0 (c)
|              |    \--- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0 (c)
|              \--- org.jetbrains.kotlin:kotlin-stdlib:2.0.0 -> 2.3.21
```

### Unit tests
```
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
BUILD SUCCESSFUL in 1s
```

### APK
```
total 27380
-rw-r--r-- 1 runner runner 28031663 Nov 27 15:33 app-debug.apk
-rw-r--r-- 1 runner runner      399 Nov 27 15:33 output-metadata.json
654bbb86756bcf7571d1dab9af9853ce2677af4dd40958811c6ed46289cd16a2  app/build/outputs/apk/debug/app-debug.apk
```
