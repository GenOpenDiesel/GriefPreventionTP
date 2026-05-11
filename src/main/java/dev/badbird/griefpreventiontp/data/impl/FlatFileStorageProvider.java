package dev.badbird.griefpreventiontp.data.impl;

import com.google.gson.reflect.TypeToken;
import dev.badbird.griefpreventiontp.GriefPreventionTP;
import dev.badbird.griefpreventiontp.data.StorageProvider;
import dev.badbird.griefpreventiontp.api.ClaimInfo;
import dev.badbird.griefpreventiontp.api.PlayerSortPreference;
import net.badbird5907.blib.util.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class FlatFileStorageProvider implements StorageProvider {
    private File folder;

    @Override
    public void init(GriefPreventionTP plugin) {
        folder = new File(plugin.getDataFolder(), "data");
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    @Override
    public void disable(GriefPreventionTP plugin) {

    }
    @Override
    public void saveClaims(Collection<ClaimInfo> claims) {
        File claimsFile = new File(GriefPreventionTP.getInstance().getDataFolder(), "claims.json");
        if (!claimsFile.exists()) {
            try {
                claimsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            ArrayList<ClaimInfo> claimInfos = new ArrayList<>(claims);
            PrintStream ps = new PrintStream(claimsFile);
            ps.print(GriefPreventionTP.getInstance().getGson().toJson(claimInfos));
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Collection<ClaimInfo> getClaims() {
        File claimsFile = new File(GriefPreventionTP.getInstance().getDataFolder(), "claims.json");
        if (!claimsFile.exists()) {
            return new ArrayList<>();
        }
        try {
            String contents = new String(Files.readAllBytes(claimsFile.toPath()));
            ArrayList<ClaimInfo> claimInfos = GriefPreventionTP.getInstance().getGson().fromJson(contents, new TypeToken<ArrayList<ClaimInfo>>() {
            }.getType());
            return claimInfos;
        } catch (IOException e) {
            e.printStackTrace();
        }
        GriefPreventionTP.getInstance().setDisabled(true);
        GriefPreventionTP.getInstance().setDisabledReason("Failed to load claims");
        return new ArrayList<>();
    }

    @Override
    public void savePlayerSortPreferences(Map<UUID, PlayerSortPreference> preferences) {
        File preferencesFile = new File(GriefPreventionTP.getInstance().getDataFolder(), "player-sort-preferences.json");
        if (!preferencesFile.exists()) {
            try {
                preferencesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (PrintStream ps = new PrintStream(preferencesFile)) {
            ps.print(GriefPreventionTP.getInstance().getGson().toJson(preferences));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<UUID, PlayerSortPreference> getPlayerSortPreferences() {
        File preferencesFile = new File(GriefPreventionTP.getInstance().getDataFolder(), "player-sort-preferences.json");
        if (!preferencesFile.exists()) {
            return new java.util.HashMap<>();
        }
        try {
            String contents = new String(Files.readAllBytes(preferencesFile.toPath()));
            if (contents.isBlank()) {
                return new java.util.HashMap<>();
            }
            Map<UUID, PlayerSortPreference> preferences = GriefPreventionTP.getInstance().getGson().fromJson(contents, new TypeToken<Map<UUID, PlayerSortPreference>>() {
            }.getType());
            return preferences == null ? new java.util.HashMap<>() : preferences;
        } catch (IOException e) {
            e.printStackTrace();
        }
        Logger.info("Failed to load player sort preferences, using defaults.");
        return new java.util.HashMap<>();
    }
}
