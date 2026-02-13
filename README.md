# QuickArrive

一款全面的 Minecraft 伺服器傳送管理插件，提供玩家互傳、傳送點管理、傳送門整合與黑名單系統。
遊戲版本要求 Java 1.20.1
伺服器端 Paper端
可與 EssentialX，AncientGates等傳送插件聯動

## 功能特色

### 🎯 主要功能

- **傳送確認系統** - 攔截所有傳送動作，要求玩家確認後才執行
- **玩家互傳** - 支援 TPA（傳送過去）和 TPAHere（請他過來）
- **傳送點管理** - 每位玩家最多 2 個傳送點（包含 EssentialsX 的 /home）
- **傳送門整合** - 直接讀取 AncientGates 的傳送門資料
- **黑名單系統** - 玩家可阻擋特定玩家的傳送請求
- **更新提醒** - 自動檢查 GitHub 新版本並通知管理員
- **圖形化選單** - 完整的 GUI 操作介面，所有功能都能透過選單執行

### ⚡ 傳送控制

- **傳送延遲** - 可設定傳送前的等待時間（預設 3 秒）
- **倒數提示** - 傳送前每秒顯示剩餘時間
- **移動取消** - 移動或受傷時自動取消傳送
- **超時機制** - 傳送請求會在指定時間後自動過期
- **權限繞過** - 管理員或特定玩家可繞過確認系統

### 🎨 使用者介面

- **可點擊按鈕** - 聊天欄顯示可點擊的 [接受傳送] 和 [拒絕傳送] 按鈕
- **玩家頭顱** - 選單中顯示線上玩家的頭顱
- **分頁系統** - 自動分頁顯示大量物品
- **置中排版** - 美觀的選單布局設計
- **自訂圖示** - 可自訂所有選單物品的材質與名稱

## 指令

### 玩家指令

| 指令 | 說明 | 權限 |
|------|------|------|
| `/tpmenu` | 開啟主選單 | `quickarrive.use` |
| `/quickarrive` | 開啟主選單（可用 `/qa` 代替） | `quickarrive.use` |
| `/tpa <玩家>` | 請求傳送到該玩家 | `quickarrive.use` |
| `/tpahere <玩家>` | 請求該玩家傳送過來 | `quickarrive.use` |
| `/tpaccept` | 接受傳送請求 | `quickarrive.use` |
| `/tpdeny` | 拒絕傳送請求 | `quickarrive.use` |

### 管理員指令

| 指令 | 說明 | 權限 |
|------|------|------|
| `/tpmenu admin` | 開啟管理員面板 | `quickarrive.admin` |
| `/tpmenu setpoint <名稱>` | 建立傳送點 | `quickarrive.admin` |
| `/tpmenu delpoint <名稱>` | 刪除傳送點 | `quickarrive.admin` |
| `/tpmenu give [玩家]` | 給予傳送選單工具 | `quickarrive.menu.give` |
| `/tpmenu points` | 列出所有傳送點 | `quickarrive.admin` |
| `/qa reload` | 重新載入設定與資料 | `quickarrive.admin` |

## 權限

| 權限 | 說明 | 預設 |
|------|------|------|
| `quickarrive.use` | 使用基本功能 | true |
| `quickarrive.admin` | 管理傳送點 | op |
| `quickarrive.bypass` | 繞過傳送確認 | op |
| `quickarrive.menu.give` | 給予選單工具 | op |
| `quickarrive.menu.admin` | 使用管理員選單 | op |

## 選單系統

### 主選單
點擊指南針工具或執行 `/tpmenu` 開啟，包含：
- **玩家** - 查看線上玩家並發送傳送請求
- **傳點** - 管理個人傳送點
- **傳送門** - 使用 AncientGates 傳送門
- **管理面板** - 管理員專用（需權限）

### 玩家選單
- 顯示所有線上玩家的頭顱
- 左鍵：傳送過去（TPA）
- 右鍵：請他過來（TPAHere）
- 右上角屏障：進入黑名單管理

### 黑名單選單
- 顯示所有線上玩家
- 點擊切換黑名單狀態
- 被加入黑名單的玩家無法向你發送傳送請求

### 傳點選單
- 顯示 EssentialsX 的 /home（唯讀）
- 顯示 QuickArrive 的傳送點（可編輯）
- 左鍵：傳送
- 右鍵：編輯名稱
- Shift+右鍵：刪除
- 空欄位：點擊建立新傳送點

### 傳送門選單
- 顯示所有已開放的 AncientGates 傳送門
- 點擊傳送到該傳送門
- 僅顯示管理員允許的傳送門

### 管理員面板
- 查看所有傳送門
- 左鍵：切換開放/關閉
- 右鍵：編輯傳送門資料
  - 顯示名稱
  - 顯示 ID
  - 圖示材質
  - 傳送座標（世界、X、Y、Z、Yaw、Pitch）
  - 可覆寫預設傳送點
- **世界傳送**（管理員專用）- 顯示所有已載入世界，點擊即可傳送到該世界重生點

## 設定檔

### config.yml 重要設定

