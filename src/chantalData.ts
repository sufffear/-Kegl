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

export type LootCategory = 'TRINKET' | 'HERB' | 'ROYAL_CLUE' | 'DEN_TROPHY';

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
  placedInDenComfort?: boolean;
}

export interface LootItemPropertyDescriptor {
  id: string;
  shortTitleRu: string;
  statTagRu: string;
  popupTitleRu: string;
  popupDescriptionRu: string;
  accentColorHex: string;
}

export function getLootItemEmoji(item: InventoryLootItem): string {
  const lower = item.titleRu.toLowerCase();
  if (item.category === 'DEN_TROPHY') {
    if (lower.includes('фонар') || lower.includes('светляч') || lower.includes('подсвеч')) return '🏮';
    if (lower.includes('подуш') || lower.includes('ковр') || lower.includes('плед')) return '🛋️';
    if (lower.includes('шкатул') || lower.includes('ларц')) return '🧰';
    if (lower.includes('ваз') || lower.includes('кувшин') || lower.includes('чаш')) return '🏺';
    if (lower.includes('картин') || lower.includes('гобелен') || lower.includes('ловец')) return '🖼️';
    return '🏠';
  }
  if (lower.includes('медальон') || lower.includes('жетон')) return '🏅';
  if (lower.includes('печать') || lower.includes('сургуч')) return '💮';
  if (lower.includes('манускрипт') || lower.includes('карта') || lower.includes('свиток')) return '📜';
  if (lower.includes('компас') || lower.includes('астролябия')) return '🧭';
  if (lower.includes('шишка') || lower.includes('дубрав')) return '🌲';
  if (lower.includes('пуговиц') || lower.includes('пряжк') || lower.includes('монет')) return '🪙';
  if (lower.includes('вереск') || lower.includes('цветок') || lower.includes('лепест')) return '🌸';
  if (lower.includes('чабрец') || lower.includes('мята') || lower.includes('зверобой') || lower.includes('трав')) return '🌿';
  if (lower.includes('ключ') || lower.includes('ларц')) return '🗝️';
  if (item.category === 'ROYAL_CLUE') return '👑';
  if (item.category === 'HERB') return '🌿';
  return '🔮';
}

