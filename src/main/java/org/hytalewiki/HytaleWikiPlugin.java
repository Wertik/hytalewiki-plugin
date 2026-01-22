package org.hytalewiki;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class HytaleWikiPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String HYTALE_WIKI_ORG_BASE_URL = "https://hytalewiki.org";

    public HytaleWikiPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Loaded Hytale Wiki plugin " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new WikiCommand(this));
        LOGGER.atInfo().log("Set up Hytale Wiki plugin " + this.getManifest().getVersion().toString());
    }
}