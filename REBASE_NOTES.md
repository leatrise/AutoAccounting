# 二改记录 / Rebase 检查笔记

这个文件用于记录当前仓库相对上游仓库 `upstream` 的二次修改，方便后续 `rebase` 时快速检查冲突点、判断哪些改动需要手动重放。

## 当前基线

- 当前分支：`master`
- 我的仓库：`origin = https://github.com/leatrise/AutoAccounting.git`
- 上游仓库：`upstream = https://github.com/AutoAccountingOrg/AutoAccounting.git`
- 记录范围：`upstream/master...HEAD`
- 当前二改提交数：`16`

建议在每次执行 `git fetch upstream` 后，先看这几个命令：

```bash
git log --oneline --reverse upstream/master..HEAD
git diff --name-only upstream/master...HEAD
git diff --stat upstream/master...HEAD
```

如果只是想先判断这次 `rebase` 风险高不高，可以按这个顺序快速过一遍：

```bash
git log --oneline --reverse upstream/master..HEAD
git diff --name-only upstream/master...HEAD
git diff --name-only upstream/master...HEAD -- app/ server/ .github/
```

重点判断三件事：

- 上游有没有改你长期二改最重的区域：`OCR`、`数据库 migration`、`GitHub Actions`
- 你本地有没有连续多个 commit 反复改同一文件，导致 `rebase` 时同一块冲突反复出现
- 这次上游更新里，是否包含你本地已经改过但实现方向不同的功能

## Rebase 前重点检查

以下文件是当前二改最容易与上游冲突的位置：

