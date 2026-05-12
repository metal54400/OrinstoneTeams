# OrinstoneTeams

Plugin Paper 1.21.11 Gradle.

## Build
```bash
gradle build
```

Jar:
```text
build/libs/OrinstoneTeams-1.0.0.jar
```

## Commandes
- `/team create <nom>`
- `/team info [team]`
- `/team baltop`
- `/team sethome`
- `/team home`
- `/team list`
- `/team join <team>`
- `/team invite <joueur>`
- `/team disband`
- `/team leave`
- `/team bal`
- `/team setowner <joueur>`
- `/team setadmin <joueur>`

## PlaceholderAPI
- `%orinstoneteams_team%` : nom de la team avec couleur configurable, par défaut `&7`
- `%orinstoneteams_team_raw%` : nom brut
- `%orinstoneteams_team_smallcaps%` : nom en smallcaps avec couleur configurable

## Vault
Vault est intégré pour `settings.create-cost`.


## Invite
Active `settings.join-requires-invite: true` pour obliger les joueurs à être invités avant `/team join`.
