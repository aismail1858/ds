package common;

public enum SagaState {
    STARTED,
    RESERVING_PRODUCTS,
    PRODUCTS_RESERVED,
    CONFIRMING_RESERVATIONS,
    COMPLETED,
    COMPENSATING,
    COMPENSATION_COMPLETED,
    FAILED;
    
    public boolean canTransitionTo(SagaState newState) {
        switch (this) {
            case STARTED:
                return newState == RESERVING_PRODUCTS || newState == FAILED;
            case RESERVING_PRODUCTS:
                return newState == PRODUCTS_RESERVED || newState == COMPENSATING || newState == FAILED;
            case PRODUCTS_RESERVED:
                return newState == CONFIRMING_RESERVATIONS || newState == COMPENSATING;
            case CONFIRMING_RESERVATIONS:
                return newState == COMPLETED || newState == COMPENSATING;
            case COMPENSATING:
                return newState == COMPENSATION_COMPLETED || newState == FAILED;
            case COMPENSATION_COMPLETED:
            case COMPLETED:
            case FAILED:
                return false; // Terminal states
            default:
                return false;
        }
    }
}
