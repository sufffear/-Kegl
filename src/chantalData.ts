export type StoryDifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD' | 'SUPER_HARD';
export type AiProviderId = 'gemini' | 'openrouter' | 'grok';

export interface OpenRouterModelOption {
  id: string;
  titleRu: string;
  subtitleRu: string;
  badgeRu: string;
}

export const OPENROUTER_MODELS_CATALOG: OpenRouterModelOption[] = [
  {
    id: 'google/gemini-2.5-flash',
    titleRu: 'Gemini 2.5 Flash (OpenRouter)',
    subtitleRu: 'Высокая скорость, отличная память длинного контекста и чёткий JSON',
    badgeRu: 'Быстрый и точный',
  },
  {
    id: 'x-ai/grok-3-mini',
    titleRu: 'Grok 3 Mini (xAI через OpenRouter)',
    subtitleRu: 'Живой юмор в духе «Хоббита», хитрые диалоги и яркая драматургия',
    badgeRu: 'Юмор и харизма',
  },
  {
    id: 'anthropic/claude-3.7-sonnet',
    titleRu: 'Claude 3.7 Sonnet (Anthropic)',
    subtitleRu: 'Эталонная книжная проза, тонкие эмоции и глубокая атмосфера «Ведьмака»',
    badgeRu: 'Книжный шедевр',
  },
  {
    id: 'deepseek/deepseek-chat',
    titleRu: 'DeepSeek V3 (DeepSeek Chat)',
    subtitleRu: 'Отлично помнит старые развилки (3-й и 5-й походы) и строит детективные интриги',
    badgeRu: 'Мастер сюжета',
  },
  {
    id: 'meta-llama/llama-3.3-70b-instruct:free',
    titleRu: 'Llama 3.3 70B Instruct (:free)',
    subtitleRu: 'Бесплатная ролевая модель в каталоге OpenRouter для бесконечных приключений',
    badgeRu: 'Бесплатно в OR',
  },
];

export interface ChantalTraits {
  curiosity: number;    // Любопытство
  tenderness: number;   // Нежность
  playfulness: number;  // Игривость
  romanticism: number;  // Романтичность
  courage: number;      // Отвага
  wit: number;          // Остроумие
}

export const TRAIT_METADATA: Array<{
  key: keyof ChantalTraits;
  labelRu: string;
  subtitleRu: string;
  colorHex: string;
}> = [
  { key: 'curiosity', labelRu: 'Любопытство', subtitleRu: 'Тяга к загадкам и тропам', colorHex: '#F59E0B' },
  { key: 'tenderness', labelRu: 'Нежность', subtitleRu: 'Теплота и привязанность', colorHex: '#EC4899' },
  { key: 'playfulness', labelRu: 'Игривость', subtitleRu: 'Юмор и озорной нрав', colorHex: '#FB923C' },
  { key: 'romanticism', labelRu: 'Романтичность', subtitleRu: 'Мечтательность и чуткость', colorHex: '#A855F7' },
  { key: 'courage', labelRu: 'Отвага', subtitleRu: 'Решительность в пути', colorHex: '#10B981' },
  { key: 'wit', labelRu: 'Остроумие', subtitleRu: 'Находчивость и торг', colorHex: '#38BDF8' },
];

export interface TraitGameplayPerks {
  curiosityBonusCoins: number;
  curiosityClueChancePct: number;
  tendernessMoodHealBonusPct: number;
  playfulnessCharmOutOfDebtChancePct: number;
  romanticismEveningCoinBonusPct: number;
  courageAvoidExhaustionChancePct: number;
  witMerchantSellBonusPct: number;
  dominantTraitKey: keyof ChantalTraits;
  dominantTraitLabelRu: string;
}

export function computeTraitGameplayPerks(traits: ChantalTraits): TraitGameplayPerks {
  const curiosityBonusCoins = Math.max(2, Math.floor(traits.curiosity / 3));
  const curiosityClueChancePct = Math.min(65, 15 + traits.curiosity * 2);
  const tendernessMoodHealBonusPct = Math.min(30, 8 + Math.floor(traits.tenderness / 2));
  const playfulnessCharmOutOfDebtChancePct = Math.min(50, 12 + traits.playfulness * 2);
  const romanticismEveningCoinBonusPct = Math.min(45, traits.romanticism * 2);
  const courageAvoidExhaustionChancePct = Math.min(55, 10 + traits.courage * 2);
  const witMerchantSellBonusPct = Math.min(60, traits.wit * 2);

  const entries = TRAIT_METADATA.map((m) => ({
    key: m.key,
    labelRu: m.labelRu,
    val: traits[m.key],
  })).sort((a, b) => b.val - a.val);

  return {
    curiosityBonusCoins,
    curiosityClueChancePct,
    tendernessMoodHealBonusPct,
    playfulnessCharmOutOfDebtChancePct,
    romanticismEveningCoinBonusPct,
    courageAvoidExhaustionChancePct,
    witMerchantSellBonusPct,
    dominantTraitKey: entries[0].key,
    dominantTraitLabelRu: entries[0].labelRu,
  };
}

export function getTraitActivePerkDescriptionRu(
  key: keyof ChantalTraits,
  perks: TraitGameplayPerks
): string {
  switch (key) {
    case 'curiosity':
      return `+${perks.curiosityBonusCoins} монет из тайников · ${perks.curiosityClueChancePct}% шанс найти улику`;
    case 'tenderness':
      return `+${perks.tendernessMoodHealBonusPct}% настроения от ласки и заботы дома`;
    case 'playfulness':
      return `${perks.playfulnessCharmOutOfDebtChancePct}% шанс обратить случайный долг в шутку`;
    case 'romanticism':
      return `+${perks.romanticismEveningCoinBonusPct}% монет в вечерних и звёздных походах`;
    case 'courage':
      return `${perks.courageAvoidExhaustionChancePct}% защита от усталости (45 Энергии)`;
    case 'wit':
      return `+${perks.witMerchantSellBonusPct}% к цене продажи диковинок скупщику`;
  }
}

