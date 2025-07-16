package common;

public enum OrderStatus {
    CREATED,
    RESERVING_PRODUCTS,
    ALL_RESERVED,
    CONFIRMING_PRODUCTS,
    COMPLETED,
    COMPENSATING,
    FAILED,
    CANCELLED
}
