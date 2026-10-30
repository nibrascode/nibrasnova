package com.nibras.nova.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class NovaLinksTest {
    @Test fun chineseGetsZhPolicy() {
        listOf("zh", "zh-CN", "ZH", "zh-Hans").forEach {
            assertEquals(NovaLinks.PRIVACY_POLICY_ZH, NovaLinks.privacyPolicyFor(it))
        }
    }
    @Test fun othersGetDefaultPolicy() {
        listOf("az", "en", "tr", "ru", "ar", null, "").forEach {
            assertEquals(NovaLinks.PRIVACY_POLICY, NovaLinks.privacyPolicyFor(it))
        }
    }
}
