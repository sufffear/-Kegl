import dotenv from 'dotenv';
import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { GoogleGenAI, Type } from '@google/genai';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
app.use(express.json({ limit: '4mb' }));

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY,
  httpOptions: {
    headers: {
      'User-Agent': 'aistudio-build',
    },
  },
});

export interface ThreeTierChroniclePayload {
  fullChroniclePassports: string[];
  npcRelationshipDossier: string[];
  recentThreeFullStories: Array<{
    journeyNumber: number;
    titleRu: string;
    regionTitleRu: string;
    encounteredNpcRu: string;
    storyTextRu: string;
    chosenActionRu: string;
  }>;
}

export interface StoryGenerationPayload {
  provider: 'gemini' | 'openrouter' | 'grok';
  openRouterModel?: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | 'SUPER_HARD';
  stageNumber: 1 | 2 | 3 | 4;
  isTransformed: boolean;
  totalJourneys: number;
  dailyArcStep: 1 | 2 | 3;
  timeOfDayRu: string;
  regionTitleRu: string;
  regionLoreRu: string;
  encounteredNpcRu: string;
  stayedInSameRegion?: boolean;
  autonomousReasonRu?: string;
  equippedItemsRu: string[];
  backpackGearRu?: string[];
  denItemsRu: string[];
  denTierNumber?: number;
  denProgressTextRu?: string;
  guaranteedDenTrophy?: boolean;
  traits: {
    curiosity: number;
    tenderness: number;
    playfulness: number;
    romanticism: number;
    courage: number;
    wit: number;
  };
  isFillerEpisode: boolean;
  customFillerPrompt?: string;
  customLoreText?: string;
  chronicleMemory?: ThreeTierChroniclePayload;
}

const STORY_RESPONSE_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    titleRu: {
      type: Type.STRING,
      description: 'Художественный заголовок истории на русском языке.',
    },
    storyTextRu: {
      type: Type.STRING,
      description:
        'Законченная художественная история примерно из 9-11 предложений на русском языке. Каждый раз разная структура (диалог, загадка, сценка, тайна, приключение). Обязательно обыграй снаряжение и гостинец, которые игрок положил Шанталь с собой в поход (или то, что она пошла налегке)!',
    },
    chantalMapNoteRu: {
      type: Type.STRING,
      description:
        'Короткая заметка на карте: куда именно свернула Шанталь, почему она выбрала этот маршрут (или вернулась туда снова) и кого встретила (1-2 предложения).',
    },
    pastEchoNoteRu: {
      type: Type.STRING,
      description:
        'Короткая пометка для игрока (1 предложение): на какой именно прошлый поход (например Поход #1, #2, #3 или #5) и какой прошлый выбор игрока сделана отсылка в этой главе.',
    },
    lootItem: {
      type: Type.OBJECT,
      properties: {
        titleRu: { type: Type.STRING, description: 'Название найденного предмета.' },
        category: {
          type: Type.STRING,
          description:
            'TRINKET (диковинка), HERB (целебная находка), ROYAL_CLUE (королевская улика) или DEN_TROPHY (уникальный предмет уюта и интерьера для Домика, найденный или выменянный в походе, который сразу засчитывается +1 в счётчик уровня жилья X/10!).',
        },
        descriptionRu: {
          type: Type.STRING,
          description: 'Атмосферное описание предмета в инвентаре (2-3 предложения с историей находки).',
        },
        sellPriceCoins: { type: Type.INTEGER, description: 'Цена продажи в монетах (от 12 до 30).' },
      },
      required: ['titleRu', 'category', 'descriptionRu', 'sellPriceCoins'],
    },
    outcomeType: {
      type: Type.STRING,
      description:
        'GOOD_REWARD (обычная награда), BAD_DEBT (плохая новость: долг в монетах) или BAD_EXHAUSTION (плохая новость: усталость, нужно 45 энергии на следующий поход).',
    },
    coinsEarned: {
      type: Type.INTEGER,
      description:
        'Базовое количество заработанных монет за 1 поход (от 15 до 24 монет, чтобы за 3 сессии полного дня накапливалось ~55-65 монет на 1 предмет для домика).',
    },
    badNewsTitleRu: {
      type: Type.STRING,
      description: 'Если outcomeType = BAD_DEBT или BAD_EXHAUSTION — короткое описание неприятности, иначе пустая строка.',
    },
    debtAmountCoins: {
      type: Type.INTEGER,
      description: 'Если outcomeType = BAD_DEBT — сумма долга (например 20), иначе 0.',
    },
    choices: {
      type: Type.ARRAY,
      description:
        'Ровно 3 нешаблонных варианта ответа пользователя, напрямую привязанных к событиям и деталям этой конкретной истории.',
      items: {
        type: Type.OBJECT,
        properties: {
          labelRu: {
            type: Type.STRING,
            description:
              'Естественная реплика или действие игрока в ответ на историю (1–2 предложения). Без шаблонов и без прямых подсказок, какую характеристику это повысит.',
          },
          followUpReactionRu: {
            type: Type.STRING,
            description:
              'Развёрнутая живая реакция Шанталь на этот выбор из 2–4 полноценных предложений! Иногда смешная, иногда забавная, иногда ироничная, трогательная или неожиданная — строго без шаблонов, с описанием её живой эмоции, жестов или ответа на слова игрока.',
          },
          traitDeltas: {
            type: Type.OBJECT,
            properties: {
              curiosity: { type: Type.INTEGER, description: 'От -1 до +2' },
              tenderness: { type: Type.INTEGER, description: 'От -1 до +2' },
              playfulness: { type: Type.INTEGER, description: 'От -1 до +2' },
              romanticism: { type: Type.INTEGER, description: 'От -1 до +2' },
              courage: { type: Type.INTEGER, description: 'От -1 до +2' },
              wit: { type: Type.INTEGER, description: 'От -1 до +2' },
            },
            required: ['curiosity', 'tenderness', 'playfulness', 'romanticism', 'courage', 'wit'],
          },
        },
        required: ['labelRu', 'followUpReactionRu', 'traitDeltas'],
      },
    },
  },
  required: [
    'titleRu',
    'storyTextRu',
    'chantalMapNoteRu',
    'pastEchoNoteRu',
    'lootItem',
    'outcomeType',
    'coinsEarned',
    'badNewsTitleRu',
    'debtAmountCoins',
    'choices',
  ],
};

