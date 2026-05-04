package com.flashcard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeckRequest(
        @NotBlank @Size(min = 1, max = 100)
        String title,

        @Size(max = 500)
        String description
) {}
