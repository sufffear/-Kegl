package com.example

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.*
import com.example.domain.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp")
class ChantalItemTransactionsUiTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val compose = createAndroidComposeRule<MainActivity>()
    private val repo get() = ChantalProgressRepository(context)
    private val seed = object : ExternalResource() {
        override fun before() {
            WorkManagerTestInitHelper.initializeTestWorkManager(context)
            val inventory = (0..29).map { i -> ChantalInventoryLootItem("fixture_$i", "Предмет $i", "Вещь $i", "🌿",
                when(i) { 0 -> ChantalLootCategory.HERB; 1 -> ChantalLootCategory.TRINKET; 2 -> ChantalLootCategory.ROYAL_CLUE; else -> ChantalLootCategory.DEN_TROPHY },
                "Описание находки $i. ".repeat(4), 12, "Мельница", "Сегодня") }
            val ep = ChantalNarrativeSample.episode().copy(id = "gift_fixture", npcNameRu = "Брамбл", chosenActionSummaryRu = "Поддержать")
            repo.replace(JSONObject().put("schemaVersion",1).put("hasCompletedPrologue",true)
                .put("coins",1000).put("wit",0).put("curiosity",5).put("maturityXp",29).put("totalJourneys",2)
                .put("moodPct",40).put("hasUsedPetThisCycle",true)
                .put("inventoryList", JSONArray(inventory.map { ChantalProgressCodec.encode(it) }))
                .put("journalList",JSONArray().put(ChantalProgressCodec.encode(ep))).toString())
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(seed).around(compose)
    private fun click(tag: String) { compose.onNodeWithTag(tag).performScrollTo().performClick(); compose.waitForIdle() }
    private fun bag() { compose.onNodeWithTag("nav_chantal").performClick(); click("btn_quick_bag") }
    private fun shop() { compose.onNodeWithTag("nav_chantal").performClick(); click("btn_chantal_menu"); click("chantal_world_shop") }
    private fun confirmTwice() {
        val click = compose.onNodeWithTag("item_confirm").performScrollTo().fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnIdle { click(); click() }; compose.waitForIdle()
    }
    private fun unchangedStory() {
        val saved = repo.load()
        assertEquals(29, saved.getInt("maturityXp")); assertEquals(2, saved.getInt("totalJourneys"))
        assertEquals(1, saved.getJSONArray("journalList").length())
    }
    @Test fun readOnlyTrophyDetailsKeepInventoryScrollAndDoNotAwardAnything() {
        bag()
        compose.onNodeWithTag("bag_item_fixture_29").performScrollTo()
        val top = compose.onNodeWithTag("bag_item_fixture_29").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("bag_item_fixture_29").performClick()
        compose.onNodeWithTag("chantal_item_dialog").assertIsDisplayed()
        compose.onNodeWithText("Стоит в Домике", substring = true).assertExists()
        compose.onNode(isDialog()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Back) }; compose.waitForIdle()
        assertEquals(top,compose.onNodeWithTag("bag_item_fixture_29").fetchSemanticsNode().boundsInRoot.top,1f)
        assertEquals(1000,repo.load().getInt("coins")); assertEquals(30,repo.load().getJSONArray("inventoryList").length()); unchangedStory()
    }
    @Test fun saleRequiresConfirmationAndSurvivesRecreationWithoutDoubleReward() {
        bag(); click("bag_item_fixture_0"); click("item_action_sell"); click("item_cancel")
        assertEquals(1000,repo.load().getInt("coins"))
        click("item_action_sell"); confirmTwice()
        compose.onNodeWithTag("item_action_result").assertTextContains("+12 М", substring = true)
        assertEquals(1012,repo.load().getInt("coins")); assertEquals(29,repo.load().getJSONArray("inventoryList").length())
        click("item_close"); compose.activityRule.scenario.recreate(); compose.waitForIdle()
        assertEquals(1012,repo.load().getInt("coins")); compose.onNodeWithTag("bag_item_fixture_0").assertDoesNotExist(); unchangedStory()
    }
    @Test fun herbAndClueKeepTheirExistingEffectsAndCannotBeReused() {
        bag(); click("bag_item_fixture_0"); click("item_action_brew"); confirmTwice(); click("item_close")
        assertEquals(55,repo.load().getInt("moodPct")); assertEquals(1000,repo.load().getInt("coins"))
        click("bag_item_fixture_2"); click("item_action_inspect"); click("item_close")
        click("bag_item_fixture_2"); compose.onNodeWithTag("item_action_inspect").assertIsNotEnabled(); click("item_close")
        assertEquals(6,repo.load().getInt("curiosity")); unchangedStory()
    }
    @Test fun consumableIsBoughtOncePerConfirmationAndCanBeBoughtAgainDeliberately() {
        shop()
        val item = DEFAULT_CHANTAL_SHOP_ITEMS.first { it.category == ChantalShopCategory.CONSUMABLE }
        // Switch to expedition stock using its visible category button.
        compose.onNodeWithText("🎒 Снаряжение").performScrollTo().performClick()
        click("shop_item_${item.id}"); click("item_action_buy")
        assertEquals(1000,repo.load().getInt("coins")); confirmTwice()
        assertEquals(1000-item.priceCoins,repo.load().getInt("coins"))
        assertEquals(1,repo.load().getJSONObject("pantryProvisionCounts").getInt(item.id)); click("item_close")
        click("shop_item_${item.id}"); click("item_action_buy"); confirmTwice(); click("item_close")
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        assertEquals(1000-2*item.priceCoins,repo.load().getInt("coins"))
        assertEquals(2,repo.load().getJSONObject("pantryProvisionCounts").getInt(item.id)); unchangedStory()
    }
    @Test fun craftingConsumesExactlyTwoItemsAndReportsResult() {
        bag(); compose.onNodeWithText("Смастерить Оберег").performScrollTo().performClick()
        click("item_action_craft"); assertEquals(30,repo.load().getJSONArray("inventoryList").length()); confirmTwice()
        compose.onNodeWithTag("item_action_result").assertTextContains("Оберег создан",substring=true)
        assertEquals(28,repo.load().getJSONArray("inventoryList").length()); assertTrue(repo.load().getBoolean("hasActiveRoadAmulet"))
        assertEquals(1000,repo.load().getInt("coins")); unchangedStory()
    }
    @Test fun giftIsNotConsumedUntilRecipientIsConfirmed() {
        bag(); click("bag_item_fixture_1"); click("item_action_gift")
        click("item_action_gift_0"); click("item_cancel")
        assertEquals(30,repo.load().getJSONArray("inventoryList").length())
        click("item_action_gift_0"); confirmTwice()
        compose.onNodeWithTag("item_action_result").assertTextContains("Брамбл",substring=true)
        assertEquals(29,repo.load().getJSONArray("inventoryList").length()); assertEquals(1000,repo.load().getInt("coins")); unchangedStory()
    }
    @Test fun gearPurchaseEquipsOnlyOnceAndAlreadyOwnedGearHasNoBuyAction() {
        compose.onNodeWithTag("nav_chantal").performClick(); click("chantal_context_action"); click("slot_card_OUTFIT")
        val gear = DEFAULT_CHANTAL_SHOP_ITEMS.first { it.loadoutSlotType == ChantalLoadoutSlotType.OUTFIT }
        click("gear_item_${gear.id}"); click("item_action_buy_equip"); confirmTwice()
        assertEquals(1000-gear.priceCoins,repo.load().getInt("coins")); assertEquals(gear.id,repo.load().getString("equippedOutfitId"))
        click("item_close"); click("gear_item_${gear.id}")
        compose.onNodeWithTag("item_action_buy_equip").assertDoesNotExist(); click("item_action_equip"); click("item_close")
        assertTrue(repo.load().isNull("equippedOutfitId")); assertEquals(1000-gear.priceCoins,repo.load().getInt("coins")); unchangedStory()
    }
    @Test fun permanentFurnitureAndMerchantOfferCannotBeBoughtAgainAfterRestart() {
        shop()
        val item = DEFAULT_CHANTAL_SHOP_ITEMS.first { it.category == ChantalShopCategory.DEN_COMFORT && it.denStep == 1 }
        val offer = DEFAULT_CHANTAL_MERCHANT_OFFERS.first()
        click("shop_item_${item.id}"); click("item_action_buy"); confirmTwice(); click("item_close")
        click("shop_item_${item.id}"); compose.onNodeWithTag("item_action_buy").assertIsNotEnabled(); click("item_close")
        click("shop_offer_${offer.id}"); click("item_action_buy"); confirmTwice(); click("item_close")
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        click("shop_offer_${offer.id}"); compose.onNodeWithTag("item_action_buy").assertIsNotEnabled(); click("item_close")
        assertEquals(1000-item.priceCoins-offer.discountPriceCoins,repo.load().getInt("coins"))
        val owned = ChantalProgressCodec.strings(repo.load().getJSONArray("ownedShopItemIds"))
        assertEquals(1,owned.count { it == item.id })
        assertEquals(listOf(offer.id),ChantalProgressCodec.strings(repo.load().getJSONArray("claimedMerchantOfferIds")))
        unchangedStory()
    }

}
