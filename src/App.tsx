import React, { useEffect, useMemo, useState } from 'react';
import {
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  Compass,
  Coins,
  Dumbbell,
  Flame,
  Heart,
  Home,
  Pause,
  Play,
  Plus,
  RotateCcw,
  Settings,
  Shield,
  Sparkles,
  Volume2,
  Zap,
} from 'lucide-react';
import ChantalCompanionView from './ChantalCompanionView';
import {
  CompletedSessionRecord,
  createInitialHistory,
  DailySlotItem,
  formatDurationRu,
  FreeIntervalTemplate,
  INITIAL_FREE_TEMPLATES,
  MILESTONES,
  PlanGroupCode,
  playVelarisTone,
  REAL_DAILY_SLOTS,
  StreakMilestone,
  VELARIS_THEMES,
  VelarisThemeId,
} from './velarisData';

const TODAY_ISO = '2026-11-06';

const MONTH_NAMES_RU = [
  'Январь',
  'Февраль',
  'Март',
  'Апрель',
  'Май',
  'Июнь',
  'Июль',
  'Август',
  'Сентябрь',
  'Октябрь',
  'Ноябрь',
  'Декабрь',
];

const MONTH_GENITIVE_SHORT_RU = [
  'янв.',
  'февр.',
  'мар.',
  'апр.',
  'мая',
  'июн.',
  'июл.',
  'авг.',
  'сент.',
  'окт.',
  'нояб.',
  'дек.',
];

const WEEKDAYS_SHORT_RU = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];

interface DayAggregate {
  localDate: string;
  dayNumber: number;
  monthIndex: number;
  year: number;
  shortWeekdayRu: string;
  monthShortRu: string;
  isToday: boolean;
  completedCount: number;
  assignedCount: number;
  plannedCompletedCount: number;
  freeCompletedCount: number;
  totalWorkoutSeconds: number;
  holdSecondsTotal: number;
  closedPeriodsCount: number;
  intensityLevel: 0 | 1 | 2 | 3;
  isFrozen: boolean;
  sessions: CompletedSessionRecord[];
}

interface ActiveWorkoutState {
  titleRu: string;
  contractionSec: number;
  relaxationSec: number;
  totalRepetitions: number;
  currentRepetition: number;
  phase: 'CONTRACTION' | 'RELAXATION';
  phaseRemainingSec: number;
  isPaused: boolean;
  isMandatory: boolean;
  slotId?: string;
}

