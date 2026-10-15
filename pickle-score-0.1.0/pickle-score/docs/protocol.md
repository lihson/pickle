# 同步協定 v1

Watch 為單一權威，Android 只鏡像。透過 Connect IQ BLE 通道傳 Dictionary／Map，不使用自行建立的 GATT server。

## 狀態

```json
{"v":1,"type":"state","session":"unique-game-id","revision":4,"a":5,"b":3,"serving":0,"server":2}
```

`serving` 0=A／1=B；`server` 1 或 2。每個修改含 undo 都增加 revision，undo 不使 revision 倒退。新局新 session、revision=0。

Android 拒絕無效欄位、負分、超過 99 分、小數、同局舊版本。相同版本只接受相同資料並重新 ACK。不同 session 由使用者確認是否切換。成功存檔才 ACK；檢查存檔返回值，失敗時不送 ACK，仍需實機注入測試。

## ACK

```json
{"v":1,"type":"ack","session":"unique-game-id","revision":4}
```

手錶僅在 app-level ACK 相符時顯示 SYNC OK，不把 BLE transmit onComplete 視為對方 App 已記錄。每 3 秒重送最新狀態；超過 10 秒無有效 ACK 顯示未確認。資料永久保存在手錶，不依賴計時器存活。當 App 不在前景不保證傳送。

## 修正指令

```json
{"v":1,"type":"command","session":"unique-game-id","base":4,"action":"set","a":6,"b":3,"serving":0,"server":2}
```

undo 指令 action=undo，省略分數欄位。只有 session 及 base 與手錶當下狀態完全相符才執行。執行後 revision+1；重複指令的 base 已過期，自然不重複執行。沒有歷史可 undo 時不變。

持久化採最新快照＋最多 20 筆復原狀態，不是完整事件日誌。手機重連時收到最新快照即可恢復顯示，不保證回放每一回合。選擇另一隻手錶時現有比賽需以 session 確認切換。
