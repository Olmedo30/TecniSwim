package com.project.tecniswim.ui.evaluate;

import androidx.lifecycle.ViewModel;
import java.util.HashMap;
import java.util.Map;

/**
 * ViewModel para guardar las respuestas de lateral y frontal por separado.
 * La clave en el Map es algo como "LATERAL|<texto del criterio>" o "FRONTAL|<texto del criterio>".
 * El valor es Boolean: true = “Apto”, false = “No apto”.
 */
public class QuestionsViewModel extends ViewModel {
    private final Map<String, Boolean> respuestas = new HashMap<>();

    public void setRespuesta(String criterioConSeccion, boolean esApto) {
        respuestas.put(criterioConSeccion, esApto);
    }

    public boolean tieneRespuesta(String criterioConSeccion) {
        return respuestas.containsKey(criterioConSeccion);
    }

    public Boolean getRespuesta(String criterioConSeccion) {
        return respuestas.get(criterioConSeccion);
    }

    public int getNumContestadosEnSeccion(String prefijo) {
        int contador = 0;
        for (String key : respuestas.keySet()) {
            if (key.startsWith(prefijo + "|")) {
                contador++;
            }
        }
        return contador;
    }

    public Map<String, Boolean> getTodasRespuestas() {
        return respuestas;
    }
}