export function getInteractivePettingDialogue(
  traits: ChantalTraits,
  isTransformed: boolean,
  variantIndex: number
): { actionLabelRu: string; reactionTextRu: string; traitBadgeRu: string } {
  const perks = computeTraitGameplayPerks(traits);
  const dom = perks.dominantTraitKey;

  if (isTransformed) {
    const reactionsByTrait: Record<keyof ChantalTraits, string[]> = {
      curiosity: [
        'Шанталь отрывается от старинной карты, поправляет огненно-рыжую прядь и с живым блеском в глазах говорит: «Смотри, на полях этого манускрипта проступают новые руны! Ещё пара походов — и мы разгадаем шифр».',
        'Она задумчиво вертит в пальцах серебряный медальон у камина: «Мне не даёт покоя та закрытая башня в Каменных Чертогах. Как только наберём сил, я снова наведаюсь туда».',
      ],
      tenderness: [
        'Шанталь мягко касается твоей ладони тёплыми пальцами и улыбается у огня: «Как же хорошо возвращаться домой, когда меня здесь ждут. Рядом с тобой даже древнее проклятие отступило».',
        'Она уютно кутается в плед у камина и прислоняется к твоему плечу: «Побудь со мной немного в тишине, прежде чем дорога снова позовёт в путь».',
      ],
      playfulness: [
        'Шанталь лукаво прищуривает золотисто-карие глаза и смеётся: «Знаешь, в лисьем облике воровать яблоки с ярмарки было куда проще! Но я и сейчас могу обвести ворчливого купца вокруг пальца».',
        'Она игриво щёлкает пальцами, зажигая маленькую танцующую искорку над ладонью: «Спорим на медовый пирог, что сегодня я принесу самую редкую диковинку?»',
      ],
      romanticism: [
        'Глядя на искры в камине, Шанталь тихо произносит: «Звёзды над перевалом сегодня светят совсем как в ту ночь, когда я впервые увидела твой фонарь в мокром лесу...»',
        'Она бережно поправляет изумрудный ворот и смотрит на тебя с тёплой задумчивостью: «В балладах менестрелей редко говорят правду, но наша с тобой история — настоящее чудо».',
      ],
      courage: [
        'Шанталь решительно проверяет ремешок дорожной сумки: «Тени у северной заставы сгущаются, но меня больше не запугать. Мы доведём это расследование до конца».',
        'Её взгляд вспыхивает твёрдым янтарным огнём: «Пусть буря воет на перевале — с твоей поддержкой я пройду любую тропу».',
      ],
      wit: [
        'Шанталь с ироничной улыбкой пересчитывает золотые монеты: «Гном Торвин снова пытался сбить цену, но против королевской логики и пары острых шуток у него не было ни шанса!»',
        'Она хитро приподнимает бровь: «У меня уже готов план из трёх шагов, как выведать секрет у звездочёта и при этом остаться в прибыли».',
      ],
    };
    const pool = reactionsByTrait[dom];
    return {
      actionLabelRu: 'Поговорить по душам у камина',
      reactionTextRu: pool[variantIndex % pool.length],
      traitBadgeRu: `Ведущая черта: ${perks.dominantTraitLabelRu} (+${perks.tendernessMoodHealBonusPct}% настроения)`,
    };
  }

  const foxReactionsByTrait: Record<keyof ChantalTraits, string[]> = {
    curiosity: [
      'Ты гладишь Шанталь за рыжим ушком — она тут же смешно наклоняет голову набок и начинает обнюхивать твои карманы в поисках карт или загадочных камешков!',
      'Лисёнок радостно потявкивает от ласки, но её уши уже навострены в сторону окна: кажется, она заметила светлячка у садовой калитки.',
    ],
    tenderness: [
      'Шанталь блаженно зажмуривает янтарные глаза, утыкается тёплым влажным носом в твою ладонь и тихонько урчит, обвив руку пушистым хвостом.',
      'От твоего прикосновения лисёнок доверчиво переворачивается на спинку у самого камина, подставляя пушистый животик и сонно зевая.',
    ],
    playfulness: [
      'Едва ты протягиваешь руку, Шанталь делает озорной прыжок передними лапками, легонько прихватывает твой рукав и затевает весёлую возню на пледе!',
      'Она смешно фыркает, крутится волчком за собственным рыжим хвостом и тут же плюхается рядом, требуя добавки ласки.',
    ],
    romanticism: [
      'Шанталь тихонько садится у твоих ног и задумчиво смотрит на отблески камина, словно слушает далёкую сказочную колыбельную.',
      'Она ласково трётся щекой о твою ладонь и долго смотрит в окно на первые вечерние звёзды удивительно глубоким, почти человеческим взглядом.',
    ],
    courage: [
      'Лисёнок гордо выпрямляет спинку, когда ты треплешь её по холке, и издаёт короткий смелый возглас — она явно считает себя главным защитником этого дома!',
      'Шанталь бодро стучит лапкой по походной сумке у двери, показывая, что не боится ни дождя, ни дальних лесных оврагов.',
    ],
    wit: [
      'Шанталь хитро прищуривается от удовольствия и ловко подвигает лапкой пустую мисочку ближе к тебе — мол, ласка лаской, а про ежевичный пирог не забывай!',
      'Она внимательно следит за твоими жестами, будто понимает каждое слово, и сообразительно кивает рыжей мордочкой.',
    ],
  };

  const pool = foxReactionsByTrait[dom];
  return {
    actionLabelRu: 'Погладить Шанталь за ушком',
    reactionTextRu: pool[variantIndex % pool.length],
    traitBadgeRu: `Ведущая черта: ${perks.dominantTraitLabelRu} (+${perks.tendernessMoodHealBonusPct}% настроения)`,
  };
}

export type LootCategory = 'TRINKET' | 'HERB' | 'ROYAL_CLUE';

export interface InventoryLootItem {
  id: string;
  titleRu: string;
  category: LootCategory;
  descriptionRu: string;
  sellPriceCoins: number;
  foundAtRegionRu: string;
  foundDateIso: string;
  canHealExhaustion?: boolean;
  inspectedLoreBonusClaimed?: boolean;
}

export type ShopCategory = 'APPAREL' | 'DEN_COMFORT' | 'CONSUMABLE';

export interface ChantalShopItem {
  id: string;
  titleRu: string;
  category: ShopCategory;
  priceCoins: number;
  upgradeCostCoins?: number;
  maxTier?: 1 | 2 | 3;
  visualSlotLabelRu: string;
  descriptionRu: string;
  gameplayEffectRu: string;
  reducesExhaustionCostBy?: number;
  preventsMishapBonusPct?: number;
  bonusCoinsPerJourney?: number;
  instantAction?: 'HEAL_EXHAUSTION' | 'CLEAR_DEBT' | 'BOOST_MOOD';
}

export interface TravelingMerchantOffer {
  id: string;
  titleRu: string;
  subtitleRu: string;
  originalPriceCoins: number;
  discountPriceCoins: number;
  rewardType: 'ROYAL_CLUE_MAP' | 'HERB_BUNDLE' | 'ROAD_AMULET' | 'TRAIT_TOME';
  effectDescriptionRu: string;
}

export const TRAVELING_MERCHANT_OFFERS: TravelingMerchantOffer[] = [
  {
    id: 'merch_secret_map',
    titleRu: 'Карта потайных звериных троп',
    subtitleRu: 'Редкость странствующего купца · Скидка 35%',
    originalPriceCoins: 85,
    discountPriceCoins: 55,
    rewardType: 'ROYAL_CLUE_MAP',
    effectDescriptionRu:
      'Мгновенно открывает наводку на Королевскую улику в шкатулку и даёт +2 к Любопытству ↑↑',
  },
  {
    id: 'merch_solar_amulet',
    titleRu: 'Янтарный оберег Попутного Ветра',
    subtitleRu: 'Заряженный талисман для следующего похода',
    originalPriceCoins: 60,
    discountPriceCoins: 38,
    rewardType: 'ROAD_AMULET',
    effectDescriptionRu:
      'Даёт 100% защиту от плохих новостей и +50% монет в следующем путешествии Шанталь',
  },
  {
    id: 'merch_mountain_herbs',
    titleRu: 'Связка редких трав со Звёздного перевала',
    subtitleRu: 'Свежий сбор горного травника',
    originalPriceCoins: 45,
    discountPriceCoins: 28,
    rewardType: 'HERB_BUNDLE',
    effectDescriptionRu:
      'Добавляет 2 целебные травы прямо в Сумку (для снятия усталости или домашней алхимии)',
  },
];

export interface NpcFriendshipEntry {
  npcId: string;
  nameRu: string;
  regionTitleRu: string;
  favoriteGiftHintRu: string;
  perkDescriptionRu: string;
}

export const KEY_COMPANION_NPCS: NpcFriendshipEntry[] = [
  {
    npcId: 'npc_bramble',
    nameRu: 'Барсук-травник Брамбл',
    regionTitleRu: 'Старая водяная мельница',
    favoriteGiftHintRu: 'Ценит лесные шишки, травы и старинные баночки',
    perkDescriptionRu: 'Присылает целебный чабрец и +1 к Нежности за каждый уровень дружбы',
  },
  {
    npcId: 'npc_corvus',
    nameRu: 'Ворон-почтальон Корвус',
    regionTitleRu: 'Янтарная дубрава',
    favoriteGiftHintRu: 'Обожает блестящие пуговицы, жетоны и монетки',
    perkDescriptionRu: 'Приносит тайные слухи (+1 к Любопытству и +15 Монет за уровень дружбы)',
  },
  {
    npcId: 'npc_torvin',
    nameRu: 'Гном-гончар Торвин',
    regionTitleRu: 'Туманная лощина',
    favoriteGiftHintRu: 'Любит редкие камни, пряжки и ремесленные диковинки',
    perkDescriptionRu: 'Обучает искусству торга (+1 к Остроумию и подарок в монетах)',
  },
  {
    npcId: 'npc_hugo',
    nameRu: 'Старый перевозчик Хьюго',
    regionTitleRu: 'Старая водяная мельница',
    favoriteGiftHintRu: 'Рад дорожным сувенирам и историям о речном затоне',
    perkDescriptionRu: 'Помогает искать королевские тайники (+1 к Отваге и +15 Монет)',
  },
];

export interface DenVisualTierInfo {
  tierNumber: 1 | 2 | 3;
  titleRu: string;
  subtitleRu: string;
  imageUrl: string;
  unlockedFeaturesRu: string[];
  nextUnlockHintRu: string;
}

export const DEN_TIER_IMAGES = {
  tier1: '/src/assets/images/chantal_stage1_rescued_kit_1794001402267.jpg',
  tier2: '/src/assets/images/chantal_den_tier2_1794031492048.jpg',
  tier3: '/src/assets/images/chantal_den_tier3_1794031493489.jpg',
  merchantChest: '/src/assets/images/chantal_merchant_chest_1794031494723.jpg',
};

