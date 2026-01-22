package org.hytalewiki.net;

import com.hypixel.hytale.logger.backend.HytaleLogManager;
import org.hytalewiki.net.response.PageObject;
import org.hytalewiki.net.response.SearchResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WikiClientTests {

    private static final String HYTALE_WIKI_ORG_BASE_URL = "https://hytalewiki.org";

    private final WikiClient client = new WikiClient(HYTALE_WIKI_ORG_BASE_URL);

    @BeforeAll
    public static void beforeAll() {
        // doesn't work afair
        System.setProperty("java.util.logging.manager", HytaleLogManager.class.getName());
    }

    @Test
    void buildsTestURLs() {
        assertEquals("https://hytalewiki.org/w/Iron_Shovel?action=edit", client.getEditPageUrl("Iron Shovel"));
    }

    @Test
    public void buildsQueryParams() {
        assertEquals("/search?q=Iron+Shovel", WikiClient.PathBuilder.create("/search")
                .param("q", "Iron Shovel")
                .toString());
    }

    @Test
    public void createsCorrectRestURIs() {
        assertEquals(HYTALE_WIKI_ORG_BASE_URL + "/rest.php/v1/search", client.buildRestPath("/search").toString());

        assertEquals(HYTALE_WIKI_ORG_BASE_URL + "/rest.php/v1/search?q=Iron+Shovel&limit=10",
                client.buildRestPath("/search")
                        .param("q", "Iron Shovel")
                        .param("limit", "10")
                        .toURI().toString());
    }

    @Test
    public void searchesForPages() {
        SearchResult response = assertDoesNotThrow(() -> {
            return client.searchTitle("Iron", 10);
        });

        assertFalse(response.getPages().isEmpty());
    }

    @Test
    public void downloadsHTMLContent() {
        String result = assertDoesNotThrow(() -> {
            return client.html("Iron");
        });

        assertNotNull(result);
    }

    @Test
    public void getsBarePages() {
        PageObject response = assertDoesNotThrow(() -> {
            return client.page("Iron");
        });

        assertNotNull(response);
    }


}