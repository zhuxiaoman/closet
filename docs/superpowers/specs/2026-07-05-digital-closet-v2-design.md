# 电子衣橱 v2 设计文档

- **日期：** 2026-07-05
- **状态：** 设计阶段（待用户审阅）
- **范围：** 在已上线的 MVP 基础上做 v2 升级，沿用 uni-app + Spring Boot 技术栈。
- **上游文档：** [`2026-07-01-digital-closet-design.md`](2026-07-01-digital-closet-design.md)

## 1. 概述

### 1.1 背景

MVP（T1-T36）已上线，包含 10 个页面、9 个后端控制器、5+5 默认分类、单用户自托管。用户反馈：页面视觉单调、功能太简单。

v2 在不动 MVP 数据模型核心、不引入多用户体系的前提下，做两件事：

1. **视觉系统升级**：从「朴素白底 + uView 默认组件」改为治愈系樱粉燕麦（B 樱粉燕麦）整套设计系统。
2. **功能扩展**：从 6 个基础功能扩展为 16 个，按 B 方案分两期交付。

### 1.2 目标

- 视觉上达到主流电子衣橱 App（Acloset / Whering / Stylebook）的精致度
- 在不引入云端 AI 服务的前提下，用本地规则引擎实现「AI 搭配推荐」
- 接入和风天气 SDK，做真正的「天气 + 穿搭」联动
- 衣橱 5 种视图切换，满足不同浏览习惯
- 高级分享海报，提升搭配分享的传播力

### 1.3 非目标

- **多用户 / 社区**：v2 不做，单独评估
- **真 AI 模型**：v2 用本地规则引擎，v2.1+ 评估接入 LLM
- **电商 / 二手交易平台对接**：v2 只做清单和文案生成，不接 API
- **iOS / Android 原生 App**：仅做 H5 + 微信小程序两端

## 2. 视觉系统

### 2.1 调色板（B 樱粉燕麦）

| 角色 | 色值 | 用途 |
|---|---|---|
| 背景主色 | `#ffffff` | 页面底色、卡片底色 |
| 背景辅色 | `#faf6f3` | 大区块底色、tabbar 邻接区 |
| 卡片底色 | `#f9f0ee` | 次级卡片、统计块 |
| 强调主色 | `#d49aa5` | CTA 按钮、激活态、徽标 |
| 强调辅色 | `#b8808a` | 图标 stroke、hover 态 |
| 文字主色 | `#4a3a3a` | 标题、正文 |
| 文字辅色 | `#8a6d70` | 副标题、说明 |
| 文字弱化 | `#b8808a` @ 60% | placeholder、disabled |
| 边框色 | `#f0d9dd` | 分割线、输入框边框 |
| 状态 - 成功 | `#7a9070` | 已完成、保存成功 |
| 状态 - 警告 | `#d49aa5` | 提醒、未保存 |
| 状态 - 错误 | `#c75d6d` | 错误、删除 |

色卡见：[`.superpowers/brainstorm/2060-1783237748/content/04-warm-palette-options.html`](../../../.superpowers/brainstorm/2060-1783237748/content/04-warm-palette-options.html)

### 2.2 字体

- **主字体**：`"PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif`
- **数字字体**：同上，开启 `font-feature-settings: "tnum"` 等宽数字
- **海报字体**：英文 `Georgia, "Times New Roman", serif`（杂志风模板专用）

| 层级 | 字号 | 字重 | 行高 |
|---|---|---|---|
| H1 | 18px | 600 | 1.3 |
| H2 | 16px | 600 | 1.4 |
| H3 | 14px | 600 | 1.4 |
| 正文 | 12px | 400 | 1.6 |
| 辅助 | 11px | 400 | 1.5 |
| caption | 9px | 400 | 1.4 |

### 2.3 圆角与阴影

- 卡片：`border-radius: 12px`
- 按钮：`border-radius: 18px`（胶囊）/ `8px`（方形）
- 输入框：`border-radius: 10px`
- 头像：`border-radius: 50%`
- 阴影：`0 6px 20px rgba(184,128,138,0.18), 0 2px 4px rgba(184,128,138,0.08)`（卡片）
- 阴影弱：`0 1px 3px rgba(184,128,138,0.08)`（次级卡片）

### 2.4 图标