export function getDenVisualTierInfo(
  ownedDenItemCount: number,
  previewTierOverride?: 1 | 2 | 3 | null
): DenVisualTierInfo {
  const effectiveTier: 1 | 2 | 3 =
    previewTierOverride ??
    (ownedDenItemCount >= 4 ? 3 : ownedDenItemCount >= 2 ? 2 : 1);

  if (effectiveTier === 3) {
    return {
      tierNumber: 3,
      titleRu: 'Уровень Домика III · Королевский чертог и Алхимический кабинет',
      subtitleRu:
        'Стрельчатое окно со звёздной астролябией, резной стол с эликсирами и оберегами, библиотека хроник и тёплый камин',
      imageUrl: DEN_TIER_IMAGES.tier3,
      unlockedFeaturesRu: [
        'Алхимический стол: крафт оберегов даёт +50% монет и щит от неудач',
        'Звёздная астролябия: +15% шанс найти Королевскую улику',
        'Максимальный уют: восстановление после грозы всего за 30 Энергии',
      ],
      nextUnlockHintRu: 'Достигнут максимальный III уровень обустройства Домика!',
    };
  }

  if (effectiveTier === 2) {
    return {
      tierNumber: 2,
      titleRu: 'Уровень Домика II · Тёплая лесная гостиная у камина',
      subtitleRu:
        'Каменный очаг с негасимым фонарём, резная дубовая полка со светящимися шишками, пучки чабреца и кресло с пледом',
      imageUrl: DEN_TIER_IMAGES.tier2,
      unlockedFeaturesRu: [
        'Резная полка трофеев: +5 монет за каждый поход',
        'Каменный очаг и сушёные травы: снижение цены усталости',
      ],
      nextUnlockHintRu: `Купи ещё ${Math.max(1, 4 - ownedDenItemCount)} предм. уюта в Лавке, чтобы открыть Уровень III (Алхимический кабинет и Обсерваторию)!`,
    };
  }

  return {
    tierNumber: 1,
    titleRu: 'Уровень Домика I · Скромный уголок у камина',
    subtitleRu:
      'Первое тёплое гнёздышко с шерстяным пледом, где спасённый лисёнок согрелся после дождя',
    imageUrl: DEN_TIER_IMAGES.tier1,
    unlockedFeaturesRu: ['Шерстяной плед: -5 Энергии к цене восстановления'],
    nextUnlockHintRu:
      'Купи Полку для диковинок или Каменный очаг в Лавке, чтобы преобразить Домик до Уровня II (с новой иллюстрацией интерьера)!',
  };
}

export const CHANTAL_SHOP_ITEMS: ChantalShopItem[] = [
  // 1. Одежда и снаряжение (можно прокачивать Tier 1 -> Tier 2 -> Tier 3!)
  {
    id: 'gear_knitted_scarf',
    titleRu: 'Тёплый вязаный шарф',
    category: 'APPAREL',
    priceCoins: 35,
    upgradeCostCoins: 30,
    maxTier: 3,
    visualSlotLabelRu: 'На шее: Шерстяной шарф цвета вереска',
    descriptionRu: 'Мягкий шарф ручной вязки, пахнущий сушёной лавандой и домашним теплом.',
    gameplayEffectRu: 'Защищает от холодного ветра · снижает шанс простудиться в походе на 15% (+8% за каждый Тир)',
    preventsMishapBonusPct: 15,
  },
  {
    id: 'gear_herb_satchel',
    titleRu: 'Кожаная сумка следопыта',
    category: 'APPAREL',
    priceCoins: 60,
    upgradeCostCoins: 45,
    maxTier: 3,
    visualSlotLabelRu: 'Через плечо: Тиснёная сумка с пряжкой',
    descriptionRu: 'Лёгкая дорожная сумочка из мягкой кожи с отделениями для трав и диковинок.',
    gameplayEffectRu: '+6 монет из каждого похода (+4 монеты за каждый Тир улучшения)',
    preventsMishapBonusPct: 10,
    bonusCoinsPerJourney: 6,
  },
  {
    id: 'gear_brass_lantern',
    titleRu: 'Медный путевой фонарь',
    category: 'APPAREL',
    priceCoins: 85,
    upgradeCostCoins: 55,
    maxTier: 3,
    visualSlotLabelRu: 'В лапе: Негасимый медный фонарь',
    descriptionRu: 'Старинный фонарь гномьей работы, освещающий даже самые туманные овраги.',
    gameplayEffectRu: 'Освещает ночные тропы · снижает риск передряг на 25% (+10% за Тир)',
    preventsMishapBonusPct: 25,
  },
  {
    id: 'gear_emerald_cloak',
    titleRu: 'Изумрудный плащ с золотым шитьём',
    category: 'APPAREL',
    priceCoins: 140,
    upgradeCostCoins: 75,
    maxTier: 3,
    visualSlotLabelRu: 'На плечах: Изумрудный дорожный плащ',
    descriptionRu: 'Благородный плащ с тонкой золотой вышивкой, удивительно к лицу огненно-рыжей шерсти (и локонам).',
    gameplayEffectRu: 'Вызывает уважение торговцев · +10 монет за поход и защита от непогоды',
    preventsMishapBonusPct: 25,
    bonusCoinsPerJourney: 10,
  },

  // 2. Обустройство домика (открывает новые иллюстрации Домика Уровня II и III!)
  {
    id: 'den_heather_blanket',
    titleRu: 'Плед из овечьей шерсти',
    category: 'DEN_COMFORT',
    priceCoins: 40,
    upgradeCostCoins: 30,
    maxTier: 3,
    visualSlotLabelRu: 'В уголке: Мягкий клетчатый плед',
    descriptionRu: 'Тёплое гнёздышко у самого камина, где так сладко дремать после долгой дороги.',
    gameplayEffectRu: 'Снижает затраты на восстановление после тяжёлого похода на 5 Энергии',
    reducesExhaustionCostBy: 5,
  },
  {
    id: 'den_curiosity_shelf',
    titleRu: 'Дубовая полка для диковинок',
    category: 'DEN_COMFORT',
    priceCoins: 65,
    upgradeCostCoins: 45,
    maxTier: 3,
    visualSlotLabelRu: 'На стене: Резная полка с трофеями',
    descriptionRu: 'Фигурная полка из морёного дуба, где Шанталь раскладывает светящиеся шишки и реликвии (открывает интерьер Домика II).',
    gameplayEffectRu: 'Повышает уют Домика · +5 бонусных монет за каждый поход (+4 за Тир)',
    bonusCoinsPerJourney: 5,
  },
  {
    id: 'den_stone_hearth',
    titleRu: 'Каменный очаг с медным котелком',
    category: 'DEN_COMFORT',
    priceCoins: 95,
    upgradeCostCoins: 55,
    maxTier: 3,
    visualSlotLabelRu: 'В центре дома: Живой тёплый камелёк',
    descriptionRu: 'Потрескивающие берёзовые дрова и связки душистых трав над камином (часть интерьера Домика II).',
    gameplayEffectRu: 'Снижает штраф усталости ещё на 5 Энергии (восстановление за 35 вместо 45)',
    reducesExhaustionCostBy: 5,
  },
  {
    id: 'den_alchemy_table',
    titleRu: 'Дубовый алхимический стол',
    category: 'DEN_COMFORT',
    priceCoins: 115,
    upgradeCostCoins: 65,
    maxTier: 3,
    visualSlotLabelRu: 'У окна: Резной стол алхимии и оберегов',
    descriptionRu: 'Мастерская со склянками, ступкой и пергаментами для создания дорожных талисманов (открывает интерьер Домика III).',
    gameplayEffectRu: 'Усиливает домашний крафт оберегов и снижает усталость ещё на 5 Энергии',
    reducesExhaustionCostBy: 5,
    bonusCoinsPerJourney: 6,
  },
  {
    id: 'den_star_astrolabe',
    titleRu: 'Латунная звёздная астролябия',
    category: 'DEN_COMFORT',
    priceCoins: 135,
    upgradeCostCoins: 75,
    maxTier: 3,
    visualSlotLabelRu: 'У сводчатого окна: Королевская астролябия',
    descriptionRu: 'Тонкий астрономический прибор у стрельчатого окна с видом на звёздный лес (вершина интерьера Домика III).',
    gameplayEffectRu: '+8 монет за поход и повышенный шанс найти Королевскую улику',
    bonusCoinsPerJourney: 8,
  },

  // 3. Расходники и быстрые решения
  {
    id: 'supply_herbal_balm',
    titleRu: 'Тёплый медово-травяной бальзам',
    category: 'CONSUMABLE',
    priceCoins: 20,
    visualSlotLabelRu: 'Сразу снимает усталость',
    descriptionRu: 'Душистый отвар старого барсука Брамбла. Мгновенно возвращает бодрость продрогшему путнику.',
    gameplayEffectRu: 'Мгновенно снимает штраф «45 Энергии» и возвращает обычную цену похода (30 Энергии)',
    instantAction: 'HEAL_EXHAUSTION',
  },
  {
    id: 'supply_honey_pie',
    titleRu: 'Сладкий ежевичный пирог',
    category: 'CONSUMABLE',
    priceCoins: 15,
    visualSlotLabelRu: 'Угощение для настроения',
    descriptionRu: 'Свежая выпечка с медовой корочкой с мельничной ярмарки.',
    gameplayEffectRu: 'Мгновенно поднимает настроение Шанталь до 100% и даёт +5 Энергии бодрости',
    instantAction: 'BOOST_MOOD',
  },
];

