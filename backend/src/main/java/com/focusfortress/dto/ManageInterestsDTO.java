package com.focusfortress.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class ManageInterestsDTO {

    @NotNull(message = "Interests to add cannot be null")
    private Set<String> add;

    @NotNull(message = "Interests to remove cannot be null")
    private Set<String> remove;

    // This method checks that at least one interest is being added or removed,
    public boolean isValid() {
        if (add == null) add = Set.of();
        if (remove == null) remove = Set.of();

        if (add.isEmpty() && remove.isEmpty()) {
            return false;
        }

        for (String interest : add) {
            if (remove.contains(interest)) {
                return false;
            }
        }

        return true;
    }
}