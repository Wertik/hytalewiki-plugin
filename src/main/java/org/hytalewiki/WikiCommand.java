package org.hytalewiki;

import au.ellie.hyui.builders.ContainerBuilder;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.UIElementBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.hytalewiki.net.RequestException;
import org.hytalewiki.net.WikiClient;
import org.hytalewiki.net.response.PageObject;
import org.hytalewiki.net.response.SearchEntry;
import org.hytalewiki.net.response.SearchResult;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WikiCommand extends AbstractAsyncCommand {

    private static final HytaleLogger log = HytaleLogger.forEnclosingClass();

    private final HytaleWikiPlugin plugin;

    private final WikiClient client = new WikiClient(HytaleWikiPlugin.HYTALE_WIKI_ORG_BASE_URL);

    private final WikiTextGenerator generator = new WikiTextGenerator();

    public WikiTextGenerator getGenerator() {
        return generator;
    }

    public WikiCommand(HytaleWikiPlugin plugin) {
        super("wiki", "Opens a wiki link for the specified item.");
        this.plugin = plugin;

        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP

        this.addSubCommand(new WikiPageCommand(this));
        this.addSubCommand(new WikiHandCommand(this));
        this.addUsageVariant(new WikiSearchCommand(this));
    }

    private static class WikiHandCommand extends AbstractAsyncCommand {
        private final FlagArg uiFlag;

        private final WikiCommand parent;

        WikiHandCommand(WikiCommand command) {
            super("hand", "Provide a link to the wiki page for the item in hand.");
            this.parent = command;

            this.uiFlag = this.withFlagArg("ui", "Open the in-game UI.");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context) {
            boolean ui = this.uiFlag.get(context);

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

            if (ui) {
                return CompletableFuture.runAsync(() -> this.parent.openPageUI(player, page));
            }

            context.sendMessage(this.parent.makeResultRow(page.getTitle(), page.getKey()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class WikiSearchCommand extends AbstractAsyncCommand {

        @Nonnull
        private final RequiredArg<String> termArg;

        private final FlagArg uiFlag;

        private final WikiCommand parent;

        WikiSearchCommand(WikiCommand command) {
            super("Search for a term on the wiki.");
            this.parent = command;

            this.termArg = this.withRequiredArg("term", "Term to search for.", ArgTypes.STRING);
            this.uiFlag = this.withFlagArg("ui", "Open the in-game UI.");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context) {
            // Attempt a search, look for exact matches

            String term = context.get(this.termArg)
                    // String extra quotes when using "Hello world" syntax for string arguments
                    .replace("\"", "");

            boolean ui = context.get(this.uiFlag);

            // todo: try and look for an item with the term as an id, search for the display name
            // todo: if there's an in-game item with the name, invite the user to create it

            SearchResult result;
            try {
                result = this.parent.client.search(term, 10);
            } catch (RequestException e) {
                return CompletableFuture.failedFuture(e);
            }

            final SearchEntry exactMatch = parent.findExactMatch(result, term);

            if (exactMatch != null && ui && context.sender() instanceof Player player) {

                PageObject page;
                try {
                    page = this.parent.client.page(exactMatch.getKey());
                } catch (RequestException e) {
                    return CompletableFuture.failedFuture(e);
                }

                return CompletableFuture.runAsync(() -> parent.openPageUI(player, page));
            }

            Message message = parent.makeHeader("Results");
            if (exactMatch == null) {
                message.insert(this.parent.makeCreateNotice(term)).insert(Message.raw("\n"));
            }
            message.insert(parent.makeResultList(result.getPages()));

            context.sendMessage(message);
            return CompletableFuture.completedFuture(null);
        }
    }

    private class WikiPageCommand extends AbstractAsyncCommand {
        private final FlagArg uiFlag;
        private final RequiredArg<String> keyArg;

        private final WikiCommand parent;

        WikiPageCommand(WikiCommand command) {
            super("page", "Open a wiki page by exact key.");
            this.parent = command;

            this.setPermissionGroup(GameMode.Adventure);

            this.uiFlag = this.withFlagArg("ui", "Open UI with the page rendered.");
            this.keyArg = this.withRequiredArg("key", "The key to search for.", ArgTypes.STRING);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context) {
            String key = context.get(this.keyArg)
                    // String extra quotes when using "Hello world" syntax for string arguments
                    .replace("\"", "");

            log.at(Level.INFO).log("Key: '" + key + "'");

            boolean ui = this.uiFlag.get(context);

            CommandSender sender = context.sender();

            if (ui && !(sender instanceof Player)) {
                context.sendMessage(Message.raw("Cannot open UI for console.").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            PageObject page;
            try {
                page = client.page(key);
            } catch (RequestException e) {
                return CompletableFuture.failedFuture(e);
            }

            if (page == null || page.getKey() == null) {
                context.sendMessage(makeCreateNotice(key));
                return CompletableFuture.completedFuture(null);
            }

            if (ui) {
                return CompletableFuture.runAsync(() -> openPageUI((Player) sender, page));
            }

            context.sendMessage(this.parent.makeResultRow(page.getTitle(), page.getKey()));
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

    private void openPageUI(Player player, PageObject page) {
        player.sendMessage(Message.raw("UIs are highly unstable. Expect it not to work.").color(Color.lightGray));

        World world = player.getWorld();

        if (world == null) {
            player.sendMessage(Message.raw("Failed to open UI.").color(Color.RED));
            log.at(Level.SEVERE).log("Failed to open UI for player " + player.getDisplayName() + " (world == null)");
            return;
        }

        world.execute(() -> {
            Ref<EntityStore> ref = player.getReference();

            if (ref == null) {
                player.sendMessage(Message.raw("Failed to open UI.").color(Color.RED));
                log.at(Level.SEVERE).log("Failed to open UI for player " + player.getDisplayName() + " (ref == null)");
                return;
            }

            Store<EntityStore> store = ref.getStore();

            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());

            UIElementBuilder<?> root = getGenerator().generate(page.getSource());

            ContainerBuilder container = ContainerBuilder.container().addContentChild(root);
            container.withTitleText(page.getTitle());

            PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRefComponent);
            pageBuilder.addElement(container);
            pageBuilder.open(store);
        });
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

    private Message makeResultList(List<SearchEntry> entries) {
        Message builder = Message.empty();
        for (SearchEntry entry : entries) {
            builder.insert(makeResultRow(entry)).insert(Message.raw("\n"));
        }
        return builder;
    }

    private String getDisplayName(Item item) {
        String translationKey = item.getTranslationKey();
        return Message.translation(translationKey).getAnsiMessage();
    }
}