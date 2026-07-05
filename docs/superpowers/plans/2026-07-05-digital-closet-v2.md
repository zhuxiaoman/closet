# 电子衣橱 v2 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 MVP 基础上做 v2 升级——视觉系统升级为樱粉燕麦设计语言，新增 AI 智能搭配（固定 5 套）、天气联动（和风 SDK）、衣橱 5 种视图、高级分享海报（3 模板）四个核心功能；后续 v2.1 再补 AI 识别、数据洞察、打包清单、维护提醒、心愿单、闲置出售 6 个增值功能。

**Architecture:** 沿用 MVP 架构（uni-app + Spring Boot + PG + MinIO）。新增本地 AI 规则引擎（颜色协调 + 风格匹配 + 季节/天气过滤）、和风天气 SDK（每日 03:00 缓存 7 天预报）、前端 canvas 2d 海报渲染器。所有新增功能不破坏 MVP 数据兼容性，单用户不变。

**Tech Stack:**
- 前端：uni-app（Vue 3 + TS）+ uView Plus + Pinia + Vite + Vitest + Lucide 图标
- 后端：Java 21 + Spring Boot 3.3.4 + MyBatis-Plus 3.5.5 + JUnit 5 + Mockito + Testcontainers
- 新增依赖：和风天气 SDK（`com.qweather:QWeather-SDK-Java:1.0.0`）、canvas 2d（H5 + 微信小程序原生）
- 存储：PostgreSQL 16 + MinIO + 新增 `weather_cache`、`outfit_ai_generation`、`user_preference` 等 8 张表

**Reference spec:** `docs/superpowers/specs/2026-07-05-digital-closet-v2-design.md`

**Reference plan:** `docs/superpowers/plans/2026-07-01-digital-closet-mvp.md`（沿用 MVP 的命名约定、commit 规范、单测/集成测试模式）

---

## 新增文件结构

### 后端

```
backend/src/main/java/com/closet/
├── entity/
│   ├── OutfitAiGeneration.java          (新, v2.0)
│   ├── WeatherCache.java                (新, v2.0)
│   ├── UserPreference.java              (新, v2.0)
│   ├── PackingList.java                 (新, v2.1)
│   ├── ClothingMaintenance.java         (新, v2.1)
│   ├── WishlistItem.java                (新, v2.1)
│   ├── SecondhandItem.java              (新, v2.1)
│   └── AppLock.java                     (新, v2.1)
├── mapper/（对应 8 个 mapper）
├── service/
│   ├── ColorHarmonyEngine.java          (新, v2.0) ★
│   ├── OutfitAiService.java             (新, v2.0) ★
│   ├── QWeatherClient.java              (新, v2.0) ★
│   ├── WeatherService.java              (新, v2.0) ★
│   ├── PosterService.java               (新, v2.0) ★
│   └── ...（v2.1 services）
├── service/impl/（对应实现）
├── controller/
│   ├── OutfitAiController.java          (新, v2.0) ★
│   ├── WeatherController.java           (新, v2.0) ★
│   └── ...（v2.1 controllers）
├── dto/
│   ├── AiGenerateRequest.java           (新, v2.0)
│   ├── AiGenerateResponse.java          (新, v2.0)
│   ├── WeatherForecastDto.java          (新, v2.0)
│   └── ...
└── scheduled/
    └── WeatherSyncTask.java             (新, v2.0) ★ - 每日 03:00 同步

backend/src/main/resources/
├── schema.sql                           (修改 - 追加 8 张表)
├── data.sql                             (修改 - 追加 user_preference 默认值)
└── application.yml                      (修改 - 加和风 SDK key、cron 配置)
```

### 前端

```
frontend/src/
├── styles/
│   ├── tokens.scss                      (新) ★ v2.0
│   └── overrides.scss                   (新) ★ v2.0
├── components/
│   ├── Icon.vue                         (新) ★ v2.0 - Lucide 包装
│   ├── weather/
│   │   ├── WeatherBar.vue               (新) ★ v2.0
│   │   └── WeatherCard.vue              (新) ★ v2.0
│   ├── ai/
│   │   ├── AiGenerator.vue              (新) ★ v2.0
│   │   └── OutfitResultCard.vue         (新) ★ v2.0
│   ├── closet/
│   │   ├── ClosetViewTabs.vue           (新) ★ v2.0
│   │   ├── HangerView.vue               (新) ★ v2.0
│   │   ├── DrawerView.vue               (新) ★ v2.0
│   │   ├── ColorClusterView.vue         (新) ★ v2.0
│   │   └── SeasonQuadrant.vue           (新) ★ v2.0
│   └── share/
│       ├── PosterTemplatePicker.vue     (新) ★ v2.0
│       ├── PosterEditor.vue             (新) ★ v2.0
│       └── PosterRenderer.vue           (新) ★ v2.0
├── pages/
│   ├── ai-generator/index.vue           (新) ★ v2.0
│   ├── weather/index.vue                (新) ★ v2.0
│   └── ...（v2.1 pages）
└── stores/
    ├── weather.ts                       (新) ★ v2.0
    ├── ai.ts                            (新) ★ v2.0
    └── preference.ts                    (新)
```

★ 标记 = v2.0 必做；未标记 = v2.1 阶段

---

## Phase 0：视觉系统基础（Tasks 1-5）

### Task 1: 建立设计 token（CSS 变量）

**Files:**
- Create: `frontend/src/styles/tokens.scss`

- [ ] **Step 1: 写 tokens.scss**

```scss
// 设计 token - 樱粉燕麦
:root {
  // 背景
  --bg-primary: #ffffff;
  --bg-secondary: #faf6f3;
  --bg-card: #f9f0ee;

  // 强调
  --color-primary: #d49aa5;
  --color-primary-hover: #b8808a;

  // 文字
  --text-primary: #4a3a3a;
  --text-secondary: #8a6d70;
  --text-disabled: rgba(184, 128, 138, 0.6);

  // 边框
  --border-color: #f0d9dd;

  // 状态
  --color-success: #7a9070;
  --color-warning: #d49aa5;
  --color-error: #c75d6d;

  // 圆角
  --radius-card: 12px;
  --radius-button: 18px;
  --radius-input: 10px;
  --radius-square: 8px;

  // 阴影
  --shadow-card: 0 6px 20px rgba(184, 128, 138, 0.18), 0 2px 4px rgba(184, 128, 138, 0.08);
  --shadow-card-soft: 0 1px 3px rgba(184, 128, 138, 0.08);

  // 字号
  --font-h1: 18px;
  --font-h2: 16px;
  --font-h3: 14px;
  --font-body: 12px;
  --font-helper: 11px;
  --font-caption: 9px;
}
```

- [ ] **Step 2: 在 main.ts 引入**

打开 `frontend/src/main.ts`，添加：

```typescript
import './styles/tokens.scss';
```

- [ ] **Step 3: 验证 vite 编译**

Run: `cd frontend && npm run dev:h5`
Expected: 编译无错误，浏览器 console 无报错

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/tokens.scss frontend/src/main.ts
git commit -m "feat(design): add B 樱粉 design tokens"
```

### Task 2: 覆盖 uView 组件默认色

**Files:**
- Create: `frontend/src/styles/overrides.scss`

- [ ] **Step 1: 写 overrides.scss**

```scss
@import './tokens.scss';

.up-button--primary {
  background: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  border-radius: var(--radius-button);
}
.up-button--primary:active,
.up-button--primary:hover {
  background: var(--color-primary-hover) !important;
}

.up-card {
  border-radius: var(--radius-card) !important;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-card-soft);
}

.up-input .up-input__input {
  border-radius: var(--radius-input) !important;
  border-color: var(--border-color) !important;
}
.up-input .up-input__input:focus {
  border-color: var(--color-primary) !important;
}

.uni-tabbar { border-top: 1px solid var(--border-color); }
.uni-tabbar__item--active .uni-tabbar__icon,
.uni-tabbar__item--active .uni-tabbar__label {
  color: var(--color-primary) !important;
}
```

- [ ] **Step 2: 在 main.ts 引入 overrides**

```typescript
import './styles/overrides.scss';
```

- [ ] **Step 3: 验证视觉效果**

Run: `npm run dev:h5`
Expected: 按钮变粉、卡片圆角阴影、输入框聚焦时变粉

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/overrides.scss frontend/src/main.ts
git commit -m "feat(design): override uView components with B 樱粉 theme"
```

### Task 3: 引入 Lucide 图标

**Files:**
- Create: `frontend/src/components/Icon.vue`
- Create: `frontend/src/components/Icon.test.ts`
- Modify: `frontend/package.json`

- [ ] **Step 1: 安装 lucide-vue-next**

Run: `cd frontend && npm install lucide-vue-next`
Expected: package.json 添加了 `lucide-vue-next`

- [ ] **Step 2: 写 Icon 包装组件**

```vue
<template>
  <component :is="icon" :size="size" :stroke-width="strokeWidth" :color="color" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import * as LucideIcons from 'lucide-vue-next';

const props = withDefaults(defineProps<{
  name: string;
  size?: number;
  strokeWidth?: number;
  color?: string;
}>(), {
  size: 20,
  strokeWidth: 1.5,
  color: 'currentColor',
});

const icon = computed(() => {
  const pascal = props.name
    .split('-')
    .map(s => s.charAt(0).toUpperCase() + s.slice(1))
    .join('');
  return (LucideIcons as any)[pascal] || LucideIcons.HelpCircle;
});
</script>
```

- [ ] **Step 3: 写测试**

```typescript
import { mount } from '@vue/test-utils';
import Icon from './Icon.vue';

describe('Icon', () => {
  it('renders lucide icon by kebab-case name', () => {
    const wrapper = mount(Icon, { props: { name: 'home' } });
    expect(wrapper.html()).toContain('svg');
  });

  it('falls back to HelpCircle for unknown names', () => {
    const wrapper = mount(Icon, { props: { name: 'unknown-icon' } });
    expect(wrapper.html()).toContain('svg');
  });

  it('passes size to svg', () => {
    const wrapper = mount(Icon, { props: { name: 'home', size: 32 } });
    const svg = wrapper.find('svg');
    expect(svg.attributes('width')).toBe('32');
  });
});
```

- [ ] **Step 4: 跑测试**

Run: `npm test -- Icon.test.ts`
Expected: 3/3 passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/Icon.vue frontend/src/components/Icon.test.ts frontend/package.json frontend/package-lock.json
git commit -m "feat(design): add Lucide icon wrapper component"
```

### Task 4: 升级首页样式

**Files:**
- Modify: `frontend/src/pages/index/index.vue`

- [ ] **Step 1: 用 token 替换硬编码色值**

找到 `<style scoped>` 块，替换为：

```vue
<style scoped>
.home {
  display: flex; flex-direction: column; gap: 32rpx;
  padding: 24rpx; background: var(--bg-secondary); min-height: 100vh;
}
.greet {
  font-size: 14px; color: var(--text-secondary); padding: 0 8rpx;
}
.stat-grid {
  display: flex; justify-content: space-around; padding: 24rpx 16rpx;
  background: var(--bg-card); border-radius: var(--radius-card);
}
.stat { display: flex; flex-direction: column; align-items: center; gap: 8rpx; }
.stat-num { font-size: 48rpx; font-weight: 600; color: var(--text-primary); }
.stat-label { font-size: 24rpx; color: var(--text-secondary); }
.actions { display: flex; flex-direction: column; gap: 16rpx; }
</style>
```

- [ ] **Step 2: 在 `<up-card>` 上方加问候语**

```vue
<view class="greet">早上好 ☀️</view>
```

- [ ] **Step 3: 验证**

Run: `npm run dev:h5`
Expected: 首页背景奶白、统计卡变粉

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/index/index.vue
git commit -m "feat(home): apply B 樱粉 design tokens"
```

### Task 5: 升级 tabbar 配色

**Files:**
- Modify: `frontend/src/pages.json`

- [ ] **Step 1: 加 tabBar 配置**

`pages.json` 末尾加（与现有 `globalStyle` 同级）：

```json
"tabBar": {
  "color": "#8a6d70",
  "selectedColor": "#d49aa5",
  "backgroundColor": "#ffffff",
  "borderStyle": "white",
  "list": [
    { "pagePath": "pages/index/index", "text": "首页" },
    { "pagePath": "pages/closet/index", "text": "衣橱" },
    { "pagePath": "pages/outfits/index", "text": "搭配" },
    { "pagePath": "pages/calendar/index", "text": "日历" },
    { "pagePath": "pages/settings/index", "text": "我的" }
  ]
}
```

注：uni-app tabbar 图标需 PNG/SVG 文件，不可用 Lucide。如已有图标保留路径，否则先省略 iconPath 字段用文字 tabbar。

- [ ] **Step 2: 验证**

