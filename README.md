# Hytale Wiki plugin

Allows the player to search the wiki in-game. Provides links to best results or previews the wiki page in-game.

Drawbacks:

- Not possible to avoid the "untrusted link" prompt
- Not possible to open a link directly without clicking in chat, as it happens client-side

## Commands

| Command                            | Description                                                                                                        | Permission               |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------|--------------------------|
| `/wiki`                            | provides a base link to the wiki                                                                                   | `hytalewiki.wiki`        |
| `/wiki hand`                       | open page for item in hand                                                                                         | `hytalewiki.wiki.hand`   |
| `/wiki <search term>`              | search the wiki for the most relevant entries, looks for exact matches; if exact match and `--ui`, open in-game UI | `hytalewiki.wiki.search` |
| `/wiki page <page key/page title>` | open the page directly with no search                                                                              | `hytalewiki.wiki.page`   |

## Notes

If the search term or page title matches an in-game item ID exactly, it gets translated into the item's display name for
the search. This is because hytalewiki.org uses display names for page keys instead of IDs.

## Showcase

![showcase_hand.png](assets/showcase_hand.png)
![showcase_page.png](assets/showcase_page.png)
![showcase_page_not_exist.png](assets/showcase_page_not_exist.png)
![showcase_search_results.png](assets/showcase_search_results.png)
