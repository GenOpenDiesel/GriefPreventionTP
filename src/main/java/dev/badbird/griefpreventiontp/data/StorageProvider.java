package dev.badbird.griefpreventiontp.data;

import dev.badbird.griefpreventiontp.GriefPreventionTP;
import dev.badbird.griefpreventiontp.api.ClaimInfo;
import dev.badbird.griefpreventiontp.api.PlayerSortPreference;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface StorageProvider {
    void init(GriefPreventionTP plugin);

    void disable(GriefPreventionTP plugin);

    void saveClaims(Collection<ClaimInfo> claims);

    Collection<ClaimInfo> getClaims();

    void savePlayerSortPreferences(Map<UUID, PlayerSortPreference> preferences);

    Map<UUID, PlayerSortPreference> getPlayerSortPreferences();
}
