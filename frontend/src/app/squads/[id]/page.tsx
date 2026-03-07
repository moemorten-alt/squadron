'use client';

import { use, useState } from 'react';
import useSWR from 'swr';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import AppLayout from '@/components/layout/AppLayout';
import { api } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/context/ToastContext';
import { allocationBadgeClass, cn } from '@/lib/utils';
import type { AllocationDto } from '@/types';
import { ArrowLeft, Plus, Pencil, Trash2 } from 'lucide-react';
import AllocationForm from '@/components/allocations/AllocationForm';
import SquadForm from '@/components/squads/SquadForm';

export default function SquadDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const { data: squad, error, isLoading, mutate } = useSWR(`squads/${id}`, () => api.squads.get(Number(id)));
  const { isAdmin } = useAuth();
  const { toast } = useToast();
  const [showForm, setShowForm] = useState(false);
  const [editAllocation, setEditAllocation] = useState<AllocationDto | null>(null);
  const [showSquadForm, setShowSquadForm] = useState(false);
  const [deletingAllocationId, setDeletingAllocationId] = useState<number | null>(null);
  const [confirmDeleteAllocationId, setConfirmDeleteAllocationId] = useState<number | null>(null);
  const [confirmDeleteSquad, setConfirmDeleteSquad] = useState(false);

  async function handleDeleteSquad() {
    if (!squad) return;
    setConfirmDeleteSquad(false);
    await api.squads.delete(squad.id);
    toast(`"${squad.name}" deleted`);
    router.push('/squads');
  }

  async function handleDeleteAllocation(a: AllocationDto) {
    setConfirmDeleteAllocationId(null);
    setDeletingAllocationId(a.id);
    try {
      await api.allocations.delete(a.id);
      toast(`${a.personName} removed from squad`);
      mutate();
    } finally {
      setDeletingAllocationId(null);
    }
  }

  return (
    <AppLayout>
      <div className="max-w-5xl mx-auto">
        <Link href="/squads" className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-900 mb-6">
          <ArrowLeft className="w-4 h-4" /> Back to Squads
        </Link>

        {isLoading && (
          <div className="flex justify-center py-20">
            <div className="w-6 h-6 border-2 border-brand-600 border-t-transparent rounded-full animate-spin" />
          </div>
        )}
        {error && <div className="text-red-600 bg-red-50 border border-red-200 rounded-lg p-4">Failed to load squad.</div>}

        {squad && (
          <>
            <div className="flex items-start justify-between mb-6">
              <div>
                <h1 className="text-2xl font-semibold text-gray-900">{squad.name}</h1>
                {squad.description && <p className="text-sm text-gray-500 mt-1">{squad.description}</p>}
                <div className="flex items-center gap-3 mt-2">
                  <span className="text-sm text-gray-600">{squad.totalHeadcount} members</span>
                  {squad.totalHeadcount > 0 && (
                    <span className={cn('text-xs font-medium px-2 py-0.5 rounded-full', allocationBadgeClass(squad.totalAllocationPercent / Math.max(squad.totalHeadcount, 1)))}>
                      {Math.round(squad.totalAllocationPercent / Math.max(squad.totalHeadcount, 1))}% avg allocation
                    </span>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                {isAdmin && (
                  <>
                    <button
                      onClick={() => { setEditAllocation(null); setShowForm(true); }}
                      className="flex items-center gap-2 bg-brand-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-brand-700 transition-colors"
                    >
                      <Plus className="w-4 h-4" /> Add allocation
                    </button>
                    <button
                      onClick={() => setShowSquadForm(true)}
                      className="p-2 text-gray-400 hover:text-brand-600 transition-colors rounded-lg hover:bg-gray-100"
                      title="Edit squad"
                    >
                      <Pencil className="w-4 h-4" />
                    </button>
                    {confirmDeleteSquad ? (
                      <span className="flex items-center gap-1.5 text-xs bg-white border border-red-200 rounded-lg px-2.5 py-1.5">
                        <span className="text-gray-600">Delete squad?</span>
                        <button
                          onClick={handleDeleteSquad}
                          className="text-red-600 font-semibold hover:underline"
                        >
                          Confirm
                        </button>
                        <span className="text-gray-300">·</span>
                        <button
                          onClick={() => setConfirmDeleteSquad(false)}
                          className="text-gray-400 hover:text-gray-600"
                        >
                          Cancel
                        </button>
                      </span>
                    ) : (
                      <button
                        onClick={() => setConfirmDeleteSquad(true)}
                        className="p-2 text-gray-400 hover:text-red-600 transition-colors rounded-lg hover:bg-red-50"
                        title="Delete squad"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </>
                )}
              </div>
            </div>

            <div className="bg-white border border-gray-100 rounded-xl overflow-hidden shadow-[0_2px_12px_rgba(43,10,91,0.07)]">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="text-left px-5 py-3 font-medium text-gray-600">Person</th>
                    <th className="text-left px-5 py-3 font-medium text-gray-600">Roles</th>
                    <th className="text-left px-5 py-3 font-medium text-gray-600">Technologies</th>
                    <th className="text-left px-5 py-3 font-medium text-gray-600">Allocation</th>
                    <th className="text-left px-5 py-3 font-medium text-gray-600">Notes</th>
                    {isAdmin && <th className="px-5 py-3" />}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {squad.allocations.map(a => (
                    <tr key={a.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-5 py-3.5">
                        <Link href={`/persons/${a.personId}`} className="font-medium text-gray-900 hover:text-brand-700 transition-colors">
                          {a.personName}
                        </Link>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex flex-wrap gap-1">
                          {a.roles.map(r => (
                            <span key={r} className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded-full">{r}</span>
                          ))}
                        </div>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex flex-wrap gap-1">
                          {a.technologies.map(t => (
                            <span key={t} className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">{t}</span>
                          ))}
                        </div>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={cn('text-xs font-semibold px-2.5 py-1 rounded-full', allocationBadgeClass(a.allocationPercent))}>
                          {a.allocationPercent}%
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-gray-500 max-w-xs">
                        <div className="space-y-0.5">
                          {a.publicComment && <p className="text-xs">{a.publicComment}</p>}
                          {isAdmin && a.adminNote && (
                            <p className="text-xs text-amber-700 bg-amber-50 px-2 py-0.5 rounded">{a.adminNote}</p>
                          )}
                          {a.endDate && <p className="text-xs text-red-600">Until {a.endDate}</p>}
                        </div>
                      </td>
                      {isAdmin && (
                        <td className="px-5 py-3.5 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => { setEditAllocation(a); setShowForm(true); }}
                              className="text-xs text-gray-500 hover:text-brand-700 transition-colors"
                            >
                              Edit
                            </button>
                            {confirmDeleteAllocationId === a.id ? (
                              <span className="flex items-center gap-1 text-xs">
                                <button
                                  onClick={() => handleDeleteAllocation(a)}
                                  disabled={deletingAllocationId === a.id}
                                  className="text-red-600 font-medium hover:underline disabled:opacity-50"
                                >
                                  {deletingAllocationId === a.id ? '…' : 'Confirm'}
                                </button>
                                <span className="text-gray-300">·</span>
                                <button
                                  onClick={() => setConfirmDeleteAllocationId(null)}
                                  className="text-gray-400 hover:text-gray-600"
                                >
                                  Cancel
                                </button>
                              </span>
                            ) : (
                              <button
                                onClick={() => setConfirmDeleteAllocationId(a.id)}
                                className="text-xs text-gray-400 hover:text-red-600 transition-colors"
                              >
                                Remove
                              </button>
                            )}
                          </div>
                        </td>
                      )}
                    </tr>
                  ))}
                  {squad.allocations.length === 0 && (
                    <tr>
                      <td colSpan={isAdmin ? 6 : 5} className="px-5 py-8 text-center text-gray-400 text-sm">
                        No allocations yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>

      {showForm && (
        <AllocationForm
          initial={editAllocation}
          defaultSquadId={squad?.id}
          onClose={() => { setShowForm(false); setEditAllocation(null); }}
          onSaved={() => {
            toast(editAllocation ? 'Allocation updated' : 'Allocation added');
            mutate();
            setShowForm(false);
            setEditAllocation(null);
          }}
        />
      )}

      {showSquadForm && squad && (
        <SquadForm
          initial={squad}
          onClose={() => setShowSquadForm(false)}
          onSaved={() => {
            toast('Squad updated');
            mutate();
            setShowSquadForm(false);
          }}
        />
      )}
    </AppLayout>
  );
}
