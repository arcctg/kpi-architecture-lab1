package com.flashcard.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 2, max = 50)
        String displayName,

        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                 message = "must contain at least one letter and one digit")
        String password
) {}
