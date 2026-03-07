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
import { ArrowLeft, Plus, Mail, Pencil, Trash2 } from 'lucide-react';
import AllocationForm from '@/components/allocations/AllocationForm';
import PersonForm from '@/components/persons/PersonForm';

export default function PersonDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const { data: person, error, isLoading, mutate } = useSWR(`persons/${id}`, () => api.persons.get(Number(id)));
  const { isAdmin } = useAuth();
  const { toast } = useToast();
  const [showForm, setShowForm] = useState(false);
  const [editAllocation, setEditAllocation] = useState<AllocationDto | null>(null);
  const [showPersonForm, setShowPersonForm] = useState(false);
  const [deletingAllocationId, setDeletingAllocationId] = useState<number | null>(null);
  const [confirmDeleteAllocationId, setConfirmDeleteAllocationId] = useState<number | null>(null);
  const [confirmDeletePerson, setConfirmDeletePerson] = useState(false);

  async function handleDeletePerson() {
    if (!person) return;
    setConfirmDeletePerson(false);
    await api.persons.deactivate(person.id);
    toast(`${person.name} removed`);
    router.push('/persons');
  }

  async function handleDeleteAllocation(a: AllocationDto) {
    setConfirmDeleteAllocationId(null);
    setDeletingAllocationId(a.id);
    try {
      await api.allocations.delete(a.id);
      toast(`Removed from ${a.squadName}`);
      mutate();
    } finally {
      setDeletingAllocationId(null);
    }
  }

  return (
    <AppLayout>
      <div className="max-w-4xl mx-auto">
        <Link href="/persons" className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-900 mb-6">
          <ArrowLeft className="w-4 h-4" /> Back to People
        </Link>

        {isLoading && (
          <div className="flex justify-center py-20">
            <div className="w-6 h-6 border-2 border-brand-600 border-t-transparent rounded-full animate-spin" />
          </div>
        )}
        {error && <div className="text-red-600 bg-red-50 border border-red-200 rounded-lg p-4">Failed to load person.</div>}

        {person && (
          <div className="space-y-6">
            {/* Header card */}
            <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-[0_2px_12px_rgba(43,10,91,0.07)]">
              <div className="flex items-start justify-between">
                <div>
                  <h1 className="text-2xl font-semibold text-gray-900">{person.name}</h1>
                  {person.email && (
                    <a href={`mailto:${person.email}`} className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-brand-700 mt-1 transition-colors">
                      <Mail className="w-3.5 h-3.5" />
                      {person.email}
                    </a>
                  )}

                  {person.tags.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 mt-3">
                      {person.tags.map(tag => (
                        <span key={tag} className="text-xs bg-amber-50 text-amber-700 border border-amber-200 px-2.5 py-0.5 rounded-full font-medium">
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}

                  {isAdmin && person.adminNote && (
                    <div className="mt-3 text-sm text-amber-800 bg-amber-50 border border-amber-200 rounded-lg px-4 py-2">
                      <span className="font-medium">Note: </span>{person.adminNote}
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className={cn('text-sm font-semibold px-3 py-1.5 rounded-full', allocationBadgeClass(person.totalAllocation))}>
                    {person.totalAllocation}% allocated
                  </span>
                  {isAdmin && (
                    <>
                      <button
                        onClick={() => setShowPersonForm(true)}
                        className="p-1.5 text-gray-400 hover:text-brand-600 transition-colors rounded-lg hover:bg-gray-100"
                        title="Edit person"
                      >
                        <Pencil className="w-4 h-4" />
                      </button>
                      {confirmDeletePerson ? (
                        <span className="flex items-center gap-1.5 text-xs bg-white border border-red-200 rounded-lg px-2.5 py-1.5">
                          <span className="text-gray-600">Remove person?</span>
                          <button
                            onClick={handleDeletePerson}
                            className="text-red-600 font-semibold hover:underline"
                          >
                            Confirm
                          </button>
                          <span className="text-gray-300">·</span>
                          <button
                            onClick={() => setConfirmDeletePerson(false)}
                            className="text-gray-400 hover:text-gray-600"
                          >
                            Cancel
                          </button>
                        </span>
                      ) : (
                        <button
                          onClick={() => setConfirmDeletePerson(true)}
                          className="p-1.5 text-gray-400 hover:text-red-600 transition-colors rounded-lg hover:bg-red-50"
                          title="Remove person"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </>
                  )}
                </div>
              </div>
            </div>

            {/* Allocations */}
            <div>
              <div className="flex items-center justify-between mb-3">
                <h2 className="font-semibold text-gray-900">Squad allocations</h2>
                {isAdmin && (
                  <button
                    onClick={() => { setEditAllocation(null); setShowForm(true); }}
                    className="flex items-center gap-1.5 text-sm bg-brand-600 text-white px-3 py-1.5 rounded-lg hover:bg-brand-700 transition-colors"
                  >
                    <Plus className="w-3.5 h-3.5" /> Add
                  </button>
                )}
              </div>

              <div className="bg-white border border-gray-100 rounded-xl overflow-hidden shadow-[0_2px_12px_rgba(43,10,91,0.07)]">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-200 bg-gray-50">
                      <th className="text-left px-5 py-3 font-medium text-gray-600">Squad</th>
                      <th className="text-left px-5 py-3 font-medium text-gray-600">Roles</th>
                      <th className="text-left px-5 py-3 font-medium text-gray-600">Technologies</th>
                      <th className="text-left px-5 py-3 font-medium text-gray-600">Allocation</th>
                      <th className="text-left px-5 py-3 font-medium text-gray-600">Notes</th>
                      {isAdmin && <th className="px-5 py-3" />}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {person.allocations.map(a => (
                      <tr key={a.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-5 py-3.5">
                          <Link href={`/squads/${a.squadId}`} className="font-medium text-gray-900 hover:text-brand-700 transition-colors">
                            {a.squadName}
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
                    {person.allocations.length === 0 && (
                      <tr>
                        <td colSpan={isAdmin ? 6 : 5} className="px-5 py-8 text-center text-gray-400 text-sm">
                          No allocations yet.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>

      {showForm && (
        <AllocationForm
          initial={editAllocation}
          defaultPersonId={person?.id}
          onClose={() => { setShowForm(false); setEditAllocation(null); }}
          onSaved={() => {
            toast(editAllocation ? 'Allocation updated' : 'Allocation added');
            mutate();
            setShowForm(false);
            setEditAllocation(null);
          }}
        />
      )}

      {showPersonForm && person && (
        <PersonForm
          initial={person}
          onClose={() => setShowPersonForm(false)}
          onSaved={() => {
            toast('Person updated');
            mutate();
            setShowPersonForm(false);
          }}
        />
      )}
    </AppLayout>
  );
}
