package co.com.bancolombia.model.franchise.exception;

public class InvalidFranchiseNameException extends RuntimeException {

    public InvalidFranchiseNameException() {
        super("Franchise name must not be blank");
    }
}
