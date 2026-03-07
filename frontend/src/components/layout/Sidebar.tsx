'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { LayoutGrid, Users, LogOut, Shield, UserCog } from 'lucide-react';
import { cn } from '@/lib/utils';

const navItems = [
  { href: '/persons', label: 'People',  icon: Users },
  { href: '/squads',  label: 'Squads',  icon: LayoutGrid },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { user, isAdmin, logout } = useAuth();

  return (
    <aside className="flex flex-col w-56 min-h-screen bg-brand-800 px-3 py-5 shrink-0">
      {/* Logo */}
      <Link href="/persons" className="flex items-center gap-2.5 px-2 mb-8">
        <div className="w-8 h-8 rounded-lg bg-white/15 flex items-center justify-center">
          <span className="text-white font-bold text-sm">S</span>
        </div>
        <span className="font-semibold text-white text-lg tracking-tight">Squadron</span>
      </Link>

      {/* Navigation */}
      <nav className="flex-1 space-y-0.5">
        {navItems.map(({ href, label, icon: Icon }) => (
          <Link
            key={href}
            href={href}
            className={cn(
              'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
              pathname.startsWith(href)
                ? 'bg-white/15 text-white'
                : 'text-white/60 hover:bg-white/10 hover:text-white'
            )}
          >
            <Icon className="w-4 h-4" />
            {label}
          </Link>
        ))}
        {isAdmin && (
          <Link
            href="/users"
            className={cn(
              'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
              pathname.startsWith('/users')
                ? 'bg-white/15 text-white'
                : 'text-white/60 hover:bg-white/10 hover:text-white'
            )}
          >
            <UserCog className="w-4 h-4" />
            Users
          </Link>
        )}
      </nav>

      {/* Bottom: user + role + logout */}
      <div className="border-t border-white/10 pt-4 mt-4 space-y-2">
        {isAdmin && (
          <div className="flex items-center gap-2 px-3 py-1">
            <Shield className="w-3.5 h-3.5 text-brand-300" />
            <span className="text-xs font-medium text-brand-300">Admin</span>
          </div>
        )}
        <div className="px-3 text-xs text-white/40 truncate">{user?.email}</div>
        <button
          onClick={logout}
          className="flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm text-white/60 hover:bg-white/10 hover:text-white transition-colors"
        >
          <LogOut className="w-4 h-4" />
          Sign out
        </button>
      </div>
    </aside>
  );
}
