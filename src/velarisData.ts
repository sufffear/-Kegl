export type VelarisThemeId = 'theme_dawn' | 'theme_sky' | 'theme_aurora' | 'theme_sunset' | 'theme_sakura';

export interface VelarisPalette {
  id: VelarisThemeId;
  titleRu: string;
  subtitleRu: string;
  bgTop: string;
  bgBottom: string;
  surface: string;
  surfaceElevated: string;
  primaryAccent: string;
  secondaryAccent: string;
  successAccent: string;
  warningAccent: string;
  selectedContainer: string;
  selectedBorder: string;
  textPrimary: string;
  textSecondary: string;
  outline: string;
}

export const VELARIS_THEMES: Record<VelarisThemeId, VelarisPalette> = {
  theme_dawn: {
    id: 'theme_dawn',
    titleRu: 'Apple Чистый Белый',
    subtitleRu: 'Белоснежные карточки, воздушный жемчужный фон и свежий изумруд',
    bgTop: '#F5F8F7',
    bgBottom: '#E6EFEA',
    surface: '#FFFFFF',
    surfaceElevated: '#F2F7F5',
    primaryAccent: '#10B981',
    secondaryAccent: '#059669',
    successAccent: '#059669',
    warningAccent: '#D97706',
    selectedContainer: '#D1FAE5',
    selectedBorder: '#10B981',
    textPrimary: '#111827',
    textSecondary: '#4B5563',
    outline: '#CBD5E1',
  },
  theme_sky: {
    id: 'theme_sky',
    titleRu: 'Samsung Небесный',
    subtitleRu: 'Светлый облачно-голубой стиль One UI, чистый белый и небесная лазурь',
    bgTop: '#F2F7FF',
    bgBottom: '#E0ECFC',
    surface: '#FFFFFF',
    surfaceElevated: '#EEF5FD',
    primaryAccent: '#007AFF',
    secondaryAccent: '#0284C7',
    successAccent: '#059669',
    warningAccent: '#D97706',
    selectedContainer: '#DBEAFE',
    selectedBorder: '#007AFF',
    textPrimary: '#0F172A',
    textSecondary: '#475569',
    outline: '#CBD5E1',
  },
  theme_aurora: {
    id: 'theme_aurora',
    titleRu: 'Свежая Мята',
    subtitleRu: 'Воздушный мятно-шалфейный оттенок, белые поверхности и живая зелень',
    bgTop: '#F0FDF4',
    bgBottom: '#DCFCE7',
    surface: '#FFFFFF',
    surfaceElevated: '#ECFDF3',
    primaryAccent: '#16A34A',
    secondaryAccent: '#0D9488',
    successAccent: '#15803D',
    warningAccent: '#D97706',
    selectedContainer: '#DCFCE7',
    selectedBorder: '#16A34A',
    textPrimary: '#0F291E',
    textSecondary: '#3F6252',
    outline: '#BBF7D0',
  },
  theme_sunset: {
    id: 'theme_sunset',
    titleRu: 'Утренний Персик',
    subtitleRu: 'Тёплый кремово-жемчужный фон, чистый белый и мягкий солнечный коралл',
    bgTop: '#FFF8F4',
    bgBottom: '#FDECE3',
    surface: '#FFFFFF',
    surfaceElevated: '#FFF3EC',
    primaryAccent: '#EA580C',
    secondaryAccent: '#D97706',
    successAccent: '#059669',
    warningAccent: '#D97706',
    selectedContainer: '#FFEDD5',
    selectedBorder: '#EA580C',
    textPrimary: '#1F140E',
    textSecondary: '#5C473C',
    outline: '#FED7AA',
  },
  theme_sakura: {
    id: 'theme_sakura',
    titleRu: 'Воздушная Лаванда',
    subtitleRu: 'Нежная утренняя сирень, белоснежные карточки и чистый ирисовый акцент',
    bgTop: '#F9F6FF',
    bgBottom: '#EFE8FD',
    surface: '#FFFFFF',
    surfaceElevated: '#F4EFFE',
    primaryAccent: '#7C3AED',
    secondaryAccent: '#6366F1',
    successAccent: '#059669',
    warningAccent: '#D97706',
    selectedContainer: '#EDE9FE',
    selectedBorder: '#7C3AED',
    textPrimary: '#171126',
    textSecondary: '#4E4366',
    outline: '#DDD6FE',
  },
};