export interface RegionNpcInfo {
  nameRu: string;
  roleRu: string;
}

export interface MapRegionNode {
  id: string;
  titleRu: string;
  minJourneysRequired: number;
  coordXPercent: number;
  coordYPercent: number;
  loreRu: string;
  residentNpcRu: string;
  npcRoleRu: string;
  npcs: RegionNpcInfo[];
  unfinishedThreadsRu: string[];
}

export const WORLD_MAP_REGIONS: MapRegionNode[] = [
  {
    id: 'reg_old_mill',
    titleRu: 'Старая водяная мельница',
    minJourneysRequired: 0,
    coordXPercent: 52,
    coordYPercent: 34,
    loreRu: 'Шумное водяное колесо, мучная пыль в солнечных лучах и ярмарочные телеги у моста.',
    residentNpcRu: 'Барсук-травник Брамбл',
    npcRoleRu: 'Хранитель лесных кореньев и медовых отваров',
    npcs: [
      { nameRu: 'Барсук-травник Брамбл', roleRu: 'Хранитель кореньев и медовых отваров' },
      { nameRu: 'Мельничиха Марта', roleRu: 'Хозяйка пекарни и знаток окружных слухов' },
      { nameRu: 'Старый перевозчик Хьюго', roleRu: 'Страж речного парома у плотины' },
    ],
    unfinishedThreadsRu: [
      'разобрать старый сундук на чердаке мельницы, ключ от которого потерялся в камышах',
      'помочь травнику Брамблу собрать недостающие коренья для редкого согревающего бальзама',
      'расспросить паромщика Хьюго о ночных путниках в плащах с королевскими лилиями',
    ],
  },
  {
    id: 'reg_amber_forest',
    titleRu: 'Янтарная дубрава',
    minJourneysRequired: 0,
    coordXPercent: 26,
    coordYPercent: 56,
    loreRu: 'Тёплый золотистый лес с вековыми дубами, грибными полянами и старыми дорожными указателями.',
    residentNpcRu: 'Ворон-почтальон Корвус',
    npcRoleRu: 'Собиратель блестящих пуговиц и лесных сплетен',
    npcs: [
      { nameRu: 'Ворон-почтальон Корвус', roleRu: 'Собиратель блестящих пуговиц и писем' },
      { nameRu: 'Лесничий Олеандр', roleRu: 'Смотритель древних дубов и звериных троп' },
      { nameRu: 'Белка-меняла Тилли', roleRu: 'Хозяйка тайной лавки в дупле старого вяза' },
    ],
    unfinishedThreadsRu: [
      'дочитать оборванное письмо с сургучной печатью, которое уронил ворон Корвус',
      'проверить тайник под корнями Царь-Дуба, который открывается только в сумерках',
      'завершить спор с белкой Тилли о ценности старинной серебряной пряжки',
    ],
  },
  {
    id: 'reg_misty_hollow',
    titleRu: 'Туманная лощина',
    minJourneysRequired: 5,
    coordXPercent: 64,
    coordYPercent: 68,
    loreRu: 'Сумрачное ущелье со светящимся мхом, старым каменным мостом и эхом забытых песен.',
    residentNpcRu: 'Гном-гончар Торвин',
    npcRoleRu: 'Мастер глиняных кувшинов и азартный игрок в кости',
    npcs: [
      { nameRu: 'Гном-гончар Торвин', roleRu: 'Мастер обжига и любитель загадок' },
      { nameRu: 'Фонарщица Эльза', roleRu: 'Хранительница болотных огней на мосту' },
      { nameRu: 'Старый рудокоп Балин', roleRu: 'Искатель самоцветов в глубоких пещерах' },
    ],
    unfinishedThreadsRu: [
      'отыскать вторую половину расколотой глиняной таблички в мастерской Торвина',
      'пройти с фонарщицей Эльзой по дальней арке моста, куда не заглядывают торговцы',
      'помочь рудокопу Балину открыть заваленную галерею со светящимся кварцем',
    ],
  },
  {
    id: 'reg_stone_halls',
    titleRu: 'Древние Каменные Чертоги',
    minJourneysRequired: 15,
    coordXPercent: 84,
    coordYPercent: 28,
    loreRu: 'Заросшие плющом руины королевской заставы, где на камнях высечены полустёртые лилии.',
    residentNpcRu: 'Странствующий менестрель Лютик-младший',
    npcRoleRu: 'Искатель баллад о пропавшей принцессе',
    npcs: [
      { nameRu: 'Странствующий менестрель Лютик-младший', roleRu: 'Искатель забытых королевских баллад' },
      { nameRu: 'Архивариус Себастьян', roleRu: 'Хранитель уцелевшей замковой библиотеки' },
      { nameRu: 'Капитан ночной стражи Родерик', roleRu: 'Рыцарь в отставке, помнящий старый двор' },
    ],
    unfinishedThreadsRu: [
      'собрать недостающие куплеты баллады о заколдованном дитяти вместе с менестрелем',
      'открыть закрытый читальный зал архивариуса Себастьяна, где хранятся родословные свитки',
      'узнать у капитана Родерика, кто отдал приказ закрыть ворота в ночь бури',
    ],
  },
  {
    id: 'reg_mountain_pass',
    titleRu: 'Звёздный перевал чародеев',
    minJourneysRequired: 40,
    coordXPercent: 58,
    coordYPercent: 14,
    loreRu: 'Высокогорные тропы, где воздух звенит от древней магии и открывается вид на дальние королевства.',
    residentNpcRu: 'Хранительница обсерватории Ирэн',
    npcRoleRu: 'Звездочёт и знаток древних проклятий',
    npcs: [
      { nameRu: 'Хранительница обсерватории Ирэн', roleRu: 'Звездочёт и мастер снятия иллюзий' },
      { nameRu: 'Алхимик Калькештейн-северный', roleRu: 'Исследователь древних эликсиров и чар' },
      { nameRu: 'Гонец Серебряного Шпиля', roleRu: 'Тайный вестник из столичного замка' },
    ],
    unfinishedThreadsRu: [
      'дождаться особого схождения созвездий в телескопе Ирэн для чтения тайной печати',
      'проверить реакцию древнего кристалла у алхимика на прикосновение Шанталь',
      'перехватить зашифрованное послание гонца Серебряного Шпиля на горной развилке',
    ],
  },
];

export interface StoryChoiceOption {
  labelRu: string;
  followUpReactionRu: string;
  traitDeltas: ChantalTraits;
}

export interface GeneratedStoryEpisode {
  id: string;
  journeyNumber: number;
  createdAtIso: string;
  titleRu: string;
  storyTextRu: string;
  chantalMapNoteRu: string;
  pastEchoNoteRu?: string;
  regionId: string;
  regionTitleRu: string;
  encounteredNpcRu: string;
  dailyArcStep: 1 | 2 | 3;
  isFillerEpisode: boolean;
  customFillerPrompt?: string;
  lootItem: InventoryLootItem;
  outcomeType: 'GOOD_REWARD' | 'BAD_DEBT' | 'BAD_EXHAUSTION';
  coinsEarned: number;
  badNewsTitleRu: string;
  debtAmountCoins: number;
  choices: StoryChoiceOption[];
  selectedChoiceIndex?: number;
  userCustomResponseText?: string;
  aiSourceLabel?: string;
  stayedInSameRegion?: boolean;
  autonomousReasonRu?: string;
  isMilestoneChest?: boolean;
}

export interface ActiveJourneyTimerState {
  startedAtEpochMs: number;
  durationMs: number; // 3_600_000 (1 hour)
  regionId: string;
  regionTitleRu: string;
  encounteredNpcRu: string;
  npcRoleRu?: string;
  dailyArcStep: 1 | 2 | 3;
  isFillerEpisode: boolean;
  customFillerPrompt?: string;
  stayedInSameRegion?: boolean;
  autonomousReasonRu?: string;
}

export interface AutonomousDestinationChoice {
  region: MapRegionNode;
  encounteredNpcRu: string;
  npcRoleRu: string;
  stayedInSameRegion: boolean;
  decisionReasonRu: string;
  unfinishedThreadRu: string;
}

