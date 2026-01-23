# Hytale Wiki plugin

Allows the player to search the wiki in-game. Provides links to best results or previews the wiki page in-game.

Drawbacks:

- Not possible to avoid the "untrusted link" prompt
- Not possible to open a link directly without clicking in chat, as it happens client-side

## Commands

| Command                            | Description                                                                                                        |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `/wiki`                            | provides a base link to the wiki                                                                                   |
| `/wiki hand`                       | open page for item in hand                                                                                         |
| `/wiki <search term>`              | search the wiki for the most relevant entries, looks for exact matches; if exact match and `--ui`, open in-game UI |
| `/wiki page <page key/page title>` | open the page directly with no search                                                                              |

## Showcase

![showcase_hand.png](assets/showcase_hand.png)
![showcase_page.png](assets/showcase_page.png)
![showcase_page_not_exist.png](assets/showcase_page_not_exist.png)
![showcase_search_results.png](assets/showcase_search_results.png)
