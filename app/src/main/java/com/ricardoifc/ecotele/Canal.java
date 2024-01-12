package com.ricardoifc.ecotele;

public class Canal {
    private String nombre;
    private String url;

    public Canal(String nombre, String url) {
        this.nombre = nombre;
        this.url = url;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUrl() {
        return url;
    }
}
