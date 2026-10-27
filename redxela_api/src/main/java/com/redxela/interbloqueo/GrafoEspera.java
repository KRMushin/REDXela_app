package com.redxela.interbloqueo;

import com.redxela.dominio.TipoRecurso;
import com.redxela.recursos.EstadoRecurso;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Grafo de espera (wait-for graph) construido a partir del estado de los recursos.
 * 
 * Grafo de Asignación/Espera de Recursos (Resource Allocation Graph).
 * Cada arista "proceso A espera al recurso R retenido por B" comprimido como A → B.
 * Un ciclo en este grafo es la condición de espera circular de Coffman y, si todos
 * los retenedores están bloqueados, constituye un interbloqueo real.
 */
public class GrafoEspera {

    /** Arista etiquetada con el recurso que provoca la espera. */
    public record Arista(String espera, String retiene, TipoRecurso recurso) {}

    private final Map<TipoRecurso, EstadoRecurso> estados = new HashMap<>();
    private final Map<String, Set<String>> adyacencia = new HashMap<>();
    private final List<Arista> aristas = new ArrayList<>();
    private final Set<String> nodos = new HashSet<>();
    private final Set<String> bloqueados = new HashSet<>();

    private GrafoEspera() {
    }

    /**
     * Construye el grafo a partir de la foto de cada pool de recursos.
     * Solo genera aristas cuando existen procesos esperando y retenedores.
     */
    public static GrafoEspera construir(List<EstadoRecurso> estadosRecursos) {
        GrafoEspera grafo = new GrafoEspera();
        for (EstadoRecurso estado : estadosRecursos) {
            grafo.estados.put(estado.tipo(), estado);
            grafo.bloqueados.addAll(estado.pedidosEnEspera());
            grafo.nodos.addAll(estado.pedidosAsignados());
            grafo.nodos.addAll(estado.pedidosEnEspera());

            for (String espera : estado.pedidosEnEspera()) {
                for (String retiene : estado.pedidosAsignados()) {
                    grafo.adyacencia.computeIfAbsent(espera, k -> new TreeSet<>()).add(retiene);
                    grafo.aristas.add(new Arista(espera, retiene, estado.tipo()));
                }
            }
        }
        return grafo;
    }

    /**
     * DFS con pila de recursión: devuelve el primer ciclo encontrado o lista vacía.
     * Un nodo en la pila de recursión que se vuelve a alcanzar es una espera circular.
     */
    public List<String> buscarCiclo() {
        Set<String> visitados = new HashSet<>();
        Set<String> enPila = new LinkedHashSet<>();
        Deque<String> pila = new ArrayDeque<>();
        for (String nodo : new TreeSet<>(nodos)) {
            List<String> ciclo = dfs(nodo, visitados, enPila, pila);
            if (!ciclo.isEmpty()) {
                return ciclo;
            }
        }
        return List.of();
    }

    private List<String> dfs(String nodo, Set<String> visitados, Set<String> enPila, Deque<String> pila) {
        if (enPila.contains(nodo)) {
            List<String> ciclo = new ArrayList<>();
            boolean dentro = false;
            for (String actual : pila) {
                if (actual.equals(nodo)) {
                    dentro = true;
                }
                if (dentro) {
                    ciclo.add(actual);
                }
            }
            return ciclo;
        }
        if (!visitados.add(nodo)) {
            return List.of();
        }
        enPila.add(nodo);
        pila.addLast(nodo);
        for (String vecino : adyacencia.getOrDefault(nodo, Set.of())) {
            List<String> ciclo = dfs(vecino, visitados, enPila, pila);
            if (!ciclo.isEmpty()) {
                return ciclo;
            }
        }
        enPila.remove(nodo);
        pila.removeLast();
        return List.of();
    }

    /** Recursos involucrados en las aristas internas del ciclo detectado. */
    public Set<TipoRecurso> recursosDelCiclo(List<String> ciclo) {
        Set<String> nodosCiclo = new HashSet<>(ciclo);
        Set<TipoRecurso> recursos = new TreeSet<>(Comparator.comparing(TipoRecurso::name));
        for (Arista arista : aristas) {
            if (nodosCiclo.contains(arista.espera()) && nodosCiclo.contains(arista.retiene())) {
                recursos.add(arista.recurso());
            }
        }
        return recursos;
    }

    public List<String> retenedoresDe(TipoRecurso recurso) {
        EstadoRecurso estado = estados.get(recurso);
        return estado == null ? List.of() : estado.pedidosAsignados();
    }

    public Set<String> bloqueados() {
        return Set.copyOf(bloqueados);
    }

    public List<Arista> aristas() {
        return List.copyOf(aristas);
    }
}