// Начальная летопись первых 3 походов (чтобы с самого старта Трёхслойная Память помнила Походы #1, #2, #3!)
export const INITIAL_SEEDED_JOURNAL_HISTORY: GeneratedStoryEpisode[] = [
  {
    id: 'seed_ep_3',
    journeyNumber: 3,
    createdAtIso: '2026-11-06',
    titleRu: 'Тайна камышового затона у Мельницы',
    storyTextRu:
      'В свой третий выход из дома Шанталь вернулась к Старой водяной мельнице, где у причала возился со смоляной лодкой старый перевозчик Хьюго. Лисёнок заметила в густых камышах у берега полузатопленный дубовый ларец со сломанной печатью, но достать его с воды самой было не под силу. Хьюго помог подцепить находку багром и удивлённо покачал головой, увидев на крышке гравировку королевской лилии. Внутри оказалась старинная латунная пуговица и обрывок карты, ведущий к каменному мосту.',
    chantalMapNoteRu:
      'Шанталь обследовала речной затон у Старой водяной мельницы вместе с перевозчиком Хьюго.',
    pastEchoNoteRu: 'Связь с Походом #1: Брамбл подсказал спросить у паромщика Хьюго про старый затон.',
    regionId: 'reg_old_mill',
    regionTitleRu: 'Старая водяная мельница',
    encounteredNpcRu: 'Старый перевозчик Хьюго',
    dailyArcStep: 3,
    isFillerEpisode: false,
    lootItem: {
      id: 'seed_loot_3',
      titleRu: 'Пуговица с жилета мельника',
      category: 'TRINKET',
      descriptionRu: 'Блестящая латунная пуговица с изображением колоса.',
      sellPriceCoins: 15,
      foundAtRegionRu: 'Старая водяная мельница',
      foundDateIso: '2026-11-06',
    },
    outcomeType: 'GOOD_REWARD',
    coinsEarned: 28,
    badNewsTitleRu: '',
    debtAmountCoins: 0,
    choices: [
      {
        labelRu: 'Пообещать Хьюго вернуться и вместе найти ключ от второго отделения ларца',
        followUpReactionRu: 'Шанталь серьёзно кивнула — это обещание в Походе #3 ещё обязательно напомнит о себе!',
        traitDeltas: { curiosity: 2, tenderness: 0, playfulness: 0, romanticism: 0, courage: 1, wit: 1 },
      },
    ],
    selectedChoiceIndex: 0,
    aiSourceLabel: 'Летопись мира',
  },
  {
    id: 'seed_ep_2',
    journeyNumber: 2,
    createdAtIso: '2026-11-05',
    titleRu: 'Спор с вороном Корвусом под Царь-Дубом',
    storyTextRu:
      'Во втором походе Шанталь отправилась по солнечной тропе в Янтарную дубраву, где на нижней ветке векового дуба ворон-почтальон Корвус перебирал свою коллекцию блестящих находок. Увидев маленького рыжего лисёнка, ворон уронил вниз странную светящуюся шишку и тут же потребовал за неё выкуп — весёлую загадку или корочку пирога. Шанталь хитро прищурилась, сделала вид, что шишка её совсем не интересует, и выменяла её на найденное перо сойки.',
    chantalMapNoteRu:
      'Шанталь побывала в Янтарной дубраве и перехитрила ворона-почтальона Корвуса.',
    regionId: 'reg_amber_forest',
    regionTitleRu: 'Янтарная дубрава',
    encounteredNpcRu: 'Ворон-почтальон Корвус',
    dailyArcStep: 2,
    isFillerEpisode: false,
    lootItem: {
      id: 'seed_loot_2',
      titleRu: 'Светящаяся шишка из Янтарной дубравы',
      category: 'TRINKET',
      descriptionRu: 'Тёплая кедровая шишка, которая мягко светится в сумерках.',
      sellPriceCoins: 20,
      foundAtRegionRu: 'Янтарная дубрава',
      foundDateIso: '2026-11-05',
    },
    outcomeType: 'GOOD_REWARD',
    coinsEarned: 25,
    badNewsTitleRu: '',
    debtAmountCoins: 0,
    choices: [
      {
        labelRu: 'Похвалить за хитрость и попросить Корвуса приносить нам лесные письма',
        followUpReactionRu: 'Корвус запомнил уговор из Похода #2 и обещал следить за новостями в лесу!',
        traitDeltas: { curiosity: 1, tenderness: 0, playfulness: 1, romanticism: 0, courage: 0, wit: 2 },
      },
    ],
    selectedChoiceIndex: 0,
    aiSourceLabel: 'Летопись мира',
  },
  {
    id: 'seed_ep_1',
    journeyNumber: 1,
    createdAtIso: '2026-11-04',
    titleRu: 'Первые шаги за порог и травы барсука Брамбла',
    storyTextRu:
      'Едва оправившись после дождливого вечера, в свой самый первый поход Шанталь осторожно выбежала за калитку к Старой водяной мельнице. Старый барсук-травник Брамбл сразу узнал в ней необычную гостью, осмотрел её зажившую лапку и подарил пучок душистого солнечного чабреца. Когда Шанталь случайно задела лапкой старинный серебряный медальон на столе травника, тот слабо вспыхнул тёплым янтарным светом.',
    chantalMapNoteRu:
      'Первый выход Шанталь к Старой водяной мельнице и знакомство с травником Брамблом.',
    regionId: 'reg_old_mill',
    regionTitleRu: 'Старая водяная мельница',
    encounteredNpcRu: 'Барсук-травник Брамбл',
    dailyArcStep: 1,
    isFillerEpisode: false,
    lootItem: {
      id: 'seed_loot_1',
      titleRu: 'Медальон с полустёртым гербом лилии',
      category: 'ROYAL_CLUE',
      descriptionRu: 'Старинный серебряный жетон, откликнувшийся на прикосновение Шанталь.',
      sellPriceCoins: 40,
      foundAtRegionRu: 'Старая водяная мельница',
      foundDateIso: '2026-11-04',
    },
    outcomeType: 'GOOD_REWARD',
    coinsEarned: 22,
    badNewsTitleRu: '',
    debtAmountCoins: 0,
    choices: [
      {
        labelRu: 'Ласково согреть Шанталь дома и спрятать медальон в шкатулку тайн',
        followUpReactionRu: 'С этого вечера в Походе #1 началась ваша большая общая история.',
        traitDeltas: { curiosity: 1, tenderness: 2, playfulness: 0, romanticism: 1, courage: 0, wit: 0 },
      },
    ],
    selectedChoiceIndex: 0,
    aiSourceLabel: 'Летопись мира',
  },
];

export interface ThreeTierChroniclePayload {
  /** Слой 1: Сжатый паспорт КАЖДОГО похода с #1 по текущий (чтобы в 20-м походе ИИ помнил 3-й и 5-й!) */
  fullChroniclePassports: string[];
  /** Слой 2: Живая картотека отношений с NPC и данных обещаний по всей истории */
  npcRelationshipDossier: string[];
  /** Слой 3: Полный текст последних 3 походов для бесшовного стиля */
  recentThreeFullStories: Array<{
    journeyNumber: number;
    titleRu: string;
    regionTitleRu: string;
    encounteredNpcRu: string;
    storyTextRu: string;
    chosenActionRu: string;
  }>;
}

/**
 * Формирует «Трёхслойную Память Хроник» по всем записям Дневника (от Похода #1 до текущего):
 * 1) Полная летопись решений (1 строка на каждый поход — помнит и 1-й, и 3-й, и 5-й поход даже на 50-м шаге!).
 * 2) Досье знакомств с персонажами (кто что помнит и какие обещания даны).
 * 3) Полный текст последних 3 глав.
 */
