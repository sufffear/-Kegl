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
  Heart,
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
  CHANTAL_SHOP_ITEMS,
  ChantalShopItem,
  ChantalTraits,
  formatTraitDeltasBadge,
  GeneratedStoryEpisode,
  getChantalStageInfo,
  InventoryLootItem,
  pickAutonomousRegionForChantal,
  StoryDifficultyLevel,
  TRAIT_METADATA,
  WORLD_MAP_IMAGE_URL,
  WORLD_MAP_REGIONS,
} from './chantalData';

interface ChantalCompanionViewProps {
  energy: number;
  setEnergy: React.Dispatch<React.SetStateAction<number>>;
  coins: number;
  setCoins: React.Dispatch<React.SetStateAction<number>>;
  currentStreakDays: number;
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
    return Number.isFinite(saved) && saved >= 0 ? saved : 3;
  });
  const [forceTransformPreview, setForceTransformPreview] = useState<boolean>(false);

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

  // 3. Toggleable Trait Change Arrow Toast
  const [showTraitArrowsSetting, setShowTraitArrowsSetting] = useState<boolean>(true);
  const [activeTraitToast, setActiveTraitToast] = useState<string[] | null>(null);
  const [lastReactionBanner, setLastReactionBanner] = useState<string | null>(null);

  // 4. Mood, Condition ("Bad News": 45 Energy Exhaustion & Coin Debt)
  const [moodPct, setMoodPct] = useState<number>(88);
  const [isExhausted45, setIsExhausted45] = useState<boolean>(false);
  const [debtCoins, setDebtCoins] = useState<number>(0);
  const [debtReasonRu, setDebtReasonRu] = useState<string>('');

  // 5. Owned Shop Items & Inventory Loot
  const [ownedShopItemIds, setOwnedShopItemIds] = useState<string[]>(['den_heather_blanket']);
  const [inventoryItems, setInventoryItems] = useState<InventoryLootItem[]>([
    {
      id: 'init_loot_1',
      titleRu: 'Светящаяся шишка из Янтарной дубравы',
      category: 'TRINKET',
      descriptionRu:
        'Тёплая кедровая шишка, которую Шанталь принесла из своего первого похода. Мягко светится в сумерках и пахнет смолой.',
      sellPriceCoins: 20,
      foundAtRegionRu: 'Янтарная дубрава',
      foundDateIso: '2026-11-05',
    },
    {
      id: 'init_loot_2',
      titleRu: 'Пуговица с жилета мельника',
      category: 'TRINKET',
      descriptionRu:
        'Блестящая латунная пуговица с изображением колоса. Шанталь выменяла её у ворона Корвуса на корочку пирога.',
      sellPriceCoins: 15,
      foundAtRegionRu: 'Старая водяная мельница',
      foundDateIso: '2026-11-06',
    },
  ]);

  // 6. Active 60-Min Journey & Unclaimed Story
  const [activeJourney, setActiveJourney] = useState<ActiveJourneyTimerState | null>(null);
  const [nowEpochMs, setNowEpochMs] = useState<number>(() => Date.now());
  const [activeStoryEpisode, setActiveStoryEpisode] = useState<GeneratedStoryEpisode | null>(null);
  const [isGeneratingStory, setIsGeneratingStory] = useState<boolean>(false);
  const [customFillerInput, setCustomFillerInput] = useState<string>('');

  // 7. Journal History
  const [journalHistory, setJournalHistory] = useState<GeneratedStoryEpisode[]>([]);
  const [subView, setSubView] = useState<ChantalSubView>('den');

  // 8. AI Storyteller Config & Custom Uploaded File
  const [aiProvider, setAiProvider] = useState<AiProviderId>('grok');
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

  // Calculate required Energy for the next journey (30 normal, or 45 when exhausted minus home comfort reduction)
  const requiredJourneyEnergy = useMemo(() => {
    if (!isExhausted45) return 30;
    const comfortDiscount = CHANTAL_SHOP_ITEMS.filter(
      (item) => ownedShopItemIds.includes(item.id) && item.reducesExhaustionCostBy
    ).reduce((acc, item) => acc + (item.reducesExhaustionCostBy ?? 0), 0);
    return Math.max(30, 45 - comfortDiscount);
  }, [isExhausted45, ownedShopItemIds]);

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

  // Show temporary arrow badge when traits change
  const triggerTraitToast = (deltas: Partial<ChantalTraits>) => {
    if (!showTraitArrowsSetting) return;
    const formatted = formatTraitDeltasBadge(deltas);
    if (formatted.length === 0) return;
    setActiveTraitToast(formatted);
    window.setTimeout(() => {
      setActiveTraitToast(null);
    }, 4500);
  };

  // Start autonomous journey (1 hour timer)
  const handleStartAutonomousJourney = (isFiller: boolean = false, fillerText?: string) => {
    if (energy < requiredJourneyEnergy) return;
    if (soundEnabled) onPlayTone('hold');

    setEnergy((prev) => Math.max(0, prev - requiredJourneyEnergy));
    setIsExhausted45(false);
    setLastReactionBanner(null);

    // Chantal autonomously picks her destination on the map!
    const lastRegId = journalHistory[0]?.regionId;
    const chosenRegion = pickAutonomousRegionForChantal(totalJourneys, lastRegId);

    setActiveJourney({
      startedAtEpochMs: Date.now(),
      durationMs: 60 * 60 * 1000, // 60 minutes real-time
      regionId: chosenRegion.id,
      regionTitleRu: chosenRegion.titleRu,
      encounteredNpcRu: chosenRegion.residentNpcRu,
      dailyArcStep: timeOfDayInfo.dailyArcStep,
      isFillerEpisode: isFiller,
      customFillerPrompt: fillerText,
    });
    setNowEpochMs(Date.now());
  };

  // Generate story when Chantal returns from her journey (via /api/chantal/generate-story or fallback)
  const handleFinishJourneyAndReadStory = async (journeyToResolve: ActiveJourneyTimerState) => {
    setIsGeneratingStory(true);
    const regionObj =
      WORLD_MAP_REGIONS.find((r) => r.id === journeyToResolve.regionId) ?? WORLD_MAP_REGIONS[0];

    try {
      const response = await fetch('/api/chantal/generate-story', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          provider: aiProvider,
          difficulty: storyDifficulty,
          stageNumber: stageInfo.stageNumber,
          isTransformed: stageInfo.isTransformed,
          totalJourneys: totalJourneys + 1,
          dailyArcStep: journeyToResolve.dailyArcStep,
          timeOfDayRu: timeOfDayInfo.labelRu,
          regionTitleRu: regionObj.titleRu,
          regionLoreRu: regionObj.loreRu,
          encounteredNpcRu: regionObj.residentNpcRu,
          equippedItemsRu: equippedApparelItems.map((i) => i.titleRu),
          denItemsRu: ownedDenItems.map((i) => i.titleRu),
          traits,
          isFillerEpisode: journeyToResolve.isFillerEpisode,
          customFillerPrompt: journeyToResolve.customFillerPrompt,
          customLoreText: customLoreContent,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      const raw = data.story;
      const nowIso = new Date().toISOString().slice(0, 10);

      const episode: GeneratedStoryEpisode = {
        id: `ep_${Date.now()}`,
        createdAtIso: nowIso,
        titleRu: raw.titleRu || `Путешествие в «${regionObj.titleRu}»`,
        storyTextRu: raw.storyTextRu,
        chantalMapNoteRu:
          raw.chantalMapNoteRu ||
          `Шанталь сама выбрала тропу в «${regionObj.titleRu}» и встретила там персонажа ${regionObj.residentNpcRu}.`,
        regionId: regionObj.id,
        regionTitleRu: regionObj.titleRu,
        encounteredNpcRu: regionObj.residentNpcRu,
        dailyArcStep: journeyToResolve.dailyArcStep,
        isFillerEpisode: journeyToResolve.isFillerEpisode,
        customFillerPrompt: journeyToResolve.customFillerPrompt,
        lootItem: {
          id: `loot_${Date.now()}`,
          titleRu: raw.lootItem?.titleRu || 'Диковинка с лесной тропы',
          category:
            raw.lootItem?.category === 'HERB' || raw.lootItem?.category === 'ROYAL_CLUE'
              ? raw.lootItem.category
              : 'TRINKET',
          descriptionRu:
            raw.lootItem?.descriptionRu ||
            'Необычная вещь, которую Шанталь бережно принесла в сумке из своего приключения.',
          sellPriceCoins: Number(raw.lootItem?.sellPriceCoins) || 20,
          foundAtRegionRu: regionObj.titleRu,
          foundDateIso: nowIso,
        },
        outcomeType:
          raw.outcomeType === 'BAD_DEBT' || raw.outcomeType === 'BAD_EXHAUSTION'
            ? raw.outcomeType
            : 'GOOD_REWARD',
        coinsEarned: Math.round(
          (Number(raw.coinsEarned) || 28) * streakMultiplierInfo.multiplier
        ),
        badNewsTitleRu: raw.badNewsTitleRu || '',
        debtAmountCoins: Number(raw.debtAmountCoins) || 0,
        choices: Array.isArray(raw.choices) && raw.choices.length >= 3 ? raw.choices.slice(0, 3) : [],
        aiSourceLabel: data.source === 'grok-xai' ? 'Grok (xAI)' : 'Нейросеть ИИ',
      };

      setActiveStoryEpisode(episode);
      setActiveJourney(null);
      if (soundEnabled) onPlayTone('complete');
    } catch {
      // Seamless fallback if offline or API key not yet configured
      const fallback = buildFallbackStoryEpisode({
        region: regionObj,
        dailyArcStep: journeyToResolve.dailyArcStep,
        isFillerEpisode: journeyToResolve.isFillerEpisode,
        customFillerPrompt: journeyToResolve.customFillerPrompt,
        isTransformed: stageInfo.isTransformed,
        totalJourneys: totalJourneys + 1,
      });
      fallback.coinsEarned = Math.round(fallback.coinsEarned * streakMultiplierInfo.multiplier);
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

    // 1. Add loot item to inventory
    setInventoryItems((prev) => [activeStoryEpisode.lootItem, ...prev]);

    // 2. Apply Coins & Bad News outcomes
    const shelfBonus = ownedShopItemIds.includes('den_curiosity_shelf') ? 5 : 0;
    const totalCoinsAdded = activeStoryEpisode.coinsEarned + shelfBonus;
    setCoins((prev) => prev + totalCoinsAdded);

    if (activeStoryEpisode.outcomeType === 'BAD_DEBT' && activeStoryEpisode.debtAmountCoins > 0) {
      setDebtCoins((prev) => prev + activeStoryEpisode.debtAmountCoins);
      setDebtReasonRu(
        activeStoryEpisode.badNewsTitleRu || 'Непредвиденный долг на ярмарке'
      );
    } else if (activeStoryEpisode.outcomeType === 'BAD_EXHAUSTION') {
      setIsExhausted45(true);
    }

    // 3. Only canonical episodes modify traits and advance canonical journey count!
    if (!activeStoryEpisode.isFillerEpisode) {
      setTraits((prev) => ({
        curiosity: Math.max(1, prev.curiosity + (chosen.traitDeltas.curiosity || 0)),
        tenderness: Math.max(1, prev.tenderness + (chosen.traitDeltas.tenderness || 0)),
        playfulness: Math.max(1, prev.playfulness + (chosen.traitDeltas.playfulness || 0)),
        romanticism: Math.max(1, prev.romanticism + (chosen.traitDeltas.romanticism || 0)),
        courage: Math.max(1, prev.courage + (chosen.traitDeltas.courage || 0)),
        wit: Math.max(1, prev.wit + (chosen.traitDeltas.wit || 0)),
      }));
      triggerTraitToast(chosen.traitDeltas);
      setTotalJourneys((prev) => prev + 1);
    }

    // 4. Save to journal & close active scroll
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

    // Add loot & coins
    setInventoryItems((prev) => [activeStoryEpisode.lootItem, ...prev]);
    setCoins((prev) => prev + activeStoryEpisode.coinsEarned);

    // If the player wrote a custom text to work off a debt, clear the debt!
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

  // Buy or use item in Shop
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
      setMoodPct((m) => Math.min(100, m + 8));
    }
  };

  // Sell item from Inventory for Coins
  const handleSellInventoryItem = (loot: InventoryLootItem) => {
    if (soundEnabled) onPlayTone('complete');
    setCoins((prev) => prev + loot.sellPriceCoins);
    setInventoryItems((prev) => prev.filter((i) => i.id !== loot.id));
  };

  // PROLOGUE SCENE (When user first meets the injured red fox kit)
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

  // Compute remaining journey time if active
  const elapsedMs = activeJourney ? Math.max(0, nowEpochMs - activeJourney.startedAtEpochMs) : 0;
  const remainingMs = activeJourney ? Math.max(0, activeJourney.durationMs - elapsedMs) : 0;
  const isJourneyFinished = activeJourney !== null && remainingMs <= 0;
  const remainingMinutes = Math.floor(remainingMs / 60000);
  const remainingSeconds = Math.floor((remainingMs % 60000) / 1000);

  // Dynamic 15-minute road notes during the 1-hour expedition
  const activeRoadNoteRu = useMemo(() => {
    if (!activeJourney) return '';
    const minsElapsed = Math.floor(elapsedMs / 60000);
    if (minsElapsed < 15) {
      return `0–15 мин: Шанталь бодро выбежала за калитку и сама свернула по карте в сторону «${activeJourney.regionTitleRu}».`;
    }
    if (minsElapsed < 30) {
      return `15–30 мин: Миновала старый каменный мост и заинтересовалась необычными следами у ручья.`;
    }
    if (minsElapsed < 45) {
      return `30–45 мин: Встретила персонажа ${activeJourney.encounteredNpcRu} — кажется, там завязалась целая история!`;
    }
    return `45–60 мин: Сумка полна новостей и находок — Шанталь уже бежит по обратной тропинке к дому!`;
  }, [activeJourney, elapsedMs]);

  return (
    <div className="max-w-5xl mx-auto space-y-5 text-[#F6EEDF]">
      {/* Floating Trait Change Arrow Toast (Can be toggled off in settings) */}
      {activeTraitToast && (
        <div className="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-5 py-3 rounded-2xl bg-[#271E17] border-2 border-[#F59E0B] text-[#FFF8EB] shadow-2xl flex items-center gap-2 animate-bounce">
          <Sparkles className="w-4 h-4 text-[#F59E0B] shrink-0" />
          <span className="text-xs sm:text-sm font-extrabold tracking-tight">
            {activeTraitToast.join(' · ')}
          </span>
        </div>
      )}

      {/* Top Currency & Atmosphere Bar (Hobbit Warm Oak & Brass Theme) */}
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

          {/* Two Currencies: Energy & Coins */}
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

        {/* Sub-navigation Tabs inside Chantal World */}
        <div className="grid grid-cols-3 sm:grid-cols-6 gap-1.5 mt-4 p-1.5 rounded-2xl bg-[#15100C]">
          {[
            { id: 'den', label: 'Домик', icon: Heart },
            { id: 'map', label: 'Карта троп', icon: Compass },
            { id: 'inventory', label: `Сумка (${inventoryItems.length})`, icon: Backpack },
            { id: 'shop', label: 'Лавка', icon: ShoppingBag },
            { id: 'journal', label: `Дневник (${journalHistory.length})`, icon: BookOpen },
            { id: 'ai_settings', label: 'ИИ и Лор', icon: FileText },
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

      {/* ACTIVE STORY READING & RESPONSE MODAL / VIEW */}
      {activeStoryEpisode && (
        <section className="rounded-[28px] p-5 sm:p-6 bg-[#2A2018] border-2 border-[#F59E0B] shadow-xl space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#4D3D2C] pb-3.5">
            <div>
              <div className="text-xs font-semibold text-[#F59E0B]">
                {activeStoryEpisode.isFillerEpisode
                  ? 'Побочный эпизод (Филлер — не меняет характер и основной сюжет)'
                  : `Основной сюжет · Арка дня ${activeStoryEpisode.dailyArcStep}/3 · ${activeStoryEpisode.regionTitleRu}`}
              </div>
              <h2 className="text-xl sm:text-2xl font-extrabold text-[#FFF8EB] mt-0.5">
                {activeStoryEpisode.titleRu}
              </h2>
            </div>
            <span className="text-xs font-semibold text-[#D5C3AA]">
              Рассказчик: {activeStoryEpisode.aiSourceLabel || 'ИИ'}
            </span>
          </div>

          {/* Where Chantal went on the map */}
          <div className="rounded-2xl p-3.5 bg-[#1E1712] border border-[#4D3D2C] flex items-start gap-3">
            <MapPin className="w-5 h-5 text-[#F59E0B] shrink-0 mt-0.5" />
            <div className="text-xs sm:text-sm text-[#E7D8C3]">
              <span className="font-bold text-[#FFF8EB]">Маршрут Шанталь на карте: </span>
              {activeStoryEpisode.chantalMapNoteRu}
            </div>
          </div>

          {/* Parchment Story Body (~10 sentences) */}
          <div className="rounded-2xl p-4 sm:p-5 bg-[#33271D] border border-[#5C4834] space-y-3 text-sm sm:text-[15px] leading-relaxed text-[#FAF0E1]">
            {activeStoryEpisode.storyTextRu.split('\n').map((para, i) =>
              para.trim() ? <p key={i}>{para}</p> : null
            )}
          </div>

          {/* Loot Item Brought Back + Coins / Bad News Banner */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {/* Brought Item */}
            <div className="rounded-2xl p-4 bg-[#1E1712] border border-[#10B981]/50 space-y-1.5">
              <div className="text-xs font-bold text-[#10B981]">
                Принесена находка в инвентарь · Оценка {activeStoryEpisode.lootItem.sellPriceCoins} монет
              </div>
              <div className="text-base font-extrabold text-[#FFF8EB]">
                {activeStoryEpisode.lootItem.titleRu}
              </div>
              <p className="text-xs text-[#D5C3AA] leading-relaxed">
                {activeStoryEpisode.lootItem.descriptionRu}
              </p>
            </div>

            {/* Outcome: Coins + Optional Bad News */}
            <div
              className="rounded-2xl p-4 bg-[#1E1712] border space-y-1.5"
              style={{
                borderColor:
                  activeStoryEpisode.outcomeType === 'GOOD_REWARD' ? '#F59E0B88' : '#EF444499',
              }}
            >
              <div className="text-xs font-bold text-[#F59E0B]">
                Добыча из похода: +{activeStoryEpisode.coinsEarned} Монет
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

          {/* 3 Canonical Response Buttons (Change traits + advance main story) */}
          <div className="space-y-2.5">
            <div className="text-xs font-bold text-[#D5C3AA]">
              Выбери сюжетный ответ (двигает основной сюжет и влияет на характер Шанталь):
            </div>
            <div className="grid grid-cols-1 gap-2.5">
              {activeStoryEpisode.choices.map((choice, idx) => {
                const previewArrows = formatTraitDeltasBadge(choice.traitDeltas);
                return (
                  <button
                    key={idx}
                    onClick={() => handleSelectCanonicalChoice(idx)}
                    className="w-full min-h-[52px] p-3.5 rounded-2xl bg-[#1E1712] hover:bg-[#382B20] border border-[#5C4834] text-left flex flex-col sm:flex-row sm:items-center justify-between gap-2 cursor-pointer transition-colors"
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

          {/* Free Text Input (Filler side episode / does NOT affect traits or main plot) */}
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

      {/* SUBVIEW 1: DEN & CHARACTER CARD */}
      {subView === 'den' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
          {/* Left Column: Chantal Portrait, Stage, Visual Gear & 6 Traits */}
          <div className="lg:col-span-6 space-y-5">
            <div className="rounded-[26px] overflow-hidden bg-[#1E1712] border border-[#4D3D2C] shadow-md">
              <div className="relative aspect-square w-full max-h-[380px] overflow-hidden bg-[#120D0A]">
                <img
                  src={stageInfo.portraitUrl}
                  alt={stageInfo.titleRu}
                  referrerPolicy="no-referrer"
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-[#1E1712] via-transparent to-black/30" />

                {/* Top Status Overlay */}
                <div className="absolute top-3.5 left-3.5 right-3.5 flex items-center justify-between gap-2">
                  <span className="px-3 py-1 rounded-xl bg-black/65 backdrop-blur-md text-xs font-bold text-[#FFF8EB]">
                    Походов: {totalJourneys} · Настроение {moodPct}%
                  </span>
                  {activeJourney && (
                    <span className="px-3 py-1 rounded-xl bg-[#EA580C] text-xs font-extrabold text-white">
                      В путешествии ({activeJourney.regionTitleRu})
                    </span>
                  )}
                </div>

                {/* Bottom Stage Info Overlay */}
                <div className="absolute bottom-3.5 left-4 right-4">
                  <div className="text-xs font-bold text-[#F59E0B]">
                    {stageInfo.nextStageHintRu}
                  </div>
                  <p className="text-xs text-[#E7D8C3] mt-0.5 leading-snug">
                    {stageInfo.subtitleRu}
                  </p>
                </div>
              </div>

              {/* Visual Equipped Gear & Den Comforts Display */}
              <div className="p-4 sm:p-5 space-y-4">
                {lastReactionBanner && (
                  <div className="rounded-2xl p-3 bg-[#2A2018] border border-[#F59E0B]/50 text-xs text-[#FAF0E1]">
                    {lastReactionBanner}
                  </div>
                )}

                <div>
                  <div className="text-xs font-bold text-[#D5C3AA] mb-2">
                    Визуальная экипировка и уют в домике:
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {equippedApparelItems.map((gear) => (
                      <div
                        key={gear.id}
                        className="rounded-xl px-3 py-2 bg-[#281F18] border border-[#F59E0B]/40 text-xs font-semibold text-[#FFF8EB]"
                      >
                        ✦ {gear.visualSlotLabelRu}
                      </div>
                    ))}
                    {ownedDenItems.map((den) => (
                      <div
                        key={den.id}
                        className="rounded-xl px-3 py-2 bg-[#281F18] border border-[#10B981]/40 text-xs font-semibold text-[#E7D8C3]"
                      >
                        ⌂ {den.visualSlotLabelRu}
                      </div>
                    ))}
                  </div>
                </div>

                {/* 6 Character Traits Bars */}
                <div className="space-y-2.5 pt-2 border-t border-[#382B20]">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-extrabold text-[#FFF8EB]">
                      Характер Шанталь (6 параметров)
                    </span>
                    <span className="text-xs text-[#D5C3AA]">
                      Меняется только от сюжетных кнопок
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2.5">
                    {TRAIT_METADATA.map((meta) => {
                      const val = traits[meta.key];
                      const pct = Math.min(100, Math.max(10, val * 3));
                      return (
                        <div
                          key={meta.key}
                          className="rounded-xl p-2.5 bg-[#281F18] space-y-1.5"
                        >
                          <div className="flex items-center justify-between text-xs font-bold">
                            <span className="text-[#FFF8EB]">{meta.labelRu}</span>
                            <span
                              className="tabular-nums font-extrabold"
                              style={{ color: meta.colorHex }}
                            >
                              {val}
                            </span>
                          </div>
                          <div className="w-full h-1.5 rounded-full bg-[#15100C] overflow-hidden">
                            <div
                              className="h-full rounded-full transition-all duration-300"
                              style={{
                                width: `${pct}%`,
                                backgroundColor: meta.colorHex,
                              }}
                            />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column: Expedition Dispatch, Bad News Management & Filler Task */}
          <div className="lg:col-span-6 space-y-5">
            {/* Bad News Alert Card (If Exhausted 45 Energy or In Coin Debt) */}
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
                      Ты можешь накопить {requiredJourneyEnergy} Энергии упражнениями Кегеля ИЛИ сразу согреть Шанталь медово-травяным бальзамом за 20 Монет.
                    </p>
                    <button
                      onClick={() => {
                        const balm = CHANTAL_SHOP_ITEMS.find(
                          (i) => i.id === 'supply_herbal_balm'
                        );
                        if (balm) handleBuyShopItem(balm);
                      }}
                      disabled={coins < 20}
                      className="min-h-[42px] px-4 rounded-xl bg-[#10B981] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
                    >
                      Вылечить бальзамом за 20 Монет (вернуть цену 30 Энергии)
                    </button>
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

            {/* Journey Control Card (Send on 1-Hour Expedition or Track Active Timer) */}
            <div className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-extrabold text-[#FFF8EB]">
                    {activeJourney ? 'Шанталь сейчас в походе' : 'Отправить в путешествие'}
                  </h2>
                  <p className="text-xs text-[#D5C3AA] mt-0.5">
                    {activeJourney
                      ? `Она сама выбрала маршрут: «${activeJourney.regionTitleRu}»`
                      : 'Шанталь сама выберет тропу на карте и принесёт историю + находку'}
                  </p>
                </div>
                <Compass className="w-6 h-6 text-[#F59E0B]" />
              </div>

              {activeJourney ? (
                <div className="space-y-4">
                  {/* Live 60-Min Timer Display */}
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
                      onClick={() => handleFinishJourneyAndReadStory(activeJourney)}
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
                  {/* Energy Progress towards requiredJourneyEnergy */}
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

                  {/* Main Canonical Expedition Button */}
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
                      onClick={onGoToWorkout}
                      className="min-h-[52px] rounded-2xl bg-[#281F18] border border-[#10B981]/50 text-[#10B981] font-extrabold text-sm flex items-center justify-center gap-2 cursor-pointer"
                    >
                      <Flame className="w-4 h-4" />
                      <span>К упражнениям (+10 Эн.)</span>
                    </button>
                  </div>

                  {/* Direct Filler Errand Box (Send Chantal with custom text without affecting main story) */}
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
                Шанталь сама выбирает, куда отправиться, а ты можешь наблюдать за её маршрутом и встречами
              </p>
            </div>
            <span className="text-xs font-bold text-[#F59E0B]">
              Открыто локаций: {WORLD_MAP_REGIONS.filter((r) => totalJourneys >= r.minJourneysRequired).length} из {WORLD_MAP_REGIONS.length}
            </span>
          </div>

          {/* Interactive Map Canvas */}
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

          {/* Region Cards & Encounters */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {WORLD_MAP_REGIONS.map((reg) => {
              const isUnlocked = totalJourneys >= reg.minJourneysRequired;
              return (
                <div
                  key={reg.id}
                  className="rounded-2xl p-4 bg-[#281F18] border border-[#4D3D2C] space-y-1.5"
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
                  <div className="text-xs font-semibold text-[#10B981] pt-1">
                    Встречи на тропе: {reg.residentNpcRu} ({reg.npcRoleRu})
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* SUBVIEW 3: INVENTORY OF CURIOSITIES WITH LORE */}
      {subView === 'inventory' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-extrabold text-[#FFF8EB]">
                Сумка находок и лесных диковинок
              </h2>
              <p className="text-xs text-[#D5C3AA] mt-0.5">
                Предметы с историей, которые Шанталь приносит из своих путешествий
              </p>
            </div>
            <span className="text-xs font-bold text-[#F59E0B]">
              Всего предметов: {inventoryItems.length}
            </span>
          </div>

          {inventoryItems.length === 0 ? (
            <div className="rounded-2xl p-6 bg-[#281F18] text-center text-sm text-[#D5C3AA]">
              В сумке пока пусто. Отправь Шанталь в путешествие, и она обязательно принесёт интересную находку!
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
              {inventoryItems.map((loot) => (
                <div
                  key={loot.id}
                  className="rounded-2xl p-4 bg-[#281F18] border border-[#4D3D2C] flex flex-col justify-between gap-3"
                >
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs font-semibold text-[#F59E0B]">
                      <span>
                        {loot.category === 'ROYAL_CLUE'
                          ? 'Загадочная старинная улика'
                          : loot.category === 'HERB'
                          ? 'Лесная целебная находка'
                          : 'Памятная диковинка'}{' '}
                        · {loot.foundAtRegionRu}
                      </span>
                      <span className="tabular-nums">{loot.foundDateIso}</span>
                    </div>
                    <h3 className="text-base font-extrabold text-[#FFF8EB]">
                      {loot.titleRu}
                    </h3>
                    <p className="text-xs text-[#E7D8C3] leading-relaxed">
                      {loot.descriptionRu}
                    </p>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-[#382B20]">
                    <span className="text-xs font-bold text-[#D5C3AA]">
                      Цена у скупщика: {loot.sellPriceCoins} Монет
                    </span>
                    <button
                      onClick={() => handleSellInventoryItem(loot)}
                      className="min-h-[38px] px-3.5 rounded-xl bg-[#382B20] hover:bg-[#4D3D2C] text-xs font-extrabold text-[#F59E0B] cursor-pointer"
                    >
                      Продать за +{loot.sellPriceCoins} Монет
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {/* SUBVIEW 4: 3-CATEGORY SHOP */}
      {subView === 'shop' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-xl font-extrabold text-[#FFF8EB]">
                Лесная лавка редкостей и уюта
              </h2>
              <p className="text-xs text-[#D5C3AA] mt-0.5">
                Покупки отображаются на карточке Шанталь, защищают от передряг и снижают цену восстановления
              </p>
            </div>
            <div className="text-sm font-extrabold text-[#F59E0B] tabular-nums">
              В кошельке: {coins} Монет
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
            {CHANTAL_SHOP_ITEMS.map((item) => {
              const isOwned = ownedShopItemIds.includes(item.id);
              const isConsumable = item.category === 'CONSUMABLE';
              const canAfford = coins >= item.priceCoins;

              return (
                <div
                  key={item.id}
                  className="rounded-2xl p-4 bg-[#281F18] border border-[#4D3D2C] flex flex-col justify-between gap-3"
                >
                  <div className="space-y-1.5">
                    <div className="text-xs font-bold text-[#F59E0B]">
                      {item.category === 'APPAREL'
                        ? 'Одежда и снаряжение'
                        : item.category === 'DEN_COMFORT'
                        ? 'Обустройство домика'
                        : 'Припасы и лечение'}
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
                    <span className="text-sm font-extrabold tabular-nums text-[#F59E0B]">
                      {item.priceCoins} Монет
                    </span>

                    {isOwned && !isConsumable ? (
                      <span className="text-xs font-extrabold text-[#10B981] flex items-center gap-1">
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Куплено и активно</span>
                      </span>
                    ) : (
                      <button
                        onClick={() => handleBuyShopItem(item)}
                        disabled={!canAfford}
                        className="min-h-[42px] px-4 rounded-xl bg-[#EA580C] disabled:opacity-40 text-white text-xs font-extrabold cursor-pointer"
                      >
                        {isConsumable ? 'Купить и применить' : 'Купить в лавке'}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* SUBVIEW 5: LEATHER JOURNAL OF STORIES */}
      {subView === 'journal' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-4">
          <div>
            <h2 className="text-xl font-extrabold text-[#FFF8EB]">
              Кожаный дневник странствий
            </h2>
            <p className="text-xs text-[#D5C3AA] mt-0.5">
              Все пережитые истории основного сюжета и одноразовые филлер-вылазки
            </p>
          </div>

          {journalHistory.length === 0 ? (
            <div className="rounded-2xl p-6 bg-[#281F18] text-center text-sm text-[#D5C3AA]">
              Записей пока нет. Заверши первое путешествие, и история появится в дневнике!
            </div>
          ) : (
            <div className="space-y-3.5">
              {journalHistory.map((entry) => (
                <div
                  key={entry.id}
                  className="rounded-2xl p-4 bg-[#281F18] border border-[#4D3D2C] space-y-2"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-bold text-[#F59E0B]">
                    <span>
                      {entry.isFillerEpisode
                        ? 'Побочный эпизод (Филлер)'
                        : `Основной сюжет · ${entry.regionTitleRu}`}
                    </span>
                    <span className="tabular-nums">{entry.createdAtIso}</span>
                  </div>
                  <h3 className="text-base font-extrabold text-[#FFF8EB]">
                    {entry.titleRu}
                  </h3>
                  <p className="text-xs sm:text-sm text-[#E7D8C3] leading-relaxed">
                    {entry.storyTextRu}
                  </p>
                  {entry.userCustomResponseText && (
                    <div className="text-xs font-semibold text-[#10B981] pt-1">
                      Твой ответ / поручение: «{entry.userCustomResponseText}»
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {/* SUBVIEW 6: AI STORYTELLER SETTINGS, DIFFICULTY & CUSTOM LORE FILE UPLOAD */}
      {subView === 'ai_settings' && (
        <section className="rounded-[26px] p-5 bg-[#1E1712] border border-[#4D3D2C] space-y-5">
          <div>
            <h2 className="text-xl font-extrabold text-[#FFF8EB]">
              Настройки ИИ-рассказчика, сложности и файла лора
            </h2>
            <p className="text-xs text-[#D5C3AA] mt-0.5">
              Управление генератором историй (Grok / Gemini), уровнем драматизма и загрузка твоего файла персонализации
            </p>
          </div>

          {/* 1. AI Provider Selection (Grok vs Built-in Gemini) */}
          <div className="rounded-2xl p-4 bg-[#281F18] space-y-3">
            <div className="text-sm font-extrabold text-[#FFF8EB]">
              Провайдер нейросети для генерации историй:
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              <button
                onClick={() => setAiProvider('grok')}
                className="min-h-[48px] p-3 rounded-xl border text-left cursor-pointer"
                style={{
                  backgroundColor: aiProvider === 'grok' ? '#382B20' : '#15100C',
                  borderColor: aiProvider === 'grok' ? '#F59E0B' : '#4D3D2C',
                }}
              >
                <div className="text-sm font-extrabold text-[#FFF8EB]">
                  Grok (xAI API) {aiProvider === 'grok' ? '✓' : ''}
                </div>
                <div className="text-xs text-[#D5C3AA] mt-0.5">
                  Острый юмор, живая драма в духе «Хоббита» и «Ведьмака»
                </div>
              </button>

              <button
                onClick={() => setAiProvider('gemini')}
                className="min-h-[48px] p-3 rounded-xl border text-left cursor-pointer"
                style={{
                  backgroundColor: aiProvider === 'gemini' ? '#382B20' : '#15100C',
                  borderColor: aiProvider === 'gemini' ? '#F59E0B' : '#4D3D2C',
                }}
              >
                <div className="text-sm font-extrabold text-[#FFF8EB]">
                  Gemini 3.8 Flash {aiProvider === 'gemini' ? '✓' : ''}
                </div>
                <div className="text-xs text-[#D5C3AA] mt-0.5">
                  Быстрая генерация со строгой JSON-схемой предметов и черт
                </div>
              </button>
            </div>
          </div>

          {/* 2. Story Difficulty Level (4 levels) */}
          <div className="rounded-2xl p-4 bg-[#281F18] space-y-3">
            <div className="text-sm font-extrabold text-[#FFF8EB]">
              Сложность и интенсивность сюжета:
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
                  Файл персонализации для нейросети (.txt, .md, .json)
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

            {/* Quick Stage Switcher for Testing all 4 Stages */}
            <div className="pt-3 border-t border-[#382B20] space-y-2">
              <div className="text-xs font-bold text-[#D5C3AA]">
                Быстрый тест стадий взросления и ресурсов (для проверки интерфейса):
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => {
                    setForceTransformPreview(false);
                    setTotalJourneys(3);
                  }}
                  className="px-3 py-2 rounded-xl bg-[#15100C] text-xs font-bold text-[#FFF8EB] cursor-pointer"
                >
                  Стадия 1 (3 пох.)
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
                    setCoins((c) => c + 50);
                  }}
                  className="px-3 py-2 rounded-xl bg-[#10B981]/20 border border-[#10B981] text-xs font-bold text-[#10B981] cursor-pointer"
                >
                  +30 Энергии и +50 Монет (Тест)
                </button>
              </div>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