export type PlanGroupCode = 'MORNING' | 'NOON' | 'EVENING';

export interface DailySlotItem {
  id: string;
  groupCode: PlanGroupCode;
  groupTitleRu: string;
  titleRu: string;
  contractionSec: number;
  relaxationSec: number;
  repetitions: number;
}

export const REAL_DAILY_SLOTS: DailySlotItem[] = [
  // Утро
  { id: 'real_v2_morning_fast', groupCode: 'MORNING', groupTitleRu: 'Утро', titleRu: 'Быстрая тренировка', contractionSec: 1, relaxationSec: 1, repetitions: 100 },
  { id: 'real_v2_morning_slow', groupCode: 'MORNING', groupTitleRu: 'Утро', titleRu: 'Силовой контроль', contractionSec: 10, relaxationSec: 5, repetitions: 10 },
  { id: 'real_v2_morning_endurance', groupCode: 'MORNING', groupTitleRu: 'Утро', titleRu: 'Выносливость', contractionSec: 25, relaxationSec: 10, repetitions: 10 },
  // Обед
  { id: 'real_v2_noon_fast', groupCode: 'NOON', groupTitleRu: 'Обед', titleRu: 'Быстрая тренировка', contractionSec: 1, relaxationSec: 1, repetitions: 100 },
  { id: 'real_v2_noon_slow', groupCode: 'NOON', groupTitleRu: 'Обед', titleRu: 'Силовой контроль', contractionSec: 10, relaxationSec: 5, repetitions: 10 },
  { id: 'real_v2_noon_endurance', groupCode: 'NOON', groupTitleRu: 'Обед', titleRu: 'Выносливость', contractionSec: 25, relaxationSec: 10, repetitions: 10 },
  // Вечер
  { id: 'real_v2_evening_fast', groupCode: 'EVENING', groupTitleRu: 'Вечер', titleRu: 'Быстрая тренировка', contractionSec: 1, relaxationSec: 1, repetitions: 100 },
  { id: 'real_v2_evening_slow', groupCode: 'EVENING', groupTitleRu: 'Вечер', titleRu: 'Силовой контроль', contractionSec: 10, relaxationSec: 5, repetitions: 10 },
  { id: 'real_v2_evening_endurance', groupCode: 'EVENING', groupTitleRu: 'Вечер', titleRu: 'Выносливость', contractionSec: 25, relaxationSec: 10, repetitions: 10 },
];

export interface FreeIntervalTemplate {
  slotIndex: number;
  name: string;
  contractionSec: number;
  relaxationSec: number;
  targetMinutes: number;
  targetSeconds: number;
}

export const INITIAL_FREE_TEMPLATES: FreeIntervalTemplate[] = [
  { slotIndex: 0, name: 'Быстрый', contractionSec: 2, relaxationSec: 2, targetMinutes: 2, targetSeconds: 0 },
  { slotIndex: 1, name: 'Медленный', contractionSec: 10, relaxationSec: 5, targetMinutes: 3, targetSeconds: 0 },
  { slotIndex: 2, name: 'Длительное удержание', contractionSec: 20, relaxationSec: 10, targetMinutes: 5, targetSeconds: 0 },
  { slotIndex: 3, name: 'Мой вариант 4', contractionSec: 5, relaxationSec: 5, targetMinutes: 2, targetSeconds: 30 },
  { slotIndex: 4, name: 'Мой вариант 5', contractionSec: 15, relaxationSec: 10, targetMinutes: 4, targetSeconds: 10 },
];

export interface CompletedSessionRecord {
  id: string;
  localDate: string;
  slotId?: string;
  titleRu: string;
  isMandatory: boolean;
  durationSeconds: number;
  holdSeconds: number;
}

export interface StreakMilestone {
  targetDays: number;
  titleRu: string;
  badgeEmoji: string;
  descriptionRu: string;
}

