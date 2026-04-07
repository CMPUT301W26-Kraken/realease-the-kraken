package com.example.releasethekraken.model;

/**
 * A generic class that holds a result success w/ data or an error exception.
 * Added as a default for Android Studio when making certain views, will likely be made redundant
 * later and deleted accordingly.
 *
 * @param <T> The type of data held in a successful result.
 */
public class Result<T> {
    /**
     * Private constructor to limit subclass types to Success and Error.
     */
    private Result() {
    }

    /**
     * Returns a string representation of the Result object.
     * @return A string indicating Success with data or Error with exception.
     */
    @Override
    public String toString() {
        if (this instanceof Result.Success) {
            Result.Success success = (Result.Success) this;
            return "Success[data=" + success.getData().toString() + "]";
        } else if (this instanceof Result.Error) {
            Result.Error error = (Result.Error) this;
            return "Error[exception=" + error.getError().toString() + "]";
        }
        return "";
    }

    /**
     * A subclass of Result that represents a successful outcome containing data.
     * @param <T> The type of the data.
     */
    public final static class Success<T> extends Result {
        private T data;

        /**
         * Constructs a Success result with the provided data.
         * @param data The data to be held in the result.
         */
        public Success(T data) {
            this.data = data;
        }

        /**
         * Gets the data held in the success result.
         * @return The data.
         */
        public T getData() {
            return this.data;
        }
    }

    /**
     * A subclass of Result that represents a failed outcome containing an exception.
     */
    public final static class Error extends Result {
        private Exception error;

        /**
         * Constructs an Error result with the provided exception.
         * @param error The exception encountered.
         */
        public Error(Exception error) {
            this.error = error;
        }

        /**
         * Gets the exception held in the error result.
         * @return The exception.
         */
        public Exception getError() {
            return this.error;
        }
    }
}
