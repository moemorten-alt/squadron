'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { useRouter } from 'next/navigation';
import AppLayout from '@/components/layout/AppLayout';
import { api } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/context/ToastContext';
import type { UserDto } from '@/types';
import { Plus, X, Shield, Eye, Trash2 } from 'lucide-react';

export default function UsersPage() {
  const router = useRouter();
  const { isAdmin } = useAuth();
  const { toast } = useToast();
  const { data: users, isLoading, error, mutate } = useSWR('users', api.users.list);

  const [showForm, setShowForm] = useState(false);
  const [editUser, setEditUser] = useState<UserDto | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  // Redirect non-admins
  if (!isAdmin) {
    router.replace('/squads');
    return null;
  }

  async function handleDelete(user: UserDto) {
    setConfirmDeleteId(null);
    setDeletingId(user.id);
    try {
      await api.users.delete(user.id);
      toast(`${user.email} deleted`);
      mutate();
    } finally {
      setDeletingId(null);
    }
  }

  async function handleToggleActive(user: UserDto) {
    await api.users.update(user.id, { active: !user.active });
    toast(user.active ? `${user.email} deactivated` : `${user.email} activated`);
    mutate();
  }

  return (
    <AppLayout>
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-semibold text-gray-900">Users</h1>
            <p className="text-sm text-gray-500 mt-1">Manage who can log in and their access level</p>
          </div>
          <button
            onClick={() => { setEditUser(null); setShowForm(true); }}
            className="flex items-center gap-1.5 text-sm bg-brand-600 text-white px-4 py-2 rounded-lg hover:bg-brand-700 transition-colors"
          >
            <Plus className="w-4 h-4" /> New user
          </button>
        </div>

        {isLoading && (
          <div className="flex justify-center py-20">
            <div className="w-6 h-6 border-2 border-brand-600 border-t-transparent rounded-full animate-spin" />
          </div>
        )}
        {error && <div className="text-red-600 bg-red-50 border border-red-200 rounded-lg p-4">Failed to load users.</div>}

        {users && (
          <div className="bg-white border border-gray-100 rounded-xl overflow-hidden shadow-[0_2px_12px_rgba(43,10,91,0.07)]">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-5 py-3 font-medium text-gray-600">Email</th>
                  <th className="text-left px-5 py-3 font-medium text-gray-600">Role</th>
                  <th className="text-left px-5 py-3 font-medium text-gray-600">Status</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {users.map(user => (
                  <tr key={user.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-5 py-3.5 text-gray-900">{user.email}</td>
                    <td className="px-5 py-3.5">
                      {user.role === 'ADMIN' ? (
                        <span className="inline-flex items-center gap-1 text-xs font-medium text-brand-700 bg-brand-50 border border-brand-200 px-2 py-0.5 rounded-full">
                          <Shield className="w-3 h-3" /> Admin
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs font-medium text-gray-600 bg-gray-100 px-2 py-0.5 rounded-full">
                          <Eye className="w-3 h-3" /> Viewer
                        </span>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      <button
                        onClick={() => handleToggleActive(user)}
                        className={`text-xs font-medium px-2 py-0.5 rounded-full transition-colors ${
                          user.active
                            ? 'bg-green-50 text-green-700 hover:bg-green-100'
                            : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                        }`}
                      >
                        {user.active ? 'Active' : 'Inactive'}
                      </button>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => { setEditUser(user); setShowForm(true); }}
                          className="text-xs text-gray-500 hover:text-brand-700 transition-colors"
                        >
                          Edit
                        </button>
                        {confirmDeleteId === user.id ? (
                          <span className="flex items-center gap-1 text-xs">
                            <button
                              onClick={() => handleDelete(user)}
                              disabled={deletingId === user.id}
                              className="text-red-600 font-medium hover:underline disabled:opacity-50"
                            >
                              {deletingId === user.id ? '…' : 'Confirm'}
                            </button>
                            <span className="text-gray-300">·</span>
                            <button
                              onClick={() => setConfirmDeleteId(null)}
                              className="text-gray-400 hover:text-gray-600"
                            >
                              Cancel
                            </button>
                          </span>
                        ) : (
                          <button
                            onClick={() => setConfirmDeleteId(user.id)}
                            disabled={deletingId === user.id}
                            className="text-xs text-gray-400 hover:text-red-600 transition-colors disabled:opacity-50"
                          >
                            {deletingId === user.id ? '…' : 'Delete'}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
                {users.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-5 py-10 text-center text-gray-400 text-sm">No users yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showForm && (
        <UserForm
          initial={editUser}
          onClose={() => { setShowForm(false); setEditUser(null); }}
          onSaved={() => {
            toast(editUser ? 'User updated' : 'User created');
            mutate();
            setShowForm(false);
            setEditUser(null);
          }}
        />
      )}
    </AppLayout>
  );
}

function UserForm({ initial, onClose, onSaved }: { initial: UserDto | null; onClose: () => void; onSaved: () => void }) {
  const [email, setEmail] = useState(initial?.email ?? '');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'ADMIN' | 'VIEWER'>(initial?.role ?? 'VIEWER');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!initial && !password) { setError('Password is required'); return; }
    setSaving(true);
    setError('');
    try {
      if (initial) {
        await api.users.update(initial.id, {
          role,
          ...(password ? { password } : {}),
        });
      } else {
        await api.users.create({ email, password, role });
      }
      onSaved();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="font-semibold text-gray-900">{initial ? 'Edit user' : 'New user'}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X className="w-5 h-5" /></button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          {error && (
            <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-4 py-2">{error}</div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required={!initial}
              disabled={!!initial}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-50 disabled:text-gray-500"
              placeholder="user@example.com"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Password {initial && <span className="text-gray-400 font-normal">(leave blank to keep current)</span>}
            </label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required={!initial}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
              placeholder={initial ? '••••••••' : 'Minimum 6 characters'}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Role</label>
            <div className="flex gap-2">
              {(['VIEWER', 'ADMIN'] as const).map(r => (
                <button
                  key={r}
                  type="button"
                  onClick={() => setRole(r)}
                  className={`flex-1 flex items-center justify-center gap-1.5 text-sm py-2 rounded-lg border font-medium transition-colors ${
                    role === r
                      ? 'bg-brand-600 text-white border-brand-600'
                      : 'bg-white text-gray-600 border-gray-300 hover:border-brand-400'
                  }`}
                >
                  {r === 'ADMIN' ? <Shield className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                  {r === 'ADMIN' ? 'Admin' : 'Viewer'}
                </button>
              ))}
            </div>
            <p className="text-xs text-gray-500 mt-1.5">
              {role === 'ADMIN' ? 'Full access: create, edit, delete, see private notes.' : 'Read-only access. Cannot see admin notes or tags.'}
            </p>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">Cancel</button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 text-sm bg-brand-600 text-white rounded-lg hover:bg-brand-700 disabled:opacity-50 transition-colors"
            >
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