export default function App() {
  const [themeId, setThemeId] = useState<VelarisThemeId>('theme_dawn');
  const palette = VELARIS_THEMES[themeId];

  const [activeTab, setActiveTab] = useState<'home' | 'plan' | 'chantal' | 'settings'>('chantal');
  const [chantalEnergy, setChantalEnergy] = useState<number>(() => {
    const saved = Number(localStorage.getItem('chantal_energy'));
    return Number.isFinite(saved) && saved >= 0 ? saved : 30;
  });
  const [chantalCoins, setChantalCoins] = useState<number>(() => {
    const saved = Number(localStorage.getItem('chantal_coins'));
    return Number.isFinite(saved) && saved >= 0 ? saved : 65;
  });

  useEffect(() => {
    localStorage.setItem('chantal_energy', String(chantalEnergy));
  }, [chantalEnergy]);

  useEffect(() => {
    localStorage.setItem('chantal_coins', String(chantalCoins));
  }, [chantalCoins]);

  const [historyRecords, setHistoryRecords] = useState<CompletedSessionRecord[]>(() =>
    createInitialHistory(TODAY_ISO)
  );
  const [frozenDates, setFrozenDates] = useState<string[]>(['2026-11-03', '2026-10-24']);

  // Unified Monthly Calendar state
  const [monthOffset, setMonthOffset] = useState<number>(0);
  const [selectedDate, setSelectedDate] = useState<string>(TODAY_ISO);
  const [isHistoryExpanded, setIsHistoryExpanded] = useState<boolean>(false);
  const [inspectedMilestone, setInspectedMilestone] = useState<StreakMilestone | null>(null);

  // Plan screen state
  const [planSubTab, setPlanSubTab] = useState<'scheduled' | 'free'>('scheduled');
  const [selectedGroupCode, setSelectedGroupCode] = useState<PlanGroupCode>('NOON');
  const [freeTemplates, setFreeTemplates] = useState<FreeIntervalTemplate[]>(INITIAL_FREE_TEMPLATES);
  const [editingTemplateIdx, setEditingTemplateIdx] = useState<number | null>(null);

  // Active workout timer state
  const [activeWorkout, setActiveWorkout] = useState<ActiveWorkoutState | null>(null);
  const [isTimerScreenOpen, setIsTimerScreenOpen] = useState<boolean>(false);
  const [soundEnabled, setSoundEnabled] = useState<boolean>(true);

  // Timer tick effect
  useEffect(() => {
    if (!activeWorkout || activeWorkout.isPaused) return;
    const timer = window.setInterval(() => {
      setActiveWorkout((prev) => {
        if (!prev || prev.isPaused) return prev;
        if (prev.phaseRemainingSec > 1) {
          return { ...prev, phaseRemainingSec: prev.phaseRemainingSec - 1 };
        }
        // Switch phase or finish repetition
        if (prev.phase === 'CONTRACTION') {
          if (soundEnabled) playVelarisTone('relax');
          return {
            ...prev,
            phase: 'RELAXATION',
            phaseRemainingSec: prev.relaxationSec,
          };
        } else {
          if (prev.currentRepetition < prev.totalRepetitions) {
            if (soundEnabled) playVelarisTone('hold');
            return {
              ...prev,
              currentRepetition: prev.currentRepetition + 1,
              phase: 'CONTRACTION',
              phaseRemainingSec: prev.contractionSec,
            };
          } else {
            // Completed workout!
            if (soundEnabled) playVelarisTone('complete');
            const holdSec = prev.contractionSec * prev.totalRepetitions;
            const totalSec = (prev.contractionSec + prev.relaxationSec) * prev.totalRepetitions;
            const newRec: CompletedSessionRecord = {
              id: `rec_${Date.now()}`,
              localDate: TODAY_ISO,
              slotId: prev.slotId,
              titleRu: prev.titleRu,
              isMandatory: prev.isMandatory,
              durationSeconds: totalSec,
              holdSeconds: holdSec,
            };
            setHistoryRecords((old) => [newRec, ...old]);
            setChantalEnergy((e) => e + 10);
            setChantalCoins((c) => c + 5);
            setSelectedDate(TODAY_ISO);
            setMonthOffset(0);
            setIsTimerScreenOpen(false);
            return null;
          }
        }
      });
    }, 1000);
    return () => window.clearInterval(timer);
  }, [activeWorkout, soundEnabled]);

  // Aggregate records by date
  const dayAggregatesMap = useMemo(() => {
    const byDate = new Map<string, CompletedSessionRecord[]>();
    historyRecords.forEach((rec) => {
      const list = byDate.get(rec.localDate) ?? [];
      list.push(rec);
      byDate.set(rec.localDate, list);
    });

    const result = new Map<string, DayAggregate>();
    const buildForDate = (dateIso: string): DayAggregate => {
      const d = new Date(`${dateIso}T12:00:00Z`);
      const jsDay = d.getUTCDay(); // 0=Sun..6=Sat
      const isoDay = jsDay === 0 ? 7 : jsDay;
      const sessions = byDate.get(dateIso) ?? [];
      const planned = sessions.filter((s) => s.isMandatory);
      const extra = sessions.filter((s) => !s.isMandatory);
      const totalWorkoutSeconds = sessions.reduce((acc, s) => acc + s.durationSeconds, 0);
      const holdSecondsTotal = sessions.reduce((acc, s) => acc + s.holdSeconds, 0);
      const closedPeriodsCount = Math.min(3, Math.floor(planned.length / 3));
      const isFrozen = frozenDates.includes(dateIso) && sessions.length === 0;

      let intensityLevel: 0 | 1 | 2 | 3 = 0;
      if (sessions.length >= 6 || planned.length >= 9) {
        intensityLevel = 3;
      } else if (sessions.length >= 3 || closedPeriodsCount >= 1) {
        intensityLevel = 2;
      } else if (sessions.length > 0) {
        intensityLevel = 1;
      }

      return {
        localDate: dateIso,
        dayNumber: d.getUTCDate(),
        monthIndex: d.getUTCMonth(),
        year: d.getUTCFullYear(),
        shortWeekdayRu: WEEKDAYS_SHORT_RU[isoDay - 1],
        monthShortRu: MONTH_GENITIVE_SHORT_RU[d.getUTCMonth()],
        isToday: dateIso === TODAY_ISO,
        completedCount: sessions.length,
        assignedCount: 9,
        plannedCompletedCount: Math.min(9, planned.length),
        freeCompletedCount: extra.length,
        totalWorkoutSeconds,
        holdSecondsTotal,
        closedPeriodsCount,
        intensityLevel,
        isFrozen,
        sessions,
      };
    };

    // Populate 6 months around TODAY_ISO
    const base = new Date(`${TODAY_ISO}T12:00:00Z`);
    const start = new Date(Date.UTC(base.getUTCFullYear(), base.getUTCMonth() - 5, 1, 12));
    const end = new Date(Date.UTC(base.getUTCFullYear(), base.getUTCMonth() + 1, 0, 12));

    for (let t = start.getTime(); t <= end.getTime(); t += 86400000) {
      const iso = new Date(t).toISOString().slice(0, 10);
      result.set(iso, buildForDate(iso));
    }

    return result;
  }, [historyRecords, frozenDates]);

  // Compute Streak, Best Streak, and Weekly KPIs
  const overviewStats = useMemo(() => {
    const base = new Date(`${TODAY_ISO}T12:00:00Z`);
    let currentStreak = 0;
    const todayAgg = dayAggregatesMap.get(TODAY_ISO);
    const isTodayActive = (todayAgg?.completedCount ?? 0) > 0;

    let cursor = new Date(base.getTime() - (isTodayActive ? 0 : 86400000));
    while (true) {
      const key = cursor.toISOString().slice(0, 10);
      const agg = dayAggregatesMap.get(key);
      if (agg && agg.completedCount > 0) {
        currentStreak++;
        cursor = new Date(cursor.getTime() - 86400000);
      } else if (agg && agg.isFrozen) {
        cursor = new Date(cursor.getTime() - 86400000);
      } else {
        break;
      }
    }

    const bestStreak = Math.max(currentStreak, 14);

    let weekSessions = 0;
    let weekHoldSeconds = 0;
    let weekTotalSeconds = 0;
    let prevWeekHoldSeconds = 0;

    for (let i = 0; i < 7; i++) {
      const k = new Date(base.getTime() - i * 86400000).toISOString().slice(0, 10);
      const a = dayAggregatesMap.get(k);
      if (a) {
        weekSessions += a.completedCount;
        weekHoldSeconds += a.holdSecondsTotal;
        weekTotalSeconds += a.totalWorkoutSeconds;
      }
    }

    for (let i = 7; i < 14; i++) {
      const k = new Date(base.getTime() - i * 86400000).toISOString().slice(0, 10);
      const a = dayAggregatesMap.get(k);
      if (a) {
        prevWeekHoldSeconds += a.holdSecondsTotal;
      }
    }

    const totalSessionsAllTime = historyRecords.length;
    const totalHoldAllTime = historyRecords.reduce((acc, r) => acc + r.holdSeconds, 0);

    const deltaPct =
      prevWeekHoldSeconds > 0
        ? Math.round(((weekHoldSeconds - prevWeekHoldSeconds) * 100) / prevWeekHoldSeconds)
        : 100;

    const achievedMilestone =
      [...MILESTONES].reverse().find((m) => currentStreak >= m.targetDays) ?? MILESTONES[0];
    const nextMilestone =
      MILESTONES.find((m) => m.targetDays > currentStreak) ?? MILESTONES[MILESTONES.length - 1];
    const prevTarget =
      [...MILESTONES].reverse().find((m) => m.targetDays <= currentStreak)?.targetDays ?? 0;
    const span = Math.max(1, nextMilestone.targetDays - prevTarget);
    const milestoneFraction = Math.min(1, Math.max(0.08, (currentStreak - prevTarget) / span));

    const availableFreezeShields = Math.max(0, 3 - frozenDates.length);

    return {
      currentStreak,
      bestStreak,
      isTodayActive,
      weekSessions,
      weekHoldSeconds,
      weekTotalSeconds,
      totalSessionsAllTime,
      totalHoldAllTime,
      deltaPct,
      rankTitleRu: achievedMilestone.titleRu,
      nextMilestone,
      milestoneFraction,
      availableFreezeShields,
      maxFreezeShields: 3,
    };
  }, [dayAggregatesMap, historyRecords, frozenDates]);

  // Displayed Month Grid calculation
  const displayedMonthData = useMemo(() => {
    const base = new Date(`${TODAY_ISO}T12:00:00Z`);
    const target = new Date(Date.UTC(base.getUTCFullYear(), base.getUTCMonth() + monthOffset, 1, 12));
    const year = target.getUTCFullYear();
    const monthIndex = target.getUTCMonth();
    const daysInMonth = new Date(Date.UTC(year, monthIndex + 1, 0)).getUTCDate();
    const firstJsDay = target.getUTCDay();
    const leadingEmpty = firstJsDay === 0 ? 6 : firstJsDay - 1;

    const cells: Array<DayAggregate | null> = [];
    for (let i = 0; i < leadingEmpty; i++) {
      cells.push(null);
    }
    for (let day = 1; day <= daysInMonth; day++) {
      const iso = `${year}-${String(monthIndex + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      const existing = dayAggregatesMap.get(iso);
      if (existing) {
        cells.push(existing);
      } else {
        const d = new Date(`${iso}T12:00:00Z`);
        const jsD = d.getUTCDay();
        const isoD = jsD === 0 ? 7 : jsD;
        cells.push({
          localDate: iso,
          dayNumber: day,
          monthIndex,
          year,
          shortWeekdayRu: WEEKDAYS_SHORT_RU[isoD - 1],
          monthShortRu: MONTH_GENITIVE_SHORT_RU[monthIndex],
          isToday: iso === TODAY_ISO,
          completedCount: 0,
          assignedCount: 9,
          plannedCompletedCount: 0,
          freeCompletedCount: 0,
          totalWorkoutSeconds: 0,
          holdSecondsTotal: 0,
          closedPeriodsCount: 0,
          intensityLevel: 0,
          isFrozen: false,
          sessions: [],
        });
      }
    }
    while (cells.length % 7 !== 0) {
      cells.push(null);
    }

    const activeDaysInDisplayedMonth = cells.filter((c) => c && c.completedCount > 0).length;
    const totalSessionsInDisplayedMonth = cells.reduce(
      (acc, c) => acc + (c ? c.completedCount : 0),
      0
    );

    return {
      year,
      monthIndex,
      titleRu: `${MONTH_NAMES_RU[monthIndex]} ${year}`,
      cells,
      activeDaysInDisplayedMonth,
      totalSessionsInDisplayedMonth,
    };
  }, [monthOffset, dayAggregatesMap]);

  const selectedDayData: DayAggregate = useMemo(() => {
    return (
      dayAggregatesMap.get(selectedDate) ??
      dayAggregatesMap.get(TODAY_ISO)!
    );
  }, [dayAggregatesMap, selectedDate]);

  // Today's mandatory slots status
  const todayCompletedSlotIds = useMemo(() => {
    const todayRecs = dayAggregatesMap.get(TODAY_ISO)?.sessions ?? [];
    return new Set(todayRecs.map((r) => r.slotId).filter(Boolean));
  }, [dayAggregatesMap]);

  const nextAvailableSlot: DailySlotItem | undefined = useMemo(() => {
    return REAL_DAILY_SLOTS.find((s) => !todayCompletedSlotIds.has(s.id));
  }, [todayCompletedSlotIds]);

  const startSlotWorkout = (slot: DailySlotItem) => {
    if (soundEnabled) playVelarisTone('hold');
    setActiveWorkout({
      titleRu: `${slot.groupTitleRu} · ${slot.titleRu}`,
      contractionSec: slot.contractionSec,
      relaxationSec: slot.relaxationSec,
      totalRepetitions: Math.min(slot.repetitions, 12), // Scaled for quick interactive preview
      currentRepetition: 1,
      phase: 'CONTRACTION',
      phaseRemainingSec: slot.contractionSec,
      isPaused: false,
      isMandatory: true,
      slotId: slot.id,
    });
    setIsTimerScreenOpen(true);
  };

  const startBlitzRescue = () => {
    if (soundEnabled) playVelarisTone('hold');
    setActiveWorkout({
      titleRu: '⚡ Блиц-спасение серии (60 сек)',
      contractionSec: 1,
      relaxationSec: 1,
      totalRepetitions: 15,
      currentRepetition: 1,
      phase: 'CONTRACTION',
      phaseRemainingSec: 1,
      isPaused: false,
      isMandatory: false,
    });
    setIsTimerScreenOpen(true);
  };

  const startFreeTemplateWorkout = (tpl: FreeIntervalTemplate) => {
    if (soundEnabled) playVelarisTone('hold');
    const cycleSec = Math.max(2, tpl.contractionSec + tpl.relaxationSec);
    const totalTargetSec = tpl.targetMinutes * 60 + tpl.targetSeconds;
    const reps = Math.max(3, Math.min(30, Math.round(totalTargetSec / cycleSec)));
    setActiveWorkout({
      titleRu: tpl.name,
      contractionSec: tpl.contractionSec,
      relaxationSec: tpl.relaxationSec,
      totalRepetitions: reps,
      currentRepetition: 1,
      phase: 'CONTRACTION',
      phaseRemainingSec: tpl.contractionSec,
      isPaused: false,
      isMandatory: false,
    });
    setIsTimerScreenOpen(true);
  };

  const quickCompleteSessionOnDate = (dateIso: string, isMandatory: boolean) => {
    if (soundEnabled) playVelarisTone('complete');
    const daySessions = dayAggregatesMap.get(dateIso)?.sessions ?? [];
    const mandatoryDone = daySessions.filter((s) => s.isMandatory).length;
    const slot = REAL_DAILY_SLOTS[mandatoryDone % REAL_DAILY_SLOTS.length];

    const newRec: CompletedSessionRecord = isMandatory
      ? {
          id: `rec_${Date.now()}`,
          localDate: dateIso,
          slotId: slot.id,
          titleRu: `${slot.groupTitleRu} · ${slot.titleRu}`,
          isMandatory: true,
          durationSeconds: (slot.contractionSec + slot.relaxationSec) * slot.repetitions,
          holdSeconds: slot.contractionSec * slot.repetitions,
        }
      : {
          id: `rec_${Date.now()}`,
          localDate: dateIso,
          titleRu: 'Дополнительная интервальная сессия',
          isMandatory: false,
          durationSeconds: 180,
          holdSeconds: 110,
        };

    setHistoryRecords((prev) => [newRec, ...prev]);
    setChantalEnergy((e) => e + 10);
    setChantalCoins((c) => c + 5);
  };

  const toggleFreezeForDate = (dateIso: string) => {
    setFrozenDates((prev) => {
      if (prev.includes(dateIso)) {
        return prev.filter((d) => d !== dateIso);
      }
      if (prev.length >= 3) return prev;
      return [...prev, dateIso];
    });
  };

  return (
    <div
      className="min-h-screen w-full transition-colors duration-200"
      style={{
        background: `linear-gradient(180deg, ${palette.bgTop} 0%, ${palette.bgBottom} 100%)`,
        color: palette.textPrimary,
      }}
    >
      {/* Top Bar Contract: 3 zones */}
      <header
        className="sticky top-0 z-30 flex items-center justify-between gap-8 px-6 py-3.5 backdrop-blur-md border-b"
        style={{
          backgroundColor: `${palette.surface}E6`,
          borderColor: `${palette.outline}55`,
        }}
      >
        <div className="text-lg font-extrabold tracking-tight whitespace-nowrap shrink-0">
          Velaris
        </div>

        <nav className="hidden md:flex items-center gap-6 text-sm font-semibold">
          <button
            onClick={() => {
              setIsTimerScreenOpen(false);
              setActiveTab('home');
            }}
            className="whitespace-nowrap shrink-0 transition-opacity hover:opacity-80 cursor-pointer"
            style={{
              color: activeTab === 'home' && !isTimerScreenOpen ? palette.primaryAccent : palette.textSecondary,
            }}
          >
            Главная и статистика
          </button>
          <button
            onClick={() => {
              setIsTimerScreenOpen(false);
              setActiveTab('plan');
            }}
            className="whitespace-nowrap shrink-0 transition-opacity hover:opacity-80 cursor-pointer"
            style={{
              color: activeTab === 'plan' && !isTimerScreenOpen ? palette.primaryAccent : palette.textSecondary,
            }}
          >
            Тренировки
          </button>
          <button
            onClick={() => {
              setIsTimerScreenOpen(false);
              setActiveTab('chantal');
            }}
            className="whitespace-nowrap shrink-0 transition-opacity hover:opacity-80 cursor-pointer"
            style={{
              color: activeTab === 'chantal' && !isTimerScreenOpen ? '#EA580C' : palette.textSecondary,
            }}
          >
            Шанталь ({chantalEnergy} Эн.)
          </button>
          <button
            onClick={() => {
              setIsTimerScreenOpen(false);
              setActiveTab('settings');
            }}
            className="whitespace-nowrap shrink-0 transition-opacity hover:opacity-80 cursor-pointer"
            style={{
              color: activeTab === 'settings' && !isTimerScreenOpen ? palette.primaryAccent : palette.textSecondary,
            }}
          >
            Оформление и звук
          </button>
        </nav>

        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={() => {
              if (activeWorkout) {
                setIsTimerScreenOpen(true);
              } else if (nextAvailableSlot) {
                startSlotWorkout(nextAvailableSlot);
              } else {
                startBlitzRescue();
              }
            }}
            className="px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap shrink-0 transition-transform active:scale-95 cursor-pointer"
            style={{
              backgroundColor: palette.primaryAccent,
              color: '#FFFFFF',
            }}
          >
            {activeWorkout ? 'К активному таймеру' : 'Начать сессию'}
          </button>
        </div>
      </header>

      {/* Main Content Container: Samsung S25 Ultra focused layout with responsive multi-column expansion on wide screens */}
      <main className="max-w-6xl mx-auto px-4 sm:px-6 pt-5 pb-28">
        {isTimerScreenOpen && activeWorkout ? (
          /* ACTIVE WORKOUT TIMER SCREEN */
          <div className="max-w-[440px] mx-auto">
            <div
              className="rounded-3xl p-6 shadow-sm border"
              style={{
                backgroundColor: palette.surface,
                borderColor: `${palette.outline}45`,
              }}
            >
              <div className="flex items-center justify-between gap-4 mb-6">
                <button
                  onClick={() => setIsTimerScreenOpen(false)}
                  className="min-h-[44px] px-3 rounded-xl flex items-center gap-1.5 text-sm font-semibold cursor-pointer"
                  style={{
                    backgroundColor: palette.surfaceElevated,
                    color: palette.textPrimary,
                  }}
                >
                  <ChevronLeft className="w-4 h-4" />
                  <span>Свернуть</span>
                </button>
                <span
                  className="text-xs font-bold"
                  style={{ color: palette.textSecondary }}
                >
                  Цикл {activeWorkout.currentRepetition} из {activeWorkout.totalRepetitions}
                </span>
              </div>

              <div className="text-center mb-6">
                <h2 className="text-xl font-extrabold tracking-tight mb-1">
                  {activeWorkout.titleRu}
                </h2>
                <p className="text-sm font-medium" style={{ color: palette.textSecondary }}>
                  {activeWorkout.isPaused
                    ? 'Пауза — расслабьтесь'
                    : activeWorkout.phase === 'CONTRACTION'
                    ? 'Удержание · сожмите мышцы'
                    : 'Отдых · полное расслабление'}
                </p>
              </div>

              {/* Large Constant-Size Timer Ring */}
              <div className="relative w-64 h-64 mx-auto my-6 flex items-center justify-center">
                <svg className="w-full h-full -rotate-90" viewBox="0 0 220 220">
                  <circle
                    cx="110"
                    cy="110"
                    r="94"
                    fill="none"
                    stroke={palette.surfaceElevated}
                    strokeWidth="14"
                  />
                  <circle
                    cx="110"
                    cy="110"
                    r="94"
                    fill="none"
                    stroke={
                      activeWorkout.isPaused
                        ? palette.warningAccent
                        : activeWorkout.phase === 'CONTRACTION'
                        ? palette.primaryAccent
                        : palette.secondaryAccent
                    }
                    strokeWidth="14"
                    strokeLinecap="round"
                    strokeDasharray={2 * Math.PI * 94}
                    strokeDashoffset={
                      2 *
                      Math.PI *
                      94 *
                      (1 -
                        activeWorkout.phaseRemainingSec /
                          Math.max(
                            1,
                            activeWorkout.phase === 'CONTRACTION'
                              ? activeWorkout.contractionSec
                              : activeWorkout.relaxationSec
                          ))
                    }
                    className="transition-all duration-300"
                  />
                </svg>
                <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                  <span
                    className="text-sm font-bold mb-1"
                    style={{
                      color:
                        activeWorkout.phase === 'CONTRACTION'
                          ? palette.primaryAccent
                          : palette.secondaryAccent,
                    }}
                  >
                    {activeWorkout.phase === 'CONTRACTION' ? 'Удержание' : 'Отдых'}
                  </span>
                  <span className="text-6xl font-extrabold tabular-nums tracking-tight">
                    {activeWorkout.phaseRemainingSec}
                  </span>
                  <span className="text-xs font-medium mt-1" style={{ color: palette.textSecondary }}>
                    секунд
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 mt-8">
                <button
                  onClick={() =>
                    setActiveWorkout((prev) =>
                      prev ? { ...prev, isPaused: !prev.isPaused } : null
                    )
                  }
                  className="min-h-[52px] rounded-2xl font-bold text-base flex items-center justify-center gap-2 cursor-pointer transition-transform active:scale-98"
                  style={{
                    backgroundColor: palette.primaryAccent,
                    color: '#FFFFFF',
                  }}
                >
                  {activeWorkout.isPaused ? (
                    <>
                      <Play className="w-5 h-5" />
                      <span>Продолжить</span>
                    </>
                  ) : (
                    <>
                      <Pause className="w-5 h-5" />
                      <span>Пауза</span>
                    </>
                  )}
                </button>

                <button
                  onClick={() => {
                    setActiveWorkout(null);
                    setIsTimerScreenOpen(false);
                  }}
                  className="min-h-[52px] rounded-2xl font-bold text-base flex items-center justify-center gap-2 cursor-pointer"
                  style={{
                    backgroundColor: palette.surfaceElevated,
                    color: palette.textPrimary,
                  }}
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>Завершить</span>
                </button>
              </div>
            </div>
          </div>
        ) : activeTab === 'home' ? (
          /* HOME & REDESIGNED SAMSUNG S25 ULTRA STATISTICS SCREEN */
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Left / Primary S25 Ultra Column (Statistics & Unified Calendar) */}
            <div className="lg:col-span-7 xl:col-span-7 flex flex-col gap-5 max-w-[460px] lg:max-w-none mx-auto w-full">
              {/* REDESIGNED STATISTICS BLOCK FOR SAMSUNG S25 ULTRA */}
              <section
                aria-label="Статистика и единый календарь Velaris"
                className="rounded-[26px] p-5 sm:p-6 shadow-xs border transition-colors"
                style={{
                  backgroundColor: palette.surface,
                  borderColor: `${palette.outline}38`,
                }}
              >
                {/* 1. Calm, Large Header + Streak & Freeze Shields */}
                <div
                  className="rounded-[22px] p-4 sm:p-5 mb-5"
                  style={{ backgroundColor: palette.surfaceElevated }}
                >
                  <div className="flex items-start justify-between gap-4 mb-4">
                    <div>
                      <p
                        className="text-sm font-semibold mb-1"
                        style={{ color: palette.textSecondary }}
                      >
                        Статистика и ритм
                      </p>
                      <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight tabular-nums">
                        Серия {overviewStats.currentStreak} дн.
                      </h1>
                      <p
                        className="text-sm font-medium mt-1"
                        style={{ color: palette.textSecondary }}
                      >
                        {overviewStats.rankTitleRu}
                        <span className="mx-1.5" aria-hidden="true">
                          ·
                        </span>
                        Рекорд {overviewStats.bestStreak} дн.
                      </p>
                    </div>

                    {/* Freeze Shields Badge */}
                    <div
                      className="rounded-2xl px-3.5 py-2.5 text-center shrink-0"
                      style={{ backgroundColor: palette.surface }}
                    >
                      <div
                        className="text-base font-extrabold tabular-nums"
                        style={{ color: palette.secondaryAccent }}
                      >
                        ❄️ {overviewStats.availableFreezeShields}/{overviewStats.maxFreezeShields}
                      </div>
                      <div
                        className="text-xs font-medium mt-0.5"
                        style={{ color: palette.textSecondary }}
                      >
                        Защита серии
                      </div>
                    </div>
                  </div>

                  {/* 3 Large, Borderless KPI Blocks */}
                  <div className="grid grid-cols-3 gap-2.5 mb-4">
                    <div
                      className="rounded-2xl p-3 text-center"
                      style={{ backgroundColor: palette.surface }}
                    >
                      <div
                        className="text-xl font-extrabold tabular-nums"
                        style={{ color: palette.primaryAccent }}
                      >
                        {overviewStats.weekSessions}
                      </div>
                      <div
                        className="text-xs font-medium mt-0.5"
                        style={{ color: palette.textSecondary }}
                      >
                        За 7 дней
                      </div>
                    </div>

                    <div
                      className="rounded-2xl p-3 text-center"
                      style={{ backgroundColor: palette.surface }}
                    >
                      <div
                        className="text-xl font-extrabold tabular-nums"
                        style={{ color: palette.secondaryAccent }}
                      >
                        {formatDurationRu(overviewStats.weekHoldSeconds)}
                      </div>
                      <div
                        className="text-xs font-medium mt-0.5"
                        style={{ color: palette.textSecondary }}
                      >
                        Чистое сжатие
                      </div>
                    </div>

                    <div
                      className="rounded-2xl p-3 text-center"
                      style={{ backgroundColor: palette.surface }}
                    >
                      <div
                        className="text-xl font-extrabold tabular-nums"
                        style={{ color: palette.successAccent }}
                      >
                        {overviewStats.totalSessionsAllTime}
                      </div>
                      <div
                        className="text-xs font-medium mt-0.5"
                        style={{ color: palette.textSecondary }}
                      >
                        Всего сессий
                      </div>
                    </div>
                  </div>

                  {/* Next Milestone Progress + Interactive Milestone Tokens */}
                  <div className="space-y-2.5">
                    <div className="flex items-center justify-between text-xs font-semibold">
                      <span style={{ color: palette.textSecondary }}>
                        Следующий рубеж: «{overviewStats.nextMilestone.titleRu}» ({overviewStats.nextMilestone.targetDays} дн.)
                      </span>
                      <span
                        className="tabular-nums font-bold"
                        style={{ color: palette.primaryAccent }}
                      >
                        Ещё {Math.max(1, overviewStats.nextMilestone.targetDays - overviewStats.currentStreak)} дн.
                      </span>
                    </div>

                    <div
                      className="w-full h-2 rounded-full overflow-hidden"
                      style={{ backgroundColor: palette.surface }}
                    >
                      <div
                        className="h-full rounded-full transition-all duration-300"
                        style={{
                          width: `${Math.round(overviewStats.milestoneFraction * 100)}%`,
                          background: `linear-gradient(90deg, ${palette.primaryAccent}, ${palette.successAccent})`,
                        }}
                      />
                    </div>

                    <div className="grid grid-cols-7 gap-1.5 pt-1">
                      {MILESTONES.map((badge) => {
                        const isUnlocked =
                          Math.max(overviewStats.currentStreak, overviewStats.bestStreak) >=
                          badge.targetDays;
                        const isSelected = inspectedMilestone?.targetDays === badge.targetDays;
                        return (
                          <button
                            key={badge.targetDays}
                            onClick={() => {
                              setInspectedMilestone(isSelected ? null : badge);
                              if (soundEnabled) playVelarisTone('salute');
                            }}
                            className="min-h-[48px] rounded-xl flex flex-col items-center justify-center py-1.5 cursor-pointer transition-transform active:scale-95"
                            style={{
                              backgroundColor: isSelected
                                ? palette.selectedContainer
                                : isUnlocked
                                ? `${palette.successAccent}24`
                                : palette.surface,
                            }}
                          >
                            <span className="text-sm leading-none">{badge.badgeEmoji}</span>
                            <span
                              className="text-[11px] font-bold tabular-nums mt-1"
                              style={{
                                color: isUnlocked ? palette.successAccent : palette.textSecondary,
                              }}
                            >
                              {badge.targetDays}д
                            </span>
                          </button>
                        );
                      })}
                    </div>

                    {inspectedMilestone && (
                      <div
                        className="rounded-2xl p-3.5 mt-2 flex items-start justify-between gap-3"
                        style={{ backgroundColor: palette.surface }}
                      >
                        <div>
                          <div className="text-sm font-bold">
                            {inspectedMilestone.badgeEmoji} {inspectedMilestone.titleRu} ·{' '}
                            {inspectedMilestone.targetDays} дн.
                          </div>
                          <p
                            className="text-xs mt-1 leading-relaxed"
                            style={{ color: palette.textSecondary }}
                          >
                            {inspectedMilestone.descriptionRu}
                          </p>
                        </div>
                        <button
                          onClick={() => setInspectedMilestone(null)}
                          className="text-xs font-bold px-2.5 py-1.5 rounded-lg shrink-0 cursor-pointer"
                          style={{
                            backgroundColor: palette.surfaceElevated,
                            color: palette.textSecondary,
                          }}
                        >
                          Скрыть
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {/* 2. UNIFIED MONTHLY CALENDAR WITH MONTH SWITCHING (No Week/Heatmap Toggle, No Complex Legend) */}
                <div
                  className="rounded-[22px] p-4 sm:p-5 mb-5"
                  style={{ backgroundColor: palette.surfaceElevated }}
                >
                  {/* Month Navigation Bar (44px+ touch targets for S25 Ultra) */}
                  <div className="flex items-center justify-between gap-3 mb-4">
                    <button
                      onClick={() => setMonthOffset((m) => Math.max(-5, m - 1))}
                      aria-label="Предыдущий месяц"
                      className="w-11 h-11 rounded-xl flex items-center justify-center cursor-pointer transition-transform active:scale-95"
                      style={{
                        backgroundColor: palette.surface,
                        color: palette.textPrimary,
                      }}
                    >
                      <ChevronLeft className="w-5 h-5" />
                    </button>

                    <div className="text-center flex-1">
                      <button
                        onClick={() => setMonthOffset(0)}
                        className="text-lg font-extrabold tracking-tight cursor-pointer"
                        style={{ color: palette.textPrimary }}
                      >
                        {displayedMonthData.titleRu}
                      </button>
                      <div
                        className="text-xs font-medium mt-0.5"
                        style={{
                          color: monthOffset === 0 ? palette.textSecondary : palette.primaryAccent,
                        }}
                      >
                        {monthOffset === 0
                          ? `Активных дней: ${displayedMonthData.activeDaysInDisplayedMonth} · Сессий: ${displayedMonthData.totalSessionsInDisplayedMonth}`
                          : 'Нажмите на название месяца, чтобы вернуться к текущему'}
                      </div>
                    </div>

                    <button
                      onClick={() => setMonthOffset((m) => Math.min(0, m + 1))}
                      disabled={monthOffset >= 0}
                      aria-label="Следующий месяц"
                      className="w-11 h-11 rounded-xl flex items-center justify-center cursor-pointer transition-transform active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed"
                      style={{
                        backgroundColor: palette.surface,
                        color: palette.textPrimary,
                      }}
                    >
                      <ChevronRight className="w-5 h-5" />
                    </button>
                  </div>

                  {/* Weekday Headers */}
                  <div className="grid grid-cols-7 gap-1.5 mb-2">
                    {WEEKDAYS_SHORT_RU.map((dayName) => (
                      <div
                        key={dayName}
                        className="text-center text-xs font-bold py-1"
                        style={{ color: palette.textSecondary }}
                      >
                        {dayName}
                      </div>
                    ))}
                  </div>

                  {/* Unified Month Calendar Grid: Combines Date + Activity Intensity + Session Count */}
                  <div className="grid grid-cols-7 gap-1.5 sm:gap-2">
                    {displayedMonthData.cells.map((cell, idx) => {
                      if (!cell) {
                        return <div key={`empty_${idx}`} className="h-[58px] rounded-2xl" />;
                      }

                      const isSelected = cell.localDate === selectedDayData.localDate;
                      const hasSessions = cell.completedCount > 0;

                      let cellBg = palette.surface;
                      if (cell.intensityLevel === 3) {
                        cellBg = `${palette.successAccent}33`;
                      } else if (cell.intensityLevel === 2) {
                        cellBg = `${palette.primaryAccent}26`;
                      } else if (cell.intensityLevel === 1) {
                        cellBg = `${palette.primaryAccent}14`;
                      } else if (cell.isFrozen) {
                        cellBg = `${palette.secondaryAccent}22`;
                      } else if (isSelected) {
                        cellBg = palette.selectedContainer;
                      }

                      const barWidthPct =
                        cell.intensityLevel === 3
                          ? 100
                          : cell.intensityLevel === 2
                          ? 66
                          : cell.intensityLevel === 1
                          ? 36
                          : cell.isFrozen
                          ? 50
                          : 0;

                      return (
                        <button
                          key={cell.localDate}
                          onClick={() => setSelectedDate(cell.localDate)}
                          className="h-[58px] rounded-2xl px-1 py-1.5 flex flex-col items-center justify-between cursor-pointer transition-all active:scale-95"
                          style={{
                            backgroundColor: cellBg,
                            boxShadow: isSelected
                              ? `inset 0 0 0 2px ${palette.selectedBorder}`
                              : cell.isToday
                              ? `inset 0 0 0 1.5px ${palette.primaryAccent}99`
                              : 'none',
                          }}
                        >
                          {/* Date Number */}
                          <span
                            className="text-sm sm:text-[15px] font-extrabold tabular-nums leading-none"
                            style={{
                              color: isSelected ? palette.primaryAccent : palette.textPrimary,
                            }}
                          >
                            {cell.dayNumber}
                          </span>

                          {/* Session Count or Freeze Shield */}
                          <span
                            className="text-[11px] font-extrabold tabular-nums leading-none"
                            style={{
                              color:
                                cell.intensityLevel === 3
                                  ? palette.successAccent
                                  : hasSessions
                                  ? palette.primaryAccent
                                  : cell.isFrozen
                                  ? palette.secondaryAccent
                                  : 'transparent',
                            }}
                          >
                            {hasSessions
                              ? `${cell.completedCount} сес.`
                              : cell.isFrozen
                              ? '❄️'
                              : '·'}
                          </span>

                          {/* Subtle Bottom Intensity Indicator */}
                          <div
                            className="w-4/5 h-1 rounded-full overflow-hidden"
                            style={{
                              backgroundColor:
                                barWidthPct > 0 ? `${palette.outline}35` : 'transparent',
                            }}
                          >
                            {barWidthPct > 0 && (
                              <div
                                className="h-full rounded-full"
                                style={{
                                  width: `${barWidthPct}%`,
                                  backgroundColor:
                                    cell.intensityLevel === 3
                                      ? palette.successAccent
                                      : cell.isFrozen
                                      ? palette.secondaryAccent
                                      : palette.primaryAccent,
                                }}
                              />
                            )}
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* 3. BRIEF SELECTED DAY SUMMARY (Mandatory Program, Extra Workouts, Workout Time) */}
                <div
                  className="rounded-[22px] p-4 sm:p-5 mb-4"
                  style={{ backgroundColor: palette.surfaceElevated }}
                >
                  <div className="flex items-center justify-between gap-3 mb-3.5">
                    <div>
                      <h2 className="text-base sm:text-lg font-extrabold tracking-tight">
                        {selectedDayData.isToday
                          ? `Сегодня · ${selectedDayData.shortWeekdayRu}, ${selectedDayData.dayNumber} ${selectedDayData.monthShortRu}`
                          : `${selectedDayData.shortWeekdayRu}, ${selectedDayData.dayNumber} ${selectedDayData.monthShortRu} ${selectedDayData.year}`}
                      </h2>
                    </div>

                    <span
                      className="text-sm font-extrabold tabular-nums"
                      style={{
                        color:
                          selectedDayData.completedCount > 0
                            ? palette.successAccent
                            : selectedDayData.isFrozen
                            ? palette.secondaryAccent
                            : palette.textSecondary,
                      }}
                    >
                      {selectedDayData.completedCount > 0
                        ? `Сессий: ${selectedDayData.completedCount}`
                        : selectedDayData.isFrozen
                        ? '❄️ Защищён заморозкой'
                        : 'Без занятий'}
                    </span>
                  </div>

                  {/* 3 Brief Summary Metric Tiles: Mandatory Program, Extra Sessions, Workout Time */}
                  <div className="grid grid-cols-3 gap-2.5">
                    <div
                      className="rounded-2xl p-3"
                      style={{ backgroundColor: palette.surface }}
                    >
                      <div
                        className="text-xs font-medium"
                        style={{ color: palette.textSecondary }}
                      >
                        Обязательная
                      </div>
                      <div
                        className="text-lg font-extrabold tabular-nums mt-1"
                        style={{
                          color:
                            selectedDayData.plannedCompletedCount > 0
                              ? palette.successAccent
                              : palette.textPrimary,
                        }}
                      >
                        {selectedDayData.plannedCompletedCount} из {selectedDayData.assignedCount}
                      </div>
                      <div
                        className="text-xs font-medium mt-0.5"
                        style={{ color: palette.textSecondary }}
                      >
                        {selectedDayData.plannedCompletedCount >= selectedDayData.assignedCount
                          ? 'План закрыт ✓'
                          : selectedDayData.closedPeriodsCount > 0
                          ? `Этапов: ${selectedDayData.closedPeriodsCount}/3`
                          : selectedDayData.plannedCompletedCount > 0
                          ? 'В процессе'
                          : 'Не начата'}
                      </div>
                    </div>

                    <div
                      className="rounded-2xl p-3"
                      style={{ backgroundColor: palette.surface }}
                    >
                      <div
                        className="text-xs font-medium"
                        style={{ color: palette.textSecondary }}
                      >
                        Дополнительно
                      </div>
                      <div
                        className="text-lg font-extrabold tabular-nums mt-1"
                        style={{
                          color:
                            selectedDayData.freeCompletedCount > 0
                              ? palette.primaryAccent
                              : palette.textPrimary,
                        }}
                      >
                        {selectedDayData.freeCompletedCount > 0
                          ? `+${selectedDayData.freeCompletedCount}`
                          : '0'}
                      </div>
                      <div
                        className="text-xs font-medium mt-0.5"
                        style={{ color: palette.textSecondary }}
                      >
                        {selectedDayData.freeCompletedCount > 0
                          ? 'Своб. сессии'
                          : 'Без доп.'}
                      </div>
                    </div>

                    <div
                      className="rounded-2xl p-3"
                      style={{ backgroundColor: palette.surface }}
                    >
                      <div
                        className="text-xs font-medium"
                        style={{ color: palette.textSecondary }}
                      >
                        Время тренировки
                      </div>
                      <div
                        className="text-lg font-extrabold tabular-nums mt-1"
                        style={{
                          color:
                            selectedDayData.totalWorkoutSeconds > 0
                              ? palette.secondaryAccent
                              : palette.textPrimary,
                        }}
                      >
                        {formatDurationRu(selectedDayData.totalWorkoutSeconds)}
                      </div>
                      <div
                        className="text-xs font-medium mt-0.5 tabular-nums"
                        style={{ color: palette.textSecondary }}
                      >
                        Сжатие {formatDurationRu(selectedDayData.holdSecondsTotal)}
                      </div>
                    </div>
                  </div>

                  {/* Contextual Actions for Selected Day (Freeze Shield or Quick Log for testing) */}
                  <div className="flex flex-wrap items-center gap-2 mt-3.5">
                    {selectedDayData.completedCount === 0 && !selectedDayData.isToday && (
                      <button
                        onClick={() => toggleFreezeForDate(selectedDayData.localDate)}
                        className="min-h-[44px] px-4 rounded-xl text-xs font-bold flex items-center gap-2 cursor-pointer transition-opacity hover:opacity-90"
                        style={{
                          backgroundColor: palette.surface,
                          color: palette.secondaryAccent,
                        }}
                      >
                        <Shield className="w-4 h-4" />
                        <span>
                          {selectedDayData.isFrozen
                            ? 'Снять защиту заморозки с этого дня'
                            : `❄️ Защитить пропуск заморозкой (${overviewStats.availableFreezeShields} дост.)`}
                        </span>
                      </button>
                    )}

                    <button
                      onClick={() => quickCompleteSessionOnDate(selectedDayData.localDate, true)}
                      className="min-h-[42px] px-3.5 rounded-xl text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                      style={{
                        backgroundColor: palette.surface,
                        color: palette.primaryAccent,
                      }}
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>Отметить обязательную</span>
                    </button>

                    <button
                      onClick={() => quickCompleteSessionOnDate(selectedDayData.localDate, false)}
                      className="min-h-[42px] px-3.5 rounded-xl text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                      style={{
                        backgroundColor: palette.surface,
                        color: palette.secondaryAccent,
                      }}
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>+ Доп. занятие</span>
                    </button>
                  </div>
                </div>

                {/* 4. EXPANDABLE SESSION HISTORY BELOW */}
                <div
                  className="rounded-[22px] overflow-hidden"
                  style={{ backgroundColor: palette.surfaceElevated }}
                >
                  <button
                    onClick={() => setIsHistoryExpanded((prev) => !prev)}
                    className="w-full min-h-[56px] px-5 py-3.5 flex items-center justify-between gap-4 text-left cursor-pointer"
                  >
                    <div>
                      <div className="text-base font-extrabold">История занятий</div>
                      <div
                        className="text-xs font-medium mt-0.5"
                        style={{ color: palette.textSecondary }}
                      >
                        {selectedDayData.sessions.length > 0
                          ? `За ${selectedDayData.dayNumber} ${selectedDayData.monthShortRu}: ${selectedDayData.sessions.length} сесс.`
                          : 'Подробный список выполненных сессий'}
                      </div>
                    </div>

                    <div
                      className="flex items-center gap-1.5 text-sm font-bold shrink-0"
                      style={{ color: palette.primaryAccent }}
                    >
                      <span>{isHistoryExpanded ? 'Свернуть' : 'Развернуть'}</span>
                      {isHistoryExpanded ? (
                        <ChevronUp className="w-4 h-4" />
                      ) : (
                        <ChevronDown className="w-4 h-4" />
                      )}
                    </div>
                  </button>

                  {isHistoryExpanded && (
                    <div className="px-4 pb-4 space-y-2">
                      {selectedDayData.sessions.length > 0 ? (
                        selectedDayData.sessions.map((session) => (
                          <div
                            key={session.id}
                            className="rounded-2xl px-4 py-3 flex items-center justify-between gap-3"
                            style={{ backgroundColor: palette.surface }}
                          >
                            <div className="flex items-center gap-3 min-w-0">
                              <CheckCircle2
                                className="w-5 h-5 shrink-0"
                                style={{
                                  color: session.isMandatory
                                    ? palette.successAccent
                                    : palette.primaryAccent,
                                }}
                              />
                              <div className="min-w-0">
                                <div className="text-sm font-bold truncate">
                                  {session.titleRu}
                                </div>
                                <div
                                  className="text-xs font-medium mt-0.5"
                                  style={{ color: palette.textSecondary }}
                                >
                                  {session.isMandatory
                                    ? 'Обязательная программа'
                                    : 'Дополнительное занятие'}
                                  <span className="mx-1.5" aria-hidden="true">
                                    ·
                                  </span>
                                  Сжатие {formatDurationRu(session.holdSeconds)}
                                </div>
                              </div>
                            </div>

                            <div
                              className="text-sm font-extrabold tabular-nums shrink-0"
                              style={{ color: palette.secondaryAccent }}
                            >
                              {formatDurationRu(session.durationSeconds)}
                            </div>
                          </div>
                        ))
                      ) : (
                        <div
                          className="rounded-2xl p-4 text-sm"
                          style={{
                            backgroundColor: palette.surface,
                            color: palette.textSecondary,
                          }}
                        >
                          В выбранный день завершённых сессий нет. Выберите активную дату в календаре выше или запустите тренировку.
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </section>
            </div>

            {/* Right / Companion Column on Desktop (Active Session Card & Quick Controls) */}
            <div className="lg:col-span-5 xl:col-span-5 flex flex-col gap-5 max-w-[460px] lg:max-w-none mx-auto w-full">
              {/* Active Session Card (from VelarisHomeScreen) */}
              <section
                aria-label="Активная сессия дня"
                className="rounded-[26px] p-5 sm:p-6 shadow-xs border"
                style={{
                  backgroundColor: palette.surface,
                  borderColor: `${palette.outline}38`,
                }}
              >
                <div className="flex items-center justify-between gap-3 mb-4">
                  <div className="flex items-center gap-2.5">
                    <div
                      className="w-9 h-9 rounded-xl flex items-center justify-center"
                      style={{ backgroundColor: `${palette.primaryAccent}22` }}
                    >
                      <Dumbbell
                        className="w-5 h-5"
                        style={{ color: palette.primaryAccent }}
                      />
                    </div>
                    <div>
                      <h2 className="text-lg font-extrabold tracking-tight">
                        Активная сессия
                      </h2>
                      <p
                        className="text-xs font-medium"
                        style={{ color: palette.textSecondary }}
                      >
                        Обязательная программа на сегодня
                      </p>
                    </div>
                  </div>

                  <span
                    className="text-sm font-extrabold tabular-nums"
                    style={{ color: palette.secondaryAccent }}
                  >
                    {todayCompletedSlotIds.size} из 9 за день
                  </span>
                </div>

                {/* Morning / Noon / Evening Progress Strip */}
                <div className="grid grid-cols-3 gap-2 mb-4">
                  {(['MORNING', 'NOON', 'EVENING'] as PlanGroupCode[]).map((code) => {
                    const groupSlots = REAL_DAILY_SLOTS.filter((s) => s.groupCode === code);
                    const done = groupSlots.filter((s) => todayCompletedSlotIds.has(s.id)).length;
                    const isAllDone = done === groupSlots.length;
                    const label =
                      code === 'MORNING' ? 'Утро' : code === 'NOON' ? 'Обед' : 'Вечер';
                    return (
                      <div
                        key={code}
                        className="rounded-xl px-3 py-2.5 flex items-center justify-between"
                        style={{
                          backgroundColor: isAllDone
                            ? `${palette.successAccent}20`
                            : palette.surfaceElevated,
                        }}
                      >
                        <span
                          className="text-xs font-bold"
                          style={{
                            color: isAllDone ? palette.successAccent : palette.textPrimary,
                          }}
                        >
                          {label}
                        </span>
                        <span
                          className="text-xs font-extrabold tabular-nums"
                          style={{
                            color: isAllDone ? palette.successAccent : palette.textSecondary,
                          }}
                        >
                          {isAllDone ? `✓ ${done}/3` : `${done}/3`}
                        </span>
                      </div>
                    );
                  })}
                </div>

                {nextAvailableSlot ? (
                  <div
                    className="rounded-2xl p-4 mb-4"
                    style={{ backgroundColor: palette.surfaceElevated }}
                  >
                    <div className="flex items-center justify-between gap-2 mb-3">
                      <span className="text-base font-extrabold">
                        {nextAvailableSlot.groupTitleRu} · {nextAvailableSlot.titleRu}
                      </span>
                      <span
                        className="text-xs font-bold tabular-nums"
                        style={{ color: palette.secondaryAccent }}
                      >
                        {formatDurationRu(
                          (nextAvailableSlot.contractionSec + nextAvailableSlot.relaxationSec) *
                            nextAvailableSlot.repetitions
                        )}
                      </span>
                    </div>

                    <div className="grid grid-cols-3 gap-2">
                      <div
                        className="rounded-xl p-2.5 text-center"
                        style={{ backgroundColor: palette.surface }}
                      >
                        <div
                          className="text-[11px] font-medium"
                          style={{ color: palette.textSecondary }}
                        >
                          Сжатие
                        </div>
                        <div className="text-sm font-extrabold tabular-nums mt-0.5">
                          {nextAvailableSlot.contractionSec} сек
                        </div>
                      </div>
                      <div
                        className="rounded-xl p-2.5 text-center"
                        style={{ backgroundColor: palette.surface }}
                      >
                        <div
                          className="text-[11px] font-medium"
                          style={{ color: palette.textSecondary }}
                        >
                          Отдых
                        </div>
                        <div className="text-sm font-extrabold tabular-nums mt-0.5">
                          {nextAvailableSlot.relaxationSec} сек
                        </div>
                      </div>
                      <div
                        className="rounded-xl p-2.5 text-center"
                        style={{ backgroundColor: palette.surface }}
                      >
                        <div
                          className="text-[11px] font-medium"
                          style={{ color: palette.textSecondary }}
                        >
                          Повторов
                        </div>
                        <div className="text-sm font-extrabold tabular-nums mt-0.5">
                          {nextAvailableSlot.repetitions}
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div
                    className="rounded-2xl p-4 mb-4 flex items-center gap-3"
                    style={{ backgroundColor: `${palette.successAccent}18` }}
                  >
                    <CheckCircle2
                      className="w-6 h-6 shrink-0"
                      style={{ color: palette.successAccent }}
                    />
                    <div>
                      <div
                        className="text-sm font-extrabold"
                        style={{ color: palette.successAccent }}
                      >
                        Все обязательные тренировки на сегодня выполнены
                      </div>
                      <div
                        className="text-xs mt-0.5"
                        style={{ color: palette.textSecondary }}
                      >
                        Утро, Обед и Вечер полностью закрыты.
                      </div>
                    </div>
                  </div>
                )}

                <div className="grid grid-cols-2 gap-2.5">
                  <button
                    onClick={() => {
                      if (nextAvailableSlot) {
                        startSlotWorkout(nextAvailableSlot);
                      } else {
                        setActiveTab('plan');
                      }
                    }}
                    className="min-h-[48px] rounded-2xl font-bold text-sm flex items-center justify-center gap-2 cursor-pointer transition-transform active:scale-98"
                    style={{
                      backgroundColor: palette.primaryAccent,
                      color: '#FFFFFF',
                    }}
                  >
                    <Play className="w-4 h-4 fill-current" />
                    <span>{nextAvailableSlot ? 'Запустить' : 'К расписанию'}</span>
                  </button>

                  <button
                    onClick={startBlitzRescue}
                    className="min-h-[48px] rounded-2xl font-bold text-sm flex items-center justify-center gap-1.5 cursor-pointer"
                    style={{
                      backgroundColor: palette.surfaceElevated,
                      color: palette.primaryAccent,
                    }}
                  >
                    <Zap className="w-4 h-4" />
                    <span>Блиц 60 сек</span>
                  </button>
                </div>
              </section>

              {/* Companion Quick Card on Home Screen */}
              <section
                aria-label="Домик лисёнка Шанталь"
                className="rounded-[26px] p-5 sm:p-6 shadow-xs border bg-[#1E1712] text-[#F6EEDF] border-[#4D3D2C]"
              >
                <div className="flex items-center justify-between gap-3 mb-3">
                  <div>
                    <div className="text-xs font-semibold text-[#F59E0B]">
                      Компаньон · Мир лесных троп
                    </div>
                    <h3 className="text-lg font-extrabold text-[#FFF8EB]">
                      Шанталь ждёт приключений
                    </h3>
                  </div>
                  <div className="text-right text-xs font-extrabold tabular-nums">
                    <div className="text-[#10B981]">⚡ {chantalEnergy} Энергии</div>
                    <div className="text-[#F59E0B] mt-0.5">🪙 {chantalCoins} Монет</div>
                  </div>
                </div>
                <p className="text-xs text-[#D5C3AA] leading-relaxed mb-4">
                  Каждая выполненная сессия даёт +10 Энергии. За 30 Энергии (блок Утро, Обед или Вечер) Шанталь уходит в часовой поход и приносит историю от ИИ и находку в инвентарь.
                </p>
                <button
                  onClick={() => setActiveTab('chantal')}
                  className="w-full min-h-[46px] rounded-2xl bg-[#EA580C] text-white text-xs sm:text-sm font-extrabold flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Compass className="w-4 h-4" />
                  <span>Открыть домик Шанталь и карту</span>
                </button>
              </section>

              {/* Quick Theme Switcher Card */}
              <section
                aria-label="Быстрый выбор темы Velaris"
                className="rounded-[26px] p-5 sm:p-6 shadow-xs border"
                style={{
                  backgroundColor: palette.surface,
                  borderColor: `${palette.outline}38`,
                }}
              >
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <h3 className="text-base font-extrabold">Палитра оформления</h3>
                    <p
                      className="text-xs font-medium"
                      style={{ color: palette.textSecondary }}
                    >
                      5 светлых тем One UI / Velaris
                    </p>
                  </div>
                  <Sparkles
                    className="w-4 h-4"
                    style={{ color: palette.primaryAccent }}
                  />
                </div>

                <div className="grid grid-cols-1 gap-2">
                  {(Object.keys(VELARIS_THEMES) as VelarisThemeId[]).map((id) => {
                    const t = VELARIS_THEMES[id];
                    const isCurrent = id === themeId;
                    return (
                      <button
                        key={id}
                        onClick={() => setThemeId(id)}
                        className="min-h-[48px] px-3.5 py-2.5 rounded-2xl flex items-center justify-between gap-3 text-left cursor-pointer transition-all"
                        style={{
                          backgroundColor: isCurrent
                            ? t.selectedContainer
                            : palette.surfaceElevated,
                          boxShadow: isCurrent
                            ? `inset 0 0 0 1.5px ${t.selectedBorder}`
                            : 'none',
                        }}
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <span
                            className="w-5 h-5 rounded-full shrink-0"
                            style={{ backgroundColor: t.primaryAccent }}
                          />
                          <div className="min-w-0">
                            <div className="text-sm font-bold truncate">{t.titleRu}</div>
                          </div>
                        </div>
                        {isCurrent && (
                          <span
                            className="text-xs font-extrabold shrink-0"
                            style={{ color: t.primaryAccent }}
                          >
                            Выбрана ✓
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              </section>
            </div>
          </div>
        ) : activeTab === 'plan' ? (
          /* WORKOUT PLAN & FREE INTERVALS SCREEN */
          <div className="max-w-[540px] mx-auto space-y-5">
            <div
              className="rounded-2xl p-1.5 grid grid-cols-2 gap-1.5"
              style={{ backgroundColor: palette.surfaceElevated }}
            >
              <button
                onClick={() => setPlanSubTab('scheduled')}
                className="min-h-[44px] rounded-xl text-sm font-bold cursor-pointer transition-colors"
                style={{
                  backgroundColor:
                    planSubTab === 'scheduled' ? palette.surface : 'transparent',
                  color:
                    planSubTab === 'scheduled'
                      ? palette.primaryAccent
                      : palette.textSecondary,
                }}
              >
                По плану (Утро · Обед · Вечер)
              </button>
              <button
                onClick={() => setPlanSubTab('free')}
                className="min-h-[44px] rounded-xl text-sm font-bold cursor-pointer transition-colors"
                style={{
                  backgroundColor:
                    planSubTab === 'free' ? palette.surface : 'transparent',
                  color:
                    planSubTab === 'free'
                      ? palette.primaryAccent
                      : palette.textSecondary,
                }}
              >
                Мои интервалы (5)
              </button>
            </div>

            {planSubTab === 'scheduled' ? (
              <div
                className="rounded-[26px] p-5 sm:p-6 border space-y-4"
                style={{
                  backgroundColor: palette.surface,
                  borderColor: `${palette.outline}38`,
                }}
              >
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-xl font-extrabold">Ежедневная программа</h2>
                    <p
                      className="text-xs font-medium mt-0.5"
                      style={{ color: palette.textSecondary }}
                    >
                      3 этапа в день по 3 обязательные тренировки
                    </p>
                  </div>
                  <span
                    className="text-sm font-extrabold tabular-nums"
                    style={{ color: palette.primaryAccent }}
                  >
                    {todayCompletedSlotIds.size} / 9
                  </span>
                </div>

                <div
                  className="grid grid-cols-3 gap-2 p-1.5 rounded-2xl"
                  style={{ backgroundColor: palette.surfaceElevated }}
                >
                  {(['MORNING', 'NOON', 'EVENING'] as PlanGroupCode[]).map((g) => {
                    const label =
                      g === 'MORNING' ? 'Утро' : g === 'NOON' ? 'Обед' : 'Вечер';
                    const isSelected = selectedGroupCode === g;
                    return (
                      <button
                        key={g}
                        onClick={() => setSelectedGroupCode(g)}
                        className="min-h-[44px] rounded-xl text-sm font-bold cursor-pointer"
                        style={{
                          backgroundColor: isSelected ? palette.surface : 'transparent',
                          color: isSelected ? palette.primaryAccent : palette.textSecondary,
                        }}
                      >
                        {label}
                      </button>
                    );
                  })}
                </div>

                <div className="space-y-3">
                  {REAL_DAILY_SLOTS.filter((s) => s.groupCode === selectedGroupCode).map(
                    (slot) => {
                      const isDone = todayCompletedSlotIds.has(slot.id);
                      const totalSec =
                        (slot.contractionSec + slot.relaxationSec) * slot.repetitions;
                      return (
                        <div
                          key={slot.id}
                          className="rounded-2xl p-4 flex items-center justify-between gap-4"
                          style={{ backgroundColor: palette.surfaceElevated }}
                        >
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-base font-extrabold">
                                {slot.titleRu}
                              </span>
                              {isDone && (
                                <span
                                  className="text-xs font-bold"
                                  style={{ color: palette.successAccent }}
                                >
                                  ✓ Выполнено
                                </span>
                              )}
                            </div>
                            <div
                              className="text-xs font-medium mt-1 tabular-nums"
                              style={{ color: palette.textSecondary }}
                            >
                              Удержание {slot.contractionSec} с · Отдых {slot.relaxationSec} с ·{' '}
                              {slot.repetitions} повт. ({formatDurationRu(totalSec)})
                            </div>
                          </div>

                          <button
                            onClick={() => startSlotWorkout(slot)}
                            className="min-h-[44px] px-4 rounded-xl text-xs font-bold shrink-0 cursor-pointer"
                            style={{
                              backgroundColor: isDone
                                ? palette.surface
                                : palette.primaryAccent,
                              color: isDone ? palette.primaryAccent : '#FFFFFF',
                            }}
                          >
                            {isDone ? 'Повторить' : 'Начать'}
                          </button>
                        </div>
                      );
                    }
                  )}
                </div>
              </div>
            ) : (
              <div
                className="rounded-[26px] p-5 sm:p-6 border space-y-4"
                style={{
                  backgroundColor: palette.surface,
                  borderColor: `${palette.outline}38`,
                }}
              >
                <div>
                  <h2 className="text-xl font-extrabold">Мои интервалы</h2>
                  <p
                    className="text-xs font-medium mt-0.5"
                    style={{ color: palette.textSecondary }}
                  >
                    Дополнительные свободные тренировки (учитываются в статистике дня отдельно от обязательной программы)
                  </p>
                </div>

                <div className="space-y-3">
                  {freeTemplates.map((tpl, idx) => {
                    const isEditing = editingTemplateIdx === idx;
                    return (
                      <div
                        key={tpl.slotIndex}
                        className="rounded-2xl p-4 space-y-3"
                        style={{ backgroundColor: palette.surfaceElevated }}
                      >
                        <div className="flex items-center justify-between gap-3">
                          <div>
                            <div className="text-base font-extrabold">{tpl.name}</div>
                            <div
                              className="text-xs font-medium mt-1 tabular-nums"
                              style={{ color: palette.textSecondary }}
                            >
                              Удержание {tpl.contractionSec} с · Отдых {tpl.relaxationSec} с ·{' '}
                              {tpl.targetMinutes} мин{' '}
                              {tpl.targetSeconds > 0 ? `${tpl.targetSeconds} с` : ''}
                            </div>
                          </div>

                          <div className="flex items-center gap-2">
                            <button
                              onClick={() =>
                                setEditingTemplateIdx(isEditing ? null : idx)
                              }
                              className="min-h-[42px] px-3 rounded-xl text-xs font-bold cursor-pointer"
                              style={{
                                backgroundColor: palette.surface,
                                color: palette.textSecondary,
                              }}
                            >
                              {isEditing ? 'Готово' : 'Изменить'}
                            </button>
                            <button
                              onClick={() => startFreeTemplateWorkout(tpl)}
                              className="min-h-[42px] px-4 rounded-xl text-xs font-bold cursor-pointer"
                              style={{
                                backgroundColor: palette.primaryAccent,
                                color: '#FFFFFF',
                              }}
                            >
                              Начать
                            </button>
                          </div>
                        </div>

                        {isEditing && (
                          <div className="grid grid-cols-3 gap-2 pt-2">
                            <label className="text-xs font-semibold">
                              <span style={{ color: palette.textSecondary }}>Сжатие (с)</span>
                              <input
                                type="number"
                                min={1}
                                max={60}
                                value={tpl.contractionSec}
                                onChange={(e) => {
                                  const val = Math.max(1, Number(e.target.value) || 1);
                                  setFreeTemplates((prev) =>
                                    prev.map((item, i) =>
                                      i === idx ? { ...item, contractionSec: val } : item
                                    )
                                  );
                                }}
                                className="mt-1 w-full rounded-xl px-3 py-2 text-sm font-bold tabular-nums"
                                style={{ backgroundColor: palette.surface }}
                              />
                            </label>
                            <label className="text-xs font-semibold">
                              <span style={{ color: palette.textSecondary }}>Отдых (с)</span>
                              <input
                                type="number"
                                min={1}
                                max={60}
                                value={tpl.relaxationSec}
                                onChange={(e) => {
                                  const val = Math.max(1, Number(e.target.value) || 1);
                                  setFreeTemplates((prev) =>
                                    prev.map((item, i) =>
                                      i === idx ? { ...item, relaxationSec: val } : item
                                    )
                                  );
                                }}
                                className="mt-1 w-full rounded-xl px-3 py-2 text-sm font-bold tabular-nums"
                                style={{ backgroundColor: palette.surface }}
                              />
                            </label>
                            <label className="text-xs font-semibold">
                              <span style={{ color: palette.textSecondary }}>Минуты</span>
                              <input
                                type="number"
                                min={1}
                                max={30}
                                value={tpl.targetMinutes}
                                onChange={(e) => {
                                  const val = Math.max(1, Number(e.target.value) || 1);
                                  setFreeTemplates((prev) =>
                                    prev.map((item, i) =>
                                      i === idx ? { ...item, targetMinutes: val } : item
                                    )
                                  );
                                }}
                                className="mt-1 w-full rounded-xl px-3 py-2 text-sm font-bold tabular-nums"
                                style={{ backgroundColor: palette.surface }}
                              />
                            </label>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        ) : activeTab === 'chantal' ? (
          <ChantalCompanionView
            energy={chantalEnergy}
            setEnergy={setChantalEnergy}
            coins={chantalCoins}
            setCoins={setChantalCoins}
            currentStreakDays={overviewStats.currentStreak}
            onGoToWorkout={() => setActiveTab('plan')}
            soundEnabled={soundEnabled}
            onPlayTone={playVelarisTone}
          />
        ) : (
          /* SETTINGS SCREEN */
          <div className="max-w-[540px] mx-auto space-y-5">
            <div
              className="rounded-[26px] p-5 sm:p-6 border space-y-4"
              style={{
                backgroundColor: palette.surface,
                borderColor: `${palette.outline}38`,
              }}
            >
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-extrabold">Звук и акустический профиль</h2>
                  <p
                    className="text-xs font-medium mt-0.5"
                    style={{ color: palette.textSecondary }}
                  >
                    Эмоциональные тоны для фаз сокращения и расслабления
                  </p>
                </div>
                <Volume2
                  className="w-5 h-5"
                  style={{ color: palette.primaryAccent }}
                />
              </div>

              <div
                className="rounded-2xl p-4 flex items-center justify-between"
                style={{ backgroundColor: palette.surfaceElevated }}
              >
                <div>
                  <div className="text-sm font-bold">Звуковые сигналы фаз</div>
                  <div
                    className="text-xs mt-0.5"
                    style={{ color: palette.textSecondary }}
                  >
                    Плавное скольжение частоты и субгармоники
                  </div>
                </div>
                <button
                  onClick={() => setSoundEnabled((s) => !s)}
                  className="min-h-[42px] px-4 rounded-xl text-xs font-bold cursor-pointer"
                  style={{
                    backgroundColor: soundEnabled
                      ? palette.primaryAccent
                      : palette.surface,
                    color: soundEnabled ? '#FFFFFF' : palette.textSecondary,
                  }}
                >
                  {soundEnabled ? 'Включены' : 'Выключены'}
                </button>
              </div>

              <div className="grid grid-cols-2 gap-2.5">
                <button
                  onClick={() => playVelarisTone('hold')}
                  className="min-h-[46px] rounded-2xl text-xs font-bold cursor-pointer"
                  style={{
                    backgroundColor: palette.surfaceElevated,
                    color: palette.primaryAccent,
                  }}
                >
                  ▶ Прослушать «Удержание»
                </button>
                <button
                  onClick={() => playVelarisTone('relax')}
                  className="min-h-[46px] rounded-2xl text-xs font-bold cursor-pointer"
                  style={{
                    backgroundColor: palette.surfaceElevated,
                    color: palette.secondaryAccent,
                  }}
                >
                  ▶ Прослушать «Отдых»
                </button>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Compact Active Workout Return Bar above Bottom Nav when workout is running in background */}
      {activeWorkout && !isTimerScreenOpen && (
        <div className="fixed bottom-16 left-0 right-0 z-30 px-4 pb-2">
          <div
            className="max-w-[460px] mx-auto rounded-2xl px-4 py-3 shadow-md flex items-center justify-between gap-3 border"
            style={{
              backgroundColor: palette.surface,
              borderColor: palette.primaryAccent,
            }}
          >
            <div className="min-w-0">
              <div className="text-xs font-extrabold truncate">
                {activeWorkout.titleRu}
              </div>
              <div
                className="text-xs font-semibold tabular-nums"
                style={{ color: palette.primaryAccent }}
              >
                {activeWorkout.isPaused
                  ? 'На паузе'
                  : activeWorkout.phase === 'CONTRACTION'
                  ? 'Удержание'
                  : 'Отдых'}{' '}
                · {activeWorkout.phaseRemainingSec} с · Цикл {activeWorkout.currentRepetition}/
                {activeWorkout.totalRepetitions}
              </div>
            </div>
            <button
              onClick={() => setIsTimerScreenOpen(true)}
              className="min-h-[40px] px-3.5 rounded-xl text-xs font-bold shrink-0 cursor-pointer"
              style={{
                backgroundColor: palette.primaryAccent,
                color: '#FFFFFF',
              }}
            >
              Открыть
            </button>
          </div>
        </div>
      )}

      {/* Fixed 3-Tab Bottom Navigation Bar (Samsung S25 Ultra Thumb Zone) */}
      <nav
        aria-label="Нижняя навигация Velaris"
        className="fixed bottom-0 left-0 right-0 z-40 backdrop-blur-md border-t"
        style={{
          backgroundColor: `${palette.surface}F2`,
          borderColor: `${palette.outline}55`,
        }}
      >
        <div className="max-w-[460px] mx-auto grid grid-cols-4 h-16 items-center px-2">
          <button
            onClick={() => {
              setIsTimerScreenOpen(false);
              setActiveTab('home');
            }}
            className="h-full flex flex-col items-center justify-center cursor-pointer"
            style={{
              color:
                activeTab === 'home' && !isTimerScreenOpen
                  ? palette.primaryAccent
                  : palette.textSecondary,
            }}
          >
            <Home className="w-5 h-5" />
            <span className="text-[11px] font-bold mt-1 whitespace-nowrap">Главная</span>
          </button>

          <button
            onClick={() => {
              setIsTimerScreenOpen(false);
              setActiveTab('plan');
            }}
            className="h-full flex flex-col items-center justify-center cursor-pointer"
            style={{
              color:
                activeTab === 'plan' && !isTimerScreenOpen
                  ? palette.primaryAccent
                  : palette.textSecondary,
            }}
          >
            <Flame className="w-5 h-5" />
            <span className="text-[11px] font-bold mt-1 whitespace-nowrap">Тренировки</span>
          </button>

          <button
            onClick={() => {
              setIsTimerScreenOpen(false);
              setActiveTab('chantal');
            }}
            className="h-full flex flex-col items-center justify-center cursor-pointer"
            style={{
              color:
                activeTab === 'chantal' && !isTimerScreenOpen
                  ? '#EA580C'
                  : palette.textSecondary,
            }}
          >
            <Heart className="w-5 h-5" />
            <span className="text-[11px] font-bold mt-1 whitespace-nowrap">Шанталь</span>
          </button>

          <button
            onClick={() => {
              setIsTimerScreenOpen(false);
              setActiveTab('settings');
            }}
            className="h-full flex flex-col items-center justify-center cursor-pointer"
            style={{
              color:
                activeTab === 'settings' && !isTimerScreenOpen
                  ? palette.primaryAccent
                  : palette.textSecondary,
            }}
          >
            <Settings className="w-5 h-5" />
            <span className="text-[11px] font-bold mt-1 whitespace-nowrap">Настройки</span>
          </button>
        </div>
      </nav>
    </div>
  );
}
