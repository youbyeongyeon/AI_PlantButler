plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // 이 줄을 추가하세요
    alias(libs.plugins.google.devtools.ksp) apply false
}