```yaml
teleport:
  timeout-seconds: 60        # 傳送請求超時時間（秒）
  delay-seconds: 3           # 傳送前延遲時間（秒）
  intercept-essentialsx: true # 攔截 EssentialsX 的 TPA 指令
  cancel-on-move: false      # 移動時取消傳送
  cancel-on-damage: false    # 受傷時取消傳送

bypass:
  players: []                # 繞過確認的玩家名單

updater:
  enabled: true              # 啟用更新檢查
  check-interval-minutes: 60 # 檢查間隔（分鐘）

menu:
  title: "&0QuickArrive"     # 選單標題
  size: 54                   # 選單大小（9、18、27、36、45、54）
  background-material: BLACK_STAINED_GLASS_PANE  # 背景方塊
  tool:
    enabled: true            # 啟用選單工具
    material: COMPASS        # 工具材質
    name: "&bQuickArrive 選單"
```

### 訊息自訂

所有訊息都可在 `config.yml` 的 `messages` 區段自訂：
- 傳送請求訊息
- 按鈕文字與顏色
- 成功/失敗提示
- 倒數計時文字

## 整合支援

### EssentialsX
- 讀取玩家的 /home 資料
- 攔截 `/tpa`、`/tpahere`、`/tpaccept`、`/tpdeny` 指令
- 計入 2 個傳送點上限

### AncientGates
- 自動讀取 `plugins/AncientGates/gates.json`
- 管理員控制哪些傳送門開放給玩家
- 可覆寫傳送門的目標座標
- 自訂傳送門顯示名稱與圖示

## 安裝

### 從 GitHub 下載並自行打包

1. 下載原始碼：
  - 直接下載 ZIP（GitHub 頁面 -> Code -> Download ZIP），或
  - 使用 git：`git clone https://github.com/junginlau/QuickArrive.git`
2. 進入專案資料夾。
3. 使用 Gradle 打包：
  - Windows：`gradlew.bat build`
  - Linux/Mac：`./gradlew build`
4. 打包完成後，jar 位置在 `build/libs/`。
5. 將 jar 放入伺服器的 `plugins` 資料夾並重啟。

#### Gradle Wrapper 使用方式

- Windows：`gradlew.bat build`
- Windows（簡化）：`build.bat`
- Linux/Mac：`./gradlew build`
- 停止背景 Gradle 程序：`./gradlew --stop`

#### GitHub Actions 自動打包

- 每次推送或 PR 會自動執行 `./gradlew build`
- 設定檔位置：`.github/workflows/build.yml`

### 直接安裝

1. 下載 `quickarrive-1.0.0.jar`
2. 放入伺服器的 `plugins` 資料夾
3. 重啟伺服器
4. （選用）編輯 `plugins/QuickArrive/config.yml` 調整設定
5. 執行 `/tpmenu` 開始使用

## 相容性

- **Minecraft 版本**: 1.20.1+
- **伺服器軟體**: Paper / Spigot
- **Java 版本**: 17+
- **選用依賴**:
  - EssentialsX（讀取玩家 homes）
  - AncientGates（讀取傳送門資料）

## 檔案結構

```
plugins/QuickArrive/
├── config.yml           # 主設定檔
├── players.yml          # 玩家資料（黑名單、傳送點）
└── gates.yml            # 傳送門設定（開放狀態、自訂資料）
```

## 使用範例

### 玩家互傳流程
1. 玩家 A 執行 `/tpa PlayerB` 或在選單點擊玩家 B
2. 玩家 B 收到聊天提示與可點擊按鈕
3. 玩家 B 點擊 [接受傳送] 或執行 `/tpaccept`
4. 倒數 3 秒後玩家 A 傳送到玩家 B

### 建立傳送點
1. 玩家移動到想設定的位置
2. 開啟 `/tpmenu` → 傳點
3. 點擊空欄位建立傳送點
4. 傳送點會自動命名為 slot1 或 slot2

### 管理傳送門
1. 管理員執行 `/tpmenu admin`
2. 查看所有 AncientGates 傳送門
3. 左鍵切換開放/關閉
4. 右鍵編輯顯示資料與座標

## 技術特點

- **非同步處理** - 不影響伺服器效能
- **UUID 儲存** - 黑名單使用 UUID 避免改名問題
- **事件優先級** - 正確攔截其他插件的傳送
- **記憶體優化** - 使用 ConcurrentHashMap 處理併發
- **可擴展性** - 模組化設計易於新增功能

## 常見問題

**Q: 如何繞過傳送確認？**  
A: 給予玩家 `quickarrive.bypass` 權限，或將玩家名稱加入 `config.yml` 的 `bypass.players` 清單。

**Q: 傳送點上限包含 EssentialsX 的 home 嗎？**  
A: 是的，EssentialsX 的 /home 會計入 2 個傳送點上限。

**Q: 如何關閉 EssentialsX 指令攔截？**  
A: 在 `config.yml` 設定 `teleport.intercept-essentialsx: false`。

**Q: 選單工具遺失了怎麼辦？**  
A: 執行 `/tpmenu give` 或 `/tpmenu give <玩家名稱>` 重新取得。

**Q: AncientGates 傳送門沒有顯示？**  
A: 確認 `plugins/AncientGates/gates.json` 存在，並在管理員面板開放該傳送門。

## 授權

此插件為自訂開發，請勿未經許可重新分發。

## 支援

如有問題或建議，請聯繫伺服器管理員。

---

**版本**: 1.0.0  
**最後更新**: 2026年2月
