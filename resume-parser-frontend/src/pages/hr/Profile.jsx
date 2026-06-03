import { useState, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import api from '../../services/api';
import { toast } from 'react-toastify';
import { motion } from 'framer-motion';
import { BiUser, BiEnvelope, BiPhone, BiLockAlt, BiSave } from 'react-icons/bi';

const Profile = () => {
  const { user, updateUser } = useContext(AuthContext);
  const [formData, setFormData] = useState({
    fullName: user?.name || '',
    email: user?.email || '',
    phoneNumber: user?.phone || user?.phoneNumber || '',
    password: '',
    confirmPassword: '',
  });
  const [saving, setSaving] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.password && formData.password !== formData.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }

    try {
      setSaving(true);
      const response = await api.put('/hr/profile', {
        fullName: formData.fullName,
        phoneNumber: formData.phoneNumber,
        password: formData.password || null,
      });

      toast.success(response.data.message || 'Profile updated successfully');
      
      // Update local storage and AuthContext state
      updateUser({
        name: formData.fullName,
        phone: formData.phoneNumber,
        phoneNumber: formData.phoneNumber
      });
      
      setFormData((prev) => ({ ...prev, password: '', confirmPassword: '' }));
    } catch (error) {
      console.error(error);
      toast.error(error.response?.data?.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* ── Page Header ── */}
      <div>
        <h1 className="text-xl md:text-2xl font-bold text-slate-900 font-syne">Profile Settings</h1>
        <p className="text-slate-500 text-sm mt-1">Update your personal details and account settings</p>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm"
      >
        {/* Cover Block / Avatar Header */}
        <div className="h-32 bg-gradient-to-r from-blue-500 to-indigo-600 relative">
          <div className="absolute -bottom-10 left-8">
            <div className="w-20 h-20 rounded-2xl bg-white border-4 border-white shadow-md flex items-center justify-center text-blue-600 text-3xl font-black font-syne">
              {formData.fullName?.charAt(0)?.toUpperCase() || 'H'}
            </div>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-8 pt-14 space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Full Name */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600 flex items-center gap-1.5">
                <BiUser className="text-slate-400 text-sm" /> Full Name
              </label>
              <input
                type="text"
                name="fullName"
                required
                placeholder="Enter your name"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-white focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 text-sm transition-all"
                value={formData.fullName}
                onChange={handleChange}
              />
            </div>

            {/* Email Address (Disabled) */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600 flex items-center gap-1.5">
                <BiEnvelope className="text-slate-400 text-sm" /> Email Address
              </label>
              <input
                type="email"
                name="email"
                disabled
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-slate-50 text-slate-400 cursor-not-allowed text-sm"
                value={formData.email}
              />
            </div>

            {/* Phone Number */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600 flex items-center gap-1.5">
                <BiPhone className="text-slate-400 text-sm" /> Phone Number
              </label>
              <input
                type="tel"
                name="phoneNumber"
                placeholder="Enter your phone number"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-white focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 text-sm transition-all"
                value={formData.phoneNumber}
                onChange={handleChange}
              />
            </div>
          </div>

          <hr className="border-slate-100 my-6" />

          <h3 className="text-sm font-bold text-slate-900 font-syne">Change Password</h3>
          <p className="text-xs text-slate-400">Leave these blank if you do not want to change your password</p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* New Password */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600 flex items-center gap-1.5">
                <BiLockAlt className="text-slate-400 text-sm" /> New Password
              </label>
              <input
                type="password"
                name="password"
                placeholder="••••••••"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-white focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 text-sm transition-all"
                value={formData.password}
                onChange={handleChange}
              />
            </div>

            {/* Confirm Password */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-650 flex items-center gap-1.5">
                <BiLockAlt className="text-slate-400 text-sm" /> Confirm Password
              </label>
              <input
                type="password"
                name="confirmPassword"
                placeholder="••••••••"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 bg-white focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 text-sm transition-all"
                value={formData.confirmPassword}
                onChange={handleChange}
              />
            </div>
          </div>

          <div className="flex justify-end pt-4">
            <button
              type="submit"
              disabled={saving}
              className="flex items-center gap-2 px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold transition-all disabled:opacity-50"
            >
              <BiSave className="text-base" />
              {saving ? 'Saving...' : 'Save Settings'}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
};

export default Profile;
