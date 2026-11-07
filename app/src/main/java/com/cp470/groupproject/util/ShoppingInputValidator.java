package com.cp470.groupproject.util;

/**
 * Validates the text typed in the shopping list form. plain java, so it is tested on the jvm.
 */
public final class ShoppingInputValidator {

    /** what is wrong with the input, if anything */
    public enum Error { NONE, NAME_REQUIRED, QUANTITY_INVALID }

    /** outcome of a validation: the error, or the cleaned values when there is none */
    public static final class Result {
        /** what is wrong with the input, or NONE */
        public final Error error;
        /** trimmed name */
        public final String name;
        /** amount, 1 when the field was empty (only meaningful when there is no error) */
        public final double quantity;

        Result(Error error, String name, double quantity) {
            this.error = error;
            this.name = name;
            this.quantity = quantity;
        }

        /** @return true when the input can be saved */
        public boolean isValid() {
            return error == Error.NONE;
        }
    }

    private ShoppingInputValidator() { }

    /**
     * checks the name and the quantity of a shopping item.
     * an empty quantity means 1, and both "1,5" and "1.5" are accepted.
     *
     * @param name text of the name field
     * @param quantityText text of the quantity field
     * @return the result, never null
     */
    public static Result validate(String name, String quantityText) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            return new Result(Error.NAME_REQUIRED, cleanName, 0);
        }
        String qty = quantityText == null ? "" : quantityText.trim();
        if (qty.isEmpty()) {
            return new Result(Error.NONE, cleanName, 1.0);
        }
        try {
            double value = Double.parseDouble(qty.replace(',', '.'));
            if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
                return new Result(Error.QUANTITY_INVALID, cleanName, 0);
            }
            return new Result(Error.NONE, cleanName, value);
        } catch (NumberFormatException e) {
            return new Result(Error.QUANTITY_INVALID, cleanName, 0);
        }
    }
}
