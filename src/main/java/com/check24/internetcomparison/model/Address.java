package com.check24.internetcomparison.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record Address(
    @NotBlank(message = "Straße darf nicht leer sein")
    @Size(max = 100, message = "Straße darf maximal 100 Zeichen lang sein")
    String street,

    @NotBlank(message = "Hausnummer darf nicht leer sein")
    @Size(max = 10, message = "Hausnummer darf maximal 10 Zeichen lang sein")
    String houseNumber,

    @NotBlank(message = "Postleitzahl darf nicht leer sein")
    @Pattern(regexp = "^[0-9]{5}$", message = "Postleitzahl muss aus 5 Ziffern bestehen")
    String zip,

    @NotBlank(message = "Stadt darf nicht leer sein")
    @Size(max = 100, message = "Stadt darf maximal 100 Zeichen lang sein")
    String city
) {}
