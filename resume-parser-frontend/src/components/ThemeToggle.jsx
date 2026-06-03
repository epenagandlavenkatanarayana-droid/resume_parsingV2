import { useEffect, useState } from 'react';
import { BiSun, BiMoon } from 'react-icons/bi';

/**
 * ThemeToggle – a simple dark/light mode switch.
 * It toggles a "dark" or "light" class on the <html> element and persists the choice.
 */
const ThemeToggle = () => {
  // Initialize from localStorage, default to dark mode for premium look
  const [dark, setDark] = useState(() => {
    const stored = typeof window !== 'undefined' ? localStorage.getItem('theme') : null;
    return stored ? stored === 'dark' : true;
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
    // Persist user preference
    localStorage.setItem('theme', dark ? 'dark' : 'light');
  }, [dark]);

  return (
    <button
      onClick={() => setDark(!dark)}
      className="fixed top-4 right-4 z-50 flex items-center gap-1 p-2 bg-white/10 rounded-full text-white hover:bg-white/20 transition"
      aria-label="Toggle dark/light mode"
    >
      {dark ? <BiMoon size={20} /> : <BiSun size={20} />}
    </button>
  );
};

export default ThemeToggle;
