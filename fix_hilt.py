import os

app_build = 'android/app/build.gradle.kts'
with open(app_build, 'r') as f:
    content = f.read()

content = content.replace(
    'id("com.google.dagger.hilt.android")',
    'id("com.google.dagger.hilt.android")\n    id("kotlin-kapt")'
)

content = content.replace(
    'implementation("com.google.dagger:hilt-android:2.50")',
    'implementation("com.google.dagger:hilt-android:2.50")\n    kapt("com.google.dagger:hilt-compiler:2.50")'
)

with open(app_build, 'w') as f:
    f.write(content)
