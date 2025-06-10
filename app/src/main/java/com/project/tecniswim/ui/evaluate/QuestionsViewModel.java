package com.project.tecniswim.ui.evaluate;

import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class QuestionsViewModel extends ViewModel {
    private String selectedStyle = null;
    private final Map<String, Boolean> respuestas = new HashMap<>();
    private String tecnicoNombre;
    public void setTecnicoNombre(String nombre) { tecnicoNombre = nombre; }
    public String getTecnicoNombre() { return tecnicoNombre; }

    public void setSelectedStyle(String style) {
        this.selectedStyle = style;
    }

    public String getSelectedStyle() {
        return selectedStyle;
    }

    public void setRespuesta(String clave, boolean valor) {
        respuestas.put(clave, valor);
    }

    public boolean tieneRespuesta(String clave) {
        return respuestas.containsKey(clave);
    }

    public boolean getRespuesta(String clave) {
        Boolean val = respuestas.get(clave);
        return val != null && val;
    }

    /**
     * Cuenta cuántas preguntas de una sección concreta (por ejemplo, "VISIÓN LATERAL") han sido contestadas.
     * Se asume que las claves en el mapa de respuestas tienen la forma "SECCION|<texto criterio>"
     */
    public int getNumContestadosEnSeccion(String seccion) {
        int contador = 0;
        for (String clave : respuestas.keySet()) {
            if (clave.startsWith(seccion + "|")) {
                contador++;
            }
        }
        return contador;
    }

    /**
     * Devuelve el mapa completo de respuestas (para construir el PDF, etc.).
     */
    public Map<String, Boolean> getTodasRespuestas() {
        return respuestas;
    }
}