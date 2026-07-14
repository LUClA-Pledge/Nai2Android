package cn.sta1n.nai2android

import kotlin.test.Test
import kotlin.test.assertEquals

class CreateScreenLayoutPolicyTest {
    @Test
    fun multiline_editor_limits_normal_fields_to_four_visible_lines() {
        assertEquals(4, createMultilineFieldMaxLines(2))
        assertEquals(4, createMultilineFieldMaxLines(3))
        assertEquals(4, createMultilineFieldMaxLines(4))
    }

    @Test
    fun multiline_editor_never_uses_fewer_lines_than_its_minimum() {
        assertEquals(8, createMultilineFieldMaxLines(8))
    }
}
