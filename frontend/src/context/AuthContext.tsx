'use client';

import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import type { AuthUser } from '@/types';

interface AuthContextType {
  user: AuthUser | null;
  isAdmin: boolean;
  loading: boolean;
  login: (token: string, email: string, role: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('squadron_token');
    const email = localStorage.getItem('squadron_email');
    const role  = localStorage.getItem('squadron_role');
    if (token && email && role) {
      setUser({ token, email, role: role as AuthUser['role'] });
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    const handler = () => {
      localStorage.removeItem('squadron_token');
      localStorage.removeItem('squadron_email');
      localStorage.removeItem('squadron_role');
      setUser(null);
      router.push('/login');
    };
    window.addEventListener('auth:expired', handler);
    return () => window.removeEventListener('auth:expired', handler);
  }, [router]);

  const login = (token: string, email: string, role: string) => {
    localStorage.setItem('squadron_token', token);
    localStorage.setItem('squadron_email', email);
    localStorage.setItem('squadron_role',  role);
    setUser({ token, email, role: role as AuthUser['role'] });
  };

  const logout = () => {
    localStorage.removeItem('squadron_token');
    localStorage.removeItem('squadron_email');
    localStorage.removeItem('squadron_role');
    setUser(null);
    router.push('/login');
  };

  return (
    <AuthContext.Provider value={{ user, isAdmin: user?.role === 'ADMIN', loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}
