/**
 * Builds the leaver badge label. Returns null when the person has no leaver
 * date, which includes the key being absent entirely - PersonDto is serialised
 * with @JsonInclude(NON_NULL), so null dates are omitted from the JSON.
 */
export function formatLeaverDate(iso?: string): string | null {
  if (!iso) return null;
  return `Left ${iso}`;
}
