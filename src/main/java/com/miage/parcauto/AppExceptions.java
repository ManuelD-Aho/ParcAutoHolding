package main.java.com.miage.parcauto;

public class AppExceptions {

    public static class ErreurBaseDeDonnees extends RuntimeException {
        public ErreurBaseDeDonnees() {
            super();
        }

        public ErreurBaseDeDonnees(String message) {
            super(message);
        }

        public ErreurBaseDeDonnees(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ErreurLogiqueMetier extends RuntimeException {
        public ErreurLogiqueMetier() {
            super();
        }

        public ErreurLogiqueMetier(String message) {
            super(message);
        }

        public ErreurLogiqueMetier(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ErreurAuthentification extends RuntimeException {
        public ErreurAuthentification() {
            super();
        }

        public ErreurAuthentification(String message) {
            super(message);
        }

        public ErreurAuthentification(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ErreurAutorisation extends RuntimeException {
        public ErreurAutorisation() {
            super();
        }

        public ErreurAutorisation(String message) {
            super(message);
        }

        public ErreurAutorisation(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ErreurValidation extends RuntimeException {
        public ErreurValidation() {
            super();
        }

        public ErreurValidation(String message) {
            super(message);
        }

        public ErreurValidation(String message, Throwable cause) {
            super(message, cause);
        }
    }
}