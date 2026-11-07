import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
  Backpack,
  BookOpen,
  CheckCircle2,
  Compass,
  FileText,
  Gift,
  Heart,
  Home,
  MapPin,
  Menu,
  Send,
  ShoppingBag,
  Sparkles,
  Timer,
  Upload,
  X,
} from 'lucide-react';
import {
  ActiveJourneyTimerState,
  AiProviderId,
  buildFallbackStoryEpisode,
  buildThreeTierChronicleMemory,
  CHANTAL_SHOP_ITEMS,
  ChantalShopItem,
  ChantalTraits,
  computeTraitGameplayPerks,
  DEN_TIER_IMAGES,
  formatTraitDeltasBadge,
  GeneratedStoryEpisode,
  getChantalStageInfo,
  getDenVisualTierInfo,
  getInteractivePettingDialogue,
  getLootItemEmoji,
  getLootItemProperties,
  getLootItemShortTitle,
  getMerchantOfferEmoji,
  getMerchantOfferProperties,
  getMerchantOfferShortTitle,
  getRegionEmoji,
  getRegionShortTitle,
  getShopItemCategoryLabelRu,
  getShopItemEmoji,
  getShopItemProperties,
  getShopItemShortTitle,
  getTraitActivePerkDescriptionRu,
  INITIAL_SEEDED_JOURNAL_HISTORY,
  InventoryLootItem,
  KEY_COMPANION_NPCS,
  LootItemPropertyDescriptor,
  NpcFriendshipEntry,
  OPENROUTER_MODELS_CATALOG,
  pickAutonomousRegionForChantal,
  StoryDifficultyLevel,
  TRAIT_METADATA,
  TRAVELING_MERCHANT_OFFERS,
  TravelingMerchantOffer,
  WORLD_MAP_IMAGE_URL,
  WORLD_MAP_REGIONS,
} from './chantalData';

interface ChantalCompanionViewProps {
  energy: number;
  setEnergy: React.Dispatch<React.SetStateAction<number>>;
  coins: number;
  setCoins: React.Dispatch<React.SetStateAction<number>>;
  currentStreakDays: number;
  todayPlannedDoneCount?: number;
  todayClosedPeriodsCount?: number;
  lastCompletedWorkoutTitle?: string;
  onGoToWorkout: () => void;
  soundEnabled: boolean;
  onPlayTone: (kind: 'hold' | 'relax' | 'complete' | 'salute') => void;
}

type ChantalSubView = 'den' | 'map' | 'inventory' | 'shop' | 'journal' | 'ai_settings';

