import { motion } from 'framer-motion';

const AdminDashboard = () => {
  return (
    <motion.div
      className="p-6 bg-white rounded-xl shadow-md"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      <h1 className="text-2xl font-bold text-slate-800 mb-4">Admin Dashboard</h1>
      <p className="text-slate-600">
        Welcome to the Admin panel. Here you can manage system settings, users, and view analytics.
      </p>
      {/* Add admin-specific widgets or analytics components here */}
    </motion.div>
  );
};

export default AdminDashboard;
