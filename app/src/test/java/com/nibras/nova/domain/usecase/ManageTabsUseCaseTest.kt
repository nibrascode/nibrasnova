package com.nibras.nova.domain.usecase

import com.nibras.nova.domain.model.TabItem
import com.nibras.nova.domain.model.TabWorkspace
import com.nibras.nova.domain.repository.TabRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ManageTabsUseCaseTest {

    private val tabRepository: TabRepository = mockk(relaxed = true)
    private val useCase = ManageTabsUseCase(tabRepository)

    @Test
    fun `openNewTab delegates creation to TabRepository with workspace and selects tab immediately`() = runTest {
        val expected = TabItem(
            id = "tab-1",
            url = "https://kotlinlang.org",
            isPrivate = false,
            isActive = true,
            workspace = TabWorkspace.WORK
        )
        coEvery {
            tabRepository.createTab(
                url = "https://kotlinlang.org",
                isPrivate = false,
                isDesktopMode = false,
                selectImmediately = true,
                workspace = TabWorkspace.WORK,
                parentTabId = null
            )
        } returns expected

        val created = useCase.openNewTab(
            url = "https://kotlinlang.org",
            isPrivate = false,
            workspace = TabWorkspace.WORK
        )
        assertEquals("tab-1", created.id)
        assertEquals(TabWorkspace.WORK, created.workspace)
        coVerify(exactly = 1) {
            tabRepository.createTab("https://kotlinlang.org", false, false, true, TabWorkspace.WORK, null)
        }
    }
}
