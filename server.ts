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

export interface StoryGenerationPayload {
  provider: 'grok' | 'gemini';
  difficulty: 'EASY' | 'MEDIUM' | 'HARD' | 'SUPER_HARD';
  stageNumber: 1 | 2 | 3 | 4;
  isTransformed: boolean;
  totalJourneys: number;
  dailyArcStep: 1 | 2 | 3;
  timeOfDayRu: string;
  regionTitleRu: string;
  regionLoreRu: string;
  encounteredNpcRu: string;
  equippedItemsRu: string[];
  denItemsRu: string[];
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
  hasEquippedLantern?: boolean;
  hasEquippedSatchel?: boolean;
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
        'Законченная художественная история примерно из 9-11 предложений на русском языке. Каждый раз разная структура (диалог, загадка, сценка, тайна, приключение).',
    },
    chantalMapNoteRu: {
      type: Type.STRING,
      description:
        'Короткая заметка на карте: куда именно свернула Шанталь и кого/что она там встретила (1-2 предложения).',
    },
    lootItem: {
      type: Type.OBJECT,
      properties: {
        titleRu: { type: Type.STRING, description: 'Название найденного предмета.' },
        category: {
          type: Type.STRING,
          description: 'TRINKET (диковинка), HERB (целебная находка) или ROYAL_CLUE (странная старинная улика).',
        },
        descriptionRu: {
          type: Type.STRING,
          description: 'Атмосферное описание предмета в инвентаре (2-3 предложения с историей находки).',
        },
        sellPriceCoins: { type: Type.INTEGER, description: 'Цена продажи в монетах (от 12 до 45).' },
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
      description: 'Количество заработанных монет (от 20 до 55).',
    },
    badNewsTitleRu: {
      type: Type.STRING,
      description: 'Если outcomeType = BAD_DEBT или BAD_EXHAUSTION — короткое описание неприятности, иначе пустая строка.',
    },
    debtAmountCoins: {
      type: Type.INTEGER,
      description: 'Если outcomeType = BAD_DEBT — сумма долга (например 25), иначе 0.',
    },
    choices: {
      type: Type.ARRAY,
      description: 'Ровно 3 варианта ответа пользователя на историю.',
      items: {
        type: Type.OBJECT,
        properties: {
          labelRu: { type: Type.STRING, description: 'Текст кнопки ответа (1 предложение).' },
          followUpReactionRu: {
            type: Type.STRING,
            description: 'Живая реакция Шанталь на этот выбор (1-2 предложения).',
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

  const episodeRule = payload.isFillerEpisode
    ? `ЭТО ОДНОРАЗОВЫЙ ПОБОЧНЫЙ ЭПИЗОД (ФИЛЛЕР) по личному поручению пользователя: "${payload.customFillerPrompt || 'Прогуляться и устроить шалость'}". Отыграй это поручение ярко, с юмором и деталями, но не двигай глобальный канонический сюжет.`
    : `ЭТО КАНОНИЧЕСКИЙ СЮЖЕТНЫЙ ПОХОД (Шаг дневной арки ${payload.dailyArcStep} из 3: ${
        payload.dailyArcStep === 1 ? 'Утренняя завязка' : payload.dailyArcStep === 2 ? 'Дневная кульминация' : 'Вечерняя развязка'
      }). Шанталь сама выбрала путь на карте: "${payload.regionTitleRu}" (${payload.regionLoreRu}) и встретила там: ${payload.encounteredNpcRu}.`;

  const customLoreSection = payload.customLoreText
    ? `\nДОПОЛНИТЕЛЬНЫЙ ФАЙЛ ЛОРА И ПЕРСОНАЛИЗАЦИИ ОТ ПОЛЬЗОВАТЕЛЯ (обязательно учитывай эти детали в истории):\n"""\n${payload.customLoreText.slice(0, 6000)}\n"""\n`
    : '';

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

Экипировка на Шанталь: ${payload.equippedItemsRu.join(', ') || 'нет'}.
Обустройство домика: ${payload.denItemsRu.join(', ') || 'скромный уголок'}.
Время суток: ${payload.timeOfDayRu}. Всего походов за плечами: ${payload.totalJourneys}.
${customLoreSection}
ТРЕБОВАНИЯ К ИСТОРИИ:
1. Напиши полноценную, литературно завершённую историю на русском языке объёмом около 9–11 предложений (разделяй на 2–3 абзаца для удобства чтения с экрана телефона).
2. Чередуй структуру: используй живые сценки, диалоги со встреченными обитателями леса, неожиданные находки или маленькие детективные загадки.
3. Обязательно опиши интересный предмет (lootItem), который Шанталь принесла с собой в инвентарь, с атмосферным описанием.
4. Верни ответ СТРОГО в формате JSON по заданной схеме.`;
}

app.post('/api/chantal/generate-story', async (req, res) => {
  try {
    const payload = req.body as StoryGenerationPayload;
    const systemInstruction = buildSystemInstruction(payload);
    const userPrompt = payload.isFillerEpisode
      ? `Сгенерируй побочный эпизод (филлер) по заданию игрока: "${payload.customFillerPrompt}". Верни JSON.`
      : `Сгенерируй новую историю путешествия Шанталь в локацию "${payload.regionTitleRu}", встреча: "${payload.encounteredNpcRu}". Верни JSON.`;

    // 1. If Grok (xAI) is selected and XAI_API_KEY is available in environment
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
            {
              role: 'system',
              content:
                systemInstruction +
                '\nОтвечай строго валидным JSON объектом с полями: titleRu, storyTextRu, chantalMapNoteRu, lootItem {titleRu, category, descriptionRu, sellPriceCoins}, outcomeType (GOOD_REWARD | BAD_DEBT | BAD_EXHAUSTION), coinsEarned, badNewsTitleRu, debtAmountCoins, choices (массив из 3 объектов {labelRu, followUpReactionRu, traitDeltas: {curiosity, tenderness, playfulness, romanticism, courage, wit}}).',
            },
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
            source: 'grok-xai',
            story: JSON.parse(content),
          });
        }
      }
    }

    // 2. Default: Server-side Gemini API (@google/genai)
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
      source: payload.provider === 'grok' ? 'gemini-fallback-for-grok' : 'gemini-3.8-flash',
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