export const MILESTONES: StreakMilestone[] = [
  { targetDays: 3, titleRu: 'Искра ритма', badgeEmoji: '🔥', descriptionRu: '3 дня подряд: запуск нейромышечного отклика и включение базовой привычки.' },
  { targetDays: 7, titleRu: 'Железная неделя', badgeEmoji: '⚡', descriptionRu: '7 дней подряд: полная неделя дисциплины и уверенный контроль быстрых сжиманий.' },
  { targetDays: 14, titleRu: 'Двойной контур', badgeEmoji: '🛡️', descriptionRu: '14 дней подряд: синхронное укрепление быстрых и медленных мышечных волокон.' },
  { targetDays: 21, titleRu: 'Нейро-привычка', badgeEmoji: '💎', descriptionRu: '21 день подряд: тренировки стали автоматическим утренним, дневным и вечерним ритуалом.' },
  { targetDays: 30, titleRu: 'Мастер контроля', badgeEmoji: '👑', descriptionRu: '30 дней подряд: месяц без обрыва цепи и высокая выносливость на 25-секундных удержаниях.' },
  { targetDays: 60, titleRu: 'Титан тонуса', badgeEmoji: '🏆', descriptionRu: '60 дней подряд: глубокий мышечный тонус и эталонная стабильность.' },
  { targetDays: 100, titleRu: 'Легенда Velaris', badgeEmoji: '🌟', descriptionRu: '100 дней подряд: высший титул дисциплины и мастерства Velaris.' },
];

export function formatDurationRu(seconds: number): string {
  const s = Math.max(0, Math.round(seconds));
  if (s === 0) return '0 с';
  const min = Math.floor(s / 60);
  const rem = s % 60;
  if (min === 0) return `${rem} с`;
  if (rem === 0) return `${min} мин`;
  return `${min}м ${rem}с`;
}

export function createInitialHistory(todayIso: string = '2026-11-06'): CompletedSessionRecord[] {
  const records: CompletedSessionRecord[] = [];
  const base = new Date(`${todayIso}T12:00:00Z`);

  const patternByOffset: Record<number, { mandatoryCount: number; extraCount: number }> = {
    0: { mandatoryCount: 4, extraCount: 1 }, // Сегодня (6 нояб.) — Утро закрыто + 1 Обед + 1 Доп.
    1: { mandatoryCount: 9, extraCount: 0 }, // 5 нояб. — Идеальный день (9 из 9)
    2: { mandatoryCount: 6, extraCount: 1 }, // 4 нояб. — 2 этапа + 1 доп
    3: { mandatoryCount: 0, extraCount: 0 }, // 3 нояб. — Замороженный пропуск ❄️
    4: { mandatoryCount: 9, extraCount: 0 }, // 2 нояб.
    5: { mandatoryCount: 5, extraCount: 0 }, // 1 нояб.
    6: { mandatoryCount: 6, extraCount: 1 }, // 31 окт.
    7: { mandatoryCount: 9, extraCount: 0 }, // 30 окт.
    8: { mandatoryCount: 3, extraCount: 1 }, // 29 окт.
    9: { mandatoryCount: 6, extraCount: 0 }, // 28 окт.
    10: { mandatoryCount: 9, extraCount: 1 }, // 27 окт.
    11: { mandatoryCount: 3, extraCount: 0 }, // 26 окт.
    12: { mandatoryCount: 6, extraCount: 0 }, // 25 окт.
    14: { mandatoryCount: 9, extraCount: 0 }, // 23 окт.
    15: { mandatoryCount: 6, extraCount: 1 }, // 22 окт.
    16: { mandatoryCount: 3, extraCount: 0 }, // 21 окт.
    18: { mandatoryCount: 9, extraCount: 0 }, // 19 окт.
    19: { mandatoryCount: 6, extraCount: 0 }, // 18 окт.
    21: { mandatoryCount: 9, extraCount: 1 }, // 16 окт.
    23: { mandatoryCount: 6, extraCount: 0 }, // 14 окт.
    25: { mandatoryCount: 3, extraCount: 1 }, // 12 окт.
    28: { mandatoryCount: 9, extraCount: 0 }, // 9 окт.
    31: { mandatoryCount: 6, extraCount: 0 }, // 6 окт.
    34: { mandatoryCount: 9, extraCount: 1 }, // 3 окт.
    38: { mandatoryCount: 6, extraCount: 0 }, // 29 сент.
    41: { mandatoryCount: 9, extraCount: 0 }, // 26 сент.
    44: { mandatoryCount: 3, extraCount: 1 }, // 23 сент.
  };

  Object.entries(patternByOffset).forEach(([offsetStr, cfg]) => {
    const offset = Number(offsetStr);
    const d = new Date(base.getTime() - offset * 86400000);
    const dateIso = d.toISOString().slice(0, 10);

    for (let i = 0; i < cfg.mandatoryCount; i++) {
      const slot = REAL_DAILY_SLOTS[i % REAL_DAILY_SLOTS.length];
      const holdSec = slot.contractionSec * slot.repetitions;
      const totalSec = (slot.contractionSec + slot.relaxationSec) * slot.repetitions;
      records.push({
        id: `rec_${dateIso}_m_${i}`,
        localDate: dateIso,
        slotId: slot.id,
        titleRu: `${slot.groupTitleRu} · ${slot.titleRu}`,
        isMandatory: true,
        durationSeconds: totalSec,
        holdSeconds: holdSec,
      });
    }

    for (let j = 0; j < cfg.extraCount; j++) {
      records.push({
        id: `rec_${dateIso}_f_${j}`,
        localDate: dateIso,
        titleRu: j % 2 === 0 ? 'Свободный спринт · Мои интервалы' : '⚡ Блиц-подход (60 сек)',
        isMandatory: false,
        durationSeconds: j % 2 === 0 ? 120 : 60,
        holdSeconds: j % 2 === 0 ? 60 : 30,
      });
    }
  });

  return records;
}

