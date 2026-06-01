import { NavLink } from 'react-router-dom';

const Sidebar = ({ links }) => {
  return (
    <aside className="w-64 bg-surface border-r border-slate-200 h-[calc(100vh-4rem)] hidden md:block overflow-y-auto">
      <div className="py-6 px-4 flex flex-col gap-2">
        {links.map((link) => (
          <NavLink
            key={link.path}
            to={link.path}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 rounded-lg font-medium transition-colors duration-200 ${
                isActive
                  ? 'bg-primary/10 text-primary'
                  : 'text-text-light hover:bg-slate-100 hover:text-text'
              }`
            }
          >
            <span className="text-xl">{link.icon}</span>
            {link.label}
          </NavLink>
        ))}
      </div>
    </aside>
  );
};

export default Sidebar;