export function buildThreeTierChronicleMemory(
  journalHistory: GeneratedStoryEpisode[]
): ThreeTierChroniclePayload {
  // Сортируем от самого первого похода (#1) к самому свежему для хронологической летописи
  const chronological = [...journalHistory].sort(
    (a, b) => (a.journeyNumber || 0) - (b.journeyNumber || 0)
  );

  // Слой 1: Паспорта всех походов с #1 по #N
  const fullChroniclePassports = chronological.map((ep) => {
    const chosenText =
      ep.selectedChoiceIndex !== undefined && ep.choices[ep.selectedChoiceIndex]
        ? ep.choices[ep.selectedChoiceIndex].labelRu
        : ep.userCustomResponseText || 'Поддержал Шанталь дома';
    const epTypeTag = ep.isFillerEpisode ? '[Филлер]' : `[Поход #${ep.journeyNumber}]`;
    return `${epTypeTag} Локация: «${ep.regionTitleRu}» | Персонаж: ${ep.encounteredNpcRu} | Глава: «${ep.titleRu}» | ВЫБОР ИГРОКА: «${chosenText}» | Находка: «${ep.lootItem.titleRu}»`;
  });

  // Слой 2: Картотека NPC (группируем все встречи с каждым NPC и номера походов)
  const npcMap = new Map<string, string[]>();
  chronological.forEach((ep) => {
    const chosenText =
      ep.selectedChoiceIndex !== undefined && ep.choices[ep.selectedChoiceIndex]
        ? ep.choices[ep.selectedChoiceIndex].labelRu
        : ep.userCustomResponseText || 'Доброе расставание';
    const list = npcMap.get(ep.encounteredNpcRu) || [];
    list.push(`Поход #${ep.journeyNumber} («${ep.titleRu}» -> выбор: «${chosenText}»)`);
    npcMap.set(ep.encounteredNpcRu, list);
  });

  const npcRelationshipDossier: string[] = [];
  npcMap.forEach((meetings, npcName) => {
    npcRelationshipDossier.push(
      `• ${npcName} (встреч: ${meetings.length}): ${meetings.join('; ')}`
    );
  });

  // Слой 3: Полный текст последних 3 походов (самые свежие в начале journalHistory)
  const recentThreeFullStories = journalHistory.slice(0, 3).map((ep) => {
    const chosenText =
      ep.selectedChoiceIndex !== undefined && ep.choices[ep.selectedChoiceIndex]
        ? ep.choices[ep.selectedChoiceIndex].labelRu
        : ep.userCustomResponseText || 'Поддержал Шанталь дома';
    return {
      journeyNumber: ep.journeyNumber,
      titleRu: ep.titleRu,
      regionTitleRu: ep.regionTitleRu,
      encounteredNpcRu: ep.encounteredNpcRu,
      storyTextRu: ep.storyTextRu,
      chosenActionRu: chosenText,
    };
  });

  return {
    fullChroniclePassports,
    npcRelationshipDossier,
    recentThreeFullStories,
  };
}

// Stage progression helper WITHOUT spoiling Stage 4 to the player beforehand!
export function getChantalStageInfo(totalJourneys: number, forceTransformPreview: boolean = false) {
  const isTransformed = forceTransformPreview || totalJourneys >= 100;

  if (isTransformed) {
    const masteryLevel = Math.floor(Math.max(0, totalJourneys - 100) / 50) + 1;
    const nextMilestone = 100 + masteryLevel * 50;
    return {
      stageNumber: 4 as const,
      isTransformed: true,
      titleRu: `Шанталь · Расколдованная принцесса (Ранг ${masteryLevel})`,
      subtitleRu: 'Проклятие лисьего облика пало — впереди большая магическая сага в мире королевств и чар',
      currentStageStart: 100 + (masteryLevel - 1) * 50,
      nextStageTarget: nextMilestone,
      nextStageHintRu: `До следующей главы мастерства: ${Math.max(1, nextMilestone - totalJourneys)} поход.`,
      portraitUrl: '/src/assets/images/chantal_stage4_cursed_princess_1794001407610.jpg',
    };
  }

  if (totalJourneys <= 10) {
    return {
      stageNumber: 1 as const,
      isTransformed: false,
      titleRu: 'Стадия I · Спасённый лисёнок',
      subtitleRu: 'Совсем кроха в тёплом пледе: только осваивается в доме и делает первые шаги за порог',
      currentStageStart: 0,
      nextStageTarget: 10,
      nextStageHintRu: `До следующей вехи взросления: ${Math.max(1, 10 - totalJourneys)} из 10 походов`,
      portraitUrl: '/src/assets/images/chantal_stage1_rescued_kit_1794001402267.jpg',
    };
  }

  if (totalJourneys <= 50) {
    return {
      stageNumber: 2 as const,
      isTransformed: false,
      titleRu: 'Стадия II · Любопытный лисёнок',
      subtitleRu: 'Окрепла, носит маленькую кожаную сумку через плечо и смело исследует окрестные тропы',
      currentStageStart: 10,
      nextStageTarget: 50,
      nextStageHintRu: `Прогресс стадии: ${totalJourneys - 10} из 40 походов (всего ${totalJourneys}/50)`,
      portraitUrl: '/src/assets/images/chantal_stage2_curious_scout_1794001403826.jpg',
    };
  }

  return {
    stageNumber: 3 as const,
    isTransformed: false,
    titleRu: 'Стадия III · Юная путешественница',
    subtitleRu: 'В дорожном плаще с медным фонарём: уходит на дальние перевалы, где шепчутся древние тайны',
    currentStageStart: 50,
    nextStageTarget: 100,
    nextStageHintRu: `Прогресс стадии: ${totalJourneys - 50} из 50 походов (всего ${totalJourneys}/100)`,
    portraitUrl: '/src/assets/images/chantal_stage3_lantern_wanderer_1794001405831.jpg',
  };
}

export const WORLD_MAP_IMAGE_URL = '/src/assets/images/chantal_parchment_world_map_1794001407857.jpg';

export function formatTraitDeltasBadge(deltas: Partial<ChantalTraits>): string[] {
  const nameMap: Record<keyof ChantalTraits, string> = {
    curiosity: 'Любопытство',
    tenderness: 'Нежность',
    playfulness: 'Игривость',
    romanticism: 'Романтичность',
    courage: 'Отвага',
    wit: 'Остроумие',
  };

  const parts: string[] = [];
  (Object.keys(nameMap) as Array<keyof ChantalTraits>).forEach((k) => {
    const val = deltas[k] ?? 0;
    if (val > 0) {
      parts.push(`${nameMap[k]} ${val >= 2 ? '↑↑' : '↑'}`);
    } else if (val < 0) {
      parts.push(`${nameMap[k]} ${val <= -2 ? '↓↓' : '↓'}`);
    }
  });
  return parts;
}

export function pickAutonomousRegionForChantal(
  totalJourneys: number,
  lastRegionId?: string,
  lastEncounteredNpcRu?: string,
  hasActiveDebt?: boolean
): AutonomousDestinationChoice {
  const unlocked = WORLD_MAP_REGIONS.filter((r) => totalJourneys >= r.minJourneysRequired);
  const lastRegion = unlocked.find((r) => r.id === lastRegionId);

  const shouldStayInSameRegion =
    Boolean(lastRegion) && (hasActiveDebt ? Math.random() < 0.75 : Math.random() < 0.45);

  const chosenRegion =
    shouldStayInSameRegion && lastRegion
      ? lastRegion
      : unlocked[Math.floor(Math.random() * unlocked.length)] ?? WORLD_MAP_REGIONS[0];

  const stayedInSameRegion = Boolean(lastRegionId && chosenRegion.id === lastRegionId);

  const npcPool = chosenRegion.npcs?.length
    ? chosenRegion.npcs
    : [{ nameRu: chosenRegion.residentNpcRu, roleRu: chosenRegion.npcRoleRu }];

  const differentNpcs = npcPool.filter((n) => n.nameRu !== lastEncounteredNpcRu);
  const chosenNpc =
    stayedInSameRegion && differentNpcs.length > 0 && Math.random() < 0.7
      ? differentNpcs[Math.floor(Math.random() * differentNpcs.length)]
      : npcPool[Math.floor(Math.random() * npcPool.length)];

  const threadPool = chosenRegion.unfinishedThreadsRu?.length
    ? chosenRegion.unfinishedThreadsRu
    : ['исследовать новые зацепки на лесной тропе'];
  const chosenThread = threadPool[Math.floor(Math.random() * threadPool.length)];

  const decisionReasonRu = stayedInSameRegion
    ? `Шанталь решила снова вернуться в локацию «${chosenRegion.titleRu}», потому что у неё там остались незаконченные дела (${chosenThread}) и назначена встреча с персонажем ${chosenNpc.nameRu}.`
    : `Шанталь изучила карту и отправилась в локацию «${chosenRegion.titleRu}», чтобы ${chosenThread} и повидаться с персонажем ${chosenNpc.nameRu}.`;

  return {
    region: chosenRegion,
    encounteredNpcRu: chosenNpc.nameRu,
    npcRoleRu: chosenNpc.roleRu,
    stayedInSameRegion,
    decisionReasonRu,
    unfinishedThreadRu: chosenThread,
  };
}

