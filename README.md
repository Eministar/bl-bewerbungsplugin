# BL-Bewerbungsplugin

Kleines Spigot-Plugin für die Bewerbung: Spieler können per Command Items aus der Config bekommen. Mit Permissions, Cooldowns, Sounds, Tab-Completion und Reload.

## Was kann das Ding?

- `/spawnitem <item>` gibt Config-Items
- jedes Item hat eine eigene Permission
- Cooldown pro Spieler + Item
- Cooldown kann per Permission überschrieben werden
- Cooldowns bleiben nach Reconnect/Restart drin
- `/myplugin reload` lädt die Config neu
- `/info` zeigt die kleine Bewerbungsnachricht
- Config updated sich selbst, ohne deine Werte wegzuballern

## Build / Install

```bash
mvn package
```

Dann die Jar aus `target/BL-Bewerbungsplugin-1.0-SNAPSHOT.jar` in den Server-`plugins` Ordner ziehen und Server starten.

## Commands

```text
/spawnitem <Item-Name>
/getitem <Item-Name>
/myplugin reload
/info
```

## Permissions

```text
bewerbungsplugin.admin.reload
bewerbungsplugin.spawnitem.<item>
bewerbungsplugin.spawnitem.*
bewerbungsplugin.cooldown.bypass
bewerbungsplugin.cooldown.<sekunden>
```

Beispiele:

```text
bewerbungsplugin.spawnitem.apfel
bewerbungsplugin.cooldown.30
bewerbungsplugin.cooldown.0
```

Wenn jemand mehrere Cooldown-Perms hat, nimmt das Plugin den kleinsten Wert. Also `cooldown.10` gewinnt gegen `cooldown.60`.

## Config kurz erklärt

Items liegen in der `config.yml`:

```yml
items:
  apfel:
    material: GOLDEN_APPLE
    amount: 1
    cooldown: 180
    permission: "bewerbungsplugin.spawnitem.apfel"
    display-name: "&6Notfall-Apfel"
```

Dann geht:

```text
/spawnitem apfel
```

Messages, Prefix, Sounds und Startnachricht sind auch in der Config. Sounds können als Bukkit-Name wie `UI_BUTTON_CLICK` oder als Registry-Key wie `ui.button.click` rein.

## Cooldowns

Cooldowns werden aktuell in `cooldowns.yml` gespeichert, damit keiner einfach reconnectet und den Cooldown dodged.

`cooldowns.yml` kann man nutzen aber nicht wirklich nice für production. YAML für Cooldowns ist bei vielen Spielern einfach meh, weil Datei-Schreiberei langsam und nicht super stabil ist. Eine DB wäre viel cleaner.

Aber DB wäre besser. Ist aber Overkill hierfür imo.

## Auto Config Update

Wenn neue Config-Optionen dazukommen, ergänzt das Plugin sie automatisch. Deine bestehenden Werte werden nicht überschrieben.
