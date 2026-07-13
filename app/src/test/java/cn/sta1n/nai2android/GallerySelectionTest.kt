package cn.sta1n.nai2android

import kotlin.test.Test
import kotlin.test.assertEquals

class GallerySelectionTest {
    @Test
    fun toggle_adds_then_removes_an_image_id() {
        val initial = GallerySelection()

        val selected = initial.toggle("one")
        val cleared = selected.toggle("one")

        assertEquals(setOf("one"), selected.ids)
        assertEquals(emptySet(), cleared.ids)
    }

    @Test
    fun select_all_replaces_selection_with_visible_image_ids() {
        val existing = GallerySelection(setOf("hidden"))
        val visible = listOf(testImage("one"), testImage("two"))

        assertEquals(setOf("one", "two"), existing.selectAll(visible).ids)
    }

    @Test
    fun remove_clears_only_deleted_ids() {
        val selection = GallerySelection(setOf("one", "two", "three"))

        assertEquals(setOf("one", "three"), selection.remove(setOf("two")).ids)
    }

    private fun testImage(id: String) = ImageRecord(
        id = id,
        localUri = "file:///data/data/cn.sta1n.nai2android/files/gallery/$id.png",
        createdAt = 1,
        prompt = "1girl",
        archiveTags = emptyList(),
        artist = "",
        negativePrompt = "",
        presetName = "",
        favorite = false,
        savedToDevice = false
    )
}
