package pw.tdekk.deob;

import java.util.*;

/**
 * Created by TimD on 6/17/2016.
 */
public class DirectedGraph<V, E> implements Iterable<V> {

    private final Map<V, Set<E>> base = new HashMap<>();

    public Set<E> edgeAt(int index) {
        return (Set<E>) base.values().toArray()[index];
    }

    public int size() {
        return base.size();
    }

    public boolean containsVertex(V vertex) {
        return base.containsKey(vertex);
    }

    public boolean containsEdge(V vertex, E edge) {
        return base.containsKey(vertex) && base.get(vertex).contains(edge);
    }

    public boolean addVertex(V vertex) {
        if (base.containsKey(vertex)) {
            return false;
        }
        base.put(vertex, new HashSet<>());
        return true;
    }

    public void addEdge(V start, E dest) {
        if (!base.containsKey(start)) {
            return;
        }
        base.get(start).add(dest);
    }

    public void removeEdge(V start, E dest) {
        if (!base.containsKey(start)) {
            return;
        }
        base.get(start).remove(dest);
    }

    public Set<E> edgesFrom(V vertex) {
        return Collections.unmodifiableSet(base.get(vertex));
    }

    public void consume(DirectedGraph<V, E> graph) {
        this.base.putAll(graph.base);
    }

    @Override
    public final Iterator<V> iterator() {
        return base.keySet().iterator();
    }

    public void clear() {
        base.clear();
    }
}