- 统一使用 **Lucide** 风格线性图标，stroke 1.5px
- 默认色：`#b8808a`（强调辅色）
- 激活态：`#d49aa5`（强调主色）
- 来源：[lucide.dev](https://lucide.dev/) ，按需引入
- 不使用 emoji 替代图标

### 2.5 组件升级

替换 MVP 中所有 uView Plus 组件的自定义样式：

| 原组件 | 升级点 |
|---|---|
| `<up-button>` | 自定义 class：主色 `#d49aa5`、圆角 18px、字号 14px |
| `<up-card>` | 边框 `#f0d9dd`、阴影 弱、内边距 16rpx |
| `<up-input>` | 边框 `#f0d9dd`、聚焦时 `#d49aa5` |
| `<up-tabbar>` | 激活色 `#d49aa5`，图标 22px |
| `<up-empty>` | 文案 + 插画 |
| 新增 `<up-collapse>` | 抽屉视图用 |
| 新增 `<up-segmented>` | 视图切换器 |

## 3. 信息架构

### 3.1 底部导航（5 项 tabbar）

| Tab | 图标 | v2.0 升级 |
|---|---|---|
| 首页 | home | 加问候语 + 天气卡 + 4 个快捷入口 |
| 衣橱 | grid | 顶部加视图切换器（5 种） |
| 搭配 | layers | 加 AI 生成器入口 |
| 日历 | calendar | 顶部加 7 天天气条 |
| 我的 | user | 加二级入口列表（v2.1 项灰色占位） |

### 3.2 页面层级

```
首页
├── 问候语（时间感知）
├── 今日天气卡
├── 统计概览（3 项）
├── 近期穿着（最近 7 天）
├── 快捷入口（4 个）
└── AI 推荐今日穿搭（v2.0 新）

衣橱（5 种视图切换）
├── 网格视图（MVP 升级）
├── 衣架视图（v2.0 新）
├── 抽屉视图（v2.0 新）
├── 色块视图（v2.0 新）
├── 季节轮换视图（v2.0 新）
└── 衣物详情 / 表单（MVP 沿用）

搭配
├── 搭配列表（MVP 升级：加 AI 推荐 tab）
├── AI 搭配生成器（v2.0 新）
├── 搭配详情（MVP 升级：加分享海报）
├── 搭配表单（MVP 沿用）
└── 海报编辑器（v2.0 新）

日历
├── 月历视图（MVP 升级：加天气条）
├── 当日详情（MVP 升级：加 AI 推荐）
└── 7 天天气预报卡（v2.0 新）

我的
├── 个人信息
├── 分类管理 / 标签管理
├── 心愿单（v2.1 占位）
├── 闲置出售（v2.1 占位）
├── 维护提醒（v2.1 占位）
├── 数据洞察（v2.1 占位）
├── 密码锁（v2.1 占位）
└── 数据导出
```

页面树可视化：[`.superpowers/brainstorm/2060-1783237748/content/07-info-architecture.html`](../../../.superpowers/brainstorm/2060-1783237748/content/07-info-architecture.html)

## 4. 功能范围

### 4.1 v2.0（核心 4 项，5-7 周）

#### F1 · AI 智能搭配

- **入口**：搭配 tab 「+」菜单 / 衣物详情页「找搭配」/ 首页快捷入口
- **流程**：选起点（1+ 件衣物）→ 选场合 + 季节 → 生成 → **固定 5 套结果**
- **结果动作**：保存整套 / 重生成 / 换单品（局部替换）/ 不喜欢（反馈）
- **技术**：本地规则引擎（颜色协调表 + 风格匹配 + 季节过滤 + 天气适配）

#### F2 · 天气场景联动

- **天气源**：和风天气 SDK（免费层每日 1000 次）
- **数据**：7 天预报，每日凌晨缓存到 `weather_cache` 表
- **入口**：日历页顶部天气条 / 当日详情 / 首页天气卡
- **联动**：天气异常（雨/高温/紫外线）→ 自动插入穿衣提醒
- **AI 推荐**：当日穿搭推荐 = 搭配引擎结果 + 天气过滤

#### F3 · 衣橱 5 种视图

- **视图切换器**：衣橱页顶部一级 tab，5 个 tab
- **5 种视图**：网格 / 衣架 / 抽屉 / 色块 / 季节
- **共用数据**：同一份衣物 API，前端按视图类型重新排列
- **性能**：衣架 / 抽屉用虚拟滚动（>100 件时）

#### F4 · 高级分享海报

- **入口**：搭配详情页 → 分享按钮
- **3 个模板**：
  - 杂志大片（黑底 + 衬线大字）
  - 色块拼图（白底 + 2x2 物品）
  - 心情日记（暖棕 + 手写感）
- **微调**：字体 / 底色 / 贴纸（v2.0 简化版，仅字体 + 底色）
- **导出**：保存到相册 / 分享微信 / 小红书

### 4.2 v2.1（增值 6 项，3-4 周）

| 编号 | 功能 | 复杂度 |
|---|---|---|
| F5 | AI 智能识别（拍照自动填字段） | 中高 |
| F6 | 深度数据洞察（热力图 + CPW + 寿命） | 中 |
| F7 | 旅行打包清单（天数 + 目的地 → 自动凑齐） | 中 |
| F8 | 衣物维护提醒（洗涤 + 收纳 + 寿命） | 低 |
| F9 | 心愿单 / 购物清单（想买 + 缺什么 + 价格追踪） | 中 |
| F10 | 闲置出售 / 转赠（文案生成 + 转赠清单） | 中 |

### 4.3 未来评估（不承诺）

- 多用户 / 社区：单用户定位明确，需重新评估产品方向
- 真 AI 模型：v2.1 后评估是否接入 LLM（OpenAI / 通义千问）
- 二手平台 API 对接：闲鱼 / 微信小商店

## 5. 数据模型（新增）

### 5.1 `outfit_ai_generation`

记录 AI 搭配生成历史，用于反馈学习。

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | |
| seed_clothing_ids | jsonb | 起点衣物 ID 列表（1+ 件） |
| occasion | varchar(32) | 通勤/约会/... |
| season | varchar(16) | 季节 |
| weather_snapshot | jsonb | 生成时的天气快照 |
| result_outfit_ids | jsonb | 生成的搭配 ID 列表 |
| feedback | varchar(16) | like / dislike / none |
| created_at | timestamptz | |

### 5.2 `weather_cache`

每日缓存指定城市的天气。

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | |
| city_code | varchar(32) | 城市 code |
| city_name | varchar(64) | 城市名 |
| forecast_date | date | 预报日期 |
| temp_high | numeric(4,1) | 最高温 |
| temp_low | numeric(4,1) | 最低温 |
| weather_code | varchar(16) | 和风天气 code |
| weather_desc | varchar(64) | 晴/多云/雨 |
| uv_index | int | 紫外线指数 |
| humidity | int | 湿度 |
| fetched_at | timestamptz | 拉取时间 |

唯一索引：`(city_code, forecast_date)`

### 5.3 `packing_list`

旅行打包清单。

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | |
| title | varchar(128) | 行程标题 |
| destination | varchar(64) | 目的地 |
| start_date | date | |
| end_date | date | |
| occasion | varchar(32) | |
| outfit_ids | jsonb | 推荐的搭配 |
| item_ids | jsonb | 推荐的衣物 |
| status | varchar(16) | draft / packing / done |
| created_at | timestamptz | |

### 5.4 `clothing_maintenance`

衣物维护记录。

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | |
| clothing_id | bigint FK | |
| type | varchar(16) | wash / dry_clean / store / repair |
| performed_at | date | |
| next_due_at | date | 下次维护日期 |
| notes | text | |
| created_at | timestamptz | |

### 5.5 `wishlist_item`

心愿单。

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | |
| name | varchar(128) | |
| category_id | bigint FK→category.id | 想买的分类 |
| expected_price | numeric(10,2) | |
| priority | varchar(16) | low / mid / high |
| status | varchar(16) | want / bought / removed |
| notes | text | |
| created_at | timestamptz | |

### 5.6 `secondhand_item`

闲置出售 / 转赠。

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | |
| clothing_id | bigint FK→clothing.id | 关联衣物 |
| type | varchar(16) | sell / donate |
| asking_price | numeric(10,2) | |
| listing_text | text | 自动生成的文案 |
| status | varchar(16) | listed / sold / donated / withdrawn |
| created_at | timestamptz | |

### 5.7 `user_preference`

用户偏好（穿衣风格、色系、季节偏好），用于 AI 推荐个性化。

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | |
| pref_key | varchar(64) unique | favorite_color / style / ... |
| pref_value | jsonb | |
| updated_at | timestamptz | |

### 5.8 `app_lock`

App 启动密码锁。

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigserial PK | |
| pin_hash | varchar(255) | bcrypt 哈希 |
| auto_lock_minutes | int | 自动锁定时长，0 = 不锁 |
| enabled | boolean | |
| updated_at | timestamptz | |

## 6. API 新增

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/outfits/ai-generate` | AI 搭配生成（v2.0） |
| POST | `/api/v1/outfits/ai-generation/{id}/feedback` | 反馈（v2.0） |
| GET | `/api/v1/weather/forecast` | 7 天天气预报（v2.0） |
| GET | `/api/v1/weather/today` | 今日天气简报（v2.0） |
| POST | `/api/v1/clothing/ai-identify` | 上传图片 AI 识别字段（v2.1 F5） |
| GET | `/api/v1/stats/heatmap` | 365 天穿着热力图（v2.1 F6） |
| GET | `/api/v1/stats/cost-per-wear` | CPW 排行（v2.1 F6） |
| POST | `/api/v1/packing-lists` | 创建打包清单（v2.1 F7） |
| POST | `/api/v1/packing-lists/auto-generate` | 自动生成（v2.1 F7） |
| POST | `/api/v1/clothing/{id}/maintenance` | 记录维护（v2.1 F8） |
| GET | `/api/v1/maintenance/upcoming` | 即将到期维护（v2.1 F8） |
| GET/POST/PUT/DELETE | `/api/v1/wishlist` | 心愿单 CRUD（v2.1 F9） |
| GET/POST/PUT/DELETE | `/api/v1/secondhand` | 闲置 CRUD（v2.1 F10） |
| POST | `/api/v1/secondhand/{id}/generate-listing` | 生成文案（v2.1 F10） |
| GET/POST/PUT | `/api/v1/app-lock` | 密码锁配置（v2.1） |

## 7. 组件新增（前端）

### 7.1 新增组件

| 组件 | 路径 | 用途 |
|---|---|---|
| `WeatherBar.vue` | `components/weather/` | 7 天天气条 |
| `WeatherCard.vue` | `components/weather/` | 当日天气卡 |
| `AiGenerator.vue` | `components/ai/` | AI 搭配生成器（含步骤引导） |
| `OutfitResultCard.vue` | `components/ai/` | 单套结果展示 + 反馈按钮 |
| `ClosetViewTabs.vue` | `components/closet/` | 视图切换器 |
| `HangerView.vue` | `components/closet/` | 衣架视图 |
| `DrawerView.vue` | `components/closet/` | 抽屉视图（用 `<up-collapse>`） |
| `ColorClusterView.vue` | `components/closet/` | 色块聚类视图 |
| `SeasonQuadrant.vue` | `components/closet/` | 季节四象限 |
| `PosterTemplatePicker.vue` | `components/share/` | 海报模板选择器 |
| `PosterEditor.vue` | `components/share/` | 海报实时编辑器 |
| `PosterRenderer.vue` | `components/share/` | 海报渲染（canvas 2d） |

### 7.2 新增页面

| 页面 | 路径 | 用途 |
|---|---|---|
| `pages/ai-generator/index.vue` | AI 搭配生成器主入口 |
| `pages/weather/index.vue` | 7 天天气详情 |
| `pages/wishlist/index.vue` | 心愿单（v2.1） |
| `pages/secondhand/index.vue` | 闲置出售（v2.1） |
| `pages/maintenance/index.vue` | 维护提醒（v2.1） |
| `pages/stats/heatmap.vue` | 热力图（v2.1） |
| `pages/profile/index.vue` | 我的（整合二级入口） |

### 7.3 新增 stores（Pinia）

| Store | 状态 |
|---|---|
| `weather.ts` | 当前城市 + 7 天预报缓存 |
| `ai.ts` | AI 生成历史 + 反馈 |
| `preference.ts` | 用户偏好 |

## 8. 关键交互流程

### 8.1 AI 搭配生成

```
[入口]
  ├── 搭配 tab 「+」菜单 → AI 生成
  ├── 衣物详情页 → 找搭配
  └── 首页快捷入口

[流程]
  1. 选起点：选 1+ 件衣物（必选，存为 seed_clothing_ids JSON 数组）/ 场合 / 季节
  2. 点击「生成」 → POST /outfits/ai-generate
  3. 加载动画（本地规则引擎 < 1s）
  4. 展示 5 套结果（固定数量）
  5. 用户操作：
     - 单击 → 进入搭配详情画布
     - 「保存」→ POST /outfits 创建 + 写入 ai_generation.feedback = none
     - 「重生成」→ 重新调用 POST /outfits/ai-generate
     - 「换单品」→ 在结果上局部替换某件衣物
     - 「不喜欢」→ POST /ai-generation/{id}/feedback = dislike
  6. 返回结果页

[本地规则引擎 v2.0]
  输入：起点衣物列表 + 场合 + 季节 + 天气
  步骤：
    1. 季节过滤：排除非当前季节的衣物
    2. 天气过滤：高温排除厚外套，雨天加防风单品
    3. 颜色协调：基于色轮互补 + 同色系聚类
    4. 风格匹配：基于 category + tag 匹配
    5. 随机组合：从候选池中采样 5 套，去重
  输出：5 套搭配（每套 3-6 件）
```

### 8.2 天气联动

```
[定时任务] 每日 03:00
  └── 查询 user_preference.city_code
  └── 调用和风 API → 写入 weather_cache

[日历页加载]
  └── GET /weather/forecast?city=...
  └── 展示顶部 7 天天气条

[当日详情]
  └── GET /weather/today
  └── GET /outfits/ai-generate?weather_id=...
  └── 展示天气卡 + AI 推荐
  └── 「采纳」→ POST /calendar + 自动写 wear_log
  └── 「不喜欢」→ 反馈
```

### 8.3 海报编辑

```
[入口] 搭配详情 → 「分享」

[模板选择]
  4 个模板卡（3 预设 + 1 自定义占位）
  单选 → 进入预览

[预览]
  PosterRenderer 用 canvas 2d 渲染
  模板配置 JSON：背景 + 文字层 + 物品图层 + 贴纸层

[微调]
  字体：3 选 1（衬线 / 无衬线 / 手写感）
  底色：取自 2.1 调色板的 5 色（强调主色 d49aa5 / 背景主色 ffffff / 背景辅色 faf6f3 / 强调辅色 b8808a / 文字主色 4a3a3a）
  贴纸：v2.0 仅做占位

[导出]
  「保存到相册」 → canvas.toDataURL → uni.saveImageToPhotosAlbum
  「分享」 → uni.share（微信 / 小红书）
```

## 9. 测试

### 9.1 后端

- **单元测试**：service 层 ≥ 80% 行覆盖
  - `OutfitAiService`：颜色协调、风格匹配、季节过滤、5 套去重
  - `WeatherService`：缓存命中、过期刷新、API 失败降级
  - `PosterService`：文案生成、模板参数校验
- **集成测试**：
  - AI 搭配全链路：选起点 → 生成 → 保存 → 反馈
  - 天气缓存：写入 → 命中 → 过期刷新
  - 海报 CRUD + 生成

### 9.2 前端

- **组件测试**：所有新增组件
- **页面测试**：所有新增页面 + 升级页面
- **视觉回归**：用 Playwright 截图对比前后样式（4 个 tabbar 状态 + 关键页面）
- **E2E**：暂不做

### 9.3 AI 引擎验证

- **离线评测集**：构造 20 组起点 + 场合，验证生成的 5 套：
  - 季节正确率 ≥ 95%
  - 颜色协调（无明显撞色）≥ 90%
  - 不重复（5 套互不相同）= 100%
- **用户反馈循环**：v2.0 收集 dislike 反馈，v2.1 调优权重

## 10. 交付物

### 10.1 设计交付（本次）

| 类型 | 内容 | 位置 |
|---|---|---|
| 原型图（低保真） | 4 种视觉风格对比 | [01-visual-style.html](../../../.superpowers/brainstorm/2060-1783237748/content/01-visual-style.html) |
| UI 图（中保真） | B 风格首页 + 衣橱页 + 搭配页 | [02-visual-style-mockups.html](../../../.superpowers/brainstorm/2060-1783237748/content/02-visual-style-mockups.html) |
| UI 图（带图标） | B + 图标版三屏 | [03-visual-style-warm-icons.html](../../../.superpowers/brainstorm/2060-1783237748/content/03-visual-style-warm-icons.html) |
| 配色方案 | 4 种白色基底配色对比 | [04-warm-palette-options.html](../../../.superpowers/brainstorm/2060-1783237748/content/04-warm-palette-options.html) |
| 信息架构图 | sitemap + 导航结构 | [07-info-architecture.html](../../../.superpowers/brainstorm/2060-1783237748/content/07-info-architecture.html) |
| 关键页 UI 原型 | 4 个新功能核心页面 | [08-v2-key-features.html](../../../.superpowers/brainstorm/2060-1783237748/content/08-v2-key-features.html) |
| 视觉伴侣 URL | http://localhost:51392 | 实时访问 |

### 10.2 实施交付（下个阶段）

- 实施计划：`docs/superpowers/plans/2026-07-05-digital-closet-v2.md`
- 按 writing-plans skill 拆分 task，每个 task 一个 PR

## 11. 实施计划（高层）

### 11.1 v2.0 时间线（5-7 周）

| 周 | 后端 | 前端 |
|---|---|---|
| W1 | schema 迁移（v2.0 2 张表：outfit_ai_generation、weather_cache）+ 应用启动配置 | 设计系统改造（颜色变量 + 组件样式覆盖） |
| W2 | `OutfitAiService` + 规则引擎 + 单测 | 视觉系统全套组件升级 + tabbar 重做 |
| W3 | `/outfits/ai-generate` + 集成测试 | AI 搭配页面 + 三步引导 + 结果展示 |
| W4 | `WeatherService` + 和风 SDK + 缓存表 | 天气组件 + 日历页升级 + 首页天气卡 |
| W5 | 海报模板 JSON + 生成接口 | 海报编辑器 + 模板选择器 + 导出 |
| W6 | 衣橱视图 API（如需） | 衣橱 5 种视图实现 |
| W7 | 联调 + 集成测试 + 性能 | 联调 + 视觉回归 + 文档更新 |

### 11.2 v2.1 时间线（3-4 周）

| 周 | 后端 | 前端 |
|---|---|---|
| W8 | AI 识别接口 + 深度数据 API | 心愿单 + 闲置出售页面 |
| W9 | 维护提醒 + 打包清单 | 维护提醒 + 打包清单页面 |
| W10 | 热力图 + CPW + 数据洞察 | 数据洞察页（含图表） |
| W11 | 密码锁 + 收尾 | 密码锁 + 全量回归 |

### 11.3 风险与依赖

| 风险 | 影响 | 应对 |
|---|---|---|
| 和风 SDK 限速 | 天气数据拉取失败 | 本地缓存 24h，失败时返回昨日缓存 |
| 本地规则引擎效果差 | AI 搭配质量低 | 准备评测集，v2.0 收集 dislike 数据，v2.1 优化 |
| 海报渲染性能 | canvas 在小程序端慢 | 用离屏 canvas，预渲染到内存 |
| 用户量小（单用户自托管） | 反馈数据少 | AI 反馈权重基于显式 dislike 而非 implicit |

## 12. 开放问题

1. **城市定位**：默认城市用 GPS 自动获取还是手动设置？v2.0 用 IP 定位。
2. **AI 反馈学习**：v2.0 收集的 dislike 数据是用于本地权重调整，还是上传到云端训练？v2 暂只本地。
3. **海报贴纸**：v2.0 简化版只做字体 + 底色，完整贴纸编辑器放 v2.2？
4. **数据洞察图表库**：前端用 `<up-chart>`（uView 自带）还是引入 `echarts-for-uniapp`？先 uView 自带，不够再换。
5. **密码锁哈希**：PIN 码用 bcrypt 还是更轻量的 scrypt？单用户场景用 bcrypt 4 轮即可。

## 13. 验收清单

### 13.1 视觉系统验收

- [ ] 所有页面使用统一调色板，无 MVP 默认蓝紫色
- [ ] 字体统一为 PingFang SC
- [ ] 所有图标用 Lucide 线性图标，无 emoji 替代
- [ ] 圆角统一 12px / 18px / 8px
- [ ] 阴影统一为樱粉色调

### 13.2 v2.0 功能验收

- [ ] AI 搭配：固定生成 5 套，支持重生成 / 换单品 / 反馈
- [ ] 天气：日历页顶部 7 天天气条 + 当日详情天气卡
- [ ] 衣橱：5 种视图切换无闪烁
- [ ] 海报：3 模板 + 字体底色微调 + 导出图片

### 13.3 测试验收

- [ ] 后端单测覆盖率 ≥ 80%
- [ ] 前端核心组件 + 页面有 Vitest 测试
- [ ] AI 引擎评测集 20 组通过率 ≥ 90%
- [ ] 视觉回归：4 个 tabbar 状态截图对比无差异

### 13.4 文档验收

- [ ] 本 spec 文档用户审阅通过
- [ ] 实施计划（plan）用户审阅通过
- [ ] handoff.md 更新
- [ ] README 更新（v2.0 功能列表）
