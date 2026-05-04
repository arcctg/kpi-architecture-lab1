package com.flashcard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCardRequest(
        @NotBlank @Size(min = 1, max = 200)
        String term,

        @NotBlank @Size(min = 1, max = 500)
        String definition
) {}
