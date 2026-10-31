'use client';

import { useState } from 'react';
import useSWR from 'swr';
import Link from 'next/link';
import AppLayout from '@/components/layout/AppLayout';
import { api } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/context/ToastContext';
import { allocationBadgeClass, cn } from '@/lib/utils';
import { formatLeaverDate } from '@/lib/formatLeaverDate';
import type { PersonDto, Tag, Technology, DeveloperRole } from '@/types';
import { Search, Plus, Check, X, Pencil } from 'lucide-react';
import PersonForm from '@/components/persons/PersonForm';
import AllocationForm from '@/components/allocations/AllocationForm';
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  useReactTable,
  type SortingState,
} from '@tanstack/react-table';

const col = createColumnHelper<PersonDto>();

interface AllocationDraft {
  id: number;
  squadId: number;
  squadName: string;
  percent: number;
  roleIds: number[];
  technologyIds: number[];
  // preserve fields not shown in the inline editor
  publicComment?: string;
  adminNote?: string;
  startDate?: string;
  endDate?: string;
}

interface EditDraft {
  name: string;
  email: string;
  adminNote: string;
  tagIds: number[];
  leaverDate?: string;
  allocations: AllocationDraft[];
  removedAllocationIds: number[];
}

export default function PersonsPage() {
  const { data: persons, isLoading, error, mutate } = useSWR('persons', api.persons.list);
  const { isAdmin } = useAuth();
  const { toast } = useToast();
  const { data: allTags } = useSWR(isAdmin ? 'lookup/tags' : null, api.lookup.tags);
  const { data: allRoles } = useSWR(isAdmin ? 'lookup/roles' : null, api.lookup.roles);
  const { data: allTechnologies } = useSWR(isAdmin ? 'lookup/technologies' : null, api.lookup.technologies);
  const [search, setSearch] = useState('');
  const [sorting, setSorting] = useState<SortingState>([]);
  const [showForm, setShowForm] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editDraft, setEditDraft] = useState<EditDraft | null>(null);
  const [saving, setSaving] = useState(false);
  const [addAllocationForPersonId, setAddAllocationForPersonId] = useState<number | null>(null);

  function startEdit(person: PersonDto) {
    const tagIds = allTags
      ? allTags.filter((t: Tag) => person.tags.includes(t.name)).map((t: Tag) => t.id)
      : [];
    const allocations: AllocationDraft[] = person.allocations.map(a => ({
      id: a.id,
      squadId: a.squadId,
      squadName: a.squadName,
      percent: a.allocationPercent,
      roleIds: allRoles?.filter((r: DeveloperRole) => a.roles.includes(r.name)).map((r: DeveloperRole) => r.id) ?? [],
      technologyIds: allTechnologies?.filter((t: Technology) => a.technologies.includes(t.name)).map((t: Technology) => t.id) ?? [],
      publicComment: a.publicComment,
      adminNote: a.adminNote,
      startDate: a.startDate,
      endDate: a.endDate,
    }));
    setEditDraft({ name: person.name, email: person.email ?? '', adminNote: person.adminNote ?? '', tagIds, leaverDate: person.leaverDate, allocations, removedAllocationIds: [] });
    setEditingId(person.id);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditDraft(null);
  }

  async function saveEdit(personId: number) {
    if (!editDraft) return;
    setSaving(true);
    try {
      await api.persons.update(personId, {
        name: editDraft.name.trim(),
        email: editDraft.email.trim() || undefined,
        adminNote: editDraft.adminNote.trim() || undefined,
        tagIds: editDraft.tagIds,
        // the Leaver column is read-only here; round-trip the stored value so
        // PersonService.update (which assigns unconditionally) cannot erase it
        leaverDate: editDraft.leaverDate,
      });
      await Promise.all(editDraft.removedAllocationIds.map(id => api.allocations.delete(id)));
      const activeAllocations = editDraft.allocations.filter(
        a => !editDraft.removedAllocationIds.includes(a.id)
      );
      await Promise.all(activeAllocations.map(a =>
        api.allocations.update(a.id, {
          personId,
          squadId: a.squadId,
          roleIds: a.roleIds,
          technologyIds: a.technologyIds,
          allocationPercent: a.percent,
          publicComment: a.publicComment,
          adminNote: a.adminNote,
          startDate: a.startDate,
          endDate: a.endDate,
        })
      ));
      await mutate();
      toast('Changes saved');
      cancelEdit();
    } finally {
      setSaving(false);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent, personId: number) {
    if (e.key === 'Enter') { e.preventDefault(); saveEdit(personId); }
    if (e.key === 'Escape') { e.preventDefault(); cancelEdit(); }
  }

  async function handleDelete(person: PersonDto) {
    setConfirmDeleteId(null);
    setDeletingId(person.id);
    try {
      await api.persons.deactivate(person.id);
      toast(`${person.name} removed`);
      mutate();
    } finally {
      setDeletingId(null);
    }
  }

  function toggleAllocationTech(allocationId: number, techId: number) {
    setEditDraft(d => {
      if (!d) return d;
      return {
        ...d,
        allocations: d.allocations.map(a => {
          if (a.id !== allocationId) return a;
          return {
            ...a,
            technologyIds: a.technologyIds.includes(techId)
              ? a.technologyIds.filter(id => id !== techId)
              : [...a.technologyIds, techId],
          };
        }),
      };
    });
  }

  function removeAllocation(allocationId: number) {
    setEditDraft(d => d ? { ...d, removedAllocationIds: [...d.removedAllocationIds, allocationId] } : d);
  }

  function updateAllocationPercent(allocationId: number, percent: number) {
    setEditDraft(d => {
      if (!d) return d;
      return {
        ...d,
        allocations: d.allocations.map(a => a.id === allocationId ? { ...a, percent } : a),
      };
    });
  }

  const columns = [
    col.accessor('name', {
      header: 'Name',
      cell: info => {
        const person = info.row.original;
        if (editingId === person.id && editDraft) {
          return (
            <div className="space-y-1.5 py-0.5">
              <input
                autoFocus
                type="text"
                value={editDraft.name}
                onChange={e => setEditDraft(d => d ? { ...d, name: e.target.value } : d)}
                onKeyDown={e => handleKeyDown(e, person.id)}
                className="w-full border border-gray-300 dark:border-white/15 rounded-md px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                placeholder="Full name"
              />
              <input
                type="email"
                value={editDraft.email}
                onChange={e => setEditDraft(d => d ? { ...d, email: e.target.value } : d)}
                onKeyDown={e => handleKeyDown(e, person.id)}
                className="w-full border border-gray-300 dark:border-white/15 rounded-md px-2 py-1 text-sm text-gray-500 dark:text-white/50 focus:outline-none focus:ring-2 focus:ring-brand-500"
                placeholder="Email (optional)"
              />
            </div>
          );
        }
        return (
          <div>
            <Link href={`/persons/${person.id}`} className="font-medium text-gray-900 dark:text-white hover:text-brand-700 transition-colors">
              {info.getValue()}
            </Link>
            {person.email && (
              <div className="text-xs text-gray-400 dark:text-white/40 mt-0.5">{person.email}</div>
            )}
          </div>
        );
      },
    }),

    col.accessor(row => row.allocations.map(a => a.squadName).sort().join(', '), {
      id: 'squads',
      header: 'Squads',
      cell: ({ row }) => {
        const person = row.original;
        if (editingId === person.id && editDraft) {
          const activeAllocations = editDraft.allocations.filter(
            a => !editDraft.removedAllocationIds.includes(a.id)
          );
          return (
            <div className="space-y-1.5 py-0.5">
              {activeAllocations.map(a => (
                <div key={a.id} className="flex items-center gap-1.5">
                  <span className="text-xs font-medium text-gray-700 dark:text-white/80 flex-1 truncate">{a.squadName}</span>
                  <input
                    type="number"
                    min={0}
                    max={100}
                    value={a.percent}
                    onChange={e => updateAllocationPercent(a.id, Number(e.target.value))}
                    onKeyDown={e => handleKeyDown(e, person.id)}
                    className="w-12 border border-gray-300 dark:border-white/15 rounded px-1 py-0.5 text-xs text-center focus:outline-none focus:ring-1 focus:ring-brand-500"
                  />
                  <span className="text-xs text-gray-400 dark:text-white/40">%</span>
                  <button
                    onClick={() => removeAllocation(a.id)}
                    className="text-gray-300 dark:text-white/30 hover:text-red-500 p-0.5 transition-colors"
                    title="Remove from squad"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </div>
              ))}
              {activeAllocations.length === 0 && (
                <span className="text-xs text-gray-300 dark:text-white/30 italic">No squads</span>
              )}
              <button
                type="button"
                onClick={() => {
                  cancelEdit();
                  setAddAllocationForPersonId(person.id);
                }}
                className="text-xs text-brand-600 dark:text-brand-300 hover:text-brand-700 dark:hover:text-brand-200 hover:underline mt-0.5"
              >
                + Add squad
              </button>
            </div>
          );
        }
        return (
          <div className="flex flex-wrap gap-1">
            {person.allocations.map(a => (
              <Link key={a.id} href={`/squads/${a.squadId}`} className="text-xs bg-gray-100 dark:bg-white/10 text-gray-700 dark:text-white/70 hover:bg-brand-50 hover:text-brand-700 px-2 py-0.5 rounded-full transition-colors">
                {a.squadName} <span className="text-gray-400 dark:text-white/40">{a.allocationPercent}%</span>
              </Link>
            ))}
          </div>
        );
      },
    }),

    col.accessor(row => [...new Set(row.allocations.flatMap(a => a.roles))].sort().join(', '), {
      id: 'roles',
      header: 'Roles',
      cell: ({ row }) => {
        const person = row.original;
        const roles = [...new Set(person.allocations.flatMap(a => a.roles))].sort();
        return (
          <div className="flex flex-wrap gap-1">
            {roles.map(r => (
              <span key={r} className="text-xs bg-blue-50 dark:bg-blue-400/15 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded-full">{r}</span>
            ))}
          </div>
        );
      },
    }),

    col.accessor(row => [...new Set(row.allocations.flatMap(a => a.technologies))].sort().join(', '), {
      id: 'technologies',
      header: 'Technologies',
      cell: ({ row }) => {
        const person = row.original;
        if (editingId === person.id && editDraft && allTechnologies) {
          const activeAllocations = editDraft.allocations.filter(
            a => !editDraft.removedAllocationIds.includes(a.id)
          );
          if (activeAllocations.length === 0) {
            return <span className="text-xs text-gray-300 dark:text-white/30 italic">—</span>;
          }
          return (
            <div className="space-y-2 py-0.5">
              {activeAllocations.map(a => (
                <div key={a.id}>
                  {activeAllocations.length > 1 && (
                    <div className="text-xs text-gray-400 dark:text-white/40 mb-1">{a.squadName}</div>
                  )}
                  <div className="flex flex-wrap gap-1">
                    {allTechnologies.map((tech: Technology) => (
                      <button
                        key={tech.id}
                        type="button"
                        onClick={() => toggleAllocationTech(a.id, tech.id)}
                        className={`text-xs px-1.5 py-0.5 rounded-full border transition-colors ${
                          a.technologyIds.includes(tech.id)
                            ? 'bg-gray-700 border-gray-700 text-white'
                            : 'bg-white dark:bg-white/5 border-gray-200 dark:border-white/10 text-gray-400 dark:text-white/40 hover:border-gray-400 dark:hover:border-white/30'
                        }`}
                      >
                        {tech.name}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          );
        }
        const techs = [...new Set(person.allocations.flatMap(a => a.technologies))].sort();
        return (
          <div className="flex flex-wrap gap-1">
            {techs.map(t => (
              <span key={t} className="text-xs bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-white/70 px-2 py-0.5 rounded-full">{t}</span>
            ))}
          </div>
        );
      },
    }),

    col.accessor('totalAllocation', {
      header: 'Total',
      cell: info => {
        const person = info.row.original;
        // In edit mode, compute total live from draft
        if (editingId === person.id && editDraft) {
          const draftTotal = editDraft.allocations
            .filter(a => !editDraft.removedAllocationIds.includes(a.id))
            .reduce((sum, a) => sum + a.percent, 0);
          return (
            <span className={cn('text-xs font-semibold px-2.5 py-1 rounded-full', allocationBadgeClass(draftTotal))}>
              {draftTotal}%
            </span>
          );
        }
        return (
          <span className={cn('text-xs font-semibold px-2.5 py-1 rounded-full', allocationBadgeClass(info.getValue()))}>
            {info.getValue()}%
          </span>
        );
      },
    }),

    col.accessor('leaverDate', {
      id: 'leaverDate',
      header: 'Leaver',
      sortUndefined: 'last',
      cell: ({ row }) => {
        const person = row.original;
        // Editable in place, like Note. Only admins can reach this branch: the
        // pencil that sets editingId lives in the admin-only actions column, so
        // the column stays read-only for VIEWERs while remaining visible to them.
        if (editingId === person.id && editDraft) {
          return (
            <input
              type="date"
              value={editDraft.leaverDate ?? ''}
              // Clearing the input must actually clear the stored date, so an
              // empty string becomes undefined rather than '' - the payload omits
              // the key and the server nulls the column.
              onChange={e => setEditDraft(d => d ? { ...d, leaverDate: e.target.value || undefined } : d)}
              onKeyDown={e => handleKeyDown(e, person.id)}
              className="w-full min-w-[118px] border border-gray-300 dark:border-white/15 dark:bg-white/5 dark:text-white rounded-md px-1.5 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
              title="Leave empty if they are still here. Future dates are fine."
            />
          );
        }
        const label = formatLeaverDate(person.leaverDate);
        return label
          ? <span className="text-xs whitespace-nowrap px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 border border-gray-200 dark:bg-white/10 dark:text-white/60 dark:border-white/15">{label}</span>
          : <span className="text-xs text-gray-300 dark:text-white/30">—</span>;
      },
    }),

    ...(isAdmin ? [
      col.accessor(row => row.tags.slice().sort().join(', '), {
        id: 'tags',
        header: 'Tags',
        cell: info => {
          const person = info.row.original;
          if (editingId === person.id && editDraft && allTags) {
            return (
              <div className="flex flex-wrap gap-1 py-0.5">
                {allTags.map((tag: Tag) => (
                  <button
                    key={tag.id}
                    type="button"
                    onClick={() => setEditDraft(d => {
                      if (!d) return d;
                      return {
                        ...d,
                        tagIds: d.tagIds.includes(tag.id)
                          ? d.tagIds.filter(id => id !== tag.id)
                          : [...d.tagIds, tag.id],
                      };
                    })}
                    className={`text-xs px-2 py-0.5 rounded-full border transition-colors ${
                      editDraft.tagIds.includes(tag.id)
                        ? 'bg-amber-100 dark:bg-amber-400/20 border-amber-400 dark:border-amber-400/50 text-amber-800 dark:text-amber-200'
                        : 'bg-white dark:bg-white/5 border-gray-300 dark:border-white/15 text-gray-500 dark:text-white/50 hover:border-amber-300'
                    }`}
                  >
                    {tag.name}
                  </button>
                ))}
              </div>
            );
          }
          return (
            <div className="flex flex-wrap gap-1">
              {person.tags.map(t => (
                <span key={t} className="text-xs bg-amber-50 dark:bg-amber-400/15 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-400/30 px-2 py-0.5 rounded-full">{t}</span>
              ))}
            </div>
          );
        },
      }),

      col.display({
        id: 'adminNote',
        header: 'Note',
        enableSorting: false,
        cell: ({ row }) => {
          const person = row.original;
          if (editingId === person.id && editDraft) {
            return (
              <input
                type="text"
                value={editDraft.adminNote}
                onChange={e => setEditDraft(d => d ? { ...d, adminNote: e.target.value } : d)}
                onKeyDown={e => handleKeyDown(e, person.id)}
                className="w-full min-w-[120px] border border-amber-200 dark:border-amber-400/30 bg-amber-50 dark:bg-amber-400/10 rounded-md px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400"
                placeholder="Admin note…"
              />
            );
          }
          return person.adminNote
            ? <span className="text-xs text-amber-700 dark:text-amber-300 truncate max-w-[150px] block" title={person.adminNote}>{person.adminNote}</span>
            : <span className="text-xs text-gray-300 dark:text-white/30">—</span>;
        },
      }),

      col.display({
        id: 'actions',
        header: '',
        enableSorting: false,
        cell: ({ row }) => {
          const person = row.original;
          if (editingId === person.id) {
            return (
              <div className="flex items-center justify-end gap-1">
                <button
                  onClick={() => saveEdit(person.id)}
                  disabled={saving || !editDraft?.name.trim()}
                  className="p-1 text-green-600 dark:text-green-400 hover:text-green-700 dark:hover:text-green-300 disabled:opacity-40 transition-colors"
                  title="Save (Enter)"
                >
                  <Check className="w-4 h-4" />
                </button>
                <button
                  onClick={cancelEdit}
                  disabled={saving}
                  className="p-1 text-gray-400 dark:text-white/40 hover:text-gray-600 dark:hover:text-white/80 transition-colors"
                  title="Cancel (Esc)"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            );
          }
          return (
            <div className="flex items-center justify-end gap-2">
              <button
                onClick={() => startEdit(person)}
                className="p-1 text-gray-400 dark:text-white/40 hover:text-brand-600 transition-colors"
                title="Edit"
              >
                <Pencil className="w-3.5 h-3.5" />
              </button>
              {confirmDeleteId === person.id ? (
                <span className="flex items-center gap-1 text-xs">
                  <button
                    onClick={() => handleDelete(person)}
                    disabled={deletingId === person.id}
                    className="text-red-600 dark:text-red-400 font-medium hover:underline disabled:opacity-50"
                  >
                    {deletingId === person.id ? '…' : 'Confirm'}
                  </button>
                  <span className="text-gray-300 dark:text-white/30">·</span>
                  <button
                    onClick={() => setConfirmDeleteId(null)}
                    className="text-gray-400 dark:text-white/40 hover:text-gray-600 dark:hover:text-white/80"
                  >
                    Cancel
                  </button>
                </span>
              ) : (
                <button
                  onClick={() => setConfirmDeleteId(person.id)}
                  disabled={deletingId === person.id}
                  className="text-xs text-gray-400 dark:text-white/40 hover:text-red-600 dark:hover:text-red-400 transition-colors disabled:opacity-50"
                >
                  {deletingId === person.id ? '…' : 'Remove'}
                </button>
              )}
            </div>
          );
        },
      }),
    ] : []),
  ];

  const filtered = (() => {
    if (!persons) return [];
    const q = search.toLowerCase();
    if (!q) return persons;
    return persons.filter(p =>
      p.name.toLowerCase().includes(q) ||
      p.allocations.some(a =>
        a.squadName.toLowerCase().includes(q) ||
        a.roles.some(r => r.toLowerCase().includes(q)) ||
        a.technologies.some(t => t.toLowerCase().includes(q))
      ) ||
      p.tags.some(t => t.toLowerCase().includes(q))
    );
  })();

  const table = useReactTable({
    data: filtered,
    columns,
    state: { sorting },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
  });

  return (
    <AppLayout>
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">People</h1>
            <p className="text-sm text-gray-500 dark:text-white/50 mt-1">
              {filtered.length} of {persons?.length ?? '—'} people
            </p>
          </div>
          {isAdmin && (
            <button
              onClick={() => setShowForm(true)}
              className="flex items-center gap-1.5 text-sm bg-brand-600 text-white px-4 py-2 rounded-lg hover:bg-brand-700 transition-colors"
            >
              <Plus className="w-4 h-4" /> New person
            </button>
          )}
        </div>

        <div className="relative mb-5 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-white/40" />
          <input
            type="text"
            placeholder="Search by name, squad, tech, role…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-gray-300 dark:border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
          />
        </div>

        {isLoading && (
          <div className="flex justify-center py-20">
            <div className="w-6 h-6 border-2 border-brand-600 border-t-transparent rounded-full animate-spin" />
          </div>
        )}
        {error && <div className="text-red-600 bg-red-50 dark:bg-red-400/10 border border-red-200 dark:border-red-400/30 rounded-lg p-4">Failed to load people.</div>}

        {!isLoading && !error && (
          <div className="bg-white dark:bg-brand-800 border border-gray-100 dark:border-white/10 rounded-xl overflow-x-auto shadow-[0_2px_12px_rgba(43,10,91,0.07)] dark:shadow-[0_2px_12px_rgba(0,0,0,0.35)]">
            <table className="w-full text-sm">
              <thead>
                {table.getHeaderGroups().map(hg => (
                  <tr key={hg.id} className="border-b border-gray-200 dark:border-white/10 bg-gray-50 dark:bg-white/5">
                    {hg.headers.map(header => {
                      const canSort = header.column.getCanSort();
                      const sorted = header.column.getIsSorted();
                      return (
                        <th
                          key={header.id}
                          className={cn(
                            'text-left px-5 py-3 font-medium text-gray-600 dark:text-white/70 select-none',
                            canSort && 'cursor-pointer hover:text-gray-900 dark:hover:text-white',
                            // Pinned alongside its cells - see the td below.
                            header.column.id === 'actions' && 'sticky right-0 z-20 bg-gray-50 dark:bg-brand-800 shadow-[-6px_0_6px_-6px_rgba(43,10,91,0.15)] dark:shadow-[-6px_0_6px_-6px_rgba(0,0,0,0.5)]'
                          )}
                          onClick={canSort ? header.column.getToggleSortingHandler() : undefined}
                        >
                          <span className="flex items-center gap-1">
                            {flexRender(header.column.columnDef.header, header.getContext())}
                            {canSort && (
                              <span className="text-gray-400 dark:text-white/40">
                                {sorted === 'asc' ? ' ↑' : sorted === 'desc' ? ' ↓' : ' ↕'}
                              </span>
                            )}
                          </span>
                        </th>
                      );
                    })}
                  </tr>
                ))}
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-white/10">
                {table.getRowModel().rows.map(row => {
                  const isEditing = editingId === row.original.id;
                  return (
                    <tr
                      key={row.id}
                      className={cn('transition-colors', isEditing ? 'bg-blue-50/40 dark:bg-blue-400/10' : 'hover:bg-gray-50 dark:hover:bg-white/5')}
                    >
                      {row.getVisibleCells().map(cell => {
                        // The row controls stay pinned to the right edge. Editing a row
                        // widens it (the date picker has a large intrinsic minimum), and
                        // an unpinned Save button ends up outside the visible area.
                        const isActions = cell.column.id === 'actions';
                        return (
                          <td
                            key={cell.id}
                            className={cn(
                              'px-5 py-3.5 align-top',
                              // Needs an opaque background or the scrolled content shows through.
                              // The shadow marks it as pinned rather than as content
                              // that is mysteriously sitting on top of the row.
                              isActions && 'sticky right-0 z-10 shadow-[-6px_0_6px_-6px_rgba(43,10,91,0.15)] dark:shadow-[-6px_0_6px_-6px_rgba(0,0,0,0.5)]',
                              isActions && (isEditing ? 'bg-blue-50 dark:bg-brand-800' : 'bg-white dark:bg-brand-800')
                            )}
                          >
                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
                {table.getRowModel().rows.length === 0 && (
                  <tr>
                    <td colSpan={columns.length} className="px-5 py-10 text-center text-gray-400 dark:text-white/40 text-sm">
                      No people match your search.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showForm && (
        <PersonForm
          onClose={() => setShowForm(false)}
          onSaved={() => {
            toast('Person added');
            mutate();
            setShowForm(false);
          }}
        />
      )}

      {addAllocationForPersonId !== null && (
        <AllocationForm
          defaultPersonId={addAllocationForPersonId}
          onClose={() => setAddAllocationForPersonId(null)}
          onSaved={() => {
            toast('Squad allocation added');
            mutate();
            setAddAllocationForPersonId(null);
          }}
        />
      )}
    </AppLayout>
  );
}
