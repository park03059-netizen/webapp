# 맞춤사진 Android 앱

`https://park03059-netizen.github.io/parkminseok/`을 여는 전체화면 Android WebView 앱입니다. **Android Studio 없이 GitHub Actions에서 설치 가능한 APK를 빌드**합니다.

## GitHub에서 APK 만들기

1. GitHub에서 새 저장소를 만듭니다. 기존 웹사이트 저장소와 분리해도 됩니다.
2. 이 프로젝트의 파일과 폴더를 **저장소 최상위**에 올립니다. `.github/workflows/android.yml`, `app`, `gradle`, `gradlew`, `gradlew.bat`이 포함되어야 합니다. 소스 ZIP을 사용한다면 먼저 압축을 풀어 올립니다. ZIP 자체를 올리면 빌드되지 않습니다.
3. 저장소의 **Actions → Build Android APK**를 엽니다. Push하면 자동 실행되며, **Run workflow**로 직접 실행할 수도 있습니다. Actions가 비활성화되어 있으면 먼저 활성화합니다.
4. 성공한 실행을 열어 하단 **Artifacts → fitphoto-apk**를 내려받고 압축을 풉니다.
5. `fitphoto-debug.apk`를 Android 휴대폰으로 옮겨 실행합니다. 설치 화면에서 요청하면 해당 브라우저/파일 앱의 **알 수 없는 앱 설치**를 허용합니다.

아티팩트는 30일 보관됩니다. 만료되면 Actions를 다시 실행하세요. SDK, Java, Gradle 설치는 Actions가 처리하며 GitHub Secrets 없이 빌드할 수 있습니다.

### `Failed to find package 'tools'`로 실패한 경우

`setup-android@v3`의 기본 패키지 목록에는 더 이상 제공되지 않는 `tools`가 포함되어 있습니다. 이 프로젝트는 `packages: 'platform-tools'`를 명시해 이를 제외하고, 다음 단계에서 Android 36과 Build Tools 35.0.0을 설치합니다. 수정된 `.github/workflows/android.yml`을 GitHub에 커밋한 뒤 **새 실행**을 사용하세요. 이전 실패 실행의 Re-run은 이전 workflow를 사용하므로 수정 사항이 반영되지 않습니다.

Git을 사용한다면 아래 명령으로 업로드할 수 있습니다. GitHub에서 빈 저장소를 만든 후 `YOUR_REPOSITORY`를 실제 저장소 이름으로 바꾸세요. 최초 push에는 GitHub 로그인이 필요합니다.

```powershell
git init
git add .
git commit -m "Add fullscreen Android WebView app"
git branch -M main
git remote add origin https://github.com/park03059-netizen/YOUR_REPOSITORY.git
git push -u origin main
```

`.gitignore`가 로컬 SDK, Gradle 캐시, 빌드 결과를 제외합니다. `.github` 폴더는 반드시 업로드하세요. 기존 저장소에 넣는 경우에는 기존 앱 파일이나 workflow를 덮어쓰지 않도록 별도 저장소를 권장합니다.

## 앱 기능

- 앱 이름: **맞춤사진** / 패키지: `io.github.park03059.fitphoto`
- Android 8.0 이상(API 26), compile/target SDK 36
- 주소 표시줄과 앱 상단바 없이 표시하며 상태바·내비게이션 바를 숨깁니다. 화면 가장자리 스와이프로 시스템 바를 잠시 표시할 수 있습니다. 데스크톱/분할 화면 등 시스템 창 모드에서는 OS가 표시를 제한할 수 있습니다.
- JavaScript, DOM storage, 사진 단일·복수 선택 지원
- 사이트의 Blob/Data URL 사진·GIF·ZIP 내려받기를 Android **다른 이름으로 저장** 창에 연결합니다. 파일 하나당 최대 24MB이며, 저장 위치를 직접 고릅니다. 저장 또는 취소 후 다음 파일을 내려받으세요.
- 뒤로 가기: 웹 방문 기록을 먼저 이동하고, 기록이 없으면 앱을 종료합니다.
- 연결 오류 시 다시 시도 화면을 표시합니다.
- 사이트 내부 경로는 앱에서 열고, 외부 HTTP(S)·전화·메일 링크는 외부 앱에서 엽니다. 일반 HTTP(S) 파일 내려받기도 브라우저에서 처리합니다.
- 인터넷 권한만 선언합니다. 사진 선택·저장은 Android 파일 선택기를 이용하므로 저장소 전체 접근 권한이 필요하지 않습니다.
- HTTPS만 앱 안에서 사용하고 SSL 오류를 우회하지 않습니다. 파일 저장 메시지는 지정한 HTTPS 출처의 최상위 페이지에서만 받습니다.

