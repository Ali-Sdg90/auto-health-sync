Build:

.\gradlew.bat assembleDebug

---------------------------

Deploy:

C:\Android\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

---------------------------

Advanced Build:

cd "C:\Users\alisd\OneDrive\Documents\GitHub\auto-health-sync"

function Read-PlainSecret($Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)

    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$env:ANDROID_HOME = "C:\Android"
$env:ANDROID_SDK_ROOT = "C:\Android"
$env:ANDROID_KEYSTORE_PATH = "C:\Users\alisd\.android\keystores\auto-health-sync-release.jks"
$env:ANDROID_KEYSTORE_PASSWORD = Read-PlainSecret "Keystore password"
$env:ANDROID_KEY_ALIAS = "auto-health-sync"
$env:ANDROID_KEY_PASSWORD = Read-PlainSecret "Key password"

.\gradlew.bat --no-daemon testDebugUnitTest lintRelease assembleRelease

Remove-Item Env:ANDROID_KEYSTORE_PASSWORD
Remove-Item Env:ANDROID_KEY_PASSWORD
