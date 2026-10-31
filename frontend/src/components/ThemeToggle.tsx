'use client';

import { Moon, Sun } from 'lucide-react';
import { useTheme } from '@/context/ThemeContext';

export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <button
      type="button"
      onClick={toggleTheme}
      role="switch"
      aria-checked={isDark}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      className="relative inline-flex h-6 w-11 shrink-0 items-center rounded-full bg-white/15 transition-colors hover:bg-white/25 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-300 focus-visible:ring-offset-2 focus-visible:ring-offset-brand-800"
    >
      <span
        className={`absolute left-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-white shadow-sm transition-transform duration-200 ${
          isDark ? 'translate-x-5' : 'translate-x-0'
        }`}
      >
        {isDark
          ? <Moon className="w-3 h-3 text-brand-700" />
          : <Sun className="w-3 h-3 text-amber-500" />}
      </span>
    </button>
  );
}
