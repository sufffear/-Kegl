package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import com.example.ui.*
import com.example.ui.theme.VelarisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w320dp-h640dp")
class ChantalItemDialogTest {
    @get:Rule val compose = createComposeRule()
    @Test fun confirmingCancellingAndDoubleClickCannotSpendTwice() {
        var calls = 0
        compose.setContent { VelarisTheme {
            ChantalItemDetailsDialog("Трава", "Описание", actions = listOf(
                ChantalItemAction("sell", "Продать · +12 М", confirmation = "Предмет исчезнет", success = "Продано") { calls++ }
            ), onDismiss = {})
        } }
        compose.onNodeWithTag("item_action_sell").performClick()
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithTag("item_cancel").performClick()
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithTag("item_action_sell").performClick()
        val click = compose.onNodeWithTag("item_confirm").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnIdle { click(); click() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, calls) }
        compose.onNodeWithTag("item_action_result").assertTextEquals("Продано")
        compose.onNodeWithTag("item_action_sell").assertDoesNotExist()
    }
    @Test fun longDescriptionAtLargeFontScrollsAndCloseRemainsReachable() {
        var closed = false
        compose.setContent { VelarisTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                ChantalItemDetailsDialog("Длинное название памятной находки", "Описание. ".repeat(120) + "Конец описания.",
                    actions = listOf(ChantalItemAction("buy", "Купить · 100 М", enabled = false, success = "Получено") {}), onDismiss = { closed = true })
            }
        } }
        compose.onNodeWithText("Конец описания.", substring = true).assertExists()
        compose.onNodeWithTag("item_action_buy").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("item_close").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(closed) }
    }
    @Test fun failureIsVisibleAndNeverPretendsToBeSuccessOrRetries() {
        var calls = 0
        compose.setContent { VelarisTheme {
            ChantalItemDetailsDialog("Предмет", "Описание", actions = listOf(
                ChantalItemAction("use", "Использовать", success = "Готово") { calls++; error("fixture") }
            ), onDismiss = {})
        } }
        compose.onNodeWithTag("item_action_use").performClick(); compose.waitForIdle()
        compose.onNodeWithTag("item_action_result").assertTextContains("Не удалось", substring = true)
        compose.onNodeWithTag("item_action_use").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, calls) }
    }
    @Test fun availabilityIsRecheckedWhileConfirmationIsOpen() {
        var available by mutableStateOf(true)
        var calls = 0
        compose.setContent { VelarisTheme {
            ChantalItemDetailsDialog("Покупка", "Цена: 10 М", actions = listOf(
                ChantalItemAction("buy", "Купить", available, "Потратить 10 М?", "Получено") { calls++ }
            ), onDismiss = {})
        } }
        compose.onNodeWithTag("item_action_buy").performClick()
        compose.runOnIdle { available = false }
        compose.onNodeWithTag("item_confirm").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, calls) }
    }
    @Test fun realShopShowsMissingCoinsAndPreventsBuying() {
        var bought = 0
        val item = com.example.domain.DEFAULT_CHANTAL_SHOP_ITEMS.first { it.category == com.example.domain.ChantalShopCategory.DEN_COMFORT }
        compose.setContent { VelarisTheme {
            ChantalShopSectionCard(coins = 0, itemsInCurrentStep = 0, selectedShopEntryId = item.id,
                onSelectShopEntryId = {}, shopCategoryTab = "den_comfort", onSelectShopCategoryTab = {},
                shopDenStepFilter = 1, onSelectShopDenStepFilter = {}, ownedShopItemIds = emptyList(),
                claimedMerchantOfferIds = emptyList(), onBuyMerchantOffer = { bought++ }, onBuyShopItem = { bought++ })
        } }
        compose.onNodeWithText("Не хватает монет", substring = true).assertExists()
        compose.onNodeWithTag("item_action_buy").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0,bought) }
    }

}