웹사이트를 온라인으로 표시하는 앱이므로 최초 로딩과 사이트 갱신에는 인터넷이 필요합니다. 사이트 수정은 다음 로딩에 반영됩니다. 파일 저장 기능을 쓰려면 Android System WebView/Chrome을 최신으로 유지하세요. 프로세스가 종료되면 편집 중인 사진과 미완료 저장 작업은 복원되지 않으므로 다시 선택·내려받아야 합니다.

## APK 서명

기본 결과물은 설치 가능한 **개발/테스트용 debug APK**입니다. Play Store 배포용 서명은 포함하지 않습니다. Actions의 새 실행에서는 개발 서명키가 달라질 수 있어 이전 APK 위에 설치가 안 되면 기존 앱을 삭제한 뒤 설치해야 합니다. 이때 앱의 로컬 데이터가 삭제됩니다. 지속적인 배포 업데이트가 필요하다면 본인 소유의 고정 release keystore와 GitHub Secrets를 추가해야 합니다.

## 수정 위치

- 웹사이트 URL과 탐색 허용 범위: `app/src/main/java/io/github/park03059/fitphoto/MainActivity.java`의 `HOME`, `ORIGIN`, `isInternal()`
- 사진 저장 연결: `app/src/main/assets/downloads.js`
- 앱 이름: `app/src/main/res/values/strings.xml`
- 패키지/버전: `app/build.gradle` (패키지 변경 시 Java 경로와 namespace도 함께 수정)
- 아이콘 원본: `assets/app-icon.png`
- 앱에서 사용하는 아이콘 이미지: `app/src/main/res/drawable-nodpi/icon_art.png`
- 빌드 자동화: `.github/workflows/android.yml`

## Android Studio 없는 로컬 빌드 (선택)

GitHub Actions만 사용한다면 이 단계는 필요 없습니다. 로컬에서는 JDK 17과 Android SDK Command-line Tools가 필요합니다.

```powershell
# ANDROID_HOME을 SDK 설치 경로로 설정한 후 실행
sdkmanager 'platforms;android-36' 'build-tools;35.0.0'
sdkmanager --licenses
.\gradlew.bat lintDebug assembleDebug
```

결과: `app/build/outputs/apk/debug/app-debug.apk`

## 빌드 구성과 아이콘

Gradle 8.13 + Android Gradle Plugin 8.13.2 + JDK 17을 고정했습니다. Gradle 배포 파일은 SHA-256으로 확인하고 Actions에서도 Wrapper를 검증합니다. 빌드 작업은 Android lint 통과 후 APK를 업로드합니다.

2026-11-11에 Windows 명령줄에서 `lintDebug assembleDebug` 성공, APK 개발 서명 검증, 앱 이름·최소/대상 SDK 확인을 완료했습니다. JavaScript 사진 저장 연결도 파일명·데이터 전달, 클릭 처리, 용량 초과 및 구형 WebView 안내를 검사했습니다. Lint의 비차단 경고는 남아 있으며, 실기기 실행과 실제 GitHub Actions 실행은 아직 검증하지 않았습니다.

아이콘은 내장 ImageGen 도구로 제작한 흰색 P 모노그램과 네이비·틸 배경입니다. Android adaptive icon으로 연결하여 기기의 원형·둥근 사각형 마스크를 따릅니다. 생성 프롬프트는 `assets/icon-prompt.txt`에 있습니다.

공식 참고: [Android immersive mode](https://developer.android.com/develop/ui/views/layout/immersive), [AndroidX WebKit](https://developer.android.com/jetpack/androidx/releases/webkit), [AGP 호환성](https://developer.android.com/build/releases/gradle-plugin).
