# TahoCurrency

A standalone Paper plugin with a separate currency balance, mob-kill rewards, and a GUI shop.

## Features
- Killing eligible mobs rewards a random **200–1,000 Taho Coins** per kill.
- Persistent balances saved to `plugins/TahoCurrency/balances.yml`.
- `/taho` checks your Taho Coins balance; `/taho reload` reloads the config for admins with `tahocurrency.admin`.
- `/tahoshop` opens the shop GUI.
- `/tahobaltop` opens a leaderboard GUI showing the top 10 balances.
- `/currency` remains available for balance/shop and `/currency reload` for admins.
- Shop: Elytra with **Unbreaking III + Mending** for **100,000**; Dragon Head for **50,000**.
- Configurable rewards, excluded mobs, prices, shop slots, item names/lore, and messages.
- `/currency reload` remains available as a legacy admin command.

## Build
Requires JDK 25 and Maven, targeting Paper 26.1.0.

```sh
mvn -U clean package
```

Put `target/TahoCurrency.jar` into your server's `plugins` directory and restart. Edit `plugins/TahoCurrency/config.yml` to configure it.

## Notes
This is its own currency and does not use Vault or change your server's existing economy. Player kills do not award coins. Rewards are given to the player who gets the mob kill credit.
