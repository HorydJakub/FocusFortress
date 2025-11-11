package com.focusfortress.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class ManageInterestsDTO {

    @NotNull
    private Set<String> add;

    @NotNull
    private Set<String> remove;

    public boolean isValid() {
        return add != null && remove != null;
    }
}