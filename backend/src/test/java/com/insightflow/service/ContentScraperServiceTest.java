package com.insightflow.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentScraperServiceTest {

    private final ContentScraperService scraper = new ContentScraperService();

    @Test
    void testStripHtmlRemovesTagsAndCollapsesWhitespace() {
        String html = "<html><body><h1>Title</h1><p>Paragraph with <a href='#'>link</a>.</p></body></html>";
        String stripped = scraper.stripHtml(html);

        assertEquals("Title Paragraph with link .", stripped);
    }

    @Test
    void testResolveInputReturnsPlainTextDirectly() {
        String text = "This is ordinary text, not a URL.";
        String resolved = scraper.resolveInput(text);

        assertEquals(text, resolved);
    }
}
