export type StoryDifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD' | 'SUPER_HARD';
export type AiProviderId = 'grok' | 'gemini';

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
  { key: 'wit', labelRu: 'Остроумие', subtitleRu: 'Находчивость и хитрость', colorHex: '#38BDF8' },
];

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
}

export type ShopCategory = 'APPAREL' | 'DEN_COMFORT' | 'CONSUMABLE';

export interface ChantalShopItem {
  id: string;
  titleRu: string;
  category: ShopCategory;
  priceCoins: number;
  visualSlotLabelRu: string;
  descriptionRu: string;
  gameplayEffectRu: string;
  reducesExhaustionCostBy?: number;
  preventsMishapBonusPct?: number;
  instantAction?: 'HEAL_EXHAUSTION' | 'CLEAR_DEBT' | 'BOOST_MOOD';
}

export const CHANTAL_SHOP_ITEMS: ChantalShopItem[] = [
  // 1. Одежда и снаряжение (визуально на карточке + защита в походах)
  {
    id: 'gear_knitted_scarf',
    titleRu: 'Тёплый вязаный шарф',
    category: 'APPAREL',
    priceCoins: 35,
    visualSlotLabelRu: 'На шее: Шерстяной шарф цвета вереска',
    descriptionRu: 'Мягкий шарф ручной вязки, пахнущий сушёной лавандой и домашним теплом.',
    gameplayEffectRu: 'Защищает от холодного ветра · снижает шанс простудиться в походе на 15%',
    preventsMishapBonusPct: 15,
  },
  {
    id: 'gear_herb_satchel',
    titleRu: 'Кожаная сумка следопыта',
    category: 'APPAREL',
    priceCoins: 60,
    visualSlotLabelRu: 'Через плечо: Тиснёная сумка с пряжкой',
    descriptionRu: 'Лёгкая дорожная сумочка из мягкой кожи с отделениями для трав и диковинок.',
    gameplayEffectRu: '+20% к ценности находок и монет из каждого путешествия',
    preventsMishapBonusPct: 10,
  },
  {
    id: 'gear_brass_lantern',
    titleRu: 'Медный путевой фонарь',
    category: 'APPAREL',
    priceCoins: 85,
    visualSlotLabelRu: 'В лапе: Негасимый медный фонарь',
    descriptionRu: 'Старинный фонарь гномьей работы, освещающий даже самые туманные овраги.',
    gameplayEffectRu: 'Освещает ночные тропы · снижает риск передряг на 25%',
    preventsMishapBonusPct: 25,
  },
  {
    id: 'gear_emerald_cloak',
    titleRu: 'Изумрудный плащ с золотым шитьём',
    category: 'APPAREL',
    priceCoins: 140,
    visualSlotLabelRu: 'На плечах: Изумрудный дорожный плащ',
    descriptionRu: 'Благородный плащ с тонкой золотой вышивкой, удивительно к лицу огненно-рыжей шерсти (и локонам).',
    gameplayEffectRu: 'Вызывает уважение торговцев · +25% монет и защита от непогоды',
    preventsMishapBonusPct: 25,
  },

  // 2. Обустройство домика (визуально меняет уют + снижает штраф восстановления с 45 до 35 энергии)
  {
    id: 'den_heather_blanket',
    titleRu: 'Плед из овечьей шерсти',
    category: 'DEN_COMFORT',
    priceCoins: 40,
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
    visualSlotLabelRu: 'На стене: Резная полка с трофеями',
    descriptionRu: 'Фигурная полка из морёного дуба, где Шанталь бережно раскладывает свои лесные сокровища.',
    gameplayEffectRu: 'Повышает настроение дома · +5 бонусных монет за каждый поход',
  },
  {
    id: 'den_stone_hearth',
    titleRu: 'Каменный очаг с медным котелком',
    category: 'DEN_COMFORT',
    priceCoins: 110,
    visualSlotLabelRu: 'В центре дома: Живой тёплый камелёк',
    descriptionRu: 'Потрескивающие берёзовые дрова и ароматный травяной взвар, согревающий в любую непогоду.',
    gameplayEffectRu: 'Снижает штраф усталости ещё на 5 Энергии (восстановление за 35 вместо 45)',
    reducesExhaustionCostBy: 5,
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

export interface MapRegionNode {
  id: string;
  titleRu: string;
  minJourneysRequired: number;
  coordXPercent: number;
  coordYPercent: number;
  loreRu: string;
  residentNpcRu: string;
  npcRoleRu: string;
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
  },
];

export interface StoryChoiceOption {
  labelRu: string;
  followUpReactionRu: string;
  traitDeltas: ChantalTraits;
}

export interface GeneratedStoryEpisode {
  id: string;
  createdAtIso: string;
  titleRu: string;
  storyTextRu: string;
  chantalMapNoteRu: string;
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
}

export interface ActiveJourneyTimerState {
  startedAtEpochMs: number;
  durationMs: number; // 3_600_000 (1 hour)
  regionId: string;
  regionTitleRu: string;
  encounteredNpcRu: string;
  dailyArcStep: 1 | 2 | 3;
  isFillerEpisode: boolean;
  customFillerPrompt?: string;
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

// Autonomous destination picker so Chantal chooses her own path on the map!
export function pickAutonomousRegionForChantal(totalJourneys: number, lastRegionId?: string): MapRegionNode {
  const unlocked = WORLD_MAP_REGIONS.filter((r) => totalJourneys >= r.minJourneysRequired);
  const pool = unlocked.length > 1 ? unlocked.filter((r) => r.id !== lastRegionId) : unlocked;
  const idx = Math.floor(Math.random() * pool.length);
  return pool[idx] ?? WORLD_MAP_REGIONS[0];
}

// Rich offline/fallback generator so stories always work even without internet
export function buildFallbackStoryEpisode(params: {
  region: MapRegionNode;
  dailyArcStep: 1 | 2 | 3;
  isFillerEpisode: boolean;
  customFillerPrompt?: string;
  isTransformed: boolean;
  totalJourneys: number;
}): GeneratedStoryEpisode {
  const { region, dailyArcStep, isFillerEpisode, customFillerPrompt, isTransformed } = params;
  const nowIso = new Date().toISOString().slice(0, 10);

  if (isFillerEpisode) {
    const taskText = customFillerPrompt?.trim() || 'устроить неожиданную вылазку на ярмарку';
    return {
      id: `ep_filler_${Date.now()}`,
      createdAtIso: nowIso,
      titleRu: `Вне сюжета · Особое поручение`,
      storyTextRu: `Услышав твоё особое напутствие («${taskText}»), Шанталь весело повела ушами и тут же сорвалась с места по боковой тропинке. На полпути к локации «${region.titleRu}» она устроила настоящий переполох: местный ${region.residentNpcRu} только руками развёл, наблюдая за её проделками. Она действовала с такой непосредственностью и азартом, что даже хмурые путники у дороги не смогли сдержать улыбки. В суматохе ей удалось не только в точности выполнить твоё поручение, но и выменять у прохожих горсть звонких монет. Правда, на обратном пути она чуть не уронила свою добычу в ручей, но ловко подхватила её у самой воды. Домой она вернулась невероятно довольная собой, положив перед тобой любопытный сувенир на память об этой выходке.`,
      chantalMapNoteRu: `Шанталь свернула с главной дороги ради твоего поручения в районе «${region.titleRu}» и удивила ${region.residentNpcRu}.`,
      regionId: region.id,
      regionTitleRu: region.titleRu,
      encounteredNpcRu: region.residentNpcRu,
      dailyArcStep,
      isFillerEpisode: true,
      customFillerPrompt: taskText,
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

  // Decide if a "Bad News" event occurs (~22% chance in fallback for variety)
  const roll = Math.random();
  const outcomeType: 'GOOD_REWARD' | 'BAD_DEBT' | 'BAD_EXHAUSTION' =
    roll < 0.14 ? 'BAD_DEBT' : roll < 0.28 ? 'BAD_EXHAUSTION' : 'GOOD_REWARD';

  if (isTransformed) {
    return {
      id: `ep_witcher_${Date.now()}`,
      createdAtIso: nowIso,
      titleRu: `Огни над «${region.titleRu}»`,
      storyTextRu: `Поправив изумрудный ворот и коснувшись тёплого медальона на груди, Шанталь — теперь уже в своём истинном девичьем облике с копной огненно-рыжих волос — бесшумно ступила на камни локации «${region.titleRu}». Ветер приносил запах озона и старого вереска, а впереди у костра её уже поджидал ${region.residentNpcRu}. «Я знал, что заклятие не вечно, ваше высочество», — тихо произнёс он, протягивая ей свиток с печатью северного двора. Шанталь прищурила золотисто-карие глаза, заметив в тени деревьев следы чужих подков — кто-то ещё искал этот архив до её прихода. Одним изящным жестом пальцев она зажгла в ладони крошечную янтарную искру, чтобы прочесть выцветшие строки. Внутри свитка оказался спрятан старинный артефакт, который явно принадлежал её матери-принцессе. ${
        outcomeType === 'BAD_DEBT'
          ? 'Однако, чтобы выкупить молчание стражника у переправы, ей пришлось пообещать 25 полновесных монет до следующего рассвета.'
          : outcomeType === 'BAD_EXHAUSTION'
          ? 'Обратный путь через грозовой перевал отнял немало магических сил — на восстановление перед новым походом потребуется 45 единиц Энергии.'
          : 'Расплатившись за редкие сведения, она вернулась домой с полным кошельком золота и новой зацепкой.'
      }`,
      chantalMapNoteRu: `Шанталь самостоятельно направилась в «${region.titleRu}», где тайно встретилась с персонажем ${region.residentNpcRu}.`,
      regionId: region.id,
      regionTitleRu: region.titleRu,
      encounteredNpcRu: region.residentNpcRu,
      dailyArcStep,
      isFillerEpisode: false,
      lootItem: {
        id: `loot_${Date.now()}`,
        titleRu: 'Перстень с геральдической лилией',
        category: 'ROYAL_CLUE',
        descriptionRu: 'Тонкое серебряное кольцо, внутри которого выгравирована строчка из колыбельной королевского дома. От камня исходит едва заметное магическое тепло.',
        sellPriceCoins: 42,
        foundAtRegionRu: region.titleRu,
        foundDateIso: nowIso,
      },
      outcomeType,
      coinsEarned: 38,
      badNewsTitleRu:
        outcomeType === 'BAD_DEBT'
          ? 'Обещанный выкуп стражнику у переправы: долг 25 Монет'
          : outcomeType === 'BAD_EXHAUSTION'
          ? 'Магическое истощение после грозового перевала: нужно 45 Энергии (или бальзам)'
          : '',
      debtAmountCoins: outcomeType === 'BAD_DEBT' ? 25 : 0,
      choices: [
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
      ],
      aiSourceLabel: 'Локальный рассказчик',
    };
  }

  return {
    id: `ep_hobbit_${Date.now()}`,
    createdAtIso: nowIso,
    titleRu: `Тропинка к локации «${region.titleRu}»`,
    storyTextRu: `Едва утренний туман поднялся над травой, маленький рыжий лисёнок Шанталь сама выбрала направление по старой карте и бодро засеменила в сторону локации «${region.titleRu}». По дороге она то и дело останавливалась, чтобы обнюхать светящиеся шляпки лесных грибов и погоняться за золотистым шмелём. У старого моста её заметил ${region.residentNpcRu}, который как раз разбирал корзины с травами и диковинками. Увидев пушистую гостью с удивительно умными, почти человеческими глазами, он угостил её корочкой медового хлеба. Взамен Шанталь помогла ему отыскать закатившуюся под корень дуба старинную вещицу, которую никто не мог заметить в густом мху. На секунду лисёнок замерла, глядя на своё отражение в чистой воде ручья, словно силясь вспомнить далёкий сон о каменном замке и музыке лютни. ${
      outcomeType === 'BAD_DEBT'
        ? 'Правда, вильнув хвостом от радости, она случайно смахнула с прилавка связку глиняных склянок — теперь на нас висит неловкий долг в 25 Монет перед мастером!'
        : outcomeType === 'BAD_EXHAUSTION'
        ? 'На обратном пути её застал внезапный холодный ливень: Шанталь прибежала домой промокшей и уставшей, поэтому на согревание и следующий поход понадобится 45 Энергии вместо 30.'
        : 'Вытряхнув из шерсти репейник, она гордо выложила на порог заработанные монеты и свою необычную лесную находку.'
    }`,
    chantalMapNoteRu: `Шанталь сама побежала по тропе в «${region.titleRu}» и повстречала там персонажа ${region.residentNpcRu}.`,
    regionId: region.id,
    regionTitleRu: region.titleRu,
    encounteredNpcRu: region.residentNpcRu,
    dailyArcStep,
    isFillerEpisode: false,
    lootItem: {
      id: `loot_${Date.now()}`,
      titleRu:
        dailyArcStep === 3
          ? 'Осколок старинного зеркала в латунной оправе'
          : 'Светящаяся шишка из Янтарной дубравы',
      category: dailyArcStep === 3 ? 'ROYAL_CLUE' : 'TRINKET',
      descriptionRu:
        dailyArcStep === 3
          ? 'На обратной стороне оправы виден крошечный королевский узор. Почему-то Шанталь долго и задумчиво смотрит в него перед сном.'
          : 'Тёплая кедровая шишка, которая мягко светится в темноте, как маленький лесной ночник, и пахнет смолой.',
      sellPriceCoins: dailyArcStep === 3 ? 35 : 22,
      foundAtRegionRu: region.titleRu,
      foundDateIso: nowIso,
    },
    outcomeType,
    coinsEarned: 28,
    badNewsTitleRu:
      outcomeType === 'BAD_DEBT'
        ? 'Разбитые склянки на прилавке: долг 25 Монет (можно оплатить или отработать)'
        : outcomeType === 'BAD_EXHAUSTION'
        ? 'Продрогла под лесным ливнем: на следующий поход нужно 45 Энергии (или тёплый бальзам)'
        : '',
    debtAmountCoins: outcomeType === 'BAD_DEBT' ? 25 : 0,
    choices: [
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
    ],
    aiSourceLabel: 'Локальный рассказчик',
  };
}
