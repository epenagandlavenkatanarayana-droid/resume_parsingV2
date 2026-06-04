import { useState, useContext } from 'react';
import { Outlet, NavLink, useNavigate, useLocation } from 'react-router-dom';
import { BiLogOut, BiUser, BiBriefcase, BiMenu, BiX } from 'react-icons/bi';
import { AuthContext } from '../context/AuthContext';
import api from '../services/api';
import { toast } from 'react-toastify';
import { motion, AnimatePresence } from 'framer-motion';

const HrLayout = () => {
  const { user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (error) {
      console.warn(
        'Server logout failed, clearing session locally:',
        error?.response?.status
      );
    } finally {
      logout();
      toast.success('Logout successfully');
      navigate('/login');
    }
  };

  const menuItems = [
    {
      path: '/hr/dashboard',
      name: 'Resumes Dashboard',
      icon: <BiBriefcase className="text-xl" />,
      subItems: [
        { filter: 'all', name: 'All Resumes', dotColor: 'bg-blue-500' },
        { filter: 'eligible', name: 'Eligible', dotColor: 'bg-emerald-500' },
        { filter: 'shortlisted', name: 'Shortlisted', dotColor: 'bg-amber-500' },
        { filter: 'not_eligible', name: 'Not Eligible', dotColor: 'bg-rose-500' },
      ],
    },
    {
      path: '/hr/profile',
      name: 'Profile Settings',
      icon: <BiUser className="text-xl" />,
    },
  ];

  const SidebarContent = () => (
    <div className="flex flex-col h-full">
      {/* Logo */}
      <div className="p-5 border-b border-slate-100">
        <div className="flex items-center gap-3">
          <div className="bg-blue-600 text-white p-2 rounded-lg flex-shrink-0">
            <BiUser className="text-xl" />
          </div>
          <h1 className="text-lg font-bold text-slate-900 leading-tight">
            Resume Parser <span className="text-blue-600">HR</span>
          </h1>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-grow py-4 px-3 space-y-1">
        {menuItems.map((item) => {
          const hasSubItems = item.subItems && item.subItems.length > 0;
          return (
            <div key={item.path} className="space-y-1">
              <NavLink
                to={item.path}
                state={hasSubItems ? { filter: 'all' } : undefined}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-4 py-3 rounded-xl font-medium transition-all duration-200 text-sm ${isActive
                    ? 'bg-blue-50 text-blue-600 shadow-sm'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                {item.icon}
                {item.name}
              </NavLink>
              
              {hasSubItems && (
                <div className="pl-4 space-y-1 transition-all duration-200">
                  {item.subItems.map((subItem) => {
                    const isSubActive =
                      location.pathname === item.path &&
                      (location.state?.filter || 'all') === subItem.filter;
                    return (
                      <NavLink
                        key={subItem.filter}
                        to={item.path}
                        state={{ filter: subItem.filter }}
                        onClick={() => setSidebarOpen(false)}
                        className={`flex items-center gap-2.5 pl-5 pr-4 py-2 rounded-lg font-medium transition-all duration-150 text-xs ${
                          isSubActive
                            ? 'bg-blue-50/50 text-blue-600 font-semibold shadow-sm'
                            : 'text-slate-500 hover:bg-slate-50 hover:text-slate-800'
                        }`}
                      >
                        <span className={`w-1.5 h-1.5 rounded-full ${subItem.dotColor}`} />
                        {subItem.name}
                      </NavLink>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </nav>

      {/* Logout - Fixed Bottom */}
      <div className="mt-auto p-3 border-t border-slate-100">
        <button
          onClick={handleLogout}
          className="flex items-center gap-3 w-full px-4 py-3 text-red-600 hover:bg-red-50 rounded-xl font-medium transition-colors text-sm"
        >
          <BiLogOut className="text-xl flex-shrink-0" />
          Logout
        </button>
      </div>
    </div>
  );

  return (
    <div className="flex h-screen bg-slate-50 overflow-hidden">
      {/* Desktop Sidebar */}
      <aside className="hidden md:flex flex-col h-screen w-64 bg-white border-r border-slate-200 shadow-sm flex-shrink-0">
        <SidebarContent />
      </aside>

      {/* Mobile Sidebar */}
      <AnimatePresence>
        {sidebarOpen && (
          <>
            {/* Backdrop */}
            <motion.div
              key="backdrop"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              className="fixed inset-0 bg-black/50 z-40 md:hidden"
              onClick={() => setSidebarOpen(false)}
            />

            {/* Drawer */}
            <motion.aside
              key="drawer"
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'tween', duration: 0.25 }}
              className="fixed top-0 left-0 bottom-0 w-72 bg-white z-50 flex flex-col shadow-2xl md:hidden"
            >
              {/* Close Button */}
              <button
                onClick={() => setSidebarOpen(false)}
                className="absolute top-4 right-4 p-1.5 rounded-lg hover:bg-slate-100 text-slate-500 transition-colors"
              >
                <BiX className="text-2xl" />
              </button>

              <SidebarContent />
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Header */}
        <header className="h-14 md:h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 md:px-8 flex-shrink-0 shadow-sm">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setSidebarOpen(true)}
              className="md:hidden p-2 rounded-lg hover:bg-slate-100 text-slate-600 transition-colors"
              aria-label="Open sidebar"
            >
              <BiMenu className="text-2xl" />
            </button>

            <h2 className="text-base md:text-xl font-bold text-slate-900">
              HR Panel
            </h2>
          </div>

          {user && (
            <div className="hidden md:flex items-center gap-2">
              <div className="h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center">
                <BiUser className="text-blue-600" />
              </div>
              <span className="text-sm font-medium text-slate-700">
                {user?.name || 'HR User'}
              </span>
            </div>
          )}
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-auto p-4 md:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default HrLayout;