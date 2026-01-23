package org.hytalewiki;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.hytalewiki.net.RequestException;
import org.hytalewiki.net.WikiClient;
import org.hytalewiki.net.response.PageObject;
import org.hytalewiki.net.response.SearchEntry;
import org.hytalewiki.net.response.SearchResult;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WikiCommand extends AbstractAsyncCommand {

    private static final HytaleLogger log = HytaleLogger.forEnclosingClass();

    private final HytaleWikiPlugin plugin;

    private final WikiClient client = new WikiClient(HytaleWikiPlugin.HYTALE_WIKI_ORG_BASE_URL);

    public WikiCommand(HytaleWikiPlugin plugin) {
        super("wiki", "Opens a wiki link for the specified item.");
        this.plugin = plugin;

        this.setPermissionGroup(GameMode.Adventure);

        this.requirePermission("hytalewiki.wiki");

        this.addSubCommand(new WikiPageCommand(this));
        this.addSubCommand(new WikiHandCommand(this));
        this.addUsageVariant(new WikiSearchCommand(this));
    }

    private static class WikiHandCommand extends AbstractAsyncCommand {
        private final WikiCommand parent;

        WikiHandCommand(WikiCommand command) {
            super("hand", "Provide a link to the wiki page for the item in hand.");
            this.parent = command;

            this.requirePermission("hytalewiki.wiki.hand");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context) {
            if (!(context.sender() instanceof Player player)) {
                context.sendMessage(Message.raw("Only players can do this.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            ItemStack activeHotbarItem = player.getInventory().getActiveHotbarItem();

            if (activeHotbarItem == null) {
                context.sendMessage(Message.raw("That's just your dirty hand."));
                return CompletableFuture.completedFuture(null);
            }

            Item item = activeHotbarItem.getItem();

            String displayName = this.parent.getDisplayName(item);

            // HytaleWiki uses display names instead of in-game ids for page keys

            PageObject page;
            try {
                page = this.parent.client.page(displayName);
            } catch (RequestException e) {
                return CompletableFuture.failedFuture(e);
            }

            if (page == null || page.getKey() == null) {
                context.sendMessage(this.parent.makeCreateNotice(displayName));
                return CompletableFuture.completedFuture(null);
            }

            context.sendMessage(this.parent.makeResultRow(page.getTitle(), page.getKey()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class WikiSearchCommand extends AbstractAsyncCommand {

        @Nonnull
        private final RequiredArg<String> termArg;

        private final WikiCommand parent;

        WikiSearchCommand(WikiCommand command) {
            super("Search for a term on the wiki.");
            this.parent = command;

            this.requirePermission("hytalewiki.wiki.search");

            this.termArg = this.withRequiredArg("term", "Term to search for.", ArgTypes.STRING);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context) {
            // Attempt a search, look for exact matches

            String term = context.get(this.termArg)
                    // String extra quotes when using "Hello world" syntax for string arguments
                    .replace("\"", "");

            String query = term;
            SearchResult result;

            Item existingItem = Item.getAssetStore().getAssetMap().getAsset(term);

            boolean transformed = false;

            if (existingItem != null) {
                // Item exists, which means the term was an in-game ID.
                // We use display names on the wiki.
                query = this.parent.getDisplayName(existingItem);
                transformed = true;
            }

            try {
                result = this.parent.client.search(query, 10);
            } catch (RequestException e) {
                return CompletableFuture.failedFuture(e);
            }

            final SearchEntry exactMatch = parent.findExactMatch(result, term);

            Message message = parent.makeHeader("Results");

            if (transformed) {
                message.insert(this.parent.makeQueryChangeNotice(query)).insert("\n");
            }

            if (exactMatch == null) {
                message.insert(this.parent.makeCreateNotice(query)).insert("\n");
            }

            message.insert(parent.makeResultList(result.getPages()));

            context.sendMessage(message);
            return CompletableFuture.completedFuture(null);
        }
    }

    private class WikiPageCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> keyArg;

        private final WikiCommand parent;

        WikiPageCommand(WikiCommand command) {
            super("page", "Open a wiki page by exact key.");
            this.parent = command;

            this.requirePermission("hytalewiki.wiki.page");

            this.keyArg = this.withRequiredArg("key", "The key to search for.", ArgTypes.STRING);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context) {
            String key = context.get(this.keyArg)
                    // String extra quotes when using "Hello world" syntax for string arguments
                    .replace("\"", "");

            Item existingItem = Item.getAssetStore().getAssetMap().getAsset(key);

            String query = key;

            boolean transformed = false;

            if (existingItem != null) {
                // Item exists, which means the term was an in-game ID.
                // We use display names on the wiki.

                // todo: expand to other ID types as well

                query = this.parent.getDisplayName(existingItem);
                transformed = true;
            }

            PageObject page;
            try {
                page = client.page(query);
            } catch (RequestException e) {
                return CompletableFuture.failedFuture(e);
            }

            Message message = Message.empty();

            if (transformed) {
                message.insert(this.parent.makeQueryChangeNotice(query)).insert("\n");
            }

            if (page == null || page.getKey() == null) {
                context.sendMessage(message.insert(makeCreateNotice(key)));
                return CompletableFuture.completedFuture(null);
            }

            context.sendMessage(message.insert(this.parent.makeResultRow(page.getTitle(), page.getKey())));
            return CompletableFuture.completedFuture(null);
        }
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context) {
        // Link to the base wiki page

        context.sendMessage(Message.join(
                makeHeader("HytaleWiki.org"),
                Message.raw("[ Click to open ]").link(client.getBaseUrl()).color(Colors.HYPIXEL_BUTTON_COLOR)
        ));

        return CompletableFuture.completedFuture(null);
    }

    private SearchEntry findExactMatch(SearchResult result, String term) {
        SearchEntry exactMatch = null;

        for (SearchEntry entry : result.getPages()) {
            if (entry.getTitle().equalsIgnoreCase(term)) {
                exactMatch = entry;
                break;
            }
        }
        return exactMatch;
    }

    private Message makeHeader(String title) {
        return Message.raw("== ").color(Colors.WIKI_SECTION_COLOR)
                .insert(Message.raw(title).color(Color.WHITE).bold(true))
                .insert(Message.raw(" ==\n").color(Colors.WIKI_SECTION_COLOR));
    }

    private Message makeResultRow(SearchEntry entry) {
        return makeResultRow(entry.getTitle(), entry.getKey());
    }

    private Message makeResultRow(String title, String key) {
        return Message.join(Message.raw(title).color(Colors.HYPIXEL_TEXT_COLOR),
                Message.raw("     "),
                Message.raw("[ View ]")
                        .color(Colors.HYPIXEL_BUTTON_COLOR)
                        .link(this.client.getPageUrl(key)),
                Message.raw(" | ").color(Color.lightGray),
                Message.raw("[ Edit ]")
                        .color(Colors.HYPIXEL_BUTTON_COLOR)
                        .link(this.client.getEditPageUrl(key)));
    }

    private Message makeCreateNotice(String key) {
        return Message.join(
                Message.raw("Page \"").color(Color.WHITE),
                Message.raw(key).color(Color.RED),
                Message.raw("\" doesn't exist yet.").color(Color.WHITE),
                Message.raw(" "),
                Message.raw("[ Create ]").color(Colors.HYPIXEL_BUTTON_COLOR).link(client.getEditPageUrl(key))
        );
    }

    private Message makeQueryChangeNotice(String query) {
        return Message.join(
                Message.raw("Query changed to \"").color(Color.lightGray),
                Message.raw(query).color(Color.CYAN),
                Message.raw("\".").color(Color.lightGray)
        );
    }

    private Message makeResultList(List<SearchEntry> entries) {
        Message builder = Message.empty();
        for (Iterator<SearchEntry> iterator = entries.iterator(); iterator.hasNext(); ) {
            SearchEntry entry = iterator.next();
            builder.insert(makeResultRow(entry));

            // newline after row until the last one (server adds the last one automatically)
            if (iterator.hasNext()) {
                builder.insert(Message.raw("\n"));
            }
        }
        return builder;
    }

    private String getDisplayName(Item item) {
        String translationKey = item.getTranslationKey();
        return Message.translation(translationKey).getAnsiMessage();
    }
}