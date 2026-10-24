package com.succh.unifeed.data.rss

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RsshubCatalogTest {

    @Test
    fun parseNamespaceJson() {
        val json = """
            {
              "zhihu": {
                "name": "知乎",
                "lang": "zh-CN",
                "categories": ["social-media"],
                "routes": {
                  "/hot/:category?": {
                    "path": "/hot/:category?",
                    "name": "知乎热榜",
                    "example": "/zhihu/hot",
                    "parameters": {"category": "分类"}
                  },
                  "/zhuanlan/:id": {
                    "path": "/zhuanlan/:id",
                    "name": "专栏",
                    "example": "/zhihu/zhuanlan/googledevelopers",
                    "parameters": {"id": "专栏 ID"}
                  }
                }
              }
            }
        """.trimIndent()
        val list = RsshubCatalog.parse(json)
        assertEquals(1, list.size)
        assertEquals("知乎", list[0].name)
        assertEquals(2, list[0].routes.size)
        val hot = list[0].routes.first { it.title == "知乎热榜" }
        assertEquals("/zhihu/hot", hot.example)
        assertFalse(hot.needsUserParam)
        val url = RsshubCatalog.buildSubscribeUrl("https://rsshub.app", hot)
        assertEquals("https://rsshub.app/zhihu/hot", url)
    }

    @Test
    fun categoryLabel() {
        assertEquals("社交媒体", RsshubCatalog.labelOf("social-media"))
        assertEquals("二次元/ACG", RsshubCatalog.labelOf("anime"))
    }

    @Test
    fun builtinFeedCount() {
        val n = com.succh.unifeed.ui.BuiltinFeeds.all.size
        assertTrue("预置源应不少于 20 个，实际 $n", n >= 20)
        assertTrue(com.succh.unifeed.ui.BuiltinFeeds.unrepaired.isNotEmpty())
    }
}
