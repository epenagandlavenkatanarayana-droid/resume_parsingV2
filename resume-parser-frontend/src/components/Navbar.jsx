import { useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { BiLogOut, BiUserCircle } from 'react-icons/bi';
import { MdOutlineLibraryBooks } from 'react-icons/md';

const Navbar = () => {
  const { user, logout, isAuthenticated } = useContext(AuthContext);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="h-14 md:h-16 bg-white shadow-sm border-b border-slate-200 flex items-center justify-between px-4 md:px-6 sticky top-0 z-40">
      {/* Logo */}
      <div className="flex items-center gap-2">
        <div className="bg-blue-600 text-white p-1.5 md:p-2 rounded-lg">
          <MdOutlineLibraryBooks className="text-lg md:text-xl" />
        </div>
        <Link to="/" className="text-lg md:text-xl font-bold text-slate-900 tracking-tight">
          Resume<span className="text-blue-600">Parser</span>
        </Link>
      </div>

      {/* Right side */}
      <div className="flex items-center gap-2 md:gap-4">
        {isAuthenticated ? (
          <>
            {/* User chip */}
            <div className="flex items-center gap-1.5 text-slate-600 font-medium bg-slate-50 px-2.5 py-1.5 rounded-full border border-slate-200 text-sm">
              <BiUserCircle className="text-xl text-slate-400 flex-shrink-0" />
              <span className="hidden sm:inline max-w-[120px] truncate">{user?.name || 'User'}</span>
            </div>

            {/* Logout */}
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 text-red-500 hover:bg-red-50 px-2.5 py-2 rounded-lg transition-colors font-medium text-sm"
              title="Logout"
            >
              <BiLogOut className="text-lg" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </>
        ) : (
          <Link
            to="/login"
            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg font-semibold transition-colors shadow-sm text-sm"
          >
            Login
          </Link>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
