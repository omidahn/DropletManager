package com.omiddd.dropletmanager.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.data.model.Droplet
import com.omiddd.dropletmanager.data.model.DropletAction
import com.omiddd.dropletmanager.data.model.Image
import com.omiddd.dropletmanager.data.model.NetworkV4
import com.omiddd.dropletmanager.data.model.Networks
import com.omiddd.dropletmanager.data.model.Region
import com.omiddd.dropletmanager.data.model.Size
import com.omiddd.dropletmanager.ui.viewmodel.CostSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DropletListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun activeDropletButtonsInvokeExpectedCallbacks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val refreshLabel = context.getString(R.string.refresh)
        val switchProjectLabel = context.getString(R.string.switch_project)
        val stopLabel = context.getString(R.string.stop)
        val rebootLabel = context.getString(R.string.reboot)
        val consoleLabel = context.getString(R.string.console)

        var refreshes = 0
        var switchProjectCalls = 0
        val actions = mutableListOf<DropletAction>()
        val consoles = mutableListOf<Int>()

        composeRule.setContent {
            DropletListScreen(
                droplets = listOf(fakeDroplet(status = "active")),
                isLoading = false,
                error = null,
                costSummary = CostSummary(),
                onRefresh = { refreshes++ },
                onDropletClicked = {},
                onActionClicked = { _, action -> actions += action },
                onOpenConsole = { droplet -> consoles += droplet.id },
                onSwitchProject = { switchProjectCalls++ }
            )
        }

        composeRule.onNodeWithContentDescription(refreshLabel).performClick()
        composeRule.onNodeWithContentDescription(switchProjectLabel).performClick()
        composeRule.onNodeWithText(stopLabel).performClick()
        composeRule.onNodeWithText(rebootLabel).performClick()
        composeRule.onNodeWithText(consoleLabel).performClick()

        assertEquals(1, refreshes)
        assertEquals(1, switchProjectCalls)
        assertEquals(listOf(DropletAction.PowerOff, DropletAction.Reboot), actions)
        assertEquals(listOf(1), consoles)
    }

    @Test
    fun inactiveDropletShowsStartAction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val startLabel = context.getString(R.string.start)
        val consoleLabel = context.getString(R.string.console)
        val actions = mutableListOf<DropletAction>()

        composeRule.setContent {
            DropletListScreen(
                droplets = listOf(fakeDroplet(status = "off")),
                isLoading = false,
                error = null,
                costSummary = CostSummary(),
                onRefresh = {},
                onDropletClicked = {},
                onActionClicked = { _, action -> actions += action },
                onOpenConsole = {},
                onSwitchProject = {}
            )
        }

        composeRule.onNodeWithText(startLabel).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(consoleLabel).assertIsDisplayed()

        assertEquals(listOf(DropletAction.PowerOn), actions)
    }

    private fun fakeDroplet(status: String): Droplet {
        return Droplet(
            id = 1,
            name = "web-1",
            memory = 1024,
            vcpus = 1,
            disk = 25,
            status = status,
            region = Region("fra1", "Frankfurt", emptyList(), true, emptyList()),
            image = Image(101, "22.04 x64", "Ubuntu", "ubuntu-22-04-x64", "distribution"),
            size = Size("s-1vcpu-1gb", 1024, 1, 25, 1.0, 6.0, 0.009, true),
            createdAt = "2024-01-01T00:00:00Z",
            networks = Networks(
                v4 = listOf(NetworkV4("203.0.113.10", "255.255.255.0", "203.0.113.1", "public")),
                v6 = emptyList()
            ),
            features = emptyList(),
            tags = emptyList(),
            volumeIds = emptyList(),
            sizeSlug = "s-1vcpu-1gb",
            backupIds = emptyList(),
            snapshotIds = emptyList(),
            monitoringPolicy = null,
            backups = false,
            monitoring = true,
            ipv6 = false
        )
    }
}
