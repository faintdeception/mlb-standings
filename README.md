# MLB Standings Glyph

A Nothing Phone Glyph Matrix application that displays MLB standings information directly on your device's Glyph Matrix display.

## Features

- **Favorite Team Record**: Display your favorite team's current wins and losses
- **Top 5 MLB Teams**: Cycle through the top 5 teams in MLB by overall ranking
- **Division Standings**: Show standings for your favorite team's division
- **Data Caching**: Fetches data once per day to minimize API calls
- **Glyph Button Integration**: Use short/long press to navigate and change display modes

## Display Modes

The app has three display modes that you can cycle through using the Glyph Button:

1. **Team Record**: Shows your favorite team's wins/losses in a compact format
2. **Top Teams**: Displays the top 5 MLB teams with their rankings and abbreviations
3. **Division**: Shows all teams in your favorite team's division with rankings

## Controls

- **Short Press**: Activate the MLB Standings Glyph Toy (cycles through available toys)
- **Long Press**: Change display mode (Team Record → Top Teams → Division → Team Record)
- **AOD Support**: Automatically updates display every minute when in Always-On Display mode

## Setup Instructions

### Prerequisites

1. **Nothing Phone with Glyph Matrix** (Phone 3 or compatible device)
2. **Android Studio** for building the app
3. **GlyphMatrixSDK.aar** from the Nothing Developer Programme

### Installation

1. **Clone this repository**:
   ```bash
   git clone <repository-url>
   cd mlb-standings
   ```

2. **Add Glyph Matrix SDK**:
   - Download `GlyphMatrixSDK.aar` from [Nothing Developer Programme](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
   - Place it in `app/libs/GlyphMatrixSDK.aar`

3. **Build and Install**:
   - Open the project in Android Studio
   - Connect your Nothing Phone
   - Build and run the application

4. **Enable the Glyph Toy**:
   - Go to Settings → Glyph Interface on your Nothing Phone
   - Find "MLB Standings" in the Glyph Toys list
   - Move it from "Disabled" to "Active" state

### Configuration

1. **Open the MLB Standings app** on your phone
2. **Select your favorite team** from the dropdown menu
3. **Tap "Refresh Data"** to fetch the latest standings
4. **Test the display** using the "Test Glyph Display" button

## Data Source

This app uses the official MLB Stats API:
- **Endpoint**: `https://statsapi.mlb.com/api/v1/standings`
- **Update frequency**: Once per day (cached locally)
- **Data includes**: Team records, standings, rankings, and division information

## Technical Details

### Architecture

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Dependencies**:
  - GlyphMatrixSDK (Nothing Developer Kit)
  - Retrofit (API calls)
  - DataStore (Settings and caching)
  - Coroutines (Async operations)

### Project Structure

```
app/src/main/java/com/dreampipe/mlbstandings/
├── MainActivity.kt                    # Main configuration screen
├── data/
│   ├── api/
│   │   └── MLBApiService.kt          # MLB API interface
│   ├── model/
│   │   └── MLBStandingsModels.kt     # Data models
│   └── repository/
│       └── MLBStandingsRepository.kt  # Data management
└── glyph/
    ├── GlyphMatrixUtils.kt           # Bitmap generation utilities
    └── MLBStandingsGlyphToyService.kt # Main Glyph Toy service
```

### Key Components

- **MLBStandingsGlyphToyService**: Main service that handles Glyph Matrix display and user interactions
- **MLBStandingsRepository**: Manages API calls, caching, and data retrieval
- **GlyphMatrixUtils**: Utility functions for creating displayable bitmaps from MLB data

## Usage

### First Time Setup

1. Install and open the app
2. Select your favorite MLB team
3. Tap "Refresh Data" to download current standings
4. Enable the Glyph Toy in phone settings

### Daily Usage

1. **Short press** the Glyph Button to activate MLB Standings
2. **Long press** to cycle through display modes:
   - Your team's record (W/L)
   - Top 5 MLB teams
   - Your team's division standings
3. Data automatically refreshes once per day

## Troubleshooting

### Common Issues

- **"Network Error"**: Check internet connection and try refreshing data
- **"Team not found"**: Ensure your favorite team name matches exactly with MLB roster
- **Glyph not displaying**: Verify the toy is enabled in Glyph Interface settings

### Debug Mode

The app includes comprehensive logging. Connect to Android Studio and check the logcat for detailed information:
- Tag: `MLBStandingsGlyphToy` for Glyph Matrix operations
- Tag: `MainActivity` for UI operations

## Contributing

This project is built following Nothing's minimalist philosophy - simple, functional, and unobtrusive. When contributing:

1. Keep the display simple and readable on the 25x25 Glyph Matrix
2. Minimize network calls (respect the once-per-day cache)
3. Ensure all interactions work with Glyph Button only
4. Test thoroughly on actual Nothing Phone hardware

## License

This project is open source. Please respect MLB's terms of service when using their API.

## Acknowledgments

- **Nothing Developer Programme** for the Glyph Matrix SDK
- **MLB** for providing the public stats API
- **Nothing Community** for support and inspiration

---

*Built with ⚾ for Nothing Phone owners who love baseball*
