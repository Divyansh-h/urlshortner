import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, Link2, LogOut, Hexagon, Flag } from 'lucide-react';

const Sidebar = () => {
  const { logout } = useAuth();

  return (
    <div className="sidebar">
      <div className="sidebar-logo">
        <Hexagon size={28} />
        <span>URLShort</span>
      </div>

      <nav style={{ flex: 1 }}>
        <NavLink 
          to="/dashboard" 
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <LayoutDashboard size={20} />
          Dashboard
        </NavLink>
        <NavLink 
          to="/urls" 
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <Link2 size={20} />
          URLs
        </NavLink>
        <NavLink 
          to="/flags" 
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <Flag size={20} />
          Feature Flags
        </NavLink>
      </nav>

      <button onClick={logout} className="nav-link" style={{ background: 'transparent', border: 'none', cursor: 'pointer', width: '100%', textAlign: 'left' }}>
        <LogOut size={20} />
        Sign Out
      </button>
    </div>
  );
};

export default Sidebar;
