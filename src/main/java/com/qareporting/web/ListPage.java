package com.qareporting.web;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Recherche + pagination d'une liste déjà limitée au périmètre du rôle (listForUser).
 * Les critères viennent de l'URL (?q=…&status=…&page=…) : une recherche se partage et
 * survit au bouton « Précédent ». Filtrage en mémoire, suffisant pour quelques milliers
 * de lignes ; au-delà, il faudrait paginer en JPQL (voir le cahier des charges, §10).
 */
public class ListPage<T> implements Serializable {

    public static final int PAGE_SIZE = 20;

    private List<T> items = List.of();
    private int page = 1;
    private int pageCount = 1;
    private int total;

    /**
     * @param all        la liste complète (déjà dans le périmètre)
     * @param query      texte recherché (insensible à la casse et aux accents), ou null
     * @param searchable texte d'une ligne dans lequel chercher
     * @param extra      filtre supplémentaire (statut...), ou null
     * @param requested  numéro de page demandé (1 par défaut)
     */
    public void apply(List<T> all, String query, Function<T, String> searchable, Predicate<T> extra, Integer requested) {
        String needle = normalize(query);
        List<T> filtered = new ArrayList<>();
        for (T row : all) {
            if (extra != null && !extra.test(row)) {
                continue;
            }
            if (!needle.isEmpty() && !normalize(searchable.apply(row)).contains(needle)) {
                continue;
            }
            filtered.add(row);
        }
        total = filtered.size();
        pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        page = requested == null ? 1 : Math.min(Math.max(1, requested), pageCount);
        int from = (page - 1) * PAGE_SIZE;
        items = new ArrayList<>(filtered.subList(from, Math.min(from + PAGE_SIZE, total)));
    }

    /**
     * Pagination faite par la base : on donne le nombre total de lignes, on reçoit le rang
     * de la première ligne à charger (page demandée ramenée dans les bornes), puis setItems().
     */
    public int prepare(long totalRows, Integer requested) {
        total = (int) Math.min(totalRows, Integer.MAX_VALUE);
        pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        page = requested == null ? 1 : Math.min(Math.max(1, requested), pageCount);
        return (page - 1) * PAGE_SIZE;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    /** Minuscules sans accents : « Réouverte » est trouvé en tapant « reouverte ». */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(s.strip(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getPageCount() { return pageCount; }
    public int getTotal() { return total; }
    public boolean isPaginated() { return pageCount > 1; }
    public boolean isHasPrevious() { return page > 1; }
    public boolean isHasNext() { return page < pageCount; }
    public int getPreviousPage() { return page - 1; }
    public int getNextPage() { return page + 1; }
    public int getFrom() { return total == 0 ? 0 : (page - 1) * PAGE_SIZE + 1; }
    public int getTo() { return Math.min(page * PAGE_SIZE, total); }
}