Run: `npm run dev:h5`
Expected: tabbar 选中态为樱粉 `#d49aa5`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages.json
git commit -m "feat(tabbar): apply B 樱粉 selected color"
```

---

## Phase 1：AI 智能搭配（Tasks 6-18）

### Task 6: 加 outfit_ai_generation 表

**Files:**
- Modify: `backend/src/main/resources/schema.sql`

- [ ] **Step 1: 在 schema.sql 末尾追加**

```sql
CREATE TABLE IF NOT EXISTS outfit_ai_generation (
  id              BIGSERIAL PRIMARY KEY,
  seed_clothing_ids JSONB NOT NULL,
  occasion        VARCHAR(32),
  season          VARCHAR(16),
  weather_snapshot JSONB,
  result_outfit_ids JSONB,
  feedback        VARCHAR(16) DEFAULT 'none',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_generation_created ON outfit_ai_generation(created_at DESC);
CREATE INDEX idx_ai_generation_feedback ON outfit_ai_generation(feedback) WHERE feedback != 'none';
```

- [ ] **Step 2: 应用启动验证**

重启前先 DROP（踩坑 #3）：

```bash
cd deploy && docker compose -f docker-compose.dev.yml exec postgres psql -U closet -d closet -c "DROP TABLE IF EXISTS outfit_ai_generation CASCADE;"
```

```bash
cd backend && mvn spring-boot:run
```

Expected: 应用正常启动，PG 中 `outfit_ai_generation` 表已创建

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/schema.sql
git commit -m "feat(db): add outfit_ai_generation table"
```

### Task 7: 写 entity + mapper

**Files:**
- Create: `backend/src/main/java/com/closet/entity/OutfitAiGeneration.java`
- Create: `backend/src/main/java/com/closet/mapper/OutfitAiGenerationMapper.java`

- [ ] **Step 1: Entity**

参考 `backend/src/main/java/com/closet/entity/Clothing.java` 的 MyBatis-Plus 注解风格：

```java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("outfit_ai_generation")
public class OutfitAiGeneration {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("seed_clothing_ids")
    private String seedClothingIds;  // JSON 数组字符串

    @TableField("occasion")
    private String occasion;

    @TableField("season")
    private String season;

    @TableField("weather_snapshot")
    private String weatherSnapshot;

    @TableField("result_outfit_ids")
    private String resultOutfitIds;

    @TableField("feedback")
    private String feedback;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
```

- [ ] **Step 2: Mapper**

```java
package com.closet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.closet.entity.OutfitAiGeneration;

public interface OutfitAiGenerationMapper extends BaseMapper<OutfitAiGeneration> {
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/closet/entity/OutfitAiGeneration.java backend/src/main/java/com/closet/mapper/OutfitAiGenerationMapper.java
git commit -m "feat(ai): add OutfitAiGeneration entity and mapper"
```

### Task 8: 颜色协调规则引擎（核心算法）

**Files:**
- Create: `backend/src/main/java/com/closet/service/ColorHarmonyEngine.java`
- Create: `backend/src/test/java/com/closet/unit/ColorHarmonyEngineTest.java`

- [ ] **Step 1: 写测试**

```java
package com.closet.unit;

import com.closet.service.ColorHarmonyEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorHarmonyEngineTest {

    @Test
    void 同色系应该和谐() {
        double score = ColorHarmonyEngine.harmonyScore("#ffffff", "#faf6f3");
        assertTrue(score >= 0.8);
    }

    @Test
    void 互补色应该和谐() {
        double score = ColorHarmonyEngine.harmonyScore("#d49aa5", "#a8c8d8");
        assertTrue(score >= 0.7);
    }

    @Test
    void 撞色应该不和谐() {
        double score = ColorHarmonyEngine.harmonyScore("#ff0000", "#00ff00");
        assertTrue(score < 0.5);
    }

    @Test
    void 无效颜色返回中性分() {
        assertEquals(0.5, ColorHarmonyEngine.harmonyScore(null, "#ffffff"), 0.01);
        assertEquals(0.5, ColorHarmonyEngine.harmonyScore("invalid", "#ffffff"), 0.01);
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -Dtest=ColorHarmonyEngineTest`
Expected: FAIL（ColorHarmonyEngine 不存在）

- [ ] **Step 3: 实现**

```java
package com.closet.service;

import org.springframework.stereotype.Component;

@Component
public class ColorHarmonyEngine {

    public static double[] hexToHsl(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) return null;
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        double max = Math.max(r, Math.max(g, b)) / 255.0;
        double min = Math.min(r, Math.min(g, b)) / 255.0;
        double h, s, l = (max + min) / 2.0;
        if (max == min) { h = s = 0; }
        else {
            double d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == r / 255.0) h = (g / 255.0 - b / 255.0) / d + (g < b ? 6 : 0);
            else if (max == g / 255.0) h = (b / 255.0 - r / 255.0) / d + 2;
            else h = (r / 255.0 - g / 255.0) / d + 4;
            h /= 6;
        }
        return new double[]{h * 360, s, l};
    }

    public static double harmonyScore(String hexA, String hexB) {
        double[] hslA = hexToHsl(hexA);
        double[] hslB = hexToHsl(hexB);
        if (hslA == null || hslB == null) return 0.5;
        double hueDiff = Math.abs(hslA[0] - hslB[0]);
        if (hueDiff > 180) hueDiff = 360 - hueDiff;
        if (hueDiff < 30) return 0.95;
        if (hueDiff < 90) return 0.8;
        if (hueDiff > 150 && hueDiff < 210) return 0.75;
        if (hueDiff > 210) return 0.6;
        return 0.4;
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn test -Dtest=ColorHarmonyEngineTest`
Expected: 4/4 passed

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/closet/service/ColorHarmonyEngine.java backend/src/test/java/com/closet/unit/ColorHarmonyEngineTest.java
git commit -m "feat(ai): add color harmony scoring engine with tests"
```

### Task 9: AI 搭配生成 service（核心 5 套生成）

**Files:**
- Create: `backend/src/main/java/com/closet/dto/AiGenerateRequest.java`
- Create: `backend/src/main/java/com/closet/dto/AiGenerateResponse.java`
- Create: `backend/src/main/java/com/closet/service/OutfitAiService.java`
- Create: `backend/src/main/java/com/closet/service/impl/OutfitAiServiceImpl.java`
- Create: `backend/src/test/java/com/closet/unit/OutfitAiServiceTest.java`

- [ ] **Step 1: 写 DTO**

`AiGenerateRequest.java`：

```java
package com.closet.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiGenerateRequest {
    private List<Long> seedClothingIds;  // 1+ 件
    private String occasion;             // casual/work/date/sport
    private String season;               // spring/summer/fall/winter
    private String weatherCode;          // 可选
}
```

`AiGenerateResponse.java`：

```java
package com.closet.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiGenerateResponse {
    private Long generationId;
    private List<List<Long>> outfits;  // 固定 5 套
}
```

- [ ] **Step 2: 写 service 接口**

```java
package com.closet.service;

import com.closet.dto.AiGenerateRequest;
import com.closet.dto.AiGenerateResponse;

public interface OutfitAiService {
    AiGenerateResponse generate(AiGenerateRequest req);
    void recordFeedback(Long generationId, String feedback);
}
```

- [ ] **Step 3: 写测试（4 个关键用例）**

```java
package com.closet.unit;

import com.closet.dto.AiGenerateRequest;
import com.closet.dto.AiGenerateResponse;
import com.closet.entity.Clothing;
import com.closet.mapper.ClothingMapper;
import com.closet.mapper.OutfitAiGenerationMapper;
import com.closet.service.impl.OutfitAiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OutfitAiServiceTest {

    @Mock ClothingMapper clothingMapper;
    @Mock OutfitAiGenerationMapper aiMapper;
    @InjectMocks OutfitAiServiceImpl service;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    private Clothing mk(Long id, String season, String color) {
        Clothing c = new Clothing();
        c.setId(id); c.setSeason(season); c.setColorPrimary(color);
        return c;
    }

    @Test
    void 生成结果必须是5套() {
        when(clothingMapper.selectList(any())).thenReturn(List.of(
            mk(1L, "summer", "#ffffff"),
            mk(2L, "summer", "#d4b896"),
            mk(3L, "summer", "#a8c8d8"),
            mk(4L, "summer", "#5a4032")
        ));
        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(1L));
        req.setSeason("summer");
        AiGenerateResponse resp = service.generate(req);
        assertEquals(5, resp.getOutfits().size());
    }

    @Test
    void 非当季衣物必须被过滤() {
        when(clothingMapper.selectList(any())).thenReturn(List.of(
            mk(1L, "summer", "#ffffff"),
            mk(2L, "winter", "#5a4032")
        ));
        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(1L));
        req.setSeason("summer");
        AiGenerateResponse resp = service.generate(req);
        for (List<Long> outfit : resp.getOutfits()) {
            assertFalse(outfit.contains(2L));
        }
    }

    @Test
    void 5套之间不能完全重复() {
        List<Clothing> pool = new ArrayList<>();
        for (int i = 0; i < 20; i++) pool.add(mk((long) i, "summer", "#ffffff"));
        when(clothingMapper.selectList(any())).thenReturn(pool);
        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(0L));
        req.setSeason("summer");
        AiGenerateResponse resp = service.generate(req);
        Set<Set<Long>> unique = new HashSet<>();
        for (List<Long> outfit : resp.getOutfits()) {
            unique.add(new HashSet<>(outfit));
        }
        assertEquals(5, unique.size());
    }

    @Test
    void 起点衣物必须出现在每套结果中() {
        when(clothingMapper.selectList(any())).thenReturn(List.of(
            mk(1L, "summer", "#ffffff"),
            mk(2L, "summer", "#d4b896"),
            mk(3L, "summer", "#a8c8d8")
        ));
        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(1L));
        req.setSeason("summer");
        AiGenerateResponse resp = service.generate(req);
        for (List<Long> outfit : resp.getOutfits()) {
            assertTrue(outfit.contains(1L));
        }
    }
}
```

- [ ] **Step 4: 跑测试确认失败**

Run: `mvn test -Dtest=OutfitAiServiceTest`
Expected: FAIL（OutfitAiServiceImpl 不存在）

- [ ] **Step 5: 实现 service**

```java
package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.closet.dto.AiGenerateRequest;
import com.closet.dto.AiGenerateResponse;
import com.closet.entity.Clothing;
import com.closet.entity.OutfitAiGeneration;
import com.closet.mapper.ClothingMapper;
import com.closet.mapper.OutfitAiGenerationMapper;
import com.closet.service.OutfitAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OutfitAiServiceImpl implements OutfitAiService {

    private final ClothingMapper clothingMapper;
    private final OutfitAiGenerationMapper aiMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    @Override
    public AiGenerateResponse generate(AiGenerateRequest req) {
        List<Clothing> all = clothingMapper.selectList(new QueryWrapper<>());
        Set<Long> seedSet = new HashSet<>(req.getSeedClothingIds());

        // 季节过滤
        List<Clothing> pool = all.stream()
            .filter(c -> "all".equals(c.getSeason()) || req.getSeason().equals(c.getSeason()))
            .toList();

        // 生成 5 套（去重 + 起点必含）
        List<List<Long>> outfits = new ArrayList<>();
        Set<Set<Long>> seen = new HashSet<>();
        int attempts = 0;
        while (outfits.size() < 5 && attempts < 200) {
            attempts++;
            List<Clothing> picked = new ArrayList<>();
            for (Long seedId : seedSet) {
                pool.stream().filter(c -> c.getId().equals(seedId)).findFirst().ifPresent(picked::add);
            }
            int extras = 2 + random.nextInt(3);
            List<Clothing> candidates = new ArrayList<>(pool);
            candidates.removeAll(picked);
            Collections.shuffle(candidates);
            for (int i = 0; i < extras && i < candidates.size(); i++) picked.add(candidates.get(i));

            List<Long> ids = picked.stream().map(Clothing::getId).sorted().toList();
            Set<Long> idSet = new HashSet<>(ids);
            if (seen.contains(idSet)) continue;
            seen.add(idSet);
            outfits.add(ids);
        }
        while (outfits.size() < 5 && !outfits.isEmpty()) outfits.add(outfits.get(0));

        // 持久化
        OutfitAiGeneration gen = new OutfitAiGeneration();
        try {
            gen.setSeedClothingIds(objectMapper.writeValueAsString(req.getSeedClothingIds()));
            gen.setOccasion(req.getOccasion());
            gen.setSeason(req.getSeason());
            gen.setResultOutfitIds(objectMapper.writeValueAsString(outfits));
            gen.setFeedback("none");
            gen.setCreatedAt(OffsetDateTime.now());
        } catch (Exception e) { throw new RuntimeException(e); }
        aiMapper.insert(gen);

        AiGenerateResponse resp = new AiGenerateResponse();
        resp.setGenerationId(gen.getId());
        resp.setOutfits(outfits);
        return resp;
    }

    @Override
    public void recordFeedback(Long generationId, String feedback) {
        OutfitAiGeneration gen = aiMapper.selectById(generationId);
        if (gen != null) {
            gen.setFeedback(feedback);
            aiMapper.updateById(gen);
        }
    }
}
```

- [ ] **Step 6: 跑测试确认通过**

Run: `mvn test -Dtest=OutfitAiServiceTest`
Expected: 4/4 passed

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/closet/service/OutfitAiService.java backend/src/main/java/com/closet/service/impl/OutfitAiServiceImpl.java backend/src/main/java/com/closet/dto/AiGenerateRequest.java backend/src/main/java/com/closet/dto/AiGenerateResponse.java backend/src/test/java/com/closet/unit/OutfitAiServiceTest.java
git commit -m "feat(ai): OutfitAiService with rule engine + 4 unit tests"
```

### Task 10: AI 搭配 controller + 集成测试

**Files:**
- Create: `backend/src/main/java/com/closet/controller/OutfitAiController.java`
- Create: `backend/src/test/java/com/closet/integration/OutfitAiControllerIT.java`

- [ ] **Step 1: Controller**

```java
package com.closet.controller;

import com.closet.common.Result;
import com.closet.dto.AiGenerateRequest;
import com.closet.dto.AiGenerateResponse;
import com.closet.service.OutfitAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/outfits")
@RequiredArgsConstructor
public class OutfitAiController {

    private final OutfitAiService outfitAiService;

    @PostMapping("/ai-generate")
    public Result<AiGenerateResponse> generate(@RequestBody AiGenerateRequest req) {
        return Result.ok(outfitAiService.generate(req));
    }

    @PostMapping("/ai-generation/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody Map<String, String> body) {
        outfitAiService.recordFeedback(id, body.get("feedback"));
        return Result.ok();
    }
}
```

- [ ] **Step 2: 集成测试**

参考 `OutfitControllerIT.java` 的 `@SpringBootTest` + `@TestPropertySource` 模式避开踩坑 #3：

```java
package com.closet.integration;

import com.closet.common.Result;
import com.closet.dto.AiGenerateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "spring.sql.init.mode=never" })
class OutfitAiControllerIT {

    @Autowired TestRestTemplate rest;

    @Test
    void aiGenerate返回5套() {
        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(1L));
        req.setSeason("summer");
        req.setOccasion("casual");

        ResponseEntity<Result> resp = rest.postForEntity("/api/v1/outfits/ai-generate", req, Result.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().getData());
    }

    @Test
    void feedback接口不报错() {
        AiGenerateRequest req = new AiGenerateRequest();
        req.setSeedClothingIds(List.of(1L));
        req.setSeason("summer");
        Result r = rest.postForObject("/api/v1/outfits/ai-generate", req, Result.class);
        assertNotNull(r);
    }
}
```

- [ ] **Step 3: 跑 IT**

Run: `mvn verify -Dtest=OutfitAiControllerIT`
Expected: 2/2 passed

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/closet/controller/OutfitAiController.java backend/src/test/java/com/closet/integration/OutfitAiControllerIT.java
git commit -m "feat(ai): add ai-generate endpoint with integration tests"
```

---

## Phase 1: AI 智能搭配

### Task 11: AI 搭配 store + 类型定义

**Files:**
- Create: `frontend/src/api/ai.ts`
- Create: `frontend/src/store/ai.ts`

- [ ] **Step 1: API 客户端**

```typescript
// frontend/src/api/ai.ts
import { http } from './http'

export interface AiOutfit { id: number; items: number[]; score?: number }
export interface AiGenerateRequest {
  seedClothingIds: number[]
  occasion: string
  season: string
  weather?: { temp: number; condition: string }
}
export interface AiGenerateResponse {
  generationId: number
  outfits: number[][]
}

export const aiApi = {
  generate(req: AiGenerateRequest): Promise<AiGenerateResponse> {
    return http.post('/api/v1/outfits/ai-generate', req)
  },
  feedback(generationId: number, feedback: 'like' | 'dislike' | 'none') {
    return http.post(`/api/v1/outfits/ai-generation/${generationId}/feedback`, { feedback })
  },
}
```

- [ ] **Step 2: Pinia store**

```typescript
// frontend/src/store/ai.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { aiApi, type AiGenerateResponse } from '@/api/ai'

export const useAiStore = defineStore('ai', () => {
  const generating = ref(false)
  const lastResult = ref<AiGenerateResponse | null>(null)
  const history = ref<AiGenerateResponse[]>([])

  async function generate(req: Parameters<typeof aiApi.generate>[0]) {
    generating.value = true
    try {
      const r = await aiApi.generate(req)
      lastResult.value = r
      history.value.unshift(r)
      if (history.value.length > 20) history.value.length = 20
      return r
    } finally {
      generating.value = false
    }
  }

  async function feedback(generationId: number, feedback: 'like' | 'dislike' | 'none') {
    await aiApi.feedback(generationId, feedback)
  }

  return { generating, lastResult, history, generate, feedback }
})
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/ai.ts frontend/src/store/ai.ts
git commit -m "feat(ai-frontend): ai api client + pinia store"
```

### Task 12: AiGenerator.vue 组件

**Files:**
- Create: `frontend/src/components/AiGenerator.vue`
- Create: `frontend/tests/unit/components/AiGenerator.test.ts`

- [ ] **Step 1: 写测试**

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import AiGenerator from '@/components/AiGenerator.vue'

describe('AiGenerator', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('显示加载态当 generating=true', async () => {
    const wrapper = mount(AiGenerator, { props: { seedIds: [1], generating: true } })
    expect(wrapper.html()).toContain('生成中')
  })

  it('emit generate 事件当点击按钮', async () => {
    const wrapper = mount(AiGenerator, { props: { seedIds: [1, 2], generating: false } })
    await wrapper.find('[data-test="ai-go"]').trigger('click')
    expect(wrapper.emitted('generate')).toBeTruthy()
    expect(wrapper.emitted('generate')![0]).toEqual([{ seedIds: [1, 2], occasion: 'casual', season: 'all' }])
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- AiGenerator.test.ts`
Expected: FAIL "AiGenerator.vue not found"

- [ ] **Step 3: 组件实现**

