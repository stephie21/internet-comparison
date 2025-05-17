package com.check24.internetcomparison.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validAddressShouldPassValidation() {
        Address address = new Address("Musterstraße", "123", "12345", "Berlin");
        var violations = validator.validate(address);
        assertTrue(violations.isEmpty());
    }

    @Test
    void invalidZipCodeShouldFailValidation() {
        Address address = new Address("Musterstraße", "123", "1234", "Berlin");
        var violations = validator.validate(address);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Postleitzahl muss aus 5 Ziffern bestehen", violations.iterator().next().getMessage());
    }

    @Test
    void emptyStreetShouldFailValidation() {
        Address address = new Address("", "123", "12345", "Berlin");
        var violations = validator.validate(address);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Straße darf nicht leer sein", violations.iterator().next().getMessage());
    }
} 