// Web Audio API Emotive Synthesizer (matching SoundSynthesisSpec.kt)
let sharedAudioCtx: AudioContext | null = null;

export function playVelarisTone(kind: 'hold' | 'relax' | 'complete' | 'salute') {
  try {
    const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!AudioContextClass) return;
    if (!sharedAudioCtx) {
      sharedAudioCtx = new AudioContextClass();
    }
    if (sharedAudioCtx.state === 'suspended') {
      sharedAudioCtx.resume();
    }

    const now = sharedAudioCtx.currentTime;
    const osc = sharedAudioCtx.createOscillator();
    const subOsc = sharedAudioCtx.createOscillator();
    const gain = sharedAudioCtx.createGain();

    osc.type = 'sine';
    subOsc.type = 'triangle';

    if (kind === 'hold') {
      // Rising emotive tone for contraction
      osc.frequency.setValueAtTime(392, now);
      osc.frequency.exponentialRampToValueAtTime(523.25, now + 0.24);
      subOsc.frequency.setValueAtTime(196, now);
      gain.gain.setValueAtTime(0.001, now);
      gain.gain.linearRampToValueAtTime(0.18, now + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.32);
    } else if (kind === 'relax') {
      // Soft descending release tone for relaxation
      osc.frequency.setValueAtTime(440, now);
      osc.frequency.exponentialRampToValueAtTime(329.63, now + 0.28);
      subOsc.frequency.setValueAtTime(220, now);
      gain.gain.setValueAtTime(0.001, now);
      gain.gain.linearRampToValueAtTime(0.15, now + 0.04);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.36);
    } else {
      // Triumphant chord arpeggio
      osc.frequency.setValueAtTime(523.25, now);
      osc.frequency.setValueAtTime(659.25, now + 0.1);
      osc.frequency.setValueAtTime(783.99, now + 0.2);
      subOsc.frequency.setValueAtTime(261.63, now);
      gain.gain.setValueAtTime(0.001, now);
      gain.gain.linearRampToValueAtTime(0.2, now + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.48);
    }

    osc.connect(gain);
    subOsc.connect(gain);
    gain.connect(sharedAudioCtx.destination);

    osc.start(now);
    subOsc.start(now);
    osc.stop(now + 0.5);
    subOsc.stop(now + 0.5);
  } catch {
    // Ignore audio errors when autoplay is blocked
  }
}