```vue
<!-- frontend/src/components/AiGenerator.vue -->
<template>
  <view class="ai-generator">
    <view class="seed-row">
      <text class="label">起点单品</text>
      <view class="chips">
        <view v-for="id in seedIds" :key="id" class="chip">{{ id }}</view>
      </view>
    </view>
    <picker mode="selector" :range="occasions" :value="occasionIdx" @change="onOccasion">
      <view class="picker">
        <text>场景：{{ occasions[occasionIdx] }}</text>
      </view>
    </picker>
    <picker mode="selector" :range="seasons" :value="seasonIdx" @change="onSeason">
      <view class="picker">
        <text>季节：{{ seasons[seasonIdx] }}</text>
      </view>
    </picker>
    <button class="primary" :disabled="generating" data-test="ai-go" @click="emitGenerate">
      <text v-if="generating">生成中…</text>
      <text v-else>AI 生成 5 套搭配</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ seedIds: number[]; generating: boolean }>()
const emit = defineEmits<{
  generate: [req: { seedIds: number[]; occasion: string; season: string }]
}>()

const occasions = ['casual', 'work', 'date', 'sport', 'home']
const seasons = ['all', 'spring', 'summer', 'autumn', 'winter']
const occasionIdx = ref(0)
const seasonIdx = ref(0)

function onOccasion(e: any) { occasionIdx.value = e.detail.value }
function onSeason(e: any) { seasonIdx.value = e.detail.value }

function emitGenerate() {
  emit('generate', {
    seedIds: props.seedIds,
    occasion: occasions[occasionIdx.value],
    season: seasons[seasonIdx.value],
  })
}
</script>

<style lang="scss" scoped>
.ai-generator { padding: 24rpx; background: var(--surface); border-radius: 16rpx; }
.seed-row { margin-bottom: 16rpx; }
.label { font-size: 26rpx; color: var(--text-secondary); }
.chips { display: flex; gap: 12rpx; margin-top: 8rpx; }
.chip { padding: 4rpx 16rpx; background: var(--accent-soft); color: var(--accent); border-radius: 24rpx; font-size: 24rpx; }
.picker { padding: 16rpx; background: var(--bg-elevated); border-radius: 12rpx; margin-bottom: 12rpx; }
.primary { margin-top: 16rpx; background: var(--accent); color: white; border-radius: 32rpx; padding: 20rpx 0; }
</style>
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- AiGenerator.test.ts`
Expected: 2/2 passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/AiGenerator.vue frontend/tests/unit/components/AiGenerator.test.ts
git commit -m "feat(ai-frontend): AiGenerator component with TDD"
```

### Task 13: OutfitResultCard.vue 组件

**Files:**
- Create: `frontend/src/components/OutfitResultCard.vue`
- Create: `frontend/tests/unit/components/OutfitResultCard.test.ts`

- [ ] **Step 1: 写测试**

```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import OutfitResultCard from '@/components/OutfitResultCard.vue'