- `app/build.gradle.kts`
- `app/src/main/java/com/google/android/accessibility/selecttospeak/SelectToSpeakService.kt`
- `app/src/main/java/net/ankio/auto/service/OcrService.kt`
- `app/src/main/java/net/ankio/auto/service/ocr/OcrTools.kt`
- `app/src/main/java/net/ankio/auto/ui/fragment/RuleEditV3Fragment.kt`
- `app/src/main/java/net/ankio/auto/utils/PrefManager.kt`
- `app/src/main/res/xml/settings_interaction.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `server/src/main/java/org/ezbook/server/db/AppDatabase.kt`
- `server/src/main/java/org/ezbook/server/db/Db.kt`
- `server/src/main/java/org/ezbook/server/db/model/RuleModel.kt`
- `server/src/main/java/org/ezbook/server/constant/Setting.kt`
- `server/src/main/java/org/ezbook/server/constant/DefaultData.kt`
- `.github/workflows/android.yml`
- `.github/workflows/main.py`

如果上游也改了这些文件，`rebase` 时不要只看是否能自动合并，还要确认：

- OCR 流程是否仍保留“优先使用无障碍页面文本”的逻辑。
- OCR 成功/失败反馈是否仍符合你的当前约定：成功轻振动，失败仅弹窗，不在 OCR 开始时振动。
- 无障碍页面文本的拼接分隔符是否仍是你需要的格式。
- 设置页里 `ocrAccessibilityPageData` 开关是否还存在，key 和默认值是否一致。
- `RuleModel.description`、数据库 `version` 和 migration 链是否连续一致。
- CI 工作流是否仍保持“以本地自用流程为准”的策略；上游 workflow 变更默认丢弃，除非带来了当前构建链必须接入的新步骤。
- `test/bin/main/*` 这批测试产物是否仍需要保留，避免把上游测试结构更新错覆盖。

## 推荐 Rebase 流程

如果后续要把本地二改重新压到最新上游，建议按这个顺序：

```bash
git fetch upstream
git log --oneline --reverse upstream/master..HEAD
git diff --name-only upstream/master...HEAD
git rebase upstream/master
```

发生冲突时，处理顺序建议固定成下面这样：

1. 先处理 `server` 层数据库和常量定义。
2. 再处理 `app` 层 OCR / 设置页 / UI 逻辑。
3. 最后处理 `.github/workflows/*` 和构建脚本。

原因：

- 数据库版本和 migration 一旦合错，通常不会在代码合并阶段马上暴露，但会在升级时直接出问题。
- OCR 相关改动同时跨 `Service`、`Tools`、`Preference` 和文案资源，适合在常量和存储层稳定后再处理。
- CI 工作流通常以你当前自用流程为准，绝大多数上游改动都应直接放弃；放最后处理，便于只挑出真正需要吸收的新构建要素。

每解决完一批冲突，建议至少执行一次人工复查：

- `git diff --check`
- `git diff --name-only --diff-filter=U`
- `git log --oneline --decorate -n 12`

人工验收重点：

- OCR 入口是否仍可触发，且成功后才轻振动。
- OCR 失败路径是否仍只显示错误横幅，没有误触发成功反馈。
- 设置项 key、默认值、Preference 注册、`PrefManager` 字段是否一一对应。
- 数据库版本号、migration 注册顺序、表字段定义是否一致。
- GitHub Actions 默认保留你当前的自定义流程，不主动追上游实现；只检查上游是否新增了你当前构建链必须接入的步骤、环境变量、产物路径或触发条件。

## 当前二改清单

### 1. `f7581fa0` `feat(ocr): 允许直接使用无障碍数据作为结果`

目的：
- 在 OCR 流程中，允许直接读取无障碍树页面文本作为识别结果，减少截图 OCR 依赖。

涉及文件：
- `app/src/main/java/net/ankio/auto/service/OcrService.kt`
- `app/src/main/java/net/ankio/auto/service/ocr/OcrTools.kt`
- `app/src/main/java/com/google/android/accessibility/selecttospeak/SelectToSpeakService.kt`
- `app/src/main/java/net/ankio/auto/utils/PrefManager.kt`
- `app/src/main/res/xml/settings_interaction.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `server/src/main/java/org/ezbook/server/constant/Setting.kt`
- `server/src/main/java/org/ezbook/server/constant/DefaultData.kt`
- `test/bin/main/*`

关键点：
- 新增设置项：`ocr_accessibility_page_data`
- 默认值：`false`
- 逻辑位置：`OcrService.performOcrCapture(useVision: Boolean)`
- 行为：当 `!useVision && PrefManager.ocrAccessibilityPageData` 时，优先走 `OcrTools.collectPageText()`

Rebase 时确认：
- 上游是否重构了 `performOcrCapture`、OCR 入口或设置项注册方式。
- `SelectToSpeakService.instance` 是否还可直接访问。
- 新版设置页是否仍使用相同 preference key。

### 2. `966321d7` `chore(ocr): 修改无障碍拼接符为逗号`

目的：
- 将页面文本收集后的拼接方式由空格改为逗号，便于后续规则解析。

涉及文件：
- `app/src/main/java/com/google/android/accessibility/selecttospeak/SelectToSpeakService.kt`
- `app/build.gradle.kts`

关键点：
- `joinToString(" ")` 改为 `joinToString(", ")`

Rebase 时确认：
- 上游是否改了文本清洗、去重或拼接逻辑。
- 如果上游新增结构化文本输出，这个逗号策略可能要重新评估。

### 3. `98b543a5` `chore(ocr): 添加空格分隔符`

目的：
- 对无障碍文本拼接格式做过一次中间调整。

涉及文件：
- `app/src/main/java/com/google/android/accessibility/selecttospeak/SelectToSpeakService.kt`

说明：
- 这个提交和后续 `966321d7` 都在修改同一块文本拼接逻辑。
- `rebase` 时如果只看最终结果，可以直接关注最终状态是否仍为逗号拼接，不必机械恢复每一步历史。

### 4. `41f2dd92` `remove time trigger & bump version`

目的：
- 去掉 workflow 中的定时触发，并调整版本相关内容。

涉及文件：
- `.github/workflows/canary.yml`
- `app/build.gradle.kts`

Rebase 时确认：
- 上游版本号策略、构建号策略是否发生变化。
- 若上游已废弃 `canary.yml`，只需要确认你的构建行为是否仍保留。
- 对 workflow 本身，默认仍以你当前本地方案为准，不需要为了跟上游保持一致而回收这些变更。

### 5. `11e3fc4c` `chore: action build changes`

目的：
- 将原有多套 workflow 改成当前自用的 Android Release 构建流程。

涉及文件：
- `.github/workflows/android.yml`
- `.github/workflows/main.py`
- `.github/workflows/beta.yml`
- `.github/workflows/canary.yml`
- `.github/workflows/stable.yml`
- `.github/workflows/key`
- `.gitignore`
- `.idea/misc.xml`
- `app/build.gradle.kts`
- `package.json`
- `package-lock.json`

关键点：
- 新增 `android.yml`
- 删除 `beta.yml`、`canary.yml`、`stable.yml`
- 删除 workflow 内置 `key`
- 精简 `main.py`，移除发布网盘相关逻辑
- 引入 `package.json` / `package-lock.json`

Rebase 时确认：
- 上游是否改了签名参数注入方式、Gradle 构建命令或 release 输出目录。
- 上游如果更新了 workflow 脚本，默认直接丢弃，不主动合并。
- 只有当上游引入了你当前构建链缺失、但实际已经依赖的新步骤时，才选择性把那部分逻辑手动摘进来。
- 这类“可选择吸收”的内容通常包括：
  - 新的构建前置检查
  - 新增的签名或产物处理步骤
  - 新的环境变量约定
  - 发布产物路径或命名规则变化

### 6. `38275d79` `fix: build error`

目的：
- 修复构建脚本相关错误。

涉及文件：
- `app/build.gradle.kts`

Rebase 时确认：
- 上游如果更新 AGP、Kotlin、签名配置、`buildTypes` 或 `productFlavors`，这里最容易再次冲突。

### 7. `bbccfe05` `fix: keystore format error`

目的：
- 修复 CI / 构建中的 keystore 格式问题。

涉及文件：
- `.github/workflows/android.yml`
- `app/build.gradle.kts`

Rebase 时确认：
- 上游如果改了签名读取方式，重新核对：
  - `ANDROID_KEYSTORE_PATH`
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`
  - `KEYSTORE_BASE64`
- 除了这些构建链必需项，不需要因为上游 workflow 改版而同步其余实现细节。

### 8. `2ae5cd3d` `feat: 允许用户自定义规则描述`

目的：
- 在 `add/edit rule` 页面中支持手动编辑规则描述。
- 规则描述不再自动生成默认文案，允许为空。
- 清理已经不再使用的 locale 文案资源。
- 为 `RuleModel` 新增 `description` 持久化字段。

涉及文件：
- `app/src/main/assets/rule/index.html`
- `app/src/main/java/net/ankio/auto/ui/adapter/DataRuleAdapter.kt`
- `app/src/main/java/net/ankio/auto/ui/fragment/RuleEditV3Fragment.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `server/src/main/java/org/ezbook/server/db/model/RuleModel.kt`
- `server/src/main/java/org/ezbook/server/db/AppDatabase.kt`
- `server/src/main/java/org/ezbook/server/db/Db.kt`

关键点：
- `RuleModel` 新增字段：`description: String = ""`
- `AppDatabase` 版本：`21 -> 22`
- 新增迁移：`MIGRATION_21_22`
- 迁移内容：
  - `ALTER TABLE RuleModel ADD COLUMN description TEXT NOT NULL DEFAULT ''`
- 规则编辑页现在有可选的 `description` 输入框，但不再生成任何默认值。
- 规则列表描述现在只显示数据库中的 `description`，为空就空，不再按微信/支付宝/短信/通知类型做兜底文案。
- 已删除不再使用的 locale 字段：
  - `rule_desc_official_template`
  - `rule_desc_user_custom`
  - `rule_desc_type_notice`
  - `rule_desc_type_app`
  - `rule_desc_type_sms`
  - `rule_desc_type_data`
  - `rule_desc_wechat_notice`
  - `rule_desc_alipay_app`
  - `rule_desc_sms_hint`
  - `rule_desc_other_notice`

Rebase 时确认：
- 这是一次数据库结构变更，后续 `rebase` 时必须重点检查 `server/src/main/java/org/ezbook/server/db/AppDatabase.kt` 和 `server/src/main/java/org/ezbook/server/db/Db.kt`。
- 如果上游也改了 `AppDatabase` 的 `version`、新增了别的 migration，不能直接选一边覆盖，必须手动合并：
  - 重新计算最终 `version`
  - 保留双方新增的 migration 注册
  - 确认 migration 起止版本连续、无断档、无重复
- 如果上游也改了 `RuleModel` 表结构，重点检查 `description` 字段是否与上游字段新增发生冲突。
- 如果上游改了规则编辑页或规则列表展示逻辑，确认不要把“允许为空、无默认文案”的行为误合并回旧逻辑。
- 如果上游恢复或新增了这些 locale 字段，需要先确认是否重新引入了默认描述逻辑，不要机械保留删除结果。
- 检查上游是否在其他地方引用了被移除的 locale 字段。

特别注意：
- 本次进行了 `AppDatabase` 升级，这一项在以后 `rebase` 时要特别当心。
- 数据库版本和 migration 冲突通常不会在代码合并时显眼暴露，但会直接影响升级路径；这里必须人工复查，不能只依赖自动合并。

### 9. `249dd74` `refactor: 修改振动机制`

目的：
- 调整 OCR 的触觉反馈策略，避免在识别开始时就振动。
- 改为只有 OCR 成功时给一次轻振动，失败时只保留现有弹窗/横幅提醒。

涉及文件：
- `app/src/main/java/net/ankio/auto/service/OcrService.kt`

关键点：
- 删除 OCR 开始时的统一振动调用。
- 成功分支改为触发轻振动：`VibrationEffect.createOneShot(40, 120)`
- 失败分支不振动，仍走 `ocrView.showError(...)`
- 当前行为约定：
  - 成功：轻振动 + 成功横幅
  - 失败：仅错误横幅

Rebase 时确认：
- 上游如果改了 `executeOcrFlow` 的成功/失败分支，重新确认振动调用仍只出现在成功路径。
- 上游如果引入了新的成功提示组件或统一反馈层，不要把“开始即振动”的旧行为误合并回来。
- 如果上游也改了 `OcrService` 里的 `try/catch`、结果判定或 `ocrView` 调用，注意不要把失败路径误判成成功。
- 如果后续上游引入了系统级 `performHapticFeedback` 或别的触感封装，需要重新决定是否继续保留当前 `Vibrator` 实现。

### 10. `75da542` `feat(ocr): 磁贴自动开启无障碍`

目的：
- 保留原有 OCR 快捷磁贴，在点击时按配置决定是否自动尝试开启本应用无障碍服务。
- 当应用已持有 `WRITE_SECURE_SETTINGS` 时，直接写入 `Settings.Secure` 开启无障碍；否则继续回退到系统无障碍设置页。
- 为该行为新增设置项。

涉及文件：
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/net/ankio/auto/service/OcrService.kt`
- `app/src/main/java/net/ankio/auto/service/OcrTileService.kt`
- `app/src/main/java/net/ankio/auto/service/ocr/OcrTools.kt`
- `app/src/main/java/net/ankio/auto/utils/SystemUtils.kt`
- `app/src/main/java/net/ankio/auto/utils/PrefManager.kt`
- `app/src/main/java/net/ankio/auto/ui/fragment/settings/InteractionPreferenceFragment.kt`
- `app/src/main/res/xml/settings_interaction.xml`
- `server/src/main/java/org/ezbook/server/constant/Setting.kt`
- `server/src/main/java/org/ezbook/server/constant/DefaultData.kt`

关键点：
- 没有新增磁贴，仍使用原有 `OcrTileService`。
- 新增设置项：`ocr_tile_auto_enable_accessibility`
- 默认值：`true`
- 磁贴点击时会透传：`allowAutoEnableAccessibility`
- `OcrService` 在手动 OCR 入口读取该参数，并决定本次是否允许自动开启无障碍。
- `OcrTools.requestPermission(allowAutoEnable: Boolean = true)` 现在的优先级是：
  - 已启用无障碍：直接通过
  - 允许自动开启且持有 `WRITE_SECURE_SETTINGS`：尝试直接启用
  - 否则：跳转系统无障碍设置页
- `SystemUtils` 新增：
  - `canWriteSecureSettings()`
  - `enableAccessibilityService(serviceClass)`
- `AndroidManifest.xml` 新增权限声明：
  - `android.permission.WRITE_SECURE_SETTINGS`

Rebase 时确认：
- 上游如果改了 OCR 快捷磁贴入口、`FloatingWindowTriggerActivity` 或 `CoreService.start(...)` 的透传逻辑，确认 `allowAutoEnableAccessibility` 没有在中间层丢失。
- 上游如果改了 `OcrTools.requestPermission()`、无障碍检测方式或设置页结构，重点确认这次“按来源决定是否自动开启”的分支仍然保留。
- 上游如果引入了新的权限申请封装，不要把 `WRITE_SECURE_SETTINGS` 的直接启用逻辑误删成只会跳设置页。
- 如果后续决定放弃 `WRITE_SECURE_SETTINGS` 方案，至少要保留“磁贴可配置是否自动尝试开启无障碍”的用户行为约定。

特别注意：
- 这次改动横跨 `磁贴入口 -> Activity 透传 -> Service 权限检查 -> Secure Settings 写入 -> 设置页开关`，`rebase` 时很容易只合并到其中一半；需要按整条链路人工复查。

### 11. `cd34456` `feat(service): 恢复被上游移除的翻转触发`

目的：
- 恢复上游移除的“翻转手机触发当前页面识别”能力。
- 保留上游新增的“双击机身背部触发当前页面识别”能力。
- 让翻转触发和双击背部触发共存，二者各自使用独立开关。

涉及文件：
- `app/src/main/java/net/ankio/auto/service/CoreService.kt`
- `app/src/main/java/net/ankio/auto/service/FlipOcrTriggerService.kt`
- `app/src/main/java/net/ankio/auto/service/OcrService.kt`
- `app/src/main/java/net/ankio/auto/service/ocr/FlipDetector.kt`
- `app/src/main/java/net/ankio/auto/ui/fragment/settings/InteractionPreferenceFragment.kt`
- `app/src/main/java/net/ankio/auto/utils/PrefManager.kt`
- `app/src/main/res/xml/settings_interaction.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `server/src/main/java/org/ezbook/server/constant/Setting.kt`
- `server/src/main/java/org/ezbook/server/constant/DefaultData.kt`

关键点：
- 翻转触发恢复为独立子服务：`FlipOcrTriggerService`
- 翻转检测逻辑恢复为：`FlipDetector`
- `CoreService` 同时注册：
  - `FlipOcrTriggerService`
  - `BackTapOcrTriggerService`
- 设置项拆成两个独立开关：
  - `ocrFlipTrigger`
  - `ocrBackTapTrigger`
- 存储键拆成两个独立 key：
  - `Setting.OCR_FLIP_TRIGGER = "ocr_flip_trigger"`
  - `Setting.OCR_BACK_TAP_TRIGGER = "ocr_back_tap_trigger"`
- 默认值均为开启：
  - `DefaultData.OCR_FLIP_TRIGGER = true`
  - `DefaultData.OCR_BACK_TAP_TRIGGER = true`
- 两个触发方式最终都通过 `IntentType.OCR` 进入 `OcrService`，继续复用统一的锁屏、白名单、权限、并发和错误提示逻辑。

Rebase 时确认：
- 如果上游继续改动 `BackTapOcrTriggerService`、Columbus Tap 模型或传感器触发入口，不要把翻转触发再次覆盖掉。
- 如果上游继续沿用 `ocr_flip_trigger` 表示双击背部触发，需要手动拆回两个 key，避免翻转和双击背部共用同一个偏好位。
- 如果上游重构 `CoreService.initializeServices()`，确认两个子服务仍然都被注册，且 Xposed / LSPatch / OCR 模式下行为一致。
- 如果上游改动 `OcrService` 的 OCR Intent 入口，确认翻转和双击背部仍然都走统一 OCR 流程，不要绕过白名单、锁屏、无障碍权限和 `ocrDoing` 防重入判断。
- 如果上游改动设置页或 `PrefManager.SyncData`，确认两个开关的 XML key、`PrefManager` 字段、`Setting` 常量和 `DefaultData` 默认值仍一一对应。
- 如果上游新增了触发方式开关的模式限制，保留当前约定：翻转触发和双击背部触发都只依赖 `CoreService` 是否运行，不再只限制为非 Xposed 模式。

特别注意：
- 这里不要继续沿用上游“把旧 `ocr_flip_trigger` 语义改成双击背部”的做法；本地约定是旧 key 留给翻转，新 key 给双击背部。
- 这条提交和 `75da542` 的磁贴自动开启无障碍逻辑相互独立：磁贴是否自动开启无障碍只影响磁贴入口，不应该影响翻转/双击背部触发是否可用。
- `cd34456` 提交里还包含版本号、IDE、测试产物或 Gradle include 之类的非核心变更时，后续 rebase 应优先按语义保留上述 OCR 触发逻辑，不要因为这些旁路文件冲突而误删触发服务。

### 12. `2020478f` `feat(ocr/settings): 记住页面新增 不再询问。允许不再询问当前页面是否记住`

目的：
- 在 OCR 成功后的“记住此页面？”弹窗里加入“不再询问”行为。
- 用户选择“不再询问”后，将当前页面签名写入独立忽略列表；之后同一页面不再弹出记住页面确认框。
- 在页面管理页中同时管理“已记住”和“不再询问”两类页面。

涉及文件：
- `app/src/main/java/net/ankio/auto/service/OcrService.kt`
- `app/src/main/java/net/ankio/auto/service/ocr/PageSignatureManager.kt`
- `app/src/main/java/net/ankio/auto/ui/dialog/RememberPageDialog.kt`
- `app/src/main/java/net/ankio/auto/ui/fragment/settings/PageSignaturesFragment.kt`
- `app/src/main/java/net/ankio/auto/utils/PrefManager.kt`
- `app/src/main/res/layout/fragment_page_signatures.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `server/src/main/java/org/ezbook/server/constant/Setting.kt`

关键点：
- 新增页面忽略列表设置：
  - `Setting.IGNORED_PAGE_SIGNATURES = "setting_ignored_page_signatures"`
  - `PrefManager.ignoredPageSignatures`
- 忽略列表复用 `PageSignature` JSON 结构，与已记住页面使用同一套 key / 匹配语义。
- `PageSignatureManager` 新增：
  - `getAllIgnored()`
  - `ignore(sig)`
  - `removeIgnored(key)`
  - `isIgnored(packageName, activityName, structureFingerprint)`
- 已记住列表和忽略列表互斥：
  - 记住页面时移除同 key 的忽略记录。
  - 忽略页面时移除同 key 的已记住记录。
- `OcrService.showRememberPageDialog(...)` 弹窗前同时检查已记住和已忽略页面。
- `RememberPageDialog` 的负面按钮改为“不再询问”；按返回键或点弹窗外部仍只是关闭弹窗，不写入任何列表。
- `PageSignaturesFragment` 在同一页面用顶部 `TabLayout` 切换：
  - “已记住”读取 `PageSignatureManager.getAll()`
  - “不再询问”读取 `PageSignatureManager.getAllIgnored()`
  - 长按删除时分别调用 `remove(...)` 或 `removeIgnored(...)`

Rebase 时确认：
- 上游如果改了“记住页面”弹窗，负面按钮仍写入 ignored list，而不是退回普通取消。
- 上游如果改了弹窗关闭行为，点击返回键和点击外部不会写入 remembered / ignored。
- 上游如果改了 `PageSignature` 或 `PageSignatureManager.matches(...)`，确认 ignored list 使用同一套 key 和匹配规则。
- 上游如果修复或重构结构指纹采集链路，确认 remembered 和 ignored 两套列表同步使用新的结构指纹。
- 上游如果改了 `OcrService` 的 OCR 成功回调或 `showRememberPageDialog(...)` 调用条件，确认已忽略页面不会继续弹出确认框。
- 上游如果改了页面管理页，确认同页双列表仍保留，且删除 ignored 记录后该页面会重新允许弹出询问。

特别注意：
- ignored list 的语义是“以后不再询问是否记住”，不是“禁止自动 OCR”。已记住页面的自动触发行为仍由 remembered list 决定。
- 本条只记录“记住页面 / 不再询问 / 忽略列表”相关变更；提交中的版本号递增等旁路变更不在这里记录。

### 13. `working tree` `feat(category): 分类规则记住标签`

目的：
- 让分类规则不仅记住账本、分类和备注，也能记住账单标签。
- 用户在账单编辑弹窗中修改分类或标签并勾选“记住分类”后，自动生成/覆盖的分类规则会带上当前标签。
- 手动编辑分类规则时，可以在规则结果里选择标签，后续命中该规则时同步回填到账单。

涉及文件：
- `app/src/main/java/net/ankio/auto/ui/dialog/components/ActionButtonsComponent.kt`
- `app/src/main/java/net/ankio/auto/ui/fragment/components/CategoryRuleEditComponent.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `server/src/main/java/org/ezbook/server/tools/BillService.kt`

关键点：
- 没有新增数据库表、字段、版本号或 migration。
- 标签信息写入现有 `CategoryRuleModel.element` JSON 的结果项：
  - `tags` 使用逗号分隔的标签名字符串。
- 标签信息也写入现有 `CategoryRuleModel.js` 返回对象：
  - `return { book, category, remark, tags }`
- 服务端分类规则命中后，仅当返回结果里存在 `tags` 字段时，才更新 `BillInfoModel.tags`。
- 旧分类规则没有 `tags` 字段时仍按原逻辑工作，不会强制清空账单标签。
- 自动记住分类的触发条件从“分类变化”扩展为“分类或标签变化”。

Rebase 时确认：
- 上游如果改了 `CategoryRuleEditComponent.getRule()` 或分类规则 `element` JSON 结构，确认结果项仍包含 `tags`，且旧规则缺失 `tags` 时能正常回显为无标签。
- 上游如果改了 `ActionButtonsComponent.rememberCategoryAuto()`，确认自动生成的 system 分类规则仍把当前 `billInfoModel.tags` 写入 `element` 和 `js`。
- 上游如果改了 `BillService.categorize(...)` 或分类规则执行结果解析，确认只有规则返回 `tags` 时才覆盖 `bill.tags`，不要让旧规则误清空标签。
- 上游如果调整了标签存储格式或 `BillInfoModel.getTagList()/setTagList()`，需要同步评估分类规则里逗号分隔字符串是否仍兼容。
- 上游如果给分类规则新增正式字段或数据库迁移，要重新判断是否继续把标签放在 `element/js` 中，避免和上游结构重复表达。

特别注意：
- 这条是“写入已有字段的数据格式扩展”，不是数据库结构变更；后续 rebase 时不要误加 migration。
- 已存在的旧分类规则不会自动批量补齐标签；只有重新编辑保存或自动记住分类覆盖后，规则记录才会包含 `tags`。

## 后续追加模板

以后每次你做完一个“明显偏离上游”的二改，可以在下面追加一段：

```md
### N. `<short-hash>` `<commit title>`

目的：
- 这里写这次二改想解决什么问题。

涉及文件：
- `path/to/file1`
- `path/to/file2`

关键点：
- 这里写改动入口、配置 key、默认值、开关名、脚本名等。

Rebase 时确认：
- 上游如果改了什么，这次二改最可能在哪些点冲突。
- 如果自动合并成功，还需要人工复查什么行为。
```

## 建议维护方式

- 不要试图记录所有小修小补，只记录“和上游容易撞车”的改动。
- 一条记录最好对应一个 commit；如果一个功能拆成多个 commit，可以在同一条里合并写最终状态。
- `rebase` 完成后，如果某项二改已经被上游正式吸收，可以在对应条目后标记：`已被上游吸收，可删除本地补丁`。
