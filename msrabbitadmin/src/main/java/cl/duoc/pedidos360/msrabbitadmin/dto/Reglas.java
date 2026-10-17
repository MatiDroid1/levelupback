package cl.duoc.pedidos360.msrabbitadmin.dto;

public final class Reglas {
    private Reglas() {}

    public static final String NOMBRE = "^(?!amq\\.)[A-Za-z0-9._-]{1,255}$";
    public static final String MENSAJE_NOMBRE =
        "Solo letras, numeros, punto, guion y guion bajo; maximo 255 caracteres; no puede empezar con 'amq.'";
}
