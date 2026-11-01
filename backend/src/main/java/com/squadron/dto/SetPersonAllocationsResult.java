package com.squadron.dto;

import java.util.List;

public record SetPersonAllocationsResult(
        Long personId,
        String personName,
        boolean dryRun,
        int totalBefore,
        int totalAfter,
        List<Entry> before,
        List<Entry> after,
        List<Change> changes,
        List<String> warnings
) {
    /** allocationId is null for an allocation a dry run would create. */
    public record Entry(Long allocationId, Long squadId, String squadName, int allocationPercent, List<String> roles) {}

    /** action: created | updated | removed | unchanged. Percents are null where they do not apply. */
    public record Change(String action, Long allocationId, Long squadId, String squadName,
                         Integer fromPercent, Integer toPercent) {}
}
