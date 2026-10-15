# 0.1.0 驗證紀錄

- 使用 Eclipse ECJ 3.38.0、Java 17 編譯 Android 全部 Java 與測試，配合實際 Garmin ciq-companion-app-sdk 2.2.0 AAR 及 Android 14 API 類別：通過。
- 執行 Java ScoreTest：42 checks PASS，包含開局例外、雙方發球得分、失發球、11 分勝兩分、終局停止，以及無效／重複／過時快照。
- 這是 Java 參考計分器及 Android 資料驗證器的測試，未執行 Monkey C 計分器，因此不將 Java 結果當作 G1 執行保證。
- Garmin Connect IQ SDK 9.2.0，Monkey C 預設型別檢查，無指定裝置通用編譯：BUILD SUCCESSFUL。
- G1 專用編譯：被缺少裝置定義阻擋，尚未完成。SDK Manager 的裝置下載需要使用者自行登入 Garmin。
- Android APK：aapt2 35、D8 8.7.18，min SDK 26、target 35，簽章 v2/v3 驗證通過。不是透過 Gradle 全流程建置；Gradle 專案仍需另驗證。
- 未執行 Android UI 測試、S25 安裝啟動、G1 模擬器、G1 裝置或實際藍牙；沒有量測延遲、距離、耗電。

測試範圍限制：API 類別編譯不能證明 Android 執行行為與 Garmin Connect 路由。第一個實機目標是傳送快照並收回 ACK。
