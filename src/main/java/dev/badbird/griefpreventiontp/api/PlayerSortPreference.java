package dev.badbird.griefpreventiontp.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlayerSortPreference {
    private String sortType = "DEFAULT";
    private boolean sortAscending = true;

    public PlayerSortPreference(String sortType, boolean sortAscending) {
        this.sortType = sortType;
        this.sortAscending = sortAscending;
    }
}
