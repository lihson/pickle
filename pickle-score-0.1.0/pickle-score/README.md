# 匹克球計分：Garmin Descent G1 ＋ Samsung S25

第一版 0.1.0，雙打傳統發球得分制。手錶記分，手機顯示；不含相機辨識。

## 目前交付狀態

- `dist/pickle-score-0.1.0-debug.apk`：可安裝的 Android 測試版，已編譯及驗證簽章，尚未在 S25 啟動測試。
- `watch/`：完整 Connect IQ 原始碼，已使用 Garmin SDK 9.2.0 通用編譯及預設型別檢查通過。
- **尚未產生可供 G1 安裝的專用 PRG**：目前環境缺少 G1 裝置定義；需在 Garmin SDK Manager 登入帳號並下載 Descent G1 定義後建置。
- Android 全部 Java 原始碼已用實際 Garmin SDK 2.2.0 與 Android 14 API 類別檢查通過。
- Java 計分參考模型與 Android 真正使用的資料驗證器共 42 項檢查通過。
- 這些檢查不等於手錶實測：G1 記憶體、實體按鍵、版面、藍牙傳送與背景行為仍待驗證。

## 最先可以做的事：安裝手機版

1. S25 安裝並登入 Garmin Connect，完成 G1 配對。
2. 將 `dist/pickle-score-0.1.0-debug.apk` 傳到 S25，開啟安裝；必要時允許該檔案來源安裝應用程式。
3. 開啟「匹克球計分」，按「連接 G1」，允許附近裝置／藍牙權限，選擇 G1。
4. 在手錶專用程式尚未安裝前，手機會停在等待比分，這是預期狀態。

## G1 按鍵設計

畫面 A＝我方，B＝對方；身份固定，不隨球場換邊改變。第一版英文字介面，避免 G1 字型差異。

| 按鍵 | 功能 |
|---|---|
| UP | A 贏這一回合 |
| DOWN | B 贏這一回合 |
| BACK | 復原上一筆，最多保留 20 筆狀態 |
| START | 手動送出目前比分 |
| 長按 MENU | 開啟選單，選 A／B 首發以開始新局，或退出 App |

按鍵到行為的對應尚須在 G1 實機確認。新局會覆蓋目前比賽，第一版沒有多局歷史。

## 計分行為

開局 0－0－2；發球方贏加分。第一發球員輸回合，切第二發球員；第二發球員輸回合，換對方第一發球員。預設 11 分、領先 2 分結束，結束後停止一般加分，仍可復原或修正。

例：A 發球、5:3、第 1 發球員。B 贏 → 5:3、第 2 發球員；B 再贏 → B 發球、報分 3－5－1。

手錶保存完整最新比分與有限復原歷史，離線可記分。手機收到狀態後先存檔再回傳 ACK。每 3 秒傳目前狀態，以便重連恢復；不傳送完整每球歷史。未確認或超時時標記不同步。

手機的「修正／復原」都是送回手錶執行，手機不會先自行改分。若指令送出前後手錶已進入新版，修正可能被拒絕；以手錶回傳的版本為準，重新操作即可。

## 在 Windows 電腦建置手錶版

1. 安裝 VS Code、Garmin Monkey C 擴充套件，以及 Garmin Connect IQ SDK Manager。
2. 在 SDK Manager 登入自己的 Garmin 帳號，安裝 SDK，並在 Devices 下載 **Descent G1**。
3. 用 VS Code 開啟 `watch` 資料夾。透過 Monkey C 的 developer key 功能產生自己的開發金鑰，妥善保留。
4. 選擇 `descentg1` 目標，執行 Monkey C 的 Build for Device。或在 `watch` 目錄執行：

```sh
monkeyc -f monkey.jungle -d descentg1 -o ../dist/pickle-score-g1.prg -y /absolute/path/developer_key.der
```

5. 用 USB 將 G1 連到電腦，依 Garmin 裝置側載方式將 PRG 放進 `GARMIN/APPS`，安全退出後在手錶開啟 App。
6. Android／手錶專案使用同一個固定應用 UUID。請勿只改其中一邊；側載版本的訊息路由需實機確認。

本專案不包含你的 Garmin 帳號或手錶簽署私鑰。通用編譯輸出不作為 G1 安裝檔交付。

## 建置 Android

使用 Android Studio 開啟 `android/`，準備 JDK 17、Android SDK 35 與 Gradle 8.9（本專案未附 Gradle wrapper 二進位）。Android Gradle Plugin 8.7.3。以 Build APK 建置；命令列可先用 Gradle 8.9 產生 wrapper：

```sh
cd android
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

Windows 使用 `gradlew.bat assembleDebug`。輸出 `app/build/outputs/apk/debug/app-debug.apk`。

`android/debug.keystore` 是這個原型的公開測試簽章（密碼 android），僅用來讓測試 APK 可持續更新，不能用作正式上架簽章。

## 本機測試

有 JDK 17 時，在專案根目錄執行：

```sh
sh tests/run.sh
```

Windows 可在 Git Bash 執行，或手動用 javac 編譯腳本列出的 3 個檔案，再執行 ScoreTest。

## 實機驗收順序

1. 手錶離線：開局、雙方贏回合、兩次換發球、復原、重開 App 恢復資料。
2. 近距離連線：手機顯示與手錶一致，手錶顯示 SYNC OK。
3. 手機修正：送到手錶後兩邊一致；快速連按及重傳不重複計分。
4. 關藍牙後在手錶記數回合，再開藍牙：手機追上最新比分。
5. 手機旋轉、重新啟動 App：舊資料標記為最後保存狀態，重新訂閱後恢復同步。
6. 測球場兩端距離與人體遮蔽；不保證藍牙可涵蓋整座球場。

第一版要求手機比分 App 與手錶記分 App 保持前景，不宣稱背景即時同步；不要停用 Garmin Connect。尚未驗證關閉網際網路時 Garmin SDK 的實際行為。

## 官方參考

- https://developer.garmin.com/connect-iq/compatible-devices/
- https://developer.garmin.com/connect-iq/api-docs/Toybox/Communications.html
- https://github.com/garmin/connectiq-android-sdk
- https://developer.garmin.com/connect-iq/connect-iq-basics/getting-started/
