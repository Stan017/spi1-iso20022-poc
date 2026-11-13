package com.stan.poc.spi2iso.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {

    private final List<String> errores = new ArrayList<>();

    public void addError(String codigo, String mensaje) {
        errores.add(codigo + ": " + mensaje);
    }

    public boolean isValid() {
        return errores.isEmpty();
    }

    public List<String> getErrores() {
        return Collections.unmodifiableList(errores);
    }

    @Override
    public String toString() {
        return isValid() ? "VALIDO" : String.join(" | ", errores);
    }
}
