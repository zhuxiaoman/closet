package com.closet.unit;

import com.closet.service.ColorHarmonyEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit test for {@link ColorHarmonyEngine}.
 * The engine scores a pair of HEX colors by their HSL hue distance:
 * < 30 deg   -> monochrome     (score 0.95)
 * < 90 deg   -> analogous      (score 0.80)
 * 150..210   -> complementary  (score 0.75)
 * > 210      -> triadic-ish    (score 0.60)
 * else       -> clash          (score 0.40)
 * Invalid / null HEX inputs short-circuit to a neutral 0.50.
 */
class ColorHarmonyEngineTest {

    @Test
    void 同色系应该返回高分() {
        // white #ffffff vs off-white #faf6f3 -> hue ~ identical, score >= 0.8
        double score = ColorHarmonyEngine.harmonyScore("#ffffff", "#faf6f3");
        assertTrue(score >= 0.8,
                "同色系颜色应至少 0.8 分，实际 " + score);
    }

    @Test
    void 互补色应该和谐() {
        // 樱粉 #d49aa5 (hue ~ 350) vs 雾蓝 #a8c8d8 (hue ~ 200) -> ~150 deg, complementary
        double score = ColorHarmonyEngine.harmonyScore("#d49aa5", "#a8c8d8");
        assertTrue(score >= 0.7,
                "互补色应至少 0.7 分，实际 " + score);
    }

    @Test
    void 撞色应该不和谐() {
        // 纯红 vs 纯绿 -> hue diff ~ 120 actually triadic...
        // 改用纯红 vs 纯青（hue ~180 -> complementary）但 plan 里是 ff0000 vs 00ff00
        // 0x00ff00 hue ~120, 0xff0000 hue ~0 -> diff 120 -> triadic bucket
        // 按 plan 既定断言：< 0.5 表示不和谐
        double score = ColorHarmonyEngine.harmonyScore("#ff0000", "#00ff00");
        assertTrue(score < 0.5,
                "撞色应低于 0.5 分，实际 " + score);
    }

    @Test
    void 无效颜色返回中性分() {
        assertEquals(0.5, ColorHarmonyEngine.harmonyScore(null, "#ffffff"), 0.01);
        assertEquals(0.5, ColorHarmonyEngine.harmonyScore("invalid", "#ffffff"), 0.01);
        assertEquals(0.5, ColorHarmonyEngine.harmonyScore("#fff", "#ffffff"), 0.01);
    }
}