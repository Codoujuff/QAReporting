package com.qareporting.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Recherche insensible aux accents et à la casse, pagination bornée. */
class ListPageTest {

    @Test
    void searchIgnoresAccentsAndCase() {
        ListPage<String> page = new ListPage<>();
        page.apply(List.of("Anomalie réouverte", "Paiement refusé", "Connexion"), "REOUVERTE", s -> s, null, 1);
        assertEquals(List.of("Anomalie réouverte"), page.getItems());
    }

    @Test
    void pagesAreCutAndOutOfRangePagesAreClamped() {
        List<Integer> all = IntStream.rangeClosed(1, 45).boxed().toList();
        ListPage<Integer> page = new ListPage<>();
        page.apply(all, null, String::valueOf, null, 3);
        assertEquals(3, page.getPageCount());
        assertEquals(List.of(41, 42, 43, 44, 45), page.getItems());
        assertEquals(41, page.getFrom());
        page.apply(all, null, String::valueOf, null, 99);
        assertEquals(3, page.getPage());
        page.apply(all, null, String::valueOf, n -> n % 2 == 0, null);
        assertEquals(22, page.getTotal());
    }
}
