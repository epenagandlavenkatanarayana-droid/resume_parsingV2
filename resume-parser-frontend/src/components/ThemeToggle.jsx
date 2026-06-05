import { useEffect, useState } from 'react';
import { BiSun, BiMoon } from 'react-icons/bi';

/**
 * ThemeToggle – a simple, reusable dark/light mode switch.
 * It toggles a "dark" or "light" class on the <html> element and persists the choice.
 */
const ThemeToggle = ({ className = "" }) => {
  // Initialize from localStorage, default to light mode (false) to match main panels
  const [dark, setDark] = useState(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem('theme') : null;
    return stored ? stored === 'dark' : false;
  });

  useEffect(() => {
    const root = document.documentElement;
    if (dark) {
      root.classList.add('dark');
      root.classList.remove('light');
    } else {
      root.classList.remove('dark');
      root.classList.add('light');
    }
    localStorage.setItem('theme', dark ? 'dark' : 'light');
  }, [dark]);

  return (
    <button
      onClick={() => setDark(!dark)}
      className={`p-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-500 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-700/50 hover:text-slate-850 dark:hover:text-slate-200 transition-all duration-200 shadow-sm flex items-center justify-center ${className}`}
      aria-label="Toggle dark/light mode"
    >
      {dark ? <BiSun className="text-xl" /> : <BiMoon className="text-xl" />}
    </button>
  );
};

export default ThemeToggle;
