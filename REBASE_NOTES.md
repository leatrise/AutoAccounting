# 二改记录 / Rebase 检查笔记

这个文件用于记录当前仓库相对上游仓库 `upstream` 的二次修改，方便后续 `rebase` 时快速检查冲突点、判断哪些改动需要手动重放。

## 当前基线

- 当前分支：`master`
- 我的仓库：`origin = https://github.com/leatrise/AutoAccounting.git`
- 上游仓库：`upstream = https://github.com/AutoAccountingOrg/AutoAccounting.git`
- 记录范围：`upstream/master...HEAD`
- 当前二改提交数：`7`

建议在每次执行 `git fetch upstream` 后，先看这几个命令：

```bash
git log --oneline --reverse upstream/master..HEAD
git diff --name-only upstream/master...HEAD
git diff --stat upstream/master...HEAD
```

## Rebase 前重点检查

以下文件是当前二改最容易与上游冲突的位置：

- `app/build.gradle.kts`
- `app/src/main/java/com/google/android/accessibility/selecttospeak/SelectToSpeakService.kt`
- `app/src/main/java/net/ankio/auto/service/OcrService.kt`
- `app/src/main/java/net/ankio/auto/service/ocr/OcrTools.kt`
- `app/src/main/java/net/ankio/auto/utils/PrefManager.kt`
- `app/src/main/res/xml/settings_interaction.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `server/src/main/java/org/ezbook/server/constant/Setting.kt`
- `server/src/main/java/org/ezbook/server/constant/DefaultData.kt`
- `.github/workflows/android.yml`
- `.github/workflows/main.py`

如果上游也改了这些文件，`rebase` 时不要只看是否能自动合并，还要确认：

- OCR 流程是否仍保留“优先使用无障碍页面文本”的逻辑。
- 无障碍页面文本的拼接分隔符是否仍是你需要的格式。
- 设置页里 `ocrAccessibilityPageData` 开关是否还存在，key 和默认值是否一致。
- CI 工作流是否仍使用你当前的签名、构建和 Telegram 通知流程。
- `test/bin/main/*` 这批测试产物是否仍需要保留，避免把上游测试结构更新错覆盖。

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
- 上游如果更新了 workflow 脚本，不要直接覆盖你的自定义发包流程。

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

### 8. `working tree` `feat(rule): 规则描述可编辑，并移除默认描述文案`

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