export function getLootItemShortTitle(item: InventoryLootItem): string {
  const lower = item.titleRu.toLowerCase();
  if (lower.includes('фонарик')) return 'Лесной фонарик';
  if (lower.includes('медальон')) return 'Медальон';
  if (lower.includes('чабрец')) return 'Чабрец';
  if (lower.includes('шишка')) return 'Лесная шишка';
  if (lower.includes('пуговиц')) return 'Пуговица';
  const cleaned = item.titleRu.replace(/^Сокровище Сундука Главы #\d+:\s*/i, '').trim();
  const words = cleaned.split(/\s+/);
  if (words.length <= 2) return cleaned;
  return `${words[0]} ${words[1]}`;
}

export function getLootItemProperties(
  item: InventoryLootItem,
  witSellPrice: number
): LootItemPropertyDescriptor[] {
  if (item.category === 'DEN_TROPHY') {
    return [
      {
        id: 'prop_den_trophy_count',
        shortTitleRu: 'Свойство I · В зачёт жилья (+1/10)',
        statTagRu: 'Засчитано в уровень Домика ✓',
        popupTitleRu: 'Уникальный трофей интерьера из похода',
        popupDescriptionRu:
          'Эта вещь найдена Шанталь в путешествии и автоматически засчитывается как +1 предмет в счётчике обустройства Домика (например, 6 куплено + 1 принесла = 7 из 10)! Экономит целый день накопления монет.',
        accentColorHex: '#10B981',
      },
      {
        id: 'prop_den_trophy_exclusivity',
        shortTitleRu: 'Свойство II · Не купить в лавке',
        statTagRu: 'Лесной эксклюзив',
        popupTitleRu: 'Диковинный предмет уюта',
        popupDescriptionRu:
          'Такие предметы нельзя купить за монеты в обычной лавке — их можно только отыскать в тайниках с фонарём/отмычками или выменять у жителей леса на походные гостинцы.',
        accentColorHex: '#F59E0B',
      },
      {
        id: 'prop_den_trophy_bonus',
        shortTitleRu: `Свойство III · Из «${item.foundAtRegionRu}»`,
        statTagRu: '+1 монета / поход',
        popupTitleRu: 'Тёплая память о приключении',
        popupDescriptionRu: `Украшает жилище Шанталь и напоминает о походе в «${item.foundAtRegionRu}» (${item.foundDateIso}), давая +1 бонусную монету в будущих странствиях.`,
        accentColorHex: '#38BDF8',
      },
    ];
  }

  if (item.category === 'ROYAL_CLUE') {
    return [
      {
        id: 'prop_lore_curiosity',
        shortTitleRu: item.inspectedLoreBonusClaimed
          ? 'Свойство I · Тайна изучена (✓)'
          : 'Свойство I · Любопытство +1 ↑',
        statTagRu: item.inspectedLoreBonusClaimed ? 'Изучено ✓' : '+1 Любопытство ↑',
        popupTitleRu: 'Пробуждение памяти и Любопытства',
        popupDescriptionRu: item.inspectedLoreBonusClaimed
          ? 'Вы с Шанталь уже изучили эту реликвию у камина — бонус +1 к Любопытству получен и навсегда записан в характер.'
          : 'При внимательном осмотре у камина Шанталь вспоминает детали своего прошлого. Даёт постоянный бонус +1 к черте «Любопытство».',
        accentColorHex: '#F59E0B',
      },
      {
        id: 'prop_royal_seal',
        shortTitleRu: 'Свойство II · Печать шкатулки',
        statTagRu: 'Защита от продажи',
        popupTitleRu: 'Неприкосновенная реликвия Акта II',
        popupDescriptionRu:
          'Предмет отмечен королевской геральдикой. Хранится в особой шкатулке тайн и защищён от случайной продажи скупщику.',
        accentColorHex: '#A855F7',
      },
      {
        id: 'prop_region_resonance',
        shortTitleRu: `Свойство III · След: ${item.foundAtRegionRu}`,
        statTagRu: 'Сюжетная нить',
        popupTitleRu: `Связь с локацией «${item.foundAtRegionRu}»`,
        popupDescriptionRu: `Эта находка была принесена из локации «${item.foundAtRegionRu}» (${item.foundDateIso}) и учитывается нейросетью в будущих главах.`,
        accentColorHex: '#10B981',
      },
    ];
  }

  if (item.category === 'HERB') {
    return [
      {
        id: 'prop_herb_heal',
        shortTitleRu: 'Свойство I · Бодрость и уют',
        statTagRu: 'Усталость 45→30 Эн. · +15% настр.',
        popupTitleRu: 'Тёплый отвар у камина',
        popupDescriptionRu:
          'Заваривание этой травы мгновенно снимает дорожную усталость (возвращает цену похода с 45 к 30 Энергии) и повышает настроение Шанталь на +15%.',
        accentColorHex: '#10B981',
      },
      {
        id: 'prop_herb_alchemy',
        shortTitleRu: 'Свойство II · Реагент оберега',
        statTagRu: '+50% монет · Щит',
        popupTitleRu: 'Ингредиент для Оберега Попутного Ветра',
        popupDescriptionRu:
          'Соединяется с любой диковинкой или второй травой, создавая Оберег Попутного Ветра (+50% к монетам, 100% защита от неудач и +1 к Любопытству ↑).',
        accentColorHex: '#F59E0B',
      },
      {
        id: 'prop_herb_trade',
        shortTitleRu: `Свойство III · Ценность травника`,
        statTagRu: `+${witSellPrice} Монет`,
        popupTitleRu: 'Продажа лесному скупщику',
        popupDescriptionRu: `С учётом текущей черты «Остроумие» скупщик заплатит за свежий сбор ${witSellPrice} Монет (базовая цена: ${item.sellPriceCoins} М).`,
        accentColorHex: '#38BDF8',
      },
    ];
  }

  return [
    {
      id: 'prop_trinket_gift',
      shortTitleRu: 'Свойство I · Дар жителю леса',
      statTagRu: 'Дружба +1 ур. · +18 М · Черта ↑',
      popupTitleRu: 'Желанный подарок для друзей-NPC',
      popupDescriptionRu:
        'Если подарить эту диковинку Брамблу, Корвусу, Торвину или Хьюго, уровень дружбы вырастет на +1, а в ответ придут +18 Монет и +1 к профильной черте характера!',
      accentColorHex: '#EA580C',
    },
    {
      id: 'prop_trinket_alchemy',
      shortTitleRu: 'Свойство II · Основа для оберега',
      statTagRu: 'Крафт с травой',
      popupTitleRu: 'Магический фокус талисмана',
      popupDescriptionRu:
        'В паре с целебной травой превращается в Оберег Попутного Ветра, защищающий следующий поход от непогоды и долгов.',
      accentColorHex: '#10B981',
    },
    {
      id: 'prop_trinket_sell',
      shortTitleRu: `Свойство III · Оценка купца`,
      statTagRu: `+${witSellPrice} Монет`,
      popupTitleRu: 'Торг с бонусом Остроумия',
      popupDescriptionRu: `Можно выгодно продать в лесной лавке за ${witSellPrice} Монет благодаря прокачанному Остроумию Шанталь (базовая цена: ${item.sellPriceCoins} М).`,
      accentColorHex: '#F59E0B',
    },
  ];
}

export type ShopCategory = 'APPAREL' | 'DEN_COMFORT' | 'CONSUMABLE';

export type LoadoutSlotCategory = 'OUTFIT' | 'TOOL' | 'WEAPON' | 'BUNDLE';

export interface ChantalShopItem {
  id: string;
  titleRu: string;
  shortTitleRu: string;
  emoji: string;
  category: ShopCategory;
  loadoutSlotType?: LoadoutSlotCategory; // Слот экипировки: Одежда (OUTFIT), Инструмент (TOOL), Оружие/Защита (WEAPON), Узелок (BUNDLE)
  denStep?: 1 | 2 | 3; // Для предметов уюта: ступень 1 (1-10), 2 (11-20), 3 (21-30)
  priceCoins: number;
  upgradeCostCoins?: number;
  maxTier?: 1 | 2 | 3;
  maxDurability?: number; // На сколько походов хватает заряда/прочности снаряжения
  refillCostCoins?: number; // Цена починки / заправки на 5 походов
  denTrophyChanceBonusPct?: number; // Бонус к шансу принести предмет интерьера в Домик (+1 к X/10)
  expeditionCoinMultiplierPct?: number; // Бонус % к монетам при взятии в поход
  canPackInBackpack?: boolean; // Для гостинцев: можно положить в узелок в поход
  backpackEffectRu?: string; // Что даёт, если положить с собой в поход
  visualSlotLabelRu: string;
  descriptionRu: string;
  gameplayEffectRu: string;
  reducesExhaustionCostBy?: number;
  preventsMishapBonusPct?: number;
  bonusCoinsPerJourney?: number;
  instantAction?: 'HEAL_EXHAUSTION' | 'CLEAR_DEBT' | 'BOOST_MOOD' | 'PACK_PROVISION';
}

export interface TravelingMerchantOffer {
  id: string;
  titleRu: string;
  subtitleRu: string;
  originalPriceCoins: number;
  discountPriceCoins: number;
  rewardType: 'ROYAL_CLUE_MAP' | 'HERB_BUNDLE' | 'ROAD_AMULET' | 'DEN_TROPHY_CRATE';
  effectDescriptionRu: string;
}

export const TRAVELING_MERCHANT_OFFERS: TravelingMerchantOffer[] = [
  {
    id: 'merch_den_relic',
    titleRu: 'Резной фонарик со светлячком (В Домик +1/10)',
    subtitleRu: 'Редкий предмет уюта из фургона · Сразу +1 к уровню жилья',
    originalPriceCoins: 75,
    discountPriceCoins: 46,
    rewardType: 'DEN_TROPHY_CRATE',
    effectDescriptionRu:
      'Диковинный ночник для Домика со спящим лесным светлячком — сразу засчитывается как +1 предмет в счётчике уровня жилья (X/10)!',
  },
  {
    id: 'merch_secret_map',
    titleRu: 'Карта потайных звериных троп',
    subtitleRu: 'Редкость странствующего купца · Скидка 35%',
    originalPriceCoins: 80,
    discountPriceCoins: 52,
    rewardType: 'ROYAL_CLUE_MAP',
    effectDescriptionRu:
      'Мгновенно открывает наводку на Королевскую улику в шкатулку и даёт +2 к Любопытству ↑↑',
  },
  {
    id: 'merch_solar_amulet',
    titleRu: 'Янтарный оберег Попутного Ветра',
    subtitleRu: 'Заряженный талисман для следующего похода',
    originalPriceCoins: 55,
    discountPriceCoins: 34,
    rewardType: 'ROAD_AMULET',
    effectDescriptionRu:
      'Даёт 100% защиту от плохих новостей и +50% монет в следующем путешествии Шанталь',
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
  tierNumber: 1 | 2 | 3 | 4;
  titleRu: string;
  subtitleRu: string;
  imageUrl: string;
  unlockedFeaturesRu: string[];
  nextUnlockHintRu: string;
  totalDenItems: number;
  ownedShopDenCount: number;
  foundTrophyDenCount: number;
  itemsInCurrentStep: number;
  itemsTargetPerStep: 10;
  progressBadgeRu: string;
}

export const DEN_TIER_IMAGES = {
  tier1: '/src/assets/images/chantal_stage1_rescued_kit_1794001402267.jpg',
  tier2: '/src/assets/images/chantal_den_tier2_1794031492048.jpg',
  tier3: '/src/assets/images/chantal_den_tier3_1794031493489.jpg',
  tier4: '/src/assets/images/chantal_den_tier4_1794041326329.jpg',
  merchantChest: '/src/assets/images/chantal_merchant_chest_1794031494723.jpg',
};

/**
 * Система «10 предметов на каждый уровень жилья» (4 Уровня):
 * - 0..9 предметов уюта -> Уровень I (копим 10 предметов до Уровня II)
 * - 10..19 предметов уюта -> Уровень II (копим 10 предметов до Уровня III)
 * - 20..29 предметов уюта -> Уровень III (копим 10 предметов до Уровня IV)
 * - 30+ предметов уюта -> Уровень IV (максимальный Сказочный терем и Башня Звездочёта!)
 * В зачёт идут КАК купленные в Лавке предметы уюта, ТАК И принесённые Шанталь из походов трофеи для дома (DEN_TROPHY)!
 */
export function getDenVisualTierInfo(
  ownedShopDenCount: number,
  foundTrophyDenCount: number = 0
): DenVisualTierInfo {
  const totalDenItems = ownedShopDenCount + foundTrophyDenCount;
  const effectiveTier: 1 | 2 | 3 | 4 =
    totalDenItems >= 30 ? 4 : totalDenItems >= 20 ? 3 : totalDenItems >= 10 ? 2 : 1;

  const itemsInCurrentStep =
    effectiveTier === 4 ? 10 : totalDenItems - (effectiveTier - 1) * 10;

  const breakdownSuffix =
    foundTrophyDenCount > 0
      ? ` (${ownedShopDenCount} куплено + ${foundTrophyDenCount} принесла из походов)`
      : ` (куплено в лавке: ${ownedShopDenCount})`;

  const progressBadgeRu =
    effectiveTier === 4
      ? `Макс. Уровень IV (${totalDenItems} предм. уюта)`
      : `На след. уровень: ${itemsInCurrentStep} / 10${breakdownSuffix}`;

  if (effectiveTier === 4) {
    return {
      tierNumber: 4,
      titleRu: 'Жилище Шанталь (Уровень IV) · Сказочный терем и Башня Звездочёта',
      subtitleRu:
        'Витражный свод со звёздным телескопом, кристаллы-светильники под балками, полная библиотека хроник и бархатная подушка у камина',
      imageUrl: DEN_TIER_IMAGES.tier4,
      unlockedFeaturesRu: [
        'Вершина домашнего уюта (30+ предметов): максимальная бодрость и уют',
        'Звёздный телескоп и витраж: +25% шанс найти Королевскую улику',
        'Полная защита от дорожной стужи: восстановление всегда за 30 Энергии',
      ],
      nextUnlockHintRu: `Достигнут максимальный IV уровень жилья! Всего предметов уюта: ${totalDenItems}${breakdownSuffix}.`,
      totalDenItems,
      ownedShopDenCount,
      foundTrophyDenCount,
      itemsInCurrentStep: 10,
      itemsTargetPerStep: 10,
      progressBadgeRu,
    };
  }

  if (effectiveTier === 3) {
    return {
      tierNumber: 3,
      titleRu: 'Жилище Шанталь (Уровень III) · Уютный домик с камином и мастерской',
      subtitleRu:
        'Тёплый каменный камин, резной стол с эликсирами и оберегами, дубовые полки с диковинками и мягкое кресло',
      imageUrl: DEN_TIER_IMAGES.tier3,
      unlockedFeaturesRu: [
        'Алхимический уголок: крафт оберегов даёт повышенные монеты и щит',
        'Снижение штрафа усталости до минимума благодаря тёплому очагу',
      ],
      nextUnlockHintRu: `До Уровня IV («Сказочный терем»): ${itemsInCurrentStep} из 10 предметов этого уровня${breakdownSuffix}`,
      totalDenItems,
      ownedShopDenCount,
      foundTrophyDenCount,
      itemsInCurrentStep,
      itemsTargetPerStep: 10,
      progressBadgeRu,
    };
  }

  if (effectiveTier === 2) {
    return {
      tierNumber: 2,
      titleRu: 'Жилище Шанталь (Уровень II) · Тёплая лесная нора с очагом',
      subtitleRu:
        'Обжитая нора с каменным камельком, негасимым фонарём, резной полкой со светящимися шишками и пучками чабреца',
      imageUrl: DEN_TIER_IMAGES.tier2,
      unlockedFeaturesRu: [
        'Полка трофеев: пассивный бонус к монетам в каждом походе',
        'Каменный очаг и сушёные травы: снижение цены усталости',
      ],
      nextUnlockHintRu: `До Уровня III («Уютный домик»): ${itemsInCurrentStep} из 10 предметов этого уровня${breakdownSuffix}`,
      totalDenItems,
      ownedShopDenCount,
      foundTrophyDenCount,
      itemsInCurrentStep,
      itemsTargetPerStep: 10,
      progressBadgeRu,
    };
  }

  return {
    tierNumber: 1,
    titleRu: 'Жилище Шанталь (Уровень I) · Скромный уголок у камина',
    subtitleRu:
      'Первое тёплое гнёздышко с шерстяным пледом, где спасённый лисёнок согрелся после дождя',
    imageUrl: DEN_TIER_IMAGES.tier1,
    unlockedFeaturesRu: ['Шерстяной плед: -2 Энергии к цене восстановления после ливня'],
    nextUnlockHintRu: `До Уровня II («Тёплая лесная нора»): ${itemsInCurrentStep} из 10 предметов${breakdownSuffix}`,
    totalDenItems,
    ownedShopDenCount,
    foundTrophyDenCount,
    itemsInCurrentStep,
    itemsTargetPerStep: 10,
    progressBadgeRu,
  };
}

export const CHANTAL_SHOP_ITEMS: ChantalShopItem[] = [
  // ============================================================================
  // ОТДЕЛ 1: СНАРЯЖЕНИЕ В ПОХОД (Одежда, Инструменты, Оружие/Защита и Узелок)
  // ============================================================================
  // --- СЛОТ 1: ОДЕЖДА И ЭКИПИРОВКА (loadoutSlotType: 'OUTFIT') ---
  {
    id: 'gear_knitted_scarf',
    titleRu: 'Тёплый вязаный шарф',
    shortTitleRu: 'Шарф',
    emoji: '🧣',
    category: 'APPAREL',
    loadoutSlotType: 'OUTFIT',
    priceCoins: 38,
    upgradeCostCoins: 28,
    maxTier: 3,
    maxDurability: 5,
    refillCostCoins: 10,
    preventsMishapBonusPct: 35,
    bonusCoinsPerJourney: 2,
    visualSlotLabelRu: 'Одежда: Шерстяной шарф цвета вереска',
    descriptionRu:
      'Мягкий шарф ручной вязки, пахнущий сушёной лавандой. Согревает на ветреных перевалах и в сырой Туманной лощине.',
    gameplayEffectRu:
      'В походе: защищает от ливня и простуды (-35% шанс прийти уставшей или в плохом настроении). Запас ухода: 5 походов (10 М).',
  },
  {
    id: 'gear_leather_jacket',
    titleRu: 'Походная курточка следопыта',
    shortTitleRu: 'Курточка',
    emoji: '🧥',
    category: 'APPAREL',
    loadoutSlotType: 'OUTFIT',
    priceCoins: 46,
    upgradeCostCoins: 32,
    maxTier: 3,
    maxDurability: 5,
    refillCostCoins: 11,
    expeditionCoinMultiplierPct: 20,
    preventsMishapBonusPct: 25,
    bonusCoinsPerJourney: 4,
    visualSlotLabelRu: 'Одежда: Лёгкая кожаная курточка с кармашками',
    descriptionRu:
      'Удобная замшевая курточка с медными застёжками и множеством потайных карманов для монет и карт.',
    gameplayEffectRu:
      'В походе: +20% монет и -25% риск неприятностей в чащах. Запас прочности: 5 походов (11 М).',
  },
  {
    id: 'gear_herb_satchel',
    titleRu: 'Кожаная жилетка-перевязь травника',
    shortTitleRu: 'Перевязь',
    emoji: '🎒',
    category: 'APPAREL',
    loadoutSlotType: 'OUTFIT',
    priceCoins: 48,
    upgradeCostCoins: 32,
    maxTier: 3,
    maxDurability: 5,
    refillCostCoins: 10,
    expeditionCoinMultiplierPct: 20,
    denTrophyChanceBonusPct: 15,
    preventsMishapBonusPct: 15,
    bonusCoinsPerJourney: 4,
    visualSlotLabelRu: 'Одежда: Тиснёная перевязь с отделениями',
    descriptionRu:
      'Лёгкая дорожная перевязь из мягкой кожи с кармашками для целебных трав, монет и хрупких диковинок.',
    gameplayEffectRu:
      'В походе: +20% монет и +15% шанс принести вещь для Домика. Запас прочности: 5 походов (10 М).',
  },
  {
    id: 'gear_emerald_cloak',
    titleRu: 'Изумрудный плащ от непогоды',
    shortTitleRu: 'Плащ',
    emoji: '🦸',
    category: 'APPAREL',
    loadoutSlotType: 'OUTFIT',
    priceCoins: 85,
    upgradeCostCoins: 50,
    maxTier: 3,
    maxDurability: 6,
    refillCostCoins: 15,
    denTrophyChanceBonusPct: 15,
    expeditionCoinMultiplierPct: 25,
    preventsMishapBonusPct: 30,
    bonusCoinsPerJourney: 6,
    visualSlotLabelRu: 'Одежда: Изумрудный дорожный плащ',
    descriptionRu:
      'Плотный непромокаемый плащ с золотым шитьём — защищает от горных бурь и вызывает уважение у лесных купцов.',
    gameplayEffectRu:
      'В походе: +25% монет, -30% риск долгов и +15% шанс редкого предмета в дом. Запас прочности: 6 походов (15 М).',
  },

  // --- СЛОТ 2: ИНСТРУМЕНТЫ И НАВИГАЦИЯ (loadoutSlotType: 'TOOL') ---
  {
    id: 'gear_brass_lantern',
    titleRu: 'Масляный путевой фонарь',
    shortTitleRu: 'Фонарь',
    emoji: '🏮',
    category: 'APPAREL',
    loadoutSlotType: 'TOOL',
    priceCoins: 55,
    upgradeCostCoins: 35,
    maxTier: 3,
    maxDurability: 5,
    refillCostCoins: 12,
    denTrophyChanceBonusPct: 25,
    preventsMishapBonusPct: 25,
    bonusCoinsPerJourney: 3,
    visualSlotLabelRu: 'Инструмент: Негасимый медный фонарь',
    descriptionRu:
      'Старинный фонарь гномьей работы. Позволяет Шанталь заглядывать в тёмные чердаки мельницы, пещеры и ночные чащи.',
    gameplayEffectRu:
      'В походе: +25% шанс найти уникальный предмет интерьера для Домика (+1 к X/10) и -25% риск передряг в темноте. Запас масла: 5 походов (12 М).',
  },
  {
    id: 'gear_ancient_compass',
    titleRu: 'Старинный латунный компас',
    shortTitleRu: 'Компас',
    emoji: '🧭',
    category: 'APPAREL',
    loadoutSlotType: 'TOOL',
    priceCoins: 52,
    upgradeCostCoins: 35,
    maxTier: 3,
    maxDurability: 5,
    refillCostCoins: 12,
    expeditionCoinMultiplierPct: 30,
    preventsMishapBonusPct: 15,
    bonusCoinsPerJourney: 4,
    visualSlotLabelRu: 'Инструмент: Точный компас звездочёта',
    descriptionRu:
      'Чуткая магнитная стрелка помогает Шанталь находить короткие звериные тропы и купеческие тайники с монетами.',
    gameplayEffectRu:
      'В походе: +30% к монетам из путешествия. Запас калибровки: 5 походов (12 М).',
  },
  {
    id: 'gear_lockpick_trowel',
    titleRu: 'Связка отмычек и лопатка следопыта',
    shortTitleRu: 'Отмычки',
    emoji: '🗝️',
    category: 'APPAREL',
    loadoutSlotType: 'TOOL',
    priceCoins: 64,
    upgradeCostCoins: 40,
    maxTier: 3,
    maxDurability: 5,
    refillCostCoins: 14,
    denTrophyChanceBonusPct: 20,
    expeditionCoinMultiplierPct: 15,
    bonusCoinsPerJourney: 4,
    visualSlotLabelRu: 'Инструмент: Отмычки искателя кладов',
    descriptionRu:
      'Тонкие серебряные отмычки и лёгкая сапёрная лопатка. Если в истории попадётся запертая шкатулка или клад под корнями, Шанталь вскроет его прямо в сюжете!',
    gameplayEffectRu:
      'В походе: открывает запертые ларцы (+20% шанс найти предмет для Домика +1/10 и +15% монет). Запас прочности: 5 походов (14 М).',
  },

  // --- СЛОТ 3: ОРУЖИЕ И САМОЗАЩИТА В ПУТИ (loadoutSlotType: 'WEAPON') ---
  {
    id: 'weapon_poison_apple',
    titleRu: 'Ядовитое сонное яблоко в карамели',
    shortTitleRu: 'Сон-яблоко',
    emoji: '🍎',
    category: 'APPAREL',
    loadoutSlotType: 'WEAPON',
    priceCoins: 28,
    upgradeCostCoins: 22,
    maxTier: 3,
    maxDurability: 4,
    refillCostCoins: 8,
    preventsMishapBonusPct: 35,
    denTrophyChanceBonusPct: 20,
    bonusCoinsPerJourney: 3,
    visualSlotLabelRu: 'Защита: Сонное яблоко-приманка',
    descriptionRu:
      'Аппетитное на вид яблочко с сонным соком мака. Если на лесной тропе попадётся жадный тролль, стражник или разбойник — он тут же уснёт, оставив сундук без присмотра!',
    gameplayEffectRu:
      'В походе: +35% защита от засад и долгов, +20% шанс унести трофей из чужого тайника. Запас: 4 похода (8 М).',
  },
  {
    id: 'weapon_sleep_darts',
    titleRu: 'Духовая трубка с сонными дротиками',
    shortTitleRu: 'Дротики',
    emoji: '🎯',
    category: 'APPAREL',
    loadoutSlotType: 'WEAPON',
    priceCoins: 36,
    upgradeCostCoins: 26,
    maxTier: 3,
    maxDurability: 5,
    refillCostCoins: 9,
    preventsMishapBonusPct: 40,
    expeditionCoinMultiplierPct: 15,
    bonusCoinsPerJourney: 4,
    visualSlotLabelRu: 'Оружие: Бесшумные сонные дротики',
    descriptionRu:
      'Лёгкая тростниковая трубка с дротиками, пропитанными сонной пыльцой. Позволяет Шанталь бесшумно защитить себя в самой опасной чаще.',
    gameplayEffectRu:
      'В походе: +40% защита от любых врагов и неприятностей, +15% к добыче монет. Запас дротиков: 5 походов (9 М).',
  },
  {
    id: 'weapon_thorn_sling',
    titleRu: 'Ореховая праща с шипами шиповника',
    shortTitleRu: 'Праща',
    emoji: '🏹',
    category: 'APPAREL',
    loadoutSlotType: 'WEAPON',
    priceCoins: 32,
    upgradeCostCoins: 24,
    maxTier: 3,
    maxDurability: 5,
    refillCostCoins: 8,
    preventsMishapBonusPct: 30,
    expeditionCoinMultiplierPct: 20,
    bonusCoinsPerJourney: 4,
    visualSlotLabelRu: 'Оружие: Меткая лесная праща',
    descriptionRu:
      'Упругая праща, из которой Шанталь без промаха сбивает высоко висящие кошельки, замки и отпугивает наглых ворон.',
    gameplayEffectRu:
      'В походе: +30% защита в пути и +20% монет из труднодоступных тайников. Запас снарядов: 5 походов (8 М).',
  },
  {
    id: 'weapon_smoke_acorn',
    titleRu: 'Дымовые жёлуди алхимика',
    shortTitleRu: 'Дым-жёлудь',
    emoji: '💨',
    category: 'APPAREL',
    loadoutSlotType: 'WEAPON',
    priceCoins: 24,
    upgradeCostCoins: 20,
    maxTier: 3,
    maxDurability: 4,
    refillCostCoins: 7,
    preventsMishapBonusPct: 45,
    bonusCoinsPerJourney: 3,
    visualSlotLabelRu: 'Защита: Алхимическая дымовая завеса',
    descriptionRu:
      'При броске под лапки создаёт густое облако душистого серебристого тумана, позволяя мгновенно ускользнуть от любой опасности.',
    gameplayEffectRu:
      'В походе: +45% защита от засад и погони, оберегает от усталости. Запас: 4 похода (7 М).',
  },

  // --- СЛОТ 4: УЗЕЛОК С ПРИПАСАМИ (loadoutSlotType: 'BUNDLE') ---
  {
    id: 'supply_honey_pie',
    titleRu: 'Горячий пирожок с ежевикой',
    shortTitleRu: 'Пирожок',
    emoji: '🥧',
    category: 'CONSUMABLE',
    loadoutSlotType: 'BUNDLE',
    priceCoins: 14,
    canPackInBackpack: true,
    backpackEffectRu:
      'В узелке: 100% защита от усталости (45 Эн.) и +10% к монетам!',
    visualSlotLabelRu: 'Узелок в поход или угощение дома',
    descriptionRu:
      'Душистая выпечка с медовой корочкой. Можно скормить Шанталь дома ИЛИ положить ей в узелок перед выходом в поход!',
    gameplayEffectRu:
      'Дома: 100% настроение и +5 Энергии. В походе: защищает от истощения и радует встречных зверьков в сюжете.',
    instantAction: 'BOOST_MOOD',
  },
  {
    id: 'supply_herbal_balm',
    titleRu: 'Горшочек лесного мёда и бальзам',
    shortTitleRu: 'Мёд-бальзам',
    emoji: '🍯',
    category: 'CONSUMABLE',
    loadoutSlotType: 'BUNDLE',
    priceCoins: 18,
    canPackInBackpack: true,
    denTrophyChanceBonusPct: 35,
    backpackEffectRu:
      'В узелке: обмен с лесными жителями на предмет для Домика (+35% шанс +1 к X/10)!',
    visualSlotLabelRu: 'Снятие усталости дома или обмен в походе',
    descriptionRu:
      'Ароматный вересковый мёд с травами Брамбла. Дома снимает усталость, а в походе служит лучшей валютой для обмена с барсуком, ежом или гномом!',
    gameplayEffectRu:
      'Дома: снимает штраф «45 Энергии». В походе: +35% шанс выменять редкий предмет интерьера для Домика (+1 к уровню жилья).',
    instantAction: 'HEAL_EXHAUSTION',
  },
  {
    id: 'supply_shiny_buttons',
    titleRu: 'Горсть блестящих пуговиц для обмена',
    shortTitleRu: 'Пуговицы',
    emoji: '🪙',
    category: 'CONSUMABLE',
    loadoutSlotType: 'BUNDLE',
    priceCoins: 12,
    canPackInBackpack: true,
    expeditionCoinMultiplierPct: 35,
    denTrophyChanceBonusPct: 20,
    backpackEffectRu:
      'В узелке: ворон Корвус и белка Тилли дадут за них +35% монет и редкие находки!',
    visualSlotLabelRu: 'Походный узелок для лесного обмена',
    descriptionRu:
      'Мешочек переливающихся перламутровых и латунных пуговиц — любимое сокровище ворона Корвуса и белки-менялы Тилли.',
    gameplayEffectRu:
      'Кладётся в узелок перед походом: в сюжете Шанталь выгодно выменяет пуговицы на полную горсть монет (+35%) или вещицу в дом!',
    instantAction: 'PACK_PROVISION',
  },

  // ============================================================================
  // ОТДЕЛ 2: УЮТ В ДОМИК — 30 ПРЕДМЕТОВ ПО 3 СТУПЕНЯМ (10 предм. = +1 Уровень жилья!)
  // Математика: 1 сессия ~18-20 М, 3 сессии (1 день) ~55-60 М = 1 предмет в день!
  // ============================================================================

  // --- СТУПЕНЬ I: Первые 10 предметов (Переход с Уровня I на Уровень II · 35–68 Монет) ---
  {
    id: 'den_heather_blanket',
    titleRu: 'Плед из овечьей шерсти',
    shortTitleRu: 'Плед',
    emoji: '🛋️',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 35,
    upgradeCostCoins: 25,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · Уголок у огня',
    descriptionRu: 'Тёплое гнёздышко у самого камина, где так сладко дремать после долгой дороги.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · Снижает усталость на 2 Энергии',
    reducesExhaustionCostBy: 2,
  },
  {
    id: 'den_moss_bedding',
    titleRu: 'Подстилка из вереска и мха',
    shortTitleRu: 'Подстилка',
    emoji: '🌿',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 38,
    upgradeCostCoins: 25,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · Лесная лежанка',
    descriptionRu: 'Пружинистая душистая перина из сухого лесного мха и цветущего вереска.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +1 монета за каждый поход',
    bonusCoinsPerJourney: 1,
  },
  {
    id: 'den_clay_mug',
    titleRu: 'Глиняная кружка с ежевичной глазурью',
    shortTitleRu: 'Кружка',
    emoji: '☕',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 40,
    upgradeCostCoins: 25,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · На низком столике',
    descriptionRu: 'Тёплая кружка работы гнома Торвина для вечернего молока с мёдом.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +1 монета за поход',
    bonusCoinsPerJourney: 1,
  },
  {
    id: 'den_lavender_bundle',
    titleRu: 'Пучок сушёной лаванды и чабреца',
    shortTitleRu: 'Лаванда',
    emoji: '🌸',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 42,
    upgradeCostCoins: 28,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · Под балкой потолка',
    descriptionRu: 'Душистые травы наполняют нору умиротворяющим ароматом летнего луга.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · Снижает усталость на 1 Энергию',
    reducesExhaustionCostBy: 1,
  },
  {
    id: 'den_birch_firewood',
    titleRu: 'Поленница сухих берёзовых дров',
    shortTitleRu: 'Дрова',
    emoji: '🪵',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 45,
    upgradeCostCoins: 28,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · У правой стены',
    descriptionRu: 'Аккуратно сложенные поленья с белой берестой, весело трещащие в огне.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +1 монета за поход',
    bonusCoinsPerJourney: 1,
  },
  {
    id: 'den_wind_curtain',
    titleRu: 'Плотная занавеска от сквозняков',
    shortTitleRu: 'Занавеска',
    emoji: '🧶',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 48,
    upgradeCostCoins: 30,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · На входной двери',
    descriptionRu: 'Тяжёлая суконная портьера с вышитым листом дуба не пускает ночной холод в дом.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · Снижает усталость на 2 Энергии',
    reducesExhaustionCostBy: 2,
  },
  {
    id: 'den_acorn_chimes',
    titleRu: 'Лесной колокольчик из желудей',
    shortTitleRu: 'Колокольчик',
    emoji: '🔔',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 50,
    upgradeCostCoins: 30,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · Над крыльцом',
    descriptionRu: 'Мелодично звенит на ветру, возвещая о возвращении Шанталь из похода.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +1 монета за поход',
    bonusCoinsPerJourney: 1,
  },
  {
    id: 'den_iron_kettle',
    titleRu: 'Чугунный котелок на треноге',
    shortTitleRu: 'Котелок',
    emoji: '🫖',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 55,
    upgradeCostCoins: 35,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · Над углями',
    descriptionRu: 'В нём всегда булькает согревающий взвар из лесных ягод и кореньев.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · Снижает усталость на 2 Энергии',
    reducesExhaustionCostBy: 2,
  },
  {
    id: 'den_supply_chest',
    titleRu: 'Дубовый сундучок для припасов',
    shortTitleRu: 'Сундучок',
    emoji: '📦',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 62,
    upgradeCostCoins: 38,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · В кладовом углу',
    descriptionRu: 'Крепкий ларец с коваными уголками, куда мыши не проберутся за сухарями.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +2 монеты за поход',
    bonusCoinsPerJourney: 2,
  },
  {
    id: 'den_porch_lantern',
    titleRu: 'Ветровой фонарь у калитки',
    shortTitleRu: 'Фонарь дома',
    emoji: '🕯️',
    category: 'DEN_COMFORT',
    denStep: 1,
    priceCoins: 68,
    upgradeCostCoins: 40,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень I · У садовой дорожки',
    descriptionRu: 'Тёплый янтарный огонёк служит маяком для Шанталь в самые туманные сумерки.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +2 монеты за поход',
    bonusCoinsPerJourney: 2,
  },

  // --- СТУПЕНЬ II: Предметы 11–20 (Переход с Уровня II на Уровень III · 50–88 Монет) ---
  {
    id: 'den_woven_rug',
    titleRu: 'Домотканый половик у очага',
    shortTitleRu: 'Половик',
    emoji: '🧶',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 50,
    upgradeCostCoins: 35,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · На деревянном полу',
    descriptionRu: 'Пестрый узорчатый коврик, на котором Шанталь обожает катать светящиеся шишки.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +1 монета за поход',
    bonusCoinsPerJourney: 1,
  },
  {
    id: 'den_herb_rack',
    titleRu: 'Сушилка для трав над камином',
    shortTitleRu: 'Сушилка трав',
    emoji: '🌿',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 55,
    upgradeCostCoins: 35,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · Над каминной полкой',
    descriptionRu: 'Резная жердочка с пучками зверобоя, мяты и лунного вереска.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · Снижает усталость на 2 Энергии',
    reducesExhaustionCostBy: 2,
  },
  {
    id: 'den_curiosity_shelf',
    titleRu: 'Дубовая полка для диковинок',
    shortTitleRu: 'Полка',
    emoji: '🪵',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 58,
    upgradeCostCoins: 40,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · На центральной стене',
    descriptionRu: 'Фигурная полка из морёного дуба для выставки лесных трофеев и редких камней.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +2 монеты за поход',
    bonusCoinsPerJourney: 2,
  },
  {
    id: 'den_rocking_chair',
    titleRu: 'Плетёное кресло-качалка',
    shortTitleRu: 'Кресло',
    emoji: '🪑',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 65,
    upgradeCostCoins: 42,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · У правого края камина',
    descriptionRu: 'Уютное кресло из гибкой ивовой лозы, где так приятно слушать вечерние рассказы Шанталь.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +2 монеты за поход',
    bonusCoinsPerJourney: 2,
  },
  {
    id: 'den_copper_samovar',
    titleRu: 'Пузатый медный чайник',
    shortTitleRu: 'Чайник',
    emoji: '⚱️',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 68,
    upgradeCostCoins: 42,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · На чайном столике',
    descriptionRu: 'Начищенный до зеркального блеска медный чайник, пускающий ароматные колечки пара.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · Снижает усталость на 2 Энергии',
    reducesExhaustionCostBy: 2,
  },
  {
    id: 'den_patchwork_quilt',
    titleRu: 'Лоскутное одеяло мельничихи',
    shortTitleRu: 'Одеяло',
    emoji: '🛌',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 72,
    upgradeCostCoins: 45,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · На спальной тахте',
    descriptionRu: 'Тёплое стёганое одеяло из бархатных и льняных лоскутков, сшитое мельничихой Мартой.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +2 монеты за поход',
    bonusCoinsPerJourney: 2,
  },
  {
    id: 'den_oak_table',
    titleRu: 'Круглый дубовый столик для карт',
    shortTitleRu: 'Столик',
    emoji: '🪑',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 75,
    upgradeCostCoins: 45,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · Посреди гостиной',
    descriptionRu: 'Основательный стол, на котором вы с Шанталь раскладываете карты лесных маршрутов.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +2 монеты за поход',
    bonusCoinsPerJourney: 2,
  },
  {
    id: 'den_stone_hearth',
    titleRu: 'Каменный очаг с кованой решёткой',
    shortTitleRu: 'Очаг',
    emoji: '🔥',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 78,
    upgradeCostCoins: 48,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · Сердце дома',
    descriptionRu: 'Настоящий сложенный из речного камня камин с узорчатой решёткой.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · Снижает усталость на 3 Энергии',
    reducesExhaustionCostBy: 3,
  },
  {
    id: 'den_wall_clock',
    titleRu: 'Ходики с резной совой',
    shortTitleRu: 'Ходики',
    emoji: '🕰️',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 82,
    upgradeCostCoins: 50,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · В простенке у окна',
    descriptionRu: 'Старинные часы с латунными гирьками на цепочках мерно отсчитывают минуты до возвращения.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +2 монеты за поход',
    bonusCoinsPerJourney: 2,
  },
  {
    id: 'den_book_nook',
    titleRu: 'Книжный шкафчик со сказками',
    shortTitleRu: 'Книжный шкаф',
    emoji: '📚',
    category: 'DEN_COMFORT',
    denStep: 2,
    priceCoins: 88,
    upgradeCostCoins: 52,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень II · Домашняя библиотека',
    descriptionRu: 'Стеллаж с кожаными фолиантами, гербариями и летописью ваших приключений.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +3 монеты за поход',
    bonusCoinsPerJourney: 3,
  },

  // --- СТУПЕНЬ III: Предметы 21–30 (Переход с Уровня III на Уровень IV «Сказочный терем» · 80–115 Монет) ---
  {
    id: 'den_velvet_cushion',
    titleRu: 'Изумрудная бархатная подушка с кистями',
    shortTitleRu: 'Подушка',
    emoji: '💎',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 80,
    upgradeCostCoins: 50,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · На ковре у камина',
    descriptionRu: 'Роскошная подушка изумрудного бархата с золотыми кистями — любимое королевское место Шанталь.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +2 монеты за поход',
    bonusCoinsPerJourney: 2,
  },
  {
    id: 'den_music_box',
    titleRu: 'Заводная музыкальная шкатулка',
    shortTitleRu: 'Музыкальная шкатулка',
    emoji: '🎶',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 85,
    upgradeCostCoins: 52,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · На каминной полке',
    descriptionRu: 'Серебряный валик наигрывает забытую колыбельную северного королевского дома.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +2 монеты за поход',
    bonusCoinsPerJourney: 2,
  },
  {
    id: 'den_crystal_chandeliers',
    titleRu: 'Подвесные кристаллы-светильники',
    shortTitleRu: 'Кристаллы',
    emoji: '🔮',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 92,
    upgradeCostCoins: 55,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · Под сводом крыши',
    descriptionRu: 'Парящие на кованых цепях кристаллы и сферы, заливающие терем мягким золотисто-голубым сиянием.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +3 монеты за поход',
    bonusCoinsPerJourney: 3,
  },
  {
    id: 'den_alchemy_table',
    titleRu: 'Дубовый алхимический стол',
    shortTitleRu: 'Стол алхимии',
    emoji: '⚗️',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 95,
    upgradeCostCoins: 58,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · Мастерская оберегов',
    descriptionRu: 'Резной стол со склянками, ступкой и пергаментами для создания мощных дорожных талисманов.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · Крафт оберега даёт +75% монет',
    reducesExhaustionCostBy: 3,
    bonusCoinsPerJourney: 3,
  },
  {
    id: 'den_map_tapestry',
    titleRu: 'Гобелен королевских рубежей',
    shortTitleRu: 'Гобелен-карта',
    emoji: '🗺️',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 98,
    upgradeCostCoins: 60,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · На каменной кладке',
    descriptionRu: 'Вышитая золотой нитью старинная карта перевалов и забытых твердынь.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +3 монеты за поход',
    bonusCoinsPerJourney: 3,
  },
  {
    id: 'den_royal_casket_stand',
    titleRu: 'Резной постамент для шкатулки улик',
    shortTitleRu: 'Постамент',
    emoji: '👑',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 102,
    upgradeCostCoins: 62,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · Почётное место реликвий',
    descriptionRu: 'Высокая резная тумба с бархатной подушечкой для хранения королевских печатей и медальонов.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +3 монеты за поход',
    bonusCoinsPerJourney: 3,
  },
  {
    id: 'den_stained_window',
    titleRu: 'Стрельчатый витраж «Звёздная ночь»',
    shortTitleRu: 'Витраж',
    emoji: '🪟',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 105,
    upgradeCostCoins: 65,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · Сводчатое окно',
    descriptionRu: 'Цветное стекло с изображением полной луны и созвездий играет радужными бликами на полу.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +3 монеты за поход',
    bonusCoinsPerJourney: 3,
  },
  {
    id: 'den_alchemist_library',
    titleRu: 'Сводчатый стеллаж манускриптов',
    shortTitleRu: 'Архив свитков',
    emoji: '📜',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 108,
    upgradeCostCoins: 65,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · Угловая галерея',
    descriptionRu: 'Высокие полки до самого потолка со светящимися эликсирами и древними хрониками.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +3 монеты за поход',
    bonusCoinsPerJourney: 3,
  },
  {
    id: 'den_star_astrolabe',
    titleRu: 'Латунный телескоп и астролябия',
    shortTitleRu: 'Телескоп',
    emoji: '🔭',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 110,
    upgradeCostCoins: 70,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · У витражного окна',
    descriptionRu: 'Большой астрономический телескоп на треноге для наблюдения за магическими созвездиями.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +4 монеты за поход и шанс улики',
    bonusCoinsPerJourney: 4,
  },
  {
    id: 'den_guardian_gramophone',
    titleRu: 'Старинный медный граммофон',
    shortTitleRu: 'Граммофон',
    emoji: '🎺',
    category: 'DEN_COMFORT',
    denStep: 3,
    priceCoins: 115,
    upgradeCostCoins: 75,
    maxTier: 3,
    visualSlotLabelRu: 'Ступень III · Вечерний салон',
    descriptionRu: 'Редчайший механизм с раструбом в форме золотого цветка, наполняющий дом тёплым звучанием лютни.',
    gameplayEffectRu: '+1 к уровню жилья (X/10) · +4 монеты за поход',
    bonusCoinsPerJourney: 4,
  },
];

export function getShopItemEmoji(item: ChantalShopItem): string {
  return item.emoji || '🕯️';
}

export function getShopItemShortTitle(item: ChantalShopItem): string {
  return item.shortTitleRu || item.titleRu.split(' ')[0];
}

export function getShopItemCategoryLabelRu(category: ShopCategory): string {
  if (category === 'APPAREL') return '🎒 Снаряжение в поход';
  if (category === 'DEN_COMFORT') return '🏠 Уют в Домик (+1 к X/10)';
  return '🥧 Припас в узелок / Домой';
}

export function getShopItemProperties(
  item: ChantalShopItem,
  currentTier: number,
  remainingDurability?: number
): LootItemPropertyDescriptor[] {
  const effectiveTier = Math.max(1, currentTier || 1);
  if (item.category === 'APPAREL') {
    const mishapPct = (item.preventsMishapBonusPct ?? 10) + (effectiveTier - 1) * 8;
    const coinMultPct = (item.expeditionCoinMultiplierPct ?? 0) + (effectiveTier - 1) * 5;
    const trophyChancePct = item.denTrophyChanceBonusPct ?? 0;
    const maxDur = item.maxDurability ?? 5;
    const remDur = remainingDurability ?? maxDur;
    return [
      {
        id: 'shop_prop_slot',
        shortTitleRu: 'Свойство I · В поход',
        statTagRu:
          trophyChancePct > 0
            ? `+${trophyChancePct}% шанс вещи в Дом`
            : coinMultPct > 0
            ? `+${coinMultPct}% монет в пути`
            : `Защита от непогоды`,
        popupTitleRu: 'Полезность при сборе в поход',
        popupDescriptionRu: `Когда вы берёте «${item.titleRu}» с собой в поход, ИИ-рассказчик вплетает этот предмет в сюжет истории! ${item.gameplayEffectRu}`,
        accentColorHex: '#38BDF8',
      },
      {
        id: 'shop_prop_protection',
        shortTitleRu: 'Свойство II · Надёжность',
        statTagRu: `-${mishapPct}% передряг · Заряд ${remDur}/${maxDur}`,
        popupTitleRu: 'Защита в дороге и запас прочности',
        popupDescriptionRu: `Снижает вероятность попасть под ливень или в долг на ${mishapPct}%. Одного заряда хватает на ${maxDur} походов, после чего предмет можно легко заправить/починить за ${item.refillCostCoins ?? 12} Монет.`,
        accentColorHex: '#10B981',
      },
      {
        id: 'shop_prop_income',
        shortTitleRu: 'Свойство III · Мастерство',
        statTagRu: `Тир ${effectiveTier}/${item.maxTier ?? 3}`,
        popupTitleRu: 'Улучшение снаряжения (Тир 1 → 3)',
        popupDescriptionRu:
          'Прокачка Тира усиливает защиту, доход монет и восстанавливает полный запас прочности снаряжения.',
        accentColorHex: '#F59E0B',
      },
    ];
  }

  if (item.category === 'DEN_COMFORT') {
    const energySave = item.reducesExhaustionCostBy ?? 0;
    const bonusCoins = (item.bonusCoinsPerJourney ?? 1) + (effectiveTier - 1) * 1;
    return [
      {
        id: 'shop_prop_den_tier',
        shortTitleRu: 'Свойство I · Счётчик жилья',
        statTagRu: '+1 из 10 к Уровню Домика',
        popupTitleRu: `${item.visualSlotLabelRu} (+1 к X/10)`,
        popupDescriptionRu:
          'Каждые 10 предметов уюта (купленных в лавке или принесённых Шанталь из походов) повышают Уровень жилья и открывают новую картину Домика (I → II → III → IV)!',
        accentColorHex: '#10B981',
      },
      {
        id: 'shop_prop_den_effect',
        shortTitleRu: 'Свойство II · Бонус дома',
        statTagRu:
          energySave > 0
            ? `-${energySave} Эн. усталости`
            : `+${bonusCoins} М / поход`,
        popupTitleRu: 'Постоянный эффект уюта',
        popupDescriptionRu: item.gameplayEffectRu,
        accentColorHex: '#F59E0B',
      },
      {
        id: 'shop_prop_den_upgrade',
        shortTitleRu: 'Свойство III · Тир уюта',
        statTagRu: `Тир ${effectiveTier}/${item.maxTier ?? 3}`,
        popupTitleRu: 'Улучшение предмета интерьера',
        popupDescriptionRu:
          'Повышение Тира увеличивает ежедневный пассивный бонус монет от обустроенного дома.',
        accentColorHex: '#A855F7',
      },
    ];
  }

  return [
    {
      id: 'shop_prop_instant',
      shortTitleRu: 'Свойство I · В узелок в поход',
      statTagRu: item.denTrophyChanceBonusPct
        ? `+${item.denTrophyChanceBonusPct}% шанс вещи в Дом`
        : item.expeditionCoinMultiplierPct
        ? `+${item.expeditionCoinMultiplierPct}% монет обмена`
        : 'Щит от усталости в пути',
      popupTitleRu: 'Зачем брать с собой в поход',
      popupDescriptionRu:
        item.backpackEffectRu ||
        'Можно положить Шанталь в узелок перед походом — она использует гостинец в пути или обменяет его у жителей леса!',
      accentColorHex: '#38BDF8',
    },
    {
      id: 'shop_prop_reusable',
      shortTitleRu: 'Свойство II · Дома у камина',
      statTagRu: `${item.priceCoins} Монет`,
      popupTitleRu: 'Применение дома или в дороге',
      popupDescriptionRu: item.gameplayEffectRu,
      accentColorHex: '#10B981',
    },
  ];
}

export function getMerchantOfferEmoji(offer: TravelingMerchantOffer): string {
  if (offer.rewardType === 'DEN_TROPHY_CRATE') return '🏮';
  if (offer.rewardType === 'ROYAL_CLUE_MAP') return '🗺️';
  if (offer.rewardType === 'ROAD_AMULET') return '🧿';
  return '🌿';
}

export function getMerchantOfferShortTitle(offer: TravelingMerchantOffer): string {
  if (offer.rewardType === 'DEN_TROPHY_CRATE') return 'Ночник в Домик';
  if (offer.rewardType === 'ROYAL_CLUE_MAP') return 'Карта троп';
  if (offer.rewardType === 'ROAD_AMULET') return 'Оберег ветра';
  return 'Связка трав';
}

export function getMerchantOfferProperties(
  offer: TravelingMerchantOffer
): LootItemPropertyDescriptor[] {
  return [
    {
      id: 'merch_prop_discount',
      shortTitleRu: 'Свойство I · Скидка фургона',
      statTagRu: `${offer.discountPriceCoins} М вместо ${offer.originalPriceCoins} М`,
      popupTitleRu: 'Выгода Лавки на колёсах',
      popupDescriptionRu: `Особая цена странствующего торговца: экономия ${offer.originalPriceCoins - offer.discountPriceCoins} Монет по сравнению с обычной лавкой.`,
      accentColorHex: '#F59E0B',
    },
    {
      id: 'merch_prop_reward',
      shortTitleRu: 'Свойство II · Редкий дар',
      statTagRu:
        offer.rewardType === 'DEN_TROPHY_CRATE'
          ? '+1 из 10 в Уровень Домика'
          : offer.rewardType === 'ROYAL_CLUE_MAP'
          ? 'Улика + Любопытство ↑↑'
          : 'Щит + 50% монет',
      popupTitleRu: offer.titleRu,
      popupDescriptionRu: offer.effectDescriptionRu,
      accentColorHex: '#10B981',
    },
  ];
}

export function getRegionEmoji(regionId: string): string {
  if (regionId === 'reg_old_mill') return '⚙️';
  if (regionId === 'reg_amber_forest') return '🍂';
  if (regionId === 'reg_misty_hollow') return '🌫️';
  if (regionId === 'reg_stone_halls') return '🏛️';
  if (regionId === 'reg_mountain_pass') return '🏔️';
  return '🧭';
}

export function getRegionShortTitle(regionId: string): string {
  if (regionId === 'reg_old_mill') return 'Мельница';
  if (regionId === 'reg_amber_forest') return 'Дубрава';
  if (regionId === 'reg_misty_hollow') return 'Лощина';
  if (regionId === 'reg_stone_halls') return 'Чертоги';
  if (regionId === 'reg_mountain_pass') return 'Перевал';
  return 'Тропа';
}

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
  backpackGearIds?: string[];
  backpackProvisionId?: string | null;
  backpackGearTitlesRu?: string[];
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
    coinsEarned: 19,
    badNewsTitleRu: '',
    debtAmountCoins: 0,
    choices: [
      {
        labelRu: 'Пообещать Хьюго вернуться с фонарём и проверить, не прячется ли ключ от второго дна ларца в старой верше у плотины',
        followUpReactionRu:
          'Шанталь так энергично закивала, что её уши едва не задели край кружки с тёплым молоком. Она тут же подкатила латунную пуговицу к твоей ладони и заговорщицки прищурилась, явно представляя, как вы вдвоём устроите ночную ревизию у плотины. Кажется, старый паромщик Хьюго обзавёлся самым настойчивым пушистым напарником во всём лесу!',
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
      sellPriceCoins: 18,
      foundAtRegionRu: 'Янтарная дубрава',
      foundDateIso: '2026-11-05',
    },
    outcomeType: 'GOOD_REWARD',
    coinsEarned: 18,
    badNewsTitleRu: '',
    debtAmountCoins: 0,
    choices: [
      {
        labelRu: 'Вслух восхититься тем, как ловко она обвела пернатого скрягу вокруг пальца за обычное перо сойки',
        followUpReactionRu:
          'Услышав похвалу, Шанталь горделиво вздёрнула нос и изобразила такой важный купеческий вид, что невозможно было не рассмеяться. Секунду спустя она не выдержала собственной серьёзности, весело фыркнула и принялась катать светящуюся шишку по ковру, словно маленький золотистый фонарик. Если Корвус думал, что перехитрит эту рыжую плутовку, ему придётся брать уроки дипломатии!',
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
      'Первый выход Шанталь к Старой водяная мельнице и знакомство с травником Брамблом.',
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
    coinsEarned: 17,
    badNewsTitleRu: '',
    debtAmountCoins: 0,
    choices: [
      {
        labelRu: 'Укутать её в нагретый у камина плед и тихо спросить, почему серебряный медальон так странно согрелся в её лапах',
        followUpReactionRu:
          'Шанталь замерла, прислушиваясь к потрескиванию поленьев, и вдруг необычно осмысленным, глубоким взглядом посмотрела прямо тебе в глаза. Потом она мягко ткнулась тёплым носом в твою ладонь, словно благодаря за то, что ты не торопишь её с ответами, которых она и сама пока не помнит. Свернувшись калачиком рядом со шкатулкой, она ещё долго прикрывала медальон пушистым хвостом.',
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

export const WOODLAND_DEN_TROPHY_POOL: Array<{
  titleRu: string;
  descriptionRu: string;
}> = [
  {
    titleRu: 'Фонарик со спящим светлячком-духом',
    descriptionRu:
      'Маленький резной фонарик из коры ясеня, внутри которого на мшистой подушечке дремлет лесной светлячок. В сумерках он озаряет подоконник мягким изумрудно-золотым светом и засчитывается (+1 из 10) в уровень Домика!',
  },
  {
    titleRu: 'Резная сосновая шкатулка мельника',
    descriptionRu:
      'Тёплая шкатулка с запахом смолы и муки, которую Шанталь выменяла у водяного колеса. Отлично вписалась в интерьер Домика (+1 из 10 к уровню жилья)!',
  },
  {
    titleRu: 'Глиняный подсвечник «Лесная сова»',
    descriptionRu:
      'Уютный фигурный подсвечник ручной лепки от гнома Торвина. Сразу занял почётное место у камина и приблизил повышение уровня Домика (+1 из 10)!',
  },
  {
    titleRu: 'Ловец звёздных снов из ивовой лозы',
    descriptionRu:
      'Переплетённый серебряной нитью и перьями сойки оберег для окна. Защищает сон Шанталь и идёт в зачёт обустройства жилья (+1 из 10)!',
  },
  {
    titleRu: 'Вышитая подушечка с лунным вереском',
    descriptionRu:
      'Душистая думка, набитая целебными травами Брамбла. Сделала уголок у огня ещё теплее и прибавила +1 к счётчику уровня жилья (X/10)!',
  },
];

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
  backpackGearRu?: string[];
  guaranteedDenTrophy?: boolean;
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
    backpackGearRu = [],
    guaranteedDenTrophy = false,
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
        sellPriceCoins: 16,
        foundAtRegionRu: region.titleRu,
        foundDateIso: nowIso,
      },
      outcomeType: 'GOOD_REWARD',
      coinsEarned: 18,
      badNewsTitleRu: '',
      debtAmountCoins: 0,
      choices: [
        {
          labelRu: `Расспросить в деталях, какое лицо было у ${encounteredNpcRu}, когда она устроила этот переполох`,
          followUpReactionRu:
            'Шанталь тут же вскочила на задние лапы и с таким уморительным видом принялась изображать растерянного собеседника, что у неё самой задрожали усы от смеха. От избытка чувств она сделала кувырок через голову на пледе и гордо положила лапу поверх принесённого жетона. Похоже, твоё поручение разбудило в ней настоящий актёрский талант!',
          traitDeltas: { curiosity: 0, tenderness: 0, playfulness: 0, romanticism: 0, courage: 0, wit: 0 },
        },
        {
          labelRu: 'Покачать головой с улыбкой и проверить, не намочила ли она уши, когда ловила добычу у самого ручья',
          followUpReactionRu:
            'Она притворно возмущённо фыркнула, дескать, настоящие следопыты никогда не падают в воду, но тут же чихнула от попавшей на нос брызги. Виновато прижав уши, лисёнок сама подставила макушку под мягкое полотенце и заурчала от удовольствия. Зато выменянные монеты остались совершенно сухими!',
          traitDeltas: { curiosity: 0, tenderness: 0, playfulness: 0, romanticism: 0, courage: 0, wit: 0 },
        },
        {
          labelRu: 'Спрятать ярмарочный жетон в тайник и налить ей мисочку тёплого молока с вересковым мёдом',
          followUpReactionRu:
            'Глаза Шанталь мгновенно округлились от восторга: вся её напускная важность испарилась при первом же запахе верескового мёда. Она аккуратно вылакала угощение, облизала рыжие усы и уютно устроилась у твоих ног, изредка подрагивая кончиком хвоста во сне. Такие маленькие передышки делают ваш дом самым тёплым местом в лесу.',
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

  const backpackSentenceRu =
    backpackGearRu.length > 0
      ? `Очень кстати пригодились вещи, которые ты положил ей с собой в дорогу (${backpackGearRu.join(', ')}): благодаря им она смело заглянула в укромный тайник под старой кладкой и заключила выгодный лесной обмен! `
      : `В этот раз она отправилась в дорогу налегке: возле старого погреба ей встретился узкий тёмный лаз, куда без путевого фонаря или отмычек соваться было рискованно, но она ловко отыскала обходную тропу. `;

  const pickedDenTrophy =
    WOODLAND_DEN_TROPHY_POOL[totalJourneys % WOODLAND_DEN_TROPHY_POOL.length];

  if (isTransformed) {
    const witcherChoices: StoryChoiceOption[] = [
      {
        labelRu: `Придвинуть два бокала пряного взвара к огню и попросить её прочесть строчку со свитка, который передал ${encounteredNpcRu}`,
        followUpReactionRu:
          'Шанталь задумчиво проводит тонкими пальцами по сургучной печати, и в золотисто-карих глазах мелькает тёплая благодарность за твоё спокойствие. «Знаешь, при дворе все только и делали, что требовали отчётов, а ты просто греешь мне руки после дороги», — тихо говорит она, придвигаясь ближе к твоему плечу. Пламя камина мягко отражается в серебряной оправе перстня, пока вы вместе разбираете старинный шифр.',
        traitDeltas: { curiosity: 1, tenderness: 2, playfulness: 0, romanticism: 2, courage: 0, wit: 0 },
      },
      {
        labelRu: 'Иронично поинтересоваться, не собирается ли её высочество разорить всех стражников у переправы своими княжескими повадками',
        followUpReactionRu:
          'Она притворно ахает, грациозно откидывая со лба огненно-рыжую прядь, и лукаво щурится: «Фи, какая клевета! Между прочим, стражник сам предложил скидку за мою обворожительную улыбку!» Не удержавшись, Шанталь звонко смеётся и шутливо щёлкает тебя по носу пергаментным свитком. С ней решительно невозможно соскучиться даже посреди королевского заговора.',
        traitDeltas: { curiosity: 0, tenderness: 0, playfulness: 2, romanticism: 1, courage: 0, wit: 2 },
      },
      {
        labelRu: 'Разложить карту на дубовом столе и предложить перехватить тех ночных всадников прежде, чем они доберутся до архива',
        followUpReactionRu:
          'Улыбка мгновенно сходит с её лица, уступая место собранной, почти боевой сосредоточенности истинной наследницы северного престола. Одним точным движением она отмечает кинжалом узкую теснину на карте и одобрительно кивает твоей идее. «Именно там мы и устроим им сюрприз на рассвете — пусть узнают, что охотиться на лисицу себе дороже».',
        traitDeltas: { curiosity: 1, tenderness: 0, playfulness: -1, romanticism: 0, courage: 2, wit: 1 },
      },
    ];
    if (isMilestoneChest) {
      witcherChoices.push({
        labelRu: '★ Повернуть серебряный перстень в замке Юбилейного Сундука и открыть его потайное дно',
        followUpReactionRu:
          'Под двойным бархатным дном сундука с тихим звоном расходятся латунные пластины, и комната озаряется мягким сиянием древних рун. Шанталь затаила дыхание, бережно поднимая фамильную реликвию, которую считали утраченной со времён великой бури. Эта находка меняет расклад сил во всей вашей истории!',
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
      storyTextRu: `${returnPrefix}Поправив изумрудный ворот и коснувшись тёплого медальона из Похода #1 на груди, Шанталь — теперь уже в своём истинном девичьем облике с копной огненно-рыжих волос — бесшумно ступила на камни локации «${region.titleRu}». Ветер приносил запах озона и старого вереска, а впереди у костра её уже поджидал ${encounteredNpcRu}. «Я помню ваше давнее обещание у речного затона, ваше высочество», — тихо произнёс он, протягивая ей свиток с печатью северного двора. ${backpackSentenceRu}Одним изящным жестом пальцев она зажгла в ладони крошечную янтарную искру, чтобы прочесть выцветшие строки. Внутри тайника оказалась спрятана редкая реликвия. ${
        outcomeType === 'BAD_DEBT'
          ? 'Однако, чтобы выкупить молчание стражника у переправы, ей пришлось пообещать 20 полновесных монет до следующего рассвета.'
          : outcomeType === 'BAD_EXHAUSTION'
          ? 'Обратный путь через грозовой перевал отнял немало магических сил — на восстановление перед новым походом потребуется 45 единиц Энергии.'
          : 'Расплатившись за редкие сведения, она вернулась домой с кошельком монет и новой находкой.'
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
      lootItem: guaranteedDenTrophy
        ? {
            id: `loot_den_${Date.now()}`,
            titleRu: pickedDenTrophy.titleRu,
            category: 'DEN_TROPHY',
            descriptionRu: pickedDenTrophy.descriptionRu,
            sellPriceCoins: 25,
            foundAtRegionRu: region.titleRu,
            foundDateIso: nowIso,
            placedInDenComfort: true,
          }
        : {
            id: `loot_${Date.now()}`,
            titleRu: isMilestoneChest
              ? `Королевская реликвия Главы #${totalJourneys}: Перстень Серебряной Лилии`
              : 'Перстень с геральдической лилией',
            category: 'ROYAL_CLUE',
            descriptionRu:
              'Тонкое серебряное кольцо, внутри которого выгравирована строчка из колыбельной королевского дома. От камня исходит едва заметное магическое тепло.',
            sellPriceCoins: 45,
            foundAtRegionRu: region.titleRu,
            foundDateIso: nowIso,
          },
      outcomeType,
      coinsEarned: isMilestoneChest ? 38 : 20,
      badNewsTitleRu:
        outcomeType === 'BAD_DEBT'
          ? 'Обещанный выкуп стражника у переправы: долг 20 Монет'
          : outcomeType === 'BAD_EXHAUSTION'
          ? 'Магическое истощение после грозового перевала: нужно 45 Энергии (или бальзам/трава)'
          : '',
      debtAmountCoins: outcomeType === 'BAD_DEBT' ? 20 : 0,
      choices: witcherChoices,
      aiSourceLabel: 'Локальный рассказчик',
    };
  }

  const lootCategoryRoll: LootCategory = guaranteedDenTrophy
    ? 'DEN_TROPHY'
    : isMilestoneChest
    ? 'ROYAL_CLUE'
    : dailyArcStep === 3
    ? 'ROYAL_CLUE'
    : dailyArcStep === 2
    ? 'HERB'
    : 'TRINKET';

  const hobbitChoices: StoryChoiceOption[] = [
    {
      labelRu: `Выбрать из её рыжей шёрстки прицепившийся репейник и тихо спросить, что именно привиделась ей в воде ручья у «${region.titleRu}»`,
      followUpReactionRu:
        'Шанталь сначала смешно морщит нос, когда ты вытаскиваешь колючку, но при словах о ручье вдруг замирает и доверчиво прижимается щекой к твоей ладони. Несколько секунд она внимательно смотрит в огонь камина, словно слышит далёкую мелодию лютни, а затем издаёт тихий, почти человеческий вздох облегчения. Ей явно спокойно от того, что рядом с тобой не нужно бояться этих странных снов.',
      traitDeltas: { curiosity: 0, tenderness: 2, playfulness: 0, romanticism: 1, courage: 0, wit: 0 },
    },
    {
      labelRu: `Поддразнить её, что ${encounteredNpcRu} наверняка отдал находку только ради того, чтобы хитрая лиса не утащила у него ещё и медовую коврижку`,
      followUpReactionRu:
        'Лисёнок возмущённо тявкает, делает театральный прыжок вокруг своей оси и тут же пытается стащить со стола край салфетки, доказывая, что уж коврижку-то она бы добыла безо всяких уговоров! В её карих глазах пляшут такие озорные чертики, что удержаться от смеха просто невозможно. В конце концов она гордо садится на задние лапы и ждёт заслуженного угощения.',
      traitDeltas: { curiosity: 1, tenderness: 0, playfulness: 2, romanticism: 0, courage: 1, wit: 1 },
    },
    {
      labelRu: 'Поднести медный фонарь поближе к принесённой из-под корней дуба находке и поискать на ней следы старинного клейма',
      followUpReactionRu:
        'Шанталь мгновенно забывает про усталость, вскакивает передними лапками на край низкого столика и с видом заправского учёного наклоняет рыжую голову набок. Когда луч фонаря выхватывает тонкую царапину на находке, она взволнованно касается её когтем и вопросительно смотрит на тебя. Кажется, эта пушистая исследовательница готова хоть сейчас бежать обратно за новой зацепкой!',
      traitDeltas: { curiosity: 2, tenderness: 0, playfulness: 0, romanticism: 1, courage: 0, wit: 2 },
    },
  ];
  if (isMilestoneChest) {
    hobbitChoices.push({
      labelRu: '★ Вместе с Шанталь поддеть латунную защёлку Юбилейного Сундука Главы',
      followUpReactionRu:
        'Крышка окованного сундука со звоном откидывается, и Шанталь от неожиданности подпрыгивает на всех четырёх лапах! Внутри среди россыпи полновесных монет тускло блестит королевская печать с лилией, от которой лисёнок не может отвести заворожённого взгляда. Она бережно подталкивает реликвию носом к тебе, празднуя вашу общую юбилейную главу.',
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
    storyTextRu: `${returnPrefix}Едва утренний туман поднялся над травой, маленький рыжий лисёнок Шанталь сама выбрала направление по старой карте и бодро засеменила в сторону локации «${region.titleRu}». По дороге она вспомнила недавний уговор у речного затона из Похода #3 и решила проверить старые приметы у моста. Там её заметил ${encounteredNpcRu}, который как раз разбирал корзины с травами и диковинками. ${backpackSentenceRu}Увидев пушистую гостью с удивительно умными, почти человеческими глазами, он сразу припомнил её прежние проказы и угостил корочкой медового хлеба. Взамен Шанталь помогла ему отыскать закатившуюся под корень дуба старинную вещицу, напрямую связанную с их прошлым разговором. На секунду лисёнок замерла, глядя на своё отражение в чистой воде ручья, словно силясь вспомнить далёкий сон о каменном замке и музыке лютни. ${
      isMilestoneChest
        ? 'В награду за завершение юбилейной главы пути они вместе откопали под корнями окованный латунью Сундук Главы, полный золотых монет и королевских тайн!'
        : outcomeType === 'BAD_DEBT'
        ? 'Правда, вильнув хвостом от радости, она случайно смахнула с прилавка связку глиняных склянок — теперь на нас висит неловкий долг в 20 Монет перед мастером!'
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
    lootItem:
      lootCategoryRoll === 'DEN_TROPHY'
        ? {
            id: `loot_den_${Date.now()}`,
            titleRu: pickedDenTrophy.titleRu,
            category: 'DEN_TROPHY',
            descriptionRu: pickedDenTrophy.descriptionRu,
            sellPriceCoins: 24,
            foundAtRegionRu: region.titleRu,
            foundDateIso: nowIso,
            placedInDenComfort: true,
          }
        : {
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
            sellPriceCoins:
              lootCategoryRoll === 'ROYAL_CLUE' ? 40 : lootCategoryRoll === 'HERB' ? 16 : 18,
            foundAtRegionRu: region.titleRu,
            foundDateIso: nowIso,
            canHealExhaustion: lootCategoryRoll === 'HERB',
          },
    outcomeType,
    coinsEarned: isMilestoneChest ? 34 : 18,
    badNewsTitleRu:
      outcomeType === 'BAD_DEBT'
        ? 'Разбитые склянки на прилавке: долг 20 Монет (можно оплатить или отработать)'
        : outcomeType === 'BAD_EXHAUSTION'
        ? 'Продрогла под лесным ливнем: на следующий поход нужно 45 Энергии (или тёплый бальзам / лесная трава)'
        : '',
    debtAmountCoins: outcomeType === 'BAD_DEBT' ? 20 : 0,
    choices: hobbitChoices,
    aiSourceLabel: 'Локальный рассказчик',
  };
}