// Rich offline/fallback generator so stories always work even without internet
export function buildFallbackStoryEpisode(params: {
  region: MapRegionNode;
  encounteredNpcRu?: string;
  autonomousReasonRu?: string;
  stayedInSameRegion?: boolean;
  dailyArcStep: 1 | 2 | 3;
  isFillerEpisode: boolean;
  customFillerPrompt?: string;
  isTransformed: boolean;
  totalJourneys: number;
  traits?: ChantalTraits;
  chronicleMemory?: ThreeTierChroniclePayload;
}): GeneratedStoryEpisode {
  const {
    region,
    encounteredNpcRu = region.residentNpcRu,
    autonomousReasonRu,
    stayedInSameRegion = false,
    dailyArcStep,
    isFillerEpisode,
    customFillerPrompt,
    isTransformed,
    totalJourneys,
    traits,
    chronicleMemory,
  } = params;
  const nowIso = new Date().toISOString().slice(0, 10);
  const perks = traits ? computeTraitGameplayPerks(traits) : null;

  // Находим реальную отсылку к одному из ранних походов (#1, #2, #3...) из Летописи!
  const earlyPassport =
    chronicleMemory?.fullChroniclePassports[
      Math.min(2, (chronicleMemory?.fullChroniclePassports.length || 1) - 1)
    ] || 'Поход #3 (обещание перевозчику Хьюго найти ключ от ларца)';

  const pastEchoNoteRu = `Эхо прошлых глав: приключение опирается на твой выбор из летописи — ${earlyPassport}`;

  if (isFillerEpisode) {
    const taskText = customFillerPrompt?.trim() || 'устроить неожиданную вылазку на ярмарку';
    return {
      id: `ep_filler_${Date.now()}`,
      journeyNumber: totalJourneys,
      createdAtIso: nowIso,
      titleRu: `Вне сюжета · Особое поручение`,
      storyTextRu: `Услышав твоё особое напутствие («${taskText}»), Шанталь весело повела ушами и тут же сорвалась с места по боковой тропинке. На полпути к локации «${region.titleRu}» она устроила настоящий переполох: местный житель ${encounteredNpcRu} только руками развёл, наблюдая за её проделками. Она действовала с такой непосредственностью и азартом, что даже хмурые путники у дороги не смогли сдержать улыбки. В суматохе ей удалось не только в точности выполнить твоё поручение, но и выменять у прохожих горсть звонких монет. Правда, на обратном пути она чуть не уронила свою добычу в ручей, но ловко подхватила её у самой воды. Домой она вернулась невероятно довольная собой, положив перед тобой любопытный сувенир на память об этой выходке.`,
      chantalMapNoteRu: `Шанталь свернула с главной дороги ради твоего поручения в районе «${region.titleRu}» и удивила персонажа ${encounteredNpcRu}.`,
      pastEchoNoteRu: 'Внесюжетный филлер-эпизод (не меняет каноническую летопись).',
      regionId: region.id,
      regionTitleRu: region.titleRu,
      encounteredNpcRu,
      dailyArcStep,
      isFillerEpisode: true,
      customFillerPrompt: taskText,
      stayedInSameRegion,
      autonomousReasonRu,
      lootItem: {
        id: `loot_${Date.now()}`,
        titleRu: 'Забавный жетон ярмарочного фокусника',
        category: 'TRINKET',
        descriptionRu: 'Медная монетка с улыбающимся лисом на одной стороне и клевером на другой. Напоминание о весёлой внесюжетной авантюре.',
        sellPriceCoins: 18,
        foundAtRegionRu: region.titleRu,
        foundDateIso: nowIso,
      },
      outcomeType: 'GOOD_REWARD',
      coinsEarned: 32,
      badNewsTitleRu: '',
      debtAmountCoins: 0,
      choices: [
        {
          labelRu: 'Похвалить за находчивость и угостить тёплым чаем',
          followUpReactionRu: 'Шанталь довольно жмурится — эта маленькая шалость подняла настроение вам обоим, не затронув главную судьбу.',
          traitDeltas: { curiosity: 0, tenderness: 0, playfulness: 0, romanticism: 0, courage: 0, wit: 0 },
        },
        {
          labelRu: 'Посмеяться над историей и убрать находку в сундук',
          followUpReactionRu: 'Она весело фыркает и сворачивается калачиком на пледе, готовая к новым основным походам.',
          traitDeltas: { curiosity: 0, tenderness: 0, playfulness: 0, romanticism: 0, courage: 0, wit: 0 },
        },
        {
          labelRu: 'Попросить в следующий раз быть чуть осторожнее на мосту',
          followUpReactionRu: 'Она притворно вздыхает, но послушно кивает, пряча нос в пушистый хвост.',
          traitDeltas: { curiosity: 0, tenderness: 0, playfulness: 0, romanticism: 0, courage: 0, wit: 0 },
        },
      ],
      aiSourceLabel: 'Локальный рассказчик',
    };
  }

  const isMilestoneChest = !isFillerEpisode && totalJourneys > 0 && totalJourneys % 5 === 0;
  const roll = Math.random();
  let outcomeType: 'GOOD_REWARD' | 'BAD_DEBT' | 'BAD_EXHAUSTION' = isMilestoneChest
    ? 'GOOD_REWARD'
    : roll < 0.14
    ? 'BAD_DEBT'
    : roll < 0.28
    ? 'BAD_EXHAUSTION'
    : 'GOOD_REWARD';

  if (
    outcomeType === 'BAD_DEBT' &&
    perks &&
    Math.random() * 100 < perks.playfulnessCharmOutOfDebtChancePct
  ) {
    outcomeType = 'GOOD_REWARD';
  }
  if (
    outcomeType === 'BAD_EXHAUSTION' &&
    perks &&
    Math.random() * 100 < perks.courageAvoidExhaustionChancePct
  ) {
    outcomeType = 'GOOD_REWARD';
  }

  const returnPrefix = stayedInSameRegion
    ? `Поскольку в локации «${region.titleRu}» у неё ещё оставались незаконченные дела, Шанталь целенаправленно вернулась туда снова — на этот раз её ждал ${encounteredNpcRu}. `
    : '';

  if (isTransformed) {
    const witcherChoices: StoryChoiceOption[] = [
      {
        labelRu: 'Обнять её у камина и вместе изучить тайный свиток',
        followUpReactionRu: 'Шанталь с нежной улыбкой кладёт голову тебе на плечо, и в её глазах пляшут тёплые отблески огня.',
        traitDeltas: { curiosity: 1, tenderness: 2, playfulness: 0, romanticism: 2, courage: 0, wit: 0 },
      },
      {
        labelRu: 'Пошутить над её княжескими манерами и предложить партию в кости',
        followUpReactionRu: 'Она звонко смеётся, откидывая рыжую прядь со лба: «Ну держись, сейчас посмотрим, на чьей стороне удача!»',
        traitDeltas: { curiosity: 0, tenderness: 0, playfulness: 2, romanticism: 1, courage: 0, wit: 2 },
      },
      {
        labelRu: 'Предложить немедленно укрепить защиту дома и проверить следы всадников',
        followUpReactionRu: 'Её взгляд становится сосредоточенным и решительным — она ценит твою готовность защищать её.',
        traitDeltas: { curiosity: 1, tenderness: 0, playfulness: -1, romanticism: 0, courage: 2, wit: 1 },
      },
    ];
    if (isMilestoneChest) {
      witcherChoices.push({
        labelRu: '★ Открыть потайное дно Юбилейного Сундука Глав королевским ключом',
        followUpReactionRu: 'Под двойным бархатным дном сундука вспыхивают древние руны: вы открыли особое сокровище юбилейной главы (+2 ко всем ключевым чертам)!',
        traitDeltas: { curiosity: 2, tenderness: 1, playfulness: 1, romanticism: 1, courage: 2, wit: 2 },
      });
    }

    return {
      id: `ep_witcher_${Date.now()}`,
      journeyNumber: totalJourneys,
      createdAtIso: nowIso,
      titleRu: isMilestoneChest
        ? `★ Юбилейный Сундук Главы #${totalJourneys} в «${region.titleRu}»`
        : stayedInSameRegion
        ? `Возвращение в «${region.titleRu}»: незаконченное дело`
        : `Огни над «${region.titleRu}»`,
      storyTextRu: `${returnPrefix}Поправив изумрудный ворот и коснувшись тёплого медальона из Похода #1 на груди, Шанталь — теперь уже в своём истинном девичьем облике с копной огненно-рыжих волос — бесшумно ступила на камни локации «${region.titleRu}». Ветер приносил запах озона и старого вереска, а впереди у костра её уже поджидал ${encounteredNpcRu}. «Я помню ваше давнее обещание у речного затона, ваше высочество», — тихо произнёс он, протягивая ей свиток с печатью северного двора. Шанталь прищурила золотисто-карие глаза, заметив в тени деревьев следы чужих подков — кто-то ещё искал этот архив до её прихода. Одним изящным жестом пальцев она зажгла в ладони крошечную янтарную искру, чтобы прочесть выцветшие строки. Внутри тайника оказался спрятан старинный сундук с реликвией, которая явно принадлежала её матери-принцессе. ${
        outcomeType === 'BAD_DEBT'
          ? 'Однако, чтобы выкупить молчание стражника у переправы, ей пришлось пообещать 25 полновесных монет до следующего рассвета.'
          : outcomeType === 'BAD_EXHAUSTION'
          ? 'Обратный путь через грозовой перевал отнял немало магических сил — на восстановление перед новым походом потребуется 45 единиц Энергии.'
          : 'Расплатившись за редкие сведения, она вернулась домой с полным кошельком золота и новой зацепкой.'
      }`,
      chantalMapNoteRu:
        autonomousReasonRu ||
        `Шанталь самостоятельно направилась в «${region.titleRu}», где тайно встретилась с персонажем ${encounteredNpcRu}.`,
      pastEchoNoteRu,
      regionId: region.id,
      regionTitleRu: region.titleRu,
      encounteredNpcRu,
      dailyArcStep,
      isFillerEpisode: false,
      stayedInSameRegion,
      autonomousReasonRu,
      isMilestoneChest,
      lootItem: {
        id: `loot_${Date.now()}`,
        titleRu: isMilestoneChest
          ? `Королевская реликвия Главы #${totalJourneys}: Перстень Серебряной Лилии`
          : 'Перстень с геральдической лилией',
        category: 'ROYAL_CLUE',
        descriptionRu: 'Тонкое серебряное кольцо, внутри которого выгравирована строчка из колыбельной королевского дома. От камня исходит едва заметное магическое тепло.',
        sellPriceCoins: 55,
        foundAtRegionRu: region.titleRu,
        foundDateIso: nowIso,
      },
      outcomeType,
      coinsEarned: isMilestoneChest ? 68 : 38,
      badNewsTitleRu:
        outcomeType === 'BAD_DEBT'
          ? 'Обещанный выкуп стражника у переправы: долг 25 Монет'
          : outcomeType === 'BAD_EXHAUSTION'
          ? 'Магическое истощение после грозового перевала: нужно 45 Энергии (или бальзам/трава)'
          : '',
      debtAmountCoins: outcomeType === 'BAD_DEBT' ? 25 : 0,
      choices: witcherChoices,
      aiSourceLabel: 'Локальный рассказчик',
    };
  }

  const lootCategoryRoll: LootCategory = isMilestoneChest
    ? 'ROYAL_CLUE'
    : dailyArcStep === 3
    ? 'ROYAL_CLUE'
    : dailyArcStep === 2
    ? 'HERB'
    : 'TRINKET';

  const hobbitChoices: StoryChoiceOption[] = [
    {
      labelRu: 'Ласково вытереть ей шёрстку полотенцем и погладить за ушком',
      followUpReactionRu: 'Шанталь благодарно лижет твою ладонь тёплым носом и уютно устраивается у огня.',
      traitDeltas: { curiosity: 0, tenderness: 2, playfulness: 0, romanticism: 1, courage: 0, wit: 0 },
    },
    {
      labelRu: 'Поиграть с ней в перетягивание платка и похвалить за смелость',
      followUpReactionRu: 'Лисёнок радостно подпрыгивает, делая забавный поклон передними лапками!',
      traitDeltas: { curiosity: 1, tenderness: 0, playfulness: 2, romanticism: 0, courage: 1, wit: 1 },
    },
    {
      labelRu: 'Внимательно рассмотреть её находку при свете фонаря',
      followUpReactionRu: 'Шанталь садится рядом, наклонив рыжую голову набок, и внимательно следит за твоим взглядом.',
      traitDeltas: { curiosity: 2, tenderness: 0, playfulness: 0, romanticism: 1, courage: 0, wit: 2 },
    },
  ];
  if (isMilestoneChest) {
    hobbitChoices.push({
      labelRu: '★ Открыть кованый Сундук Юбилейной Главы вместе с Шанталь',
      followUpReactionRu: 'Крышка окованного сундука со звоном откидывается: внутри сверкает двойная россыпь золотых монет и древняя улика с гербом лилии!',
      traitDeltas: { curiosity: 2, tenderness: 1, playfulness: 1, romanticism: 1, courage: 2, wit: 1 },
    });
  }

  return {
    id: `ep_hobbit_${Date.now()}`,
    journeyNumber: totalJourneys,
    createdAtIso: nowIso,
    titleRu: isMilestoneChest
      ? `★ Юбилейный Сундук Главы #${totalJourneys} в «${region.titleRu}»`
      : stayedInSameRegion
      ? `Снова в «${region.titleRu}»: встреча с персонажем ${encounteredNpcRu}`
      : `Тропинка к локации «${region.titleRu}»`,
    storyTextRu: `${returnPrefix}Едва утренний туман поднялся над травой, маленький рыжий лисёнок Шанталь сама выбрала направление по старой карте и бодро засеменила в сторону локации «${region.titleRu}». По дороге она вспомнила недавний уговор у речного затона из Похода #3 и решила проверить старые приметы у моста. Там её заметил ${encounteredNpcRu}, который как раз разбирал корзины с травами и диковинками. Увидев пушистую гостью с удивительно умными, почти человеческими глазами, он сразу припомнил её прежние проказы и угостил корочкой медового хлеба. Взамен Шанталь помогла ему отыскать закатившуюся под корень дуба старинную вещицу, напрямую связанную с их прошлым разговором. На секунду лисёнок замерла, глядя на своё отражение в чистой воде ручья, словно силясь вспомнить далёкий сон о каменном замке и музыке лютни. ${
      isMilestoneChest
        ? 'В награду за завершение юбилейной главы пути они вместе откопали под корнями окованный латунью Сундук Главы, полный золотых монет и королевских тайн!'
        : outcomeType === 'BAD_DEBT'
        ? 'Правда, вильнув хвостом от радости, она случайно смахнула с прилавка связку глиняных склянок — теперь на нас висит неловкий долг в 25 Монет перед мастером!'
        : outcomeType === 'BAD_EXHAUSTION'
        ? 'На обратном пути её застал внезапный холодный ливень: Шанталь прибежала домой промокшей и уставшей, поэтому на согревание и следующий поход понадобится 45 Энергии вместо 30.'
        : 'Вытряхнув из шерсти репейник, она гордо выложила на порог заработанные монеты и свою необычную лесную находку.'
    }`,
    chantalMapNoteRu:
      autonomousReasonRu ||
      `Шанталь сама побежала по тропе в «${region.titleRu}» и повстречала там персонажа ${encounteredNpcRu}.`,
    pastEchoNoteRu,
    regionId: region.id,
    regionTitleRu: region.titleRu,
    encounteredNpcRu,
    dailyArcStep,
    isFillerEpisode: false,
    stayedInSameRegion,
    autonomousReasonRu,
    isMilestoneChest,
    lootItem: {
      id: `loot_${Date.now()}`,
      titleRu: isMilestoneChest
        ? `Сокровище Главы #${totalJourneys}: Королевская печать с лилией`
        : lootCategoryRoll === 'ROYAL_CLUE'
        ? 'Осколок старинного зеркала в латунной оправе'
        : lootCategoryRoll === 'HERB'
        ? 'Пучок душистого солнечного чабреца'
        : 'Светящаяся шишка из Янтарной дубравы',
      category: lootCategoryRoll,
      descriptionRu:
        lootCategoryRoll === 'ROYAL_CLUE'
          ? 'На обратной стороне оправы виден крошечный королевский узор. Почему-то Шанталь долго и задумчиво смотрит в него перед сном.'
          : lootCategoryRoll === 'HERB'
          ? 'Редкая лесная трава, согревающая даже в самую сырую погоду. Можно заварить дома, чтобы мгновенно снять усталость (45 Энергии) и поднять настроение!'
          : 'Тёплая кедровая шишка, которая мягко светится в темноте, как маленький лесной ночник, и пахнет смолой.',
      sellPriceCoins: lootCategoryRoll === 'ROYAL_CLUE' ? 45 : lootCategoryRoll === 'HERB' ? 18 : 22,
      foundAtRegionRu: region.titleRu,
      foundDateIso: nowIso,
      canHealExhaustion: lootCategoryRoll === 'HERB',
    },
    outcomeType,
    coinsEarned: isMilestoneChest ? 60 : 28,
    badNewsTitleRu:
      outcomeType === 'BAD_DEBT'
        ? 'Разбитые склянки на прилавке: долг 25 Монет (можно оплатить или отработать)'
        : outcomeType === 'BAD_EXHAUSTION'
        ? 'Продрогла под лесным ливнем: на следующий поход нужно 45 Энергии (или тёплый бальзам / лесная трава)'
        : '',
    debtAmountCoins: outcomeType === 'BAD_DEBT' ? 25 : 0,
    choices: hobbitChoices,
    aiSourceLabel: 'Локальный рассказчик',
  };
}
