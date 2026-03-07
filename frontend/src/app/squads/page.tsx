'use client';

import { useState } from 'react';
import useSWR from 'swr';
import Link from 'next/link';
import AppLayout from '@/components/layout/AppLayout';
import { api } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/context/ToastContext';
import { allocationBadgeClass, cn } from '@/lib/utils';
import type { SquadDto } from '@/types';
import { Users, ChevronRight, Plus, Pencil, Trash2, Search } from 'lucide-react';
import SquadForm from '@/components/squads/SquadForm';

export default function SquadsPage() {
  const { data: squads, error, isLoading, mutate } = useSWR('squads', api.squads.list);
  const { isAdmin } = useAuth();
  const { toast } = useToast();
  const [showForm, setShowForm] = useState(false);
  const [editSquad, setEditSquad] = useState<SquadDto | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [search, setSearch] = useState('');

  async function handleDelete(squad: SquadDto) {
    setDeletingId(squad.id);
    setConfirmDeleteId(null);
    try {
      await api.squads.delete(squad.id);
      toast(`"${squad.name}" deleted`);
      mutate();
    } finally {
      setDeletingId(null);
    }
  }

  const filtered = squads
    ? search.trim()
      ? squads.filter(s => s.name.toLowerCase().includes(search.toLowerCase()))
      : squads
    : [];

  return (
    <AppLayout>
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-semibold text-gray-900">Squads</h1>
            <p className="text-sm text-gray-500 mt-1">
              {squads ? `${filtered.length}${search ? ` of ${squads.length}` : ''} squad${squads.length !== 1 ? 's' : ''}` : '—'}
            </p>
          </div>
          {isAdmin && (
            <button
              onClick={() => { setEditSquad(null); setShowForm(true); }}
              className="flex items-center gap-1.5 text-sm bg-brand-600 text-white px-4 py-2 rounded-lg hover:bg-brand-700 transition-colors"
            >
              <Plus className="w-4 h-4" /> New squad
            </button>
          )}
        </div>

        <div className="relative mb-6 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Search squads…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
          />
        </div>

        {isLoading && (
          <div className="flex justify-center py-20">
            <div className="w-6 h-6 border-2 border-brand-600 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {error && (
          <div className="text-red-600 bg-red-50 border border-red-200 rounded-lg p-4">
            Failed to load squads.
          </div>
        )}

        {squads && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {filtered.map(squad => (
              <SquadCard
                key={squad.id}
                squad={squad}
                isAdmin={isAdmin}
                deleting={deletingId === squad.id}
                confirmingDelete={confirmDeleteId === squad.id}
                onEdit={() => { setEditSquad(squad); setShowForm(true); }}
                onDeleteRequest={() => setConfirmDeleteId(squad.id)}
                onDeleteConfirm={() => handleDelete(squad)}
                onDeleteCancel={() => setConfirmDeleteId(null)}
              />
            ))}
            {filtered.length === 0 && squads.length > 0 && (
              <p className="col-span-full text-center text-gray-400 text-sm py-12">No squads match your search.</p>
            )}
          </div>
        )}
      </div>

      {showForm && (
        <SquadForm
          initial={editSquad}
          onClose={() => { setShowForm(false); setEditSquad(null); }}
          onSaved={() => {
            toast(editSquad ? 'Squad updated' : 'Squad created');
            mutate();
            setShowForm(false);
            setEditSquad(null);
          }}
        />
      )}
    </AppLayout>
  );
}

function SquadCard({
  squad, isAdmin, deleting, confirmingDelete, onEdit, onDeleteRequest, onDeleteConfirm, onDeleteCancel,
}: {
  squad: SquadDto;
  isAdmin: boolean;
  deleting: boolean;
  confirmingDelete: boolean;
  onEdit: () => void;
  onDeleteRequest: () => void;
  onDeleteConfirm: () => void;
  onDeleteCancel: () => void;
}) {
  const avgPct = squad.totalHeadcount > 0
    ? Math.round(squad.totalAllocationPercent / squad.totalHeadcount)
    : 0;

  return (
    <div className="group bg-white border border-gray-100 rounded-xl p-5 shadow-[0_2px_12px_rgba(43,10,91,0.07)] hover:shadow-[0_4px_20px_rgba(43,10,91,0.12)] hover:border-brand-200 transition-all relative">
      {isAdmin && (
        <div className="absolute top-3 right-3 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            onClick={e => { e.preventDefault(); onEdit(); }}
            className="p-1 text-gray-400 hover:text-brand-600 rounded hover:bg-gray-100 transition-colors"
            title="Edit squad"
          >
            <Pencil className="w-3.5 h-3.5" />
          </button>
          {confirmingDelete ? (
            <span className="flex items-center gap-1 text-xs bg-white border border-red-200 rounded px-1.5 py-0.5">
              <button
                onClick={e => { e.preventDefault(); onDeleteConfirm(); }}
                disabled={deleting}
                className="text-red-600 font-medium hover:underline disabled:opacity-50"
              >
                {deleting ? '…' : 'Delete'}
              </button>
              <span className="text-gray-300">·</span>
              <button
                onClick={e => { e.preventDefault(); onDeleteCancel(); }}
                className="text-gray-400 hover:text-gray-600"
              >
                Cancel
              </button>
            </span>
          ) : (
            <button
              onClick={e => { e.preventDefault(); onDeleteRequest(); }}
              className="p-1 text-gray-400 hover:text-red-600 rounded hover:bg-red-50 transition-colors"
              title="Delete squad"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      )}

      <Link href={`/squads/${squad.id}`} className="block">
        <div className="flex items-start justify-between mb-4 pr-12">
          <h2 className="font-semibold text-gray-900 group-hover:text-brand-700 transition-colors leading-tight">
            {squad.name}
          </h2>
          <ChevronRight className="w-4 h-4 text-gray-400 group-hover:text-brand-600 shrink-0 mt-0.5" />
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5 text-sm text-gray-600">
            <Users className="w-3.5 h-3.5" />
            <span>{squad.totalHeadcount} {squad.totalHeadcount === 1 ? 'person' : 'people'}</span>
          </div>
          {squad.totalHeadcount > 0 && (
            <span className={cn('text-xs font-medium px-2 py-0.5 rounded-full', allocationBadgeClass(avgPct))}>
              {avgPct}% avg
            </span>
          )}
        </div>

        <div className="mt-4 flex flex-wrap gap-1">
          {uniqueRoles(squad).slice(0, 4).map(role => (
            <span key={role} className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
              {role}
            </span>
          ))}
        </div>
      </Link>
    </div>
  );
}

function uniqueRoles(squad: SquadDto): string[] {
  const roles = new Set<string>();
  squad.allocations.forEach(a => a.roles.forEach(r => roles.add(r)));
  return Array.from(roles).sort();
}
