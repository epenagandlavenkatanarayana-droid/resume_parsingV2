import React from 'react';
import { motion } from 'framer-motion';

/**
 * GlassCard – a reusable glass‑morphism container.
 * Applies backdrop blur, subtle border and shadow.
 * Accepts optional hover scale via Framer Motion.
 */
const GlassCard = ({ children, className = '' }) => {
  return (
    <motion.div
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      className={`backdrop-blur-xl bg-white/5 border border-white/10 rounded-2xl shadow-lg ${className}`}
    >
      {children}
    </motion.div>
  );
};

export default GlassCard;
