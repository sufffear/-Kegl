import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
  Backpack,
  BookOpen,
  CheckCircle2,
  Coins,
  Compass,
  FileText,
  Flame,
  Gift,
  Heart,
  Home,
  MapPin,
  Send,
  ShoppingBag,
  Sparkles,
  Timer,
  Upload,
  Zap,
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
  getTraitActivePerkDescriptionRu,
  INITIAL_SEEDED_JOURNAL_HISTORY,
  InventoryLootItem,
  KEY_COMPANION_NPCS,
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
  todayPlannedDoneCount = 0,
  todayClosedPeriodsCount = 0,
  lastCompletedWorkoutTitle,
  onGoToWorkout,
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
  const [forceTransformPreview, setForceTransformPreview] = useState<boolean>(false);

  // Visual Frame Switcher on Den Card: Portrait vs Den Interior Panorama + Tier Preview
  const [visualDisplayMode, setVisualDisplayMode] = useState<'portrait' | 'den_interior'>('portrait');
  const [previewDenTierOverride, setPreviewDenTierOverride] = useState<1 | 2 | 3 | null>(null);

  // 2. 6 Character Traits
  const [traits, setTraits] = useState<ChantalTraits>(() => {
    try {
      const raw = localStorage.getItem('chantal_traits');
      if (raw) return JSON.parse(raw);
    } catch {
      // ignore
    }
    return {
      curiosity: 14,
      tenderness: 16,
      playfulness: 12,
      romanticism: 10,
      courage: 11,
      wit: 13,
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
  const [claimedWorkoutPeriodsToday, setClaimedWorkoutPeriodsToday] = useState<number>(0);

  // 5. Owned Shop Items, Item Upgrade Tiers (Tier 1..3), Merchant Claims & NPC Friendship Levels
  const [ownedShopItemIds, setOwnedShopItemIds] = useState<string[]>(['den_heather_blanket']);
  const [shopItemTiers, setShopItemTiers] = useState<Record<string, number>>({
    den_heather_blanket: 1,
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
        sellPriceCoins: 20,
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
  const [selectedInventoryItemId, setSelectedInventoryItemId] = useState<string>('init_clue_1');

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

  // Persist core state
  useEffect(() => {
    localStorage.setItem('chantal_prologue_done', String(hasCompletedPrologue));
  }, [hasCompletedPrologue]);

  useEffect(() => {
    localStorage.setItem('chantal_total_journeys', String(totalJourneys));
  }, [totalJourneys]);

  useEffect(() => {
    localStorage.setItem('chantal_traits', JSON.stringify(traits));
  }, [traits]);

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

  // Stage Info (Spoiler-free until 100 journeys or preview toggle)
  const stageInfo = useMemo(
    () => getChantalStageInfo(totalJourneys, forceTransformPreview),
    [totalJourneys, forceTransformPreview]
  );

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

  // Owned Apparel & Den Comforts
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

  // Den Visual Tier Info (Tier I -> II -> III with bespoke interior artwork!)
  const denVisualInfo = useMemo(
    () => getDenVisualTierInfo(ownedDenItems.length, previewDenTierOverride),
    [ownedDenItems.length, previewDenTierOverride]
  );

  // Passive Coin Bonus from Owned & Upgraded Shop Items
  const passiveShopCoinsBonus = useMemo(() => {
    return CHANTAL_SHOP_ITEMS.filter(
      (item) => ownedShopItemIds.includes(item.id) && item.bonusCoinsPerJourney
    ).reduce((acc, item) => {
      const tier = shopItemTiers[item.id] ?? 1;
      return acc + (item.bonusCoinsPerJourney ?? 0) + (tier - 1) * 4;
    }, 0);
  }, [ownedShopItemIds, shopItemTiers]);

  // Calculate required Energy for the next journey (30 normal, or 45 when exhausted minus home comfort reduction)
  const requiredJourneyEnergy = useMemo(() => {
    if (!isExhausted45) return 30;
    const comfortDiscount = CHANTAL_SHOP_ITEMS.filter(
      (item) => ownedShopItemIds.includes(item.id) && item.reducesExhaustionCostBy
    ).reduce((acc, item) => acc + (item.reducesExhaustionCostBy ?? 0), 0);
    return Math.max(30, 45 - comfortDiscount);
  }, [isExhausted45, ownedShopItemIds]);

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

  // Next Milestone Chest info (every 5 journeys: #5, #10, #15...)
  const nextMilestoneChestJourney = Math.ceil((totalJourneys + 1) / 5) * 5;
  const isNextJourneyMilestoneChest = (totalJourneys + 1) % 5 === 0;

  // Show temporary arrow badge & record visual trait change history when traits change
  const triggerTraitToast = (
    deltas: Partial<ChantalTraits>,
    choiceLabelRu: string = 'Событие в Домике',
    storyTitleRu: string = 'Жизнь Шанталь',
    prevSnapshot?: ChantalTraits
  ) => {
    const basePrev = prevSnapshot ?? traits;
    const computedNext: ChantalTraits = {
      curiosity: Math.max(1, basePrev.curiosity + (deltas.curiosity || 0)),
      tenderness: Math.max(1, basePrev.tenderness + (deltas.tenderness || 0)),
      playfulness: Math.max(1, basePrev.playfulness + (deltas.playfulness || 0)),
      romanticism: Math.max(1, basePrev.romanticism + (deltas.romanticism || 0)),
      courage: Math.max(1, basePrev.courage + (deltas.courage || 0)),
      wit: Math.max(1, basePrev.wit + (deltas.wit || 0)),
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
      curiosity: Math.max(1, prevSnapshot.curiosity + (deltas.curiosity || 0)),
      tenderness: Math.max(1, prevSnapshot.tenderness + (deltas.tenderness || 0)),
      playfulness: Math.max(1, prevSnapshot.playfulness + (deltas.playfulness || 0)),
      romanticism: Math.max(1, prevSnapshot.romanticism + (deltas.romanticism || 0)),
      courage: Math.max(1, prevSnapshot.courage + (deltas.courage || 0)),
      wit: Math.max(1, prevSnapshot.wit + (deltas.wit || 0)),
    };
    setTraits(nextSnapshot);
    triggerTraitToast(deltas, choiceLabelRu, storyTitleRu, prevSnapshot);
  };

  // Interactive Portrait Tap ("Погладить за ушком / Поговорить у камина")
  const handlePetOrTalkWithChantal = () => {
    if (soundEnabled) onPlayTone('relax');
    const nextIdx = petVariantCounter + 1;
    setPetVariantCounter(nextIdx);
    const dialogue = getInteractivePettingDialogue(traits, stageInfo.isTransformed, nextIdx);
    setPetSpeechBubble({
      textRu: dialogue.reactionTextRu,
      badgeRu: dialogue.traitBadgeRu,
    });
    setMoodPct((prev) => Math.min(100, prev + traitPerks.tendernessMoodHealBonusPct));
    setIsHeartPulseActive(true);
    window.setTimeout(() => setIsHeartPulseActive(false), 1200);
  };

  // Live Reaction to Real Kegel Workout Blocks
  const handleClaimWorkoutBlockPraise = () => {
    if (soundEnabled) onPlayTone('salute');
    setClaimedWorkoutPeriodsToday((prev) => prev + 1);
    setCoins((prev) => prev + 15);
    setEnergy((prev) => prev + 5);
    setMoodPct(100);
    applyTraitDeltasFromChoice(
      { courage: 1 },
      'Похвала за закрытый блок тренировок Кегеля',
      'Дисциплина и тренировка'
    );
    setLastReactionBanner(
      'Шанталь радостно встречает тебя после тренировки Кегеля! Получен бонус дисциплины: +15 Монет, +5 Энергии и Отвага ↑ (+1).'
    );
  };

  // Simulate bringing a new item from a journey into the inventory state array
  const handleAddSampleJourneyLoot = () => {
    if (soundEnabled) onPlayTone('complete');
    const nowIso = new Date().toISOString().slice(0, 10);
    const samples: Array<Omit<InventoryLootItem, 'id' | 'foundDateIso'>> = [
      {
        titleRu: 'Янтарный компас старого звездочёта',
        category: 'TRINKET',
        descriptionRu:
          'Маленький латунный компас со стрелкой из прозрачного янтаря, найденный Шанталь среди мшистых валунов. Стрелка всегда дрожит в сторону северных башен.',
        sellPriceCoins: 26,
        foundAtRegionRu: 'Янтарная дубрава',
      },
      {
        titleRu: 'Засушенный цветок Лунного Вереска',
        category: 'HERB',
        descriptionRu:
          'Редкое серебристое соцветие, собранное на рассвете у старой запруды. Если заварить его с мёдом дома у камина, мгновенно снимает дорожную усталость.',
        sellPriceCoins: 22,
        foundAtRegionRu: 'Туманная лощина',
        canHealExhaustion: true,
      },
      {
        titleRu: 'Сургучная печать с королевской лилией',
        category: 'ROYAL_CLUE',
        descriptionRu:
          'Обломок тёмно-бордового сургуча с чётким оттиском короны и лилии. Шанталь долго принюхивалась к нему — запах старого воска показался ей удивительно родным.',
        sellPriceCoins: 45,
        foundAtRegionRu: 'Старая водяная мельница',
        inspectedLoreBonusClaimed: false,
      },
    ];
    const picked = samples[inventoryItems.length % samples.length];
    const newItem: InventoryLootItem = {
      ...picked,
      id: `journey_loot_${Date.now()}`,
      foundDateIso: nowIso,
    };
    setInventoryItems((prev) => [newItem, ...prev]);
    setSelectedInventoryItemId(newItem.id);
    setLastReactionBanner(
      `Шанталь принесла из путешествия новую находку: «${newItem.titleRu}» (${newItem.foundAtRegionRu})! Описание открыто в карточке осмотра.`
    );
  };

  // Start autonomous journey (1 hour timer) — Chantal can revisit the same location multiple times if logical!
  const handleStartAutonomousJourney = (isFiller: boolean = false, fillerText?: string) => {
    if (energy < requiredJourneyEnergy) return;
    if (soundEnabled) onPlayTone('hold');

    setEnergy((prev) => Math.max(0, prev - requiredJourneyEnergy));
    setIsExhausted45(false);
    setLastReactionBanner(null);

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
          denItemsRu: ownedDenItems.map((i) => i.titleRu),
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

      const baseCoins = (Number(raw.coinsEarned) || 28) * (isMilestone ? 2 : 1);
      const romanticEveningMult =
        journeyToResolve.dailyArcStep === 3
          ? 1 + traitPerks.romanticismEveningCoinBonusPct / 100
          : 1;
      const finalCoins = Math.round(
        (baseCoins + traitPerks.curiosityBonusCoins + passiveShopCoinsBonus) *
          streakMultiplierInfo.multiplier *
          romanticEveningMult *
          amuletMult
      );

      const lootCat = isMilestone
        ? 'ROYAL_CLUE'
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
          ? `★ Юбилейный Сундук Главы #${nextJourneyNum} · ${ raw.titleRu || regionObj.titleRu }`
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
            : raw.lootItem?.titleRu || 'Диковинка с лесной тропы',
          category: lootCat,
          descriptionRu:
            raw.lootItem?.descriptionRu ||
            'Необычная вещь, которую Шанталь бережно принесла в сумке из своего приключения.',
          sellPriceCoins: Number(raw.lootItem?.sellPriceCoins) || 25,
          foundAtRegionRu: regionObj.titleRu,
          foundDateIso: nowIso,
          canHealExhaustion: lootCat === 'HERB',
        },
        outcomeType: resolvedOutcome,
        coinsEarned: finalCoins,
        badNewsTitleRu: resolvedBadNewsRu,
        debtAmountCoins: resolvedDebtCoins,
        choices: baseChoices,
        aiSourceLabel: data.source || 'Gemini Flash',
      };

      setHasActiveRoadAmulet(false);
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
      });
      if (hasActiveRoadAmulet) {
        fallback.outcomeType = 'GOOD_REWARD';
        fallback.badNewsTitleRu = '';
        fallback.debtAmountCoins = 0;
      }
      const romanticEveningMult =
        journeyToResolve.dailyArcStep === 3
          ? 1 + traitPerks.romanticismEveningCoinBonusPct / 100
          : 1;
      fallback.coinsEarned = Math.round(
        (fallback.coinsEarned + traitPerks.curiosityBonusCoins + passiveShopCoinsBonus) *
          streakMultiplierInfo.multiplier *
          romanticEveningMult *
          amuletMult
      );
      setHasActiveRoadAmulet(false);
      setActiveStoryEpisode(fallback);
      setActiveJourney(null);
      if (soundEnabled) onPlayTone('complete');
    } finally {
      setIsGeneratingStory(false);
    }
  };

  // Resolve Canonical Choice Button (Advances main story + modifies 6 traits + adds loot & coins/mishaps)
  const handleSelectCanonicalChoice = (choiceIdx: number) => {
    if (!activeStoryEpisode) return;
    const chosen = activeStoryEpisode.choices[choiceIdx];
    if (!chosen) return;

    if (soundEnabled) onPlayTone('salute');

    setInventoryItems((prev) => [activeStoryEpisode.lootItem, ...prev]);
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
    setLastReactionBanner(chosen.followUpReactionRu);
    setActiveStoryEpisode(null);
    setMoodPct((m) => Math.min(100, m + 6));
  };

  // Resolve Free Text Input ("Свой текст" -> Filler response / side action, DOES NOT change traits or main plot!)
  const handleSendCustomFillerResponse = () => {
    const trimmed = customFillerInput.trim();
    if (!trimmed || !activeStoryEpisode) return;
    if (soundEnabled) onPlayTone('salute');

    setInventoryItems((prev) => [activeStoryEpisode.lootItem, ...prev]);
    setSelectedInventoryItemId(activeStoryEpisode.lootItem.id);
    setCoins((prev) => prev + activeStoryEpisode.coinsEarned);

    if (debtCoins > 0) {
      setDebtCoins(0);
      setDebtReasonRu('');
    }

    const completedEp: GeneratedStoryEpisode = {
      ...activeStoryEpisode,
      isFillerEpisode: true,
      userCustomResponseText: trimmed,
    };
    setJournalHistory((prev) => [completedEp, ...prev]);
    setLastReactionBanner(
      `Внесюжетное поручение («${trimmed}») принято! Характер и основной сюжет не затронуты.`
    );
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

  // Buy or Upgrade item in Shop
  const handleBuyShopItem = (item: ChantalShopItem) => {
    if (coins < item.priceCoins) return;
    if (soundEnabled) onPlayTone('complete');
    setCoins((prev) => prev - item.priceCoins);

    if (item.category === 'CONSUMABLE') {
      if (item.instantAction === 'HEAL_EXHAUSTION') {
        setIsExhausted45(false);
        setLastReactionBanner('Тёплый бальзам согрел Шанталь! Цена похода снова 30 Энергии.');
      } else if (item.instantAction === 'BOOST_MOOD') {
        setMoodPct(100);
        setEnergy((prev) => prev + 5);
        setLastReactionBanner('Шанталь с аппетитом съела ежевичный пирог (+5 Энергии, 100% настроение)!');
      }
      return;
    }

    if (!ownedShopItemIds.includes(item.id)) {
      setOwnedShopItemIds((prev) => [...prev, item.id]);
      setShopItemTiers((prev) => ({ ...prev, [item.id]: 1 }));
      setMoodPct((m) => Math.min(100, m + 8));
      if (item.category === 'DEN_COMFORT') {
        setPreviewDenTierOverride(null);
        setVisualDisplayMode('den_interior');
        setLastReactionBanner(
          `Куплено «${item.titleRu}»! Интерьер Домика обновлён — посмотри на панораму обустройства!`
        );
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
    setLastReactionBanner(
      `Предмет «${item.titleRu}» улучшен до Тира ${nextTier} (Мастерское качество)! Пассивный бонус увеличен.`
    );
  };

  // Buy from Traveling Merchant
  const handleBuyTravelingMerchantOffer = (offer: TravelingMerchantOffer) => {
    if (coins < offer.discountPriceCoins || claimedMerchantOfferIds.includes(offer.id)) return;
    if (soundEnabled) onPlayTone('complete');
    setCoins((prev) => prev - offer.discountPriceCoins);
    setClaimedMerchantOfferIds((prev) => [...prev, offer.id]);
    const nowIso = new Date().toISOString().slice(0, 10);

    if (offer.rewardType === 'ROYAL_CLUE_MAP') {
      setInventoryItems((prev) => [
        {
          id: `merch_clue_${Date.now()}`,
          titleRu: 'Звёздный манускрипт странствующего купца',
          category: 'ROYAL_CLUE',
          descriptionRu:
            'Старинная карта с печатью королевской лилии, выкупленная у странствующего торговца редкостями.',
          sellPriceCoins: 50,
          foundAtRegionRu: 'Фургон странствующего торговца',
          foundDateIso: nowIso,
          inspectedLoreBonusClaimed: false,
        },
        ...prev,
      ]);
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
    } else if (offer.rewardType === 'HERB_BUNDLE') {
      setInventoryItems((prev) => [
        {
          id: `merch_herb_1_${Date.now()}`,
          titleRu: 'Горный зверобой со Звёздного перевала',
          category: 'HERB',
          descriptionRu: 'Редкая трава из фургона купца. Снимает усталость или идёт на крафт оберегов.',
          sellPriceCoins: 20,
          foundAtRegionRu: 'Фургон странствующего торговца',
          foundDateIso: nowIso,
          canHealExhaustion: true,
        },
        {
          id: `merch_herb_2_${Date.now()}`,
          titleRu: 'Серебристая мята предгорий',
          category: 'HERB',
          descriptionRu: 'Душистая трава для домашней алхимии и целебного чая у камина.',
          sellPriceCoins: 20,
          foundAtRegionRu: 'Фургон странствующего торговца',
          foundDateIso: nowIso,
          canHealExhaustion: true,
        },
        ...prev,
      ]);
      setLastReactionBanner('В Сумку добавлены 2 целебные травы со Звёздного перевала!');
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

      {/* Top Currency & Atmosphere Bar */}
      <section className="rounded-[26px] p-4 sm:p-5 bg-[#1E1712] border border-[#4D3D2C] shadow-md">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="text-xs font-semibold text-[#D5C3AA]">
              {timeOfDayInfo.labelRu}
              <span className="mx-1.5" aria-hidden="true">
                ·
              </span>
              {streakMultiplierInfo.labelRu}
            </div>
            <h1 className="text-xl sm:text-2xl font-extrabold tracking-tight text-[#FFF8EB] mt-0.5">
              {stageInfo.titleRu}
            </h1>
          </div>

          <div className="flex items-center gap-2.5">
            <div className="rounded-2xl px-3.5 py-2 bg-[#281F18] border border-[#10B981]/40 flex items-center gap-2">
              <Zap className="w-4 h-4 text-[#10B981]" />
              <div>
                <div className="text-[11px] font-medium text-[#D5C3AA]">Энергия</div>
                <div className="text-sm sm:text-base font-extrabold tabular-nums text-[#10B981]">
                  {energy} ед.
                </div>
              </div>
            </div>

            <div className="rounded-2xl px-3.5 py-2 bg-[#281F18] border border-[#F59E0B]/40 flex items-center gap-2">
              <Coins className="w-4 h-4 text-[#F59E0B]" />
              <div>
                <div className="text-[11px] font-medium text-[#D5C3AA]">Монеты</div>
                <div className="text-sm sm:text-base font-extrabold tabular-nums text-[#F59E0B]">
                  {coins}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-3 sm:grid-cols-6 gap-1.5 mt-4 p-1.5 rounded-2xl bg-[#15100C]">
          {[
            { id: 'den', label: 'Домик и Уют', icon: Heart },
            { id: 'map', label: 'Карта троп', icon: Compass },
            { id: 'inventory', label: `Сумка и Крафт (${inventoryItems.length})`, icon: Backpack },
            { id: 'shop', label: 'Лавка и Купец', icon: ShoppingBag },
            { id: 'journal', label: `Летопись (${journalHistory.length})`, icon: BookOpen },
            { id: 'ai_settings', label: 'ИИ и Модели', icon: FileText },
          ].map((tab) => {
            const Icon = tab.icon;
            const isCurrent = subView === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setSubView(tab.id as ChantalSubView)}
                className="min-h-[42px] px-2.5 py-1.5 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer transition-colors whitespace-nowrap"
                style={{
                  backgroundColor: isCurrent ? '#33271D' : 'transparent',
                  color: isCurrent ? '#F59E0B' : '#C8B499',
                }}
              >
                <Icon className="w-3.5 h-3.5 shrink-0" />
                <span className="truncate">{tab.label}</span>
              </button>
            );
          })}
        </div>
      </section>

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

          {/* Loot Item Brought Back + Coins / Bad News Banner */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="rounded-2xl p-4 bg-[#1E1712] border border-[#10B981]/50 space-y-1.5">
              <div className="text-xs font-bold text-[#10B981]">
                {activeStoryEpisode.lootItem.category === 'ROYAL_CLUE'
                  ? 'Особая находка в Шкатулку королевских улик (Не продаётся)'
                  : activeStoryEpisode.lootItem.category === 'HERB'
                  ? 'Целебная находка (можно применить или сварить в оберег)'
                  : `Диковинка в сумку · Цена с Остроумием: ${getSellPriceWithWitBonus(
                      activeStoryEpisode.lootItem.sellPriceCoins
                    )} М (или подарок для NPC)`}
              </div>
              <div className="text-base font-extrabold text-[#FFF8EB]">
                {activeStoryEpisode.lootItem.titleRu}
              </div>
              <p className="text-xs text-[#D5C3AA] leading-relaxed">
                {activeStoryEpisode.lootItem.descriptionRu}
              </p>
            </div>

            <div
              className="rounded-2xl p-4 bg-[#1E1712] border space-y-1.5"
              style={{
                borderColor:
                  activeStoryEpisode.outcomeType === 'GOOD_REWARD' ? '#F59E0B88' : '#EF444499',
              }}
            >
              <div className="text-xs font-bold text-[#F59E0B]">
                Добыча из похода: +{activeStoryEpisode.coinsEarned} Монет (включая бонус Любопытства +{traitPerks.curiosityBonusCoins} и Домика +{passiveShopCoinsBonus})
              </div>
              {activeStoryEpisode.outcomeType === 'GOOD_REWARD' ? (
                <p className="text-xs text-[#D5C3AA] leading-relaxed">
                  Поход прошёл благополучно! Шанталь вернулась в отличном расположении духа и ждёт твоего отклика.
                </p>
              ) : (
                <div className="space-y-1">
                  <div className="text-xs font-extrabold text-[#F87171] flex items-center gap-1.5">
                    <AlertTriangle className="w-4 h-4 shrink-0" />
                    <span>Плохие новости из похода:</span>
                  </div>
                  <p className="text-xs text-[#FCA5A5] leading-relaxed">
                    {activeStoryEpisode.badNewsTitleRu}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* 3 or 4 Canonical Response Buttons */}
          <div className="space-y-2.5">
            <div className="text-xs font-bold text-[#D5C3AA]">
              Выбери сюжетный ответ (записывается в вечную Летопись Похода #{activeStoryEpisode.journeyNumber} и меняет характер):
            </div>
            <div className="grid grid-cols-1 gap-2.5">
              {activeStoryEpisode.choices.map((choice, idx) => {
                const previewArrows = formatTraitDeltasBadge(choice.traitDeltas);
                const isSpecialChestChoice = idx === 3;
                return (
                  <button
                    key={idx}
                    onClick={() => handleSelectCanonicalChoice(idx)}
                    className="w-full min-h-[52px] p-3.5 rounded-2xl border text-left flex flex-col sm:flex-row sm:items-center justify-between gap-2 cursor-pointer transition-colors"
                    style={{
                      backgroundColor: isSpecialChestChoice ? '#382B20' : '#1E1712',
                      borderColor: isSpecialChestChoice ? '#F59E0B' : '#5C4834',
                    }}
                  >
                    <span className="text-sm font-bold text-[#FFF8EB]">
                      {idx + 1}. {choice.labelRu}
                    </span>
                    {showTraitArrowsSetting && previewArrows.length > 0 && (
                      <span className="text-xs font-semibold text-[#F59E0B] shrink-0">
                        {previewArrows.join(' · ')}
                      </span>
                    )}
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
                className="min-h-[44px] px-4 rounded-xl bg-[#EA580C] disabled:opacity-40 text-[#FFF8EB] text-xs font-extrabold flex items-center justify-center gap-1.5 cursor-pointer shrink-0"
              >
                <Send className="w-4 h-4" />
                <span>Ответить своим текстом</span>
              </button>
            </div>
          </div>
        </section>
      )}

      {/* SUBVIEW 1: DEN & CHARACTER CARD + VISUAL DEN INTERIOR PROGRESSION */}
      {subView === 'den' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
          <div className="lg:col-span-6 space-y-5">
            <div className="rounded-[26px] overflow-hidden bg-[#1E1712] border border-[#4D3D2C] shadow-md">
              {/* Switcher between Portrait and Den Interior Panorama */}
              <div className="grid grid-cols-2 gap-1.5 p-2 bg-[#15100C] border-b border-[#382B20]">
                <button
                  onClick={() => setVisualDisplayMode('portrait')}
                  className="min-h-[38px] rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 cursor-pointer"
                  style={{
                    backgroundColor: visualDisplayMode === 'portrait' ? '#33271D' : 'transparent',
                    color: visualDisplayMode === 'portrait' ? '#F59E0B' : '#C8B499',
                  }}
                >
                  <Heart className="w-3.5 h-3.5" />
                  <span>Портрет Шанталь</span>
                </button>
                <button
                  onClick={() => setVisualDisplayMode('den_interior')}
                  className="min-h-[38px] rounded-xl text-xs font-extrabold flex items-center justify-center gap-1.5 cursor-pointer"
                  style={{
                    backgroundColor: visualDisplayMode === 'den_interior' ? '#33271D' : 'transparent',
                    color: visualDisplayMode === 'den_interior' ? '#10B981' : '#C8B499',
                  }}
                >
                  <Home className="w-3.5 h-3.5" />
                  <span>Интерьер Домика (Ур. {denVisualInfo.tierNumber}/3)</span>
                </button>
              </div>

              {visualDisplayMode === 'portrait' ? (
                <div
                  onClick={handlePetOrTalkWithChantal}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') handlePetOrTalkWithChantal();
                  }}
                  title="Нажми на портрет, чтобы погладить Шанталь или поговорить у камина"
                  className="relative aspect-square w-full max-h-[380px] overflow-hidden bg-[#120D0A] cursor-pointer group"
                >
                  <img
                    src={stageInfo.portraitUrl}
                    alt={stageInfo.titleRu}
                    referrerPolicy="no-referrer"
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-103"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-[#1E1712] via-transparent to-black/35" />

                  {isHeartPulseActive && (
                    <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                      <div className="px-4 py-2 rounded-2xl bg-black/75 border border-[#EC4899] text-[#F472B6] font-extrabold text-sm flex items-center gap-2 animate-bounce shadow-2xl">
                        <Heart className="w-5 h-5 fill-current text-[#EC4899]" />
                        <span>+{traitPerks.tendernessMoodHealBonusPct}% к настроению!</span>
                      </div>
                    </div>
                  )}

                  <div className="absolute top-3.5 left-3.5 right-3.5 flex items-center justify-between gap-2">
                    <span className="px-3 py-1 rounded-xl bg-black/65 backdrop-blur-md text-xs font-bold text-[#FFF8EB]">
                      Походов: {totalJourneys} · Настроение {moodPct}%
                    </span>
                    {activeJourney ? (
                      <span className="px-3 py-1 rounded-xl bg-[#EA580C] text-xs font-extrabold text-white">
                        В походе ({activeJourney.regionTitleRu})
                      </span>
                    ) : (
                      <span className="px-3 py-1 rounded-xl bg-black/65 text-[11px] font-bold text-[#F59E0B]">
                        Нажми: {stageInfo.isTransformed ? 'Поговорить' : 'Погладить за ушком'} ♥
                      </span>
                    )}
                  </div>

                  <div className="absolute bottom-3.5 left-4 right-4">
                    <div className="text-xs font-bold text-[#F59E0B]">
                      {stageInfo.nextStageHintRu}
                    </div>
                    <p className="text-xs text-[#E7D8C3] mt-0.5 leading-snug">
                      {stageInfo.subtitleRu}
                    </p>
                  </div>
                </div>
              ) : (
                <div className="relative aspect-video w-full overflow-hidden bg-[#120D0A]">
                  <img
                    src={denVisualInfo.imageUrl}
                    alt={denVisualInfo.titleRu}
                    referrerPolicy="no-referrer"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-[#1E1712] via-black/25 to-black/35" />
                  <div className="absolute top-3 left-3.5 right-3.5 flex flex-wrap items-center justify-between gap-2">
                    <span className="px-3 py-1 rounded-xl bg-black/75 text-xs font-extrabold text-[#10B981]">
                      Обустройство Домика: предметов уюта {ownedDenItems.length} из 5
                    </span>
                    <div className="flex items-center gap-1">
                      {([1, 2, 3] as const).map((tNum) => (
                        <button
                          key={tNum}
                          onClick={() => setPreviewDenTierOverride(tNum)}
                          className="px-2.5 py-1 rounded-lg text-[11px] font-extrabold cursor-pointer"
                          style={{
                            backgroundColor:
                              denVisualInfo.tierNumber === tNum ? '#F59E0B' : '#15100CCC',
                            color: denVisualInfo.tierNumber === tNum ? '#15100C' : '#FFF8EB',
                          }}
                        >
                          Ур. {tNum}
                        </button>
                      ))}
                    </div>
                  </div>
                  <div className="absolute bottom-3.5 left-4 right-4">
                    <div className="text-sm font-extrabold text-[#FFF8EB]">
                      {denVisualInfo.titleRu}
                    </div>
                    <p className="text-xs text-[#E7D8C3] mt-0.5 leading-snug">
                      {denVisualInfo.subtitleRu}
                    </p>
                  </div>
                </div>
              )}

              <div className="p-4 sm:p-5 space-y-4">
                <button
                  onClick={handlePetOrTalkWithChantal}
                  className="w-full min-h-[44px] px-4 py-2.5 rounded-2xl bg-[#2A2018] hover:bg-[#382B20] border border-[#EC4899]/50 text-xs sm:text-sm font-extrabold text-[#FFF8EB] flex items-center justify-center gap-2 cursor-pointer transition-transform active:scale-98"
                >
                  <Heart className="w-4 h-4 text-[#EC4899] fill-current shrink-0" />
                  <span>
                    {stageInfo.isTransformed
                      ? 'Поговорить с Шанталь у камина'
                      : 'Погладить Шанталь за рыжим ушком'}{' '}
                    (+{traitPerks.tendernessMoodHealBonusPct}% настроения)
                  </span>
                </button>

                {petSpeechBubble && (
                  <div className="rounded-2xl p-3.5 bg-[#281F18] border border-[#EC4899]/50 space-y-1">
                    <div className="text-[11px] font-bold text-[#F472B6]">
                      {petSpeechBubble.badgeRu}
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

                {/* Visual Den Panorama Mini-Card & Active Upgrades */}
                <div className="rounded-2xl p-3.5 bg-[#281F18] border border-[#10B981]/40 space-y-2.5">
                  <div className="flex items-center justify-between gap-2">
                    <div>
                      <div className="text-xs font-extrabold text-[#10B981]">
                        {denVisualInfo.titleRu}
                      </div>
                      <div className="text-[11px] text-[#D5C3AA] mt-0.5">
                        {denVisualInfo.nextUnlockHintRu}
                      </div>
                    </div>
                    <button
                      onClick={() =>
                        setVisualDisplayMode((m) =>
                          m === 'den_interior' ? 'portrait' : 'den_interior'
                        )
                      }
                      className="px-3 py-1.5 rounded-xl bg-[#1E1712] border border-[#10B981]/50 text-[11px] font-extrabold text-[#10B981] shrink-0 cursor-pointer"
                    >
                      {visualDisplayMode === 'den_interior' ? 'К портрету' : 'Показать интерьер'}
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
                    {equippedApparelItems.map((gear) => (
                      <div
                        key={gear.id}
                        className="rounded-xl px-3 py-1.5 bg-[#1E1712] border border-[#F59E0B]/40 text-xs font-semibold text-[#FFF8EB]"
                      >
                        ✦ {gear.titleRu} (Тир {shopItemTiers[gear.id] ?? 1})
                      </div>
                    ))}
                    {ownedDenItems.map((den) => (
                      <div
                        key={den.id}
                        className="rounded-xl px-3 py-1.5 bg-[#1E1712] border border-[#10B981]/40 text-xs font-semibold text-[#E7D8C3]"
                      >
                        ⌂ {den.titleRu} (Тир {shopItemTiers[den.id] ?? 1})
                      </div>
                    ))}
                  </div>
                </div>

                <div className="space-y-3 pt-2 border-t border-[#382B20]">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <span className="text-sm font-extrabold text-[#FFF8EB]">
                        Характер Шанталь и активные бонусы (6 черт)
                      </span>
                      <div className="text-[11px] text-[#D5C3AA]">
                        Любопытство, Нежность, Игривость, Романтичность, Отвага и Остроумие меняются от твоих ответов в историях
                      </div>
                    </div>
                    <span className="px-2.5 py-1 rounded-xl bg-[#281F18] border border-[#F59E0B]/40 text-xs font-extrabold text-[#F59E0B]">
                      Ведущая: {traitPerks.dominantTraitLabelRu}
                    </span>
                  </div>

                  {/* VISUAL SUMMARY OF LAST TRAIT CHANGE AFTER STORY CHOICE */}
                  {lastTraitChangeRecord && (
                    <div className="rounded-2xl p-3.5 bg-[#261C15] border-2 border-[#F59E0B] space-y-2 shadow-md">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-[11px] font-extrabold text-[#F59E0B] flex items-center gap-1.5">
                          <Sparkles className="w-3.5 h-3.5" />
                          <span>Изменение характера после выбора в истории «{lastTraitChangeRecord.storyTitleRu}»:</span>
                        </span>
                        <button
                          onClick={() => setLastTraitChangeRecord(null)}
                          className="text-[11px] font-bold text-[#D5C3AA] hover:text-white cursor-pointer"
                        >
                          Скрыть
                        </button>
                      </div>
                      <div className="text-xs font-bold text-[#FFF8EB]">
                        Ответ: «{lastTraitChangeRecord.choiceLabelRu}»
                      </div>
                      <div className="flex flex-wrap gap-1.5 pt-0.5">
                        {TRAIT_METADATA.map((meta) => {
                          const delta = lastTraitChangeRecord.deltas[meta.key] || 0;
                          if (delta === 0) return null;
                          const prevVal = lastTraitChangeRecord.previousTraits[meta.key];
                          const nextVal = lastTraitChangeRecord.newTraits[meta.key];
                          const arrows =
                            delta >= 2 ? '↑↑' : delta === 1 ? '↑' : delta <= -2 ? '↓↓' : '↓';
                          const isPositive = delta > 0;
                          return (
                            <span
                              key={meta.key}
                              className="px-2.5 py-1 rounded-xl text-xs font-extrabold border tabular-nums"
                              style={{
                                backgroundColor: isPositive ? '#064E3B90' : '#7F1D1D90',
                                borderColor: isPositive ? '#10B981' : '#F87171',
                                color: isPositive ? '#6EE7B7' : '#FCA5A5',
                              }}
                            >
                              {meta.labelRu}: {prevVal} → {nextVal} ({isPositive ? `+${delta}` : delta}{' '}
                              {arrows})
                            </span>
                          );
                        })}
                      </div>
                    </div>
                  )}

                  {/* 6 CHARACTER TRAITS GRID WITH LIVE DELTA BADGES */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    {TRAIT_METADATA.map((meta) => {
                      const val = traits[meta.key];
                      const delta = lastTraitChangeRecord?.deltas[meta.key] || 0;
                      const prevVal = lastTraitChangeRecord?.previousTraits[meta.key] ?? val;
                      const pct = Math.min(100, Math.max(10, val * 3));
                      const prevPct = Math.min(100, Math.max(10, prevVal * 3));
                      const perkDesc = getTraitActivePerkDescriptionRu(meta.key, traitPerks);
                      const isChanged = delta !== 0;
                      const arrows =
                        delta >= 2 ? '↑↑' : delta === 1 ? '↑' : delta <= -2 ? '↓↓' : '↓';

                      return (
                        <div
                          key={meta.key}
                          className="rounded-xl p-2.5 bg-[#281F18] border space-y-1.5 transition-all duration-300"
                          style={{
                            borderColor: isChanged
                              ? delta > 0
                                ? '#10B981'
                                : '#F87171'
                              : '#382B20',
                            backgroundColor: isChanged ? '#30241B' : '#281F18',
                          }}
                        >
                          <div className="flex items-center justify-between gap-1.5 text-xs font-bold">
                            <span className="text-[#FFF8EB]">{meta.labelRu}</span>
                            <div className="flex items-center gap-1.5 tabular-nums">
                              {isChanged && (
                                <span
                                  className="px-1.5 py-0.5 rounded-md text-[10px] font-extrabold"
                                  style={{
                                    backgroundColor: delta > 0 ? '#10B98125' : '#EF444425',
                                    color: delta > 0 ? '#34D399' : '#F87171',
                                  }}
                                >
                                  {prevVal}→{val} ({delta > 0 ? `+${delta}` : delta} {arrows})
                                </span>
                              )}
                              <span
                                className="font-extrabold text-sm"
                                style={{ color: meta.colorHex }}
                              >
                                {val}
                              </span>
                            </div>
                          </div>
                          <div className="relative w-full h-2 rounded-full bg-[#15100C] overflow-hidden">
                            <div
                              className="h-full rounded-full transition-all duration-500"
                              style={{
                                width: `${pct}%`,
                                backgroundColor: meta.colorHex,
                              }}
                            />
                            {delta > 0 && pct > prevPct && (
                              <div
                                className="absolute top-0 bottom-0 rounded-r-full bg-white/40 animate-pulse"
                                style={{
                                  left: `${prevPct}%`,
                                  width: `${Math.max(4, pct - prevPct)}%`,
                                }}
                              />
                            )}
                          </div>
                          <div className="text-[11px] text-[#D5C3AA] leading-tight">
                            {perkDesc}
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* INTERACTIVE STORY RESPONSE SIMULATOR TO TEST TRAIT CHANGES ANYTIME */}
                  <div className="rounded-2xl p-3.5 bg-[#281F18] border border-[#4D3D2C] space-y-2">
                    <div className="text-xs font-extrabold text-[#F59E0B]">
                      Быстрый сюжетный выбор у камина (Проверь визуальное изменение 6 черт):
                    </div>
                    <div className="grid grid-cols-1 gap-1.5">
                      <button
                        onClick={() => {
                          if (soundEnabled) onPlayTone('salute');
                          applyTraitDeltasFromChoice(
                            { curiosity: 2, romanticism: 2, tenderness: 1 },
                            'Расспросить о звёздной карте и ласково укрыть пледом у камина',
                            'Вечерняя беседа у очага'
                          );
                          setLastReactionBanner(
                            'Шанталь с восторгом показывает тебе созвездия на карте и доверчиво прижимается к плечу!'
                          );
                        }}
                        className="w-full px-3 py-2 rounded-xl bg-[#1E1712] hover:bg-[#33271D] border border-[#5C4834] text-left flex flex-wrap items-center justify-between gap-1.5 text-xs cursor-pointer"
                      >
                        <span className="font-bold text-[#FFF8EB]">
                          1. Расспросить о звёздной карте и укрыть пледом
                        </span>
                        <span className="font-extrabold text-[#34D399]">
                          Любопытство +2 ↑↑ · Романтичность +2 ↑↑ · Нежность +1 ↑
                        </span>
                      </button>

                      <button
                        onClick={() => {
                          if (soundEnabled) onPlayTone('salute');
                          applyTraitDeltasFromChoice(
                            { playfulness: 2, wit: 2, tenderness: -1 },
                            'Устроить весёлую дуэль загадок и подшутить над вороном Корвусом',
                            'Игры и загадки в Домике'
                          );
                          setLastReactionBanner(
                            'Глаза Шанталь озорно блестят: она тут же придумывает хитрую ответную шараду!'
                          );
                        }}
                        className="w-full px-3 py-2 rounded-xl bg-[#1E1712] hover:bg-[#33271D] border border-[#5C4834] text-left flex flex-wrap items-center justify-between gap-1.5 text-xs cursor-pointer"
                      >
                        <span className="font-bold text-[#FFF8EB]">
                          2. Устроить весёлую дуэль загадок и подшутить
                        </span>
                        <span className="font-extrabold text-[#F59E0B]">
                          Игривость +2 ↑↑ · Остроумие +2 ↑↑ · Нежность -1 ↓
                        </span>
                      </button>

                      <button
                        onClick={() => {
                          if (soundEnabled) onPlayTone('salute');
                          applyTraitDeltasFromChoice(
                            { courage: 2, curiosity: 1, playfulness: -1 },
                            'Похвалить за смелость в грозу и вместе проверить лесные тропы',
                            'Урок отваги следопыта'
                          );
                          setLastReactionBanner(
                            'Шанталь гордо расправляет плечи — теперь никакая буря на перевале ей не страшна!'
                          );
                        }}
                        className="w-full px-3 py-2 rounded-xl bg-[#1E1712] hover:bg-[#33271D] border border-[#5C4834] text-left flex flex-wrap items-center justify-between gap-1.5 text-xs cursor-pointer"
                      >
                        <span className="font-bold text-[#FFF8EB]">
                          3. Похвалить за смелость в грозу и проверить тропы
                        </span>
                        <span className="font-extrabold text-[#60A5FA]">
                          Отвага +2 ↑↑ · Любопытство +1 ↑ · Игривость -1 ↓
                        </span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column */}
          <div className="lg:col-span-6 space-y-5">
            {/* LIVE REACTION TO REAL KEGEL WORKOUTS CARD */}
            <div className="rounded-[26px] p-4 sm:p-5 bg-[#1E1712] border border-[#10B981]/50 space-y-3">
              <div className="flex items-center justify-between gap-2">
                <div>
                  <div className="text-xs font-extrabold text-[#10B981]">
                    Живая связь с тренировками Кегеля
                  </div>
                  <h3 className="text-base font-extrabold text-[#FFF8EB]">
                    Сегодня выполнено: {todayPlannedDoneCount} / 9 сессий (Закрыто блоков: {todayClosedPeriodsCount}/3)
                  </h3>
                </div>
                <Flame className="w-5 h-5 text-[#10B981] shrink-0" />
              </div>

              <p className="text-xs text-[#D5C3AA] leading-relaxed">
                {lastCompletedWorkoutTitle
                  ? `Последняя выполненная тренировка: «${lastCompletedWorkoutTitle}». Шанталь гордится твоей дисциплиной и приготовила награду за закрытый блок!`
                  : 'Каждая выполненная тренировка даёт +10 Энергии, а за каждый закрытый блок (Утро, Обед, Вечер) Шанталь вручает Бонус дисциплины (+15 Монет, +5 Энергии и +1 к Отваге ↑).'}
              </p>

              <div className="flex flex-wrap gap-2">
                <button
                  onClick={handleClaimWorkoutBlockPraise}
                  disabled={claimedWorkoutPeriodsToday >= Math.max(1, todayClosedPeriodsCount + 1)}
                  className="min-h-[40px] px-3.5 rounded-xl bg-[#10B981] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
                >
                  {claimedWorkoutPeriodsToday >= Math.max(1, todayClosedPeriodsCount + 1)
                    ? '✓ Награда за текущий блок получена'
                    : 'Забрать похвалу от Шанталь (+15 М · +5 Эн. · +1 Отвага ↑)'}
                </button>
                <button
                  onClick={onGoToWorkout}
                  className="min-h-[40px] px-3.5 rounded-xl bg-[#281F18] border border-[#4D3D2C] text-xs font-bold text-[#FFF8EB] cursor-pointer"
                >
                  Открыть план тренировок
                </button>
              </div>
            </div>

            {(isExhausted45 || debtCoins > 0) && (
              <div className="rounded-[26px] p-5 bg-[#2B1616] border border-[#EF4444] space-y-3">
                <div className="flex items-center gap-2 text-sm font-extrabold text-[#FCA5A5]">
                  <AlertTriangle className="w-5 h-5 text-[#EF4444] shrink-0" />
                  <span>Последствия приключения («Плохие новости»)</span>
                </div>

                {isExhausted45 && (
                  <div className="rounded-2xl p-3.5 bg-[#1C1010] space-y-2">
                    <div className="text-xs font-bold text-[#FCA5A5]">
                      Усталость после тяжёлой дороги: следующий поход требует{' '}
                      <span className="underline tabular-nums">{requiredJourneyEnergy} Энергии</span>{' '}
                      вместо 30
                    </div>
                    <p className="text-xs text-[#E7D8C3]">
                      Ты можешь накопить {requiredJourneyEnergy} Энергии упражнениями Кегеля, применить целебную траву из Сумки бесплатно ИЛИ согреть Шанталь медово-травяным бальзамом за 20 Монет.
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {herbsList.length > 0 && (
                        <button
                          onClick={() => handleUseHerbFromInventory(herbsList[0])}
                          className="min-h-[42px] px-4 rounded-xl bg-[#10B981] text-white text-xs font-extrabold cursor-pointer"
                        >
                          Заварить «{herbsList[0].titleRu}» из Сумки (Бесплатно)
                        </button>
                      )}
                      <button
                        onClick={() => {
                          const balm = CHANTAL_SHOP_ITEMS.find(
                            (i) => i.id === 'supply_herbal_balm'
                          );
                          if (balm) handleBuyShopItem(balm);
                        }}
                        disabled={coins < 20}
                        className="min-h-[42px] px-4 rounded-xl bg-[#EA580C] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
                      >
                        Бальзам из лавки за 20 Монет
                      </button>
                    </div>
                  </div>
                )}

                {debtCoins > 0 && (
                  <div className="rounded-2xl p-3.5 bg-[#1C1010] space-y-2">
                    <div className="text-xs font-bold text-[#FCA5A5]">
                      Долг в монетах: {debtCoins} Монет ({debtReasonRu})
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
                        className="min-h-[42px] px-4 rounded-xl bg-[#F59E0B] disabled:opacity-40 text-[#15100C] text-xs font-extrabold cursor-pointer"
                      >
                        Оплатить {debtCoins} Монет сейчас
                      </button>
                      <button
                        onClick={() => {
                          setCustomFillerInput(
                            'Отправляйся на ярмарку и помоги мастеру отработать наш долг!'
                          );
                        }}
                        className="min-h-[42px] px-4 rounded-xl bg-[#281F18] text-[#FFF8EB] text-xs font-bold cursor-pointer"
                      >
                        Или отработать в филлер-поручении
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}

            <div className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-extrabold text-[#FFF8EB]">
                    {activeJourney
                      ? `Шанталь в Походе #${totalJourneys + 1}`
                      : isNextJourneyMilestoneChest
                      ? `★ Отправить за Юбилейным Сундуком Главы #${totalJourneys + 1}!`
                      : `Отправить в Поход #${totalJourneys + 1}`}
                  </h2>
                  <p className="text-xs text-[#D5C3AA] mt-0.5">
                    {activeJourney
                      ? activeJourney.stayedInSameRegion
                        ? `Вернулась в «${activeJourney.regionTitleRu}» ради незаконченных дел (${activeJourney.encounteredNpcRu})`
                        : `Она сама выбрала маршрут: «${activeJourney.regionTitleRu}» (${activeJourney.encounteredNpcRu})`
                      : `ИИ помнит все твои решения с Похода #1 по #${totalJourneys} · До Сундука Главы #${nextMilestoneChestJourney}: ${Math.max(
                          1,
                          nextMilestoneChestJourney - totalJourneys
                        )} пох.`}
                  </p>
                </div>
                <Compass className="w-6 h-6 text-[#F59E0B]" />
              </div>

              {hasActiveRoadAmulet && (
                <div className="rounded-xl p-3 bg-[#281F18] border border-[#F59E0B] text-xs font-bold text-[#F59E0B] flex items-center gap-2">
                  <Sparkles className="w-4 h-4 shrink-0" />
                  <span>
                    Активен Оберег Попутного Ветра: 100% защита от неудач и повышенные монеты в следующем походе!
                  </span>
                </div>
              )}

              {activeJourney ? (
                <div className="space-y-4">
                  <div className="rounded-2xl p-4 bg-[#281F18] border border-[#F59E0B]/40 text-center space-y-2">
                    <div className="text-xs font-semibold text-[#D5C3AA] flex items-center justify-center gap-1.5">
                      <Timer className="w-4 h-4 text-[#F59E0B]" />
                      <span>
                        {isJourneyFinished
                          ? 'Шанталь вернулась домой и ждёт тебя!'
                          : 'До возвращения из похода (таймер 60 мин):'}
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
                      className="min-h-[50px] rounded-2xl bg-[#EA580C] hover:bg-[#D9460F] text-white font-extrabold text-sm flex items-center justify-center gap-2 cursor-pointer shadow-md"
                    >
                      <Sparkles className="w-4 h-4" />
                      <span>
                        {isGeneratingStory
                          ? 'ИИ пишет историю...'
                          : isJourneyFinished
                          ? 'Встретить и прочитать историю'
                          : 'Проверить сейчас (Тест без ожидания часа)'}
                      </span>
                    </button>

                    <button
                      onClick={() => setSubView('map')}
                      className="min-h-[50px] rounded-2xl bg-[#281F18] border border-[#4D3D2C] text-[#FFF8EB] font-bold text-sm flex items-center justify-center gap-2 cursor-pointer"
                    >
                      <MapPin className="w-4 h-4 text-[#F59E0B]" />
                      <span>Посмотреть её путь на карте</span>
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="rounded-2xl p-4 bg-[#281F18] space-y-2">
                    <div className="flex items-center justify-between text-xs font-bold">
                      <span className="text-[#D5C3AA]">
                        Требуется Энергии для старта похода (1 час):
                      </span>
                      <span
                        className="tabular-nums font-extrabold"
                        style={{
                          color: energy >= requiredJourneyEnergy ? '#10B981' : '#F59E0B',
                        }}
                      >
                        {energy} / {requiredJourneyEnergy} Энергии
                      </span>
                    </div>
                    <div className="w-full h-2.5 rounded-full bg-[#15100C] overflow-hidden">
                      <div
                        className="h-full rounded-full transition-all duration-300"
                        style={{
                          width: `${Math.min(
                            100,
                            Math.round((energy / requiredJourneyEnergy) * 100)
                          )}%`,
                          backgroundColor:
                            energy >= requiredJourneyEnergy ? '#10B981' : '#F59E0B',
                        }}
                      />
                    </div>
                    <p className="text-xs text-[#D5C3AA]">
                      1 упражнение Кегеля = <span className="text-[#10B981] font-bold">+10 Энергии</span>. Три упражнения за Утро, Обед или Вечер дают ровно 30 Энергии на поход!
                    </p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    <button
                      onClick={() => handleStartAutonomousJourney(false)}
                      disabled={energy < requiredJourneyEnergy}
                      className="min-h-[52px] rounded-2xl bg-[#EA580C] disabled:opacity-40 text-white font-extrabold text-sm flex items-center justify-center gap-2 cursor-pointer transition-transform active:scale-98 shadow-md"
                    >
                      <Compass className="w-5 h-5" />
                      <span>Отправить в поход ({requiredJourneyEnergy} Эн.)</span>
                    </button>

                    <button
                      onClick={() => {
                        const autoDest = pickAutonomousRegionForChantal(
                          totalJourneys,
                          journalHistory[0]?.regionId,
                          journalHistory[0]?.encounteredNpcRu,
                          false
                        );
                        handleFinishJourneyAndReadStory(
                          {
                            startedAtEpochMs: Date.now(),
                            durationMs: 3600000,
                            regionId: autoDest.region.id,
                            regionTitleRu: autoDest.region.titleRu,
                            encounteredNpcRu: autoDest.encounteredNpcRu,
                            npcRoleRu: autoDest.npcRoleRu,
                            dailyArcStep: timeOfDayInfo.dailyArcStep,
                            isFillerEpisode: false,
                            stayedInSameRegion: autoDest.stayedInSameRegion,
                            autonomousReasonRu: autoDest.decisionReasonRu,
                          },
                          true
                        );
                      }}
                      disabled={isGeneratingStory}
                      className="min-h-[52px] rounded-2xl bg-[#281F18] border border-[#F59E0B] text-[#F59E0B] font-extrabold text-xs sm:text-sm flex items-center justify-center gap-1.5 cursor-pointer"
                    >
                      <Sparkles className="w-4 h-4 shrink-0" />
                      <span>Тест: Юбилейный Сундук Главы (#{nextMilestoneChestJourney})</span>
                    </button>
                  </div>

                  <div className="rounded-2xl p-3.5 bg-[#281F18] border border-[#4D3D2C] space-y-2">
                    <div className="text-xs font-bold text-[#F59E0B]">
                      Личное поручение (Побочный филлер-поход — не влияет на основной сюжет и характер):
                    </div>
                    <div className="flex flex-col sm:flex-row gap-2">
                      <input
                        type="text"
                        value={customFillerInput}
                        onChange={(e) => setCustomFillerInput(e.target.value)}
                        placeholder="Куда лично послать Шанталь или что попросить сделать?"
                        className="flex-1 min-h-[42px] px-3 rounded-xl bg-[#15100C] border border-[#4D3D2C] text-xs text-[#FFF8EB] placeholder-[#8C7A65]"
                      />
                      <button
                        onClick={() => {
                          if (!customFillerInput.trim()) return;
                          handleStartAutonomousJourney(true, customFillerInput.trim());
                          setCustomFillerInput('');
                        }}
                        disabled={energy < requiredJourneyEnergy || !customFillerInput.trim()}
                        className="min-h-[42px] px-3.5 rounded-xl bg-[#382B20] hover:bg-[#4D3D2C] disabled:opacity-40 text-xs font-extrabold text-[#FFF8EB] cursor-pointer shrink-0"
                      >
                        Послать в филлер ({requiredJourneyEnergy} Эн.)
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* QUICK INVENTORY & ITEM DESCRIPTION CARD ON DEN SCREEN */}
            <div className="rounded-[26px] p-4 sm:p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h2 className="text-base font-extrabold text-[#FFF8EB] flex items-center gap-2">
                    <Backpack className="w-4 h-4 text-[#F59E0B]" />
                    <span>Инвентарь Шанталь: Находки из путешествий ({inventoryItems.length})</span>
                  </h2>
                  <p className="text-xs text-[#D5C3AA] mt-0.5">
                    Выбери любой принесённый из похода предмет, чтобы открыть карточку его описания и применить в деле
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleAddSampleJourneyLoot}
                    className="min-h-[34px] px-3 rounded-xl bg-[#281F18] hover:bg-[#382B20] border border-[#F59E0B]/60 text-xs font-extrabold text-[#F59E0B] cursor-pointer"
                  >
                    + Находка из похода
                  </button>
                  <button
                    onClick={() => setSubView('inventory')}
                    className="min-h-[34px] px-3 rounded-xl bg-[#33271D] text-xs font-extrabold text-[#FFF8EB] cursor-pointer"
                  >
                    Вся сумка →
                  </button>
                </div>
              </div>

              {/* Selectable Pills for Items in inventoryItems State Array */}
              {inventoryItems.length === 0 ? (
                <div className="rounded-2xl p-3.5 bg-[#281F18] text-xs text-[#D5C3AA]">
                  Сумка пуста — отправь Шанталь в путешествие или нажми «+ Находка из похода».
                </div>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {inventoryItems.map((item) => {
                    const isSelected = selectedInventoryItem?.id === item.id;
                    const badgeColor =
                      item.category === 'ROYAL_CLUE'
                        ? '#F59E0B'
                        : item.category === 'HERB'
                        ? '#10B981'
                        : '#E7D8C3';
                    return (
                      <button
                        key={item.id}
                        onClick={() => setSelectedInventoryItemId(item.id)}
                        className="px-3 py-1.5 rounded-xl border text-xs font-bold flex items-center gap-1.5 cursor-pointer transition-colors"
                        style={{
                          backgroundColor: isSelected ? '#382B20' : '#15100C',
                          borderColor: isSelected ? '#F59E0B' : '#4D3D2C',
                          color: isSelected ? '#FFF8EB' : badgeColor,
                        }}
                      >
                        <span>
                          {item.category === 'ROYAL_CLUE'
                            ? '♛'
                            : item.category === 'HERB'
                            ? '🌿'
                            : '✦'}
                        </span>
                        <span className="truncate max-w-[190px]">{item.titleRu}</span>
                      </button>
                    );
                  })}
                </div>
              )}

              {/* UI-КАРТОЧКА ОТОБРАЖЕНИЯ ОПИСАНИЯ ПРЕДМЕТА */}
              {selectedInventoryItem && (
                <div className="rounded-2xl p-4 bg-[#281F18] border-2 border-[#F59E0B]/60 space-y-3 shadow-inner">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className="px-2.5 py-0.5 rounded-lg text-[11px] font-extrabold"
                          style={{
                            backgroundColor:
                              selectedInventoryItem.category === 'ROYAL_CLUE'
                                ? '#F59E0B25'
                                : selectedInventoryItem.category === 'HERB'
                                ? '#10B98125'
                                : '#382B20',
                            color:
                              selectedInventoryItem.category === 'ROYAL_CLUE'
                                ? '#F59E0B'
                                : selectedInventoryItem.category === 'HERB'
                                ? '#10B981'
                                : '#FAF0E1',
                          }}
                        >
                          {selectedInventoryItem.category === 'ROYAL_CLUE'
                            ? '♛ Королевская улика (Защищено)'
                            : selectedInventoryItem.category === 'HERB'
                            ? '🌿 Целебная трава'
                            : '✦ Памятная диковинка'}
                        </span>
                        <span className="text-xs font-semibold text-[#D5C3AA]">
                          Локация: {selectedInventoryItem.foundAtRegionRu} · {selectedInventoryItem.foundDateIso}
                        </span>
                      </div>
                      <h3 className="text-base font-extrabold text-[#FFF8EB] mt-1.5">
                        {selectedInventoryItem.titleRu}
                      </h3>
                    </div>
                    <span className="px-2.5 py-1 rounded-xl bg-[#15100C] border border-[#4D3D2C] text-xs font-extrabold text-[#F59E0B] tabular-nums">
                      {selectedInventoryItem.category === 'ROYAL_CLUE'
                        ? 'Бесценная реликвия'
                        : `Ценность: ${getSellPriceWithWitBonus(selectedInventoryItem.sellPriceCoins)} Монет`}
                    </span>
                  </div>

                  <p className="text-xs sm:text-sm text-[#FAF0E1] leading-relaxed bg-[#1E1712] p-3 rounded-xl border border-[#382B20]">
                    {selectedInventoryItem.descriptionRu}
                  </p>

                  <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
                    {selectedInventoryItem.category === 'ROYAL_CLUE' ? (
                      <button
                        onClick={() => handleInspectRoyalClue(selectedInventoryItem)}
                        className="min-h-[38px] px-3.5 rounded-xl bg-[#F59E0B] text-[#15100C] text-xs font-extrabold cursor-pointer"
                      >
                        {selectedInventoryItem.inspectedLoreBonusClaimed
                          ? '✓ Улика изучена (хранится в шкатулке тайн)'
                          : 'Изучить улику у камина (+1 Любопытство ↑)'}
                      </button>
                    ) : selectedInventoryItem.category === 'HERB' ? (
                      <div className="flex flex-wrap items-center gap-2">
                        <button
                          onClick={() => handleUseHerbFromInventory(selectedInventoryItem)}
                          className="min-h-[38px] px-3.5 rounded-xl bg-[#10B981] text-white text-xs font-extrabold cursor-pointer"
                        >
                          Заварить траву (Снять усталость +15% настр.)
                        </button>
                        <button
                          onClick={() => handleSellInventoryItem(selectedInventoryItem)}
                          className="min-h-[38px] px-3 rounded-xl bg-[#382B20] text-xs font-extrabold text-[#F59E0B] cursor-pointer"
                        >
                          Продать скупщику (+{getSellPriceWithWitBonus(selectedInventoryItem.sellPriceCoins)} М)
                        </button>
                      </div>
                    ) : (
                      <div className="flex flex-wrap items-center gap-2">
                        <button
                          onClick={() =>
                            handleGiftTrinketToNpc(
                              selectedInventoryItem,
                              KEY_COMPANION_NPCS.find((n) => n.npcId === selectedGiftNpcId) ??
                                KEY_COMPANION_NPCS[0]
                            )
                          }
                          className="min-h-[38px] px-3.5 rounded-xl bg-[#EA580C] text-white text-xs font-extrabold cursor-pointer"
                        >
                          Подарить другу-NPC (+18 М и черта ↑)
                        </button>
                        <button
                          onClick={() => handleSellInventoryItem(selectedInventoryItem)}
                          className="min-h-[38px] px-3 rounded-xl bg-[#382B20] text-xs font-extrabold text-[#F59E0B] cursor-pointer"
                        >
                          Продать скупщику (+{getSellPriceWithWitBonus(selectedInventoryItem.sellPriceCoins)} М)
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              )}
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

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {WORLD_MAP_REGIONS.map((reg) => {
              const isUnlocked = totalJourneys >= reg.minJourneysRequired;
              return (
                <div
                  key={reg.id}
                  className="rounded-2xl p-4 bg-[#281F18] border border-[#4D3D2C] space-y-2"
                  style={{ opacity: isUnlocked ? 1 : 0.6 }}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-extrabold text-[#FFF8EB]">
                      {reg.titleRu}
                    </span>
                    <span className="text-xs font-bold text-[#F59E0B]">
                      {isUnlocked ? 'Тропа открыта' : `Нужно ${reg.minJourneysRequired} походов`}
                    </span>
                  </div>
                  <p className="text-xs text-[#D5C3AA] leading-relaxed">{reg.loreRu}</p>
                  <div className="text-xs font-semibold text-[#10B981]">
                    Персонажи локации ({reg.npcs.length}):{' '}
                    {reg.npcs.map((n) => n.nameRu).join(' · ')}
                  </div>
                  <div className="text-[11px] text-[#E7D8C3]">
                    Возможные дела: {reg.unfinishedThreadsRu[0]}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* SUBVIEW 3: SMART 3-SECTION INVENTORY + ITEM DESCRIPTION CARD + HOME ALCHEMY & NPC GIFTING */}
      {subView === 'inventory' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-xl font-extrabold text-[#FFF8EB]">
                Сумка находок, Карточка описания предмета и Домашняя Алхимия
              </h2>
              <p className="text-xs text-[#D5C3AA] mt-0.5">
                Нажми на любой предмет из путешествий, чтобы изучить его подробное описание, историю находки и свойства
              </p>
            </div>
            <div className="flex items-center gap-2.5">
              <button
                onClick={handleAddSampleJourneyLoot}
                className="min-h-[36px] px-3.5 rounded-xl bg-[#281F18] hover:bg-[#382B20] border border-[#F59E0B] text-xs font-extrabold text-[#F59E0B] cursor-pointer"
              >
                + Симулировать находку из похода
              </button>
              <span className="text-xs font-bold text-[#F59E0B]">
                Всего находок: {inventoryItems.length}
              </span>
            </div>
          </div>

          {lastReactionBanner && (
            <div className="rounded-2xl p-3.5 bg-[#2A2018] border border-[#F59E0B] text-xs font-semibold text-[#FAF0E1]">
              {lastReactionBanner}
            </div>
          )}

          {/* DEDICATED UI CARD FOR DISPLAYING SELECTED ITEM DESCRIPTION */}
          {selectedInventoryItem && (
            <div className="rounded-2xl p-4 sm:p-5 bg-[#261C15] border-2 border-[#F59E0B] space-y-3 shadow-lg">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-lg bg-[#F59E0B] text-[#15100C] text-[11px] font-extrabold">
                      Карточка описания предмета
                    </span>
                    <span className="text-xs font-bold text-[#F59E0B]">
                      {selectedInventoryItem.category === 'ROYAL_CLUE'
                        ? '♛ Королевская улика'
                        : selectedInventoryItem.category === 'HERB'
                        ? '🌿 Целебная трава'
                        : '✦ Памятная диковинка'}
                    </span>
                    <span className="text-xs text-[#D5C3AA]">
                      Принесено из локации «{selectedInventoryItem.foundAtRegionRu}» ({selectedInventoryItem.foundDateIso})
                    </span>
                  </div>
                  <h3 className="text-lg font-extrabold text-[#FFF8EB]">
                    {selectedInventoryItem.titleRu}
                  </h3>
                </div>
                <div className="px-3 py-1 rounded-xl bg-[#15100C] border border-[#5C4834] text-xs font-extrabold text-[#F59E0B] tabular-nums">
                  {selectedInventoryItem.category === 'ROYAL_CLUE'
                    ? 'Не продаётся (Реликвия сюжета)'
                    : `Цена у скупщика: ${getSellPriceWithWitBonus(selectedInventoryItem.sellPriceCoins)} Монет (база ${selectedInventoryItem.sellPriceCoins} М)`}
                </div>
              </div>

              <div className="rounded-xl p-3.5 bg-[#1E1712] border border-[#4D3D2C] space-y-1.5">
                <div className="text-[11px] font-bold text-[#D5C3AA]">
                  Подробное описание и свойства находки:
                </div>
                <p className="text-sm text-[#FFF8EB] leading-relaxed">
                  {selectedInventoryItem.descriptionRu}
                </p>
              </div>

              <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
                <div className="text-xs text-[#D5C3AA]">
                  {selectedInventoryItem.category === 'ROYAL_CLUE'
                    ? 'Свойство: Раскрывает крупицу тайны проклятия Шанталь и даёт +1 к Любопытству при осмотре.'
                    : selectedInventoryItem.category === 'HERB'
                    ? 'Свойство: Снимает дорожную усталость (возвращает цену похода к 30 Энергии) или служит ингредиентом Алхимии.'
                    : 'Свойство: Можно подарить жителю леса для повышения уровня Дружбы (+18 Монет и черта ↑) или продать.'}
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  {selectedInventoryItem.category === 'ROYAL_CLUE' ? (
                    <button
                      onClick={() => handleInspectRoyalClue(selectedInventoryItem)}
                      className="min-h-[38px] px-3.5 rounded-xl bg-[#F59E0B] text-[#15100C] text-xs font-extrabold cursor-pointer"
                    >
                      {selectedInventoryItem.inspectedLoreBonusClaimed
                        ? '✓ Изучено (в коллекции)'
                        : 'Изучить у камина (+1 Любопытство ↑)'}
                    </button>
                  ) : selectedInventoryItem.category === 'HERB' ? (
                    <>
                      <button
                        onClick={() => handleUseHerbFromInventory(selectedInventoryItem)}
                        className="min-h-[38px] px-3.5 rounded-xl bg-[#10B981] text-white text-xs font-extrabold cursor-pointer"
                      >
                        Дать Шанталь (+15% настр. и снять усталость)
                      </button>
                      <button
                        onClick={() => handleSellInventoryItem(selectedInventoryItem)}
                        className="min-h-[38px] px-3 rounded-xl bg-[#382B20] text-xs font-extrabold text-[#F59E0B] cursor-pointer"
                      >
                        Продать (+{getSellPriceWithWitBonus(selectedInventoryItem.sellPriceCoins)} М)
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={() =>
                          handleGiftTrinketToNpc(
                            selectedInventoryItem,
                            KEY_COMPANION_NPCS.find((n) => n.npcId === selectedGiftNpcId) ??
                              KEY_COMPANION_NPCS[0]
                          )
                        }
                        className="min-h-[38px] px-3.5 rounded-xl bg-[#EA580C] text-white text-xs font-extrabold cursor-pointer"
                      >
                        Подарить выбранному NPC (+18 М)
                      </button>
                      <button
                        onClick={() => handleSellInventoryItem(selectedInventoryItem)}
                        className="min-h-[38px] px-3 rounded-xl bg-[#382B20] text-xs font-extrabold text-[#F59E0B] cursor-pointer"
                      >
                        Продать (+{getSellPriceWithWitBonus(selectedInventoryItem.sellPriceCoins)} М)
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* NEW: HOME ALCHEMY & FRIENDSHIP WORKSHOP */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="rounded-2xl p-4 bg-[#281F18] border border-[#10B981]/50 space-y-3">
              <div className="flex items-center justify-between">
                <div className="text-sm font-extrabold text-[#10B981]">
                  Алхимический стол у камина (Крафт оберега)
                </div>
                {hasActiveRoadAmulet && (
                  <span className="px-2.5 py-0.5 rounded-lg bg-[#10B981]/20 text-[11px] font-bold text-[#10B981]">
                    Оберег активен ✓
                  </span>
                )}
              </div>
              <p className="text-xs text-[#D5C3AA] leading-relaxed">
                Соедини 1 Целебную траву + 1 Диковинку (или 2 Травы), чтобы сварить{' '}
                <span className="font-bold text-[#FFF8EB]">Оберег Попутного Ветра</span>: 100% защита от неудач, снятие усталости и +
                {ownedShopItemIds.includes('den_alchemy_table') ? '75%' : '50%'} к монетам в следующем походе!
              </p>
              <button
                onClick={handleCraftRoadAmuletInInventory}
                disabled={
                  herbsList.length === 0 ||
                  (herbsList.length < 2 && trinketsList.length === 0)
                }
                className="w-full min-h-[42px] px-4 rounded-xl bg-[#10B981] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
              >
                Смастерить Оберег Попутного Ветра (1 Трава + 1 Находка)
              </button>
            </div>

            <div className="rounded-2xl p-4 bg-[#281F18] border border-[#F59E0B]/50 space-y-2.5">
              <div className="text-sm font-extrabold text-[#F59E0B]">
                Дружба с жителями леса (Выбери получателя подарка):
              </div>
              <div className="grid grid-cols-2 gap-2">
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
                      <div className="text-xs font-extrabold text-[#FFF8EB] truncate">
                        {npc.nameRu}
                      </div>
                      <div className="text-[11px] font-bold text-[#F59E0B] mt-0.5">
                        Дружба: Ур. {lvl} / 5 ♥
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
          </div>

          {/* SECTION 1: ROYAL CLUES CHEST */}
          <div className="rounded-2xl p-4 bg-[#261C15] border border-[#F59E0B]/60 space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <div className="text-xs font-extrabold text-[#F59E0B]">
                  Шкатулка королевских улик и тайн прошлого (Защищено от продажи)
                </div>
                <p className="text-xs text-[#D5C3AA]">
                  Крупицы тайны проклятия Шанталь. Изучай их у камина, чтобы получать +1 к Любопытству!
                </p>
              </div>
              <span className="px-3 py-1 rounded-xl bg-[#15100C] text-xs font-extrabold text-[#F59E0B] tabular-nums">
                Собрано улик: {royalCluesList.length} / 7
              </span>
            </div>

            {royalCluesList.length === 0 ? (
              <div className="text-xs text-[#D5C3AA]">
                Улик пока нет — отправляй Шанталь в вечерние походы или открывай Юбилейные Сундуки Глав.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {royalCluesList.map((clue) => {
                  const isSelected = selectedInventoryItem?.id === clue.id;
                  return (
                    <div
                      key={clue.id}
                      onClick={() => setSelectedInventoryItemId(clue.id)}
                      className="rounded-xl p-3.5 bg-[#1E1712] border flex flex-col justify-between gap-2.5 cursor-pointer transition-colors"
                      style={{
                        borderColor: isSelected ? '#F59E0B' : '#5C4834',
                      }}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center justify-between text-[11px] font-bold text-[#F59E0B]">
                          <span>Королевская улика · {clue.foundAtRegionRu}</span>
                          <span>{isSelected ? 'Осматривается ↑' : 'Осмотреть'}</span>
                        </div>
                        <h3 className="text-sm font-extrabold text-[#FFF8EB]">
                          {clue.titleRu}
                        </h3>
                        <p className="text-xs text-[#E7D8C3] leading-relaxed">
                          {clue.descriptionRu}
                        </p>
                      </div>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedInventoryItemId(clue.id);
                          handleInspectRoyalClue(clue);
                        }}
                        className="min-h-[38px] px-3 rounded-xl bg-[#382B20] hover:bg-[#4D3D2C] text-xs font-extrabold text-[#F59E0B] cursor-pointer"
                      >
                        {clue.inspectedLoreBonusClaimed
                          ? '✓ Изучено (хранится в коллекции)'
                          : 'Изучить у камина (+1 Любопытство ↑)'}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* SECTION 2: USABLE HEALING HERBS */}
          <div className="space-y-2.5">
            <div className="text-sm font-extrabold text-[#10B981]">
              Целебные травы и припасы в сумке ({herbsList.length})
            </div>
            {herbsList.length === 0 ? (
              <div className="rounded-2xl p-4 bg-[#281F18] text-xs text-[#D5C3AA]">
                Целебных трав в сумке сейчас нет. Шанталь часто находит их на дневных тропах или у странствующего купца.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {herbsList.map((herb) => {
                  const witPrice = getSellPriceWithWitBonus(herb.sellPriceCoins);
                  const isSelected = selectedInventoryItem?.id === herb.id;
                  return (
                    <div
                      key={herb.id}
                      onClick={() => setSelectedInventoryItemId(herb.id)}
                      className="rounded-2xl p-4 bg-[#281F18] border flex flex-col justify-between gap-3 cursor-pointer transition-colors"
                      style={{
                        borderColor: isSelected ? '#F59E0B' : '#10B98180',
                      }}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center justify-between text-xs font-bold text-[#10B981]">
                          <span>Целебная находка · {herb.foundAtRegionRu}</span>
                          <span className="text-[11px] text-[#F59E0B]">
                            {isSelected ? 'Осматривается ↑' : 'Описание'}
                          </span>
                        </div>
                        <h3 className="text-base font-extrabold text-[#FFF8EB]">
                          {herb.titleRu}
                        </h3>
                        <p className="text-xs text-[#E7D8C3] leading-relaxed">
                          {herb.descriptionRu}
                        </p>
                      </div>
                      <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#382B20]">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleUseHerbFromInventory(herb);
                          }}
                          className="min-h-[38px] px-3.5 rounded-xl bg-[#10B981] text-white text-xs font-extrabold cursor-pointer"
                        >
                          Дать Шанталь (Снять усталость +15% настр.)
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleSellInventoryItem(herb);
                          }}
                          className="min-h-[38px] px-3 rounded-xl bg-[#382B20] text-xs font-bold text-[#F59E0B] cursor-pointer"
                        >
                          Продать (+{witPrice} М)
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* SECTION 3: FOREST TRINKETS (SELL OR GIFT TO NPC!) */}
          <div className="space-y-2.5">
            <div className="text-sm font-extrabold text-[#FFF8EB]">
              Памятные диковинки на продажу или в подарок другу-NPC ({trinketsList.length})
            </div>
            {trinketsList.length === 0 ? (
              <div className="rounded-2xl p-4 bg-[#281F18] text-xs text-[#D5C3AA]">
                Диковинок пока нет — после следующего похода сумка пополнится!
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                {trinketsList.map((loot) => {
                  const witPrice = getSellPriceWithWitBonus(loot.sellPriceCoins);
                  const isSelected = selectedInventoryItem?.id === loot.id;
                  const targetNpc =
                    KEY_COMPANION_NPCS.find((n) => n.npcId === selectedGiftNpcId) ??
                    KEY_COMPANION_NPCS[0];
                  return (
                    <div
                      key={loot.id}
                      onClick={() => setSelectedInventoryItemId(loot.id)}
                      className="rounded-2xl p-4 bg-[#281F18] border flex flex-col justify-between gap-3 cursor-pointer transition-colors"
                      style={{
                        borderColor: isSelected ? '#F59E0B' : '#4D3D2C',
                      }}
                    >
                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between text-xs font-semibold text-[#F59E0B]">
                          <span>Памятная диковинка · {loot.foundAtRegionRu}</span>
                          <span className="tabular-nums">
                            {isSelected ? 'Осматривается ↑' : loot.foundDateIso}
                          </span>
                        </div>
                        <h3 className="text-base font-extrabold text-[#FFF8EB]">
                          {loot.titleRu}
                        </h3>
                        <p className="text-xs text-[#E7D8C3] leading-relaxed">
                          {loot.descriptionRu}
                        </p>
                      </div>

                      <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#382B20]">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleGiftTrinketToNpc(loot, targetNpc);
                          }}
                          className="min-h-[38px] px-3 rounded-xl bg-[#EA580C] text-white text-xs font-extrabold flex items-center gap-1.5 cursor-pointer"
                        >
                          <Gift className="w-3.5 h-3.5" />
                          <span>Подарить: {targetNpc.nameRu.split(' ')[0]} (+18 М и черта ↑)</span>
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleSellInventoryItem(loot);
                          }}
                          className="min-h-[38px] px-3 rounded-xl bg-[#382B20] hover:bg-[#4D3D2C] text-xs font-extrabold text-[#F59E0B] cursor-pointer"
                        >
                          Продать (+{witPrice} М)
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </section>
      )}

      {/* SUBVIEW 4: TIERED SHOP (TIER 1 -> 2 -> 3) & TRAVELING MERCHANT */}
      {subView === 'shop' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-xl font-extrabold text-[#FFF8EB]">
                Лесная лавка (Прокачка Тир 1→3) и Странствующий торговец
              </h2>
              <p className="text-xs text-[#D5C3AA] mt-0.5">
                Покупка предметов уюта открывает новые иллюстрации Домика (Уровень II и III), а улучшение снаряжения повышает доход в монетах
              </p>
            </div>
            <div className="text-sm font-extrabold text-[#F59E0B] tabular-nums">
              В кошельке: {coins} Монет
            </div>
          </div>

          {lastReactionBanner && (
            <div className="rounded-2xl p-3.5 bg-[#2A2018] border border-[#F59E0B] text-xs font-semibold text-[#FAF0E1]">
              {lastReactionBanner}
            </div>
          )}

          {/* TRAVELING MERCHANT WAGON BANNER */}
          <div className="rounded-2xl overflow-hidden bg-[#261C15] border-2 border-[#F59E0B]/70 shadow-lg">
            <div className="relative h-44 w-full bg-[#15100C]">
              <img
                src={DEN_TIER_IMAGES.merchantChest}
                alt="Фургон странствующего торговца редкостями"
                referrerPolicy="no-referrer"
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#261C15] via-black/35 to-transparent" />
              <div className="absolute bottom-3 left-4 right-4 flex flex-wrap items-end justify-between gap-2">
                <div>
                  <span className="px-2.5 py-0.5 rounded-lg bg-[#F59E0B] text-[#15100C] text-[11px] font-extrabold">
                    Лавка на колёсах · Редкие товары со скидкой
                  </span>
                  <h3 className="text-lg font-extrabold text-[#FFF8EB] mt-1">
                    Фургон странствующего торговца редкостями
                  </h3>
                </div>
                {claimedMerchantOfferIds.length > 0 && (
                  <button
                    onClick={() => setClaimedMerchantOfferIds([])}
                    className="px-3 py-1.5 rounded-xl bg-black/70 border border-[#F59E0B] text-xs font-bold text-[#F59E0B] cursor-pointer"
                  >
                    Обновить привоз фургона
                  </button>
                )}
              </div>
            </div>

            <div className="p-4 grid grid-cols-1 md:grid-cols-3 gap-3">
              {TRAVELING_MERCHANT_OFFERS.map((offer) => {
                const isClaimed = claimedMerchantOfferIds.includes(offer.id);
                const canAfford = coins >= offer.discountPriceCoins;
                return (
                  <div
                    key={offer.id}
                    className="rounded-xl p-3.5 bg-[#1E1712] border border-[#5C4834] flex flex-col justify-between gap-2.5"
                  >
                    <div className="space-y-1">
                      <div className="text-[11px] font-bold text-[#F59E0B]">
                        {offer.subtitleRu}
                      </div>
                      <div className="text-sm font-extrabold text-[#FFF8EB]">
                        {offer.titleRu}
                      </div>
                      <p className="text-xs text-[#D5C3AA] leading-snug">
                        {offer.effectDescriptionRu}
                      </p>
                    </div>
                    <div className="flex items-center justify-between pt-2 border-t border-[#382B20]">
                      <div>
                        <span className="text-xs line-through text-[#8C7A65] mr-1.5">
                          {offer.originalPriceCoins}
                        </span>
                        <span className="text-sm font-extrabold text-[#F59E0B]">
                          {offer.discountPriceCoins} М
                        </span>
                      </div>
                      <button
                        onClick={() => handleBuyTravelingMerchantOffer(offer)}
                        disabled={isClaimed || !canAfford}
                        className="min-h-[36px] px-3 rounded-xl bg-[#EA580C] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
                      >
                        {isClaimed ? 'Куплено ✓' : 'Взять'}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* MAIN SHOP WITH TIER 1 -> 2 -> 3 UPGRADES */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
            {CHANTAL_SHOP_ITEMS.map((item) => {
              const isOwned = ownedShopItemIds.includes(item.id);
              const isConsumable = item.category === 'CONSUMABLE';
              const currentTier = shopItemTiers[item.id] ?? 1;
              const maxTier = item.maxTier ?? 3;
              const upgradeCost = (item.upgradeCostCoins ?? 40) * currentTier;
              const canAffordBuy = coins >= item.priceCoins;
              const canAffordUpgrade = coins >= upgradeCost;

              return (
                <div
                  key={item.id}
                  className="rounded-2xl p-4 bg-[#281F18] border border-[#4D3D2C] flex flex-col justify-between gap-3"
                >
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs font-bold">
                      <span className="text-[#F59E0B]">
                        {item.category === 'APPAREL'
                          ? 'Одежда и снаряжение'
                          : item.category === 'DEN_COMFORT'
                          ? 'Обустройство Домика (меняет иллюстрацию уюта)'
                          : 'Припасы и лечение'}
                      </span>
                      {!isConsumable && isOwned && (
                        <span className="px-2 py-0.5 rounded bg-[#15100C] text-[#10B981]">
                          Тир {currentTier} / {maxTier}
                        </span>
                      )}
                    </div>
                    <h3 className="text-base font-extrabold text-[#FFF8EB]">
                      {item.titleRu}
                    </h3>
                    <p className="text-xs text-[#E7D8C3] leading-relaxed">
                      {item.descriptionRu}
                    </p>
                    <p className="text-xs font-semibold text-[#10B981]">
                      Эффект: {item.gameplayEffectRu}
                    </p>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-[#382B20]">
                    {!isOwned || isConsumable ? (
                      <>
                        <span className="text-sm font-extrabold tabular-nums text-[#F59E0B]">
                          {item.priceCoins} Монет
                        </span>
                        <button
                          onClick={() => handleBuyShopItem(item)}
                          disabled={!canAffordBuy}
                          className="min-h-[42px] px-4 rounded-xl bg-[#EA580C] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
                        >
                          {isConsumable ? 'Купить и применить' : 'Купить (Тир 1)'}
                        </button>
                      </>
                    ) : currentTier < maxTier ? (
                      <>
                        <span className="text-xs font-bold text-[#D5C3AA]">
                          Улучшение до Тира {currentTier + 1}:{' '}
                          <span className="text-[#F59E0B] font-extrabold">{upgradeCost} М</span>
                        </span>
                        <button
                          onClick={() => handleUpgradeShopItemTier(item)}
                          disabled={!canAffordUpgrade}
                          className="min-h-[40px] px-3.5 rounded-xl bg-[#382B20] hover:bg-[#4D3D2C] disabled:opacity-40 border border-[#F59E0B] text-xs font-extrabold text-[#F59E0B] cursor-pointer"
                        >
                          Прокачать Тир {currentTier + 1} (+4 М/пох.)
                        </button>
                      </>
                    ) : (
                      <span className="text-xs font-extrabold text-[#10B981] flex items-center gap-1">
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Макс. Королевский Тир 3/3 активен</span>
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* SUBVIEW 5: THREE-TIER CHRONICLE & LEATHER JOURNAL */}
      {subView === 'journal' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-xl font-extrabold text-[#FFF8EB]">
                Трёхслойная Память Хроник и Кожаный Дневник
              </h2>
              <p className="text-xs text-[#D5C3AA] mt-0.5">
                Нейросеть получает все 3 слоя памяти перед каждым походом — поэтому на 20-м приключении она помнит твои выборы из 1-го, 3-го и 5-го походов!
              </p>
            </div>
            <span className="px-3 py-1 rounded-xl bg-[#281F18] border border-[#10B981]/50 text-xs font-bold text-[#10B981]">
              В летописи: {threeTierMemory.fullChroniclePassports.length} походов с #1
            </span>
          </div>

          {/* Visual Preview of Layer 1 & Layer 2 sent to the AI */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
            <div className="rounded-2xl p-4 bg-[#281F18] border border-[#F59E0B]/40 space-y-2">
              <div className="text-xs font-extrabold text-[#F59E0B]">
                Слой 1 · Сжатая летопись ВСЕХ решений с Похода #1:
              </div>
              <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1 text-[11px] text-[#E7D8C3]">
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
              <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1 text-[11px] text-[#E7D8C3]">
                {threeTierMemory.npcRelationshipDossier.map((line, i) => (
                  <div key={i} className="p-2 rounded-lg bg-[#15100C] border border-[#382B20]">
                    {line}
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Full Stories List (Layer 3 highlights the top 3 full chapters) */}
          <div className="space-y-3.5">
            <div className="text-sm font-extrabold text-[#FFF8EB]">
              Полные тексты глав (Слой 3 передаёт 3 последние главы дословно):
            </div>
            {journalHistory.map((entry, idx) => (
              <div
                key={entry.id}
                className="rounded-2xl p-4 bg-[#281F18] border border-[#4D3D2C] space-y-2"
              >
                <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-bold text-[#F59E0B]">
                  <span>
                    {entry.isFillerEpisode
                      ? 'Побочный эпизод (Филлер)'
                      : `Поход #${entry.journeyNumber} · ${entry.regionTitleRu} (${entry.encounteredNpcRu})`}
                  </span>
                  <span className="tabular-nums">
                    {idx < 3 ? '★ Слой 3 (Полный текст в ИИ) · ' : '✓ Слой 1 (Паспорт в ИИ) · '}
                    {entry.createdAtIso}
                  </span>
                </div>
                <h3 className="text-base font-extrabold text-[#FFF8EB]">
                  {entry.titleRu}
                </h3>
                <p className="text-xs sm:text-sm text-[#E7D8C3] leading-relaxed">
                  {entry.storyTextRu}
                </p>
                {entry.pastEchoNoteRu && (
                  <div className="text-xs font-semibold text-[#F59E0B]">
                    ↻ {entry.pastEchoNoteRu}
                  </div>
                )}
                {entry.selectedChoiceIndex !== undefined &&
                  entry.choices[entry.selectedChoiceIndex] && (
                    <div className="text-xs font-semibold text-[#10B981] pt-1">
                      Твой сюжетный выбор: «{entry.choices[entry.selectedChoiceIndex].labelRu}»
                    </div>
                  )}
                {entry.userCustomResponseText && (
                  <div className="text-xs font-semibold text-[#10B981] pt-1">
                    Твой ответ / поручение: «{entry.userCustomResponseText}»
                  </div>
                )}
              </div>
            ))}
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

          {/* 4. Toggle Trait Change Arrows + Developer Stage Tester */}
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

            <div className="pt-3 border-t border-[#382B20] space-y-2">
              <div className="text-xs font-bold text-[#D5C3AA]">
                Быстрый тест стадий взросления, Домика и ресурсов:
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => {
                    setForceTransformPreview(false);
                    setTotalJourneys(4);
                  }}
                  className="px-3 py-2 rounded-xl bg-[#15100C] text-xs font-bold text-[#FFF8EB] cursor-pointer"
                >
                  Стадия 1 (4 пох. · пред-сундук)
                </button>
                <button
                  onClick={() => {
                    setForceTransformPreview(false);
                    setTotalJourneys(22);
                  }}
                  className="px-3 py-2 rounded-xl bg-[#15100C] text-xs font-bold text-[#FFF8EB] cursor-pointer"
                >
                  Стадия 2 (22 пох.)
                </button>
                <button
                  onClick={() => {
                    setForceTransformPreview(false);
                    setTotalJourneys(65);
                  }}
                  className="px-3 py-2 rounded-xl bg-[#15100C] text-xs font-bold text-[#FFF8EB] cursor-pointer"
                >
                  Стадия 3 (65 пох.)
                </button>
                <button
                  onClick={() => {
                    setForceTransformPreview(true);
                    setTotalJourneys(105);
                  }}
                  className="px-3 py-2 rounded-xl bg-[#382B20] border border-[#F59E0B] text-xs font-bold text-[#F59E0B] cursor-pointer"
                >
                  Акт II · Трансформация (105 пох.)
                </button>
                <button
                  onClick={() => {
                    setEnergy((e) => e + 30);
                    setCoins((c) => c + 120);
                  }}
                  className="px-3 py-2 rounded-xl bg-[#10B981]/20 border border-[#10B981] text-xs font-bold text-[#10B981] cursor-pointer"
                >
                  +30 Энергии и +120 Монет (Тест)
                </button>
              </div>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
