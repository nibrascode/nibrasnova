package com.nibras.nova.domain.usecase

import com.nibras.nova.core.security.UrlValidator
import com.nibras.nova.domain.model.SearchEngineConfig
import com.nibras.nova.domain.model.SearchEngineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResolveInputToUrlUseCaseTest {

    private lateinit var resolveInputToUrlUseCase: ResolveInputToUrlUseCase

    @Before
    fun setUp() {
        resolveInputToUrlUseCase = ResolveInputToUrlUseCase(UrlValidator())
    }

    @Test
    fun `resolves search queries using configured search engine`() {
        val config = SearchEngineConfig(selectedEngine = SearchEngineType.BRAVE)
        val result = resolveInputToUrlUseCase("android webview security", config, httpsOnlyMode = true)
        assertTrue(result is ResolvedNavigationTarget.LoadWebUrl)
        assertEquals(
            "https://search.brave.com/search?q=android+webview+security",
            (result as ResolvedNavigationTarget.LoadWebUrl).url
        )
    }

    @Test
    fun `resolves custom search engine template with placeholder`() {
        val config = SearchEngineConfig(
            selectedEngine = SearchEngineType.CUSTOM,
            customSearchTemplate = "https://search.example.com/q?term=%s"
        )
        val result = resolveInputToUrlUseCase("nibras nova", config, httpsOnlyMode = true)
        assertTrue(result is ResolvedNavigationTarget.LoadWebUrl)
        assertEquals(
            "https://search.example.com/q?term=nibras+nova",
            (result as ResolvedNavigationTarget.LoadWebUrl).url
        )
    }

    @Test
    fun `blocks dangerous schemes typed in address bar`() {
        val config = SearchEngineConfig()
        val result = resolveInputToUrlUseCase("javascript:alert(1)", config, httpsOnlyMode = true)
        assertTrue(result is ResolvedNavigationTarget.BlockedUnsafeInput)
    }
}
