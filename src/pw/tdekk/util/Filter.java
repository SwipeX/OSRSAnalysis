package pw.tdekk.util;

/**
 * @author Tyler Sedlar
 * @since 3/1/2015
 */
public interface Filter<E> {

    public boolean accept(E e);
}
