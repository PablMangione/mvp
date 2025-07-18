package com.acainfo.mvp.exception.student;

/**
 * Excepción lanzada cuando se intenta inscribir a un estudiante
 * en un grupo que ya alcanzó su capacidad máxima.
 */
public class GroupFullException extends RuntimeException {

    public GroupFullException(String message) {
        super(message);
    }

    public GroupFullException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor con información del grupo.
     *
     * @param groupId ID del grupo lleno
     * @param currentCapacity capacidad actual
     * @param maxCapacity capacidad máxima
     */
    public GroupFullException(Long groupId, int currentCapacity, int maxCapacity) {
        super(String.format("El grupo %d está lleno (%d/%d estudiantes)",
                groupId, currentCapacity, maxCapacity));
    }
}