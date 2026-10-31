'use client';

import { useState, useEffect } from 'react';
import useSWR from 'swr';
import { api } from '@/lib/api';
import type { AllocationDto, CreateAllocationRequest } from '@/types';
import { X } from 'lucide-react';

interface Props {
  initial?: AllocationDto | null;
  defaultPersonId?: number;
  defaultSquadId?: number;
  onClose: () => void;
  onSaved: () => void;
}

export default function AllocationForm({ initial, defaultPersonId, defaultSquadId, onClose, onSaved }: Props) {
  const { data: persons }  = useSWR('persons-list',  api.persons.list);
  const { data: squads }   = useSWR('squads-list',   api.squads.list);
  const { data: roles }    = useSWR('roles',          api.lookup.roles);
  const { data: techs }    = useSWR('techs',          api.lookup.technologies);

  const [personId,       setPersonId]       = useState<number>(initial?.personId    ?? defaultPersonId ?? 0);
  const [squadId,        setSquadId]        = useState<number>(initial?.squadId     ?? defaultSquadId  ?? 0);
  const [selectedRoles,  setSelectedRoles]  = useState<Set<number>>(new Set());
  const [selectedTechs,  setSelectedTechs]  = useState<Set<number>>(new Set());
  const [percent,        setPercent]        = useState<number>(initial?.allocationPercent ?? 100);
  const [publicComment,  setPublicComment]  = useState(initial?.publicComment ?? '');
  const [adminNote,      setAdminNote]      = useState(initial?.adminNote ?? '');
  const [startDate,      setStartDate]      = useState(initial?.startDate ?? '');
  const [endDate,        setEndDate]        = useState(initial?.endDate ?? '');
  const [saving,         setSaving]         = useState(false);
  const [error,          setError]          = useState('');

  // Pre-select roles/techs when editing
  useEffect(() => {
    if (!initial || !roles || !techs) return;
    const rIds = new Set(roles.filter(r => initial.roles.includes(r.name)).map(r => r.id));
    const tIds = new Set(techs.filter(t => initial.technologies.includes(t.name)).map(t => t.id));
    setSelectedRoles(rIds);
    setSelectedTechs(tIds);
  }, [initial, roles, techs]);

  const toggle = (set: Set<number>, id: number): Set<number> => {
    const next = new Set(set);
    next.has(id) ? next.delete(id) : next.add(id);
    return next;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!personId || !squadId || selectedRoles.size === 0) {
      setError('Person, squad and at least one role are required.');
      return;
    }

    const req: CreateAllocationRequest = {
      personId,
      squadId,
      roleIds: Array.from(selectedRoles),
      technologyIds: Array.from(selectedTechs),
      allocationPercent: percent,
      publicComment: publicComment || undefined,
      adminNote: adminNote || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    };

    setSaving(true);
    setError('');
    try {
      if (initial) {
        await api.allocations.update(initial.id, req);
      } else {
        await api.allocations.create(req);
      }
      onSaved();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-white dark:bg-brand-800 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto m-4">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-white/10 sticky top-0 bg-white dark:bg-brand-800">
          <h2 className="font-semibold text-gray-900 dark:text-white">{initial ? 'Edit allocation' : 'New allocation'}</h2>
          <button onClick={onClose} className="p-1 hover:bg-gray-100 dark:hover:bg-white/10 rounded-lg transition-colors">
            <X className="w-4 h-4 text-gray-500 dark:text-white/50" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {/* Person + Squad */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-1">Person *</label>
              <select
                value={personId}
                onChange={e => setPersonId(Number(e.target.value))}
                className="w-full px-3 py-2 border border-gray-300 dark:border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
              >
                <option value={0}>Select…</option>
                {persons?.sort((a, b) => a.name.localeCompare(b.name)).map(p => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-1">Squad *</label>
              <select
                value={squadId}
                onChange={e => setSquadId(Number(e.target.value))}
                className="w-full px-3 py-2 border border-gray-300 dark:border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
              >
                <option value={0}>Select…</option>
                {squads?.sort((a, b) => a.name.localeCompare(b.name)).map(s => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Allocation % */}
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-1">Allocation % *</label>
              <input
                type="number" min={0} max={100}
                value={percent}
                onChange={e => setPercent(Number(e.target.value))}
                className="w-full px-3 py-2 border border-gray-300 dark:border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-1">Start date</label>
              <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-1">End date</label>
              <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
            </div>
          </div>

          {/* Roles */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-2">Roles *</label>
            <div className="flex flex-wrap gap-2">
              {roles?.map(r => (
                <button
                  key={r.id} type="button"
                  onClick={() => setSelectedRoles(toggle(selectedRoles, r.id))}
                  className={`text-xs px-3 py-1.5 rounded-full border transition-colors ${
                    selectedRoles.has(r.id)
                      ? 'bg-blue-600 text-white border-blue-600'
                      : 'bg-white dark:bg-white/5 text-gray-600 dark:text-white/70 border-gray-300 dark:border-white/15 hover:border-blue-400'
                  }`}
                >
                  {r.name}
                </button>
              ))}
            </div>
          </div>

          {/* Technologies */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-2">Technologies</label>
            <div className="flex flex-wrap gap-2">
              {techs?.map(t => (
                <button
                  key={t.id} type="button"
                  onClick={() => setSelectedTechs(toggle(selectedTechs, t.id))}
                  className={`text-xs px-3 py-1.5 rounded-full border transition-colors ${
                    selectedTechs.has(t.id)
                      ? 'bg-gray-700 text-white border-gray-700'
                      : 'bg-white dark:bg-white/5 text-gray-600 dark:text-white/70 border-gray-300 dark:border-white/15 hover:border-gray-500 dark:hover:border-white/30'
                  }`}
                >
                  {t.name}
                </button>
              ))}
            </div>
          </div>

          {/* Comments */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-1">Public comment</label>
            <textarea
              value={publicComment}
              onChange={e => setPublicComment(e.target.value)}
              rows={2}
              placeholder="Visible to all users…"
              className="w-full px-3 py-2 border border-gray-300 dark:border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 resize-none"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-1">
              Admin note <span className="text-amber-600 dark:text-amber-400 font-normal">(admin only)</span>
            </label>
            <textarea
              value={adminNote}
              onChange={e => setAdminNote(e.target.value)}
              rows={2}
              placeholder="Internal notes, not visible to viewers…"
              className="w-full px-3 py-2 border border-amber-200 dark:border-amber-400/30 bg-amber-50 dark:bg-amber-400/10 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-amber-400 resize-none"
            />
          </div>

          {error && <p className="text-sm text-red-600 bg-red-50 dark:bg-red-400/10 border border-red-200 dark:border-red-400/30 rounded-lg px-3 py-2">{error}</p>}

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm text-gray-700 dark:text-white/80 border border-gray-300 dark:border-white/15 rounded-lg hover:bg-gray-50 dark:hover:bg-white/5 transition-colors">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="px-4 py-2 text-sm bg-brand-600 text-white rounded-lg hover:bg-brand-700 disabled:opacity-50 transition-colors">
              {saving ? 'Saving…' : initial ? 'Save changes' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