function buildSystemInstruction(payload: StoryGenerationPayload): string {
  const difficultyMap: Record<StoryGenerationPayload['difficulty'], string> = {
    EASY: 'Лёгкий уровень: больше уюта, доброго юмора и мягких чудес, минимальный шанс плохих новостей (около 10%).',
    MEDIUM: 'Средний уровень: баланс тёплого юмора, приключенческой интриги и умеренных передряг (около 18% шанс плохой новости).',
    HARD: 'Тяжёлый уровень: острая приключенческая драма, сложные моральные выборы, загадки и заметный риск попасть в долг или устать в пути (около 25%).',
    SUPER_HARD: 'Супер тяжёлый уровень: максимум испытаний, опасные тропы, коварные торговцы и частые сюжетные вызовы (около 35% шанс передряги).',
  };

  const actTone = payload.isTransformed
    ? `АКТ II (После снятия проклятия): Шанталь больше не лисёнок — проклятие пало, и она предстала в своём истинном облике прекрасной рыжеволосой девушки (проклятого дитя принцессы, внешне и по харизме напоминающей Трисс Меригольд). Тон повествования — богатое магическое фэнтези в духе «Ведьмака»: древние чары, королевские тайны, романтика, остроумие и поиски того, кто наложил проклятие.`
    : `АКТ I (До трансформации, стадия роста ${payload.stageNumber} из 3): Шанталь — живой, выразительный рыжий лисёнок. Настроение в духе «Хоббита» — тепло, добрый юмор, чудо, живая драма без давящего мрака. ВАЖНО: НЕ раскрывай прямым текстом, что она заколдованная принцесса-девушка! Лишь изредка оставляй тонкие сказочные полунамёки (необычно осмысленный взгляд, странная реакция на королевские гербы или древние руны).`;

  const autonomousContext = payload.autonomousReasonRu
    ? `Причина выбора маршрута самой Шанталь: ${payload.autonomousReasonRu}`
    : `Шанталь сама выбрала путь на карте: "${payload.regionTitleRu}" (${payload.regionLoreRu}) и встретила там: ${payload.encounteredNpcRu}.`;

  const episodeRule = payload.isFillerEpisode
    ? `ЭТО ОДНОРАЗОВЫЙ ПОБОЧНЫЙ ЭПИЗОД (ФИЛЛЕР) по личному поручению пользователя: "${payload.customFillerPrompt || 'Прогуляться и устроить шалость'}". Отыграй это поручение ярко, с юмором и деталями, но не двигай глобальный канонический сюжет.`
    : `ЭТО КАНОНИЧЕСКИЙ СЮЖЕТНЫЙ ПОХОД #${payload.totalJourneys} (Шаг дневной арки ${payload.dailyArcStep} из 3: ${
        payload.dailyArcStep === 1 ? 'Утренняя завязка' : payload.dailyArcStep === 2 ? 'Дневная кульминация' : 'Вечерняя развязка'
      }). ${autonomousContext}`;

  const mem = payload.chronicleMemory;
  const layer1Passports =
    mem && mem.fullChroniclePassports.length > 0
      ? `\n=== СЛОЙ 1: ПОЛНАЯ ЛЕТОПИСЬ ВСЕХ РЕШЕНИЙ С 1-ГО ПОХОДА (Используй это, чтобы даже в 20-м походе делать живые отсылки к 1-му, 3-му или 5-му походу!) ===\n` +
        mem.fullChroniclePassports.join('\n') +
        '\n'
      : '';

  const layer2NpcDossier =
    mem && mem.npcRelationshipDossier.length > 0
      ? `\n=== СЛОЙ 2: КАРТОТЕКА ОТНОШЕНИЙ С ПЕРСОНАЖАМИ И ОБЕЩАНИЙ ===\n` +
        mem.npcRelationshipDossier.join('\n') +
        '\n'
      : '';

  const layer3RecentFull =
    mem && mem.recentThreeFullStories.length > 0
      ? `\n=== СЛОЙ 3: ПОЛНЫЙ ТЕКСТ ПОСЛЕДНИХ 3 ГЛАВ (ДЛЯ ЕДИНОГО СТИЛЯ И ПРЕЕМСТВЕННОСТИ) ===\n` +
        mem.recentThreeFullStories
          .map(
            (st) =>
              `[Поход #${st.journeyNumber}: «${st.titleRu}» | ${st.regionTitleRu}, ${st.encounteredNpcRu}]\nТекст: ${st.storyTextRu}\nВыбор игрока: «${st.chosenActionRu}»`
          )
          .join('\n\n') +
        '\n'
      : '';

  const customLoreSection = payload.customLoreText
    ? `\n=== ДОПОЛНИТЕЛЬНЫЙ ФАЙЛ ЛОРА И ПЕРСОНАЛИЗАЦИИ ОТ ПОЛЬЗОВАТЕЛЯ ===\n"""\n${payload.customLoreText.slice(0, 6000)}\n"""\n`
    : '';

  const backpackSection =
    payload.backpackGearRu && payload.backpackGearRu.length > 0
      ? `СНАРЯЖЕНИЕ И ГОСТИНЦЫ, ВЗЯТЫЕ С СОБОЙ В ЭТОТ ПОХОД: ${payload.backpackGearRu.join(', ')}. ОБЯЗАТЕЛЬНО ярко обыграй в тексте истории, как именно эти вещи помогли Шанталь в пути (например, фонарь осветил тёмный тайник, отмычки открыли старый ларец, а пирожок или мёд она обменяла у лесного жителя на диковинку или предмет для дома)!`
      : `В этот поход Шанталь отправилась НАЛЕГКЕ (без специального снаряжения в рюкзаке). Можешь упомянуть в сюжете деталь, где ей пригодился бы фонарь, компас или гостинец для обмена.`;

  const denTrophyInstruction = payload.guaranteedDenTrophy
    ? `ВАЖНО ПО НАХОДКЕ (lootItem): В этом походе Шанталь ОБЯЗАТЕЛЬНО находит или выменивает уникальный предмет интерьера для обустройства Домика — укажи lootItem.category = "DEN_TROPHY" (например: «Фонарик со спящим духом-светлячком», «Резная сосновая шкатулка мельника», «Старинный подсвечник из речного затона», «Подушечка с вышивкой лесных трав» и т.п.). Этот предмет сразу встанет в Домик и прибавит +1 к счётчику уровня жилья (${payload.denProgressTextRu || 'X/10'})!`
    : `По находке (lootItem): выбери категорию TRINKET, HERB, ROYAL_CLUE или (с шансом ~25%) DEN_TROPHY — редкую вещь для уюта Домика, которая засчитывается в прогресс уровня жилья (${payload.denProgressTextRu || 'X/10'}).`;

  return `Ты — ведущий нарративный дизайнер ролевой игры о Шанталь.
${actTone}
${difficultyMap[payload.difficulty]}
${episodeRule}

Текущие 6 черт характера Шанталь:
- Любопытство: ${payload.traits.curiosity}
- Нежность: ${payload.traits.tenderness}
- Игривость: ${payload.traits.playfulness}
- Романтичность: ${payload.traits.romanticism}
- Отвага: ${payload.traits.courage}
- Остроумие: ${payload.traits.wit}

${backpackSection}
Обустройство домика (Уровень ${payload.denTierNumber || 1}, прогресс уровня: ${payload.denProgressTextRu || '1/10'}): ${payload.denItemsRu.join(', ') || 'скромный уголок'}.
${denTrophyInstruction}
Время суток: ${payload.timeOfDayRu}. Текущий номер похода: #${payload.totalJourneys}.
${layer1Passports}${layer2NpcDossier}${layer3RecentFull}${customLoreSection}
ТРЕБОВАНИЯ К ИСТОРИИ И 3 ОТВЕТАМ:
1. Напиши полноценную, литературно завершённую историю на русском языке объёмом около 9–11 предложений (разделяй на 2–3 абзаца для удобства чтения с экрана телефона).
2. ПРАВИЛО «ЭХО ПРОШЛОГО»: обязательно вплети в сюжет живую отсылку к одному из более ранних походов из СЛОЯ 1 или СЛОЯ 2 (например, вспомни решение игрока из Похода #1, #2, #3 или #5, данное обещание или прошлую находку) и укажи это в поле pastEchoNoteRu!
3. Обязательно опиши интересный предмет (lootItem), который Шанталь принесла с собой в инвентарь, с атмосферным описанием. Базовые монеты coinsEarned — в диапазоне 15–24 монет.
4. ТРИ ВАРИАНТА ОТВЕТА (choices) И РЕАКЦИИ ШАНТАЛЬ (followUpReactionRu):
   - Все 3 кнопки ответа игрока (labelRu) должны быть НЕШАБЛОННЫМИ, живыми репликами или реакциями на конкретные детали рассказанной истории (без банальных шаблонов и без спойлеров к характеристикам).
   - На КАЖДЫЙ из 3 вариантов ответа обязательно напиши развёрнутую, яркую, НЕШАБЛОННУЮ реакцию самой Шанталь (followUpReactionRu) объёмом в 2–4 полноценных предложения! Реакция должна быть живой — иногда смешной, иногда забавной и лукавой, иногда неожиданной или трогательной, чтобы игрок с удовольствием читал мини-сценку того, как лисёнок (или расколдованная принцесса) отреагировала на его слова.
5. Верни ответ СТРОГО в формате JSON по заданной схеме.`;
}