export default function ChantalCompanionView({
  energy,
  setEnergy,
  coins,
  setCoins,
  currentStreakDays,
  soundEnabled,
  onPlayTone,
}: ChantalCompanionViewProps) {
  // 1. Prologue & Core Character Progression
  const [hasCompletedPrologue, setHasCompletedPrologue] = useState<boolean>(() => {
    return localStorage.getItem('chantal_prologue_done') === 'true';
  });
  const [totalJourneys, setTotalJourneys] = useState<number>(() => {
    const saved = Number(localStorage.getItem('chantal_total_journeys'));
    return Number.isFinite(saved) && saved >= 3 ? saved : 3;
  });
  const [maturityXp, setMaturityXp] = useState<number>(() => {
    const savedXp = Number(localStorage.getItem('chantal_maturity_xp'));
    if (Number.isFinite(savedXp) && savedXp >= 0) return savedXp;
    const savedJourneys = Number(localStorage.getItem('chantal_total_journeys'));
    const initJourneys = Number.isFinite(savedJourneys) && savedJourneys >= 3 ? savedJourneys : 3;
    return initJourneys * 10;
  });
  const [recentXpGainBadge, setRecentXpGainBadge] = useState<{
    amount: number;
    sourceRu: string;
  } | null>(null);
  const forceTransformPreview = false;

  // 1-time Petting per Journey Return Cycle + Collapsible Sections & Compact UI States
  const [hasUsedPetThisCycle, setHasUsedPetThisCycle] = useState<boolean>(() => {
    return localStorage.getItem('chantal_pet_used_this_cycle') === 'true';
  });
  const [isBurgerMenuOpen, setIsBurgerMenuOpen] = useState<boolean>(false);
  const [denSubTab, setDenSubTab] = useState<'none' | 'traits' | 'housing'>('none');
  const [activeLoadoutSlotPicker, setActiveLoadoutSlotPicker] = useState<
    'NONE' | 'OUTFIT' | 'TOOL' | 'WEAPON' | 'BUNDLE' | 'AMULET'
  >('NONE');
  const [activeSlotInspectId, setActiveSlotInspectId] = useState<string | null>(null);
  const [activeDenItemPopoverId, setActiveDenItemPopoverId] = useState<string | null>(null);
  const [activePropertyPopup, setActivePropertyPopup] =
    useState<LootItemPropertyDescriptor | null>(null);
  const [selectedShopEntryId, setSelectedShopEntryId] = useState<string | null>(null);
  const [shopSectionFilter, setShopSectionFilter] = useState<'den_comfort' | 'expedition'>(
    'den_comfort'
  );
  const [shopDenStepFilter, setShopDenStepFilter] = useState<1 | 2 | 3>(1);
  const [activeShopPropertyPopup, setActiveShopPropertyPopup] =
    useState<LootItemPropertyDescriptor | null>(null);
  const [activeBagWorkshopTab, setActiveBagWorkshopTab] = useState<
    'none' | 'alchemy' | 'friendship'
  >('none');
  const [selectedTraitPopupKey, setSelectedTraitPopupKey] = useState<
    keyof ChantalTraits | null
  >(null);
  const [selectedMapRegionId, setSelectedMapRegionId] = useState<string | null>(null);
  const [expandedJournalEpisodeId, setExpandedJournalEpisodeId] = useState<string | null>(
    () => INITIAL_SEEDED_JOURNAL_HISTORY[0]?.id ?? null
  );
  const [showChronicleMemoryLayers, setShowChronicleMemoryLayers] = useState<boolean>(false);
  const [lastChoiceReactionCard, setLastChoiceReactionCard] = useState<{
    episodeTitleRu: string;
    journeyNumber: number;
    regionTitleRu: string;
    playerChoiceRu: string;
    chantalReactionRu: string;
    deltas: Partial<ChantalTraits>;
    previousTraits: ChantalTraits;
    newTraits: ChantalTraits;
    coinsEarned: number;
    xpEarned: number;
    lootItem: InventoryLootItem;
    denStepCountTextRu?: string;
    isMilestoneChest?: boolean;
    isFillerEpisode?: boolean;
  } | null>(null);

  // 4-Slot Expedition Loadout: 1. Outfit (🧥), 2. Tool (🧭), 3. Weapon/Defense (🗡️), 4. Bundle (🧺: Shop Provision OR Found Loot Item)
  const [gearDurabilityMap, setGearDurabilityMap] = useState<Record<string, number>>(() => {
    try {
      const saved = localStorage.getItem('chantal_gear_durability');
      if (saved) return JSON.parse(saved);
    } catch {
      // ignore
    }
    return {
      gear_brass_lantern: 5,
      gear_knitted_scarf: 5,
    };
  });
  const [selectedSlotOutfitId, setSelectedSlotOutfitId] = useState<string | null>(null);
  const [selectedSlotToolId, setSelectedSlotToolId] = useState<string | null>(null);
  const [selectedSlotWeaponId, setSelectedSlotWeaponId] = useState<string | null>(null);
  const [pantryProvisionCounts, setPantryProvisionCounts] = useState<Record<string, number>>(() => {
    try {
      const saved = localStorage.getItem('chantal_pantry_provisions');
      if (saved) return JSON.parse(saved);
    } catch {
      // ignore
    }
    return {
      supply_honey_pie: 1,
      supply_herbal_balm: 0,
      supply_shiny_buttons: 1,
    };
  });
  const [selectedBackpackProvisionId, setSelectedBackpackProvisionId] = useState<string | null>(
    null
  );
  const [selectedBundleLootItemId, setSelectedBundleLootItemId] = useState<string | null>(null);

  // 2. 6 Character Traits (0–100 smooth progression scale; +1/+2 per choice)
  const [traits, setTraits] = useState<ChantalTraits>(() => {
    try {
      const raw = localStorage.getItem('chantal_traits_v100');
      if (raw) return JSON.parse(raw);
    } catch {
      // ignore
    }
    return {
      curiosity: 5,
      tenderness: 6,
      playfulness: 4,
      romanticism: 3,
      courage: 4,
      wit: 5,
    };
  });

  // 3. Toggleable Trait Change Arrow Toast, Visual Trait Change History & Interactive Petting State
  const [showTraitArrowsSetting, setShowTraitArrowsSetting] = useState<boolean>(true);
  const [activeTraitToast, setActiveTraitToast] = useState<string[] | null>(null);
  const [lastTraitChangeRecord, setLastTraitChangeRecord] = useState<{
    deltas: Partial<ChantalTraits>;
    previousTraits: ChantalTraits;
    newTraits: ChantalTraits;
    choiceLabelRu: string;
    storyTitleRu: string;
  } | null>(null);
  const [lastReactionBanner, setLastReactionBanner] = useState<string | null>(null);
  const [petVariantCounter, setPetVariantCounter] = useState<number>(0);
  const [petSpeechBubble, setPetSpeechBubble] = useState<{
    textRu: string;
    badgeRu: string;
  } | null>(null);
  const [isHeartPulseActive, setIsHeartPulseActive] = useState<boolean>(false);

  // 4. Mood, Condition ("Bad News": 45 Energy Exhaustion & Coin Debt) & Crafted Amulet Buff
  const [moodPct, setMoodPct] = useState<number>(88);
  const [isExhausted45, setIsExhausted45] = useState<boolean>(false);
  const [debtCoins, setDebtCoins] = useState<number>(0);
  const [debtReasonRu, setDebtReasonRu] = useState<string>('');
  const [hasActiveRoadAmulet, setHasActiveRoadAmulet] = useState<boolean>(false);

  // 5. Owned Shop Items, Item Upgrade Tiers (Tier 1..3), Merchant Claims & NPC Friendship Levels
  const [ownedShopItemIds, setOwnedShopItemIds] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem('chantal_owned_shop_items_v3');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch {
      // ignore
    }
    return ['gear_knitted_scarf'];
  });
  const [shopItemTiers, setShopItemTiers] = useState<Record<string, number>>({
    gear_knitted_scarf: 1,
  });
  const [claimedMerchantOfferIds, setClaimedMerchantOfferIds] = useState<string[]>([]);
  const [npcFriendshipLevels, setNpcFriendshipLevels] = useState<Record<string, number>>({
    npc_bramble: 2,
    npc_corvus: 1,
    npc_torvin: 1,
    npc_hugo: 2,
  });
  const [selectedGiftNpcId, setSelectedGiftNpcId] = useState<string>('npc_bramble');

  const [inventoryItems, setInventoryItems] = useState<InventoryLootItem[]>(() => {
    try {
      const saved = localStorage.getItem('chantal_inventory_v2');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch {
      // ignore
    }
    return [
      {
        id: 'init_den_trophy_1',
        titleRu: 'Фонарик со спящим светлячком-духом',
        category: 'DEN_TROPHY',
        descriptionRu:
          'Резной фонарик из коры ясеня, найденный Шанталь в дупле у ручья. Автоматически стоит на окошке в Домике и засчитан (+1 из 10) в прогресс уровня жилья!',
        sellPriceCoins: 24,
        foundAtRegionRu: 'Янтарная дубрава',
        foundDateIso: '2026-11-05',
        placedInDenComfort: true,
      },
      {
        id: 'init_clue_1',
        titleRu: 'Медальон с полустёртым гербом лилии',
        category: 'ROYAL_CLUE',
        descriptionRu:
          'Старинный серебряный жетон из Похода #1, который откликнулся теплом на прикосновение лапки Шанталь.',
        sellPriceCoins: 40,
        foundAtRegionRu: 'Старая водяная мельница',
        foundDateIso: '2026-11-04',
        inspectedLoreBonusClaimed: false,
      },
      {
        id: 'init_herb_1',
        titleRu: 'Пучок душистого солнечного чабреца',
        category: 'HERB',
        descriptionRu:
          'Согревающая лесная трава от барсука Брамбла. Можно заварить прямо из сумки, чтобы бесплатно снять усталость (45 Энергии), или пустить на крафт оберега!',
        sellPriceCoins: 18,
        foundAtRegionRu: 'Старая водяная мельница',
        foundDateIso: '2026-11-05',
        canHealExhaustion: true,
      },
      {
        id: 'init_loot_1',
        titleRu: 'Светящаяся шишка из Янтарной дубравы',
        category: 'TRINKET',
        descriptionRu:
          'Тёплая кедровая шишка из Похода #2, выменянная у ворона Корвуса. Можно продать скупщику, подарить другу-NPC или соединить с травой в оберег!',
        sellPriceCoins: 18,
        foundAtRegionRu: 'Янтарная дубрава',
        foundDateIso: '2026-11-05',
      },
      {
        id: 'init_loot_2',
        titleRu: 'Пуговица с жилета мельника',
        category: 'TRINKET',
        descriptionRu:
          'Блестящая латунная пуговица из Похода #3, найденная в камышовом затоне вместе с паромщиком Хьюго.',
        sellPriceCoins: 15,
        foundAtRegionRu: 'Старая водяная мельница',
        foundDateIso: '2026-11-06',
      },
    ];
  });
  const [selectedInventoryItemId, setSelectedInventoryItemId] = useState<string | null>(null);
  const [newlyAddedBagItemIds, setNewlyAddedBagItemIds] = useState<string[]>([]);

  // 6. Active 60-Min Journey & Unclaimed Story
  const [activeJourney, setActiveJourney] = useState<ActiveJourneyTimerState | null>(null);
  const [nowEpochMs, setNowEpochMs] = useState<number>(() => Date.now());
  const [activeStoryEpisode, setActiveStoryEpisode] = useState<GeneratedStoryEpisode | null>(null);
  const [isGeneratingStory, setIsGeneratingStory] = useState<boolean>(false);
  const [customFillerInput, setCustomFillerInput] = useState<string>('');

  // 7. Journal History (Seeded with Journeys #1, #2, #3 so 3-Tier Memory has early history from the start!)
  const [journalHistory, setJournalHistory] = useState<GeneratedStoryEpisode[]>(() => {
    try {
      const saved = localStorage.getItem('chantal_journal_v2');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch {
      // ignore
    }
    return INITIAL_SEEDED_JOURNAL_HISTORY;
  });
  const [subView, setSubView] = useState<ChantalSubView>('den');

  // 8. AI Storyteller Config: Default #1 is Direct Gemini ('gemini'), plus OpenRouter & Direct Grok
  const [aiProvider, setAiProvider] = useState<AiProviderId>('gemini');
  const [openRouterModel, setOpenRouterModel] = useState<string>('google/gemini-2.5-flash');
  const [customOpenRouterModelInput, setCustomOpenRouterModelInput] = useState<string>('');
  const [storyDifficulty, setStoryDifficulty] = useState<StoryDifficultyLevel>('MEDIUM');
  const [customLoreFileName, setCustomLoreFileName] = useState<string>('');
  const [customLoreContent, setCustomLoreContent] = useState<string>('');
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Global click-outside handler: clicking anywhere outside [data-popover-interactive="true"] closes open popovers & burger menu
  useEffect(() => {
    const handlePointerDownOutside = (e: PointerEvent) => {
      const target = e.target as HTMLElement | null;
      if (!target) return;
      if (target.closest('[data-popover-interactive="true"]')) return;
      setIsBurgerMenuOpen(false);
      setSelectedShopEntryId(null);
      setSelectedInventoryItemId(null);
      setActiveSlotInspectId(null);
      setActiveDenItemPopoverId(null);
      setSelectedTraitPopupKey(null);
      setSelectedMapRegionId(null);
      setActivePropertyPopup(null);
      setActiveShopPropertyPopup(null);
    };
    document.addEventListener('pointerdown', handlePointerDownOutside);
    return () => document.removeEventListener('pointerdown', handlePointerDownOutside);
  }, []);

  // Helper to position the sliding popover card to the right of any tapped icon without overflowing the mobile screen
  const getAdjacentPopoverClasses = (colIdx: number, mobileCols: number = 3) => {
    const mod = colIdx % mobileCols;
    if (mod === 0) {
      return 'left-[72%] sm:left-[80%] top-0';
    }
    if (mod === mobileCols - 1) {
      return 'right-0 sm:right-auto sm:left-[75%] top-11 sm:top-0';
    }
    return '-left-8 sm:left-[78%] top-2 sm:top-0';
  };

  // Persist core state
  useEffect(() => {
    localStorage.setItem('chantal_prologue_done', String(hasCompletedPrologue));
  }, [hasCompletedPrologue]);

  useEffect(() => {
    localStorage.setItem('chantal_total_journeys', String(totalJourneys));
  }, [totalJourneys]);

  useEffect(() => {
    localStorage.setItem('chantal_maturity_xp', String(maturityXp));
  }, [maturityXp]);

  useEffect(() => {
    if (!recentXpGainBadge) return;
    const timer = window.setTimeout(() => {
      setRecentXpGainBadge(null);
    }, 4500);
    return () => window.clearTimeout(timer);
  }, [recentXpGainBadge]);

  useEffect(() => {
    localStorage.setItem('chantal_traits_v100', JSON.stringify(traits));
  }, [traits]);

  useEffect(() => {
    localStorage.setItem('chantal_pet_used_this_cycle', String(hasUsedPetThisCycle));
  }, [hasUsedPetThisCycle]);

  useEffect(() => {
    try {
      localStorage.setItem('chantal_owned_shop_items_v3', JSON.stringify(ownedShopItemIds));
    } catch {
      // ignore
    }
  }, [ownedShopItemIds]);

  useEffect(() => {
    try {
      localStorage.setItem('chantal_gear_durability', JSON.stringify(gearDurabilityMap));
    } catch {
      // ignore
    }
  }, [gearDurabilityMap]);

  useEffect(() => {
    try {
      localStorage.setItem('chantal_pantry_provisions', JSON.stringify(pantryProvisionCounts));
    } catch {
      // ignore
    }
  }, [pantryProvisionCounts]);

  useEffect(() => {
    try {
      localStorage.setItem('chantal_inventory_v2', JSON.stringify(inventoryItems));
    } catch {
      // ignore
    }
  }, [inventoryItems]);

  useEffect(() => {
    try {
      localStorage.setItem('chantal_journal_v2', JSON.stringify(journalHistory));
    } catch {
      // ignore
    }
  }, [journalHistory]);

  // Live 1-second tick for 60-minute journey countdown
  useEffect(() => {
    if (!activeJourney) return;
    const interval = window.setInterval(() => {
      setNowEpochMs(Date.now());
    }, 1000);
    return () => window.clearInterval(interval);
  }, [activeJourney]);

  // One-time transient notification banner: automatically disappears after 4.5 seconds
  useEffect(() => {
    if (!lastReactionBanner) return;
    const timer = window.setTimeout(() => {
      setLastReactionBanner(null);
    }, 4500);
    return () => window.clearTimeout(timer);
  }, [lastReactionBanner]);

  // Equivalent journeys from accumulated Maturity XP (10 XP = 1 journey equivalent; petting adds +3 XP, journey adds +10/+15 XP)
  const effectiveMaturityJourneys = useMemo(
    () => Math.max(totalJourneys, Math.floor(maturityXp / 10)),
    [totalJourneys, maturityXp]
  );

  // Stage Info (Spoiler-free until 1000 XP / 100 journeys or preview toggle)
  const stageInfo = useMemo(
    () => getChantalStageInfo(effectiveMaturityJourneys, forceTransformPreview),
    [effectiveMaturityJourneys, forceTransformPreview]
  );

  // Detailed Maturity Experience (XP) & Level Progress for the Under-Portrait Progress Bar
  const maturityProgressInfo = useMemo(() => {
    const stageStartXp = stageInfo.currentStageStart * 10;
    const stageEndXp = stageInfo.nextStageTarget * 10;
    const stageTargetXp = Math.max(10, stageEndXp - stageStartXp);
    const xpInStage = Math.min(stageTargetXp, Math.max(0, maturityXp - stageStartXp));
    const progressPct = Math.min(
      100,
      Math.max(3, Math.round((xpInStage / stageTargetXp) * 100))
    );
    const xpToNextStage = Math.max(0, stageEndXp - maturityXp);
    const maturityLevel = Math.floor(maturityXp / 20) + 1;

    return {
      maturityXp,
      maturityLevel,
      xpInStage,
      stageTargetXp,
      progressPct,
      xpToNextStage,
    };
  }, [maturityXp, stageInfo.currentStageStart, stageInfo.nextStageTarget]);

  const awardMaturityXp = (amount: number, sourceRu: string) => {
    setMaturityXp((prev) => prev + amount);
    setRecentXpGainBadge({ amount, sourceRu });
  };

  // Active Gameplay Perks from all 6 Character Traits
  const traitPerks = useMemo(() => computeTraitGameplayPerks(traits), [traits]);

  // Three-Tier Chronicle Memory (Layer 1: All Passports from #1, Layer 2: NPC Dossier, Layer 3: Last 3 Full Texts)
  const threeTierMemory = useMemo(
    () => buildThreeTierChronicleMemory(journalHistory),
    [journalHistory]
  );

  // Streak Tailwind Bonus (Variant A: +25% at 3+ days, +50% at 7+ days, zero penalties)
  const streakMultiplierInfo = useMemo(() => {
    if (currentStreakDays >= 7) {
      return { bonusPct: 50, multiplier: 1.5, labelRu: 'Попутный ветер +50% монет (серия 7+ дн.)' };
    }
    if (currentStreakDays >= 3) {
      return { bonusPct: 25, multiplier: 1.25, labelRu: 'Попутный ветер +25% монет (серия 3+ дн.)' };
    }
    return { bonusPct: 0, multiplier: 1.0, labelRu: 'Серия 3+ дней даст +25% к монетам' };
  }, [currentStreakDays]);

  // Owned Apparel & Den Comforts + Den Trophies brought from Journeys
  const equippedApparelItems = useMemo(
    () =>
      CHANTAL_SHOP_ITEMS.filter(
        (item) => item.category === 'APPAREL' && ownedShopItemIds.includes(item.id)
      ),
    [ownedShopItemIds]
  );

  const ownedDenItems = useMemo(
    () =>
      CHANTAL_SHOP_ITEMS.filter(
        (item) => item.category === 'DEN_COMFORT' && ownedShopItemIds.includes(item.id)
      ),
    [ownedShopItemIds]
  );

  const denTrophiesList = useMemo(
    () => inventoryItems.filter((i) => i.category === 'DEN_TROPHY'),
    [inventoryItems]
  );

  // Den Visual Tier Info (10 items per level: Shop Den Items + Den Trophies from Journeys!)
  const denVisualInfo = useMemo(
    () => getDenVisualTierInfo(ownedDenItems.length, denTrophiesList.length),
    [ownedDenItems.length, denTrophiesList.length]
  );

  // Main illustration: transforms to the new Den Level II / III / IV artwork once 10 / 20 / 30 home items are collected
  const activeMainIllustrationUrl = useMemo(() => {
    if (stageInfo.isTransformed) return stageInfo.portraitUrl;
    if (denVisualInfo.tierNumber >= 2) return denVisualInfo.imageUrl;
    return stageInfo.portraitUrl;
  }, [stageInfo.isTransformed, stageInfo.portraitUrl, denVisualInfo.tierNumber, denVisualInfo.imageUrl]);

  // Passive Coin Bonus from Owned & Upgraded Den Comfort Items + Den Trophies
  const passiveShopCoinsBonus = useMemo(() => {
    const shopBonus = CHANTAL_SHOP_ITEMS.filter(
      (item) =>
        item.category === 'DEN_COMFORT' &&
        ownedShopItemIds.includes(item.id) &&
        item.bonusCoinsPerJourney
    ).reduce((acc, item) => {
      const tier = shopItemTiers[item.id] ?? 1;
      return acc + (item.bonusCoinsPerJourney ?? 0) + (tier - 1) * 1;
    }, 0);
    return shopBonus + denTrophiesList.length;
  }, [ownedShopItemIds, shopItemTiers, denTrophiesList.length]);

  // Active 4-Slot Expedition Loadout Summary (Outfit + Tool + Weapon + Bundle Provision/Loot)
  const activeBackpackLoadoutSummary = useMemo(() => {
    const outfitItem = selectedSlotOutfitId
      ? CHANTAL_SHOP_ITEMS.find(
          (i) =>
            i.id === selectedSlotOutfitId &&
            ownedShopItemIds.includes(i.id) &&
            (gearDurabilityMap[i.id] ?? i.maxDurability ?? 5) > 0
        ) ?? null
      : null;

    const toolItem = selectedSlotToolId
      ? CHANTAL_SHOP_ITEMS.find(
          (i) =>
            i.id === selectedSlotToolId &&
            ownedShopItemIds.includes(i.id) &&
            (gearDurabilityMap[i.id] ?? i.maxDurability ?? 5) > 0
        ) ?? null
      : null;

    const weaponItem = selectedSlotWeaponId
      ? CHANTAL_SHOP_ITEMS.find(
          (i) =>
            i.id === selectedSlotWeaponId &&
            ownedShopItemIds.includes(i.id) &&
            (gearDurabilityMap[i.id] ?? i.maxDurability ?? 5) > 0
        ) ?? null
      : null;

    const activeGearItems = [outfitItem, toolItem, weaponItem].filter(
      (x): x is ChantalShopItem => x !== null
    );

    const activeProvisionItem = selectedBackpackProvisionId
      ? CHANTAL_SHOP_ITEMS.find(
          (i) =>
            i.id === selectedBackpackProvisionId &&
            (pantryProvisionCounts[i.id] ?? 0) > 0
        ) ?? null
      : null;

    const activeBundleLootItem =
      !activeProvisionItem && selectedBundleLootItemId
        ? inventoryItems.find(
            (i) =>
              i.id === selectedBundleLootItemId &&
              (i.category === 'HERB' || i.category === 'TRINKET')
          ) ?? null
        : null;

    let coinMultBonusPct = 0;
    let denTrophyChancePct = 15 + Math.min(15, Math.floor(traits.curiosity / 2));
    let mishapProtectPct = 0;
    let extraFlatCoins = 0;
    let protectsFromExhaustion = false;

    activeGearItems.forEach((g) => {
      const tier = shopItemTiers[g.id] ?? 1;
      coinMultBonusPct += (g.expeditionCoinMultiplierPct ?? 0) + (tier - 1) * 5;
      denTrophyChancePct += g.denTrophyChanceBonusPct ?? 0;
      mishapProtectPct += (g.preventsMishapBonusPct ?? 0) + (tier - 1) * 6;
      extraFlatCoins += (g.bonusCoinsPerJourney ?? 0) + (tier - 1) * 2;
      if (g.id === 'weapon_smoke_acorn') {
        protectsFromExhaustion = true;
      }
    });

    if (activeProvisionItem) {
      coinMultBonusPct += activeProvisionItem.expeditionCoinMultiplierPct ?? 0;
      denTrophyChancePct += activeProvisionItem.denTrophyChanceBonusPct ?? 0;
      if (activeProvisionItem.id === 'supply_honey_pie') {
        protectsFromExhaustion = true;
        coinMultBonusPct += 10;
      }
    } else if (activeBundleLootItem) {
      if (activeBundleLootItem.category === 'HERB') {
        protectsFromExhaustion = true;
        mishapProtectPct += 20;
      } else {
        coinMultBonusPct += 20;
        denTrophyChancePct += 15;
      }
    }

    const allPackedTitlesRu = [
      ...activeGearItems.map((g) => g.titleRu),
      ...(activeProvisionItem ? [activeProvisionItem.titleRu] : []),
      ...(activeBundleLootItem ? [activeBundleLootItem.titleRu] : []),
    ];

    return {
      outfitItem,
      toolItem,
      weaponItem,
      activeGearItems,
      activeProvisionItem,
      activeBundleLootItem,
      allPackedTitlesRu,
      isGoingLight: allPackedTitlesRu.length === 0,
      coinMultBonusPct,
      denTrophyChancePct: Math.min(85, denTrophyChancePct),
      mishapProtectPct: Math.min(90, mishapProtectPct),
      extraFlatCoins,
      protectsFromExhaustion,
    };
  }, [
    selectedSlotOutfitId,
    selectedSlotToolId,
    selectedSlotWeaponId,
    ownedShopItemIds,
    gearDurabilityMap,
    selectedBackpackProvisionId,
    selectedBundleLootItemId,
    pantryProvisionCounts,
    inventoryItems,
    shopItemTiers,
    traits.curiosity,
  ]);

  // Calculate required Energy for the next journey (30 normal, or 45 when exhausted minus home comfort reduction)
  const requiredJourneyEnergy = useMemo(() => {
    if (!isExhausted45) return 30;
    if (denVisualInfo.tierNumber === 4) return 30;
    const comfortDiscount = CHANTAL_SHOP_ITEMS.filter(
      (item) => ownedShopItemIds.includes(item.id) && item.reducesExhaustionCostBy
    ).reduce((acc, item) => acc + (item.reducesExhaustionCostBy ?? 0), 0);
    return Math.max(30, 45 - comfortDiscount);
  }, [isExhausted45, ownedShopItemIds, denVisualInfo.tierNumber]);

  // Time of day atmosphere
  const timeOfDayInfo = useMemo(() => {
    const h = new Date().getHours();
    if (h >= 5 && h < 12) {
      return { labelRu: 'Утренний туман над холмами', dailyArcStep: 1 as const };
    }
    if (h >= 12 && h < 18) {
      return { labelRu: 'Тёплый солнечный полдень', dailyArcStep: 2 as const };
    }
    return { labelRu: 'Вечер у камина и звёздные тропы', dailyArcStep: 3 as const };
  }, []);

  const royalCluesList = useMemo(
    () => inventoryItems.filter((i) => i.category === 'ROYAL_CLUE'),
    [inventoryItems]
  );
  const herbsList = useMemo(
    () => inventoryItems.filter((i) => i.category === 'HERB'),
    [inventoryItems]
  );
  const trinketsList = useMemo(
    () => inventoryItems.filter((i) => i.category === 'TRINKET'),
    [inventoryItems]
  );

  // Currently selected item for the Inventory Item Description Inspector Card
  const selectedInventoryItem = useMemo<InventoryLootItem | null>(() => {
    if (inventoryItems.length === 0) return null;
    return (
      inventoryItems.find((item) => item.id === selectedInventoryItemId) ??
      inventoryItems[0]
    );
  }, [inventoryItems, selectedInventoryItemId]);

  const elapsedMs = activeJourney ? Math.max(0, nowEpochMs - activeJourney.startedAtEpochMs) : 0;
  const remainingMs = activeJourney ? Math.max(0, activeJourney.durationMs - elapsedMs) : 0;
  const isJourneyFinished = activeJourney !== null && remainingMs <= 0;
  const remainingMinutes = Math.floor(remainingMs / 60000);
  const remainingSeconds = Math.floor((remainingMs % 60000) / 1000);

  const activeRoadNoteRu = useMemo(() => {
    if (!activeJourney) return '';
    const minsElapsed = Math.floor(elapsedMs / 60000);
    if (minsElapsed < 15) {
      return (
        activeJourney.autonomousReasonRu ||
        `0–15 мин: Шанталь бодро выбежала за калитку и сама свернула по карте в сторону «${activeJourney.regionTitleRu}».`
      );
    }
    if (minsElapsed < 30) {
      return `15–30 мин: Идёт по тропе в «${activeJourney.regionTitleRu}», изучая следы и тайники по дороге.`;
    }
    if (minsElapsed < 45) {
      return `30–45 мин: Встретила персонажа ${activeJourney.encounteredNpcRu} — там в самом разгаре важное дело!`;
    }
    return `45–60 мин: Сумка полна новостей и находок — Шанталь уже бежит по обратной тропинке к дому!`;
  }, [activeJourney, elapsedMs]);

  // Show temporary arrow badge & record visual trait change history when traits change (0–100 scale)
  const triggerTraitToast = (
    deltas: Partial<ChantalTraits>,
    choiceLabelRu: string = 'Событие в Домике',
    storyTitleRu: string = 'Жизнь Шанталь',
    prevSnapshot?: ChantalTraits
  ) => {
    const basePrev = prevSnapshot ?? traits;
    const computedNext: ChantalTraits = {
      curiosity: Math.min(100, Math.max(1, basePrev.curiosity + (deltas.curiosity || 0))),
      tenderness: Math.min(100, Math.max(1, basePrev.tenderness + (deltas.tenderness || 0))),
      playfulness: Math.min(100, Math.max(1, basePrev.playfulness + (deltas.playfulness || 0))),
      romanticism: Math.min(100, Math.max(1, basePrev.romanticism + (deltas.romanticism || 0))),
      courage: Math.min(100, Math.max(1, basePrev.courage + (deltas.courage || 0))),
      wit: Math.min(100, Math.max(1, basePrev.wit + (deltas.wit || 0))),
    };
    setLastTraitChangeRecord({
      deltas,
      previousTraits: basePrev,
      newTraits: computedNext,
      choiceLabelRu,
      storyTitleRu,
    });

    if (!showTraitArrowsSetting) return;
    const formatted = formatTraitDeltasBadge(deltas);
    if (formatted.length === 0) return;
    setActiveTraitToast(formatted);
    window.setTimeout(() => {
      setActiveTraitToast(null);
    }, 5500);
  };

  const applyTraitDeltasFromChoice = (
    deltas: Partial<ChantalTraits>,
    choiceLabelRu: string,
    storyTitleRu: string
  ) => {
    const prevSnapshot = { ...traits };
    const nextSnapshot: ChantalTraits = {
      curiosity: Math.min(100, Math.max(1, prevSnapshot.curiosity + (deltas.curiosity || 0))),
      tenderness: Math.min(100, Math.max(1, prevSnapshot.tenderness + (deltas.tenderness || 0))),
      playfulness: Math.min(100, Math.max(1, prevSnapshot.playfulness + (deltas.playfulness || 0))),
      romanticism: Math.min(100, Math.max(1, prevSnapshot.romanticism + (deltas.romanticism || 0))),
      courage: Math.min(100, Math.max(1, prevSnapshot.courage + (deltas.courage || 0))),
      wit: Math.min(100, Math.max(1, prevSnapshot.wit + (deltas.wit || 0))),
    };
    setTraits(nextSnapshot);
    triggerTraitToast(deltas, choiceLabelRu, storyTitleRu, prevSnapshot);
  };

  // Interactive Heart Icon Tap ("Погладить за ушком") — strictly 1 time per journey return! Awards +3 Maturity XP!
  const handlePetOrTalkWithChantal = () => {
    if (hasUsedPetThisCycle || activeJourney !== null) return;
    if (soundEnabled) onPlayTone('relax');
    const nextIdx = petVariantCounter + 1;
    setPetVariantCounter(nextIdx);
    setHasUsedPetThisCycle(true);
    awardMaturityXp(3, 'Ласка ❤️');
    const dialogue = getInteractivePettingDialogue(traits, stageInfo.isTransformed, nextIdx);
    setPetSpeechBubble({
      textRu: dialogue.reactionTextRu,
      badgeRu: `${dialogue.traitBadgeRu} · +3 XP взросления`,
    });
    setMoodPct((prev) => Math.min(100, prev + traitPerks.tendernessMoodHealBonusPct));
    setIsHeartPulseActive(true);
    window.setTimeout(() => setIsHeartPulseActive(false), 1400);
  };

  // Toggle packing an APPAREL item into its dedicated slot (OUTFIT, TOOL, or WEAPON)
  const handleToggleBackpackGear = (itemId: string) => {
    const item = CHANTAL_SHOP_ITEMS.find((i) => i.id === itemId);
    if (!item) return;
    const remDur = gearDurabilityMap[itemId] ?? item.maxDurability ?? 5;
    if (remDur <= 0) return;

    const slot = item.loadoutSlotType ?? 'OUTFIT';
    if (slot === 'OUTFIT') {
      setSelectedSlotOutfitId((prev) => (prev === itemId ? null : itemId));
    } else if (slot === 'TOOL') {
      setSelectedSlotToolId((prev) => (prev === itemId ? null : itemId));
    } else if (slot === 'WEAPON') {
      setSelectedSlotWeaponId((prev) => (prev === itemId ? null : itemId));
    }
  };

  // Refill / repair expedition gear durability for refillCostCoins
  const handleRefillGearDurability = (item: ChantalShopItem) => {
    const cost = item.refillCostCoins ?? 12;
    const maxDur = item.maxDurability ?? 5;
    if (coins < cost) return;
    if (soundEnabled) onPlayTone('complete');
    setCoins((prev) => prev - cost);
    setGearDurabilityMap((prev) => ({ ...prev, [item.id]: maxDur }));
    setLastReactionBanner(
      `Снаряжение «${item.titleRu}» приведено в порядок (${maxDur}/${maxDur} походов)!`
    );
  };

  // Toggle packing a road provision into the bundle (up to 1 provision or 1 loot item)
  const handleToggleBackpackProvision = (itemId: string) => {
    const count = pantryProvisionCounts[itemId] ?? 0;
    if (count <= 0) return;
    setSelectedBundleLootItemId(null);
    setSelectedBackpackProvisionId((prev) => (prev === itemId ? null : itemId));
  };

  // Toggle packing a found Herb or Trinket from the Bag into the road bundle
  const handleToggleBundleLootItem = (lootId: string) => {
    setSelectedBackpackProvisionId(null);
    setSelectedBundleLootItemId((prev) => (prev === lootId ? null : lootId));
  };

  // Start autonomous journey (1 hour timer) — Chantal can go light OR take packed gear & bundle!
  const handleStartAutonomousJourney = (isFiller: boolean = false, fillerText?: string) => {
    if (energy < requiredJourneyEnergy) return;
    if (soundEnabled) onPlayTone('hold');

    setEnergy((prev) => Math.max(0, prev - requiredJourneyEnergy));
    setIsExhausted45(false);
    setLastReactionBanner(null);
    setActiveLoadoutSlotPicker('NONE');

    const activeGearIds = activeBackpackLoadoutSummary.activeGearItems.map((g) => g.id);
    const activeProvId = activeBackpackLoadoutSummary.activeProvisionItem?.id ?? null;
    const activeLootItem = activeBackpackLoadoutSummary.activeBundleLootItem;
    const packedTitles = activeBackpackLoadoutSummary.allPackedTitlesRu;

    // Consume 1 durability charge per packed gear item
    if (activeGearIds.length > 0) {
      setGearDurabilityMap((prev) => {
        const next = { ...prev };
        activeGearIds.forEach((id) => {
          const item = CHANTAL_SHOP_ITEMS.find((i) => i.id === id);
          const cur = next[id] ?? item?.maxDurability ?? 5;
          next[id] = Math.max(0, cur - 1);
        });
        return next;
      });
    }

    // Consume 1 road provision from pantry OR 1 packed loot item from bag
    if (activeProvId) {
      setPantryProvisionCounts((prev) => {
        const cur = prev[activeProvId] ?? 0;
        const nextCount = Math.max(0, cur - 1);
        if (nextCount === 0) {
          setSelectedBackpackProvisionId(null);
        }
        return { ...prev, [activeProvId]: nextCount };
      });
    } else if (activeLootItem) {
      setInventoryItems((prev) => prev.filter((i) => i.id !== activeLootItem.id));
      setSelectedBundleLootItemId(null);
    }

    const lastEpisode = journalHistory[0];
    const autonomousDecision = pickAutonomousRegionForChantal(
      totalJourneys,
      lastEpisode?.regionId,
      lastEpisode?.encounteredNpcRu,
      debtCoins > 0
    );

    setActiveJourney({
      startedAtEpochMs: Date.now(),
      durationMs: 60 * 60 * 1000, // 60 minutes real-time
      regionId: autonomousDecision.region.id,
      regionTitleRu: autonomousDecision.region.titleRu,
      encounteredNpcRu: autonomousDecision.encounteredNpcRu,
      npcRoleRu: autonomousDecision.npcRoleRu,
      dailyArcStep: timeOfDayInfo.dailyArcStep,
      isFillerEpisode: isFiller,
      customFillerPrompt: fillerText,
      stayedInSameRegion: autonomousDecision.stayedInSameRegion,
      autonomousReasonRu: autonomousDecision.decisionReasonRu,
      backpackGearIds: activeGearIds,
      backpackProvisionId: activeProvId ?? activeLootItem?.id ?? null,
      backpackGearTitlesRu: packedTitles,
    });
    setNowEpochMs(Date.now());
  };

  // Generate story when Chantal returns from her journey (via /api/chantal/generate-story or fallback)
  const handleFinishJourneyAndReadStory = async (
    journeyToResolve: ActiveJourneyTimerState,
    forceMilestoneOverride: boolean = false
  ) => {
    setIsGeneratingStory(true);
    const regionObj =
      WORLD_MAP_REGIONS.find((r) => r.id === journeyToResolve.regionId) ?? WORLD_MAP_REGIONS[0];
    const nextJourneyNum = forceMilestoneOverride
      ? Math.ceil((totalJourneys + 1) / 5) * 5
      : journeyToResolve.isFillerEpisode
      ? totalJourneys
      : totalJourneys + 1;
    const isMilestone = !journeyToResolve.isFillerEpisode && nextJourneyNum % 5 === 0;
    const effectiveOpenRouterModel =
      customOpenRouterModelInput.trim() || openRouterModel;

    const amuletMult = hasActiveRoadAmulet
      ? ownedShopItemIds.includes('den_alchemy_table')
        ? 1.75
        : 1.5
      : 1.0;

    const packedTitlesForStory =
      journeyToResolve.backpackGearTitlesRu ??
      activeBackpackLoadoutSummary.allPackedTitlesRu;

    const guaranteedDenTrophy =
      !isMilestone &&
      !journeyToResolve.isFillerEpisode &&
      Math.random() * 100 < activeBackpackLoadoutSummary.denTrophyChancePct;

    const expeditionMult = 1 + activeBackpackLoadoutSummary.coinMultBonusPct / 100;
    const expeditionFlat = activeBackpackLoadoutSummary.extraFlatCoins;

    try {
      const response = await fetch('/api/chantal/generate-story', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          provider: aiProvider,
          openRouterModel: effectiveOpenRouterModel,
          difficulty: storyDifficulty,
          stageNumber: stageInfo.stageNumber,
          isTransformed: stageInfo.isTransformed,
          totalJourneys: nextJourneyNum,
          dailyArcStep: journeyToResolve.dailyArcStep,
          timeOfDayRu: timeOfDayInfo.labelRu,
          regionTitleRu: regionObj.titleRu,
          regionLoreRu: regionObj.loreRu,
          encounteredNpcRu: journeyToResolve.encounteredNpcRu || regionObj.residentNpcRu,
          stayedInSameRegion: journeyToResolve.stayedInSameRegion,
          autonomousReasonRu: journeyToResolve.autonomousReasonRu,
          equippedItemsRu: equippedApparelItems.map((i) => i.titleRu),
          backpackGearRu: packedTitlesForStory,
          denItemsRu: [
            ...ownedDenItems.map((i) => i.titleRu),
            ...denTrophiesList.map((t) => t.titleRu),
          ],
          denTierNumber: denVisualInfo.tierNumber,
          denProgressTextRu: `${denVisualInfo.itemsInCurrentStep}/10`,
          guaranteedDenTrophy,
          traits,
          isFillerEpisode: journeyToResolve.isFillerEpisode,
          customFillerPrompt: journeyToResolve.customFillerPrompt,
          customLoreText: customLoreContent,
          chronicleMemory: threeTierMemory,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      const raw = data.story;
      const nowIso = new Date().toISOString().slice(0, 10);

      let resolvedOutcome: 'GOOD_REWARD' | 'BAD_DEBT' | 'BAD_EXHAUSTION' =
        isMilestone || hasActiveRoadAmulet
          ? 'GOOD_REWARD'
          : raw.outcomeType === 'BAD_DEBT' || raw.outcomeType === 'BAD_EXHAUSTION'
          ? raw.outcomeType
          : 'GOOD_REWARD';
      let resolvedBadNewsRu = resolvedOutcome === 'GOOD_REWARD' ? '' : raw.badNewsTitleRu || '';
      let resolvedDebtCoins = resolvedOutcome === 'GOOD_REWARD' ? 0 : Number(raw.debtAmountCoins) || 0;

      // Protection from packed expedition gear & provisions
      if (
        resolvedOutcome !== 'GOOD_REWARD' &&
        Math.random() * 100 < activeBackpackLoadoutSummary.mishapProtectPct
      ) {
        resolvedOutcome = 'GOOD_REWARD';
        resolvedDebtCoins = 0;
        resolvedBadNewsRu = '';
      }
      if (
        resolvedOutcome === 'BAD_EXHAUSTION' &&
        activeBackpackLoadoutSummary.protectsFromExhaustion
      ) {
        resolvedOutcome = 'GOOD_REWARD';
        resolvedBadNewsRu = '';
      }

      if (
        resolvedOutcome === 'BAD_DEBT' &&
        Math.random() * 100 < traitPerks.playfulnessCharmOutOfDebtChancePct
      ) {
        resolvedOutcome = 'GOOD_REWARD';
        resolvedDebtCoins = 0;
        resolvedBadNewsRu = '';
      }
      if (
        resolvedOutcome === 'BAD_EXHAUSTION' &&
        Math.random() * 100 < traitPerks.courageAvoidExhaustionChancePct
      ) {
        resolvedOutcome = 'GOOD_REWARD';
        resolvedBadNewsRu = '';
      }

      // Balanced session coin economy (~18-22 base coins so 3 sessions = ~55-65 coins = 1 home item per day)
      const rawCoinsClamped = Math.min(24, Math.max(15, Number(raw.coinsEarned) || 18));
      const baseCoins = rawCoinsClamped * (isMilestone ? 1.8 : 1);
      const romanticEveningMult =
        journeyToResolve.dailyArcStep === 3
          ? 1 + traitPerks.romanticismEveningCoinBonusPct / 100
          : 1;
      const finalCoins = Math.round(
        (baseCoins + traitPerks.curiosityBonusCoins + passiveShopCoinsBonus + expeditionFlat) *
          streakMultiplierInfo.multiplier *
          romanticEveningMult *
          amuletMult *
          expeditionMult
      );

      const lootCat: InventoryLootItem['category'] = isMilestone
        ? 'ROYAL_CLUE'
        : guaranteedDenTrophy || raw.lootItem?.category === 'DEN_TROPHY'
        ? 'DEN_TROPHY'
        : raw.lootItem?.category === 'HERB' || raw.lootItem?.category === 'ROYAL_CLUE'
        ? raw.lootItem.category
        : 'TRINKET';

      const baseChoices =
        Array.isArray(raw.choices) && raw.choices.length >= 3 ? raw.choices.slice(0, 3) : [];
      if (isMilestone) {
        baseChoices.push({
          labelRu: `★ Открыть потайное отделение Юбилейного Сундука Главы #${nextJourneyNum}`,
          followUpReactionRu:
            'Крышка кованого сундука со звоном откидывается: вы нашли королевскую реликвию и двойную казну монет!',
          traitDeltas: {
            curiosity: 2,
            tenderness: 1,
            playfulness: 1,
            romanticism: 1,
            courage: 2,
            wit: 1,
          },
        });
      }

      const episode: GeneratedStoryEpisode = {
        id: `ep_${Date.now()}`,
        journeyNumber: nextJourneyNum,
        createdAtIso: nowIso,
        titleRu: isMilestone
          ? `★ Юбилейный Сундук Главы #${nextJourneyNum} · ${raw.titleRu || regionObj.titleRu}`
          : raw.titleRu || `Путешествие #${nextJourneyNum} в «${regionObj.titleRu}»`,
        storyTextRu: raw.storyTextRu,
        chantalMapNoteRu:
          raw.chantalMapNoteRu ||
          journeyToResolve.autonomousReasonRu ||
          `Шанталь сама выбрала тропу в «${regionObj.titleRu}» и встретила там персонажа ${journeyToResolve.encounteredNpcRu}.`,
        pastEchoNoteRu:
          raw.pastEchoNoteRu ||
          'Связано с предыдущими решениями из летописи странствий Шанталь.',
        regionId: regionObj.id,
        regionTitleRu: regionObj.titleRu,
        encounteredNpcRu: journeyToResolve.encounteredNpcRu || regionObj.residentNpcRu,
        dailyArcStep: journeyToResolve.dailyArcStep,
        isFillerEpisode: journeyToResolve.isFillerEpisode,
        customFillerPrompt: journeyToResolve.customFillerPrompt,
        stayedInSameRegion: journeyToResolve.stayedInSameRegion,
        autonomousReasonRu: journeyToResolve.autonomousReasonRu,
        isMilestoneChest: isMilestone,
        lootItem: {
          id: `loot_${Date.now()}`,
          titleRu: isMilestone
            ? `Сокровище Сундука Главы #${nextJourneyNum}: ${raw.lootItem?.titleRu || 'Королевская печать с лилией'}`
            : raw.lootItem?.titleRu ||
              (lootCat === 'DEN_TROPHY'
                ? 'Резной фонарик со светлячком для Домика'
                : 'Диковинка с лесной тропы'),
          category: lootCat,
          descriptionRu:
            raw.lootItem?.descriptionRu ||
            (lootCat === 'DEN_TROPHY'
              ? 'Уютная вещь для обустройства Домика, найденная Шанталь в походе (+1 в счётчик уровня жилья X/10)!'
              : 'Необычная вещь, которую Шанталь бережно принесла в сумке из своего приключения.'),
          sellPriceCoins: Number(raw.lootItem?.sellPriceCoins) || 20,
          foundAtRegionRu: regionObj.titleRu,
          foundDateIso: nowIso,
          canHealExhaustion: lootCat === 'HERB',
          placedInDenComfort: lootCat === 'DEN_TROPHY',
        },
        outcomeType: resolvedOutcome,
        coinsEarned: finalCoins,
        badNewsTitleRu: resolvedBadNewsRu,
        debtAmountCoins: resolvedDebtCoins,
        choices: baseChoices,
        aiSourceLabel: data.source || 'Gemini Flash',
      };

      setHasActiveRoadAmulet(false);
      setHasUsedPetThisCycle(false);
      if (resolvedOutcome !== 'GOOD_REWARD') {
        setMoodPct((prev) => Math.max(45, prev - 25));
      }
      setActiveStoryEpisode(episode);
      setActiveJourney(null);
      if (soundEnabled) onPlayTone('complete');
    } catch {
      const fallback = buildFallbackStoryEpisode({
        region: regionObj,
        encounteredNpcRu: journeyToResolve.encounteredNpcRu,
        autonomousReasonRu: journeyToResolve.autonomousReasonRu,
        stayedInSameRegion: journeyToResolve.stayedInSameRegion,
        dailyArcStep: journeyToResolve.dailyArcStep,
        isFillerEpisode: journeyToResolve.isFillerEpisode,
        customFillerPrompt: journeyToResolve.customFillerPrompt,
        isTransformed: stageInfo.isTransformed,
        totalJourneys: nextJourneyNum,
        traits,
        chronicleMemory: threeTierMemory,
        backpackGearRu: packedTitlesForStory,
        guaranteedDenTrophy,
      });
      if (
        hasActiveRoadAmulet ||
        Math.random() * 100 < activeBackpackLoadoutSummary.mishapProtectPct
      ) {
        fallback.outcomeType = 'GOOD_REWARD';
        fallback.badNewsTitleRu = '';
        fallback.debtAmountCoins = 0;
      }
      if (
        fallback.outcomeType === 'BAD_EXHAUSTION' &&
        activeBackpackLoadoutSummary.protectsFromExhaustion
      ) {
        fallback.outcomeType = 'GOOD_REWARD';
        fallback.badNewsTitleRu = '';
      }
      const romanticEveningMult =
        journeyToResolve.dailyArcStep === 3
          ? 1 + traitPerks.romanticismEveningCoinBonusPct / 100
          : 1;
      fallback.coinsEarned = Math.round(
        (fallback.coinsEarned +
          traitPerks.curiosityBonusCoins +
          passiveShopCoinsBonus +
          expeditionFlat) *
          streakMultiplierInfo.multiplier *
          romanticEveningMult *
          amuletMult *
          expeditionMult
      );
      setHasActiveRoadAmulet(false);
      setHasUsedPetThisCycle(false);
      if (fallback.outcomeType !== 'GOOD_REWARD') {
        setMoodPct((prev) => Math.max(45, prev - 25));
      }
      setActiveStoryEpisode(fallback);
      setActiveJourney(null);
      if (soundEnabled) onPlayTone('complete');
    } finally {
      setIsGeneratingStory(false);
    }
  };

  // Resolve Canonical Choice Button (Advances main story + modifies 6 traits + shows 2-4 sentence Chantal reaction)
  const handleSelectCanonicalChoice = (choiceIdx: number) => {
    if (!activeStoryEpisode) return;
    const chosen = activeStoryEpisode.choices[choiceIdx];
    if (!chosen) return;

    if (soundEnabled) onPlayTone('salute');

    const prevSnapshot = { ...traits };
    const nextSnapshot: ChantalTraits = {
      curiosity: Math.max(1, prevSnapshot.curiosity + (chosen.traitDeltas.curiosity || 0)),
      tenderness: Math.max(1, prevSnapshot.tenderness + (chosen.traitDeltas.tenderness || 0)),
      playfulness: Math.max(1, prevSnapshot.playfulness + (chosen.traitDeltas.playfulness || 0)),
      romanticism: Math.max(1, prevSnapshot.romanticism + (chosen.traitDeltas.romanticism || 0)),
      courage: Math.max(1, prevSnapshot.courage + (chosen.traitDeltas.courage || 0)),
      wit: Math.max(1, prevSnapshot.wit + (chosen.traitDeltas.wit || 0)),
    };

    setInventoryItems((prev) => [activeStoryEpisode.lootItem, ...prev]);
    setNewlyAddedBagItemIds((prev) =>
      prev.includes(activeStoryEpisode.lootItem.id)
        ? prev
        : [activeStoryEpisode.lootItem.id, ...prev]
    );
    setSelectedInventoryItemId(activeStoryEpisode.lootItem.id);
    setCoins((prev) => prev + activeStoryEpisode.coinsEarned);

    if (activeStoryEpisode.outcomeType === 'BAD_DEBT' && activeStoryEpisode.debtAmountCoins > 0) {
      setDebtCoins((prev) => prev + activeStoryEpisode.debtAmountCoins);
      setDebtReasonRu(
        activeStoryEpisode.badNewsTitleRu || 'Непредвиденный долг на ярмарке'
      );
    } else if (activeStoryEpisode.outcomeType === 'BAD_EXHAUSTION') {
      setIsExhausted45(true);
    }

    if (!activeStoryEpisode.isFillerEpisode) {
      applyTraitDeltasFromChoice(
        chosen.traitDeltas,
        chosen.labelRu,
        activeStoryEpisode.titleRu
      );
      setTotalJourneys(activeStoryEpisode.journeyNumber);
    }

    const completedEp: GeneratedStoryEpisode = {
      ...activeStoryEpisode,
      selectedChoiceIndex: choiceIdx,
    };
    setJournalHistory((prev) => [completedEp, ...prev]);
    setExpandedJournalEpisodeId(completedEp.id);
    const nextTotalDen =
      activeStoryEpisode.lootItem.category === 'DEN_TROPHY'
        ? ownedDenItems.length + denTrophiesList.length + 1
        : ownedDenItems.length + denTrophiesList.length;
    const stepInTen = nextTotalDen >= 30 ? 10 : nextTotalDen % 10 || 10;

    const xpEarned = activeStoryEpisode.isMilestoneChest
      ? 15
      : activeStoryEpisode.isFillerEpisode
      ? 8
      : 10;
    awardMaturityXp(xpEarned, 'Поход 🧭');

    setLastChoiceReactionCard({
      episodeTitleRu: activeStoryEpisode.titleRu,
      journeyNumber: activeStoryEpisode.journeyNumber,
      regionTitleRu: activeStoryEpisode.regionTitleRu,
      playerChoiceRu: chosen.labelRu,
      chantalReactionRu: chosen.followUpReactionRu,
      deltas: chosen.traitDeltas,
      previousTraits: prevSnapshot,
      newTraits: nextSnapshot,
      coinsEarned: activeStoryEpisode.coinsEarned,
      xpEarned,
      lootItem: activeStoryEpisode.lootItem,
      denStepCountTextRu:
        activeStoryEpisode.lootItem.category === 'DEN_TROPHY'
          ? `${stepInTen} из 10`
          : undefined,
      isMilestoneChest: activeStoryEpisode.isMilestoneChest,
      isFillerEpisode: activeStoryEpisode.isFillerEpisode,
    });

    setActiveStoryEpisode(null);
    setMoodPct((m) => Math.min(100, m + 6));
  };

  // Resolve Free Text Input ("Свой текст" -> Filler response / side action, DOES NOT change traits or main plot!)
  const handleSendCustomFillerResponse = () => {
    const trimmed = customFillerInput.trim();
    if (!trimmed || !activeStoryEpisode) return;
    if (soundEnabled) onPlayTone('salute');

    const prevSnapshot = { ...traits };
    setInventoryItems((prev) => [activeStoryEpisode.lootItem, ...prev]);
    setNewlyAddedBagItemIds((prev) =>
      prev.includes(activeStoryEpisode.lootItem.id)
        ? prev
        : [activeStoryEpisode.lootItem.id, ...prev]
    );
    setSelectedInventoryItemId(activeStoryEpisode.lootItem.id);
    setCoins((prev) => prev + activeStoryEpisode.coinsEarned);

    if (debtCoins > 0) {
      setDebtCoins(0);
      setDebtReasonRu('');
    }

    const nextTotalDen =
      activeStoryEpisode.lootItem.category === 'DEN_TROPHY'
        ? ownedDenItems.length + denTrophiesList.length + 1
        : ownedDenItems.length + denTrophiesList.length;
    const stepInTen = nextTotalDen >= 30 ? 10 : nextTotalDen % 10 || 10;

    const completedEp: GeneratedStoryEpisode = {
      ...activeStoryEpisode,
      isFillerEpisode: true,
      userCustomResponseText: trimmed,
    };
    setJournalHistory((prev) => [completedEp, ...prev]);
    const xpEarned = 8;
    awardMaturityXp(xpEarned, 'Поручение 🪶');

    setLastChoiceReactionCard({
      episodeTitleRu: activeStoryEpisode.titleRu,
      journeyNumber: activeStoryEpisode.journeyNumber,
      regionTitleRu: activeStoryEpisode.regionTitleRu,
      playerChoiceRu: trimmed,
      chantalReactionRu:
        'Шанталь внимательно выслушала твоё особое поручение, радостно тявкнула и аккуратно выложила на стол принесённые из похода монеты и находку!',
      deltas: {},
      previousTraits: prevSnapshot,
      newTraits: prevSnapshot,
      coinsEarned: activeStoryEpisode.coinsEarned,
      xpEarned,
      lootItem: activeStoryEpisode.lootItem,
      denStepCountTextRu:
        activeStoryEpisode.lootItem.category === 'DEN_TROPHY'
          ? `${stepInTen} из 10`
          : undefined,
      isMilestoneChest: activeStoryEpisode.isMilestoneChest,
      isFillerEpisode: true,
    });
    setCustomFillerInput('');
    setActiveStoryEpisode(null);
  };

  // Handle Custom Lore File Upload (.txt / .md / .json)
  const handleLoreFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const content = typeof reader.result === 'string' ? reader.result : '';
      setCustomLoreFileName(file.name);
      setCustomLoreContent(content);
    };
    reader.readAsText(file, 'utf-8');
  };

  // Buy or Upgrade item in Shop (or Pack Road Provision into Pantry)
  const handleBuyShopItem = (item: ChantalShopItem, packForExpedition: boolean = false) => {
    if (coins < item.priceCoins) return;
    if (soundEnabled) onPlayTone('complete');
    setCoins((prev) => prev - item.priceCoins);

    if (item.category === 'CONSUMABLE') {
      if (packForExpedition || item.instantAction === 'PACK_PROVISION') {
        setPantryProvisionCounts((prev) => ({
          ...prev,
          [item.id]: (prev[item.id] ?? 0) + 1,
        }));
        setSelectedBackpackProvisionId(item.id);
        setLastReactionBanner(
          `Гостинец «${item.titleRu}» куплен и положен в походный узелок Шанталь!`
        );
        return;
      }

      if (item.instantAction === 'HEAL_EXHAUSTION') {
        setIsExhausted45(false);
        setLastReactionBanner('Тёплый бальзам согрел Шанталь! Цена похода снова 30 Энергии.');
      } else if (item.instantAction === 'BOOST_MOOD') {
        setMoodPct(100);
        setEnergy((prev) => prev + 5);
        setLastReactionBanner('Шанталь с аппетитом съела ежевичный пирожок (+5 Энергии, 100% настроение)!');
      }
      return;
    }

    if (!ownedShopItemIds.includes(item.id)) {
      const nextOwnedIds = [...ownedShopItemIds, item.id];
      setOwnedShopItemIds(nextOwnedIds);
      setShopItemTiers((prev) => ({ ...prev, [item.id]: 1 }));
      setMoodPct((m) => Math.min(100, m + 8));

      if (item.category === 'APPAREL') {
        const maxDur = item.maxDurability ?? 5;
        setGearDurabilityMap((prev) => ({ ...prev, [item.id]: maxDur }));
        const slot = item.loadoutSlotType ?? 'OUTFIT';
        if (slot === 'OUTFIT') setSelectedSlotOutfitId(item.id);
        else if (slot === 'TOOL') setSelectedSlotToolId(item.id);
        else if (slot === 'WEAPON') setSelectedSlotWeaponId(item.id);
        setLastReactionBanner(
          `Куплено и надето «${item.titleRu}» (запас ${maxDur}/${maxDur} походов)!`
        );
      } else if (item.category === 'DEN_COMFORT') {
        const nextShopDenCount = CHANTAL_SHOP_ITEMS.filter(
          (i) => i.category === 'DEN_COMFORT' && nextOwnedIds.includes(i.id)
        ).length;
        const nextTotalDen = nextShopDenCount + denTrophiesList.length;
        const nextInfo = getDenVisualTierInfo(nextShopDenCount, denTrophiesList.length);
        if (nextTotalDen === 10 || nextTotalDen === 20 || nextTotalDen === 30) {
          setLastReactionBanner(
            `🎉 Собрано 10 из 10 предметов уровня! Жилище Шанталь преобразилось до Уровня ${nextInfo.tierNumber} («${nextInfo.titleRu}»)!`
          );
        } else {
          setLastReactionBanner(
            `🏠 Куплено «${item.titleRu}»! Прогресс уровня жилья: ${nextInfo.itemsInCurrentStep} / 10 (${nextShopDenCount} куплено + ${denTrophiesList.length} из походов).`
          );
        }
      }
    }
  };

  const handleUpgradeShopItemTier = (item: ChantalShopItem) => {
    const currentTier = shopItemTiers[item.id] ?? 1;
    const maxTier = item.maxTier ?? 3;
    const upgradeCost = (item.upgradeCostCoins ?? 40) * currentTier;
    if (currentTier >= maxTier || coins < upgradeCost) return;

    if (soundEnabled) onPlayTone('salute');
    setCoins((prev) => prev - upgradeCost);
    const nextTier = currentTier + 1;
    setShopItemTiers((prev) => ({ ...prev, [item.id]: nextTier }));
    if (item.category === 'APPAREL') {
      setGearDurabilityMap((prev) => ({
        ...prev,
        [item.id]: item.maxDurability ?? 5,
      }));
    }
    setLastReactionBanner(
      `Предмет «${item.titleRu}» улучшен до Тира ${nextTier} (Мастерское качество)!`
    );
  };

  // Buy from Traveling Merchant
  const handleBuyTravelingMerchantOffer = (offer: TravelingMerchantOffer) => {
    if (coins < offer.discountPriceCoins || claimedMerchantOfferIds.includes(offer.id)) return;
    if (soundEnabled) onPlayTone('complete');
    setCoins((prev) => prev - offer.discountPriceCoins);
    setClaimedMerchantOfferIds((prev) => [...prev, offer.id]);
    const nowIso = new Date().toISOString().slice(0, 10);

    if (offer.rewardType === 'DEN_TROPHY_CRATE') {
      const newTrophy: InventoryLootItem = {
        id: `merch_den_trophy_${Date.now()}`,
        titleRu: 'Резной фонарик со светлячком из фургона',
        category: 'DEN_TROPHY',
        descriptionRu:
          'Диковинный лесной ночник, выкупленный в Лавке на колёсах. Сразу занял место в Домике и прибавил +1 к счётчику уровня жилья (X/10)!',
        sellPriceCoins: 28,
        foundAtRegionRu: 'Лавка на колёсах',
        foundDateIso: nowIso,
        placedInDenComfort: true,
      };
      setInventoryItems((prev) => [newTrophy, ...prev]);
      setNewlyAddedBagItemIds((prev) => [newTrophy.id, ...prev]);
      setSelectedInventoryItemId(newTrophy.id);
      const nextInfo = getDenVisualTierInfo(ownedDenItems.length, denTrophiesList.length + 1);
      setLastReactionBanner(
        `🏠 Резной фонарик поставлен в Домик! Счётчик уровня жилья: ${nextInfo.itemsInCurrentStep} / 10.`
      );
    } else if (offer.rewardType === 'ROYAL_CLUE_MAP') {
      const newClueId = `merch_clue_${Date.now()}`;
      setInventoryItems((prev) => [
        {
          id: newClueId,
          titleRu: 'Звёздный манускрипт странствующего купца',
          category: 'ROYAL_CLUE',
          descriptionRu:
            'Старинная карта с печатью королевской лилии, выкупленная у странствующего торговца редкостями.',
          sellPriceCoins: 50,
          foundAtRegionRu: 'Лавка на колёсах',
          foundDateIso: nowIso,
          inspectedLoreBonusClaimed: false,
        },
        ...prev,
      ]);
      setNewlyAddedBagItemIds((prev) => [newClueId, ...prev]);
      setTraits((prev) => ({ ...prev, curiosity: prev.curiosity + 2 }));
      triggerTraitToast({ curiosity: 2 });
      setLastReactionBanner(
        'Куплена Карта потайных троп! В шкатулку добавлена Королевская улика и Любопытство ↑↑ (+2).'
      );
    } else if (offer.rewardType === 'ROAD_AMULET') {
      setHasActiveRoadAmulet(true);
      setIsExhausted45(false);
      setLastReactionBanner(
        'Янтарный оберег Попутного Ветра активирован! Следующий поход защищён от неудач и принесёт +50% монет.'
      );
    }
  };

  // Smart Inventory Actions
  const handleInspectRoyalClue = (clue: InventoryLootItem) => {
    if (soundEnabled) onPlayTone('salute');
    if (!clue.inspectedLoreBonusClaimed) {
      setTraits((prev) => ({ ...prev, curiosity: prev.curiosity + 1 }));
      triggerTraitToast({ curiosity: 1 });
      setInventoryItems((prev) =>
        prev.map((i) =>
          i.id === clue.id ? { ...i, inspectedLoreBonusClaimed: true } : i
        )
      );
      setLastReactionBanner(
        `Вы с Шанталь внимательно изучили улику «${clue.titleRu}» у огня! Любопытство ↑ (+1).`
      );
    } else {
      setLastReactionBanner(
        `Улика «${clue.titleRu}» бережно хранится в шкатулке тайн как часть разгадки проклятия.`
      );
    }
  };

  const handleUseHerbFromInventory = (herb: InventoryLootItem) => {
    if (soundEnabled) onPlayTone('complete');
    setIsExhausted45(false);
    setMoodPct((prev) => Math.min(100, prev + 15));
    setInventoryItems((prev) => prev.filter((i) => i.id !== herb.id));
    setLastReactionBanner(
      `Заварена лесная находка «${herb.titleRu}»: усталость снята (поход снова за 30 Энергии) и +15% к настроению!`
    );
  };

  // Home Alchemy / Crafting: Combine 1 Herb + 1 Trinket (or 2 Herbs) into an Active Road Amulet!
  const handleCraftRoadAmuletInInventory = () => {
    if (herbsList.length === 0) return;
    if (herbsList.length < 2 && trinketsList.length === 0) return;
    if (soundEnabled) onPlayTone('salute');

    const herbToConsume = herbsList[0];
    const secondItemToConsume = trinketsList[0] ?? herbsList[1];
    if (!herbToConsume || !secondItemToConsume) return;

    setInventoryItems((prev) =>
      prev.filter((i) => i.id !== herbToConsume.id && i.id !== secondItemToConsume.id)
    );
    setHasActiveRoadAmulet(true);
    setIsExhausted45(false);
    setMoodPct(100);
    setTraits((prev) => ({ ...prev, curiosity: prev.curiosity + 1 }));
    triggerTraitToast({ curiosity: 1 });
    const bonusPct = ownedShopItemIds.includes('den_alchemy_table') ? 75 : 50;
    setLastReactionBanner(
      `Алхимия у камина: из «${herbToConsume.titleRu}» и «${secondItemToConsume.titleRu}» создан Оберег Попутного Ветра (+${bonusPct}% монет и 100% защита в следующем походе +1 Любопытство ↑)!`
    );
  };

  // Gift Trinket to NPC to level up Friendship
  const handleGiftTrinketToNpc = (loot: InventoryLootItem, npc: NpcFriendshipEntry) => {
    if (soundEnabled) onPlayTone('salute');
    setInventoryItems((prev) => prev.filter((i) => i.id !== loot.id));
    const nextLevel = Math.min(5, (npcFriendshipLevels[npc.npcId] ?? 1) + 1);
    setNpcFriendshipLevels((prev) => ({ ...prev, [npc.npcId]: nextLevel }));
    setCoins((prev) => prev + 18);

    if (npc.npcId === 'npc_bramble') {
      setTraits((prev) => ({ ...prev, tenderness: prev.tenderness + 1 }));
      triggerTraitToast({ tenderness: 1 });
    } else if (npc.npcId === 'npc_corvus') {
      setTraits((prev) => ({ ...prev, curiosity: prev.curiosity + 1 }));
      triggerTraitToast({ curiosity: 1 });
    } else if (npc.npcId === 'npc_torvin') {
      setTraits((prev) => ({ ...prev, wit: prev.wit + 1 }));
      triggerTraitToast({ wit: 1 });
    } else {
      setTraits((prev) => ({ ...prev, courage: prev.courage + 1 }));
      triggerTraitToast({ courage: 1 });
    }

    setLastReactionBanner(
      `Подарок «${loot.titleRu}» доставлен персонажу ${npc.nameRu}! Дружба выросла до Ур. ${nextLevel}/5, а в ответ прислано +18 Монет и бонус к характеру!`
    );
  };

  const getSellPriceWithWitBonus = (basePrice: number) => {
    return Math.round(basePrice * (1 + traitPerks.witMerchantSellBonusPct / 100));
  };

  const handleSellInventoryItem = (loot: InventoryLootItem) => {
    if (loot.category === 'ROYAL_CLUE') return;
    if (soundEnabled) onPlayTone('complete');
    const finalPrice = getSellPriceWithWitBonus(loot.sellPriceCoins);
    setCoins((prev) => prev + finalPrice);
    setInventoryItems((prev) => prev.filter((i) => i.id !== loot.id));
    setLastReactionBanner(
      `Продано скупщику: «${loot.titleRu}» за +${finalPrice} Монет (с учётом бонуса Остроумия +${traitPerks.witMerchantSellBonusPct}%)!`
    );
  };

  // PROLOGUE SCENE
  if (!hasCompletedPrologue) {
    return (
      <div className="max-w-[540px] mx-auto rounded-[28px] overflow-hidden border border-[#4D3D2C] bg-[#1C1510] text-[#F6EEDF] shadow-xl">
        <div className="relative h-72 w-full overflow-hidden bg-[#120D0A]">
          <img
            src="/src/assets/images/chantal_stage1_rescued_kit_1794001402267.jpg"
            alt="Маленький рыжий лисёнок Шанталь в тёплом пледе"
            referrerPolicy="no-referrer"
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-[#1C1510] via-[#1C1510]/40 to-transparent" />
          <div className="absolute bottom-4 left-5 right-5">
            <div className="text-xs font-semibold text-[#F59E0B] mb-1">
              Пролог · Дождливые сумерки у Старого Корня
            </div>
            <h1 className="text-2xl font-extrabold tracking-tight text-[#FFF8EB]">
              Неожиданная находка в лесу
            </h1>
          </div>
        </div>

        <div className="p-5 sm:p-6 space-y-4">
          <p className="text-sm sm:text-[15px] leading-relaxed text-[#E7D8C3]">
            Возвращаясь вечером по мшистой тропе мимо старой водяной мельницы, ты услышал тихий шорох под корнями векового дуба. В мокром папоротнике, дрожа от осеннего ливня и поджав ушибленную лапку, сидел крошечный огненно-рыжий лисёнок.
          </p>
          <p className="text-sm sm:text-[15px] leading-relaxed text-[#E7D8C3]">
            Она не убежала и не зашипела — только подняла на тебя удивительно осмысленные, тёплые янтарные глаза, словно давно ждала помощи. Ты бережно завернул малышку в свой шерстяной плащ и принёс домой к тёплому камину.
          </p>

          <div className="rounded-2xl p-4 bg-[#271E17] border border-[#4D3D2C]/70 space-y-1.5">
            <div className="text-sm font-bold text-[#F59E0B]">
              Как устроена забота о Шанталь:
            </div>
            <p className="text-xs leading-relaxed text-[#D5C3AA]">
              Каждая твоя тренировка Кегеля даёт <span className="font-bold text-[#10B981]">+10 Энергии</span>. За 30 Энергии (один закрытый блок Утро, Обед или Вечер) окрепшая Шанталь отправляется в 1-часовое приключение, сама выбирает путь на карте и возвращается с историей, монетами и лесными находками.
            </p>
          </div>

          <button
            onClick={() => {
              if (soundEnabled) onPlayTone('complete');
              setHasCompletedPrologue(true);
            }}
            className="w-full min-h-[52px] rounded-2xl bg-[#EA580C] hover:bg-[#D9460F] text-[#FFF8EB] font-extrabold text-sm sm:text-base flex items-center justify-center gap-2 cursor-pointer transition-transform active:scale-98 shadow-md"
          >
            <Heart className="w-5 h-5 fill-current" />
            <span>Согреть лисёнка у камина и назвать Шанталь</span>
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto space-y-5 text-[#F6EEDF]">
      {activeTraitToast && (
        <div className="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-5 py-3 rounded-2xl bg-[#271E17] border-2 border-[#F59E0B] text-[#FFF8EB] shadow-2xl flex items-center gap-2 animate-bounce">
          <Sparkles className="w-4 h-4 text-[#F59E0B] shrink-0" />
          <span className="text-xs sm:text-sm font-extrabold tracking-tight">
            {activeTraitToast.join(' · ')}
          </span>
        </div>
      )}

      {/* UNIFIED LOWER HEADER BAR ABOVE THE SCENE: Burger Menu on the Left + "Шанталь" + Streak/Energy/Coins */}
      <div
        className="relative z-40 rounded-2xl px-3.5 py-2.5 bg-[#1C1510] border border-[#4D3D2C] shadow-md flex items-center justify-between gap-2.5"
        data-popover-interactive="true"
      >
        <div className="flex items-center gap-2.5 min-w-0">
          <button
            onClick={() => setIsBurgerMenuOpen((prev) => !prev)}
            aria-label="Открыть меню разделов Шанталь"
            className="w-10 h-10 rounded-xl bg-[#281F18] hover:bg-[#36291E] border border-[#F59E0B]/70 flex items-center justify-center text-[#F59E0B] cursor-pointer transition-all shrink-0 shadow-inner"
          >
            {isBurgerMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-1.5 leading-snug">
              <span className="text-base sm:text-lg font-extrabold tracking-tight text-[#FFF8EB]">
                🦊 Шанталь
              </span>
              <span className="text-xs font-extrabold text-[#F59E0B]">
                ·{' '}
                {subView === 'den'
                  ? 'Домик'
                  : subView === 'map'
                  ? 'Карта троп'
                  : subView === 'inventory'
                  ? 'Сумка находок'
                  : subView === 'shop'
                  ? 'Лесная лавка'
                  : subView === 'journal'
                  ? 'Летопись'
                  : 'ИИ и Лор'}
              </span>
            </div>
            <div className="text-[11px] font-bold text-[#D5C3AA] mt-1 tabular-nums leading-snug">
              Серия {currentStreakDays} дн. · <span className="text-[#10B981]">⚡ {energy} Эн.</span> ·{' '}
              <span className="text-[#F59E0B]">🪙 {coins} М</span>
            </div>
          </div>
        </div>

        {/* Quick shortcut back to Домик when in another section */}
        {subView !== 'den' && (
          <button
            onClick={() => {
              setSubView('den');
              setIsBurgerMenuOpen(false);
            }}
            className="px-3 py-1.5 rounded-xl bg-[#281F18] hover:bg-[#36291E] border border-[#F59E0B]/50 text-xs font-extrabold text-[#F59E0B] cursor-pointer shrink-0"
          >
            🏠 В Домик
          </button>
        )}

        {/* BURGER DROPDOWN MENU WITH FULL UNTRUNCATED SECTION NAMES */}
        {isBurgerMenuOpen && (
          <div className="absolute left-0 top-[calc(100%+8px)] w-72 sm:w-80 rounded-2xl p-2 bg-[#1E1712] border-2 border-[#F59E0B] shadow-[0_18px_40px_rgba(0,0,0,0.85)] z-50 space-y-1 animate-fadeIn">
            {[
              { id: 'den' as const, label: 'Домик Шанталь', badge: `Ур. ${denVisualInfo.tierNumber}`, emoji: '🏠' },
              {
                id: 'map' as const,
                label: 'Карта троп',
                badge: `${WORLD_MAP_REGIONS.filter((r) => totalJourneys >= r.minJourneysRequired).length}/${WORLD_MAP_REGIONS.length}`,
                emoji: '🗺️',
              },
              {
                id: 'inventory' as const,
                label: 'Сумка находок',
                badge: `${inventoryItems.length} предм.`,
                emoji: '🎒',
              },
              {
                id: 'shop' as const,
                label: 'Лесная лавка',
                badge: `${coins} 🪙`,
                emoji: '🛒',
              },
              {
                id: 'journal' as const,
                label: 'Летопись странствий',
                badge: `${journalHistory.length} гл.`,
                emoji: '📖',
              },
              {
                id: 'ai_settings' as const,
                label: 'Настройки ИИ и Лора',
                badge: aiProvider === 'gemini' ? 'Gemini' : 'ИИ',
                emoji: '⚙️',
              },
            ].map((item) => {
              const isCurrent = subView === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setSubView(item.id);
                    setIsBurgerMenuOpen(false);
                  }}
                  className="w-full px-3.5 py-2.5 rounded-xl flex items-center justify-between gap-2 text-left cursor-pointer transition-all"
                  style={{
                    backgroundColor: isCurrent ? '#36291E' : 'transparent',
                    color: isCurrent ? '#F59E0B' : '#FFF8EB',
                  }}
                >
                  <div className="flex items-center gap-2.5">
                    <span className="text-base leading-none">{item.emoji}</span>
                    <span className="text-xs sm:text-sm font-extrabold">{item.label}</span>
                  </div>
                  <span className="px-2 py-0.5 rounded-lg bg-[#15100C] border border-[#4D3D2C] text-[10px] font-bold text-[#D5C3AA] tabular-nums">
                    {item.badge}
                  </span>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* ANIMATED POST-JOURNEY REWARD & REACTION POP-UP MODAL */}
      {lastChoiceReactionCard && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-3.5 sm:p-5 bg-black/80 backdrop-blur-sm"
          onClick={() => setLastChoiceReactionCard(null)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="relative w-full max-w-lg rounded-[30px] p-5 sm:p-6 bg-gradient-to-b from-[#2E2218] via-[#221912] to-[#18120D] border-2 border-[#F59E0B] shadow-[0_24px_70px_rgba(0,0,0,0.92)] space-y-4 animate-reward-pop overflow-hidden"
          >
            {/* Subtle radial golden glow in the background */}
            <div
              className="pointer-events-none absolute -top-20 left-1/2 -translate-x-1/2 w-80 h-44 rounded-full opacity-25 blur-2xl"
              style={{
                background:
                  'radial-gradient(circle, #F59E0B 0%, rgba(245,158,11,0) 70%)',
              }}
            />

            {/* Header */}
            <div className="relative flex items-start justify-between gap-3">
              <div className="space-y-1">
                <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#382B20] border border-[#F59E0B]/60 text-[11px] font-extrabold text-[#FDE68A]">
                  <Sparkles className="w-3.5 h-3.5 text-[#F59E0B]" />
                  <span>
                    {lastChoiceReactionCard.isMilestoneChest
                      ? `★ Сокровища Сундука Главы #${lastChoiceReactionCard.journeyNumber}`
                      : `Добыча из Похода #${lastChoiceReactionCard.journeyNumber} · ${lastChoiceReactionCard.regionTitleRu}`}
                  </span>
                </div>
                <h3 className="text-base sm:text-lg font-extrabold text-[#FFF8EB] leading-snug">
                  {lastChoiceReactionCard.episodeTitleRu}
                </h3>
              </div>

              <button
                onClick={() => setLastChoiceReactionCard(null)}
                aria-label="Закрыть окно наград"
                className="w-8 h-8 rounded-xl bg-[#15100C] hover:bg-[#382B20] border border-[#5C4834] flex items-center justify-center text-xs font-bold text-[#D5C3AA] hover:text-white cursor-pointer shrink-0"
              >
                ✕
              </button>
            </div>

            {/* STAGGERED ANIMATED REWARD DROP CARDS: COINS + LOOT ITEM */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* REWARD CARD 1: COINS */}
              <div className="animate-reward-drop-1 rounded-2xl p-3.5 bg-gradient-to-br from-[#2B1F14] to-[#1A130D] border-2 border-[#F59E0B] shadow-lg flex items-center gap-3">
                <div className="w-13 h-13 rounded-2xl bg-gradient-to-br from-[#F59E0B]/30 to-[#78350F]/40 border border-[#FBBF24] flex items-center justify-center shrink-0 shadow-inner">
                  <span className="text-3xl leading-none select-none animate-gentle-float">
                    🪙
                  </span>
                </div>
                <div className="min-w-0">
                  <div className="text-[10px] font-extrabold uppercase tracking-wider text-[#FCD34D]">
                    Золотые монеты
                  </div>
                  <div className="text-xl sm:text-2xl font-extrabold text-[#FFF8EB] tabular-nums leading-tight">
                    +{lastChoiceReactionCard.coinsEarned} М
                  </div>
                  <div className="text-[11px] font-bold text-[#D5C3AA] tabular-nums mt-0.5">
                    В кошельке: {coins} 🪙 · <span className="text-[#F472B6]">+{lastChoiceReactionCard.xpEarned} XP</span>
                  </div>
                </div>
              </div>

              {/* REWARD CARD 2: LOOT ITEM */}
              {(() => {
                const item = lastChoiceReactionCard.lootItem;
                const emoji = getLootItemEmoji(item);
                const isDenTrophy = item.category === 'DEN_TROPHY';
                const isClue = item.category === 'ROYAL_CLUE';
                const isHerb = item.category === 'HERB';
                const accentHex = isClue
                  ? '#F59E0B'
                  : isDenTrophy
                  ? '#EC4899'
                  : isHerb
                  ? '#10B981'
                  : '#38BDF8';
                const categoryBadgeRu = isClue
                  ? '👑 Королевская улика'
                  : isDenTrophy
                  ? `🏠 В Домик (${lastChoiceReactionCard.denStepCountTextRu || '+1 из 10'})`
                  : isHerb
                  ? '🌿 Целебная трава'
                  : `✦ Диковинка (${getSellPriceWithWitBonus(item.sellPriceCoins)} М)`;

                return (
                  <div
                    className="animate-reward-drop-2 rounded-2xl p-3.5 bg-gradient-to-br from-[#241B15] to-[#16110C] border-2 shadow-lg flex items-center gap-3"
                    style={{ borderColor: accentHex }}
                  >
                    <div
                      className="w-13 h-13 rounded-2xl border flex items-center justify-center shrink-0 shadow-inner"
                      style={{
                        backgroundColor: `${accentHex}22`,
                        borderColor: accentHex,
                      }}
                    >
                      <span className="text-3xl leading-none select-none animate-gentle-float">
                        {emoji}
                      </span>
                    </div>
                    <div className="min-w-0">
                      <div
                        className="text-[10px] font-extrabold leading-tight"
                        style={{ color: accentHex }}
                      >
                        {categoryBadgeRu}
                      </div>
                      <div className="text-xs sm:text-sm font-extrabold text-[#FFF8EB] leading-snug mt-0.5 break-words">
                        {item.titleRu}
                      </div>
                    </div>
                  </div>
                );
              })()}
            </div>

            {/* Loot Item Description Pill */}
            <div className="rounded-2xl px-3.5 py-2.5 bg-[#15100C]/90 border border-[#382B20] text-xs text-[#E7D8C3] leading-relaxed">
              <span className="font-bold text-[#FFF8EB]">
                {getLootItemEmoji(lastChoiceReactionCard.lootItem)} Находка:{' '}
              </span>
              {lastChoiceReactionCard.lootItem.descriptionRu}
            </div>

            {/* Chantal's Live Reaction to Player's Answer */}
            <div className="rounded-2xl p-3.5 bg-gradient-to-br from-[#1C140F] via-[#241A13] to-[#17110C] border border-[#F59E0B]/50 space-y-2 shadow-inner">
              <div className="text-[11px] font-bold text-[#F59E0B]">
                🦊 Реакция Шанталь на твои слова:
              </div>
              <p className="text-xs sm:text-sm italic font-serif text-[#FAF0E1] leading-relaxed">
                «{lastChoiceReactionCard.chantalReactionRu}»
              </p>

              {!lastChoiceReactionCard.isFillerEpisode && (
                <div className="flex flex-wrap items-center gap-1.5 pt-1 border-t border-[#382B20]">
                  <span className="text-[10px] font-bold text-[#D5C3AA] mr-1">
                    Характер:
                  </span>
                  {TRAIT_METADATA.map((meta) => {
                    const delta = lastChoiceReactionCard.deltas[meta.key] || 0;
                    if (delta === 0) return null;
                    const prevVal = lastChoiceReactionCard.previousTraits[meta.key];
                    const nextVal = lastChoiceReactionCard.newTraits[meta.key];
                    const arrows =
                      delta >= 2 ? '↑↑' : delta === 1 ? '↑' : delta <= -2 ? '↓↓' : '↓';
                    const isPos = delta > 0;
                    return (
                      <span
                        key={meta.key}
                        className="px-2 py-0.5 rounded-lg text-[10px] font-extrabold border tabular-nums"
                        style={{
                          backgroundColor: isPos ? '#064E3B90' : '#7F1D1D90',
                          borderColor: isPos ? '#10B981' : '#F87171',
                          color: isPos ? '#6EE7B7' : '#FCA5A5',
                        }}
                      >
                        {meta.labelRu}: {prevVal} → {nextVal} ({isPos ? `+${delta}` : delta}{' '}
                        {arrows})
                      </span>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Action Buttons */}
            <div className="flex flex-wrap items-center justify-end gap-2 pt-1">
              <button
                onClick={() => {
                  const item = lastChoiceReactionCard.lootItem;
                  setLastChoiceReactionCard(null);
                  if (item.category === 'DEN_TROPHY') {
                    setSubView('den');
                    setDenSubTab('housing');
                    setActiveDenItemPopoverId(item.id);
                  } else {
                    setSubView('inventory');
                    setSelectedInventoryItemId(item.id);
                  }
                }}
                className="min-h-[42px] px-4 rounded-2xl bg-[#281F18] hover:bg-[#36291E] border border-[#5C4834] text-xs font-extrabold text-[#FFF8EB] cursor-pointer transition-all"
              >
                {lastChoiceReactionCard.lootItem.category === 'DEN_TROPHY'
                  ? '🏠 Осмотреть в Домике'
                  : '🎒 Открыть в Сумке'}
              </button>

              <button
                onClick={() => setLastChoiceReactionCard(null)}
                className="min-h-[42px] px-5 rounded-2xl bg-gradient-to-r from-[#EA580C] to-[#F59E0B] hover:from-[#D9460F] hover:to-[#D97706] text-[#15100C] text-xs sm:text-sm font-extrabold cursor-pointer shadow-lg transition-transform active:scale-95"
              >
                Забрать награды ✨
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ACTIVE STORY READING & RESPONSE SCROLL */}
      {activeStoryEpisode && (
        <section className="rounded-[28px] p-5 sm:p-6 bg-[#2A2018] border-2 border-[#F59E0B] shadow-xl space-y-5">
          {activeStoryEpisode.isMilestoneChest && (
            <div className="relative rounded-2xl overflow-hidden border border-[#F59E0B] h-44 w-full bg-[#15100C]">
              <img
                src={DEN_TIER_IMAGES.merchantChest}
                alt="Юбилейный Сундук Главы"
                referrerPolicy="no-referrer"
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#1E1712] via-black/40 to-transparent" />
              <div className="absolute bottom-3.5 left-4 right-4">
                <span className="px-2.5 py-1 rounded-lg bg-[#F59E0B] text-[#15100C] text-xs font-extrabold">
                  ★ ЮБИЛЕЙНЫЙ СУНДУК ГЛАВЫ #{activeStoryEpisode.journeyNumber} · ДВОЙНАЯ НАГРАДА И УЛИКА
                </span>
                <p className="text-xs text-[#FFF8EB] mt-1 font-semibold">
                  Каждый 5-й поход открывает кованый сундук главы с гарантированной Королевской уликой и 4 вариантами выбора!
                </p>
              </div>
            </div>
          )}

          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#4D3D2C] pb-3.5">
            <div>
              <div className="text-xs font-semibold text-[#F59E0B]">
                {activeStoryEpisode.isFillerEpisode
                  ? 'Побочный эпизод (Филлер — не меняет характер и основной сюжет)'
                  : `Поход #${activeStoryEpisode.journeyNumber} · Арка дня ${activeStoryEpisode.dailyArcStep}/3 · ${activeStoryEpisode.regionTitleRu}`}
              </div>
              <h2 className="text-xl sm:text-2xl font-extrabold text-[#FFF8EB] mt-0.5">
                {activeStoryEpisode.titleRu}
              </h2>
            </div>
            <span className="text-xs font-semibold text-[#D5C3AA]">
              Рассказчик: {activeStoryEpisode.aiSourceLabel || 'Gemini Flash'}
            </span>
          </div>

          {/* Where Chantal went on the map + Past Adventure Echo */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="rounded-2xl p-3.5 bg-[#1E1712] border border-[#4D3D2C] flex items-start gap-3">
              <MapPin className="w-5 h-5 text-[#F59E0B] shrink-0 mt-0.5" />
              <div className="text-xs text-[#E7D8C3] leading-relaxed">
                <span className="font-bold text-[#FFF8EB]">
                  {activeStoryEpisode.stayedInSameRegion
                    ? 'Повторный визит в локацию: '
                    : 'Маршрут на карте: '}
                </span>
                {activeStoryEpisode.chantalMapNoteRu}
              </div>
            </div>

            {activeStoryEpisode.pastEchoNoteRu && (
              <div className="rounded-2xl p-3.5 bg-[#1E1712] border border-[#10B981]/50 flex items-start gap-3">
                <BookOpen className="w-5 h-5 text-[#10B981] shrink-0 mt-0.5" />
                <div className="text-xs text-[#E7D8C3] leading-relaxed">
                  <span className="font-bold text-[#10B981]">
                    Эхо прошлых походов (Трёхслойная память):{' '}
                  </span>
                  {activeStoryEpisode.pastEchoNoteRu}
                </div>
              </div>
            )}
          </div>

          {/* Parchment Story Body (~10 sentences) */}
          <div className="rounded-2xl p-4 sm:p-5 bg-[#33271D] border border-[#5C4834] space-y-3 text-sm sm:text-[15px] leading-relaxed text-[#FAF0E1]">
            {activeStoryEpisode.storyTextRu.split('\n').map((para, i) =>
              para.trim() ? <p key={i}>{para}</p> : null
            )}
          </div>

          {/* Animated Loot Item + Coins Preview Cards inside Story Scroll */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="animate-reward-drop-1 rounded-2xl p-4 bg-gradient-to-br from-[#1E1712] to-[#15100C] border-2 border-[#10B981]/70 flex items-start gap-3.5 shadow-md">
              <div className="w-12 h-12 rounded-2xl bg-[#064E3B]/50 border border-[#34D399] flex items-center justify-center shrink-0">
                <span className="text-2xl leading-none select-none animate-gentle-float">
                  {getLootItemEmoji(activeStoryEpisode.lootItem)}
                </span>
              </div>
              <div className="space-y-1 min-w-0">
                <div className="text-[11px] font-extrabold text-[#34D399]">
                  {activeStoryEpisode.lootItem.category === 'ROYAL_CLUE'
                    ? '👑 Королевская улика в Шкатулку тайн'
                    : activeStoryEpisode.lootItem.category === 'DEN_TROPHY'
                    ? '🏠 Трофей уюта в Домик (+1 из 10)'
                    : activeStoryEpisode.lootItem.category === 'HERB'
                    ? '🌿 Целебная находка'
                    : `✦ Диковинка в сумку · ${getSellPriceWithWitBonus(
                        activeStoryEpisode.lootItem.sellPriceCoins
                      )} М`}
                </div>
                <div className="text-sm sm:text-base font-extrabold text-[#FFF8EB] leading-snug">
                  {activeStoryEpisode.lootItem.titleRu}
                </div>
                <p className="text-xs text-[#D5C3AA] leading-relaxed">
                  {activeStoryEpisode.lootItem.descriptionRu}
                </p>
              </div>
            </div>

            <div
              className="animate-reward-drop-2 rounded-2xl p-4 bg-gradient-to-br from-[#1E1712] to-[#15100C] border-2 flex items-start gap-3.5 shadow-md"
              style={{
                borderColor:
                  activeStoryEpisode.outcomeType === 'GOOD_REWARD' ? '#F59E0B' : '#EF4444',
              }}
            >
              <div className="w-12 h-12 rounded-2xl bg-[#78350F]/40 border border-[#FBBF24] flex items-center justify-center shrink-0">
                <span className="text-2xl leading-none select-none animate-gentle-float">
                  🪙
                </span>
              </div>
              <div className="space-y-1 min-w-0">
                <div className="text-sm sm:text-base font-extrabold text-[#FDE68A] tabular-nums">
                  +{activeStoryEpisode.coinsEarned} Монет из похода
                </div>
                <div className="text-[11px] font-bold text-[#F59E0B]">
                  Бонус Любопытства +{traitPerks.curiosityBonusCoins} М · Бонус Домика +{passiveShopCoinsBonus} М
                </div>
                {activeStoryEpisode.outcomeType === 'GOOD_REWARD' ? (
                  <p className="text-xs text-[#D5C3AA] leading-relaxed">
                    Поход прошёл благополучно! Выбери ответ ниже, чтобы забрать монеты и находку.
                  </p>
                ) : (
                  <div className="space-y-0.5">
                    <div className="text-xs font-extrabold text-[#F87171] flex items-center gap-1.5">
                      <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                      <span>Испытание в пути:</span>
                    </div>
                    <p className="text-xs text-[#FCA5A5] leading-relaxed">
                      {activeStoryEpisode.badNewsTitleRu}
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* 3 or 4 Canonical Response Buttons (Spoiler-Free: no trait arrows shown before clicking!) */}
          <div className="space-y-2.5">
            <div className="text-xs font-bold text-[#D5C3AA]">
              Как ты отреагируешь на рассказ Шанталь? (Выбери ответ по душе — её живая реакция и изменения черт откроются после выбора):
            </div>
            <div className="grid grid-cols-1 gap-2.5">
              {activeStoryEpisode.choices.map((choice, idx) => {
                const isSpecialChestChoice = idx === 3;
                return (
                  <button
                    key={idx}
                    onClick={() => handleSelectCanonicalChoice(idx)}
                    className="w-full min-h-[52px] p-3.5 rounded-2xl border text-left flex items-center justify-between gap-2 cursor-pointer transition-colors hover:border-[#F59E0B]"
                    style={{
                      backgroundColor: isSpecialChestChoice ? '#382B20' : '#1E1712',
                      borderColor: isSpecialChestChoice ? '#F59E0B' : '#5C4834',
                    }}
                  >
                    <span className="text-sm font-bold text-[#FFF8EB] leading-snug">
                      {idx + 1}. {choice.labelRu}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Free Text Input */}
          <div className="rounded-2xl p-4 bg-[#1E1712] border border-[#4D3D2C] space-y-2.5">
            <div className="text-xs font-bold text-[#D5C3AA]">
              Или напиши свой вариант / личное поручение (режим Филлера — не меняет характер и основной сюжет):
            </div>
            <div className="flex flex-col sm:flex-row gap-2">
              <input
                type="text"
                value={customFillerInput}
                onChange={(e) => setCustomFillerInput(e.target.value)}
                placeholder="Например: Отправляйся к гончару и помоги слепить новые горшки..."
                className="flex-1 min-h-[44px] px-3.5 rounded-xl bg-[#15100C] border border-[#4D3D2C] text-sm text-[#FFF8EB] placeholder-[#8C7A65]"
              />
              <button
                onClick={handleSendCustomFillerResponse}
                disabled={!customFillerInput.trim()}
                className="min-h-[44px] px-4 rounded-xl bg-[#EA580C] disabled:opacity-40 text-white text-xs font-extrabold flex items-center justify-center gap-1.5 cursor-pointer shrink-0"
              >
                <Send className="w-4 h-4" />
                <span>Ответить своим текстом</span>
              </button>
            </div>
          </div>
        </section>
      )}

      {/* SUBVIEW 1: SCENE-FIRST CLEAN ILLUSTRATION + STRUCTURED STATUS ITEMS + COLLAPSIBLE 100-PT TRAITS + STREAMLINED JOURNEY CARD */}
      {subView === 'den' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
          {/* Left Column: Pure Illustration First + Small Heart Button + Structured Status Items + Collapsible Accordions */}
          <div className="lg:col-span-6 space-y-4">
            <div className="rounded-[26px] bg-[#1E1712] border border-[#4D3D2C] shadow-md">
              {/* 1. PURE CLEAN ILLUSTRATION FIRST (No text overlays on the artwork, only a small circular heart icon button) */}
              <div className="relative aspect-square w-full max-h-[390px] overflow-hidden rounded-t-[25px] bg-[#120D0A]">
                <img
                  src={activeMainIllustrationUrl}
                  alt={stageInfo.titleRu}
                  referrerPolicy="no-referrer"
                  className="w-full h-full object-cover"
                />

                {isHeartPulseActive && (
                  <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                    <div className="px-4 py-2 rounded-2xl bg-black/80 border border-[#EC4899] text-[#F472B6] font-extrabold text-xs sm:text-sm flex items-center gap-2 animate-bounce shadow-2xl">
                      <Heart className="w-5 h-5 fill-current text-[#EC4899] shrink-0" />
                      <span>
                        +{traitPerks.tendernessMoodHealBonusPct}% к настроению · +3 XP взросления!
                      </span>
                    </div>
                  </div>
                )}

                {/* Small circular heart icon button in the bottom-right corner */}
                <button
                  onClick={handlePetOrTalkWithChantal}
                  disabled={hasUsedPetThisCycle || Boolean(activeJourney)}
                  title={
                    activeJourney
                      ? 'Шанталь сейчас в путешествии'
                      : hasUsedPetThisCycle
                      ? 'Доступно снова после возвращения из похода'
                      : `Погладить Шанталь (+${traitPerks.tendernessMoodHealBonusPct}% к настроению и +3 XP взросления)`
                  }
                  aria-label="Погладить Шанталь"
                  className="absolute bottom-3.5 right-3.5 w-11 h-11 rounded-full border-2 flex items-center justify-center transition-all shadow-lg cursor-pointer disabled:cursor-not-allowed active:scale-95"
                  style={{
                    backgroundColor:
                      hasUsedPetThisCycle || activeJourney ? '#15100CCC' : '#2A161EE6',
                    borderColor:
                      hasUsedPetThisCycle || activeJourney ? '#4D3D2C' : '#EC4899',
                    color:
                      hasUsedPetThisCycle || activeJourney ? '#786654' : '#F472B6',
                    opacity: hasUsedPetThisCycle || activeJourney ? 0.55 : 1,
                  }}
                >
                  <Heart
                    className={`w-5 h-5 ${
                      hasUsedPetThisCycle || activeJourney ? '' : 'fill-current'
                    }`}
                  />
                </button>
              </div>

              {/* 2. VISUAL MATURITY EXPERIENCE (XP) PROGRESS BAR DIRECTLY UNDER THE PORTRAIT + STRUCTURED STATUS ITEMS */}
              <div className="p-4 sm:p-5 space-y-3.5">
                {/* MATURITY XP PROGRESS BAR CARD */}
                <div className="rounded-2xl p-3 bg-gradient-to-br from-[#281F18] to-[#1A130E] border border-[#4D3D2C] space-y-2 shadow-inner">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                        🦊 {stageInfo.titleRu}
                      </span>
                      <span className="px-2 py-0.5 rounded-lg bg-[#382B20] border border-[#F59E0B]/60 text-[10px] font-extrabold text-[#FDE68A] tabular-nums shrink-0">
                        Ур. {maturityProgressInfo.maturityLevel}
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      {recentXpGainBadge && (
                        <span className="px-2 py-0.5 rounded-full bg-[#EC4899]/20 border border-[#EC4899] text-[10px] font-extrabold text-[#F472B6] animate-pulse tabular-nums">
                          +{recentXpGainBadge.amount} XP ({recentXpGainBadge.sourceRu})
                        </span>
                      )}
                      <span className="text-[11px] font-extrabold text-[#F59E0B] tabular-nums">
                        ✨ {maturityProgressInfo.xpInStage} / {maturityProgressInfo.stageTargetXp} XP (
                        {maturityProgressInfo.progressPct}%)
                      </span>
                    </div>
                  </div>

                  {/* Animated XP Bar */}
                  <div className="w-full h-2.5 rounded-full bg-[#120D0A] border border-[#382B20] overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-700 ease-out bg-gradient-to-r from-[#F59E0B] via-[#EA580C] to-[#EC4899]"
                      style={{
                        width: `${maturityProgressInfo.progressPct}%`,
                        boxShadow: '0 0 10px rgba(245, 158, 11, 0.5)',
                      }}
                    />
                  </div>

                  <div className="flex flex-wrap items-center justify-between gap-1 text-[10px] font-semibold text-[#C8B499] tabular-nums">
                    <span>🧭 Поход: +10 XP · ❤️ Ласка: +3 XP</span>
                    <span>
                      До следующей стадии: {maturityProgressInfo.xpToNextStage} XP
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  <div className="rounded-xl p-2.5 bg-[#281F18] border border-[#382B20]">
                    <div className="text-[10px] font-bold text-[#C8B499]">Стадия</div>
                    <div className="text-xs font-extrabold text-[#FFF8EB] leading-tight mt-0.5">
                      🦊 {stageInfo.isTransformed ? 'Акт II' : `Стадия ${stageInfo.stageNumber}`}
                    </div>
                  </div>

                  <div className="rounded-xl p-2.5 bg-[#281F18] border border-[#382B20]">
                    <div className="text-[10px] font-bold text-[#C8B499]">Походов</div>
                    <div className="text-xs font-extrabold text-[#F59E0B] tabular-nums mt-0.5">
                      🧭 {totalJourneys} {activeJourney ? '· В пути' : ''}
                    </div>
                  </div>

                  <div className="rounded-xl p-2.5 bg-[#281F18] border border-[#382B20]">
                    <div className="text-[10px] font-bold text-[#C8B499]">Настроение</div>
                    <div className="text-xs font-extrabold text-[#EC4899] tabular-nums mt-0.5">
                      💛 {moodPct}%
                    </div>
                  </div>

                  <button
                    onClick={() =>
                      setDenSubTab((prev) => (prev === 'housing' ? 'none' : 'housing'))
                    }
                    className="rounded-xl p-2.5 bg-[#281F18] hover:bg-[#33271D] border border-[#10B981]/40 text-left cursor-pointer transition-colors"
                  >
                    <div className="text-[10px] font-bold text-[#C8B499]">Интерьер</div>
                    <div className="text-xs font-extrabold text-[#10B981] tabular-nums mt-0.5">
                      🏠 Ур. {denVisualInfo.tierNumber} ({denVisualInfo.itemsInCurrentStep} из 10)
                    </div>
                  </button>
                </div>

                {petSpeechBubble && (
                  <div className="rounded-2xl p-3 bg-[#281F18] border border-[#EC4899]/50 space-y-1">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-[11px] font-bold text-[#F472B6]">
                        {petSpeechBubble.badgeRu}
                      </span>
                      <button
                        onClick={() => setPetSpeechBubble(null)}
                        className="text-[10px] font-bold text-[#C8B499] hover:text-white cursor-pointer"
                      >
                        ✕
                      </button>
                    </div>
                    <p className="text-xs text-[#FAF0E1] leading-relaxed">
                      {petSpeechBubble.textRu}
                    </p>
                  </div>
                )}

                {lastReactionBanner && (
                  <div className="rounded-2xl p-3 bg-[#2A2018] border border-[#F59E0B]/50 text-xs text-[#FAF0E1]">
                    {lastReactionBanner}
                  </div>
                )}

                {/* 3. COLLAPSIBLE ACCORDION BUTTONS: [ ✨ Характер ▼ ] · [ 🏠 Интерьер ▼ ] (Removed static 'У камина' tab) */}
                <div className="grid grid-cols-2 gap-2">
                  {[
                    {
                      id: 'traits' as const,
                      label: `✨ Характер`,
                    },
                    {
                      id: 'housing' as const,
                      label: `🏠 Интерьер (${denVisualInfo.itemsInCurrentStep} из 10)`,
                    },
                  ].map((tab) => {
                    const isOpen = denSubTab === tab.id;
                    return (
                      <button
                        key={tab.id}
                        onClick={() => setDenSubTab(isOpen ? 'none' : tab.id)}
                        className="min-h-[40px] px-3 py-1.5 rounded-xl border text-xs font-extrabold cursor-pointer transition-all flex items-center justify-center gap-1.5 text-center leading-tight"
                        style={{
                          backgroundColor: isOpen ? '#33271D' : '#15100C',
                          borderColor: isOpen ? '#F59E0B' : '#382B20',
                          color: isOpen ? '#F59E0B' : '#C8B499',
                        }}
                      >
                        <span>{tab.label}</span>
                        <span className="text-[9px] shrink-0">{isOpen ? '▲' : '▼'}</span>
                      </button>
                    );
                  })}
                </div>

                {/* EXPANDABLE TAB 1: 6 CHARACTER TRAITS ON A 100-POINT SCALE WITH RIGHT-SIDE POPOVER */}
                {denSubTab === 'traits' && (
                  <div className="rounded-2xl p-3.5 bg-[#15100C] border border-[#382B20] space-y-2.5 animate-fadeIn">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="text-[11px] font-bold text-[#D5C3AA]">
                        Шкала черт (0–100) · нажми на черту:
                      </span>
                      <span className="px-2.5 py-0.5 rounded-xl bg-[#281F18] border border-[#F59E0B]/40 text-[10px] font-extrabold text-[#F59E0B]">
                        Ведущая: {traitPerks.dominantTraitLabelRu}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                      {TRAIT_METADATA.map((meta, idx) => {
                        const val = traits[meta.key];
                        const delta = lastTraitChangeRecord?.deltas[meta.key] || 0;
                        const pct = Math.min(100, Math.max(2, val));
                        const isChanged = delta !== 0;
                        const isSelectedPopup = selectedTraitPopupKey === meta.key;
                        const arrows =
                          delta >= 2 ? '↑↑' : delta === 1 ? '↑' : delta <= -2 ? '↓↓' : '↓';
                        const perkDesc = getTraitActivePerkDescriptionRu(meta.key, traitPerks);

                        return (
                          <div
                            key={meta.key}
                            className="relative"
                            data-popover-interactive="true"
                          >
                            <button
                              onClick={() =>
                                setSelectedTraitPopupKey(isSelectedPopup ? null : meta.key)
                              }
                              className="w-full rounded-xl p-2 bg-[#281F18] border text-left space-y-1.5 cursor-pointer transition-all"
                              style={{
                                borderColor: isSelectedPopup
                                  ? '#F59E0B'
                                  : isChanged
                                  ? delta > 0
                                    ? '#10B981'
                                    : '#F87171'
                                  : '#382B20',
                                backgroundColor: isSelectedPopup
                                  ? '#36291E'
                                  : isChanged
                                  ? '#30241B'
                                  : '#281F18',
                              }}
                            >
                              <div className="flex flex-wrap items-center justify-between gap-1 text-[11px] font-bold">
                                <span className="text-[#FFF8EB] leading-tight">{meta.labelRu}</span>
                                <div className="flex items-center gap-1 tabular-nums shrink-0">
                                  {isChanged && (
                                    <span
                                      className="px-1 py-0.2 rounded text-[9px] font-extrabold"
                                      style={{
                                        backgroundColor: delta > 0 ? '#10B98125' : '#EF444425',
                                        color: delta > 0 ? '#34D399' : '#F87171',
                                      }}
                                    >
                                      {delta > 0 ? `+${delta}` : delta}
                                      {arrows}
                                    </span>
                                  )}
                                  <span
                                    className="font-extrabold text-[11px]"
                                    style={{ color: meta.colorHex }}
                                  >
                                    {val}/100
                                  </span>
                                </div>
                              </div>
                              <div className="relative w-full h-1.5 rounded-full bg-[#15100C] overflow-hidden">
                                <div
                                  className="h-full rounded-full transition-all duration-500"
                                  style={{
                                    width: `${pct}%`,
                                    backgroundColor: meta.colorHex,
                                  }}
                                />
                              </div>
                            </button>

                            {isSelectedPopup && (
                              <div
                                className={`absolute z-50 w-[235px] sm:w-[260px] rounded-2xl p-3 bg-[#241A13] border-2 shadow-[0_14px_34px_rgba(0,0,0,0.85)] space-y-1.5 animate-fadeIn ${getAdjacentPopoverClasses(
                                  idx,
                                  2
                                )}`}
                                style={{ borderColor: meta.colorHex }}
                              >
                                <div className="flex items-center justify-between gap-2">
                                  <span
                                    className="text-xs font-extrabold"
                                    style={{ color: meta.colorHex }}
                                  >
                                    {meta.labelRu} · {val}/100
                                  </span>
                                  <button
                                    onClick={() => setSelectedTraitPopupKey(null)}
                                    className="text-[11px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer"
                                  >
                                    ✕
                                  </button>
                                </div>
                                <p className="text-[11px] text-[#E7D8C3] leading-snug">
                                  {perkDesc}
                                </p>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* EXPANDABLE TAB 2: HOUSING PROGRESSION (INTERIOR) WITH WARM COUNTER & COMPACT ICON POPOVERS */}
                {denSubTab === 'housing' && (
                  <div className="rounded-2xl p-3.5 bg-[#15100C] border border-[#10B981]/40 space-y-2.5 animate-fadeIn">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <div className="text-xs font-extrabold text-[#10B981] flex items-center gap-1.5">
                          <Home className="w-3.5 h-3.5" />
                          <span>Интерьер · Ур. {denVisualInfo.tierNumber}</span>
                        </div>
                        <span
                          className="px-2.5 py-0.5 rounded-lg text-[11px] font-extrabold tracking-wide border shadow-sm tabular-nums"
                          style={{
                            background: 'linear-gradient(135deg, #065F46 0%, #047857 100%)',
                            borderColor: '#34D399',
                            color: '#ECFDF5',
                          }}
                        >
                          {denVisualInfo.itemsInCurrentStep} из 10
                        </span>
                      </div>
                      <button
                        onClick={() => {
                          setShopSectionFilter('den_comfort');
                          setSubView('shop');
                        }}
                        className="px-3 py-1 rounded-xl bg-[#241B15] border border-[#F59E0B]/50 text-[11px] font-extrabold text-[#F59E0B] shrink-0 cursor-pointer"
                      >
                        В Лавку →
                      </button>
                    </div>

                    <div className="space-y-1">
                      <div className="w-full h-2.5 rounded-full bg-[#241B15] border border-[#382B20] overflow-hidden">
                        <div
                          className="h-full rounded-full transition-all duration-500 bg-gradient-to-r from-[#10B981] to-[#F59E0B]"
                          style={{
                            width: `${Math.min(
                              100,
                              Math.round((denVisualInfo.itemsInCurrentStep / 10) * 100)
                            )}%`,
                          }}
                        />
                      </div>
                      <div className="text-[11px] text-[#D5C3AA]">
                        {denVisualInfo.nextUnlockHintRu}
                      </div>
                    </div>

                    {(ownedDenItems.length > 0 || denTrophiesList.length > 0) && (
                      <div className="grid grid-cols-4 sm:grid-cols-5 gap-2 pt-1">
                        {denTrophiesList.map((trophy, idx) => {
                          const isOpen = activeDenItemPopoverId === trophy.id;
                          const isNewTrophy = newlyAddedBagItemIds.includes(trophy.id);
                          return (
                            <div
                              key={trophy.id}
                              className="relative"
                              data-popover-interactive="true"
                            >
                              <button
                                onClick={() => {
                                  setActiveDenItemPopoverId(isOpen ? null : trophy.id);
                                  if (isNewTrophy) {
                                    setNewlyAddedBagItemIds((prev) =>
                                      prev.filter((id) => id !== trophy.id)
                                    );
                                  }
                                }}
                                className={`relative w-full min-h-[76px] rounded-xl p-1.5 border flex flex-col items-center justify-center gap-1 cursor-pointer transition-colors ${
                                  isNewTrophy
                                    ? 'animate-bag-new-item-drop'
                                    : 'animate-bag-slot-drop'
                                }`}
                                style={{
                                  animationDelay: `${Math.min(idx * 45, 300)}ms`,
                                  backgroundColor: isOpen ? '#36291E' : '#241B15',
                                  borderColor: isOpen
                                    ? '#F59E0B'
                                    : isNewTrophy
                                    ? '#FBBF24'
                                    : '#10B98180',
                                }}
                              >
                                <span
                                  className="text-xl leading-none inline-block animate-bag-icon-drop"
                                  style={{ animationDelay: `${Math.min(idx * 45 + 50, 350)}ms` }}
                                >
                                  {getLootItemEmoji(trophy)}
                                </span>
                                <span className="text-[10px] font-bold text-[#FFF8EB] leading-tight w-full text-center break-words">
                                  {getLootItemShortTitle(trophy)}
                                </span>
                              </button>

                              {isOpen && (
                                <div
                                  className={`absolute z-50 w-[230px] rounded-2xl p-3 bg-[#241A13] border-2 border-[#10B981] shadow-[0_14px_34px_rgba(0,0,0,0.85)] space-y-1.5 animate-fadeIn ${getAdjacentPopoverClasses(
                                    idx,
                                    4
                                  )}`}
                                >
                                  <div className="flex items-start justify-between gap-1.5">
                                    <span className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                                      {getLootItemEmoji(trophy)} {trophy.titleRu}
                                    </span>
                                    <button
                                      onClick={() => setActiveDenItemPopoverId(null)}
                                      className="text-[10px] text-[#D5C3AA] hover:text-white cursor-pointer"
                                    >
                                      ✕
                                    </button>
                                  </div>
                                  <div className="text-[10px] font-bold text-[#10B981]">
                                    🏠 Трофей из похода (+1 из 10) · 🪙 +1 М/поход
                                  </div>
                                  <p className="text-[11px] text-[#E7D8C3] leading-snug">
                                    {trophy.descriptionRu}
                                  </p>
                                </div>
                              )}
                            </div>
                          );
                        })}

                        {ownedDenItems.map((den, idx) => {
                          const totalIdx = denTrophiesList.length + idx;
                          const isOpen = activeDenItemPopoverId === den.id;
                          const currentTier = shopItemTiers[den.id] ?? 1;
                          const maxTier = den.maxTier ?? 3;
                          const upgradeCost = (den.upgradeCostCoins ?? 40) * currentTier;

                          return (
                            <div
                              key={den.id}
                              className="relative"
                              data-popover-interactive="true"
                            >
                              <button
                                onClick={() =>
                                  setActiveDenItemPopoverId(isOpen ? null : den.id)
                                }
                                className="w-full min-h-[76px] rounded-xl p-1.5 border flex flex-col items-center justify-center gap-1 cursor-pointer transition-all"
                                style={{
                                  backgroundColor: isOpen ? '#36291E' : '#241B15',
                                  borderColor: isOpen ? '#F59E0B' : '#10B98166',
                                }}
                              >
                                <span className="text-xl leading-none">
                                  {getShopItemEmoji(den)}
                                </span>
                                <span className="text-[10px] font-bold text-[#FFF8EB] leading-tight w-full text-center break-words">
                                  {getShopItemShortTitle(den)}
                                </span>
                              </button>

                              {isOpen && (
                                <div
                                  className={`absolute z-50 w-[235px] rounded-2xl p-3 bg-[#241A13] border-2 border-[#F59E0B] shadow-[0_14px_34px_rgba(0,0,0,0.85)] space-y-2 animate-fadeIn ${getAdjacentPopoverClasses(
                                    totalIdx,
                                    4
                                  )}`}
                                >
                                  <div className="flex items-start justify-between gap-1.5">
                                    <span className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                                      {getShopItemEmoji(den)} {den.titleRu}
                                    </span>
                                    <button
                                      onClick={() => setActiveDenItemPopoverId(null)}
                                      className="text-[10px] text-[#D5C3AA] hover:text-white cursor-pointer"
                                    >
                                      ✕
                                    </button>
                                  </div>
                                  <div className="flex flex-wrap gap-1 text-[10px] font-bold">
                                    <span className="text-[#10B981]">🏠 В Домике (Т{currentTier}/3)</span>
                                    {den.bonusCoinsPerJourney && (
                                      <span className="text-[#F59E0B]">
                                        · 🪙 +{(den.bonusCoinsPerJourney ?? 0) + (currentTier - 1)} М/поход
                                      </span>
                                    )}
                                  </div>
                                  {currentTier < maxTier && (
                                    <div className="flex justify-end pt-1 border-t border-[#382B20]">
                                      <button
                                        onClick={() => {
                                          handleUpgradeShopItemTier(den);
                                          setActiveDenItemPopoverId(null);
                                        }}
                                        disabled={coins < upgradeCost}
                                        className="px-3 py-1.5 rounded-xl bg-[#EA580C] disabled:opacity-40 text-white text-[11px] font-extrabold cursor-pointer shadow-sm"
                                      >
                                        Улучшить · {upgradeCost} 🪙
                                      </button>
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Right Column: Streamlined Journey & Loadout Card + Compact Latest Find Bar (No Kegel Table, No Milestone/Energy Clutter) */}
          <div className="lg:col-span-6 space-y-4">
            {(isExhausted45 || debtCoins > 0) && (
              <div className="rounded-[24px] p-4 bg-[#2B1616] border border-[#EF4444] space-y-3">
                <div className="flex items-center gap-2 text-xs sm:text-sm font-extrabold text-[#FCA5A5]">
                  <AlertTriangle className="w-4 h-4 text-[#EF4444] shrink-0" />
                  <span>Последствия приключения</span>
                </div>

                {isExhausted45 && (
                  <div className="rounded-2xl p-3 bg-[#1C1010] space-y-2">
                    <div className="text-xs font-bold text-[#FCA5A5]">
                      Усталость после дороги: следующий поход требует{' '}
                      <span className="underline tabular-nums">{requiredJourneyEnergy} Энергии</span>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {herbsList.length > 0 && (
                        <button
                          onClick={() => handleUseHerbFromInventory(herbsList[0])}
                          className="min-h-[38px] px-3.5 rounded-xl bg-[#10B981] text-white text-xs font-extrabold cursor-pointer"
                        >
                          Заварить «{herbsList[0].titleRu}» (Бесплатно)
                        </button>
                      )}
                      <button
                        onClick={() => {
                          const balm = CHANTAL_SHOP_ITEMS.find(
                            (i) => i.id === 'supply_herbal_balm'
                          );
                          if (balm) handleBuyShopItem(balm, false);
                        }}
                        disabled={coins < 16}
                        className="min-h-[38px] px-3.5 rounded-xl bg-[#EA580C] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
                      >
                        Бальзам за 16 М
                      </button>
                    </div>
                  </div>
                )}

                {debtCoins > 0 && (
                  <div className="rounded-2xl p-3 bg-[#1C1010] space-y-2">
                    <div className="text-xs font-bold text-[#FCA5A5]">
                      Долг в монетах: {debtCoins} М ({debtReasonRu})
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <button
                        onClick={() => {
                          if (coins >= debtCoins) {
                            setCoins((c) => c - debtCoins);
                            setDebtCoins(0);
                            setDebtReasonRu('');
                            setLastReactionBanner('Долг полностью оплачен звонкими монетами!');
                          }
                        }}
                        disabled={coins < debtCoins}
                        className="min-h-[38px] px-3.5 rounded-xl bg-[#F59E0B] disabled:opacity-40 text-[#15100C] text-xs font-extrabold cursor-pointer"
                      >
                        Оплатить {debtCoins} М
                      </button>
                      <button
                        onClick={() => {
                          setCustomFillerInput(
                            'Отправляйся на ярмарку и помоги мастеру отработать наш долг!'
                          );
                        }}
                        className="min-h-[38px] px-3.5 rounded-xl bg-[#281F18] text-[#FFF8EB] text-xs font-bold cursor-pointer"
                      >
                        Вписать отработку в поручение
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* STREAMLINED EXPEDITION CARD (Graphical Amulet + 4 Distinct Colored Slots + Instant Backpack/Shop Picker + Unified Launch) */}
            <div className="rounded-[26px] p-4 sm:p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4 shadow-md">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h2 className="text-base sm:text-lg font-extrabold text-[#FFF8EB]">
                    {activeJourney
                      ? `Шанталь в путешествии #${totalJourneys + 1}`
                      : 'Сборы и отправка в поход'}
                  </h2>
                  {activeJourney && (
                    <p className="text-xs text-[#D5C3AA] mt-0.5">
                      {activeJourney.stayedInSameRegion
                        ? `Вернулась в «${activeJourney.regionTitleRu}» (${activeJourney.encounteredNpcRu})`
                        : `Маршрут: «${activeJourney.regionTitleRu}» (${activeJourney.encounteredNpcRu})`}
                    </p>
                  )}
                </div>

                {/* GRAPHICAL AMULET BADGE (Replaces verbose text banner) */}
                <button
                  onClick={() =>
                    setActiveLoadoutSlotPicker((prev) =>
                      prev === 'AMULET' ? 'NONE' : 'AMULET'
                    )
                  }
                  title="Оберег Попутного Ветра"
                  className="px-3 py-1.5 rounded-xl border flex items-center gap-1.5 text-xs font-extrabold cursor-pointer transition-all shrink-0"
                  style={{
                    backgroundColor: hasActiveRoadAmulet
                      ? '#2E1A36'
                      : activeLoadoutSlotPicker === 'AMULET'
                      ? '#2A2018'
                      : '#15100C',
                    borderColor: hasActiveRoadAmulet
                      ? '#A855F7'
                      : activeLoadoutSlotPicker === 'AMULET'
                      ? '#F59E0B'
                      : '#382B20',
                    color: hasActiveRoadAmulet ? '#E9D5FF' : '#C8B499',
                  }}
                >
                  <span className="text-sm leading-none">🧿</span>
                  {hasActiveRoadAmulet ? (
                    <span className="flex items-center gap-1.5 tabular-nums">
                      <span className="text-[#C084FC]">Амулет</span>
                      <span className="text-[#10B981]">🛡️ +100%</span>
                      <span className="text-[#F59E0B]">
                        🪙 +{ownedShopItemIds.includes('den_alchemy_table') ? '75%' : '50%'}
                      </span>
                    </span>
                  ) : (
                    <span>Амулет: Пусто</span>
                  )}
                </button>
              </div>

              {activeJourney ? (
                <div className="space-y-4">
                  <div className="rounded-2xl p-4 bg-[#281F18] border border-[#F59E0B]/40 text-center space-y-2">
                    <div className="text-xs font-semibold text-[#D5C3AA] flex items-center justify-center gap-1.5">
                      <Timer className="w-4 h-4 text-[#F59E0B]" />
                      <span>
                        {isJourneyFinished
                          ? 'Шанталь вернулась домой и ждёт тебя!'
                          : 'До возвращения из похода:'}
                      </span>
                    </div>
                    <div className="text-4xl font-extrabold tabular-nums text-[#F59E0B]">
                      {isJourneyFinished
                        ? 'Готово!'
                        : `${String(remainingMinutes).padStart(2, '0')}:${String(
                            remainingSeconds
                          ).padStart(2, '0')}`}
                    </div>
                    <p className="text-xs text-[#E7D8C3] leading-relaxed">
                      {activeRoadNoteRu}
                    </p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    <button
                      onClick={() => handleFinishJourneyAndReadStory(activeJourney, false)}
                      disabled={isGeneratingStory}
                      className="min-h-[48px] rounded-2xl bg-[#EA580C] hover:bg-[#D9460F] text-white font-extrabold text-sm flex items-center justify-center gap-2 cursor-pointer shadow-md"
                    >
                      <Sparkles className="w-4 h-4" />
                      <span>
                        {isGeneratingStory
                          ? 'Шанталь рассказывает историю...'
                          : isJourneyFinished
                          ? 'Встретить и прочитать историю'
                          : 'Позвать рожком и встретить у камина'}
                      </span>
                    </button>

                    <button
                      onClick={() => setSubView('map')}
                      className="min-h-[48px] rounded-2xl bg-[#281F18] border border-[#4D3D2C] text-[#FFF8EB] font-bold text-sm flex items-center justify-center gap-2 cursor-pointer"
                    >
                      <MapPin className="w-4 h-4 text-[#F59E0B]" />
                      <span>Путь на карте</span>
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-4">
                  {/* 1. 4 DISTINCT COLORED SLOTS: [ 🧥 Одежда ] · [ 🧭 Инструмент ] · [ 🗡️ Оружие ] · [ 🧺 Узелок ] */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    {/* SLOT 1: OUTFIT (Sapphire Blue #3B82F6) */}
                    {(() => {
                      const item = activeBackpackLoadoutSummary.outfitItem;
                      const isOpen = activeLoadoutSlotPicker === 'OUTFIT';
                      const remDur = item
                        ? gearDurabilityMap[item.id] ?? item.maxDurability ?? 5
                        : 0;
                      return (
                        <button
                          onClick={() => {
                            setActiveSlotInspectId(null);
                            setActiveLoadoutSlotPicker(isOpen ? 'NONE' : 'OUTFIT');
                          }}
                          className="p-3 rounded-2xl border-2 text-left flex flex-col justify-between gap-2 cursor-pointer transition-all"
                          style={{
                            backgroundColor: item
                              ? '#172336'
                              : isOpen
                              ? '#1B283B'
                              : '#151922',
                            borderColor:
                              item || isOpen ? '#3B82F6' : '#263449',
                          }}
                        >
                          <div className="flex items-center justify-between gap-1">
                            <span className="text-xl leading-none">
                              {item ? getShopItemEmoji(item) : '🧥'}
                            </span>
                            {item && (
                              <span className="px-1.5 py-0.5 rounded-md bg-black/40 text-[9px] font-extrabold text-[#93C5FD] tabular-nums">
                                {remDur}/{item.maxDurability ?? 5}
                              </span>
                            )}
                          </div>
                          <div
                            className="text-xs font-extrabold leading-tight break-words"
                            style={{ color: item ? '#FFF8EB' : '#93C5FD' }}
                          >
                            {item ? getShopItemShortTitle(item) : 'Пусто'}
                          </div>
                        </button>
                      );
                    })()}

                    {/* SLOT 2: TOOL (Amber Gold #F59E0B) */}
                    {(() => {
                      const item = activeBackpackLoadoutSummary.toolItem;
                      const isOpen = activeLoadoutSlotPicker === 'TOOL';
                      const remDur = item
                        ? gearDurabilityMap[item.id] ?? item.maxDurability ?? 5
                        : 0;
                      return (
                        <button
                          onClick={() => {
                            setActiveSlotInspectId(null);
                            setActiveLoadoutSlotPicker(isOpen ? 'NONE' : 'TOOL');
                          }}
                          className="p-3 rounded-2xl border-2 text-left flex flex-col justify-between gap-2 cursor-pointer transition-all"
                          style={{
                            backgroundColor: item
                              ? '#2E2113'
                              : isOpen
                              ? '#332516'
                              : '#211912',
                            borderColor:
                              item || isOpen ? '#F59E0B' : '#4A3620',
                          }}
                        >
                          <div className="flex items-center justify-between gap-1">
                            <span className="text-xl leading-none">
                              {item ? getShopItemEmoji(item) : '🧭'}
                            </span>
                            {item && (
                              <span className="px-1.5 py-0.5 rounded-md bg-black/40 text-[9px] font-extrabold text-[#FCD34D] tabular-nums">
                                {remDur}/{item.maxDurability ?? 5}
                              </span>
                            )}
                          </div>
                          <div
                            className="text-xs font-extrabold leading-tight break-words"
                            style={{ color: item ? '#FFF8EB' : '#FCD34D' }}
                          >
                            {item ? getShopItemShortTitle(item) : 'Пусто'}
                          </div>
                        </button>
                      );
                    })()}

                    {/* SLOT 3: WEAPON / DEFENSE (Ruby Red #EF4444) */}
                    {(() => {
                      const item = activeBackpackLoadoutSummary.weaponItem;
                      const isOpen = activeLoadoutSlotPicker === 'WEAPON';
                      const remDur = item
                        ? gearDurabilityMap[item.id] ?? item.maxDurability ?? 4
                        : 0;
                      return (
                        <button
                          onClick={() => {
                            setActiveSlotInspectId(null);
                            setActiveLoadoutSlotPicker(isOpen ? 'NONE' : 'WEAPON');
                          }}
                          className="p-3 rounded-2xl border-2 text-left flex flex-col justify-between gap-2 cursor-pointer transition-all"
                          style={{
                            backgroundColor: item
                              ? '#301619'
                              : isOpen
                              ? '#36191D'
                              : '#221315',
                            borderColor:
                              item || isOpen ? '#EF4444' : '#4C2428',
                          }}
                        >
                          <div className="flex items-center justify-between gap-1">
                            <span className="text-xl leading-none">
                              {item ? getShopItemEmoji(item) : '🗡️'}
                            </span>
                            {item && (
                              <span className="px-1.5 py-0.5 rounded-md bg-black/40 text-[9px] font-extrabold text-[#FCA5A5] tabular-nums">
                                {remDur}/{item.maxDurability ?? 4}
                              </span>
                            )}
                          </div>
                          <div
                            className="text-xs font-extrabold leading-tight break-words"
                            style={{ color: item ? '#FFF8EB' : '#FCA5A5' }}
                          >
                            {item ? getShopItemShortTitle(item) : 'Пусто'}
                          </div>
                        </button>
                      );
                    })()}

                    {/* SLOT 4: BUNDLE / УЗЕЛОК (Emerald Green #10B981) */}
                    {(() => {
                      const prov = activeBackpackLoadoutSummary.activeProvisionItem;
                      const loot = activeBackpackLoadoutSummary.activeBundleLootItem;
                      const isOpen = activeLoadoutSlotPicker === 'BUNDLE';
                      const hasAny = Boolean(prov || loot);
                      const count = prov ? pantryProvisionCounts[prov.id] ?? 0 : 1;
                      return (
                        <button
                          onClick={() => {
                            setActiveSlotInspectId(null);
                            setActiveLoadoutSlotPicker(isOpen ? 'NONE' : 'BUNDLE');
                          }}
                          className="p-3 rounded-2xl border-2 text-left flex flex-col justify-between gap-2 cursor-pointer transition-all"
                          style={{
                            backgroundColor: hasAny
                              ? '#132A22'
                              : isOpen
                              ? '#163027'
                              : '#12201B',
                            borderColor:
                              hasAny || isOpen ? '#10B981' : '#214235',
                          }}
                        >
                          <div className="flex items-center justify-between gap-1">
                            <span className="text-xl leading-none">
                              {prov
                                ? getShopItemEmoji(prov)
                                : loot
                                ? getLootItemEmoji(loot)
                                : '🧺'}
                            </span>
                            {hasAny && (
                              <span className="px-1.5 py-0.5 rounded-md bg-black/40 text-[9px] font-extrabold text-[#6EE7B7] tabular-nums">
                                {prov ? `×${count}` : 'Сумка'}
                              </span>
                            )}
                          </div>
                          <div
                            className="text-xs font-extrabold leading-tight break-words"
                            style={{ color: hasAny ? '#FFF8EB' : '#6EE7B7' }}
                          >
                            {prov
                              ? getShopItemShortTitle(prov)
                              : loot
                              ? getLootItemShortTitle(loot)
                              : 'Пусто'}
                          </div>
                        </button>
                      );
                    })()}
                  </div>

                  {/* DEDICATED SLOT DRAWER WHEN A SLOT OR AMULET IS CLICKED */}
                  {activeLoadoutSlotPicker === 'AMULET' && (
                    <div
                      className="rounded-2xl p-3.5 bg-[#1B1320] border-2 border-[#A855F7] space-y-2.5 animate-fadeIn"
                      data-popover-interactive="true"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-extrabold text-[#E9D5FF]">
                          🧿 Оберег Попутного Ветра (🛡️ +100% · 🪙 +{ownedShopItemIds.includes('den_alchemy_table') ? '75%' : '50%'})
                        </span>
                        <button
                          onClick={() => setActiveLoadoutSlotPicker('NONE')}
                          className="px-2 py-0.5 rounded-lg bg-black/40 text-[10px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer"
                        >
                          ✕
                        </button>
                      </div>

                      {hasActiveRoadAmulet ? (
                        <div className="text-xs text-[#6EE7B7] font-bold">
                          ✓ Амулет заряжен и защитит Шанталь в ближайшем походе!
                        </div>
                      ) : (
                        <div className="flex flex-wrap gap-2">
                          <button
                            onClick={() => {
                              handleCraftRoadAmuletInInventory();
                              setActiveLoadoutSlotPicker('NONE');
                            }}
                            disabled={
                              herbsList.length === 0 ||
                              (herbsList.length < 2 && trinketsList.length === 0)
                            }
                            className="min-h-[38px] px-3.5 rounded-xl bg-[#A855F7] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
                          >
                            ⚗️ Сварить из Сумки (1 Трава + 1 Находка)
                          </button>

                          {(() => {
                            const amuletOffer = TRAVELING_MERCHANT_OFFERS.find(
                              (o) => o.rewardType === 'ROAD_AMULET'
                            );
                            if (!amuletOffer) return null;
                            const isClaimed = claimedMerchantOfferIds.includes(amuletOffer.id);
                            return (
                              <button
                                onClick={() => {
                                  handleBuyTravelingMerchantOffer(amuletOffer);
                                  setActiveLoadoutSlotPicker('NONE');
                                }}
                                disabled={isClaimed || coins < amuletOffer.discountPriceCoins}
                                className="min-h-[38px] px-3.5 rounded-xl bg-[#281F18] border border-[#F59E0B] disabled:opacity-40 text-[#F59E0B] text-xs font-extrabold cursor-pointer"
                              >
                                🛒 Купить ({amuletOffer.discountPriceCoins} 🪙)
                              </button>
                            );
                          })()}
                        </div>
                      )}
                    </div>
                  )}

                  {(activeLoadoutSlotPicker === 'OUTFIT' ||
                    activeLoadoutSlotPicker === 'TOOL' ||
                    activeLoadoutSlotPicker === 'WEAPON') &&
                    (() => {
                      const slotType = activeLoadoutSlotPicker;
                      const slotMeta =
                        slotType === 'OUTFIT'
                          ? {
                              titleRu: '🧥 Одежда',
                              accentHex: '#3B82F6',
                              bgHex: '#131A24',
                              selectedId: selectedSlotOutfitId,
                            }
                          : slotType === 'TOOL'
                          ? {
                              titleRu: '🧭 Инструмент',
                              accentHex: '#F59E0B',
                              bgHex: '#211912',
                              selectedId: selectedSlotToolId,
                            }
                          : {
                              titleRu: '🗡️ Оружие',
                              accentHex: '#EF4444',
                              bgHex: '#221315',
                              selectedId: selectedSlotWeaponId,
                            };

                      const allSlotItems = CHANTAL_SHOP_ITEMS.filter(
                        (i) =>
                          i.category === 'APPAREL' &&
                          (i.loadoutSlotType ?? 'OUTFIT') === slotType
                      );

                      return (
                        <div
                          className="rounded-2xl p-3 border-2 space-y-2.5 animate-fadeIn"
                          style={{
                            backgroundColor: slotMeta.bgHex,
                            borderColor: slotMeta.accentHex,
                          }}
                        >
                          <div className="flex items-center justify-between gap-2">
                            <span
                              className="text-xs font-extrabold"
                              style={{ color: slotMeta.accentHex }}
                            >
                              {slotMeta.titleRu}
                            </span>
                            <button
                              onClick={() => {
                                setActiveSlotInspectId(null);
                                setActiveLoadoutSlotPicker('NONE');
                              }}
                              className="px-2 py-0.5 rounded-lg bg-black/40 text-[10px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer"
                            >
                              ✕
                            </button>
                          </div>

                          {/* COMPACT SMALL ICON TILES + RIGHT-SIDE POPOVER CARD ON TAP */}
                          <div className="grid grid-cols-4 gap-2">
                            {allSlotItems.map((gear, idx) => {
                              const isOwned = ownedShopItemIds.includes(gear.id);
                              const maxDur = gear.maxDurability ?? 5;
                              const remDur = isOwned
                                ? gearDurabilityMap[gear.id] ?? maxDur
                                : maxDur;
                              const isEquipped =
                                slotMeta.selectedId === gear.id && isOwned && remDur > 0;
                              const isInspectOpen = activeSlotInspectId === gear.id;
                              const refillCost = gear.refillCostCoins ?? 10;

                              return (
                                <div
                                  key={gear.id}
                                  className="relative"
                                  data-popover-interactive="true"
                                >
                                  <button
                                    onClick={() =>
                                      setActiveSlotInspectId(isInspectOpen ? null : gear.id)
                                    }
                                    className="w-full min-h-[82px] rounded-2xl border p-2 flex flex-col items-center justify-center gap-1 cursor-pointer transition-all"
                                    style={{
                                      backgroundColor: isInspectOpen
                                        ? '#36291E'
                                        : isEquipped
                                        ? '#261E16'
                                        : '#15100C',
                                      borderColor: isInspectOpen
                                        ? '#F59E0B'
                                        : isEquipped
                                        ? slotMeta.accentHex
                                        : isOwned
                                        ? '#5C4834'
                                        : '#382B20',
                                      boxShadow: isInspectOpen
                                        ? '0 0 0 2px rgba(245,158,11,0.5)'
                                        : 'none',
                                    }}
                                  >
                                    <span className="text-2xl leading-none select-none">
                                      {getShopItemEmoji(gear)}
                                    </span>
                                    <span className="text-[10px] font-extrabold text-[#FFF8EB] leading-tight w-full text-center break-words">
                                      {getShopItemShortTitle(gear)}
                                    </span>
                                    <span
                                      className="text-[9px] font-extrabold tabular-nums"
                                      style={{
                                        color: isEquipped
                                          ? '#10B981'
                                          : isOwned
                                          ? '#93C5FD'
                                          : '#F59E0B',
                                      }}
                                    >
                                      {isEquipped
                                        ? `✓ ${remDur}/${maxDur}`
                                        : isOwned
                                        ? `${remDur}/${maxDur}`
                                        : `${gear.priceCoins} 🪙`}
                                    </span>
                                  </button>

                                  {isInspectOpen && (
                                    <div
                                      className={`absolute z-50 w-[235px] sm:w-[260px] rounded-2xl p-3 bg-[#241A13] border-2 shadow-[0_16px_38px_rgba(0,0,0,0.9)] space-y-2 animate-fadeIn ${getAdjacentPopoverClasses(
                                        idx,
                                        4
                                      )}`}
                                      style={{ borderColor: slotMeta.accentHex }}
                                    >
                                      <div className="flex items-start justify-between gap-1.5">
                                        <div className="flex items-center gap-1.5 min-w-0">
                                          <span className="text-lg shrink-0">
                                            {getShopItemEmoji(gear)}
                                          </span>
                                          <span className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                                            {gear.titleRu}
                                          </span>
                                        </div>
                                        <button
                                          onClick={() => setActiveSlotInspectId(null)}
                                          className="text-[10px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer shrink-0"
                                        >
                                          ✕
                                        </button>
                                      </div>

                                      {/* Key Stats */}
                                      <div className="flex flex-wrap gap-1.5 text-[10px] font-extrabold">
                                        {gear.preventsMishapBonusPct && (
                                          <span className="px-2 py-0.5 rounded-lg bg-[#1E293B] text-[#93C5FD]">
                                            🛡️ +{gear.preventsMishapBonusPct}% защита
                                          </span>
                                        )}
                                        {gear.expeditionCoinMultiplierPct && (
                                          <span className="px-2 py-0.5 rounded-lg bg-[#382B20] text-[#FCD34D]">
                                            🪙 +{gear.expeditionCoinMultiplierPct}% монет
                                          </span>
                                        )}
                                        {gear.denTrophyChanceBonusPct && (
                                          <span className="px-2 py-0.5 rounded-lg bg-[#064E3B] text-[#6EE7B7]">
                                            🏠 +{gear.denTrophyChanceBonusPct}% трофей
                                          </span>
                                        )}
                                      </div>

                                      <p className="text-[11px] text-[#E7D8C3] leading-snug">
                                        {gear.descriptionRu}
                                      </p>

                                      {/* Bottom-Right Action Button */}
                                      <div className="flex items-center justify-between gap-2 pt-1.5 border-t border-[#382B20]">
                                        <span className="text-[11px] font-extrabold text-[#F59E0B] tabular-nums">
                                          {isOwned
                                            ? `Запас: ${remDur}/${maxDur}`
                                            : `${gear.priceCoins} 🪙`}
                                        </span>

                                        <div className="flex items-center gap-1.5">
                                          {isOwned && remDur < maxDur && (
                                            <button
                                              onClick={() => {
                                                handleRefillGearDurability(gear);
                                                setActiveSlotInspectId(null);
                                              }}
                                              disabled={coins < refillCost}
                                              className="px-2.5 py-1.5 rounded-xl bg-[#281F18] border border-[#F59E0B] disabled:opacity-40 text-[10px] font-extrabold text-[#F59E0B] cursor-pointer"
                                            >
                                              Починить ({refillCost} 🪙)
                                            </button>
                                          )}

                                          {isOwned ? (
                                            <button
                                              onClick={() => {
                                                handleToggleBackpackGear(gear.id);
                                                setActiveSlotInspectId(null);
                                              }}
                                              disabled={remDur <= 0}
                                              className="px-3.5 py-1.5 rounded-xl text-xs font-extrabold text-white cursor-pointer shadow-md"
                                              style={{
                                                backgroundColor: isEquipped
                                                  ? '#4D3D2C'
                                                  : slotMeta.accentHex,
                                              }}
                                            >
                                              {isEquipped ? 'Снять' : 'Взять'}
                                            </button>
                                          ) : (
                                            <button
                                              onClick={() => {
                                                if (coins < gear.priceCoins) {
                                                  setLastReactionBanner(
                                                    `Не хватает монет: нужно ${gear.priceCoins} 🪙 (в кошельке ${coins} 🪙).`
                                                  );
                                                  return;
                                                }
                                                handleBuyShopItem(gear);
                                                setActiveSlotInspectId(null);
                                              }}
                                              className="px-3.5 py-1.5 rounded-xl bg-[#EA580C] hover:bg-[#D9460F] text-white text-xs font-extrabold cursor-pointer shadow-md"
                                            >
                                              Купить · {gear.priceCoins} 🪙
                                            </button>
                                          )}
                                        </div>
                                      </div>
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      );
                    })()}

                  {activeLoadoutSlotPicker === 'BUNDLE' && (
                    <div className="rounded-2xl p-3 bg-[#12201B] border-2 border-[#10B981] space-y-2.5 animate-fadeIn">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-extrabold text-[#6EE7B7]">
                          🧺 Узелок в дорогу
                        </span>
                        <button
                          onClick={() => {
                            setActiveSlotInspectId(null);
                            setActiveLoadoutSlotPicker('NONE');
                          }}
                          className="px-2 py-0.5 rounded-lg bg-black/40 text-[10px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer"
                        >
                          ✕
                        </button>
                      </div>

                      {/* COMPACT SMALL ICON TILES FOR ALL BUNDLE ITEMS (Shop Provisions + Bag Herbs/Trinkets) */}
                      <div className="grid grid-cols-4 sm:grid-cols-6 gap-2">
                        {CHANTAL_SHOP_ITEMS.filter((i) => i.category === 'CONSUMABLE').map(
                          (prov, idx) => {
                            const count = pantryProvisionCounts[prov.id] ?? 0;
                            const isPacked =
                              selectedBackpackProvisionId === prov.id && count > 0;
                            const isInspectOpen = activeSlotInspectId === prov.id;

                            return (
                              <div
                                key={prov.id}
                                className="relative"
                                data-popover-interactive="true"
                              >
                                <button
                                  onClick={() =>
                                    setActiveSlotInspectId(isInspectOpen ? null : prov.id)
                                  }
                                  className="w-full min-h-[82px] rounded-2xl border p-2 flex flex-col items-center justify-center gap-1 cursor-pointer transition-all"
                                  style={{
                                    backgroundColor: isInspectOpen
                                      ? '#36291E'
                                      : isPacked
                                      ? '#1F382E'
                                      : '#15100C',
                                    borderColor: isInspectOpen
                                      ? '#F59E0B'
                                      : isPacked
                                      ? '#10B981'
                                      : '#382B20',
                                    boxShadow: isInspectOpen
                                      ? '0 0 0 2px rgba(245,158,11,0.5)'
                                      : 'none',
                                  }}
                                >
                                  <span className="text-2xl leading-none select-none">
                                    {getShopItemEmoji(prov)}
                                  </span>
                                  <span className="text-[10px] font-extrabold text-[#FFF8EB] leading-tight w-full text-center break-words">
                                    {getShopItemShortTitle(prov)}
                                  </span>
                                  <span
                                    className="text-[9px] font-extrabold tabular-nums"
                                    style={{
                                      color: isPacked
                                        ? '#6EE7B7'
                                        : count > 0
                                        ? '#10B981'
                                        : '#F59E0B',
                                    }}
                                  >
                                    {isPacked
                                      ? `✓ ×${count}`
                                      : count > 0
                                      ? `×${count}`
                                      : `${prov.priceCoins} 🪙`}
                                  </span>
                                </button>

                                {isInspectOpen && (
                                  <div
                                    className={`absolute z-50 w-[235px] sm:w-[260px] rounded-2xl p-3 bg-[#241A13] border-2 border-[#10B981] shadow-[0_16px_38px_rgba(0,0,0,0.9)] space-y-2 animate-fadeIn ${getAdjacentPopoverClasses(
                                      idx,
                                      4
                                    )}`}
                                  >
                                    <div className="flex items-start justify-between gap-1.5">
                                      <div className="flex items-center gap-1.5 min-w-0">
                                        <span className="text-lg shrink-0">
                                          {getShopItemEmoji(prov)}
                                        </span>
                                        <span className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                                          {prov.titleRu}
                                        </span>
                                      </div>
                                      <button
                                        onClick={() => setActiveSlotInspectId(null)}
                                        className="text-[10px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer shrink-0"
                                      >
                                        ✕
                                      </button>
                                    </div>

                                    <div className="flex flex-wrap gap-1.5 text-[10px] font-extrabold">
                                      {prov.id === 'supply_honey_pie' && (
                                        <span className="px-2 py-0.5 rounded-lg bg-[#064E3B] text-[#6EE7B7]">
                                          ⚡ Защита от усталости · 🪙 +10%
                                        </span>
                                      )}
                                      {prov.id === 'supply_herbal_balm' && (
                                        <span className="px-2 py-0.5 rounded-lg bg-[#064E3B] text-[#6EE7B7]">
                                          🌿 Снимает усталость (30 Эн.)
                                        </span>
                                      )}
                                      {prov.expeditionCoinMultiplierPct && (
                                        <span className="px-2 py-0.5 rounded-lg bg-[#382B20] text-[#FCD34D]">
                                          🪙 +{prov.expeditionCoinMultiplierPct}% монет
                                        </span>
                                      )}
                                      {prov.denTrophyChanceBonusPct && (
                                        <span className="px-2 py-0.5 rounded-lg bg-[#064E3B] text-[#6EE7B7]">
                                          🏠 +{prov.denTrophyChanceBonusPct}% трофей
                                        </span>
                                      )}
                                    </div>

                                    <p className="text-[11px] text-[#E7D8C3] leading-snug">
                                      {prov.descriptionRu}
                                    </p>

                                    <div className="flex items-center justify-between gap-2 pt-1.5 border-t border-[#382B20]">
                                      <span className="text-[11px] font-extrabold text-[#F59E0B] tabular-nums">
                                        {count > 0 ? `Запас: ${count} шт.` : `${prov.priceCoins} 🪙`}
                                      </span>

                                      <div className="flex items-center gap-1.5">
                                        {count > 0 && (
                                          <button
                                            onClick={() => {
                                              handleToggleBackpackProvision(prov.id);
                                              setActiveSlotInspectId(null);
                                            }}
                                            className="px-3 py-1.5 rounded-xl text-xs font-extrabold text-white cursor-pointer shadow-md"
                                            style={{
                                              backgroundColor: isPacked ? '#4D3D2C' : '#10B981',
                                            }}
                                          >
                                            {isPacked ? 'Убрать' : 'Взять'}
                                          </button>
                                        )}
                                        <button
                                          onClick={() => {
                                            if (coins < prov.priceCoins) {
                                              setLastReactionBanner(
                                                `Не хватает монет: нужно ${prov.priceCoins} 🪙 (в кошельке ${coins} 🪙).`
                                              );
                                              return;
                                            }
                                            handleBuyShopItem(prov, true);
                                            setActiveSlotInspectId(null);
                                          }}
                                          className="px-3 py-1.5 rounded-xl bg-[#EA580C] hover:bg-[#D9460F] text-white text-xs font-extrabold cursor-pointer shadow-md"
                                        >
                                          Купить · {prov.priceCoins} 🪙
                                        </button>
                                      </div>
                                    </div>
                                  </div>
                                )}
                              </div>
                            );
                          }
                        )}

                        {[...herbsList, ...trinketsList].map((loot, idx) => {
                          const totalIdx = 3 + idx;
                          const isPacked = selectedBundleLootItemId === loot.id;
                          const isInspectOpen = activeSlotInspectId === loot.id;

                          return (
                            <div
                              key={loot.id}
                              className="relative"
                              data-popover-interactive="true"
                            >
                              <button
                                onClick={() =>
                                  setActiveSlotInspectId(isInspectOpen ? null : loot.id)
                                }
                                className="w-full min-h-[82px] rounded-2xl border p-2 flex flex-col items-center justify-center gap-1 cursor-pointer transition-colors animate-bag-slot-drop"
                                style={{
                                  animationDelay: `${Math.min(idx * 45, 300)}ms`,
                                  backgroundColor: isInspectOpen
                                    ? '#36291E'
                                    : isPacked
                                    ? '#1F382E'
                                    : '#15100C',
                                  borderColor: isInspectOpen
                                    ? '#F59E0B'
                                    : isPacked
                                    ? '#10B981'
                                    : '#382B20',
                                }}
                              >
                                <span
                                  className="text-2xl leading-none select-none inline-block animate-bag-icon-drop"
                                  style={{ animationDelay: `${Math.min(idx * 45 + 50, 350)}ms` }}
                                >
                                  {getLootItemEmoji(loot)}
                                </span>
                                <span className="text-[10px] font-extrabold text-[#FFF8EB] leading-tight w-full text-center break-words">
                                  {getLootItemShortTitle(loot)}
                                </span>
                                <span className="text-[9px] font-extrabold text-[#6EE7B7]">
                                  {isPacked ? '✓ В узелке' : 'Сумка'}
                                </span>
                              </button>

                              {isInspectOpen && (
                                <div
                                  className={`absolute z-50 w-[235px] sm:w-[260px] rounded-2xl p-3 bg-[#241A13] border-2 border-[#10B981] shadow-[0_16px_38px_rgba(0,0,0,0.9)] space-y-2 animate-fadeIn ${getAdjacentPopoverClasses(
                                    totalIdx,
                                    4
                                  )}`}
                                >
                                  <div className="flex items-start justify-between gap-1.5">
                                    <span className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                                      {getLootItemEmoji(loot)} {loot.titleRu}
                                    </span>
                                    <button
                                      onClick={() => setActiveSlotInspectId(null)}
                                      className="text-[10px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer shrink-0"
                                    >
                                      ✕
                                    </button>
                                  </div>

                                  <div className="text-[10px] font-extrabold text-[#6EE7B7]">
                                    {loot.category === 'HERB'
                                      ? '🌿 Защита от усталости и +20% от неудач в пути'
                                      : '🔮 Обмен с жителями: +20% монет и +15% шанс трофея'}
                                  </div>

                                  <p className="text-[11px] text-[#E7D8C3] leading-snug">
                                    {loot.descriptionRu}
                                  </p>

                                  <div className="flex justify-end pt-1.5 border-t border-[#382B20]">
                                    <button
                                      onClick={() => {
                                        handleToggleBundleLootItem(loot.id);
                                        setActiveSlotInspectId(null);
                                      }}
                                      className="px-3.5 py-1.5 rounded-xl text-xs font-extrabold text-white cursor-pointer shadow-md"
                                      style={{
                                        backgroundColor: isPacked ? '#4D3D2C' : '#10B981',
                                      }}
                                    >
                                      {isPacked ? 'Убрать из узелка' : 'Положить в узелок'}
                                    </button>
                                  </div>
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}

                  {/* 2. UNIFIED JOURNEY LAUNCH + OPTIONAL PERSONAL ERRAND INPUT (1 Input + 1 Smart Button) */}
                  <div className="space-y-2.5">
                    <div className="relative">
                      <input
                        type="text"
                        value={customFillerInput}
                        onChange={(e) => setCustomFillerInput(e.target.value)}
                        placeholder="🪶 Пожелание или личное поручение в дорогу (необязательно)..."
                        className="w-full min-h-[42px] px-3.5 pr-8 rounded-xl bg-[#15100C] border border-[#382B20] focus:border-[#F59E0B] focus:outline-none text-xs text-[#FFF8EB] placeholder-[#8C7A65]"
                      />
                      {customFillerInput.trim() && (
                        <button
                          onClick={() => setCustomFillerInput('')}
                          title="Очистить поручение"
                          className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs font-bold text-[#8C7A65] hover:text-white cursor-pointer"
                        >
                          ✕
                        </button>
                      )}
                    </div>

                    <button
                      onClick={() => {
                        const trimmed = customFillerInput.trim();
                        if (trimmed) {
                          handleStartAutonomousJourney(true, trimmed);
                          setCustomFillerInput('');
                        } else {
                          handleStartAutonomousJourney(false);
                        }
                      }}
                      disabled={energy < requiredJourneyEnergy}
                      className="w-full min-h-[50px] px-4 rounded-2xl bg-[#EA580C] hover:bg-[#D9460F] disabled:opacity-40 text-white font-extrabold text-sm flex items-center justify-center gap-2 cursor-pointer transition-transform active:scale-98 shadow-md"
                    >
                      <Compass className="w-5 h-5 shrink-0" />
                      <span>
                        {customFillerInput.trim()
                          ? `Отправить с поручением · ⚡ ${requiredJourneyEnergy}`
                          : activeBackpackLoadoutSummary.isGoingLight
                          ? `Отправить в поход · ⚡ ${requiredJourneyEnergy}`
                          : `Отправить со снаряжением · ⚡ ${requiredJourneyEnergy}`}
                      </span>
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* COMPACT 1-LINE SUMMARY LINK TO BAG OF FINDS */}
            <div className="rounded-[24px] p-3.5 bg-[#1E1712] border border-[#4D3D2C] flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2.5 min-w-0">
                <Backpack className="w-4 h-4 text-[#F59E0B] shrink-0" />
                <div className="min-w-0">
                  <div className="text-xs font-extrabold text-[#FFF8EB] leading-snug">
                    {inventoryItems[0]
                      ? `${getLootItemEmoji(inventoryItems[0])} ${inventoryItems[0].titleRu}`
                      : 'Сумка находок пуста'}
                  </div>
                  <div className="text-[11px] text-[#D5C3AA]">
                    В сумке: {inventoryItems.length} · Улики: {royalCluesList.length}/7 · Трофеи в Дом: {denTrophiesList.length}
                  </div>
                </div>
              </div>
              <button
                onClick={() => setSubView('inventory')}
                className="min-h-[34px] px-3 rounded-xl bg-[#281F18] hover:bg-[#33271D] border border-[#F59E0B]/50 text-xs font-extrabold text-[#F59E0B] cursor-pointer shrink-0"
              >
                В сумку →
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SUBVIEW 2: AUTONOMOUS PARCHMENT WORLD MAP */}
      {subView === 'map' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-xl font-extrabold text-[#FFF8EB]">
                Пергаментная карта странствий Шанталь
              </h2>
              <p className="text-xs text-[#D5C3AA] mt-0.5">
                Шанталь сама выбирает маршрут и может возвращаться в одну и ту же локацию подряд, если у неё там остались незаконченные дела или несколько собеседников
              </p>
            </div>
            <span className="text-xs font-bold text-[#F59E0B]">
              Открыто локаций: {WORLD_MAP_REGIONS.filter((r) => totalJourneys >= r.minJourneysRequired).length} из {WORLD_MAP_REGIONS.length}
            </span>
          </div>

          <div className="relative rounded-2xl overflow-hidden border-2 border-[#5C4834] aspect-video w-full bg-[#2A2018]">
            <img
              src={WORLD_MAP_IMAGE_URL}
              alt="Карта мира Шанталь в стиле Хоббита"
              referrerPolicy="no-referrer"
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-black/20" />

            {WORLD_MAP_REGIONS.map((reg) => {
              const isUnlocked = totalJourneys >= reg.minJourneysRequired;
              const isCurrentActive = activeJourney?.regionId === reg.id;
              const isLastVisited = !activeJourney && journalHistory[0]?.regionId === reg.id;

              return (
                <div
                  key={reg.id}
                  style={{
                    left: `${reg.coordXPercent}%`,
                    top: `${reg.coordYPercent}%`,
                  }}
                  className="absolute -translate-x-1/2 -translate-y-1/2 flex flex-col items-center"
                >
                  <div
                    className="px-2.5 py-1 rounded-xl text-[11px] font-extrabold shadow-lg border whitespace-nowrap"
                    style={{
                      backgroundColor: isCurrentActive
                        ? '#EA580C'
                        : isLastVisited
                        ? '#F59E0B'
                        : isUnlocked
                        ? '#1E1712E6'
                        : '#15100CCC',
                      color: isCurrentActive || isLastVisited ? '#FFFFFF' : isUnlocked ? '#FFF8EB' : '#8C7A65',
                      borderColor: isCurrentActive ? '#FFF8EB' : '#5C4834',
                    }}
                  >
                    {isCurrentActive
                      ? `🦊 Шанталь здесь: ${reg.titleRu}`
                      : isLastVisited
                      ? `★ Последний поход: ${reg.titleRu}`
                      : isUnlocked
                      ? reg.titleRu
                      : `🔒 ${reg.titleRu} (с ${reg.minJourneysRequired} пох.)`}
                  </div>
                </div>
              );
            })}
          </div>

          {/* COMPACT LOCATION ICON ROW WITH ADJACENT POPOVER */}
          <div className="grid grid-cols-3 sm:grid-cols-5 gap-2">
            {WORLD_MAP_REGIONS.map((reg, idx) => {
              const isUnlocked = totalJourneys >= reg.minJourneysRequired;
              const isSelected = selectedMapRegionId === reg.id;
              return (
                <div
                  key={reg.id}
                  className="relative"
                  data-popover-interactive="true"
                >
                  <button
                    onClick={() =>
                      setSelectedMapRegionId((prev) =>
                        prev === reg.id ? null : reg.id
                      )
                    }
                    className="w-full p-2.5 rounded-2xl border flex flex-col items-center justify-center gap-1 cursor-pointer transition-all"
                    style={{
                      backgroundColor: isSelected ? '#36291E' : '#15100C',
                      borderColor: isSelected
                        ? '#F59E0B'
                        : isUnlocked
                        ? '#5C4834'
                        : '#382B20',
                      opacity: isUnlocked ? 1 : 0.65,
                    }}
                  >
                    <span className="text-2xl leading-none select-none">
                      {isUnlocked ? getRegionEmoji(reg.id) : '🔒'}
                    </span>
                    <span className="text-[11px] font-extrabold text-[#FFF8EB] leading-tight w-full text-center break-words">
                      {getRegionShortTitle(reg.id)}
                    </span>
                  </button>

                  {isSelected && (
                    <div
                      className={`absolute z-40 w-[235px] sm:w-[270px] rounded-2xl p-3 bg-[#261C15] border-2 border-[#F59E0B] shadow-2xl space-y-2 ${getAdjacentPopoverClasses(
                        idx % 3,
                        3
                      )}`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex items-center gap-1.5 min-w-0">
                          <span className="text-lg shrink-0">
                            {getRegionEmoji(reg.id)}
                          </span>
                          <div className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                            {reg.titleRu}
                          </div>
                        </div>
                        <button
                          onClick={() => setSelectedMapRegionId(null)}
                          className="text-[11px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer"
                        >
                          ✕
                        </button>
                      </div>
                      <div className="px-2 py-0.5 rounded-lg bg-[#15100C] border border-[#5C4834] text-[10px] font-extrabold text-[#F59E0B] inline-block">
                        {isUnlocked
                          ? 'Тропа открыта ✓'
                          : `Откроется с ${reg.minJourneysRequired}-го похода`}
                      </div>
                      <p className="text-[11px] italic font-serif text-[#FAF0E1] leading-snug">
                        «{reg.loreRu}»
                      </p>
                      <div className="flex flex-wrap gap-1 pt-0.5">
                        {reg.npcs.map((n, i) => (
                          <span
                            key={i}
                            className="px-2 py-0.5 rounded-lg bg-[#1E1712] border border-[#10B981]/40 text-[10px] font-semibold text-[#10B981]"
                          >
                            👤 {n.nameRu}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* SUBVIEW 3: СУМКА НАХОДОК (COMPACT ICON SLOTS + ADJACENT SLIDE-OUT POPOVER + COLLAPSIBLE WORKSHOPS) */}
      {subView === 'inventory' && (
        <section className="rounded-[26px] p-4 sm:p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg sm:text-xl font-extrabold text-[#FFF8EB] flex items-center gap-2">
              <Backpack className="w-5 h-5 text-[#F59E0B]" />
              <span>Сумка находок</span>
            </h2>
            <span className="text-xs font-bold text-[#F59E0B] tabular-nums">
              Предметов: {inventoryItems.length}
            </span>
          </div>

          {/* COMPACT ICON GRID OF ALL ITEMS IN THE BAG WITH ADJACENT POPOVER */}
          {inventoryItems.length === 0 ? (
            <div className="rounded-2xl p-6 bg-[#281F18] text-center text-xs text-[#D5C3AA]">
              В сумке пока пусто — отправь Шанталь в путешествие за новыми находками.
            </div>
          ) : (
            <div className="rounded-2xl p-3.5 bg-[#15100C] border border-[#382B20] space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] font-bold text-[#D5C3AA]">
                <span>
                  Улики: {royalCluesList.length}/7 · Трофеи: {denTrophiesList.length} · Травы: {herbsList.length} · Диковинки: {trinketsList.length}
                </span>
                <span className="text-[10px] text-[#A8947B]">
                  Нажми на предмет — справа откроется карточка
                </span>
              </div>
              <div className="grid grid-cols-4 sm:grid-cols-6 gap-2">
                {inventoryItems.map((item, idx) => {
                  const isSelected = selectedInventoryItemId === item.id;
                  const isNewlyAdded = newlyAddedBagItemIds.includes(item.id);
                  const emoji = getLootItemEmoji(item);
                  const isBundled = selectedBundleLootItemId === item.id;
                  const sellCoins = getSellPriceWithWitBonus(item.sellPriceCoins);
                  const itemProps = getLootItemProperties(item, sellCoins);
                  const borderTint =
                    item.category === 'ROYAL_CLUE'
                      ? '#F59E0B'
                      : item.category === 'DEN_TROPHY'
                      ? '#EC4899'
                      : item.category === 'HERB'
                      ? '#10B981'
                      : '#5C4834';
                  const slotDelayMs = Math.min(idx * 45, 360);
                  const iconDelayMs = Math.min(idx * 45 + 55, 415);

                  return (
                    <div
                      key={item.id}
                      className="relative"
                      data-popover-interactive="true"
                    >
                      <button
                        onClick={() => {
                          setSelectedInventoryItemId((prev) =>
                            prev === item.id ? null : item.id
                          );
                          if (isNewlyAdded) {
                            setNewlyAddedBagItemIds((prev) =>
                              prev.filter((id) => id !== item.id)
                            );
                          }
                          setActivePropertyPopup(null);
                        }}
                        title={item.titleRu}
                        className={`relative w-full min-h-[82px] rounded-2xl border p-2 flex flex-col items-center justify-center gap-1 cursor-pointer transition-colors ${
                          isNewlyAdded
                            ? 'animate-bag-new-item-drop'
                            : 'animate-bag-slot-drop'
                        }`}
                        style={{
                          animationDelay: `${slotDelayMs}ms`,
                          backgroundColor: isSelected
                            ? '#36291E'
                            : isNewlyAdded
                            ? '#2B2016'
                            : '#1E1712',
                          borderColor: isSelected
                            ? '#F59E0B'
                            : isNewlyAdded
                            ? '#FBBF24'
                            : borderTint,
                          boxShadow: isSelected
                            ? '0 0 0 2px rgba(245,158,11,0.45)'
                            : isNewlyAdded
                            ? '0 0 14px rgba(245,158,11,0.35)'
                            : 'none',
                        }}
                      >
                        {isNewlyAdded && (
                          <span className="absolute -top-1.5 -right-1 px-1.5 py-0.5 rounded-full bg-[#F59E0B] text-[#15100C] text-[8px] font-extrabold leading-none shadow">
                            ✦ Нов.
                          </span>
                        )}
                        <span
                          className="text-2xl leading-none select-none inline-block animate-bag-icon-drop"
                          style={{ animationDelay: `${iconDelayMs}ms` }}
                        >
                          {emoji}
                        </span>
                        <span className="text-[10px] font-bold text-[#FFF8EB] leading-tight w-full text-center break-words">
                          {getLootItemShortTitle(item)}
                        </span>
                      </button>

                      {/* ADJACENT POPOVER RIGHT NEXT TO TAPPED BAG ICON */}
                      {isSelected && (
                        <div
                          className={`absolute z-40 w-[235px] sm:w-[270px] rounded-2xl p-3 bg-[#261C15] border-2 border-[#F59E0B] shadow-2xl space-y-2 ${getAdjacentPopoverClasses(
                            idx % 4,
                            4
                          )}`}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div className="flex items-center gap-1.5 min-w-0">
                              <span className="text-xl shrink-0">{emoji}</span>
                              <div className="min-w-0">
                                <div className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                                  {item.titleRu}
                                </div>
                                <div className="text-[10px] font-bold text-[#F59E0B]">
                                  {item.foundAtRegionRu}
                                </div>
                              </div>
                            </div>
                            <button
                              onClick={() => setSelectedInventoryItemId(null)}
                              className="text-[11px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer"
                            >
                              ✕
                            </button>
                          </div>

                          <div className="flex flex-wrap gap-1">
                            {itemProps.map((p) => (
                              <span
                                key={p.id}
                                className="px-2 py-0.5 rounded-lg bg-[#15100C] border border-[#4D3D2C] text-[10px] font-extrabold"
                                style={{ color: p.accentColorHex }}
                              >
                                {p.statTagRu}
                              </span>
                            ))}
                          </div>

                          <p className="text-[11px] italic font-serif text-[#FAF0E1] leading-snug">
                            «{item.descriptionRu}»
                          </p>

                          {/* BOTTOM-RIGHT ACTION BUTTONS */}
                          <div className="flex flex-wrap items-center justify-end gap-1.5 pt-1 border-t border-[#382B20]">
                            {item.category === 'ROYAL_CLUE' ? (
                              <button
                                onClick={() => {
                                  handleInspectRoyalClue(item);
                                  setSelectedInventoryItemId(null);
                                }}
                                className="px-3 py-1.5 rounded-xl bg-[#F59E0B] text-[#15100C] text-[11px] font-extrabold cursor-pointer"
                              >
                                {item.inspectedLoreBonusClaimed
                                  ? '✓ Изучено'
                                  : 'Изучить (+1 Любопытство)'}
                              </button>
                            ) : item.category === 'DEN_TROPHY' ? (
                              <span className="text-[10px] font-extrabold text-[#10B981]">
                                ✓ В Домике (+1 к уюту)
                              </span>
                            ) : (
                              <>
                                <button
                                  onClick={() => {
                                    handleSellInventoryItem(item);
                                    setSelectedInventoryItemId(null);
                                  }}
                                  className="px-2.5 py-1.5 rounded-xl bg-[#382B20] text-[10px] font-extrabold text-[#F59E0B] cursor-pointer"
                                >
                                  Продать · +{sellCoins} 🪙
                                </button>
                                {item.category === 'HERB' ? (
                                  <button
                                    onClick={() => {
                                      handleUseHerbFromInventory(item);
                                      setSelectedInventoryItemId(null);
                                    }}
                                    className="px-2.5 py-1.5 rounded-xl bg-[#10B981] text-white text-[10px] font-extrabold cursor-pointer"
                                  >
                                    🌿 Заварить
                                  </button>
                                ) : (
                                  <button
                                    onClick={() => {
                                      handleGiftTrinketToNpc(
                                        item,
                                        KEY_COMPANION_NPCS.find(
                                          (n) => n.npcId === selectedGiftNpcId
                                        ) ?? KEY_COMPANION_NPCS[0]
                                      );
                                      setSelectedInventoryItemId(null);
                                    }}
                                    className="px-2.5 py-1.5 rounded-xl bg-[#EA580C] text-white text-[10px] font-extrabold cursor-pointer"
                                  >
                                    🎁 Подарить
                                  </button>
                                )}
                                <button
                                  onClick={() => {
                                    handleToggleBundleLootItem(item.id);
                                    setSelectedInventoryItemId(null);
                                  }}
                                  className="px-3 py-1.5 rounded-xl bg-[#1F382E] border border-[#10B981] text-[11px] font-extrabold text-[#6EE7B7] cursor-pointer"
                                >
                                  {isBundled ? 'Убрать' : '🧺 В узелок'}
                                </button>
                              </>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* COMPACT COLLAPSIBLE TABS FOR ALCHEMY & NPC FRIENDSHIP */}
          <div className="space-y-3">
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() =>
                  setActiveBagWorkshopTab((prev) =>
                    prev === 'alchemy' ? 'none' : 'alchemy'
                  )
                }
                className="min-h-[38px] px-3.5 rounded-xl border text-xs font-extrabold cursor-pointer flex items-center gap-2"
                style={{
                  backgroundColor:
                    activeBagWorkshopTab === 'alchemy' ? '#382B20' : '#15100C',
                  borderColor:
                    activeBagWorkshopTab === 'alchemy' ? '#10B981' : '#382B20',
                  color: '#10B981',
                }}
              >
                <span>⚗️ Алхимия {hasActiveRoadAmulet ? '(Оберег ✓)' : ''}</span>
                <span>{activeBagWorkshopTab === 'alchemy' ? '▲' : '▼'}</span>
              </button>

              <button
                onClick={() =>
                  setActiveBagWorkshopTab((prev) =>
                    prev === 'friendship' ? 'none' : 'friendship'
                  )
                }
                className="min-h-[38px] px-3.5 rounded-xl border text-xs font-extrabold cursor-pointer flex items-center gap-2"
                style={{
                  backgroundColor:
                    activeBagWorkshopTab === 'friendship' ? '#382B20' : '#15100C',
                  borderColor:
                    activeBagWorkshopTab === 'friendship' ? '#F59E0B' : '#382B20',
                  color: '#F59E0B',
                }}
              >
                <span>🎁 Дружба с жителями</span>
                <span>{activeBagWorkshopTab === 'friendship' ? '▲' : '▼'}</span>
              </button>
            </div>

            {activeBagWorkshopTab === 'alchemy' && (
              <div className="rounded-2xl p-3.5 bg-[#281F18] border border-[#10B981]/50 space-y-2.5">
                <p className="text-xs text-[#D5C3AA] leading-relaxed">
                  Соедини 1 Траву + 1 Находку, чтобы сварить{' '}
                  <span className="font-bold text-[#FFF8EB]">Оберег Попутного Ветра</span>: +100% защита и +
                  {ownedShopItemIds.includes('den_alchemy_table') ? '75%' : '50%'} монет в походе!
                </p>
                <button
                  onClick={handleCraftRoadAmuletInInventory}
                  disabled={
                    herbsList.length === 0 ||
                    (herbsList.length < 2 && trinketsList.length === 0)
                  }
                  className="w-full min-h-[38px] px-4 rounded-xl bg-[#10B981] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
                >
                  Смастерить Оберег (1 Трава + 1 Находка)
                </button>
              </div>
            )}

            {activeBagWorkshopTab === 'friendship' && (
              <div className="rounded-2xl p-3.5 bg-[#281F18] border border-[#F59E0B]/50 space-y-2.5">
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  {KEY_COMPANION_NPCS.map((npc) => {
                    const lvl = npcFriendshipLevels[npc.npcId] ?? 1;
                    const isSel = selectedGiftNpcId === npc.npcId;
                    return (
                      <button
                        key={npc.npcId}
                        onClick={() => setSelectedGiftNpcId(npc.npcId)}
                        className="p-2.5 rounded-xl border text-left cursor-pointer"
                        style={{
                          backgroundColor: isSel ? '#382B20' : '#1E1712',
                          borderColor: isSel ? '#F59E0B' : '#4D3D2C',
                        }}
                      >
                        <div className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                          {npc.nameRu}
                        </div>
                        <div className="text-[11px] font-bold text-[#F59E0B] mt-0.5">
                          Ур. {lvl} / 5 ♥
                        </div>
                      </button>
                    );
                  })}
                </div>
                <div className="text-[11px] text-[#D5C3AA]">
                  {
                    KEY_COMPANION_NPCS.find((n) => n.npcId === selectedGiftNpcId)
                      ?.perkDescriptionRu
                  }
                </div>
              </div>
            )}
          </div>
        </section>
      )}

      {/* SUBVIEW 4: ЛЕСНАЯ ЛАВКА (COMPACT ICONS + RIGHT-SIDE SLIDING POPOVER WITH BOTTOM-RIGHT КУПИТЬ BUTTON) */}
      {subView === 'shop' && (
        <section className="rounded-[26px] p-4 sm:p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-lg sm:text-xl font-extrabold text-[#FFF8EB]">
                Лесная лавка
              </h2>
              <p className="text-[11px] text-[#D5C3AA] mt-0.5">
                Нажми на предмет — справа откроется карточка с кнопкой «Купить»
              </p>
            </div>
            <div className="px-3 py-1.5 rounded-xl bg-[#2A2018] border border-[#F59E0B]/50 text-xs font-extrabold text-[#FDE68A] tabular-nums">
              🪙 В кошельке: {coins} Монет
            </div>
          </div>

          {/* ЛАВКА НА КОЛЁСАХ BANNER + COMPACT MERCHANT ICONS WITH ADJACENT POPOVER */}
          <div className="rounded-2xl bg-[#261C15] border border-[#F59E0B]/60 shadow-md">
            <div className="relative h-28 sm:h-32 w-full bg-[#15100C] rounded-t-2xl overflow-hidden">
              <img
                src={DEN_TIER_IMAGES.merchantChest}
                alt="Лавка на колёсах"
                referrerPolicy="no-referrer"
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#261C15] via-black/30 to-transparent" />
              <div className="absolute bottom-2.5 left-3.5 right-3.5 flex flex-wrap items-center justify-between gap-2">
                <span className="px-3 py-1 rounded-xl bg-[#F59E0B] text-[#15100C] text-xs font-extrabold">
                  Лавка на колёсах
                </span>
                {claimedMerchantOfferIds.length > 0 && (
                  <button
                    onClick={() => setClaimedMerchantOfferIds([])}
                    className="px-2.5 py-1 rounded-xl bg-black/70 border border-[#F59E0B] text-[11px] font-bold text-[#F59E0B] cursor-pointer"
                  >
                    Обновить
                  </button>
                )}
              </div>
            </div>

            <div className="p-3 bg-[#15100C]/60 rounded-b-2xl grid grid-cols-3 gap-2">
              {TRAVELING_MERCHANT_OFFERS.map((offer, idx) => {
                const isSelected = selectedShopEntryId === offer.id;
                const isClaimed = claimedMerchantOfferIds.includes(offer.id);
                const canAfford = coins >= offer.discountPriceCoins;
                const propsList = getMerchantOfferProperties(offer);
                return (
                  <div
                    key={offer.id}
                    className="relative"
                    data-popover-interactive="true"
                  >
                    <button
                      onClick={() => {
                        setSelectedShopEntryId((prev) =>
                          prev === offer.id ? null : offer.id
                        );
                        setActiveShopPropertyPopup(null);
                      }}
                      className="w-full min-h-[82px] p-2.5 rounded-2xl border flex flex-col items-center justify-center gap-1 cursor-pointer transition-all"
                      style={{
                        backgroundColor: isSelected ? '#36291E' : '#1E1712',
                        borderColor: isSelected ? '#F59E0B' : '#5C4834',
                        boxShadow: isSelected
                          ? '0 0 0 2px rgba(245,158,11,0.4)'
                          : 'none',
                      }}
                    >
                      <span className="text-2xl leading-none select-none">
                        {getMerchantOfferEmoji(offer)}
                      </span>
                      <span className="text-[11px] font-extrabold text-[#FFF8EB] leading-tight w-full text-center break-words">
                        {getMerchantOfferShortTitle(offer)}
                      </span>
                      <span className="text-[10px] font-bold text-[#F59E0B] tabular-nums">
                        {isClaimed ? 'Куплено ✓' : `${offer.discountPriceCoins} М`}
                      </span>
                    </button>

                    {/* ADJACENT POPOVER FOR MERCHANT OFFER */}
                    {isSelected && (
                      <div
                        className={`absolute z-40 w-[235px] sm:w-[265px] rounded-2xl p-3 bg-[#261C15] border-2 border-[#F59E0B] shadow-2xl space-y-2 ${getAdjacentPopoverClasses(
                          idx % 3,
                          3
                        )}`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex items-center gap-1.5 min-w-0">
                            <span className="text-xl shrink-0">
                              {getMerchantOfferEmoji(offer)}
                            </span>
                            <div className="min-w-0">
                              <div className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                                {offer.titleRu}
                              </div>
                              <div className="text-[10px] font-bold text-[#F59E0B]">
                                Скидка: было {offer.originalPriceCoins} М
                              </div>
                            </div>
                          </div>
                          <button
                            onClick={() => setSelectedShopEntryId(null)}
                            className="text-[11px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer"
                          >
                            ✕
                          </button>
                        </div>

                        <div className="flex flex-wrap gap-1">
                          {propsList.map((p) => (
                            <span
                              key={p.id}
                              className="px-2 py-0.5 rounded-lg bg-[#15100C] border border-[#4D3D2C] text-[10px] font-extrabold"
                              style={{ color: p.accentColorHex }}
                            >
                              {p.statTagRu}
                            </span>
                          ))}
                        </div>

                        <p className="text-[11px] italic font-serif text-[#FAF0E1] leading-snug">
                          «{offer.effectDescriptionRu}»
                        </p>

                        <div className="flex items-center justify-between pt-1 border-t border-[#382B20]">
                          <span className="text-[10px] font-bold text-[#D5C3AA]">
                            Кошелёк: {coins} М
                          </span>
                          <button
                            onClick={() => {
                              handleBuyTravelingMerchantOffer(offer);
                              setSelectedShopEntryId(null);
                            }}
                            disabled={isClaimed || !canAfford}
                            className="px-3.5 py-1.5 rounded-xl bg-[#EA580C] hover:bg-[#D9460F] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer shadow-md"
                          >
                            {isClaimed
                              ? 'Куплено ✓'
                              : `Купить · ${offer.discountPriceCoins} 🪙`}
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* 2 CLEAN DEPARTMENT TABS: 🏠 ИНТЕРЬЕР vs 🎒 СНАРЯЖЕНИЕ */}
          <div className="rounded-2xl p-3.5 sm:p-4 bg-[#15100C] border border-[#382B20] space-y-3.5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {/* TAB 1: 🏠 ИНТЕРЬЕР WITH WARM EMERALD-GOLD COUNTER BADGE (NOT BLACK) */}
              <button
                onClick={() => {
                  setShopSectionFilter('den_comfort');
                  setSelectedShopEntryId(null);
                }}
                className="min-h-[48px] px-3.5 py-2 rounded-2xl border text-left flex items-center justify-between gap-2 cursor-pointer transition-all"
                style={{
                  backgroundColor:
                    shopSectionFilter === 'den_comfort' ? '#2A2018' : '#1E1712',
                  borderColor:
                    shopSectionFilter === 'den_comfort' ? '#10B981' : '#382B20',
                }}
              >
                <div className="min-w-0">
                  <div
                    className="text-xs sm:text-sm font-extrabold leading-tight"
                    style={{
                      color:
                        shopSectionFilter === 'den_comfort'
                          ? '#FFF8EB'
                          : '#D5C3AA',
                    }}
                  >
                    🏠 Интерьер
                  </div>
                  <div className="w-20 h-1.5 rounded-full bg-[#19120D] overflow-hidden mt-1 border border-[#382B20]">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-[#10B981] to-[#F59E0B]"
                      style={{
                        width: `${Math.min(
                          100,
                          Math.max(8, denVisualInfo.itemsInCurrentStep * 10)
                        )}%`,
                      }}
                    />
                  </div>
                </div>
                <span
                  className="px-2.5 py-1 rounded-xl text-[11px] font-extrabold tabular-nums shrink-0 shadow-sm"
                  style={{
                    background:
                      'linear-gradient(135deg, #065F46 0%, #047857 100%)',
                    border: '1px solid #34D399',
                    color: '#ECFDF5',
                  }}
                >
                  {denVisualInfo.itemsInCurrentStep} из 10
                </span>
              </button>

              {/* TAB 2: 🎒 СНАРЯЖЕНИЕ */}
              <button
                onClick={() => {
                  setShopSectionFilter('expedition');
                  setSelectedShopEntryId(null);
                }}
                className="min-h-[48px] px-3.5 py-2 rounded-2xl border text-left flex items-center justify-between gap-2 cursor-pointer transition-all"
                style={{
                  backgroundColor:
                    shopSectionFilter === 'expedition' ? '#2A2018' : '#1E1712',
                  borderColor:
                    shopSectionFilter === 'expedition' ? '#F59E0B' : '#382B20',
                }}
              >
                <div className="min-w-0">
                  <div
                    className="text-xs sm:text-sm font-extrabold leading-tight"
                    style={{
                      color:
                        shopSectionFilter === 'expedition'
                          ? '#FFF8EB'
                          : '#D5C3AA',
                    }}
                  >
                    🎒 Снаряжение
                  </div>
                  <div className="text-[10px] font-bold text-[#A8947B] mt-0.5 leading-tight">
                    Одежда · Оружие · Узелок
                  </div>
                </div>
                <span
                  className="px-2.5 py-1 rounded-xl text-[11px] font-extrabold tabular-nums shrink-0 shadow-sm"
                  style={{
                    background:
                      'linear-gradient(135deg, #78350F 0%, #92400E 100%)',
                    border: '1px solid #FBBF24',
                    color: '#FEF3C7',
                  }}
                >
                  {
                    CHANTAL_SHOP_ITEMS.filter(
                      (i) => i.category !== 'DEN_COMFORT'
                    ).length
                  }{' '}
                  тов.
                </span>
              </button>
            </div>

            {/* Sub-tabs for the 3 steps of Интерьер (10 items each) */}
            {shopSectionFilter === 'den_comfort' && (
              <div className="grid grid-cols-3 gap-2">
                {([
                  { step: 1 as const, label: 'Ступень I (Ур. 1→2)' },
                  { step: 2 as const, label: 'Ступень II (Ур. 2→3)' },
                  { step: 3 as const, label: 'Ступень III (Ур. 3→4)' },
                ]).map((st) => {
                  const isStepActive = shopDenStepFilter === st.step;
                  const stepItems = CHANTAL_SHOP_ITEMS.filter(
                    (i) =>
                      i.category === 'DEN_COMFORT' &&
                      (i.denStep ?? 1) === st.step
                  );
                  const ownedInStep = stepItems.filter((i) =>
                    ownedShopItemIds.includes(i.id)
                  ).length;

                  return (
                    <button
                      key={st.step}
                      onClick={() => {
                        setShopDenStepFilter(st.step);
                        setSelectedShopEntryId(null);
                      }}
                      className="px-2 py-2 rounded-xl border text-[11px] font-extrabold flex flex-col items-center justify-center gap-0.5 cursor-pointer transition-all"
                      style={{
                        backgroundColor: isStepActive ? '#36291E' : '#1E1712',
                        borderColor: isStepActive ? '#F59E0B' : '#382B20',
                        color: isStepActive ? '#FFF8EB' : '#D5C3AA',
                      }}
                    >
                      <span className="leading-tight w-full text-center break-words">
                        {st.label}
                      </span>
                      <span className="text-[10px] text-[#34D399] tabular-nums">
                        {ownedInStep} из {stepItems.length}
                      </span>
                    </button>
                  );
                })}
              </div>
            )}

            {/* COMPACT ICON GRID OF FILTERED SHOP ITEMS WITH ADJACENT SLIDE-OUT POPOVER */}
            <div className="grid grid-cols-3 sm:grid-cols-5 gap-2.5">
              {CHANTAL_SHOP_ITEMS.filter((item) => {
                if (shopSectionFilter === 'den_comfort') {
                  return (
                    item.category === 'DEN_COMFORT' &&
                    (item.denStep ?? 1) === shopDenStepFilter
                  );
                }
                return item.category !== 'DEN_COMFORT';
              }).map((item, idx) => {
                const isSelected = selectedShopEntryId === item.id;
                const isOwned = ownedShopItemIds.includes(item.id);
                const isConsumable = item.category === 'CONSUMABLE';
                const currentTier = shopItemTiers[item.id] ?? 1;
                const maxTier = item.maxTier ?? 3;
                const upgradeCost = (item.upgradeCostCoins ?? 40) * currentTier;
                const canAffordBuy = coins >= item.priceCoins;
                const canAffordUpgrade = coins >= upgradeCost;
                const remDur =
                  item.category === 'APPAREL' && isOwned
                    ? gearDurabilityMap[item.id] ?? item.maxDurability ?? 5
                    : item.maxDurability;
                const pantryCount = isConsumable
                  ? pantryProvisionCounts[item.id] ?? 0
                  : 0;
                const itemProps = getShopItemProperties(
                  item,
                  currentTier,
                  remDur
                );
                const isEquippedInLoadout =
                  selectedSlotOutfitId === item.id ||
                  selectedSlotToolId === item.id ||
                  selectedSlotWeaponId === item.id;
                const borderTint =
                  item.category === 'DEN_COMFORT'
                    ? isOwned
                      ? '#10B981'
                      : '#4D3D2C'
                    : item.category === 'APPAREL'
                    ? '#F59E0B'
                    : '#5C4834';

                return (
                  <div
                    key={item.id}
                    className="relative"
                    data-popover-interactive="true"
                  >
                    <button
                      onClick={() => {
                        setSelectedShopEntryId((prev) =>
                          prev === item.id ? null : item.id
                        );
                        setActiveShopPropertyPopup(null);
                      }}
                      title={item.titleRu}
                      className="w-full min-h-[86px] rounded-2xl border p-2.5 flex flex-col items-center justify-center gap-1 cursor-pointer transition-all"
                      style={{
                        backgroundColor: isSelected ? '#36291E' : '#1E1712',
                        borderColor: isSelected ? '#F59E0B' : borderTint,
                        boxShadow: isSelected
                          ? '0 0 0 2px rgba(245,158,11,0.45)'
                          : 'none',
                      }}
                    >
                      <span className="text-2xl sm:text-3xl leading-none select-none">
                        {getShopItemEmoji(item)}
                      </span>
                      <span className="text-[11px] font-bold text-[#FFF8EB] leading-tight w-full text-center break-words">
                        {getShopItemShortTitle(item)}
                      </span>
                      <span
                        className="text-[10px] font-extrabold tabular-nums"
                        style={{
                          color:
                            !isConsumable && isOwned
                              ? '#10B981'
                              : pantryCount > 0
                              ? '#10B981'
                              : '#F59E0B',
                        }}
                      >
                        {!isConsumable && isOwned
                          ? item.category === 'APPAREL' && remDur !== undefined
                            ? `${remDur}/${item.maxDurability ?? 5} пох.`
                            : `✓ Т${currentTier}/3`
                          : pantryCount > 0
                          ? `Запас: ${pantryCount} (${item.priceCoins} М)`
                          : `${item.priceCoins} М`}
                      </span>
                    </button>

                    {/* RIGHT-SIDE SLIDING POPOVER WITH STATS & BOTTOM-RIGHT КУПИТЬ BUTTON */}
                    {isSelected && (
                      <div
                        className={`absolute z-40 w-[235px] sm:w-[270px] rounded-2xl p-3 bg-[#261C15] border-2 border-[#F59E0B] shadow-2xl space-y-2 ${getAdjacentPopoverClasses(
                          idx % 3,
                          3
                        )}`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex items-center gap-1.5 min-w-0">
                            <span className="text-xl shrink-0">
                              {getShopItemEmoji(item)}
                            </span>
                            <div className="min-w-0">
                              <div className="text-xs font-extrabold text-[#FFF8EB] leading-tight">
                                {item.titleRu}
                              </div>
                              <div className="text-[10px] font-bold text-[#F59E0B]">
                                {item.visualSlotLabelRu}
                              </div>
                            </div>
                          </div>
                          <button
                            onClick={() => setSelectedShopEntryId(null)}
                            className="text-[11px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer"
                          >
                            ✕
                          </button>
                        </div>

                        {/* Key stats pills */}
                        <div className="flex flex-wrap gap-1">
                          {itemProps.map((p) => (
                            <span
                              key={p.id}
                              className="px-2 py-0.5 rounded-lg bg-[#15100C] border border-[#4D3D2C] text-[10px] font-extrabold"
                              style={{ color: p.accentColorHex }}
                            >
                              {p.statTagRu}
                            </span>
                          ))}
                        </div>

                        <p className="text-[11px] italic font-serif text-[#FAF0E1] leading-snug">
                          «{item.descriptionRu}»
                        </p>

                        {/* BOTTOM-RIGHT ACTION BAR (THUMB-FRIENDLY КУПИТЬ BUTTON) */}
                        <div className="flex flex-wrap items-center justify-between gap-1.5 pt-1.5 border-t border-[#382B20]">
                          {isConsumable ? (
                            <>
                              <span className="text-[10px] font-bold text-[#D5C3AA] tabular-nums">
                                {pantryCount > 0
                                  ? `В запасе: ${pantryCount} шт.`
                                  : `Кошелёк: ${coins} М`}
                              </span>
                              <button
                                onClick={() => {
                                  handleBuyShopItem(item, true);
                                  setSelectedShopEntryId(null);
                                }}
                                disabled={!canAffordBuy}
                                className="px-3.5 py-1.5 rounded-xl bg-[#EA580C] hover:bg-[#D9460F] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer shadow-md"
                              >
                                Купить · {item.priceCoins} 🪙
                              </button>
                            </>
                          ) : !isOwned ? (
                            <>
                              <span className="text-[10px] font-bold text-[#D5C3AA] tabular-nums">
                                Кошелёк: {coins} М
                              </span>
                              <button
                                onClick={() => {
                                  handleBuyShopItem(item);
                                  setSelectedShopEntryId(null);
                                }}
                                disabled={!canAffordBuy}
                                className="px-3.5 py-1.5 rounded-xl bg-[#EA580C] hover:bg-[#D9460F] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer shadow-md"
                              >
                                Купить · {item.priceCoins} 🪙
                              </button>
                            </>
                          ) : (
                            <div className="w-full flex flex-wrap items-center justify-end gap-1.5">
                              {item.category === 'APPAREL' &&
                                (remDur ?? 5) < (item.maxDurability ?? 5) && (
                                  <button
                                    onClick={() => {
                                      handleRefillGearDurability(item);
                                      setSelectedShopEntryId(null);
                                    }}
                                    disabled={
                                      coins < (item.refillCostCoins ?? 12)
                                    }
                                    className="px-2.5 py-1.5 rounded-xl bg-[#281F18] border border-[#F59E0B] disabled:opacity-40 text-[#F59E0B] text-[10px] font-extrabold cursor-pointer"
                                  >
                                    Починить · {item.refillCostCoins ?? 12} 🪙
                                  </button>
                                )}

                              {currentTier < maxTier && (
                                <button
                                  onClick={() => {
                                    handleUpgradeShopItemTier(item);
                                    setSelectedShopEntryId(null);
                                  }}
                                  disabled={!canAffordUpgrade}
                                  className="px-2.5 py-1.5 rounded-xl bg-[#382B20] border border-[#F59E0B] disabled:opacity-40 text-[#F59E0B] text-[10px] font-extrabold cursor-pointer"
                                >
                                  Тир {currentTier + 1} · {upgradeCost} 🪙
                                </button>
                              )}

                              {item.category === 'APPAREL' ? (
                                <button
                                  onClick={() => {
                                    handleToggleBackpackGear(item.id);
                                    setSelectedShopEntryId(null);
                                  }}
                                  disabled={(remDur ?? 0) <= 0}
                                  className="px-3 py-1.5 rounded-xl bg-[#10B981] hover:bg-[#059669] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer shadow-md"
                                >
                                  {isEquippedInLoadout ? 'Убрать' : 'Взять'}
                                </button>
                              ) : (
                                <span className="px-2.5 py-1 rounded-xl bg-[#065F46] text-[#ECFDF5] text-[10px] font-extrabold">
                                  ✓ В Домике (Т{currentTier}/3)
                                </span>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </section>
      )}

      {/* SUBVIEW 5: ЛЕТОПИСЬ (COLLAPSIBLE CHAPTER CARDS + COLLAPSIBLE AI MEMORY LAYERS) */}
      {subView === 'journal' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-xl font-extrabold text-[#FFF8EB]">
                Летопись странствий
              </h2>
              <p className="text-xs text-[#D5C3AA] mt-0.5">
                Нажми на любую главу, чтобы развернуть историю, свой ответ и живую реакцию Шанталь
              </p>
            </div>
            <button
              onClick={() => setShowChronicleMemoryLayers((prev) => !prev)}
              className="px-3.5 py-1.5 rounded-xl bg-[#281F18] border border-[#F59E0B]/50 text-xs font-extrabold text-[#F59E0B] cursor-pointer"
            >
              {showChronicleMemoryLayers
                ? 'Скрыть память ИИ (Слой 1–2) ▲'
                : `Память ИИ (${threeTierMemory.fullChroniclePassports.length} пох.) ▼`}
            </button>
          </div>

          {showChronicleMemoryLayers && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
              <div className="rounded-2xl p-4 bg-[#281F18] border border-[#F59E0B]/40 space-y-2">
                <div className="text-xs font-extrabold text-[#F59E0B]">
                  Слой 1 · Сжатая летопись ВСЕХ решений с Похода #1:
                </div>
                <div className="space-y-1.5 max-h-44 overflow-y-auto pr-1 text-[11px] text-[#E7D8C3]">
                  {threeTierMemory.fullChroniclePassports.map((line, i) => (
                    <div key={i} className="p-2 rounded-lg bg-[#15100C] border border-[#382B20]">
                      {line}
                    </div>
                  ))}
                </div>
              </div>

              <div className="rounded-2xl p-4 bg-[#281F18] border border-[#10B981]/40 space-y-2">
                <div className="text-xs font-extrabold text-[#10B981]">
                  Слой 2 · Живая картотека знакомств и обещаний с NPC:
                </div>
                <div className="space-y-1.5 max-h-44 overflow-y-auto pr-1 text-[11px] text-[#E7D8C3]">
                  {threeTierMemory.npcRelationshipDossier.map((line, i) => (
                    <div key={i} className="p-2 rounded-lg bg-[#15100C] border border-[#382B20]">
                      {line}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Collapsible Chapter List */}
          <div className="space-y-2.5">
            {journalHistory.map((entry) => {
              const isExpanded = expandedJournalEpisodeId === entry.id;
              const chosenOption =
                entry.selectedChoiceIndex !== undefined
                  ? entry.choices[entry.selectedChoiceIndex]
                  : undefined;

              return (
                <div
                  key={entry.id}
                  className="rounded-2xl bg-[#281F18] border overflow-hidden transition-all"
                  style={{
                    borderColor: isExpanded ? '#F59E0B' : '#4D3D2C',
                  }}
                >
                  <button
                    onClick={() =>
                      setExpandedJournalEpisodeId(isExpanded ? null : entry.id)
                    }
                    className="w-full p-3.5 text-left flex items-center justify-between gap-3 cursor-pointer hover:bg-[#33271D]/60"
                  >
                    <div className="min-w-0">
                      <div className="text-[11px] font-bold text-[#F59E0B]">
                        {entry.isFillerEpisode
                          ? 'Побочный эпизод (Филлер)'
                          : `Поход #${entry.journeyNumber} · ${entry.regionTitleRu} · ${entry.encounteredNpcRu}`}
                      </div>
                      <div className="text-sm sm:text-base font-extrabold text-[#FFF8EB] leading-snug mt-0.5">
                        {entry.titleRu}
                      </div>
                    </div>
                    <span className="text-xs font-extrabold text-[#F59E0B] shrink-0">
                      {isExpanded ? 'Свернуть ▲' : 'Читать ▼'}
                    </span>
                  </button>

                  {isExpanded && (
                    <div className="px-4 pb-4 pt-1 border-t border-[#382B20] space-y-3">
                      <p className="text-xs sm:text-sm text-[#E7D8C3] leading-relaxed">
                        {entry.storyTextRu}
                      </p>
                      {entry.pastEchoNoteRu && (
                        <div className="text-xs font-semibold text-[#F59E0B]">
                          ↻ {entry.pastEchoNoteRu}
                        </div>
                      )}
                      {chosenOption && (
                        <div className="rounded-xl p-3 bg-[#1E1712] border border-[#10B981]/40 space-y-1.5">
                          <div className="text-xs font-bold text-[#10B981]">
                            Твои слова: «{chosenOption.labelRu}»
                          </div>
                          <p className="text-xs italic font-serif text-[#FAF0E1] leading-relaxed">
                            Реакция Шанталь: «{chosenOption.followUpReactionRu}»
                          </p>
                        </div>
                      )}
                      {entry.userCustomResponseText && (
                        <div className="text-xs font-semibold text-[#10B981]">
                          Твой ответ / поручение: «{entry.userCustomResponseText}»
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* SUBVIEW 6: AI STORYTELLER SETTINGS (Direct Gemini #1 + OpenRouter Model Selector + Direct Grok) */}
      {subView === 'ai_settings' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-5">
          <div>
            <h2 className="text-xl font-extrabold text-[#FFF8EB]">
              Настройки ИИ-рассказчика (Gemini / OpenRouter / Grok), сложности и лора
            </h2>
            <p className="text-xs text-[#D5C3AA] mt-0.5">
              По умолчанию активен прямой ключ Gemini (с бесплатным дневным лимитом). В любой момент можно переключиться на OpenRouter с выбором любой модели или прямой Grok!
            </p>
          </div>

          {/* 1. 3-Way AI Provider Gateway */}
          <div className="rounded-2xl p-4 bg-[#281F18] space-y-3.5">
            <div className="text-sm font-extrabold text-[#FFF8EB]">
              1. Выбери шлюз нейросети для генерации историй:
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2.5">
              <button
                onClick={() => setAiProvider('gemini')}
                className="min-h-[56px] p-3.5 rounded-xl border text-left cursor-pointer"
                style={{
                  backgroundColor: aiProvider === 'gemini' ? '#382B20' : '#15100C',
                  borderColor: aiProvider === 'gemini' ? '#10B981' : '#4D3D2C',
                }}
              >
                <div className="text-xs font-extrabold text-[#10B981]">
                  Основной (По умолчанию)
                </div>
                <div className="text-sm font-extrabold text-[#FFF8EB] mt-0.5">
                  Прямой Gemini Flash {aiProvider === 'gemini' ? '✓' : ''}
                </div>
                <div className="text-[11px] text-[#D5C3AA] mt-0.5">
                  Бесплатный лимит до 1500 запр./день · Строгая JSON-схема
                </div>
              </button>

              <button
                onClick={() => setAiProvider('openrouter')}
                className="min-h-[56px] p-3.5 rounded-xl border text-left cursor-pointer"
                style={{
                  backgroundColor: aiProvider === 'openrouter' ? '#382B20' : '#15100C',
                  borderColor: aiProvider === 'openrouter' ? '#F59E0B' : '#4D3D2C',
                }}
              >
                <div className="text-xs font-extrabold text-[#F59E0B]">
                  Мульти-модельный шлюз
                </div>
                <div className="text-sm font-extrabold text-[#FFF8EB] mt-0.5">
                  OpenRouter API {aiProvider === 'openrouter' ? '✓' : ''}
                </div>
                <div className="text-[11px] text-[#D5C3AA] mt-0.5">
                  Выбор любой модели: Claude 3.7, Grok 3, DeepSeek V3, Llama
                </div>
              </button>

              <button
                onClick={() => setAiProvider('grok')}
                className="min-h-[56px] p-3.5 rounded-xl border text-left cursor-pointer"
                style={{
                  backgroundColor: aiProvider === 'grok' ? '#382B20' : '#15100C',
                  borderColor: aiProvider === 'grok' ? '#F59E0B' : '#4D3D2C',
                }}
              >
                <div className="text-xs font-extrabold text-[#F59E0B]">
                  Прямой xAI
                </div>
                <div className="text-sm font-extrabold text-[#FFF8EB] mt-0.5">
                  Grok (xAI API) {aiProvider === 'grok' ? '✓' : ''}
                </div>
                <div className="text-[11px] text-[#D5C3AA] mt-0.5">
                  Прямое подключение к серверам xAI (grok-3-mini)
                </div>
              </button>
            </div>

            {/* OpenRouter Model Picker */}
            {aiProvider === 'openrouter' && (
              <div className="rounded-xl p-3.5 bg-[#15100C] border border-[#F59E0B]/50 space-y-3 mt-2">
                <div className="text-xs font-extrabold text-[#F59E0B]">
                  Выбор модели внутри OpenRouter:
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                  {OPENROUTER_MODELS_CATALOG.map((m) => {
                    const isSelected =
                      !customOpenRouterModelInput.trim() && openRouterModel === m.id;
                    return (
                      <button
                        key={m.id}
                        onClick={() => {
                          setOpenRouterModel(m.id);
                          setCustomOpenRouterModelInput('');
                        }}
                        className="p-2.5 rounded-xl border text-left cursor-pointer"
                        style={{
                          backgroundColor: isSelected ? '#281F18' : '#1E1712',
                          borderColor: isSelected ? '#F59E0B' : '#382B20',
                        }}
                      >
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-xs font-extrabold text-[#FFF8EB]">
                            {m.titleRu}
                          </span>
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-[#382B20] text-[#F59E0B]">
                            {m.badgeRu}
                          </span>
                        </div>
                        <div className="text-[11px] text-[#D5C3AA] mt-1">
                          {m.subtitleRu}
                        </div>
                      </button>
                    );
                  })}
                </div>

                <div className="space-y-1">
                  <div className="text-[11px] font-bold text-[#D5C3AA]">
                    Или введи любой собственный ID модели с сайта OpenRouter:
                  </div>
                  <input
                    type="text"
                    value={customOpenRouterModelInput}
                    onChange={(e) => setCustomOpenRouterModelInput(e.target.value)}
                    placeholder="Например: anthropic/claude-3.7-sonnet или deepseek/deepseek-r1"
                    className="w-full min-h-[40px] px-3 rounded-xl bg-[#1E1712] border border-[#4D3D2C] text-xs text-[#FFF8EB] placeholder-[#8C7A65]"
                  />
                </div>
              </div>
            )}
          </div>

          {/* 2. Story Difficulty Level (4 levels) */}
          <div className="rounded-2xl p-4 bg-[#281F18] space-y-3">
            <div className="text-sm font-extrabold text-[#FFF8EB]">
              2. Сложность и интенсивность сюжета:
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {(
                [
                  { id: 'EASY', label: 'Лёгкий' },
                  { id: 'MEDIUM', label: 'Средний' },
                  { id: 'HARD', label: 'Тяжёлый' },
                  { id: 'SUPER_HARD', label: 'Супер тяжёлый' },
                ] as Array<{ id: StoryDifficultyLevel; label: string }>
              ).map((lvl) => (
                <button
                  key={lvl.id}
                  onClick={() => setStoryDifficulty(lvl.id)}
                  className="min-h-[44px] px-3 rounded-xl text-xs font-extrabold cursor-pointer border"
                  style={{
                    backgroundColor:
                      storyDifficulty === lvl.id ? '#EA580C' : '#15100C',
                    color: '#FFF8EB',
                    borderColor:
                      storyDifficulty === lvl.id ? '#FFF8EB' : '#4D3D2C',
                  }}
                >
                  {lvl.label}
                </button>
              ))}
            </div>
          </div>

          {/* 3. Custom Lore File Upload (.txt / .md / .json) */}
          <div className="rounded-2xl p-4 bg-[#281F18] space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <div className="text-sm font-extrabold text-[#FFF8EB]">
                  3. Файл персонализации для нейросети (.txt, .md, .json)
                </div>
                <p className="text-xs text-[#D5C3AA] mt-0.5">
                  Прикрепи свой файл с идеями, фактами, персонажами или правилами — ИИ будет сверяться с ним перед каждой историей
                </p>
              </div>

              <input
                ref={fileInputRef}
                type="file"
                accept=".txt,.md,.markdown,.json"
                onChange={handleLoreFileUpload}
                className="hidden"
              />
              <button
                onClick={() => fileInputRef.current?.click()}
                className="min-h-[42px] px-4 rounded-xl bg-[#F59E0B] text-[#15100C] text-xs font-extrabold flex items-center gap-1.5 cursor-pointer"
              >
                <Upload className="w-4 h-4" />
                <span>{customLoreFileName ? 'Заменить файл' : 'Загрузить файл'}</span>
              </button>
            </div>

            {customLoreFileName && (
              <div className="text-xs font-bold text-[#10B981]">
                Активный файл: {customLoreFileName} ({customLoreContent.length} симв.)
              </div>
            )}

            <textarea
              rows={4}
              value={customLoreContent}
              onChange={(e) => setCustomLoreContent(e.target.value)}
              placeholder="Или вставь/отредактируй дополнительные указания для нейросети прямо здесь..."
              className="w-full p-3 rounded-xl bg-[#15100C] border border-[#4D3D2C] text-xs text-[#FFF8EB] placeholder-[#8C7A65]"
            />
          </div>

          {/* 4. Toggle Trait Change Arrows */}
          <div className="rounded-2xl p-4 bg-[#281F18] space-y-3">
            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="text-sm font-extrabold text-[#FFF8EB]">
                  Короткая плашка со стрелками изменения характера (↑ / ↓)
                </div>
                <div className="text-xs text-[#D5C3AA]">
                  Показывать изменения 6 параметров после нажатия сюжетных кнопок
                </div>
              </div>
              <button
                onClick={() => setShowTraitArrowsSetting((s) => !s)}
                className="min-h-[40px] px-4 rounded-xl text-xs font-extrabold cursor-pointer"
                style={{
                  backgroundColor: showTraitArrowsSetting ? '#10B981' : '#15100C',
                  color: '#FFF8EB',
                }}
              >
                {showTraitArrowsSetting ? 'Включено' : 'Скрыто'}
              </button>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
