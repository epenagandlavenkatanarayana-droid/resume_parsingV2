import { useState, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { BiEnvelope, BiKey, BiArrowBack, BiShieldQuarter, BiLockAlt } from 'react-icons/bi';
import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext';
import { toast } from 'react-toastify';

const Login = () => {
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();

  useEffect(() => {
    let timer;
    if (otpSent && cooldown > 0) {
      timer = setInterval(() => {
        setCooldown((prev) => prev - 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [otpSent, cooldown]);

  const handleSendOtp = async (e) => {
    if (e) e.preventDefault();
    if (!email) {
      toast.warn('Please enter a valid email address.');
      return;
    }
    setIsLoading(true);
    try {
      await api.post('/auth/send-otp', { email });
      setOtpSent(true);
      setCooldown(60);
      toast.success('Verification code sent! Please check your email.');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Access denied. Is your email registered?');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!email || !otp) {
      toast.warn('Please enter both email and OTP.');
      return;
    }
    setIsLoading(true);
    try {
      const response = await api.post('/auth/login', { email, otp });
      const { token, role, name } = response.data;
      
      const user = { name, email, role };
      login(user, token);
      toast.success('Login successfully');
      
      if (role === 'ADMIN') {
        navigate('/admin/dashboard');
      } else if (role === 'HR') {
        navigate('/hr/dashboard');
      } else {
        navigate('/upload-resume');
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Invalid or expired OTP. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleBackToEmail = () => {
    setOtpSent(false);
    setOtp('');
    setCooldown(0);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 relative overflow-hidden px-4">
      {/* Background Animated Gradient Mesh */}
      <div className="absolute inset-0 overflow-hidden">
        <motion.div
          animate={{
            scale: [1, 1.2, 1],
            x: [0, 50, 0],
            y: [0, -30, 0],
          }}
          transition={{
            duration: 15,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          className="absolute -top-40 -left-40 w-96 h-96 bg-indigo-600/20 rounded-full blur-[140px] pointer-events-none"
        />
        <motion.div
          animate={{
            scale: [1, 1.1, 1],
            x: [0, -40, 0],
            y: [0, 50, 0],
          }}
          transition={{
            duration: 18,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          className="absolute -bottom-40 -right-40 w-96 h-96 bg-blue-600/20 rounded-full blur-[140px] pointer-events-none"
        />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-violet-600/10 rounded-full blur-[160px] pointer-events-none" />
      </div>

      <div className="max-w-md w-full relative z-10">
        {/* Glow effect surrounding the card */}
        <div className="absolute -inset-0.5 bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-600 rounded-2xl blur-lg opacity-30 group-hover:opacity-100 transition duration-1000 group-hover:duration-200 animate-tilt"></div>

        {/* The Card */}
        <div className="relative bg-slate-900/60 backdrop-blur-2xl rounded-2xl shadow-[0_0_50px_rgba(0,0,0,0.3)] border border-slate-800/80 overflow-hidden">
          <div className="p-8">
            <div className="text-center mb-8">
              <div className="inline-flex p-3 bg-gradient-to-br from-indigo-500/10 to-blue-500/10 rounded-2xl text-blue-400 mb-4 border border-blue-500/20 shadow-inner">
                <BiShieldQuarter size={36} className="animate-pulse text-indigo-400" />
              </div>
              <h1 className="text-3xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-white via-slate-100 to-slate-300 tracking-tight mb-2">
                Resume<span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-400">Parser</span>
              </h1>
              <p className="text-slate-400 text-sm font-medium">HR & Admin Access Portal</p>
            </div>

            <AnimatePresence mode="wait">
              {!otpSent ? (
                <motion.form
                  key="email-form"
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  transition={{ duration: 0.2 }}
                  onSubmit={handleSendOtp}
                  className="space-y-6"
                >
                  <div className="space-y-2">
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">Registered Email Address</label>
                    <div className="relative group">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-blue-400 transition-colors">
                        <BiEnvelope size={20} />
                      </span>
                      <input
                        type="email"
                        required
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full pl-10 pr-4 py-3 bg-slate-950/40 border border-slate-800 rounded-xl text-white placeholder-slate-600 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all text-base shadow-inner"
                        placeholder="your-email@example.com"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={isLoading}
                    className="w-full relative group overflow-hidden bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 disabled:from-slate-800 disabled:to-slate-800 text-white font-semibold py-3.5 rounded-xl transition-all shadow-[0_4px_20px_rgba(79,70,229,0.25)] hover:shadow-[0_4px_25px_rgba(79,70,229,0.4)] flex justify-center items-center gap-2 cursor-pointer active:scale-[0.98]"
                  >
                    {isLoading ? (
                      <span className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                    ) : (
                      <>
                        <span>Request Verification Code</span>
                      </>
                    )}
                  </button>
                </motion.form>
              ) : (
                <motion.form
                  key="otp-form"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.2 }}
                  onSubmit={handleLogin}
                  className="space-y-6"
                >
                  <div className="space-y-4">
                    <div className="flex justify-between items-center">
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">Verification Code</label>
                      <button
                        type="button"
                        onClick={handleBackToEmail}
                        className="text-xs text-blue-400 hover:text-blue-300 flex items-center gap-1 transition-colors cursor-pointer font-medium"
                      >
                        <BiArrowBack size={12} /> Change Email
                      </button>
                    </div>
                    
                    <div className="text-xs text-slate-300 bg-slate-950/60 p-3.5 rounded-xl border border-slate-800/80 leading-relaxed">
                      We sent a 6-digit OTP code to: <br />
                      <span className="font-semibold text-indigo-300 mt-1 inline-block break-all">{email}</span>
                    </div>

                    <div className="relative group">
                      <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-indigo-400 transition-colors">
                        <BiKey size={20} />
                      </span>
                      <input
                        type="text"
                        maxLength={6}
                        required
                        value={otp}
                        onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                        className="w-full pl-12 pr-4 py-3.5 bg-slate-950/50 border border-slate-800 rounded-xl text-white text-center font-bold tracking-[0.5em] text-2xl placeholder-slate-800 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all shadow-inner"
                        placeholder="••••••"
                        autoFocus
                      />
                    </div>
                  </div>

                  <div className="flex flex-col gap-4">
                    <button
                      type="submit"
                      disabled={isLoading}
                      className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 disabled:from-slate-800 disabled:to-slate-800 text-white font-semibold py-3.5 rounded-xl transition-all shadow-[0_4px_20px_rgba(79,70,229,0.25)] hover:shadow-[0_4px_25px_rgba(79,70,229,0.4)] flex justify-center items-center gap-2 cursor-pointer active:scale-[0.98]"
                    >
                      {isLoading ? (
                        <span className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                      ) : (
                        'Verify & Login'
                      )}
                    </button>

                    <div className="text-center">
                      {cooldown > 0 ? (
                        <span className="text-xs text-slate-500">
                          Resend code in <span className="font-semibold text-slate-300 bg-slate-850 px-2 py-0.5 rounded border border-slate-800">{cooldown}s</span>
                        </span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => handleSendOtp()}
                          disabled={isLoading}
                          className="text-xs text-blue-400 hover:text-blue-300 font-semibold transition-colors cursor-pointer hover:underline"
                        >
                          Resend Verification Code
                        </button>
                      )}
                    </div>
                  </div>
                </motion.form>
              )}
            </AnimatePresence>
          </div>

          {/* Secure Footer */}
          <div className="px-8 py-4 bg-slate-950/40 border-t border-slate-850 flex items-center justify-center gap-2">
            <BiLockAlt size={14} className="text-slate-500" />
            <span className="text-[10px] uppercase tracking-wider text-slate-500 font-semibold">End-to-End Secure Authorization</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