const JSON_FORMAT_REMINDER =
  '\nОтвечай строго валидным JSON объектом с полями: titleRu, storyTextRu, chantalMapNoteRu, pastEchoNoteRu, lootItem {titleRu, category, descriptionRu, sellPriceCoins}, outcomeType (GOOD_REWARD | BAD_DEBT | BAD_EXHAUSTION), coinsEarned, badNewsTitleRu, debtAmountCoins, choices (массив из 3 объектов {labelRu, followUpReactionRu (2-4 предложения живой, смешной или неожиданной реакции Шанталь без шаблонов!), traitDeltas: {curiosity, tenderness, playfulness, romanticism, courage, wit}}).';

app.post('/api/chantal/generate-story', async (req, res) => {
  try {
    const payload = req.body as StoryGenerationPayload;
    const systemInstruction = buildSystemInstruction(payload);
    const userPrompt = payload.isFillerEpisode
      ? `Сгенерируй побочный эпизод (филлер) по заданию игрока: "${payload.customFillerPrompt}". Верни JSON.`
      : `Сгенерируй новую историю путешествия #${payload.totalJourneys} Шанталь в локацию "${payload.regionTitleRu}", встреча: "${payload.encounteredNpcRu}". ${payload.autonomousReasonRu || ''} Верни JSON.`;

    // 1. OpenRouter Gateway (supports selecting any model: Grok, Claude, DeepSeek, Gemini, Llama, etc.)
    if (payload.provider === 'openrouter' && process.env.OPENROUTER_API_KEY) {
      const targetModel = payload.openRouterModel?.trim() || 'google/gemini-2.5-flash';
      const orResp = await fetch('https://openrouter.ai/api/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${process.env.OPENROUTER_API_KEY}`,
          'HTTP-Referer': process.env.APP_URL || 'http://localhost:3000',
          'X-Title': 'Velaris Chantal Companion',
        },
        body: JSON.stringify({
          model: targetModel,
          response_format: { type: 'json_object' },
          messages: [
            { role: 'system', content: systemInstruction + JSON_FORMAT_REMINDER },
            { role: 'user', content: userPrompt },
          ],
        }),
      });

      if (orResp.ok) {
        const orJson = (await orResp.json()) as {
          choices?: Array<{ message?: { content?: string } }>;
        };
        const content = orJson.choices?.[0]?.message?.content;
        if (content) {
          return res.json({
            source: `OpenRouter (${targetModel})`,
            story: JSON.parse(content),
          });
        }
      }
    }

    // 2. Direct Grok (xAI API) if selected and XAI_API_KEY is configured
    if (payload.provider === 'grok' && process.env.XAI_API_KEY) {
      const grokResp = await fetch('https://api.x.ai/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${process.env.XAI_API_KEY}`,
        },
        body: JSON.stringify({
          model: 'grok-3-mini',
          response_format: { type: 'json_object' },
          messages: [
            { role: 'system', content: systemInstruction + JSON_FORMAT_REMINDER },
            { role: 'user', content: userPrompt },
          ],
        }),
      });

      if (grokResp.ok) {
        const grokJson = (await grokResp.json()) as {
          choices?: Array<{ message?: { content?: string } }>;
        };
        const content = grokJson.choices?.[0]?.message?.content;
        if (content) {
          return res.json({
            source: 'Grok (xAI API)',
            story: JSON.parse(content),
          });
        }
      }
    }

    // 3. Default #1 Primary Path: Direct Gemini API (@google/genai)
    const response = await ai.models.generateContent({
      model: 'gemini-3.8-flash',
      contents: userPrompt,
      config: {
        systemInstruction,
        responseMimeType: 'application/json',
        responseSchema: STORY_RESPONSE_SCHEMA,
      },
    });

    const rawText = (response.text ?? '').trim();
    const parsedStory = JSON.parse(rawText);

    return res.json({
      source:
        payload.provider === 'openrouter'
          ? `Gemini Flash (резерв для OpenRouter ${payload.openRouterModel || ''})`
          : payload.provider === 'grok'
          ? 'Gemini Flash (резерв для Grok)'
          : 'Gemini Flash (Прямой ключ)',
      story: parsedStory,
    });
  } catch (error) {
    console.error('Story generation error:', error);
    return res.status(500).json({
      error: error instanceof Error ? error.message : 'Failed to generate story',
    });
  }
});

async function startServer() {
  const PORT = 3000;

  if (process.env.NODE_ENV !== 'production') {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Velaris & Chantal server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
