# Gigaman

**Gigaman** is a Discord bot for [Tibia](https://www.tibia.com/), designed to help players track in-game trends with real-time data — all within your Discord server.

## 🚀 Features

- Death tracker + Anti death spam + Minimum level filter
- Custom loot splitter
- Events tracking
- Auctionable houses tracking
- Killed bosses statistics
- Mini world events tracking
- Servers status
- Boosted monster and boss
- Tibia coins prices tracker
- Drome notification

## ⚙️ Setup

### 1. Prerequisites

- Java 17+
- Maven
- [MongoDB](https://www.mongodb.com/) instance (local or remote)
- [Discord bot token](https://discord.com/developers)
- Chrome + [ChromeDriver](https://chromedriver.chromium.org/)

### 2. Clone and Build

```bash
git clone https://github.com/Gulyeh/tibiabot.git
cd tibiabot
mvn package
java -jar target/tibiabot.jar
```

### 3. Set config.env
Config should be in the same directory as jar file
```bash
BOT_KEY=your_discord_bot_token
DB_LOGIN=
DB_PASSWORD=
DB_NAME=
DB_COLLECTION_GUILDS=
DB_COLLECTION_CHARACTERS=
CHROMEDRIVER_PATH=

COIN_EMOJI_ID=
BLANK_EMOJI_ID=
RED_DOWN_EMOJI_ID=
GREEN_UP_EMOJI_ID=
GREEN_DOWN_EMOJI_ID=
GREEN_BE_ID=
YELLOW_BE_ID=
```
