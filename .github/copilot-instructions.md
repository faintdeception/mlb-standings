<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# MLB Standings Glyph - Copilot Instructions

This is an Android Kotlin project for Nothing Phone Glyph Matrix that displays MLB standings.

## Project Context

- **Target Device**: Nothing Phone with Glyph Matrix (25x25 LED display)
- **Language**: Kotlin
- **Framework**: Android SDK + GlyphMatrixSDK
- **API**: MLB Stats API (https://statsapi.mlb.com/api/v1/standings)
- **Display Philosophy**: Minimalist, low-key, respect Nothing's design principles

## Key Components

1. **MLBStandingsGlyphToyService**: Main Glyph Toy service that handles display and interactions
2. **MLBStandingsRepository**: Data management with caching (once per day updates)
3. **GlyphMatrixUtils**: Bitmap generation for 25x25 Glyph Matrix display
4. **MainActivity**: Simple configuration UI for favorite team selection

## Display Modes

- **Team Record**: Favorite team wins/losses
- **Top Teams**: Top 5 MLB teams with rankings
- **Division**: Division standings for favorite team

## Glyph Button Interactions

- **Short press**: Activate toy (cycle through available toys)
- **Long press**: Change display mode
- **AOD**: Auto-update every minute

## Development Guidelines

1. **Simplicity First**: Keep displays simple and readable on 25x25 matrix
2. **Minimal Network**: Cache data once per day, no excessive API calls
3. **Glyph-Only Controls**: All interactions through Glyph Button only
4. **Error Handling**: Always provide fallback displays for network/data errors
5. **Logging**: Use consistent tags for debugging (MLBStandingsGlyphToy, MainActivity)

## Code Patterns

- Use coroutines for async operations
- Implement proper service lifecycle (onBind/onUnbind)
- Handle Glyph Matrix events through Handler/Messenger pattern
- Store settings using DataStore preferences
- Create bitmaps programmatically for Glyph display

## When modifying:

- Test display on actual 25x25 grid constraints
- Ensure AOD compatibility for battery efficiency
- Maintain Nothing's minimal aesthetic
- Respect MLB API rate limits and caching strategy
- Follow Android service best practices
