package cn.sta1n.nai2android

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageLoadingTest {
    @Test
    fun thumbnail_sampling_limits_the_longest_side() {
        assertEquals(1, calculateInSampleSize(768, 512, 768))
        assertEquals(2, calculateInSampleSize(1536, 1024, 768))
        assertEquals(8, calculateInSampleSize(4096, 4096, 768))
        assertEquals(16, calculateInSampleSize(8192, 4096, 768))
    }
}