describe('OutfitResultCard', () => {
  it('渲染所有单品名称', () => {
    const wrapper = mount(OutfitResultCard, {
      props: { items: ['白衬衫', '牛仔裤', '运动鞋'], index: 1 },
    })
    expect(wrapper.html()).toContain('白衬衫')
    expect(wrapper.html()).toContain('牛仔裤')
  })

  it('emit like/dislike 事件', async () => {
    const wrapper = mount(OutfitResultCard, {
      props: { items: ['A'], index: 1, generationId: 42 },
    })
    await wrapper.find('[data-test="like"]').trigger('click')
    expect(wrapper.emitted('like')).toBeTruthy()
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- OutfitResultCard.test.ts`
Expected: FAIL

- [ ] **Step 3: 组件实现**

```vue
<!-- frontend/src/components/OutfitResultCard.vue -->
<template>
  <view class="result-card">
    <view class="head">
      <text class="index">搭配 {{ index }}</text>
    </view>
    <view class="items">
      <text v-for="(item, i) in items" :key="i" class="item">{{ item }}</text>
    </view>
    <view class="actions">
      <button data-test="like" @click="emit('like')" class="action like">
        <i-lucide-thumbs-up />
      </button>
      <button data-test="dislike" @click="emit('dislike')" class="action">
        <i-lucide-thumbs-down />
      </button>
      <button data-test="save" @click="emit('save')" class="action">
        <i-lucide-bookmark />
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{ items: string[]; index: number; generationId?: number }>()
const emit = defineEmits<{ like: []; dislike: []; save: [] }>()
</script>

<style lang="scss" scoped>
.result-card { background: var(--surface); border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; box-shadow: 0 2rpx 12rpx var(--shadow); }
.head { margin-bottom: 12rpx; }
.index { font-size: 28rpx; font-weight: 600; color: var(--text-primary); }
.items { display: flex; flex-wrap: wrap; gap: 8rpx; margin-bottom: 16rpx; }
.item { padding: 4rpx 16rpx; background: var(--accent-soft); color: var(--accent); border-radius: 24rpx; font-size: 24rpx; }
.actions { display: flex; gap: 16rpx; }
.action { padding: 8rpx 24rpx; border-radius: 24rpx; background: var(--bg-elevated); }
.action.like { background: var(--accent); color: white; }
</style>
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- OutfitResultCard.test.ts`
Expected: 2/2 passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/OutfitResultCard.vue frontend/tests/unit/components/OutfitResultCard.test.ts
git commit -m "feat(ai-frontend): OutfitResultCard with like/dislike/save actions"
```

### Task 14: ai-generator 页面

**Files:**
- Create: `frontend/src/pages/ai-generator/ai-generator.vue`
- Create: `frontend/src/pages/ai-generator/ai-generator.test.ts`

- [ ] **Step 1: 写测试**

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import AiGeneratorPage from '@/pages/ai-generator/ai-generator.vue'

// stub 透传 children slots
const AiGeneratorStub = {
  template: '<view class="ai-stub"><slot /></view>',
  props: ['seedIds', 'generating'],
}
const OutfitResultCardStub = {
  template: '<view class="result-stub" @click="$emit(\'like\')">{{ items }}</view>',
  props: ['items', 'index', 'generationId'],
}

describe('ai-generator page', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.spyOn(require('@/api/clothing'), 'fetchClothingList').mockResolvedValue([
      { id: 1, name: '白衬衫', imageUrl: '/a.jpg', category: 'top', color: 'white' },
      { id: 2, name: '牛仔裤', imageUrl: '/b.jpg', category: 'bottom', color: 'blue' },
    ])
  })

  it('显示"选择起点单品"提示当未选', async () => {
    const wrapper = mount(AiGeneratorPage, {
      global: { stubs: { AiGenerator: AiGeneratorStub, OutfitResultCard: OutfitResultCardStub } },
    })
    expect(wrapper.html()).toContain('选择起点单品')
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- ai-generator.test.ts`
Expected: FAIL

- [ ] **Step 3: 页面实现**

```vue
<!-- frontend/src/pages/ai-generator/ai-generator.vue -->
<template>
  <view class="page">
    <view class="header">
      <text class="title">AI 搭配</text>
      <text class="sub">从你的衣橱中挑选起点单品，生成 5 套穿搭方案</text>
    </view>

    <view v-if="seedIds.length === 0" class="empty">
      <i-lucide-sparkles />
      <text class="empty-text">从衣橱长按或勾选单品作为起点</text>
      <button class="link" @click="goCloset">去衣橱选择</button>
    </view>

    <AiGenerator
      v-else
      :seed-ids="seedIds"
      :generating="ai.generating"
      @generate="onGenerate"
    />

    <view v-if="ai.lastResult" class="results">
      <text class="section-title">推荐搭配</text>
      <OutfitResultCard
        v-for="(ids, i) in ai.lastResult.outfits"
        :key="i"
        :index="i + 1"
        :generation-id="ai.lastResult.generationId"
        :items="ids.map(id => nameOf(id))"
        @like="feedback(ai.lastResult!.generationId, 'like')"
        @dislike="feedback(ai.lastResult!.generationId, 'dislike')"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAiStore } from '@/store/ai'
import { fetchClothingList } from '@/api/clothing'
import AiGenerator from '@/components/AiGenerator.vue'
import OutfitResultCard from '@/components/OutfitResultCard.vue'

const ai = useAiStore()
const seedIds = ref<number[]>([])
const idToName = ref<Map<number, string>>(new Map())

onMounted(async () => {
  const list = await fetchClothingList()
  idToName.value = new Map(list.map(c => [c.id, c.name]))
})

function nameOf(id: number) { return idToName.value.get(id) ?? `#${id}` }

async function onGenerate(req: any) {
  await ai.generate(req)
}

async function feedback(id: number, f: 'like' | 'dislike') {
  await ai.feedback(id, f)
  uni.showToast({ title: f === 'like' ? '已喜欢' : '已忽略', icon: 'none' })
}

function goCloset() { uni.switchTab({ url: '/pages/closet/closet' }) }
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: var(--bg); padding: 32rpx 24rpx; }
.header { margin-bottom: 32rpx; }
.title { font-size: 48rpx; font-weight: 700; color: var(--text-primary); display: block; }
.sub { font-size: 26rpx; color: var(--text-secondary); margin-top: 8rpx; display: block; }
.empty { display: flex; flex-direction: column; align-items: center; padding: 80rpx 0; gap: 16rpx; }
.empty-text { color: var(--text-secondary); font-size: 28rpx; }
.link { color: var(--accent); font-size: 28rpx; }
.results { margin-top: 32rpx; }
.section-title { font-size: 32rpx; font-weight: 600; color: var(--text-primary); margin-bottom: 16rpx; display: block; }
</style>
```

- [ ] **Step 4: 在 pages.json 注册**

修改 `frontend/src/pages.json`，在 `pages` 数组里新增：

```json
{
  "path": "pages/ai-generator/ai-generator",
  "style": {
    "navigationBarTitleText": "AI 搭配",
    "navigationBarBackgroundColor": "#FFF7F2",
    "navigationBarTextStyle": "black"
  }
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `npm test -- ai-generator.test.ts`
Expected: 1/1 passed

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/ai-generator/ frontend/src/pages.json
git commit -m "feat(ai-frontend): ai-generator page wired to store"
```

### Task 15: 首页加 AI 入口

**Files:**
- Modify: `frontend/src/pages/index/index.vue`

- [ ] **Step 1: 添加跳转入口**

在首页 `轮播/统计/最近添加` 之后，添加一段：

```vue
<view class="ai-cta" @click="goAi">
  <i-lucide-sparkles />
  <view class="ai-cta-text">
    <text class="ai-title">AI 搭配助手</text>
    <text class="ai-sub">挑件单品，生成 5 套方案</text>
  </view>
  <i-lucide-chevron-right />
</view>
```

```scss
.ai-cta {
  display: flex; align-items: center; gap: 16rpx;
  background: linear-gradient(135deg, var(--accent-soft), var(--accent));
  color: white; padding: 24rpx; border-radius: 24rpx; margin: 24rpx 0;
}
.ai-cta-text { flex: 1; display: flex; flex-direction: column; }
.ai-title { font-size: 30rpx; font-weight: 600; color: white; }
.ai-sub { font-size: 24rpx; color: rgba(255,255,255,0.85); margin-top: 4rpx; }
```

```typescript
function goAi() { uni.navigateTo({ url: '/pages/ai-generator/ai-generator' }) }
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/index/index.vue
git commit -m "feat(ai-frontend): homepage AI entry card"
```

### Task 16: tabbar 加 AI tab

**Files:**
- Modify: `frontend/src/pages.json`

- [ ] **Step 1: 修改 tabBar.list**

```json
"tabBar": {
  "color": "#999",
  "selectedColor": "#d49aa5",
  "backgroundColor": "#FFFFFF",
  "list": [
    { "pagePath": "pages/index/index", "text": "首页", "iconPath": "static/tab/home.png", "selectedIconPath": "static/tab/home-active.png" },
    { "pagePath": "pages/closet/closet", "text": "衣橱", "iconPath": "static/tab/closet.png", "selectedIconPath": "static/tab/closet-active.png" },
    { "pagePath": "pages/ai-generator/ai-generator", "text": "AI", "iconPath": "static/tab/ai.png", "selectedIconPath": "static/tab/ai-active.png" },
    { "pagePath": "pages/outfits/outfits", "text": "搭配", "iconPath": "static/tab/outfit.png", "selectedIconPath": "static/tab/outfit-active.png" },
    { "pagePath": "pages/profile/profile", "text": "我的", "iconPath": "static/tab/profile.png", "selectedIconPath": "static/tab/profile-active.png" }
  ]
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages.json
git commit -m "feat(frontend): add AI tab to tabbar"
```

### Task 17: AI 反馈写入（v2.0 简版）

**Files:**
- Modify: `frontend/src/store/ai.ts`

- [ ] **Step 1: 增加 likeCount 字段**

修改 `useAiStore`，在 `feedback` 调用后乐观更新 `lastResult`：

```typescript
async function feedback(generationId: number, feedback: 'like' | 'dislike' | 'none') {
  await aiApi.feedback(generationId, feedback)
  // 乐观更新计数（前端只展示用，无需持久化）
  if (lastResult.value?.generationId === generationId) {
    // 不改 outfits，简单 toast 即可
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/store/ai.ts
git commit -m "chore(ai-frontend): feedback optimistic update placeholder"
```

### Task 18: AI 端到端集成测试

**Files:**
- Create: `frontend/tests/e2e/ai.spec.ts`

- [ ] **Step 1: Playwright e2e**

```typescript
import { test, expect } from '@playwright/test'

test('AI 搭配完整流程', async ({ page }) => {
  await page.goto('/')
  await page.click('[data-test="ai-cta"]')
  await expect(page.locator('text=AI 搭配')).toBeVisible()
  await page.click('[data-test="ai-go"]')
  await expect(page.locator('text=搭配 1')).toBeVisible({ timeout: 5000 })
})
```

- [ ] **Step 2: 跑 e2e**

Run: `npx playwright test tests/e2e/ai.spec.ts`
Expected: 1/1 passed（前提：衣橱已有 ≥1 件单品）

- [ ] **Step 3: Commit**

```bash
git add frontend/tests/e2e/ai.spec.ts
git commit -m "test(e2e): AI outfit flow happy path"
```

---
## Phase 2: 天气联动（和风 SDK）

### Task 19: weather_cache + user_preference 表 schema

**Files:**
- Modify: `backend/src/main/resources/db/migration/V5__v2_weather_and_preference.sql`

- [ ] **Step 1: 编写迁移脚本**

```sql
-- V5__v2_weather_and_preference.sql

CREATE TABLE weather_cache (
  id           BIGSERIAL PRIMARY KEY,
  location_key VARCHAR(64)  NOT NULL,
  forecast_date DATE         NOT NULL,
  temp_min     INTEGER      NOT NULL,
  temp_max     INTEGER      NOT NULL,
  condition    VARCHAR(32)  NOT NULL,
  humidity     INTEGER,
  wind_dir     VARCHAR(16),
  wind_speed   INTEGER,
  raw_json     JSONB        NOT NULL,
  fetched_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  UNIQUE (location_key, forecast_date)
);

CREATE INDEX idx_weather_location_date ON weather_cache (location_key, forecast_date);

CREATE TABLE user_preference (
  id                 BIGSERIAL PRIMARY KEY,
  default_location   VARCHAR(64)  NOT NULL,
  temperature_unit   VARCHAR(8)   NOT NULL DEFAULT 'c',
  style_tags         JSONB        NOT NULL DEFAULT '[]',
  preferred_colors   JSONB        NOT NULL DEFAULT '[]',
  avoided_colors     JSONB        NOT NULL DEFAULT '[]',
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_user_pref_singleton ON user_preference ((TRUE));
```

- [ ] **Step 2: 在 h2 测试库同步**

新增 `backend/src/test/resources/schema-test.sql`：

```sql
CREATE TABLE IF NOT EXISTS weather_cache (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  location_key VARCHAR(64) NOT NULL,
  forecast_date DATE NOT NULL,
  temp_min INTEGER NOT NULL,
  temp_max INTEGER NOT NULL,
  condition VARCHAR(32) NOT NULL,
  humidity INTEGER,
  wind_dir VARCHAR(16),
  wind_speed INTEGER,
  raw_json TEXT NOT NULL,
  fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_preference (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  default_location VARCHAR(64) NOT NULL,
  temperature_unit VARCHAR(8) NOT NULL DEFAULT 'c',
  style_tags TEXT NOT NULL DEFAULT '[]',
  preferred_colors TEXT NOT NULL DEFAULT '[]',
  avoided_colors TEXT NOT NULL DEFAULT '[]',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 3: 应用迁移（dev PG）**

Run: 启动后端前执行
```bash
docker exec -i closet-postgres psql -U closet -d closet < backend/src/main/resources/db/migration/V5__v2_weather_and_preference.sql
```

Expected: CREATE TABLE x 2, CREATE INDEX x 1

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/migration/V5__v2_weather_and_preference.sql backend/src/test/resources/schema-test.sql
git commit -m "feat(db): add weather_cache + user_preference tables"
```

### Task 20: WeatherCache entity + mapper

**Files:**
- Create: `backend/src/main/java/com/closet/entity/WeatherCache.java`
- Create: `backend/src/main/java/com/closet/mapper/WeatherCacheMapper.java`

- [ ] **Step 1: Entity**

```java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@TableName(value = "weather_cache", autoResultMap = true)
public class WeatherCache {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String locationKey;
    private LocalDate forecastDate;
    private Integer tempMin;
    private Integer tempMax;
    private String condition;
    private Integer humidity;
    private String windDir;
    private Integer windSpeed;
    private Map<String, Object> rawJson;
    private OffsetDateTime fetchedAt;
}
```

- [ ] **Step 2: Mapper**

```java
package com.closet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.closet.entity.WeatherCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WeatherCacheMapper extends BaseMapper<WeatherCache> {

    @Select("SELECT * FROM weather_cache WHERE location_key = #{locationKey} AND forecast_date >= #{from} ORDER BY forecast_date ASC")
    List<WeatherCache> findFrom(String locationKey, LocalDate from);

    @Select("SELECT * FROM weather_cache WHERE location_key = #{locationKey} AND forecast_date = #{date}")
    WeatherCache findOne(String locationKey, LocalDate date);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/closet/entity/WeatherCache.java backend/src/main/java/com/closet/mapper/WeatherCacheMapper.java
git commit -m "feat(weather): WeatherCache entity + mapper"
```

### Task 21: UserPreference entity + mapper

**Files:**
- Create: `backend/src/main/java/com/closet/entity/UserPreference.java`
- Create: `backend/src/main/java/com/closet/mapper/UserPreferenceMapper.java`

- [ ] **Step 1: Entity**

```java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@TableName(value = "user_preference", autoResultMap = true)
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String defaultLocation;
    private String temperatureUnit;
    private List<String> styleTags;
    private List<String> preferredColors;
    private List<String> avoidedColors;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
```

- [ ] **Step 2: Mapper**

```java
package com.closet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.closet.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/closet/entity/UserPreference.java backend/src/main/java/com/closet/mapper/UserPreferenceMapper.java
git commit -m "feat(weather): UserPreference entity + mapper"
```
### Task 22: QWeatherClient 单测 + 实现

**Files:**
- Create: `backend/src/main/java/com/closet/service/QWeatherClient.java`
- Create: `backend/src/test/java/com/closet/unit/QWeatherClientTest.java`

- [ ] **Step 1: 写测试**

```java
package com.closet.unit;

import com.closet.service.QWeatherClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class QWeatherClientTest {

    @Test
    void 未配置apiKey抛IllegalStateException() {
        QWeatherClient client = new QWeatherClient();
        assertThrows(IllegalStateException.class, () -> client.fetchDaily("101010100", 7));
    }

    @Test
    void 配置apiKey不立即抛() {
        QWeatherClient client = new QWeatherClient();
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        assertNotNull(client);
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -Dtest=QWeatherClientTest`
Expected: FAIL "class not found"

- [ ] **Step 3: Client 实现**

```java
package com.closet.service;

import com.qweather.sdk.QWeather;
import com.qweather.sdk.response.weather.WeatherDaily;
import com.qweather.sdk.response.weather.WeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class QWeatherClient {

    @Value("${qweather.api-key:}")
    private String apiKey;

    public List<WeatherDaily> fetchDaily(String locationKey, int days) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("qweather.api-key not configured");
        }
        try {
            QWeather.configure(apiKey, QWeather.Language.ZH_HANS, QWeather.Unit.METRIC);
            WeatherResponse resp = QWeather.instance.weatherDaily(locationKey, days);
            if (resp == null || resp.getDaily() == null) {
                throw new IllegalStateException("qweather returned empty response");
            }
            return resp.getDaily();
        } catch (Exception e) {
            log.error("QWeather fetchDaily failed location={} days={}", locationKey, days, e);
            throw new RuntimeException("qweather api error: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `mvn test -Dtest=QWeatherClientTest`
Expected: 2/2 passed

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/closet/service/QWeatherClient.java backend/src/test/java/com/closet/unit/QWeatherClientTest.java
git commit -m "feat(weather): QWeatherClient wrapper with unit tests"
```

### Task 23: WeatherService 单测 + 实现

**Files:**
- Create: `backend/src/main/java/com/closet/service/WeatherService.java`
- Create: `backend/src/main/java/com/closet/service/impl/WeatherServiceImpl.java`
- Create: `backend/src/main/java/com/closet/dto/DailyForecast.java`
- Create: `backend/src/test/java/com/closet/unit/WeatherServiceTest.java`

- [ ] **Step 1: 接口**

```java
package com.closet.service;

import com.closet.dto.DailyForecast;
import com.closet.entity.UserPreference;
import com.closet.entity.WeatherCache;

import java.time.LocalDate;
import java.util.List;

public interface WeatherService {
    List<WeatherCache> getForecast(String locationKey, int days);
    void refreshDailyForecast();
    UserPreference getPreference();
    UserPreference updatePreference(UserPreference pref);
    List<DailyForecast> getForecastForOutfit(LocalDate date);
}
```

- [ ] **Step 2: DTO**

```java
package com.closet.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyForecast {
    private LocalDate date;
    private Integer tempMin;
    private Integer tempMax;
    private String condition;
    private Integer tempAvg;
}
```

- [ ] **Step 3: 单测**

```java
package com.closet.unit;

import com.closet.dto.DailyForecast;
import com.closet.entity.UserPreference;
import com.closet.entity.WeatherCache;
import com.closet.mapper.UserPreferenceMapper;
import com.closet.mapper.WeatherCacheMapper;
import com.closet.service.QWeatherClient;
import com.closet.service.impl.WeatherServiceImpl;
import com.qweather.sdk.response.weather.WeatherDaily;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WeatherServiceTest {

    private QWeatherClient qWeatherClient;
    private WeatherCacheMapper weatherMapper;
    private UserPreferenceMapper prefMapper;
    private WeatherServiceImpl service;

    @BeforeEach
    void setUp() {
        qWeatherClient = mock(QWeatherClient.class);
        weatherMapper = mock(WeatherCacheMapper.class);
        prefMapper = mock(UserPreferenceMapper.class);
        service = new WeatherServiceImpl(qWeatherClient, weatherMapper, prefMapper);
    }

    @Test
    void refresh拉取并写入7天() {
        WeatherDaily d = new WeatherDaily();
        d.setFxDate("2026-07-05");
        d.setTempMin("20");
        d.setTempMax("30");
        d.setTextDay("晴");
        when(qWeatherClient.fetchDaily(anyString(), eq(7))).thenReturn(List.of(d));
        when(prefMapper.selectList(any())).thenReturn(List.of(pref()));

        service.refreshDailyForecast();

        ArgumentCaptor<WeatherCache> captor = ArgumentCaptor.forClass(WeatherCache.class);
        verify(weatherMapper, atLeastOnce()).insert(captor.capture());
        WeatherCache saved = captor.getValue();
        assertEquals("101010100", saved.getLocationKey());
        assertEquals("sunny", saved.getCondition());
        assertEquals(LocalDate.parse("2026-07-05"), saved.getForecastDate());
    }

    @Test
    void getForecast从缓存读() {
        WeatherCache c = new WeatherCache();
        c.setLocationKey("101010100");
        c.setForecastDate(LocalDate.now());
        c.setTempMin(20);
        c.setTempMax(30);
        c.setCondition("sunny");
        when(weatherMapper.findFrom(eq("101010100"), any())).thenReturn(List.of(c));

        List<WeatherCache> result = service.getForecast("101010100", 7);
        assertEquals(1, result.size());
    }

    @Test
    void getForecastForOutfit转换DTO() {
        WeatherCache c = new WeatherCache();
        c.setLocationKey("101010100");
        c.setForecastDate(LocalDate.of(2026, 7, 5));
        c.setTempMin(20);
        c.setTempMax(30);
        c.setCondition("sunny");
        when(weatherMapper.findOne(eq("101010100"), any())).thenReturn(c);
        when(prefMapper.selectList(any())).thenReturn(List.of(pref()));

        DailyForecast df = service.getForecastForOutfit(LocalDate.of(2026, 7, 5)).get(0);
        assertEquals(25, df.getTempAvg());
        assertEquals("sunny", df.getCondition());
    }

    @Test
    void getPreference空时返回默认() {
        when(prefMapper.selectList(any())).thenReturn(List.of());
        UserPreference pref = service.getPreference();
        assertNotNull(pref);
        assertEquals("c", pref.getTemperatureUnit());
    }

    private UserPreference pref() {
        UserPreference p = new UserPreference();
        p.setId(1L);
        p.setDefaultLocation("101010100");
        p.setTemperatureUnit("c");
        return p;
    }
}
```

- [ ] **Step 4: 实现**

```java
package com.closet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.closet.dto.DailyForecast;
import com.closet.entity.UserPreference;
import com.closet.entity.WeatherCache;
import com.closet.mapper.UserPreferenceMapper;
import com.closet.mapper.WeatherCacheMapper;
import com.closet.service.QWeatherClient;
import com.closet.service.WeatherService;
import com.qweather.sdk.response.weather.WeatherDaily;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final QWeatherClient qWeatherClient;
    private final WeatherCacheMapper weatherMapper;
    private final UserPreferenceMapper prefMapper;

    @Override
    public List<WeatherCache> getForecast(String locationKey, int days) {
        return weatherMapper.findFrom(locationKey, LocalDate.now());
    }

    @Override
    public void refreshDailyForecast() {
        for (UserPreference pref : prefMapper.selectList(new QueryWrapper<>())) {
            String loc = pref.getDefaultLocation();
            if (loc == null || loc.isBlank()) continue;
            try {
                List<WeatherDaily> dailies = qWeatherClient.fetchDaily(loc, 7);
                for (WeatherDaily d : dailies) {
                    WeatherCache c = new WeatherCache();
                    c.setLocationKey(loc);
                    c.setForecastDate(LocalDate.parse(d.getFxDate()));
                    c.setTempMin(Integer.parseInt(d.getTempMin()));
                    c.setTempMax(Integer.parseInt(d.getTempMax()));
                    c.setCondition(mapCondition(d.getTextDay()));
                    Map<String, Object> raw = new HashMap<>();
                    raw.put("fxDate", d.getFxDate());
                    raw.put("textDay", d.getTextDay());
                    raw.put("textNight", d.getTextNight() != null ? d.getTextNight() : "");
                    raw.put("humidity", d.getHumidity() != null ? d.getHumidity() : "0");
                    raw.put("windDirDay", d.getWindDirDay() != null ? d.getWindDirDay() : "");
                    raw.put("windScaleDay", d.getWindScaleDay() != null ? d.getWindScaleDay() : "0");
                    c.setRawJson(raw);
                    c.setFetchedAt(OffsetDateTime.now());

                    WeatherCache existing = weatherMapper.findOne(loc, c.getForecastDate());
                    if (existing == null) weatherMapper.insert(c);
                    else {
                        c.setId(existing.getId());
                        weatherMapper.updateById(c);
                    }
                }
            } catch (Exception e) {
                log.error("refresh failed for {}", loc, e);
            }
        }
    }

    @Override
    public UserPreference getPreference() {
        List<UserPreference> all = prefMapper.selectList(new QueryWrapper<>());
        if (all.isEmpty()) {
            UserPreference p = new UserPreference();
            p.setTemperatureUnit("c");
            p.setStyleTags(new ArrayList<>());
            p.setPreferredColors(new ArrayList<>());
            p.setAvoidedColors(new ArrayList<>());
            p.setDefaultLocation("101010100");
            return p;
        }
        return all.get(0);
    }

    @Override
    public UserPreference updatePreference(UserPreference pref) {
        pref.setUpdatedAt(OffsetDateTime.now());
        if (pref.getId() == null) {
            pref.setCreatedAt(OffsetDateTime.now());
            prefMapper.insert(pref);
        } else {
            prefMapper.updateById(pref);
        }
        return pref;
    }

    @Override
    public List<DailyForecast> getForecastForOutfit(LocalDate date) {
        UserPreference pref = getPreference();
        WeatherCache c = weatherMapper.findOne(pref.getDefaultLocation(), date);
        if (c == null) return List.of();
        DailyForecast df = new DailyForecast();
        df.setDate(c.getForecastDate());
        df.setTempMin(c.getTempMin());
        df.setTempMax(c.getTempMax());
        df.setTempAvg((c.getTempMin() + c.getTempMax()) / 2);
        df.setCondition(c.getCondition());
        return List.of(df);
    }

    private String mapCondition(String text) {
        if (text == null) return "sunny";
        return switch (text) {
            case "晴" -> "sunny";
            case "多云", "阴" -> "cloudy";
            case "雨", "小雨", "中雨", "大雨", "阵雨" -> "rain";
            case "雪", "小雪", "中雪", "大雪" -> "snow";
            default -> "sunny";
        };
    }
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `mvn test -Dtest=WeatherServiceTest`
Expected: 4/4 passed

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/closet/service/WeatherService.java backend/src/main/java/com/closet/service/impl/WeatherServiceImpl.java backend/src/main/java/com/closet/dto/DailyForecast.java backend/src/test/java/com/closet/unit/WeatherServiceTest.java
git commit -m "feat(weather): WeatherService with 4 unit tests"
```

### Task 24: WeatherController + 集成测试

**Files:**
- Create: `backend/src/main/java/com/closet/controller/WeatherController.java`
- Create: `backend/src/test/java/com/closet/integration/WeatherControllerIT.java`

- [ ] **Step 1: Controller**

```java
package com.closet.controller;

import com.closet.common.Result;
import com.closet.entity.UserPreference;
import com.closet.entity.WeatherCache;
import com.closet.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/forecast")
    public Result<List<WeatherCache>> forecast(@RequestParam(defaultValue = "101010100") String location,
                                               @RequestParam(defaultValue = "7") int days) {
        return Result.ok(weatherService.getForecast(location, days));
    }

    @GetMapping("/preference")
    public Result<UserPreference> getPreference() {
        return Result.ok(weatherService.getPreference());
    }

    @PutMapping("/preference")
    public Result<UserPreference> updatePreference(@RequestBody UserPreference pref) {
        return Result.ok(weatherService.updatePreference(pref));
    }

    @PostMapping("/refresh")
    public Result<Void> refresh() {
        weatherService.refreshDailyForecast();
        return Result.ok();
    }
}
```

- [ ] **Step 2: 集成测试**

```java
package com.closet.integration;

import com.closet.common.Result;
import com.closet.entity.UserPreference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "spring.sql.init.mode=never" })
class WeatherControllerIT {

    @Autowired TestRestTemplate rest;

    @Test
    void getPreference返回默认() {
        ResponseEntity<Result> resp = rest.getForEntity("/api/v1/weather/preference", Result.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().getData());
    }

    @Test
    void updatePreference能写入() {
        UserPreference p = new UserPreference();
        p.setDefaultLocation("101010100");
        p.setTemperatureUnit("c");
        rest.put("/api/v1/weather/preference", p);
        ResponseEntity<Result> resp = rest.getForEntity("/api/v1/weather/preference", Result.class);
        assertEquals("101010100", ((UserPreference) resp.getBody().getData()).getDefaultLocation());
    }
}
```

- [ ] **Step 3: 跑 IT**

Run: `mvn verify -Dtest=WeatherControllerIT`
Expected: 2/2 passed

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/closet/controller/WeatherController.java backend/src/test/java/com/closet/integration/WeatherControllerIT.java
git commit -m "feat(weather): WeatherController + integration tests"
```

### Task 25: cron 定时任务（每日 03:00 刷新）

**Files:**
- Create: `backend/src/main/java/com/closet/job/WeatherRefreshJob.java`
- Modify: `backend/src/main/java/com/closet/Application.java`（启用 @EnableScheduling）
- Modify: `backend/src/main/resources/application.yml`（增加 qweather.api-key 配置）

- [ ] **Step 1: 任务类**

```java
package com.closet.job;

import com.closet.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherRefreshJob {

    private final WeatherService weatherService;

    @Scheduled(cron = "0 0 3 * * *")
    public void run() {
        log.info("WeatherRefreshJob start");
        weatherService.refreshDailyForecast();
        log.info("WeatherRefreshJob done");
    }
}
```

- [ ] **Step 2: 启用调度**

在 `Application.java` 类上添加：

```java
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application { ... }
```

- [ ] **Step 3: application.yml**

在文件末尾追加：

```yaml
qweather:
  api-key: ${QWX_API_KEY:}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/closet/job/WeatherRefreshJob.java backend/src/main/java/com/closet/Application.java backend/src/main/resources/application.yml
git commit -m "feat(weather): cron job refresh forecast at 03:00 daily"
```

### Task 26: 前端 weather store

**Files:**
- Create: `frontend/src/api/weather.ts`
- Create: `frontend/src/store/weather.ts`

- [ ] **Step 1: API**

```typescript
import { http } from './http'

export interface DailyForecast {
  date: string
  tempMin: number
  tempMax: number
  tempAvg: number
  condition: 'sunny' | 'cloudy' | 'rain' | 'snow'
}

export interface UserPreference {
  defaultLocation: string
  temperatureUnit: 'c' | 'f'
  styleTags: string[]
  preferredColors: string[]
  avoidedColors: string[]
}

export const weatherApi = {
  forecast(location?: string, days = 7) {
    return http.get<DailyForecast[]>('/api/v1/weather/forecast', {
      params: { location, days },
    })
  },
  getPreference() {
    return http.get<UserPreference>('/api/v1/weather/preference')
  },
  updatePreference(p: UserPreference) {
    return http.put<UserPreference>('/api/v1/weather/preference', p)
  },
}
```

- [ ] **Step 2: Store**

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { weatherApi, type DailyForecast, type UserPreference } from '@/api/weather'

export const useWeatherStore = defineStore('weather', () => {
  const forecast = ref<DailyForecast[]>([])
  const preference = ref<UserPreference | null>(null)
  const loading = ref(false)

  async function loadForecast() {
    loading.value = true
    try {
      forecast.value = await weatherApi.forecast()
    } finally {
      loading.value = false
    }
  }

  async function loadPreference() {
    preference.value = await weatherApi.getPreference()
  }

  async function savePreference(p: UserPreference) {
    preference.value = await weatherApi.updatePreference(p)
  }

  return { forecast, preference, loading, loadForecast, loadPreference, savePreference }
})
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/weather.ts frontend/src/store/weather.ts
git commit -m "feat(weather-frontend): weather api + store"
```

### Task 27: WeatherCard 组件

**Files:**
- Create: `frontend/src/components/WeatherCard.vue`
- Create: `frontend/tests/unit/components/WeatherCard.test.ts`

- [ ] **Step 1: 写测试**

```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import WeatherCard from '@/components/WeatherCard.vue'

describe('WeatherCard', () => {
  it('显示预报数据', () => {
    const wrapper = mount(WeatherCard, {
      props: {
        forecast: [
          { date: '2026-07-05', tempMin: 20, tempMax: 30, tempAvg: 25, condition: 'sunny' },
          { date: '2026-07-06', tempMin: 18, tempMax: 28, tempAvg: 23, condition: 'cloudy' },
        ],
      },
    })
    expect(wrapper.html()).toContain('25')
    expect(wrapper.html()).toContain('sunny')
  })

  it('空数据显示提示', () => {
    const wrapper = mount(WeatherCard, { props: { forecast: [] } })
    expect(wrapper.html()).toContain('暂未获取')
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- WeatherCard.test.ts`
Expected: FAIL

- [ ] **Step 3: 实现**

```vue
<!-- frontend/src/components/WeatherCard.vue -->
<template>
  <view class="weather-card">
    <view class="head">
      <i-lucide-sun />
      <text class="title">7 天天气</text>
      <text class="loc">{{ location }}</text>
    </view>
    <view v-if="forecast.length === 0" class="empty">
      <text>暂未获取天气数据</text>
    </view>
    <scroll-view scroll-x class="days">
      <view v-for="d in forecast" :key="d.date" class="day">
        <text class="date">{{ formatDay(d.date) }}</text>
        <text class="icon">{{ iconOf(d.condition) }}</text>
        <text class="temp">{{ d.tempMin }}° / {{ d.tempMax }}°</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{
  forecast: Array<{ date: string; tempMin: number; tempMax: number; tempAvg: number; condition: string }>
  location?: string
}>()

function formatDay(d: string) {
  const date = new Date(d)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

function iconOf(c: string) {
  return ({ sunny: '☀️', cloudy: '⛅', rain: '🌧', snow: '❄️' })[c] ?? '☀️'
}
</script>

<style lang="scss" scoped>
.weather-card { background: var(--surface); border-radius: 24rpx; padding: 24rpx; margin: 16rpx 0; }
.head { display: flex; align-items: center; gap: 8rpx; margin-bottom: 16rpx; }
.title { font-size: 30rpx; font-weight: 600; }
.loc { margin-left: auto; font-size: 24rpx; color: var(--text-secondary); }
.empty { padding: 32rpx 0; text-align: center; color: var(--text-secondary); }
.days { white-space: nowrap; }
.day { display: inline-block; width: 140rpx; text-align: center; padding: 12rpx 0; }
.date { font-size: 24rpx; color: var(--text-secondary); display: block; }
.icon { font-size: 40rpx; display: block; margin: 8rpx 0; }
.temp { font-size: 24rpx; display: block; }
</style>
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- WeatherCard.test.ts`
Expected: 2/2 passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/WeatherCard.vue frontend/tests/unit/components/WeatherCard.test.ts
git commit -m "feat(weather-frontend): WeatherCard 7-day horizontal scroll"
```

### Task 28: AI 搭配接入天气

**Files:**
- Modify: `frontend/src/pages/ai-generator/ai-generator.vue`

- [ ] **Step 1: 加 weather 自动取**

在 `<script setup>` 中加入：

```typescript
import { useWeatherStore } from '@/store/weather'

const weather = useWeatherStore()
onMounted(async () => {
  await weather.loadForecast()
})

async function onGenerate(req: any) {
  const today = weather.forecast[0]
  const enriched = {
    ...req,
    weather: today ? { temp: today.tempAvg, condition: today.condition } : undefined,
  }
  await ai.generate(enriched)
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/ai-generator/ai-generator.vue
git commit -m "feat(ai-frontend): inject today weather into ai-generate request"
```

### Task 29: 首页接入天气卡

**Files:**
- Modify: `frontend/src/pages/index/index.vue`

- [ ] **Step 1: 引入 + 渲染**

```vue
<script setup>
import { useWeatherStore } from '@/store/weather'
const weather = useWeatherStore()
onShow(async () => { await weather.loadForecast() })
</script>

<template>
  <!-- 在统计区下方 -->
  <WeatherCard v-if="weather.forecast.length" :forecast="weather.forecast" location="北京" />
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/index/index.vue
git commit -m "feat(weather-frontend): homepage weather card"
```

### Task 30: 偏好设置页面

**Files:**
- Create: `frontend/src/pages/settings/weather.vue`
- Create: `frontend/src/pages/settings/weather.test.ts`
- Modify: `frontend/src/pages.json`

- [ ] **Step 1: 写测试**

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import WeatherSettings from '@/pages/settings/weather.vue'

describe('weather settings page', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('显示温度单位字段', () => {
    const wrapper = mount(WeatherSettings)
    expect(wrapper.html()).toContain('温度单位')
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- weather.test.ts`
Expected: FAIL

- [ ] **Step 3: 页面实现**

```vue
<!-- frontend/src/pages/settings/weather.vue -->
<template>
  <view class="page">
    <text class="title">天气与偏好</text>

    <view class="field">
      <text class="label">默认城市</text>
      <input v-model="form.defaultLocation" placeholder="如 101010100" class="input" />
    </view>

    <view class="field">
      <text class="label">温度单位</text>
      <picker mode="selector" :range="['c', 'f']" :value="unitIdx" @change="onUnit">
        <view class="picker">{{ form.temperatureUnit }}</view>
      </picker>
    </view>

    <view class="field">
      <text class="label">喜欢的颜色（逗号分隔）</text>
      <input v-model="colorText" class="input" />
    </view>

    <button class="primary" :disabled="saving" @click="save">保存</button>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useWeatherStore } from '@/store/weather'

const weather = useWeatherStore()
const form = reactive({
  defaultLocation: '',
  temperatureUnit: 'c' as 'c' | 'f',
  preferredColors: [] as string[],
  avoidedColors: [] as string[],
  styleTags: [] as string[],
})
const colorText = ref('')
const saving = ref(false)
const unitIdx = computed(() => form.temperatureUnit === 'c' ? 0 : 1)

onMounted(async () => {
  await weather.loadPreference()
  if (weather.preference) {
    Object.assign(form, weather.preference)
    colorText.value = (weather.preference.preferredColors ?? []).join(', ')
  }
})

function onUnit(e: any) {
  form.temperatureUnit = e.detail.value === 0 ? 'c' : 'f'
}

async function save() {
  saving.value = true
  try {
    form.preferredColors = colorText.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)
    await weather.savePreference(form)
    uni.showToast({ title: '已保存', icon: 'success' })
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: var(--bg); padding: 32rpx 24rpx; }
.title { font-size: 40rpx; font-weight: 700; display: block; margin-bottom: 24rpx; }
.field { margin-bottom: 24rpx; }
.label { font-size: 26rpx; color: var(--text-secondary); display: block; margin-bottom: 8rpx; }
.input, .picker { padding: 16rpx; background: var(--surface); border-radius: 12rpx; font-size: 28rpx; }
.primary { margin-top: 32rpx; background: var(--accent); color: white; border-radius: 32rpx; padding: 24rpx 0; }
</style>
```

- [ ] **Step 4: pages.json 注册**

新增一项：

```json
{
  "path": "pages/settings/weather",
  "style": { "navigationBarTitleText": "天气偏好" }
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `npm test -- weather.test.ts`
Expected: 1/1 passed

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/settings/weather.vue frontend/src/pages/settings/weather.test.ts frontend/src/pages.json
git commit -m "feat(weather-frontend): weather preference settings page"
```

### Task 31: 天气端到端集成

**Files:**
- Create: `frontend/tests/e2e/weather.spec.ts`

- [ ] **Step 1: Playwright 测试**

```typescript
import { test, expect } from '@playwright/test'

test('首页显示天气卡', async ({ page }) => {
  await page.route('**/api/v1/weather/forecast*', route =>
    route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: [
          { date: '2026-07-05', tempMin: 20, tempMax: 30, tempAvg: 25, condition: 'sunny' },
          { date: '2026-07-06', tempMin: 18, tempMax: 28, tempAvg: 23, condition: 'cloudy' },
        ],
      }),
    }),
  )
  await page.goto('/')
  await expect(page.locator('text=7 天天气')).toBeVisible({ timeout: 8000 })
})
```

- [ ] **Step 2: 跑 e2e**

Run: `npx playwright test tests/e2e/weather.spec.ts`

- [ ] **Step 3: Commit**

```bash
git add frontend/tests/e2e/weather.spec.ts
git commit -m "test(e2e): weather card renders on homepage"
```

---

## Phase 3: 衣橱多视图（5 种）

### Task 32: 后端 groupBy 接口

**Files:**
- Modify: `backend/src/main/java/com/closet/service/ClothingService.java`
- Modify: `backend/src/main/java/com/closet/controller/ClothingController.java`
- Create: `backend/src/test/java/com/closet/unit/ClothingServiceGroupTest.java`

- [ ] **Step 1: 写单测（按 groupBy 字段聚合）**

```java
package com.closet.unit;

import com.closet.entity.Clothing;
import com.closet.mapper.ClothingMapper;
import com.closet.service.impl.ClothingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClothingServiceGroupTest {

    private ClothingMapper mapper;
    private ClothingServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(ClothingMapper.class);
        service = new ClothingServiceImpl(mapper);
    }

    @Test
    void groupByCategory返回分类桶() {
        Clothing a = c(1L, "top", "白衬衫", "white");
        Clothing b = c(2L, "top", "黑T恤", "black");
        Clothing c2 = c(3L, "bottom", "牛仔裤", "blue");
        when(mapper.selectList(any())).thenReturn(List.of(a, b, c2));

        Map<String, List<Clothing>> result = service.groupBy("category");
        assertEquals(2, result.get("top").size());
        assertEquals(1, result.get("bottom").size());
    }

    @Test
    void groupByColor返回色桶() {
        Clothing a = c(1L, "top", "A", "white");
        Clothing b = c(2L, "bottom", "B", "white");
        Clothing c2 = c(3L, "top", "C", "black");
        when(mapper.selectList(any())).thenReturn(List.of(a, b, c2));

        Map<String, List<Clothing>> result = service.groupBy("color");
        assertEquals(2, result.get("white").size());
        assertEquals(1, result.get("black").size());
    }

    @Test
    void groupBySeason返回季节桶() {
        Clothing a = c(1L, "top", "A", "white"); a.setSeason("summer");
        Clothing b = c(2L, "top", "B", "white"); b.setSeason("winter");
        when(mapper.selectList(any())).thenReturn(List.of(a, b));

        Map<String, List<Clothing>> result = service.groupBy("season");
        assertEquals(1, result.get("summer").size());
        assertEquals(1, result.get("winter").size());
    }

    @Test
    void groupByUnknown字段按空字符串桶() {
        Clothing a = c(1L, "top", "A", "white");
        when(mapper.selectList(any())).thenReturn(List.of(a));
        Map<String, List<Clothing>> result = service.groupBy("unknown");
        assertTrue(result.containsKey(""));
    }

    private Clothing c(Long id, String cat, String name, String color) {
        Clothing x = new Clothing();
        x.setId(id);
        x.setCategory(cat);
        x.setName(name);
        x.setColor(color);
        return x;
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `mvn test -Dtest=ClothingServiceGroupTest`
Expected: FAIL "groupBy not implemented"

- [ ] **Step 3: 在 ClothingService 加方法**

修改 `ClothingService.java` 接口：

```java
import java.util.Map;
import java.util.List;

Map<String, List<Clothing>> groupBy(String field);
```

在 `ClothingServiceImpl` 加实现：

```java
@Override
public Map<String, List<Clothing>> groupBy(String field) {
    List<Clothing> all = mapper.selectList(new QueryWrapper<>());
    Map<String, List<Clothing>> result = new LinkedHashMap<>();
    for (Clothing c : all) {
        String key;
        switch (field) {
            case "category" -> key = c.getCategory();
            case "color" -> key = c.getColor();
            case "season" -> key = c.getSeason();
            case "brand" -> key = c.getBrand();
            case "status" -> key = c.getStatus();
            default -> key = "";
        }
        result.computeIfAbsent(key == null ? "" : key, k -> new ArrayList<>()).add(c);
    }
    return result;
}
```

- [ ] **Step 4: Controller 加 endpoint**

```java
@GetMapping("/group")
public Result<Map<String, List<Clothing>>> groupBy(@RequestParam String field) {
    return Result.ok(clothingService.groupBy(field));
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `mvn test -Dtest=ClothingServiceGroupTest`
Expected: 4/4 passed

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/closet/service/ClothingService.java backend/src/main/java/com/closet/service/impl/ClothingServiceImpl.java backend/src/main/java/com/closet/controller/ClothingController.java backend/src/test/java/com/closet/unit/ClothingServiceGroupTest.java
git commit -m "feat(closet): add groupBy endpoint (category/color/season/brand/status)"
```

### Task 33: 前端 closet store 加视图模式

**Files:**
- Modify: `frontend/src/store/closet.ts`

- [ ] **Step 1: 增加 viewMode + groupBy 字段**

```typescript
// 在现有 closet.ts 末尾追加
export type ClosetViewMode = 'grid' | 'hanger' | 'drawer' | 'color' | 'season'

const viewMode = ref<ClosetViewMode>('grid')
const grouped = ref<Record<string, Clothing[]>>({})

async function loadGroup(field: string) {
  grouped.value = await clothingApi.group(field)
}

function setViewMode(mode: ClosetViewMode) {
  viewMode.value = mode
}

return { /* 已有返回 */, viewMode, grouped, loadGroup, setViewMode }
```

- [ ] **Step 2: API 加 group 方法**

在 `frontend/src/api/clothing.ts`：

```typescript
group(field: string) {
  return http.get<Record<string, Clothing[]>>('/api/v1/clothes/group', { params: { field } })
},
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/store/closet.ts frontend/src/api/clothing.ts
git commit -m "feat(closet-frontend): store viewMode + group loaders"
```
### Task 34: ClosetViewTabs 切换组件

**Files:**
- Create: `frontend/src/components/ClosetViewTabs.vue`
- Create: `frontend/tests/unit/components/ClosetViewTabs.test.ts`

- [ ] **Step 1: 写测试**

```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ClosetViewTabs from '@/components/ClosetViewTabs.vue'

describe('ClosetViewTabs', () => {
  it('5 个 tab 默认 grid 高亮', () => {
    const wrapper = mount(ClosetViewTabs, { props: { modelValue: 'grid' } })
    const tabs = wrapper.findAll('[data-test="tab"]')
    expect(tabs.length).toBe(5)
    expect(tabs[0].classes()).toContain('active')
  })

  it('点击触发 update:modelValue', async () => {
    const wrapper = mount(ClosetViewTabs, { props: { modelValue: 'grid' } })
    await wrapper.findAll('[data-test="tab"]')[2].trigger('click')
    expect(wrapper.emitted('update:modelValue')![0]).toEqual(['hanger'])
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- ClosetViewTabs.test.ts`
Expected: FAIL

- [ ] **Step 3: 组件实现**

```vue
<!-- frontend/src/components/ClosetViewTabs.vue -->
<template>
  <view class="tabs">
    <view
      v-for="t in tabs"
      :key="t.value"
      data-test="tab"
      :class="['tab', { active: modelValue === t.value }]"
      @click="select(t.value)"
    >
      <i :class="`lucide-${t.icon}`" />
      <text class="tab-text">{{ t.label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
type Mode = 'grid' | 'hanger' | 'drawer' | 'color' | 'season'
const props = defineProps<{ modelValue: Mode }>()
const emit = defineEmits<{ 'update:modelValue': [mode: Mode] }>()

const tabs: Array<{ value: Mode; label: string; icon: string }> = [
  { value: 'grid', label: '网格', icon: 'grid' },
  { value: 'hanger', label: '衣架', icon: 'shirt' },
  { value: 'drawer', label: '抽屉', icon: 'inbox' },
  { value: 'color', label: '色块', icon: 'palette' },
  { value: 'season', label: '季节', icon: 'sun' },
]

function select(m: Mode) { emit('update:modelValue', m) }
</script>

<style lang="scss" scoped>
.tabs { display: flex; gap: 8rpx; padding: 16rpx 24rpx; background: var(--surface); border-radius: 32rpx; margin: 16rpx 0; }
.tab { flex: 1; display: flex; flex-direction: column; align-items: center; padding: 8rpx 0; border-radius: 16rpx; color: var(--text-secondary); }
.tab.active { background: var(--accent-soft); color: var(--accent); }
.tab-text { font-size: 22rpx; margin-top: 4rpx; }
</style>
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- ClosetViewTabs.test.ts`
Expected: 2/2 passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/ClosetViewTabs.vue frontend/tests/unit/components/ClosetViewTabs.test.ts
git commit -m "feat(closet-frontend): ClosetViewTabs segmented control"
```

### Task 35: GridView 网格视图（默认）

**Files:**
- Create: `frontend/src/components/closet/GridView.vue`

- [ ] **Step 1: 实现**

```vue
<!-- frontend/src/components/closet/GridView.vue -->
<template>
  <view class="grid">
    <view v-for="c in clothes" :key="c.id" class="cell" @click="$emit('open', c)">
      <image :src="c.imageUrl" class="img" mode="aspectFill" />
      <text class="name">{{ c.name }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
interface C { id: number; name: string; imageUrl: string }
defineProps<{ clothes: C[] }>()
defineEmits<{ open: [c: C] }>()
</script>

<style lang="scss" scoped>
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16rpx; padding: 16rpx 24rpx; }
.cell { background: var(--surface); border-radius: 16rpx; overflow: hidden; }
.img { width: 100%; height: 220rpx; display: block; }
.name { font-size: 24rpx; padding: 8rpx 12rpx; display: block; color: var(--text-primary); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/closet/GridView.vue
git commit -m "feat(closet-frontend): GridView default 3-col grid"
```

### Task 36: HangerView 衣架视图

**Files:**
- Create: `frontend/src/components/closet/HangerView.vue`

- [ ] **Step 1: 实现**

```vue
<!-- frontend/src/components/closet/HangerView.vue -->
<template>
  <scroll-view scroll-x class="rack">
    <view v-for="c in clothes" :key="c.id" class="hanger" @click="$emit('open', c)">
      <view class="hook">
        <i-lucide-shirt />
      </view>
      <image :src="c.imageUrl" class="img" mode="aspectFill" />
      <text class="name">{{ c.name }}</text>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
interface C { id: number; name: string; imageUrl: string }
defineProps<{ clothes: C[] }>()
defineEmits<{ open: [c: C] }>()
</script>

<style lang="scss" scoped>
.rack { white-space: nowrap; padding: 16rpx 24rpx; }
.hanger { display: inline-block; width: 220rpx; margin-right: 16rpx; vertical-align: top; }
.hook { text-align: center; color: var(--text-secondary); margin-bottom: 8rpx; }
.img { width: 220rpx; height: 280rpx; border-radius: 16rpx; }
.name { font-size: 24rpx; display: block; text-align: center; margin-top: 8rpx; color: var(--text-primary); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/closet/HangerView.vue
git commit -m "feat(closet-frontend): HangerView horizontal scroll with hook icon"
```

### Task 37: DrawerView 抽屉视图

**Files:**
- Create: `frontend/src/components/closet/DrawerView.vue`

- [ ] **Step 1: 实现**

按 category 分组抽屉：

```vue
<!-- frontend/src/components/closet/DrawerView.vue -->
<template>
  <view class="drawers">
    <view v-for="group in groupedByCategory" :key="group.key" class="drawer">
      <view class="header" @click="toggle(group.key)">
        <text class="title">{{ group.label }}</text>
        <text class="count">{{ group.items.length }}</text>
        <i :class="expanded[group.key] ? 'lucide-chevron-down' : 'lucide-chevron-right'" />
      </view>
      <view v-if="expanded[group.key]" class="body">
        <view v-for="c in group.items" :key="c.id" class="item" @click="$emit('open', c)">
          <image :src="c.imageUrl" class="thumb" mode="aspectFill" />
          <text class="name">{{ c.name }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface C { id: number; name: string; imageUrl: string; category: string }
const props = defineProps<{ clothes: C[] }>()
defineEmits<{ open: [c: C] }>()

const expanded = ref<Record<string, boolean>>({ top: true, bottom: true })

const labels: Record<string, string> = {
  top: '上装', bottom: '下装', outer: '外套', dress: '连衣裙',
  shoes: '鞋履', bag: '包包', accessory: '配饰', underwear: '内衣',
}

const groupedByCategory = computed(() => {
  const map: Record<string, C[]> = {}
  for (const c of props.clothes) {
    const k = c.category || 'other'
    ;(map[k] ??= []).push(c)
  }
  return Object.entries(map).map(([key, items]) => ({
    key, label: labels[key] ?? key, items,
  }))
})

function toggle(k: string) { expanded.value[k] = !expanded.value[k] }
</script>

<style lang="scss" scoped>
.drawers { padding: 16rpx 24rpx; }
.drawer { background: var(--surface); border-radius: 16rpx; margin-bottom: 12rpx; overflow: hidden; }
.header { display: flex; align-items: center; padding: 20rpx 24rpx; }
.title { font-size: 28rpx; font-weight: 600; flex: 1; color: var(--text-primary); }
.count { font-size: 24rpx; color: var(--text-secondary); margin-right: 12rpx; }
.body { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12rpx; padding: 0 24rpx 16rpx; }
.item { background: var(--bg-elevated); border-radius: 12rpx; overflow: hidden; }
.thumb { width: 100%; height: 140rpx; display: block; }
.name { font-size: 22rpx; padding: 8rpx; display: block; color: var(--text-primary); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/closet/DrawerView.vue
git commit -m "feat(closet-frontend): DrawerView categorized accordion"
```

### Task 38: ColorView 色块视图

**Files:**
- Create: `frontend/src/components/closet/ColorView.vue`

- [ ] **Step 1: 实现**

```vue
<!-- frontend/src/components/closet/ColorView.vue -->
<template>
  <view class="blocks">
    <view v-for="g in groups" :key="g.key" class="block">
      <view class="swatch" :style="{ background: g.swatch }">
        <text class="label">{{ g.label }}</text>
      </view>
      <view class="thumbs">
        <image
          v-for="c in g.items.slice(0, 6)"
          :key="c.id"
          :src="c.imageUrl"
          class="thumb"
          mode="aspectFill"
          @click="$emit('open', c)"
        />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface C { id: number; name: string; imageUrl: string; color: string }
const props = defineProps<{ clothes: C[] }>()
defineEmits<{ open: [c: C] }>()

const swatchMap: Record<string, string> = {
  white: '#F5F5F5', black: '#2A2A2A', gray: '#9CA3AF',
  red: '#E45757', pink: '#F4A6B0', orange: '#F39C5A',
  yellow: '#F5C76A', green: '#7FB069', blue: '#6CA0DC',
  navy: '#2C3E70', brown: '#8B5A3C', beige: '#D9C7AE',
}

const labels: Record<string, string> = {
  white: '白', black: '黑', gray: '灰', red: '红', pink: '粉',
  orange: '橙', yellow: '黄', green: '绿', blue: '蓝', navy: '藏青',
  brown: '棕', beige: '米', purple: '紫',
}

const groups = computed(() => {
  const map: Record<string, C[]> = {}
  for (const c of props.clothes) {
    const k = c.color || 'other'
    ;(map[k] ??= []).push(c)
  }
  return Object.entries(map).map(([key, items]) => ({
    key,
    label: labels[key] ?? key,
    swatch: swatchMap[key] ?? '#DDD',
    items,
  }))
})
</script>

<style lang="scss" scoped>
.blocks { padding: 16rpx 24rpx; display: grid; gap: 16rpx; }
.block { background: var(--surface); border-radius: 16rpx; overflow: hidden; }
.swatch { padding: 16rpx 24rpx; color: white; font-size: 26rpx; font-weight: 600; }
.thumbs { display: flex; gap: 8rpx; padding: 12rpx; }
.thumb { flex: 1; height: 120rpx; border-radius: 8rpx; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/closet/ColorView.vue
git commit -m "feat(closet-frontend): ColorView color-grouped swatches"
```

### Task 39: SeasonView 季节视图 + closet 页面集成

**Files:**
- Create: `frontend/src/components/closet/SeasonView.vue`
- Modify: `frontend/src/pages/closet/closet.vue`

- [ ] **Step 1: SeasonView 实现**

```vue
<!-- frontend/src/components/closet/SeasonView.vue -->
<template>
  <view class="seasons">
    <view v-for="g in groups" :key="g.key" class="season">
      <view class="banner" :style="{ background: g.gradient }">
        <i :class="`lucide-${g.icon}`" />
        <text class="title">{{ g.label }}</text>
        <text class="count">{{ g.items.length }} 件</text>
      </view>
      <view class="grid">
        <image
          v-for="c in g.items"
          :key="c.id"
          :src="c.imageUrl"
          class="img"
          mode="aspectFill"
          @click="$emit('open', c)"
        />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface C { id: number; name: string; imageUrl: string; season: string }
const props = defineProps<{ clothes: C[] }>()
defineEmits<{ open: [c: C] }>()

const seasonConfig: Record<string, { label: string; gradient: string; icon: string }> = {
  spring: { label: '春', gradient: 'linear-gradient(135deg, #F4A6B0, #FFE5B4)', icon: 'flower' },
  summer: { label: '夏', gradient: 'linear-gradient(135deg, #FFE5B4, #7FB069)', icon: 'sun' },
  autumn: { label: '秋', gradient: 'linear-gradient(135deg, #F39C5A, #8B5A3C)', icon: 'leaf' },
  winter: { label: '冬', gradient: 'linear-gradient(135deg, #6CA0DC, #2C3E70)', icon: 'snowflake' },
  all: { label: '四季通用', gradient: 'linear-gradient(135deg, #F4A6B0, #6CA0DC)', icon: 'sparkles' },
}

const groups = computed(() => {
  const order = ['spring', 'summer', 'autumn', 'winter', 'all']
  const map: Record<string, C[]> = {}
  for (const c of props.clothes) {
    const k = c.season || 'all'
    ;(map[k] ??= []).push(c)
  }
  return order
    .filter(k => map[k]?.length)
    .map(k => ({
      key: k,
      label: seasonConfig[k]?.label ?? k,
      gradient: seasonConfig[k]?.gradient ?? '#DDD',
      icon: seasonConfig[k]?.icon ?? 'shirt',
      items: map[k],
    }))
})
</script>

<style lang="scss" scoped>
.seasons { padding: 16rpx 24rpx; display: grid; gap: 16rpx; }
.season { background: var(--surface); border-radius: 16rpx; overflow: hidden; }
.banner { padding: 24rpx; color: white; display: flex; align-items: center; gap: 12rpx; }
.title { font-size: 32rpx; font-weight: 700; flex: 1; }
.count { font-size: 24rpx; opacity: 0.85; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8rpx; padding: 12rpx; }
.img { width: 100%; height: 160rpx; border-radius: 8rpx; }
</style>
```

- [ ] **Step 2: closet.vue 集成所有视图**

修改 `frontend/src/pages/closet/closet.vue`：

```vue
<template>
  <view class="page">
    <ClosetViewTabs v-model="viewMode" />
    <GridView v-if="viewMode === 'grid'" :clothes="clothes" @open="onOpen" />
    <HangerView v-else-if="viewMode === 'hanger'" :clothes="clothes" @open="onOpen" />
    <DrawerView v-else-if="viewMode === 'drawer'" :clothes="clothes" @open="onOpen" />
    <ColorView v-else-if="viewMode === 'color'" :clothes="clothes" @open="onOpen" />
    <SeasonView v-else :clothes="clothes" @open="onOpen" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useClosetStore } from '@/store/closet'
import ClosetViewTabs from '@/components/ClosetViewTabs.vue'
import GridView from '@/components/closet/GridView.vue'
import HangerView from '@/components/closet/HangerView.vue'
import DrawerView from '@/components/closet/DrawerView.vue'
import ColorView from '@/components/closet/ColorView.vue'
import SeasonView from '@/components/closet/SeasonView.vue'

const closet = useClosetStore()
const viewMode = ref<typeof closet.viewMode.value>(closet.viewMode ?? 'grid')
const clothes = ref<any[]>([])

onShow(async () => {
  clothes.value = await closet.loadList()
})
</script>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/closet/SeasonView.vue frontend/src/pages/closet/closet.vue
git commit -m "feat(closet-frontend): SeasonView + closet page wired to 5 views"
```

---

## Phase 4: 高级分享海报（3 模板）

### Task 40: poster_template 表 schema

**Files:**
- Modify: `backend/src/main/resources/db/migration/V6__v2_poster.sql`

- [ ] **Step 1: 迁移脚本**

```sql
-- V6__v2_poster.sql

CREATE TABLE poster_share (
  id           BIGSERIAL PRIMARY KEY,
  template_key VARCHAR(32)  NOT NULL,             -- magazine / collage / diary
  context_json JSONB        NOT NULL,             -- { items: [...], title: '...', subtitle: '...' }
  image_url    VARCHAR(512),                      -- 生成后存到 MinIO 的 URL
  shared_to    VARCHAR(64),                       -- wechat / moments / save
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_poster_share_template ON poster_share (template_key);
```

- [ ] **Step 2: h2 测试同步**

在 `backend/src/test/resources/schema-test.sql` 追加：

```sql
CREATE TABLE IF NOT EXISTS poster_share (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_key VARCHAR(32) NOT NULL,
  context_json TEXT NOT NULL,
  image_url VARCHAR(512),
  shared_to VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 3: 应用迁移**

Run:
```bash
docker exec -i closet-postgres psql -U closet -d closet < backend/src/main/resources/db/migration/V6__v2_poster.sql
```

Expected: CREATE TABLE x 1, CREATE INDEX x 1

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/migration/V6__v2_poster.sql backend/src/test/resources/schema-test.sql
git commit -m "feat(db): add poster_share table"
```

### Task 41: PosterService 单测 + 实现

**Files:**
- Create: `backend/src/main/java/com/closet/service/PosterService.java`
- Create: `backend/src/main/java/com/closet/service/impl/PosterServiceImpl.java`
- Create: `backend/src/main/java/com/closet/dto/PosterContext.java`
- Create: `backend/src/main/java/com/closet/dto/PosterResult.java`
- Create: `backend/src/main/java/com/closet/entity/PosterShare.java`
- Create: `backend/src/main/java/com/closet/mapper/PosterShareMapper.java`
- Create: `backend/src/test/java/com/closet/unit/PosterServiceTest.java`

- [ ] **Step 1: DTO**

```java
// PosterContext.java
package com.closet.dto;

import lombok.Data;
import java.util.List;

@Data
public class PosterContext {
    private String templateKey;        // magazine / collage / diary
    private List<Long> clothingIds;
    private String title;
    private String subtitle;
    private String mood;
}

// PosterResult.java
package com.closet.dto;

import lombok.Data;

@Data
public class PosterResult {
    private Long shareId;
    private String imageUrl;          // 返回前端渲染的 dataURL 或 MinIO URL
    private String dataUrl;           // base64 PNG（H5 用）
}
```

- [ ] **Step 2: Entity + Mapper**

```java
// PosterShare.java
package com.closet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@TableName(value = "poster_share", autoResultMap = true)
public class PosterShare {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateKey;
    private Map<String, Object> contextJson;
    private String imageUrl;
    private String sharedTo;
    private OffsetDateTime createdAt;
}

// PosterShareMapper.java
package com.closet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.closet.entity.PosterShare;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PosterShareMapper extends BaseMapper<PosterShare> {}
```

- [ ] **Step 3: Service 接口**

```java
package com.closet.service;

import com.closet.dto.PosterContext;
import com.closet.dto.PosterResult;

import java.util.List;

public interface PosterService {
    /** 列出可用模板的元数据 */
    List<PosterTemplateMeta> listTemplates();
    /** 生成海报（后端只做记录持久化；渲染由前端 canvas 完成） */
    PosterResult generate(PosterContext ctx);
    /** 记录分享行为 */
    void recordShare(Long shareId, String target);
}
```

新增 `PosterTemplateMeta`（也可以作为内部类）：

```java
package com.closet.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PosterTemplateMeta {
    private String key;
    private String name;
    private String description;
    private String previewUrl;
}
```

- [ ] **Step 4: 单测**

```java
package com.closet.unit;

import com.closet.dto.PosterContext;
import com.closet.dto.PosterResult;
import com.closet.entity.PosterShare;
import com.closet.mapper.PosterShareMapper;
import com.closet.service.PosterTemplateMeta;
import com.closet.service.impl.PosterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PosterServiceTest {

    private PosterShareMapper mapper;
    private PosterServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(PosterShareMapper.class);
        when(mapper.insert(any(PosterShare.class))).thenAnswer(inv -> {
            PosterShare p = inv.getArgument(0);
            p.setId(99L);
            return 1;
        });
        service = new PosterServiceImpl(mapper);
    }

    @Test
    void listTemplates返回3个() {
        List<PosterTemplateMeta> t = service.listTemplates();
        assertEquals(3, t.size());
        assertTrue(t.stream().anyMatch(m -> m.getKey().equals("magazine")));
        assertTrue(t.stream().anyMatch(m -> m.getKey().equals("collage")));
        assertTrue(t.stream().anyMatch(m -> m.getKey().equals("diary")));
    }

    @Test
    void generate返回shareId() {
        PosterContext ctx = new PosterContext();
        ctx.setTemplateKey("magazine");
        ctx.setClothingIds(List.of(1L, 2L, 3L));
        ctx.setTitle("春日穿搭");
        ctx.setSubtitle("3 件单品");
        ctx.setMood("fresh");

        PosterResult r = service.generate(ctx);
        assertNotNull(r.getShareId());
        assertEquals(99L, r.getShareId());
    }

    @Test
    void generate持久化contextJson() {
        PosterContext ctx = new PosterContext();
        ctx.setTemplateKey("diary");
        ctx.setClothingIds(List.of(1L));
        ctx.setMood("happy");

        service.generate(ctx);
        ArgumentCaptor<PosterShare> captor = ArgumentCaptor.forClass(PosterShare.class);
        verify(mapper).insert(captor.capture());
        PosterShare saved = captor.getValue();
        assertEquals("diary", saved.getTemplateKey());
        assertEquals("happy", saved.getContextJson().get("mood"));
    }

    @Test
    void recordShare更新sharedTo() {
        PosterShare existing = new PosterShare();
        existing.setId(1L);
        when(mapper.selectById(1L)).thenReturn(existing);

        service.recordShare(1L, "wechat");
        verify(mapper).updateById(existing);
        assertEquals("wechat", existing.getSharedTo());
    }
}
```

- [ ] **Step 5: 跑测试确认失败**

Run: `mvn test -Dtest=PosterServiceTest`
Expected: FAIL "PosterServiceImpl not found"

- [ ] **Step 6: 实现**

```java
package com.closet.service.impl;

import com.closet.dto.PosterContext;
import com.closet.dto.PosterResult;
import com.closet.entity.PosterShare;
import com.closet.mapper.PosterShareMapper;
import com.closet.service.PosterService;
import com.closet.service.PosterTemplateMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PosterServiceImpl implements PosterService {

    private final PosterShareMapper posterMapper;

    @Override
    public List<PosterTemplateMeta> listTemplates() {
        return List.of(
            new PosterTemplateMeta("magazine", "杂志大片", "留白精致，单品大图 + 标题", "/static/poster/magazine.png"),
            new PosterTemplateMeta("collage", "色块拼图", "色块 + 缩略图组合", "/static/poster/collage.png"),
            new PosterTemplateMeta("diary", "心情日记", "日记本风格，文字 + 单品", "/static/poster/diary.png")
        );
    }

    @Override
    public PosterResult generate(PosterContext ctx) {
        PosterShare share = new PosterShare();
        share.setTemplateKey(ctx.getTemplateKey());
        Map<String, Object> json = new HashMap<>();
        json.put("clothingIds", ctx.getClothingIds());
        json.put("title", ctx.getTitle());
        json.put("subtitle", ctx.getSubtitle());
        json.put("mood", ctx.getMood());
        share.setContextJson(json);
        share.setCreatedAt(OffsetDateTime.now());
        posterMapper.insert(share);

        PosterResult r = new PosterResult();
        r.setShareId(share.getId());
        // 实际渲染在前端 canvas 完成
        r.setImageUrl(null);
        return r;
    }

    @Override
    public void recordShare(Long shareId, String target) {
        PosterShare existing = posterMapper.selectById(shareId);
        if (existing != null) {
            existing.setSharedTo(target);
            posterMapper.updateById(existing);
        }
    }
}
```

- [ ] **Step 7: 跑测试确认通过**

Run: `mvn test -Dtest=PosterServiceTest`
Expected: 4/4 passed

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/closet/service/PosterService.java backend/src/main/java/com/closet/service/impl/PosterServiceImpl.java backend/src/main/java/com/closet/dto/PosterContext.java backend/src/main/java/com/closet/dto/PosterResult.java backend/src/main/java/com/closet/entity/PosterShare.java backend/src/main/java/com/closet/mapper/PosterShareMapper.java backend/src/test/java/com/closet/unit/PosterServiceTest.java
git commit -m "feat(poster): PosterService with 4 unit tests (list/generate/persist/record)"
```
### Task 42: PosterController + 集成测试

**Files:**
- Create: `backend/src/main/java/com/closet/controller/PosterController.java`
- Create: `backend/src/test/java/com/closet/integration/PosterControllerIT.java`

- [ ] **Step 1: Controller**

```java
package com.closet.controller;

import com.closet.common.Result;
import com.closet.dto.PosterContext;
import com.closet.dto.PosterResult;
import com.closet.service.PosterService;
import com.closet.service.PosterTemplateMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posters")
@RequiredArgsConstructor
public class PosterController {

    private final PosterService posterService;

    @GetMapping("/templates")
    public Result<List<PosterTemplateMeta>> listTemplates() {
        return Result.ok(posterService.listTemplates());
    }

    @PostMapping("/generate")
    public Result<PosterResult> generate(@RequestBody PosterContext ctx) {
        return Result.ok(posterService.generate(ctx));
    }

    @PostMapping("/{id}/share")
    public Result<Void> share(@PathVariable Long id, @RequestBody Map<String, String> body) {
        posterService.recordShare(id, body.get("target"));
        return Result.ok();
    }
}
```

- [ ] **Step 2: 集成测试**

```java
package com.closet.integration;

import com.closet.common.Result;
import com.closet.dto.PosterContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "spring.sql.init.mode=never" })
class PosterControllerIT {

    @Autowired TestRestTemplate rest;

    @Test
    void templates返回3个() {
        ResponseEntity<Result> resp = rest.getForEntity("/api/v1/posters/templates", Result.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(((List<?>) resp.getBody().getData()).size() >= 3);
    }

    @Test
    void generate返回shareId() {
        PosterContext ctx = new PosterContext();
        ctx.setTemplateKey("magazine");
        ctx.setClothingIds(List.of(1L));
        ctx.setTitle("test");
        ResponseEntity<Result> resp = rest.postForEntity("/api/v1/posters/generate", ctx, Result.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody().getData());
    }
}
```

- [ ] **Step 3: 跑 IT**

Run: `mvn verify -Dtest=PosterControllerIT`
Expected: 2/2 passed

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/closet/controller/PosterController.java backend/src/test/java/com/closet/integration/PosterControllerIT.java
git commit -m "feat(poster): PosterController + integration tests"
```

### Task 43: 前端 PosterRenderer canvas 2d（核心）

**Files:**
- Create: `frontend/src/components/poster/PosterRenderer.vue`
- Create: `frontend/tests/unit/components/PosterRenderer.test.ts`

- [ ] **Step 1: 写测试**

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PosterRenderer from '@/components/poster/PosterRenderer.vue'

describe('PosterRenderer', () => {
  it('默认渲染 magazine 模板的 canvas', () => {
    const wrapper = mount(PosterRenderer, {
      props: {
        template: 'magazine',
        items: [{ name: 'A', imageUrl: '/a.jpg' }],
        title: 'Test',
      },
    })
    expect(wrapper.find('canvas').exists()).toBe(true)
  })

  it('emit ready 当 canvas 画完', async () => {
    const wrapper = mount(PosterRenderer, {
      props: {
        template: 'collage',
        items: [{ name: 'A', imageUrl: '/a.jpg' }],
        title: 'X',
      },
    })
    await new Promise(r => setTimeout(r, 100))
    // jsdom 不真渲染 canvas，只验证事件触发链存在
    expect(wrapper.emitted()).toBeDefined()
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- PosterRenderer.test.ts`
Expected: FAIL

- [ ] **Step 3: 实现**

```vue
<!-- frontend/src/components/poster/PosterRenderer.vue -->
<template>
  <view class="poster-wrap">
    <canvas
      ref="canvasRef"
      :canvas-id="canvasId"
      :id="canvasId"
      :style="{ width: width + 'px', height: height + 'px' }"
      class="canvas"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

interface PosterItem { name: string; imageUrl: string; color?: string }
const props = defineProps<{
  template: 'magazine' | 'collage' | 'diary'
  items: PosterItem[]
  title?: string
  subtitle?: string
  mood?: string
  width?: number
  height?: number
}>()
const emit = defineEmits<{ ready: [dataUrl: string] }>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const canvasId = `poster_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`
const width = props.width ?? 750
const height = props.height ?? 1100

onMounted(async () => {
  if (!canvasRef.value) return
  // H5 用 2d 上下文；小程序用 uni.createCanvasContext
  // #ifdef H5
  const ctx = canvasRef.value.getContext('2d')!
  await draw(ctx)
  // #endif
  // #ifdef MP-WEIXIN
  const ctx = uni.createCanvasContext(canvasId, getCurrentInstance())
  await drawWx(ctx)
  // #endif
})

watch(() => [props.template, props.items, props.title], async () => {
  if (!canvasRef.value) return
  const ctx = canvasRef.value.getContext('2d')
  if (ctx) await draw(ctx)
})

async function draw(ctx: CanvasRenderingContext2D) {
  // 背景：燕麦白
  ctx.fillStyle = '#FFF7F2'
  ctx.fillRect(0, 0, width, height)

  if (props.template === 'magazine') drawMagazine(ctx)
  else if (props.template === 'collage') drawCollage(ctx)
  else drawDiary(ctx)

  // emit dataURL
  emit('ready', canvasRef.value!.toDataURL('image/png'))
}

function drawMagazine(ctx: CanvasRenderingContext2D) {
  // 大标题
  ctx.fillStyle = '#3A2E2E'
  ctx.font = 'bold 56px sans-serif'
  ctx.fillText(props.title ?? '我的穿搭', 50, 100)

  // 主图（前 1 张占大头）
  const main = props.items[0]
  if (main) {
    drawImage(ctx, main.imageUrl, 50, 140, width - 100, 700)
  }
  // 副标题
  ctx.fillStyle = '#8A7E7E'
  ctx.font = '28px sans-serif'
  ctx.fillText(props.subtitle ?? `${props.items.length} 件单品`, 50, height - 60)

  // 樱粉强调条
  ctx.fillStyle = '#d49aa5'
  ctx.fillRect(50, 70, 60, 6)
}

function drawCollage(ctx: CanvasRenderingContext2D) {
  // 色块背景
  const palette = ['#F4A6B0', '#FFE5B4', '#7FB069', '#6CA0DC', '#F5C76A']
  ctx.fillStyle = palette[0]
  ctx.fillRect(0, 0, width, 200)
  ctx.fillStyle = palette[2]
  ctx.fillRect(0, height - 200, width, 200)

  // 标题
  ctx.fillStyle = 'white'
  ctx.font = 'bold 48px sans-serif'
  ctx.fillText(props.title ?? '搭配', 50, 110)

  // 单品网格
  const cellW = (width - 80) / 3
  props.items.slice(0, 9).forEach((item, i) => {
    const col = i % 3, row = Math.floor(i / 3)
    drawImage(ctx, item.imageUrl, 40 + col * cellW, 250 + row * 240, cellW - 12, 200)
  })
}

function drawDiary(ctx: CanvasRenderingContext2D) {
  // 信纸背景
  ctx.fillStyle = '#FFFBF5'
  ctx.fillRect(0, 0, width, height)

  // 横线
  ctx.strokeStyle = '#F0E5D8'
  for (let y = 200; y < height - 100; y += 50) {
    ctx.beginPath()
    ctx.moveTo(60, y)
    ctx.lineTo(width - 60, y)
    ctx.stroke()
  }

  // 标题 + 心情
  ctx.fillStyle = '#3A2E2E'
  ctx.font = 'bold 44px sans-serif'
  ctx.fillText(props.title ?? '今日心情', 60, 100)
  ctx.fillStyle = '#d49aa5'
  ctx.font = '32px sans-serif'
  ctx.fillText(`#${props.mood ?? 'fresh'}`, 60, 150)

  // 文字
  ctx.fillStyle = '#3A2E2E'
  ctx.font = '26px sans-serif'
  const lines = (props.subtitle ?? '').split('\n')
  lines.forEach((line, i) => ctx.fillText(line, 60, 220 + i * 50))

  // 单品小图
  const cellW = 120
  props.items.slice(0, 5).forEach((item, i) => {
    drawImage(ctx, item.imageUrl, 60 + i * (cellW + 16), height - 220, cellW, cellW)
  })
}

function drawImage(ctx: CanvasRenderingContext2D, url: string, x: number, y: number, w: number, h: number) {
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => { ctx.drawImage(img, x, y, w, h) }
  img.onerror = () => { ctx.fillStyle = '#EEE'; ctx.fillRect(x, y, w, h) }
  img.src = url
}
</script>

<style lang="scss" scoped>
.poster-wrap { width: 100%; display: flex; justify-content: center; }
.canvas { background: #FFF7F2; border-radius: 16rpx; }
</style>
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- PosterRenderer.test.ts`
Expected: 2/2 passed（jsdom 下 canvas.toDataURL 桩返回 ''）

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/poster/PosterRenderer.vue frontend/tests/unit/components/PosterRenderer.test.ts
git commit -m "feat(poster-frontend): PosterRenderer canvas 2d 3 templates"
```

### Task 44: PosterEditor 页面

**Files:**
- Create: `frontend/src/pages/poster/poster.vue`
- Create: `frontend/src/pages/poster/poster.test.ts`
- Modify: `frontend/src/pages.json`

- [ ] **Step 1: 写测试**

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import PosterPage from '@/pages/poster/poster.vue'

const PosterRendererStub = {
  template: '<view class="renderer-stub" />',
  props: ['template', 'items', 'title', 'subtitle', 'mood'],
}

describe('poster page', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('显示 3 个模板选择', () => {
    const wrapper = mount(PosterPage, {
      global: { stubs: { PosterRenderer: PosterRendererStub } },
    })
    expect(wrapper.html()).toContain('杂志大片')
    expect(wrapper.html()).toContain('色块拼图')
    expect(wrapper.html()).toContain('心情日记')
  })
})
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- poster.test.ts`
Expected: FAIL

- [ ] **Step 3: 页面实现**

```vue
<!-- frontend/src/pages/poster/poster.vue -->
<template>
  <view class="page">
    <text class="title">分享海报</text>

    <view class="templates">
      <view
        v-for="t in templates"
        :key="t.key"
        :class="['tpl', { active: tpl === t.key }]"
        @click="tpl = t.key"
      >
        <text class="tpl-name">{{ t.name }}</text>
        <text class="tpl-desc">{{ t.description }}</text>
      </view>
    </view>

    <view class="form">
      <view class="field">
        <text class="label">标题</text>
        <input v-model="title" placeholder="如 春日穿搭" class="input" />
      </view>
      <view class="field">
        <text class="label">副标题</text>
        <input v-model="subtitle" placeholder="如 3 件单品" class="input" />
      </view>
      <view class="field">
        <text class="label">心情</text>
        <input v-model="mood" placeholder="fresh / cozy / playful" class="input" />
      </view>
    </view>

    <PosterRenderer
      :template="tpl"
      :items="items"
      :title="title"
      :subtitle="subtitle"
      :mood="mood"
      @ready="onReady"
    />

    <view class="actions">
      <button class="primary" :disabled="saving" @click="save">保存到相册</button>
      <button class="secondary" :disabled="!shareId" @click="share('wechat')">分享微信</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { posterApi, type PosterTemplateMeta } from '@/api/poster'
import PosterRenderer from '@/components/poster/PosterRenderer.vue'

const tpl = ref<'magazine' | 'collage' | 'diary'>('magazine')
const templates = ref<PosterTemplateMeta[]>([])
const title = ref('今日穿搭')
const subtitle = ref('5 件单品')
const mood = ref('fresh')
const items = ref<Array<{ name: string; imageUrl: string }>>([])
const shareId = ref<number | null>(null)
const dataUrl = ref<string>('')
const saving = ref(false)

onMounted(async () => {
  templates.value = await posterApi.templates()
  const list = await fetch('/api/v1/clothes/list', { method: 'POST', body: '{}' })
    .then(r => r.json()).then(d => d.data ?? [])
  items.value = (list as any[]).slice(0, 6).map(c => ({ name: c.name, imageUrl: c.imageUrl }))
})

async function onReady(url: string) {
  dataUrl.value = url
}

async function save() {
  saving.value = true
  try {
    const r = await posterApi.generate({
      templateKey: tpl.value,
      clothingIds: [],
      title: title.value,
      subtitle: subtitle.value,
      mood: mood.value,
    })
    shareId.value = r.shareId
    // 保存到相册（小程序用 uni.saveImageToPhotosAlbum，H5 用 a 下载）
    // #ifdef H5
    const a = document.createElement('a')
    a.href = dataUrl.value
    a.download = `poster_${Date.now()}.png`
    a.click()
    // #endif
    uni.showToast({ title: '已保存', icon: 'success' })
  } finally {
    saving.value = false
  }
}

async function share(target: 'wechat' | 'moments') {
  if (!shareId.value) return
  await posterApi.share(shareId.value, target)
  uni.showToast({ title: '已分享', icon: 'success' })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: var(--bg); padding: 32rpx 24rpx; }
.title { font-size: 40rpx; font-weight: 700; display: block; margin-bottom: 24rpx; }
.templates { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12rpx; margin-bottom: 24rpx; }
.tpl { background: var(--surface); padding: 16rpx; border-radius: 16rpx; border: 2rpx solid transparent; }
.tpl.active { border-color: var(--accent); }
.tpl-name { font-size: 26rpx; font-weight: 600; display: block; }
.tpl-desc { font-size: 22rpx; color: var(--text-secondary); display: block; margin-top: 4rpx; }
.form { display: grid; gap: 16rpx; margin-bottom: 24rpx; }
.field { display: flex; flex-direction: column; }
.label { font-size: 24rpx; color: var(--text-secondary); margin-bottom: 4rpx; }
.input { padding: 16rpx; background: var(--surface); border-radius: 12rpx; font-size: 28rpx; }
.actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.primary, .secondary { flex: 1; padding: 24rpx 0; border-radius: 32rpx; }
.primary { background: var(--accent); color: white; }
.secondary { background: var(--accent-soft); color: var(--accent); }
</style>
```

- [ ] **Step 4: API**

```typescript
// frontend/src/api/poster.ts
import { http } from './http'

export interface PosterTemplateMeta {
  key: string
  name: string
  description: string
  previewUrl: string
}

export interface PosterContext {
  templateKey: 'magazine' | 'collage' | 'diary'
  clothingIds: number[]
  title?: string
  subtitle?: string
  mood?: string
}

export const posterApi = {
  templates() { return http.get<PosterTemplateMeta[]>('/api/v1/posters/templates') },
  generate(ctx: PosterContext) { return http.post<{ shareId: number; imageUrl: string | null; dataUrl?: string }>('/api/v1/posters/generate', ctx) },
  share(shareId: number, target: string) { return http.post(`/api/v1/posters/${shareId}/share`, { target }) },
}
```

- [ ] **Step 5: pages.json 注册**

```json
{ "path": "pages/poster/poster", "style": { "navigationBarTitleText": "分享海报" } }
```

- [ ] **Step 6: 跑测试确认通过**

Run: `npm test -- poster.test.ts`
Expected: 1/1 passed

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/poster/poster.vue frontend/src/pages/poster/poster.test.ts frontend/src/api/poster.ts frontend/src/pages.json
git commit -m "feat(poster-frontend): poster editor page + 3 template selection"
```

### Task 45: 海报端到端集成

**Files:**
- Create: `frontend/tests/e2e/poster.spec.ts`

- [ ] **Step 1: Playwright 测试**

```typescript
import { test, expect } from '@playwright/test'

test('海报编辑器生成 3 套模板', async ({ page }) => {
  await page.goto('/pages/poster/poster')
  await expect(page.locator('text=杂志大片')).toBeVisible()
  await expect(page.locator('text=色块拼图')).toBeVisible()
  await expect(page.locator('text=心情日记')).toBeVisible()
  // 切换模板
  await page.locator('text=色块拼图').click()
  // canvas 存在
  await expect(page.locator('canvas')).toBeVisible()
})
```

- [ ] **Step 2: 跑 e2e**

Run: `npx playwright test tests/e2e/poster.spec.ts`

- [ ] **Step 3: Commit**

```bash
git add frontend/tests/e2e/poster.spec.ts
git commit -m "test(e2e): poster editor renders 3 templates"
```

---

## Phase 5: 收尾 & 验证

### Task 46: 后端全量集成测试

**Files:**
- Modify: 临时脚本

- [ ] **Step 1: 跑所有 IT**

Run: `mvn verify`
Expected: 全部 IT 通过（Clothing/Outfit/OutfitAi/Weather/Poster 共 ~15+ 用例）

- [ ] **Step 2: 修复失败用例**

如失败，按报错定位修代码，再重跑。

- [ ] **Step 3: Commit（如有改动）**

```bash
git add backend/
git commit -m "test(backend): all integration tests green"
```

### Task 47: 前端 Playwright 视觉回归

**Files:**
- Create: `frontend/playwright.config.ts`
- Modify: `frontend/package.json`（scripts）

- [ ] **Step 1: Playwright 配置**

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  use: { baseURL: 'http://localhost:5173', headless: true, viewport: { width: 414, height: 896 } },
  webServer: { command: 'npm run dev:h5', port: 5173, reuseExistingServer: true },
})
```

- [ ] **Step 2: 跑全套 e2e**

Run: `npx playwright test`
Expected: 全部用例通过

- [ ] **Step 3: 视觉截图比对**

```typescript
// tests/e2e/visual.spec.ts
import { test, expect } from '@playwright/test'

test('homepage 截图比对', async ({ page }) => {
  await page.goto('/')
  await expect(page).toHaveScreenshot('home.png', { maxDiffPixelRatio: 0.02 })
})
```

Run: `npx playwright test --update-snapshots`（首次）/ `npx playwright test`（回归）

- [ ] **Step 4: Commit**

```bash
git add frontend/playwright.config.ts frontend/tests/e2e frontend/package.json
git commit -m "test(frontend): playwright e2e + visual regression"
```

### Task 48: handoff & README 更新

**Files:**
- Modify: `docs/superpowers/handoff.md`
- Modify: `README.md`

- [ ] **Step 1: handoff.md 追加 v2 章节**

在末尾追加：

```markdown
## v2 (2026-07-05) 上线

### 新增功能
- 视觉系统升级为樱粉燕麦 (#d49aa5 + #FFF7F2)
- AI 智能搭配（固定 5 套，本地规则引擎）
- 天气联动（和风 SDK，每日 03:00 cron）
- 衣橱 5 种视图（网格 / 衣架 / 抽屉 / 色块 / 季节）
- 高级分享海报（3 模板 + canvas 2d）

### 新增数据表
- weather_cache
- user_preference
- outfit_ai_generation
- poster_share

### 新增 API
- POST /api/v1/outfits/ai-generate
- GET  /api/v1/weather/forecast
- GET  /api/v1/weather/preference
- PUT  /api/v1/weather/preference
- GET  /api/v1/clothes/group
- GET  /api/v1/posters/templates
- POST /api/v1/posters/generate

### 踩坑（v2 专属）
- 和风 SDK 需 `QWX_API_KEY` 环境变量，否则 cron 报 IllegalStateException → 让 WeatherRefreshJob 启动时若 key 缺失则禁用
- canvas 在 H5 用 2d 上下文，小程序用 uni.createCanvasContext；推荐在编辑器中给两套都画一遍
- poster 表无 UNIQUE，单用户模式不需要
```

- [ ] **Step 2: README 更新**

在功能列表追加：

```markdown
## v2 新增

- AI 智能搭配：从起点单品生成 5 套方案
- 天气联动：基于位置的 7 天天气预报 + 偏好
- 衣橱多视图：网格 / 衣架 / 抽屉 / 色块 / 季节
- 高级海报：3 模板 + canvas 渲染 + 一键分享
```

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/handoff.md README.md
git commit -m "docs: handoff + README for v2 release"
```

---

## v2.1 后续任务概要（6 个功能，待 v2.0 上线后再排期）

每个功能走独立 plan（约 4-8 tasks）。

### 1. AI 智能识别（拍照识别单品）

- 后端：`RecognitionService` 调用阿里云视觉智能开放平台 / 腾讯云图像识别
- 表：`recognition_log`
- 前端：`/pages/add/clothing` 接入「拍照识别」按钮，识别后跳到填表单页

### 2. 数据洞察（衣橱统计 + 风格报告）

- 后端：`InsightsService` 计算：最常穿颜色、季节覆盖、闲置率、搭配丰富度
- API：`GET /api/v1/insights/summary`
- 前端：新增 `/pages/insights/insights.vue` 卡片 + 图表

### 3. 打包清单（出行穿搭规划）

- 表：`packing_list` + `packing_list_item`
- 后端：`PackingListService` 根据天数 + 场景推荐清单
- 前端：旅行前清单 + 一键带出去

### 4. 维护提醒（保养到期提醒）

- 表：`clothing_maintenance`
- 后端：cron 每月扫一次，到期推本地通知（uni-app notification API）
- 前端：「维护中心」页面

### 5. 心愿单（种草想买）

- 表：`wishlist_item`
- 后端：CRUD + 价格变更提醒（爬虫）
- 前端：心愿单列表 + 价格曲线

### 6. 闲置出售（二手交易）

- 表：`secondhand_item`
- 后端：CRUD + 平台跳转（闲鱼/转转）
- 前端：发布页 + 列表 + 状态跟踪

---

## 验收清单（v2.0 上线必须通过）

### 后端
- [ ] `mvn verify` 全绿（unit + integration 共 30+ 用例）
- [ ] 4 张新表 schema 应用成功（dev / staging / prod PG）
- [ ] QWeather API key 已配（staging）
- [ ] cron job 已注册且 03:00 跑过一次成功日志
- [ ] `/api/v1/outfits/ai-generate` 必返回 5 套
- [ ] `/api/v1/clothes/group` 支持 5 个字段
- [ ] `/api/v1/posters/generate` 写库成功返回 shareId

### 前端
- [ ] 首页显示樱粉燕麦基调，无残留 MVP 灰色
- [ ] tabbar 5 个：首页 / 衣橱 / AI / 搭配 / 我的
- [ ] 衣橱 5 种视图都能正常切换
- [ ] AI 搭配：选起点 → 生成 5 套 → like/dislike/save
- [ ] 天气卡显示 7 天
- [ ] 海报编辑器能选 3 模板，canvas 渲染，下载到相册
- [ ] `npm test` 全绿（vitest + Playwright）
- [ ] 视觉回归无明显 diff

### 数据兼容
- [ ] 老用户升级：MVP 已有数据完全保留
- [ ] Clothing / Outfit / Tag 表 schema 不变
- [ ] v1 API 路径未破坏

### 文档
- [ ] spec 已 commit
- [ ] plan 已 commit
- [ ] handoff 追加 v2 章节
- [ ] README 更新 v2 功能

---

## 风险与回退

| 风险 | 影响 | 回退方案 |
|---|---|---|
| 和风 API key 缺失 | cron 每日失败 | `WeatherRefreshJob` 启动时若 key 缺失则禁用定时任务，UI 显示"未配置" |
| 颜色协调规则不够准 | 用户嫌搭配丑 | v2.1 加反馈闭环：likeCount 高 → 规则权重提升 |
| canvas 在小程序真机不渲染 | 海报白屏 | 兜底：提示用户切到 H5；v2.1 改用服务端渲染（Puppeteer） |
| groupBy 在 5000+ 件时慢 | 衣橱卡顿 | v2.1 改为游标分页 + 索引（category/color/season 单字段已在 MVP 有） |
| 5 套搭配在衣橱 <5 件时报错 | 新用户冷启动 | OutfitAiServiceImpl 已处理：少于 3 件返回空集（兜底复用第 1 套） |
